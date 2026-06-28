package org.example.repository

import org.example.entity.OfflineMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OfflineMessageRepository : JpaRepository<OfflineMessage, Long> {

    @Query(
        "select o from OfflineMessage o where o.uid = :uid and (:lastSeq is null or o.seq > :lastSeq) " +
                "order by o.seq asc"
    )
    fun findByUidAfterSeq(
        @Param("uid") uid: Long,
        @Param("lastSeq") lastSeq: Long?,
    ): List<OfflineMessage>

    @Modifying
    @Query("delete from OfflineMessage o where o.uid = :uid and o.msgId in :msgIds")
    fun deleteByUidAndMsgIds(
        @Param("uid") uid: Long,
        @Param("msgIds") msgIds: Collection<String>,
    ): Int

    @Modifying(clearAutomatically = true)
    @Query("delete from OfflineMessage o where o.uid = :uid and o.groupId = :groupId")
    fun deleteByUidAndGroupId(
        @Param("uid") uid: Long,
        @Param("groupId") groupId: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query("delete from OfflineMessage o where o.groupId = :groupId")
    fun deleteByGroupId(@Param("groupId") groupId: Long)
}
