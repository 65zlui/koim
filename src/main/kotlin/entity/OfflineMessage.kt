package org.example.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "offline_messages",
    indexes = [Index(name = "idx_offline_uid", columnList = "uid,seq")],
)
class OfflineMessage(
    @Column(nullable = false)
    var uid: Long,

    @Column(name = "msg_id", nullable = false, length = 64)
    var msgId: String,

    @Column(name = "from_uid")
    var fromUid: Long? = null,

    @Column(name = "group_id")
    var groupId: Long? = null,

    @Column(name = "msg_type")
    var msgType: Short? = null,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "send_time")
    var sendTime: Instant? = null,

    @Column
    var seq: Long? = null,

    @Column(nullable = false)
    var delivered: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
