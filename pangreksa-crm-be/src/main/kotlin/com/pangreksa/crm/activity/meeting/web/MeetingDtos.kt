package com.pangreksa.crm.activity.meeting.web

import com.pangreksa.crm.activity.ActivityLinkResolver
import com.pangreksa.crm.activity.meeting.domain.Meeting
import java.time.LocalDateTime

data class MeetingDto(
    val id: Long,
    val title: String,
    val statusId: Long?, val status: String?,
    val location: String?,
    val startAt: LocalDateTime?, val endAt: LocalDateTime?, val allDay: Boolean,
    val whoType: String?, val whoId: Long?, val whoName: String?,
    val whatType: String?, val whatId: Long?, val whatName: String?,
    val description: String?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class MeetingRequest(
    val title: String? = null,
    val statusId: Long? = null,
    val location: String? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val allDay: Boolean? = null,
    val whoType: String? = null,
    val whoId: Long? = null,
    val whatType: String? = null,
    val whatId: Long? = null,
    val description: String? = null,
    val tag: String? = null,
)

fun Meeting.toDto(links: ActivityLinkResolver) = MeetingDto(
    id = id!!, title = title,
    statusId = status?.id, status = status?.label,
    location = location,
    startAt = startAt, endAt = endAt, allDay = allDay,
    whoType = whoType, whoId = whoId, whoName = links.label(whoType, whoId),
    whatType = whatType, whatId = whatId, whatName = links.label(whatType, whatId),
    description = description,
    ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
