package com.pangreksa.crm.account.web

import com.pangreksa.crm.account.domain.Account
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountDto(
    val id: Long,
    val name: String,
    val accountNumber: Long?,
    val accountSite: String?,
    val phone: String?,
    val fax: String?,
    val website: String?,
    val tickerSymbol: String?,
    val accountTypeId: Long?, val accountType: String?,
    val ownershipId: Long?, val ownership: String?,
    val industryId: Long?, val industry: String?,
    val ratingId: Long?, val rating: String?,
    val employees: Int?,
    val annualRevenue: BigDecimal?,
    val sicCode: Int?,
    val parentAccountId: Long?, val parentAccountName: String?,
    val billingStreet: String?, val billingCity: String?, val billingState: String?,
    val billingCountry: String?, val billingCode: String?,
    val shippingStreet: String?, val shippingCity: String?, val shippingState: String?,
    val shippingCountry: String?, val shippingCode: String?,
    val description: String?,
    val ownerId: Long?, val ownerName: String?,
    val tag: String?,
    val createdAt: LocalDateTime, val updatedAt: LocalDateTime,
)

data class AccountRequest(
    val name: String? = null,
    val accountNumber: Long? = null,
    val accountSite: String? = null,
    val phone: String? = null,
    val fax: String? = null,
    val website: String? = null,
    val tickerSymbol: String? = null,
    val accountTypeId: Long? = null,
    val ownershipId: Long? = null,
    val industryId: Long? = null,
    val ratingId: Long? = null,
    val employees: Int? = null,
    val annualRevenue: BigDecimal? = null,
    val sicCode: Int? = null,
    val parentAccountId: Long? = null,
    val billingStreet: String? = null, val billingCity: String? = null, val billingState: String? = null,
    val billingCountry: String? = null, val billingCode: String? = null,
    val shippingStreet: String? = null, val shippingCity: String? = null, val shippingState: String? = null,
    val shippingCountry: String? = null, val shippingCode: String? = null,
    val description: String? = null,
    val tag: String? = null,
)

fun Account.toDto() = AccountDto(
    id = id!!, name = name, accountNumber = accountNumber, accountSite = accountSite, phone = phone,
    fax = fax, website = website, tickerSymbol = tickerSymbol,
    accountTypeId = accountType?.id, accountType = accountType?.label,
    ownershipId = ownership?.id, ownership = ownership?.label,
    industryId = industry?.id, industry = industry?.label,
    ratingId = rating?.id, rating = rating?.label,
    employees = employees, annualRevenue = annualRevenue, sicCode = sicCode,
    parentAccountId = parentAccount?.id, parentAccountName = parentAccount?.name,
    billingStreet = billingStreet, billingCity = billingCity, billingState = billingState,
    billingCountry = billingCountry, billingCode = billingCode,
    shippingStreet = shippingStreet, shippingCity = shippingCity, shippingState = shippingState,
    shippingCountry = shippingCountry, shippingCode = shippingCode,
    description = description, ownerId = owner?.id, ownerName = owner?.fullName, tag = tag,
    createdAt = createdAt, updatedAt = updatedAt,
)
