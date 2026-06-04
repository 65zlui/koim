package org.example.service

import org.example.dto.CreateGroupRequest
import org.example.dto.CreateGroupResponse
import org.example.dto.GroupInfo
import org.example.entity.ChatGroup
import org.example.entity.GroupMember
import org.example.entity.GroupMemberId
import org.example.repository.ChatGroupRepository
import org.example.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val chatGroupRepository: ChatGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional
    fun create(ownerUid: Long, req: CreateGroupRequest): CreateGroupResponse {
        val group = chatGroupRepository.save(ChatGroup(name = req.name, ownerUid = ownerUid))
        val gid = group.groupId!!
        val uids = (req.memberUids + ownerUid).toSet()
        uids.forEach {
            groupMemberRepository.save(GroupMember(id = GroupMemberId(gid, it)))
        }
        return CreateGroupResponse(gid, group.name)
    }

    @Transactional
    fun join(uid: Long, groupId: Long): GroupInfo {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        if (!groupMemberRepository.existsByIdGroupIdAndIdUid(groupId, uid)) {
            groupMemberRepository.save(GroupMember(id = GroupMemberId(groupId, uid)))
        }
        return info(groupId)
    }

    @Transactional(readOnly = true)
    fun info(groupId: Long): GroupInfo {
        val group = chatGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("group not found") }
        val count = groupMemberRepository.findUidsByGroupId(groupId).size
        return GroupInfo(group.groupId!!, group.name, group.ownerUid, count)
    }
}
