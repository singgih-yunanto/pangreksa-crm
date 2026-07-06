package com.pangreksa.crm.lead.web

import com.pangreksa.crm.lead.domain.Lead
import java.math.BigDecimal
import java.time.LocalDateTime

data class LeadDto(
    val id: Long,
    val firstName: String?, val lastName: String, val fullName: String,
    val company: String?, val title: String?,
    val salutationId: Long?, val salutation: String?,
    val email: String?, val secondaryEmail: String?,
    val phone: String?, val mobile: String?, val fax: String?, val website: String?,
    val leadSourceId: Long?, val leadSource: String?,
    val leadStatusId: Long?, val leadStatus: String?,
    val industryId: Long?, val industry: String?,
    val ratingId: Long?, val rating: String?,
    val noOfEmployees: Int?, val annualRevenue: BigDecimal?, val emailOptOut: Boolean,
    val skypeId: String?, val twitter: String?,
    val street: String?, val city: String?, val state: String?, val country: String?, val zipCode: String?,
    val description: String?,
    val converted: Boolean,
    val convertedAccountId: Long?, val convertedContactId: Long?, val convertedDealId: Long?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class LeadRequest(
    val firstName: String? = null, val lastName: String? = null, val company: String? = null, val title: String? = null,
    val salutationId: Long? = null,
    val email: String? = null, val secondaryEmail: String? = null,
    val phone: String? = null, val mobile: String? = null, val fax: String? = null, val website: String? = null,
    val leadSourceId: Long? = null, val leadStatusId: Long? = null, val industryId: Long? = null, val ratingId: Long? = null,
    val noOfEmployees: Int? = null, val annualRevenue: BigDecimal? = null, val emailOptOut: Boolean? = null,
    val skypeId: String? = null, val twitter: String? = null,
    val street: String? = null, val city: String? = null, val state: String? = null, val country: String? = null, val zipCode: String? = null,
    val description: String? = null, val tag: String? = null,
)

/** Lead conversion options. If [accountId]/[contactId] are given, link to those; otherwise create new. */
data class LeadConvertRequest(
    val accountId: Long? = null,
    val contactId: Long? = null,
    val createDeal: Boolean? = null,
    val dealName: String? = null,
    val dealStageId: Long? = null,
    val dealAmount: BigDecimal? = null,
    val dealClosingDate: java.time.LocalDate? = null,
)

data class LeadConversionResult(
    val leadId: Long,
    val accountId: Long,
    val contactId: Long,
    val dealId: Long?,
)

fun Lead.toDto() = LeadDto(
    id = id!!, firstName = firstName, lastName = lastName,
    fullName = listOfNotNull(firstName, lastName).joinToString(" ").trim(),
    company = company, title = title, salutationId = salutation?.id, salutation = salutation?.label,
    email = email, secondaryEmail = secondaryEmail, phone = phone, mobile = mobile, fax = fax, website = website,
    leadSourceId = leadSource?.id, leadSource = leadSource?.label,
    leadStatusId = leadStatus?.id, leadStatus = leadStatus?.label,
    industryId = industry?.id, industry = industry?.label,
    ratingId = rating?.id, rating = rating?.label,
    noOfEmployees = noOfEmployees, annualRevenue = annualRevenue, emailOptOut = emailOptOut,
    skypeId = skypeId, twitter = twitter, street = street, city = city, state = state, country = country, zipCode = zipCode,
    description = description,
    converted = converted,
    convertedAccountId = convertedAccount?.id, convertedContactId = convertedContact?.id, convertedDealId = convertedDeal?.id,
    ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
