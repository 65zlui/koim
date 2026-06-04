package org.example.controller

import jakarta.validation.Valid
import org.example.dto.ApiResponse
import org.example.dto.ConversationDto
import org.example.dto.ReadRequest
import org.example.security.CurrentUser
import org.example.service.ConversationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/conversation")
class ConversationController(private val conversationService: ConversationService) {

    @GetMapping("/list")
    fun list(): ApiResponse<List<ConversationDto>> {
        return ApiResponse.success(conversationService.list(CurrentUser.uid()))
    }

    @PostMapping("/read")
    fun read(@RequestBody @Valid req: ReadRequest): ApiResponse<Unit> {
        conversationService.markRead(CurrentUser.uid(), req.peerType, req.peerId)
        return ApiResponse.success()
    }
}
