package org.example.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Embeddable
data class ConversationId(
    @Column(name = "uid") var uid: Long = 0,
    @Column(name = "peer_type", length = 1) var peerType: String = "u",
    @Column(name = "peer_id") var peerId: Long = 0,
) : Serializable

@Entity
@Table(name = "conversations")
class Conversation(
    @EmbeddedId
    var id: ConversationId,

    @Column(name = "last_msg_content", columnDefinition = "TEXT")
    var lastMsgContent: String? = null,

    @Column(name = "last_msg_time")
    var lastMsgTime: Instant? = null,

    @Column(name = "unread_count", nullable = false)
    var unreadCount: Int = 0,
)
