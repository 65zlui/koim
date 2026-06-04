package org.example.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true, length = 32)
    var username: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(length = 50)
    var nickname: String? = null,

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    var avatarUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var uid: Long? = null,
)
