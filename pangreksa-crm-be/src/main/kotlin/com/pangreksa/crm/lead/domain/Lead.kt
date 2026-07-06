package com.pangreksa.crm.lead.domain

import com.pangreksa.crm.account.domain.Account
import com.pangreksa.crm.base.OwnedEntity
import com.pangreksa.crm.contact.domain.Contact
import com.pangreksa.crm.deal.domain.Deal
import com.pangreksa.crm.lookup.domain.Lookup
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "leads")
class Lead(
    @Column(name = "first_name") var firstName: String? = null,
    @Column(name = "last_name", nullable = false) var lastName: String = "",
    var company: String? = null,
    var title: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "salutation_id") var salutation: Lookup? = null,
    var email: String? = null,
    @Column(name = "secondary_email") var secondaryEmail: String? = null,
    var phone: String? = null,
    var mobile: String? = null,
    var fax: String? = null,
    var website: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_source_id") var leadSource: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_status_id") var leadStatus: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "industry_id") var industry: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rating_id") var rating: Lookup? = null,
    @Column(name = "no_of_employees") var noOfEmployees: Int? = null,
    @Column(name = "annual_revenue", precision = 16, scale = 2) var annualRevenue: BigDecimal? = null,
    @Column(name = "email_opt_out", nullable = false) var emailOptOut: Boolean = false,
    @Column(name = "skype_id") var skypeId: String? = null,
    var twitter: String? = null,
    var street: String? = null,
    var city: String? = null,
    var state: String? = null,
    var country: String? = null,
    @Column(name = "zip_code") var zipCode: String? = null,
    @Column(columnDefinition = "text") var description: String? = null,
    @Column(nullable = false) var converted: Boolean = false,
    @Column(name = "converted_at") var convertedAt: LocalDateTime? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_account_id") var convertedAccount: Account? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_contact_id") var convertedContact: Contact? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_deal_id") var convertedDeal: Deal? = null,
) : OwnedEntity()

private const val FILTERS =
    "and (:leadStatusId is null or l.leadStatus.id = :leadStatusId) " +
    "and (:leadSourceId is null or l.leadSource.id = :leadSourceId) " +
    "and (:ratingId is null or l.rating.id = :ratingId) " +
    "and (:industryId is null or l.industry.id = :industryId) " +
    "and (:ownerId is null or l.owner.id = :ownerId)"

interface LeadRepository : JpaRepository<Lead, Long> {
    @Query(
        value = "select l from Lead l " +
            "left join fetch l.salutation left join fetch l.leadSource left join fetch l.leadStatus " +
            "left join fetch l.industry left join fetch l.rating left join fetch l.owner " +
            "where (:allOwners = true or l.owner.id in :owners) " +
            "and (:q = '' or lower(l.lastName) like lower(concat('%', :q, '%')) " +
            "or lower(l.firstName) like lower(concat('%', :q, '%')) " +
            "or lower(l.company) like lower(concat('%', :q, '%'))) " +
            FILTERS,
        countQuery = "select count(l) from Lead l where (:allOwners = true or l.owner.id in :owners) " +
            "and (:q = '' or lower(l.lastName) like lower(concat('%', :q, '%')) " +
            "or lower(l.firstName) like lower(concat('%', :q, '%')) " +
            "or lower(l.company) like lower(concat('%', :q, '%'))) " +
            FILTERS,
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        @Param("leadStatusId") leadStatusId: Long?,
        @Param("leadSourceId") leadSourceId: Long?,
        @Param("ratingId") ratingId: Long?,
        @Param("industryId") industryId: Long?,
        @Param("ownerId") ownerId: Long?,
        pageable: Pageable,
    ): Page<Lead>

    @Query(
        "select l from Lead l " +
            "left join fetch l.salutation left join fetch l.leadSource left join fetch l.leadStatus " +
            "left join fetch l.industry left join fetch l.rating left join fetch l.owner where l.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Lead?
}
