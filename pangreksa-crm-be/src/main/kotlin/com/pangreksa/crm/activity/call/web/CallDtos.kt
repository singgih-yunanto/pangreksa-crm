package com.pangreksa.crm.activity.call.web

import com.pangreksa.crm.activity.ActivityLinkResolver
import com.pangreksa.crm.activity.call.domain.Call
import java.time.LocalDateTime

data class CallDto(
    val id: Long,
    val subject: String,
    val callTypeId: Long?, val callType: String?,
    val startAt: LocalDateTime?,
    val durationMinutes: Int?,
    val callPurposeId: Long?, val callPurpose: String?,
    val callResultId: Long?, val callResult: String?,
    val whoType: String?, val whoId: Long?, val whoName: String?,
    val whatType: String?, val whatId: Long?, val whatName: String?,
    val description: String?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class CallRequest(
    val subject: String? = null,
    val callTypeId: Long? = null,
    val startAt: LocalDateTime? = null,
    val durationMinutes: Int? = null,
    val callPurposeId: Long? = null,
    val callResultId: Long? = null,
    val whoType: String? = null,
    val whoId: Long? = null,
    val whatType: String? = null,
    val whatId: Long? = null,
    val description: String? = null,
    val tag: String? = null,
)

fun Call.toDto(links: ActivityLinkResolver) = CallDto(
    id = id!!, subject = subject,
    callTypeId = callType?.id, callType = callType?.label,
    startAt = startAt, durationMinutes = durationMinutes,
    callPurposeId = callPurpose?.id, callPurpose = callPurpose?.label,
    callResultId = callResult?.id, callResult = callResult?.label,
    whoType = whoType, whoId = whoId, whoName = links.label(whoType, whoId),
    whatType = whatType, whatId = whatId, whatName = links.label(whatType, whatId),
    description = description,
    ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
