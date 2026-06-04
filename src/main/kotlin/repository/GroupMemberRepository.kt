package org.example.repository

import org.example.entity.GroupMember
import org.example.entity.GroupMemberId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupMemberRepository : JpaRepository<GroupMember, GroupMemberId> {

    @Query("select gm.id.uid from GroupMember gm where gm.id.groupId = :groupId")
    fun findUidsByGroupId(groupId: Long): List<Long>

    fun existsByIdGroupIdAndIdUid(groupId: Long, uid: Long): Boolean
}
