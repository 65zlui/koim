package org.example.repository

import org.example.entity.GroupMember
import org.example.entity.GroupMemberId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupMemberRepository : JpaRepository<GroupMember, GroupMemberId> {

    @Query("select gm.id.uid from GroupMember gm where gm.id.groupId = :groupId")
    fun findUidsByGroupId(groupId: Long): List<Long>

    fun existsByIdGroupIdAndIdUid(groupId: Long, uid: Long): Boolean

    fun findAllByIdGroupId(groupId: Long): List<GroupMember>

    fun countByIdGroupId(groupId: Long): Long

    @Modifying(clearAutomatically = true)
    @Query("delete from GroupMember gm where gm.id.groupId = :groupId and gm.id.uid = :uid")
    fun deleteByGroupIdAndUid(
        @Param("groupId") groupId: Long,
        @Param("uid") uid: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("delete from GroupMember gm where gm.id.groupId = :groupId")
    fun deleteAllByGroupId(@Param("groupId") groupId: Long)
}
