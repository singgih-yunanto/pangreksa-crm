package com.pangreksa.crm.contact.web

import com.pangreksa.crm.contact.domain.Contact
import java.time.LocalDate
import java.time.LocalDateTime

data class ContactDto(
    val id: Long,
    val firstName: String?, val lastName: String, val fullName: String,
    val accountId: Long?, val accountName: String?,
    val title: String?, val department: String?,
    val email: String?, val secondaryEmail: String?,
    val phone: String?, val mobile: String?, val homePhone: String?, val otherPhone: String?, val fax: String?,
    val dateOfBirth: LocalDate?, val assistant: String?,
    val leadSourceId: Long?, val leadSource: String?,
    val reportingToId: Long?, val reportingToName: String?,
    val mailingStreet: String?, val mailingCity: String?, val mailingState: String?, val mailingCountry: String?, val mailingCode: String?,
    val otherStreet: String?, val otherCity: String?, val otherState: String?, val otherCountry: String?, val otherCode: String?,
    val emailOptOut: Boolean, val description: String?,
    val ownerId: Long?, val ownerName: String?, val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class ContactRequest(
    val firstName: String? = null, val lastName: String? = null, val accountId: Long? = null,
    val title: String? = null, val department: String? = null,
    val email: String? = null, val secondaryEmail: String? = null,
    val phone: String? = null, val mobile: String? = null, val homePhone: String? = null, val otherPhone: String? = null, val fax: String? = null,
    val dateOfBirth: LocalDate? = null, val assistant: String? = null,
    val leadSourceId: Long? = null, val reportingToId: Long? = null,
    val mailingStreet: String? = null, val mailingCity: String? = null, val mailingState: String? = null, val mailingCountry: String? = null, val mailingCode: String? = null,
    val otherStreet: String? = null, val otherCity: String? = null, val otherState: String? = null, val otherCountry: String? = null, val otherCode: String? = null,
    val emailOptOut: Boolean? = null, val description: String? = null, val tag: String? = null,
)

fun Contact.toDto() = ContactDto(
    id = id!!, firstName = firstName, lastName = lastName,
    fullName = listOfNotNull(firstName, lastName).joinToString(" ").trim(),
    accountId = account?.id, accountName = account?.name, title = title, department = department,
    email = email, secondaryEmail = secondaryEmail, phone = phone, mobile = mobile,
    homePhone = homePhone, otherPhone = otherPhone, fax = fax, dateOfBirth = dateOfBirth,
    assistant = assistant, leadSourceId = leadSource?.id, leadSource = leadSource?.label,
    reportingToId = reportingTo?.id,
    reportingToName = reportingTo?.let { listOfNotNull(it.firstName, it.lastName).joinToString(" ").trim() },
    mailingStreet = mailingStreet, mailingCity = mailingCity, mailingState = mailingState, mailingCountry = mailingCountry, mailingCode = mailingCode,
    otherStreet = otherStreet, otherCity = otherCity, otherState = otherState, otherCountry = otherCountry, otherCode = otherCode,
    emailOptOut = emailOptOut, description = description, ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
