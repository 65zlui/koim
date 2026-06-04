package org.example.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Embeddable
data class GroupMemberId(
    @Column(name = "group_id") var groupId: Long = 0,
    @Column(name = "uid") var uid: Long = 0,
) : Serializable

@Entity
@Table(name = "group_members")
class GroupMember(
    @EmbeddedId
    var id: GroupMemberId,

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),
)
