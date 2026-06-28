package org.example.repository

import org.example.entity.Conversation
import org.example.entity.ConversationId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ConversationRepository : JpaRepository<Conversation, ConversationId> {
    fun findByIdUidOrderByLastMsgTimeDesc(uid: Long): List<Conversation>

    @Modifying(clearAutomatically = true)
    @Query("delete from Conversation c where c.id.uid = :uid and c.id.peerType = :peerType and c.id.peerId = :peerId")
    fun deleteByUserAndPeer(
        @Param("uid") uid: Long,
        @Param("peerType") peerType: String,
        @Param("peerId") peerId: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("delete from Conversation c where c.id.peerType = :peerType and c.id.peerId = :peerId")
    fun deleteAllByPeerTypeAndPeerId(
        @Param("peerType") peerType: String,
        @Param("peerId") peerId: Long,
    )
}
