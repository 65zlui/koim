package org.example.service

import org.example.dto.LoginRequest
import org.example.dto.LoginResponse
import org.example.dto.RegisterRequest
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
}
