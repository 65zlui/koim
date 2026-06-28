package org.example.service

import org.example.dto.ChangePasswordRequest
import org.example.dto.LoginRequest
import org.example.dto.LoginResponse
import org.example.dto.RegisterRequest
import org.example.dto.UpdateProfileRequest
import org.example.dto.UserProfile
import org.example.dto.UserSearchResult
import org.example.entity.User
import org.example.repository.UserRepository
import org.example.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${koim.ws.path}") private val wsPath: String,
) {

    @Transactional
    fun register(req: RegisterRequest): Long {
        require(!userRepository.existsByUsername(req.username)) { "username already exists" }
        val user = User(
            username = req.username,
            passwordHash = passwordEncoder.encode(req.password),
            nickname = req.nickname,
        )
        return userRepository.save(user).uid!!
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(req.username)
            .orElseThrow { IllegalArgumentException("invalid username or password") }
        require(passwordEncoder.matches(req.password, user.passwordHash)) {
            "invalid username or password"
        }
        val token = jwtService.generate(user.uid!!)
        return LoginResponse(uid = user.uid!!, token = token, wsUrl = "$wsPath?token=$token")
    }

    fun findById(uid: Long): User? = userRepository.findById(uid).orElse(null)

    @Transactional(readOnly = true)
    fun getProfile(uid: Long): UserProfile {
        val user = userRepository.findById(uid)
            .orElseThrow { IllegalArgumentException("user not found") }
        return user.toProfile()
    }

    @Transactional
    fun updateProfile(uid: Long, req: UpdateProfileRequest): UserProfile {
        val user = userRepository.findById(uid)
            .orElseThrow { IllegalArgumentException("user not found") }
        if (req.nickname != null) user.nickname = req.nickname
        if (req.avatarUrl != null) user.avatarUrl = req.avatarUrl
        if (req.status != null) user.status = req.status
        userRepository.save(user)
        return user.toProfile()
    }

    @Transactional
    fun changePassword(uid: Long, req: ChangePasswordRequest) {
        val user = userRepository.findById(uid)
            .orElseThrow { IllegalArgumentException("user not found") }
        require(passwordEncoder.matches(req.oldPassword, user.passwordHash)) {
            "incorrect current password"
        }
        user.passwordHash = passwordEncoder.encode(req.newPassword)
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun searchUsers(query: String, selfUid: Long): List<UserSearchResult> {
        if (query.isBlank() || query.trim().length < 1) return emptyList()
        return userRepository.searchByKeyword(query.trim())
            .filter { it.uid != selfUid }
            .take(20)
            .map { it.toSearchResult() }
    }

    private fun User.toProfile() = UserProfile(
        uid = uid!!,
        username = username,
        nickname = nickname,
        avatarUrl = avatarUrl,
        status = status,
        createdAt = createdAt,
    )

    private fun User.toSearchResult() = UserSearchResult(
        uid = uid!!,
        username = username,
        nickname = nickname,
        avatarUrl = avatarUrl,
        status = status,
    )
}
