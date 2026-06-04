package org.example.controller

import jakarta.validation.Valid
import org.example.dto.ApiResponse
import org.example.dto.LoginRequest
import org.example.dto.LoginResponse
import org.example.dto.RegisterRequest
import org.example.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid req: RegisterRequest): ApiResponse<Map<String, Long>> {
        val uid = userService.register(req)
        return ApiResponse.success(mapOf("uid" to uid))
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid req: LoginRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(userService.login(req))
    }
}
