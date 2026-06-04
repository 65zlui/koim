package org.example.controller

import jakarta.validation.Valid
import org.example.dto.ApiResponse
import org.example.dto.ConfirmRequest
import org.example.dto.SendMessageRequest
import org.example.dto.SendMessageResponse
import org.example.dto.SyncRequest
import org.example.dto.SyncResponse
import org.example.security.CurrentUser
import org.example.service.MessageService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/message")
class MessageController(private val messageService: MessageService) {

    @PostMapping("/send")
    fun send(@RequestBody @Valid req: SendMessageRequest): ApiResponse<SendMessageResponse> {
        return ApiResponse.success(messageService.send(CurrentUser.uid(), req))
    }

    @PostMapping("/sync")
    fun sync(@RequestBody req: SyncRequest): ApiResponse<SyncResponse> {
        val list = messageService.sync(CurrentUser.uid(), req.lastSeq)
        return ApiResponse.success(SyncResponse(list))
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody req: ConfirmRequest): ApiResponse<Map<String, Int>> {
        val deleted = messageService.confirm(CurrentUser.uid(), req.msgIds)
        return ApiResponse.success(mapOf("deleted" to deleted))
    }
}
