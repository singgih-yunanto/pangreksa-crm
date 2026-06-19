package com.pangreksa.crm.deal.web

import com.pangreksa.crm.deal.domain.Deal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DealDto(
    val id: Long,
    val name: String,
    val stageId: Long?, val stage: String?, val stageExtra: Map<String, Any?>?,
    val amount: BigDecimal?,
    val closingDate: LocalDate?,
    val accountId: Long?, val accountName: String?,
    val contactId: Long?, val contactName: String?,
    val typeId: Long?, val type: String?,
    val probability: Int?,
    val expectedRevenue: BigDecimal?,
    val nextStep: String?,
    val leadSourceId: Long?, val leadSource: String?,
    val reasonForLossId: Long?, val reasonForLoss: String?,
    val description: String?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class DealRequest(
    val name: String? = null,
    val stageId: Long? = null,
    val amount: BigDecimal? = null,
    val closingDate: LocalDate? = null,
    val accountId: Long? = null,
    val contactId: Long? = null,
    val typeId: Long? = null,
    val nextStep: String? = null,
    val leadSourceId: Long? = null,
    val reasonForLossId: Long? = null,
    val description: String? = null,
    val tag: String? = null,
)

fun Deal.toDto() = DealDto(
    id = id!!, name = name,
    stageId = stage?.id, stage = stage?.label, stageExtra = stage?.extra,
    amount = amount, closingDate = closingDate,
    accountId = account?.id, accountName = account?.name,
    contactId = contact?.id,
    contactName = contact?.let { listOfNotNull(it.firstName, it.lastName).joinToString(" ").trim() },
    typeId = type?.id, type = type?.label,
    probability = probability, expectedRevenue = expectedRevenue, nextStep = nextStep,
    leadSourceId = leadSource?.id, leadSource = leadSource?.label,
    reasonForLossId = reasonForLoss?.id, reasonForLoss = reasonForLoss?.label,
    description = description, ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
