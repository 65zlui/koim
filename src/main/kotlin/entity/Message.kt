package org.example.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "messages",
    uniqueConstraints = [UniqueConstraint(name = "uk_messages_msg_id", columnNames = ["msg_id"])],
    indexes = [
        Index(name = "idx_messages_single_chat", columnList = "from_uid,to_uid,send_time"),
        Index(name = "idx_messages_group", columnList = "group_id,send_time"),
    ],
)
class Message(
    @Column(name = "msg_id", nullable = false, length = 64)
    var msgId: String,

    @Column(name = "from_uid", nullable = false)
    var fromUid: Long = 0,

    @Column(name = "to_uid")
    var toUid: Long? = null,

    @Column(name = "group_id")
    var groupId: Long? = null,

    @Column(name = "msg_type", nullable = false)
    var msgType: Short = 1,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "send_time", nullable = false)
    var sendTime: Instant = Instant.now(),

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var seq: Long? = null,
)
