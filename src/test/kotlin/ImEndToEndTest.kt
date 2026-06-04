package org.example

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.entity.ChatGroup
import org.example.entity.GroupMember
import org.example.entity.GroupMemberId
import org.example.repository.ChatGroupRepository
import org.example.repository.GroupMemberRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImEndToEndTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var om: ObjectMapper
    @Autowired lateinit var chatGroupRepository: ChatGroupRepository
    @Autowired lateinit var groupMemberRepository: GroupMemberRepository

    private fun register(username: String, password: String = "p@ssw0rd"): Long {
        val body = mapOf("username" to username, "password" to password, "nickname" to username)
        val resp = mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body))
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return om.readTree(resp).path("data").path("uid").asLong()
    }

    private fun login(username: String, password: String = "p@ssw0rd"): Pair<Long, String> {
        val body = mapOf("username" to username, "password" to password)
        val resp = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body))
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val node = om.readTree(resp).path("data")
        return node.path("uid").asLong() to node.path("token").asText()
    }

    private fun authPost(path: String, token: String, body: Any) =
        mockMvc.perform(
            post(path)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body))
        )

    private fun authGet(path: String, token: String) =
        mockMvc.perform(get(path).header("Authorization", "Bearer $token"))

    private fun newMsgId() = UUID.randomUUID().toString()

    @Test
    fun `single chat - offline receiver gets messages via sync then confirm clears them`() {
        val alice = "alice_${System.nanoTime()}"
        val bob = "bob_${System.nanoTime()}"
        val aliceUid = register(alice)
        val bobUid = register(bob)
        val (_, aliceToken) = login(alice)
        val (_, bobToken) = login(bob)

        // Alice sends two messages to Bob (Bob is offline since no WS connected).
        val m1 = newMsgId()
        val m2 = newMsgId()
        authPost("/api/message/send", aliceToken, mapOf("msgId" to m1, "toUid" to bobUid, "type" to 1, "content" to "hello"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.msgId").value(m1))
            .andExpect(jsonPath("$.data.seq").isNumber)
        authPost("/api/message/send", aliceToken, mapOf("msgId" to m2, "toUid" to bobUid, "type" to 1, "content" to "world"))
            .andExpect(status().isOk)

        // Bob syncs with no lastSeq → both offline messages returned.
        val syncResp = authPost("/api/message/sync", bobToken, mapOf("lastSeq" to null))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val msgs: JsonNode = om.readTree(syncResp).path("data").path("messages")
        assertEquals(2, msgs.size())
        assertEquals(m1, msgs[0].path("msgId").asText())
        assertEquals(aliceUid, msgs[0].path("fromUid").asLong())
        assertEquals("hello", msgs[0].path("content").asText())

        // Bob confirms → offline messages deleted; subsequent sync is empty.
        authPost("/api/message/confirm", bobToken, mapOf("msgIds" to listOf(m1, m2)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.deleted").value(2))

        val syncResp2 = authPost("/api/message/sync", bobToken, mapOf("lastSeq" to null))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        assertEquals(0, om.readTree(syncResp2).path("data").path("messages").size())
    }

    @Test
    fun `send is idempotent for same msgId`() {
        val u1 = "idem_a_${System.nanoTime()}"
        val u2 = "idem_b_${System.nanoTime()}"
        register(u1); val u2Uid = register(u2)
        val (_, t1) = login(u1)

        val mid = newMsgId()
        val firstResp = authPost("/api/message/send", t1, mapOf("msgId" to mid, "toUid" to u2Uid, "content" to "hi"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val firstSeq = om.readTree(firstResp).path("data").path("seq").asLong()
        assertTrue(firstSeq > 0)

        // Resend with same msgId → same seq, no duplicate stored.
        val secondResp = authPost("/api/message/send", t1, mapOf("msgId" to mid, "toUid" to u2Uid, "content" to "hi"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val secondSeq = om.readTree(secondResp).path("data").path("seq").asLong()
        assertEquals(firstSeq, secondSeq)
    }

    @Test
    fun `conversation list shows latest message and unread count, mark read clears it`() {
        val a = "conv_a_${System.nanoTime()}"
        val b = "conv_b_${System.nanoTime()}"
        val aUid = register(a)
        val bUid = register(b)
        val (_, aToken) = login(a)
        val (_, bToken) = login(b)

        authPost("/api/message/send", aToken, mapOf("msgId" to newMsgId(), "toUid" to bUid, "content" to "first")).andExpect(status().isOk)
        authPost("/api/message/send", aToken, mapOf("msgId" to newMsgId(), "toUid" to bUid, "content" to "second")).andExpect(status().isOk)

        // Bob's conversation list: 1 conversation with peer=a, unreadCount=2.
        val listResp = authGet("/api/conversation/list", bToken)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val items = om.readTree(listResp).path("data")
        assertEquals(1, items.size())
        assertEquals("u", items[0].path("peerType").asText())
        assertEquals(aUid, items[0].path("peerId").asLong())
        assertEquals("second", items[0].path("lastMsgContent").asText())
        assertEquals(2, items[0].path("unreadCount").asInt())

        // Bob opens the conversation → unreadCount reset to 0.
        authPost("/api/conversation/read", bToken, mapOf("peerType" to "u", "peerId" to aUid))
            .andExpect(status().isOk)

        val listResp2 = authGet("/api/conversation/list", bToken)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        assertEquals(0, om.readTree(listResp2).path("data")[0].path("unreadCount").asInt())

        // Sender (a) has its own conversation entry too with unreadCount=0.
        val aListResp = authGet("/api/conversation/list", aToken)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val aItems = om.readTree(aListResp).path("data")
        assertEquals(1, aItems.size())
        assertEquals(0, aItems[0].path("unreadCount").asInt())
        assertEquals(bUid, aItems[0].path("peerId").asLong())
    }

    @Test
    fun `group chat - all members receive the message except the sender`() {
        val owner = "owner_${System.nanoTime()}"
        val m2name = "gm2_${System.nanoTime()}"
        val m3name = "gm3_${System.nanoTime()}"
        val ownerUid = register(owner)
        val m2Uid = register(m2name)
        val m3Uid = register(m3name)
        val (_, ownerToken) = login(owner)
        val (_, m2Token) = login(m2name)
        val (_, m3Token) = login(m3name)

        // Create group + members directly via repositories (no admin endpoint in scope).
        val group = chatGroupRepository.save(ChatGroup(name = "team", ownerUid = ownerUid))
        val gid = group.groupId!!
        listOf(ownerUid, m2Uid, m3Uid).forEach {
            groupMemberRepository.save(GroupMember(id = GroupMemberId(gid, it)))
        }

        // Owner sends a group message.
        val mid = newMsgId()
        authPost("/api/message/send", ownerToken,
            mapOf("msgId" to mid, "groupId" to gid, "content" to "hello team"))
            .andExpect(status().isOk)

        // m2 and m3 each have one offline message; owner has none.
        for (token in listOf(m2Token, m3Token)) {
            val sync = authPost("/api/message/sync", token, mapOf("lastSeq" to null))
                .andExpect(status().isOk).andReturn().response.contentAsString
            val msgs = om.readTree(sync).path("data").path("messages")
            assertEquals(1, msgs.size())
            assertEquals(gid, msgs[0].path("groupId").asLong())
            assertEquals("hello team", msgs[0].path("content").asText())
            assertEquals(ownerUid, msgs[0].path("fromUid").asLong())
        }
        val ownerSync = authPost("/api/message/sync", ownerToken, mapOf("lastSeq" to null))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertEquals(0, om.readTree(ownerSync).path("data").path("messages").size())

        // Both receivers have a 'g' conversation entry with unread=1.
        for ((uid, token) in listOf(m2Uid to m2Token, m3Uid to m3Token)) {
            val list = authGet("/api/conversation/list", token)
                .andExpect(status().isOk).andReturn().response.contentAsString
            val items = om.readTree(list).path("data")
            val groupConv = items.find { it.path("peerType").asText() == "g" && it.path("peerId").asLong() == gid }
            assertNotNull(groupConv, "uid=$uid missing group conversation")
            assertEquals(1, groupConv!!.path("unreadCount").asInt())
        }
    }

    @Test
    fun `unauthenticated request to message endpoints is rejected`() {
        mockMvc.perform(
            post("/api/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"msgId":"x","toUid":1,"content":"hi"}""")
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/conversation/list"))
            .andExpect(status().isUnauthorized)
    }
}
