package org.example.service

import org.example.dto.CreateGroupRequest
import org.example.dto.CreateGroupResponse
import org.example.dto.GroupInfo
import org.example.dto.GroupMemberInfo
import org.example.dto.UpdateGroupRequest
import org.example.entity.ChatGroup
import org.example.entity.GroupMember
import org.example.entity.GroupMemberId
import org.example.repository.ChatGroupRepository
import org.example.repository.ConversationRepository
import org.example.repository.GroupMemberRepository
import org.example.repository.OfflineMessageRepository
import org.example.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class GroupService(
    private val chatGroupRepository: ChatGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val offlineMessageRepository: OfflineMessageRepository,
) {

    @Transactional
    fun create(ownerUid: Long, req: CreateGroupRequest): CreateGroupResponse {
        val group = chatGroupRepository.save(ChatGroup(name = req.name, ownerUid = ownerUid))
        val gid = group.groupId!!
        val uids = (req.memberUids + ownerUid).toSet()
        uids.forEach {
            groupMemberRepository.save(GroupMember(id = GroupMemberId(gid, it)))
        }
        return CreateGroupResponse(gid, group.name)
    }

    @Transactional
    fun join(uid: Long, groupId: Long): GroupInfo {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        if (!groupMemberRepository.existsByIdGroupIdAndIdUid(groupId, uid)) {
            groupMemberRepository.save(GroupMember(id = GroupMemberId(groupId, uid)))
        }
        return info(groupId)
    }

    @Transactional(readOnly = true)
    fun info(groupId: Long): GroupInfo {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        val count = groupMemberRepository.findUidsByGroupId(groupId).size
        return GroupInfo(group.groupId!!, group.name, group.ownerUid, count)
    }

    @Transactional(readOnly = true)
    fun members(groupId: Long): List<GroupMemberInfo> {
        chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        return groupMemberRepository.findAllByIdGroupId(groupId).map { gm ->
            val nickname = userRepository.findById(gm.id.uid)
                .map { it.nickname ?: it.username }
                .orElse(null)
            GroupMemberInfo(uid = gm.id.uid, nickname = nickname, joinedAt = gm.joinedAt)
        }
    }

    @Transactional
    fun updateGroup(ownerUid: Long, req: UpdateGroupRequest): GroupInfo {
        val group = chatGroupRepository.findById(req.groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        if (group.ownerUid != ownerUid) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can update group")
        }
        group.name = req.name
        chatGroupRepository.save(group)
        return info(req.groupId)
    }

    @Transactional
    fun leave(uid: Long, groupId: Long) {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        if (!groupMemberRepository.existsByIdGroupIdAndIdUid(groupId, uid)) {
            throw IllegalArgumentException("user is not a member")
        }
        val memberCount = groupMemberRepository.countByIdGroupId(groupId)
        if (group.ownerUid == uid) {
            if (memberCount > 1) {
                throw IllegalArgumentException("transfer ownership before leaving")
            }
            // Sole member + owner: dissolve group.
            groupMemberRepository.deleteAllByGroupId(groupId)
            conversationRepository.deleteAllByPeerTypeAndPeerId("g", groupId)
            offlineMessageRepository.deleteByGroupId(groupId)
            chatGroupRepository.delete(group)
        } else {
            groupMemberRepository.deleteByGroupIdAndUid(groupId, uid)
            conversationRepository.deleteByUserAndPeer(uid, "g", groupId)
            offlineMessageRepository.deleteByUidAndGroupId(uid, groupId)
        }
    }

    @Transactional
    fun kick(ownerUid: Long, groupId: Long, targetUid: Long) {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        if (group.ownerUid != ownerUid) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can kick")
        }
        if (targetUid == ownerUid) {
            throw IllegalArgumentException("cannot kick yourself, use leave")
        }
        if (!groupMemberRepository.existsByIdGroupIdAndIdUid(groupId, targetUid)) {
            throw IllegalArgumentException("user is not a member")
        }
        groupMemberRepository.deleteByGroupIdAndUid(groupId, targetUid)
        conversationRepository.deleteByUserAndPeer(targetUid, "g", groupId)
        offlineMessageRepository.deleteByUidAndGroupId(targetUid, groupId)
    }
}
