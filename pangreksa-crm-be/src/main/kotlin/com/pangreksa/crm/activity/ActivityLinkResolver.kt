package com.pangreksa.crm.activity

import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.contact.domain.ContactRepository
import com.pangreksa.crm.deal.domain.DealRepository
import com.pangreksa.crm.lead.domain.LeadRepository
import org.springframework.stereotype.Service

/**
 * Resolves a polymorphic activity link (a module key + record id, as stored in `who_type/who_id` and
 * `what_type/what_id`) to a human display label. "what" may point at accounts/deals/contacts/leads;
 * "who" at contacts/leads. Module keys match the frontend/route names.
 */
@Service
class ActivityLinkResolver(
    private val accounts: AccountRepository,
    private val deals: DealRepository,
    private val contacts: ContactRepository,
    private val leads: LeadRepository,
) {
    fun label(type: String?, id: Long?): String? {
        if (type.isNullOrBlank() || id == null) return null
        return when (type) {
            "accounts" -> accounts.findById(id).orElse(null)?.name
            "deals" -> deals.findById(id).orElse(null)?.name
            "contacts" -> contacts.findById(id).orElse(null)?.let { person(it.firstName, it.lastName) }
            "leads" -> leads.findById(id).orElse(null)?.let { person(it.firstName, it.lastName) }
            else -> null
        }
    }

    private fun person(first: String?, last: String?): String =
        listOfNotNull(first, last).joinToString(" ").trim()
}
