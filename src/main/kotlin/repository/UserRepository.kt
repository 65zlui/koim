package org.example.repository

import org.example.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean

    @Query(
        "select u from User u where " +
        "lower(u.username) like lower(concat('%', :q, '%')) or " +
        "lower(u.nickname) like lower(concat('%', :q, '%'))"
    )
    fun searchByKeyword(@Param("q") q: String): List<User>
}
