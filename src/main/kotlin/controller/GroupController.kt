package org.example.controller

import jakarta.validation.Valid
import org.example.dto.ApiResponse
import org.example.dto.CreateGroupRequest
import org.example.dto.CreateGroupResponse
import org.example.dto.GroupInfo
import org.example.dto.JoinGroupRequest
import org.example.security.CurrentUser
import org.example.service.GroupService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/group")
class GroupController(private val groupService: GroupService) {

    @PostMapping("/create")
    fun create(@RequestBody @Valid req: CreateGroupRequest): ApiResponse<CreateGroupResponse> {
        return ApiResponse.success(groupService.create(CurrentUser.uid(), req))
    }

    @PostMapping("/join")
    fun join(@RequestBody req: JoinGroupRequest): ApiResponse<GroupInfo> {
        return ApiResponse.success(groupService.join(CurrentUser.uid(), req.groupId))
    }

    @GetMapping("/{groupId}")
    fun info(@PathVariable groupId: Long): ApiResponse<GroupInfo> {
        return ApiResponse.success(groupService.info(groupId))
    }
}
