package com.pangreksa.crm.activity.task.web

import com.pangreksa.crm.activity.ActivityLinkResolver
import com.pangreksa.crm.activity.task.domain.Task
import java.time.LocalDate
import java.time.LocalDateTime

data class TaskDto(
    val id: Long,
    val subject: String,
    val statusId: Long?, val status: String?,
    val priorityId: Long?, val priority: String?,
    val dueDate: LocalDate?,
    val whoType: String?, val whoId: Long?, val whoName: String?,
    val whatType: String?, val whatId: Long?, val whatName: String?,
    val description: String?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class TaskRequest(
    val subject: String? = null,
    val statusId: Long? = null,
    val priorityId: Long? = null,
    val dueDate: LocalDate? = null,
    val whoType: String? = null,
    val whoId: Long? = null,
    val whatType: String? = null,
    val whatId: Long? = null,
    val description: String? = null,
    val tag: String? = null,
)

fun Task.toDto(links: ActivityLinkResolver) = TaskDto(
    id = id!!, subject = subject,
    statusId = status?.id, status = status?.label,
    priorityId = priority?.id, priority = priority?.label,
    dueDate = dueDate,
    whoType = whoType, whoId = whoId, whoName = links.label(whoType, whoId),
    whatType = whatType, whatId = whatId, whatName = links.label(whatType, whatId),
    description = description,
    ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
