package com.pangreksa.crm.account.service

import com.pangreksa.crm.account.domain.Account
import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.account.web.AccountRequest
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountService(
    private val repository: AccountRepository,
    private val access: AccessService,
    private val lookups: LookupService,
) {
    private val sortable = setOf("name", "annualRevenue", "createdAt", "updatedAt")

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        accountTypeId: Long? = null, industryId: Long? = null, ownershipId: Long? = null,
        ratingId: Long? = null, ownerId: Long? = null, employeesMin: Int? = null, employeesMax: Int? = null,
    ): Page<Account> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "name")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            accountTypeId, industryId, ownershipId, ratingId, ownerId, employeesMin, employeesMax, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Account {
        val a = repository.findDetailById(id) ?: throw NotFoundException("Account not found: $id")
        if (!access.canAccess(a.owner?.id)) throw NotFoundException("Account not found: $id")
        return a
    }

    @Transactional
    fun create(req: AccountRequest): Account {
        val name = req.name?.trim().orEmpty()
        if (name.isEmpty()) throw ValidationException("name", "Account name is required")
        val a = Account(name = name)
        a.owner = access.currentUser()
        apply(a, req)
        return repository.save(a)
    }

    @Transactional
    fun update(id: Long, req: AccountRequest): Account {
        val a = get(id)
        req.name?.let { if (it.isBlank()) throw ValidationException("name", "Account name is required"); a.name = it.trim() }
        apply(a, req)
        return repository.save(a)
    }

    @Transactional
    fun delete(id: Long) { get(id); repository.deleteById(id) }

    private fun apply(a: Account, req: AccountRequest) {
        req.accountNumber?.let { a.accountNumber = it }
        req.accountSite?.let { a.accountSite = it.trim() }
        req.phone?.let { a.phone = it.trim() }
        req.fax?.let { a.fax = it.trim() }
        req.website?.let { a.website = it.trim() }
        req.tickerSymbol?.let { a.tickerSymbol = it.trim() }
        req.accountTypeId?.let { a.accountType = lookups.resolveById("account_type", it) }
        req.ownershipId?.let { a.ownership = lookups.resolveById("ownership", it) }
        req.industryId?.let { a.industry = lookups.resolveById("industry", it) }
        req.ratingId?.let { a.rating = lookups.resolveById("rating", it) }
        req.employees?.let { a.employees = it }
        req.annualRevenue?.let { a.annualRevenue = it }
        req.sicCode?.let { a.sicCode = it }
        req.parentAccountId?.let { a.parentAccount = repository.findById(it).orElseThrow { ValidationException("parentAccountId", "Account not found: $it") } }
        req.billingStreet?.let { a.billingStreet = it }; req.billingCity?.let { a.billingCity = it }
        req.billingState?.let { a.billingState = it }; req.billingCountry?.let { a.billingCountry = it }
        req.billingCode?.let { a.billingCode = it }
        req.shippingStreet?.let { a.shippingStreet = it }; req.shippingCity?.let { a.shippingCity = it }
        req.shippingState?.let { a.shippingState = it }; req.shippingCountry?.let { a.shippingCountry = it }
        req.shippingCode?.let { a.shippingCode = it }
        req.description?.let { a.description = it }
        req.tag?.let { a.tag = it }
    }
}
