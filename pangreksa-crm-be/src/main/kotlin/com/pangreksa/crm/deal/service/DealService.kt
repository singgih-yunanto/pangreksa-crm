package com.pangreksa.crm.deal.service

import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.audit.AuditService
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.contact.domain.ContactRepository
import com.pangreksa.crm.deal.domain.Deal
import com.pangreksa.crm.deal.domain.DealRepository
import com.pangreksa.crm.deal.web.DealRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class DealService(
    private val repository: DealRepository,
    private val accountRepository: AccountRepository,
    private val contactRepository: ContactRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
) {
    private val sortable = setOf("name", "amount", "closingDate", "createdAt", "updatedAt")

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        stageId: Long? = null, typeId: Long? = null, leadSourceId: Long? = null, ownerId: Long? = null,
        amountMin: BigDecimal? = null, amountMax: BigDecimal? = null,
        closingFrom: LocalDate? = null, closingTo: LocalDate? = null,
    ): Page<Deal> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "name")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            stageId, typeId, leadSourceId, ownerId, amountMin, amountMax, closingFrom, closingTo, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Deal {
        val d = repository.findDetailById(id) ?: throw NotFoundException("Deal not found: $id")
        if (!access.canAccess(d.owner?.id)) throw NotFoundException("Deal not found: $id")
        return d
    }

    @Transactional
    fun create(req: DealRequest): Deal {
        val name = req.name?.trim().orEmpty()
        if (name.isEmpty()) throw ValidationException("name", "Deal name is required")
        if (req.stageId == null) throw ValidationException("stageId", "Stage is required")
        val d = Deal(name = name)
        d.owner = access.currentUser()
        d.stage = lookups.resolveById("deal_stage", req.stageId)
        apply(d, req)
        recompute(d)
        val saved = repository.save(d)
        audit.record("deals", saved.id!!, "created", mapOf("name" to saved.name))
        return saved
    }

    @Transactional
    fun update(id: Long, req: DealRequest): Deal {
        val d = get(id)
        req.name?.let { if (it.isBlank()) throw ValidationException("name", "Deal name is required"); d.name = it.trim() }
        req.stageId?.let { d.stage = lookups.resolveById("deal_stage", it) }
        apply(d, req)
        recompute(d)
        val saved = repository.save(d)
        audit.record("deals", saved.id!!, "updated", mapOf("name" to saved.name))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val d = get(id)
        repository.deleteById(id)
        audit.record("deals", id, "deleted", mapOf("name" to d.name))
    }

    /** Probability comes from the stage lookup's `extra.probability`; expected = amount × prob / 100. */
    private fun recompute(d: Deal) {
        val prob = (d.stage?.extra?.get("probability") as? Number)?.toInt() ?: 0
        d.probability = prob
        d.expectedRevenue = d.amount?.multiply(BigDecimal(prob))?.divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    }

    private fun apply(d: Deal, req: DealRequest) {
        req.amount?.let { d.amount = it }
        req.closingDate?.let { d.closingDate = it }
        req.accountId?.let { d.account = accountRepository.findById(it).orElseThrow { ValidationException("accountId", "Account not found: $it") } }
        req.contactId?.let { d.contact = contactRepository.findById(it).orElseThrow { ValidationException("contactId", "Contact not found: $it") } }
        req.typeId?.let { d.type = lookups.resolveById("deal_type", it) }
        req.nextStep?.let { d.nextStep = it.trim() }
        req.leadSourceId?.let { d.leadSource = lookups.resolveById("lead_source", it) }
        req.reasonForLossId?.let { d.reasonForLoss = lookups.resolveById("reason_for_loss", it) }
        req.description?.let { d.description = it }
        req.tag?.let { d.tag = it }
    }
}
