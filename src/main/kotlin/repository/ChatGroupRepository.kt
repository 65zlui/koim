package org.example.repository

import org.example.entity.ChatGroup
import org.springframework.data.jpa.repository.JpaRepository

interface ChatGroupRepository : JpaRepository<ChatGroup, Long>
