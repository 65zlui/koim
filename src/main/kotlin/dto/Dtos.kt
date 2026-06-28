package org.example.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 32) val username: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val password: String,
    @field:Size(max = 50) val nickname: String? = null,
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
)

data class LoginResponse(
    val uid: Long,
    val token: String,
    val wsUrl: String,
)

data class SendMessageRequest(
    @field:NotBlank @field:Size(max = 64) val msgId: String,
    val toUid: Long? = null,
    val groupId: Long? = null,
    val type: Short = 1,
    @field:Size(max = 4096) val content: String? = null,
)

data class SendMessageResponse(
    val msgId: String,
    val seq: Long,
    val sendTime: Instant,
)

data class SyncRequest(val lastSeq: Long? = null)

data class MessageDto(
    val msgId: String,
    val seq: Long?,
    val fromUid: Long?,
    val toUid: Long?,
    val groupId: Long?,
    val msgType: Short?,
    val content: String?,
    val sendTime: Instant?,
)

data class SyncResponse(val messages: List<MessageDto>)

data class ConfirmRequest(val msgIds: List<String>)

data class ConversationDto(
    val peerType: String,
    val peerId: Long,
    val peerName: String?,
    val lastMsgContent: String?,
    val lastMsgTime: Instant?,
    val unreadCount: Int,
)

data class ReadRequest(
    @field:NotBlank val peerType: String,
    val peerId: Long,
)

data class ApiResponse<T>(val ok: Boolean, val data: T? = null, val error: String? = null) {
    companion object {
        fun <T> success(data: T? = null) = ApiResponse(true, data, null)
        fun <T> failure(error: String) = ApiResponse<T>(false, null, error)
    }
}

data class CreateGroupRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    val memberUids: List<Long> = emptyList(),
)

data class CreateGroupResponse(val groupId: Long, val name: String)

data class JoinGroupRequest(val groupId: Long)

data class GroupInfo(
    val groupId: Long,
    val name: String,
    val ownerUid: Long,
    val memberCount: Int,
)

data class UpdateGroupRequest(
    val groupId: Long,
    @field:NotBlank @field:Size(max = 100) val name: String,
)

data class LeaveGroupRequest(val groupId: Long)

data class KickMemberRequest(val groupId: Long, val targetUid: Long)

data class GroupMemberInfo(
    val uid: Long,
    val nickname: String?,
    val joinedAt: Instant,
)

data class UserProfile(
    val uid: Long,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?,
    val status: String?,
    val createdAt: Instant,
)

data class UpdateProfileRequest(
    @field:Size(max = 50) val nickname: String? = null,
    val avatarUrl: String? = null,
    @field:Size(max = 100) val status: String? = null,
)

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val newPassword: String,
)

data class UserSearchResult(
    val uid: Long,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?,
    val status: String?,
)
