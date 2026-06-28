package org.example.controller

import jakarta.validation.Valid
import org.example.dto.ApiResponse
import org.example.dto.ChangePasswordRequest
import org.example.dto.LoginRequest
import org.example.dto.LoginResponse
import org.example.dto.RegisterRequest
import org.example.dto.UpdateProfileRequest
import org.example.dto.UserProfile
import org.example.dto.UserSearchResult
import org.example.security.CurrentUser
import org.example.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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

    @GetMapping("/profile")
    fun profile(): ApiResponse<UserProfile> {
        return ApiResponse.success(userService.getProfile(CurrentUser.uid()))
    }

    @PostMapping("/profile")
    fun updateProfile(@RequestBody @Valid req: UpdateProfileRequest): ApiResponse<UserProfile> {
        return ApiResponse.success(userService.updateProfile(CurrentUser.uid(), req))
    }

    @PostMapping("/password")
    fun changePassword(@RequestBody @Valid req: ChangePasswordRequest): ApiResponse<Unit> {
        userService.changePassword(CurrentUser.uid(), req)
        return ApiResponse.success()
    }

    @GetMapping("/search")
    fun search(@RequestParam q: String): ApiResponse<List<UserSearchResult>> {
        return ApiResponse.success(userService.searchUsers(q, CurrentUser.uid()))
    }
}
