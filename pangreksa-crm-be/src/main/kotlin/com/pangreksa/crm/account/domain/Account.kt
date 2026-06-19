package com.pangreksa.crm.account.domain

import com.pangreksa.crm.base.OwnedEntity
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

@Entity
@Table(name = "accounts")
class Account(
    @Column(nullable = false)
    var name: String = "",
    @Column(name = "account_number")
    var accountNumber: Long? = null,
    @Column(name = "account_site")
    var accountSite: String? = null,
    var phone: String? = null,
    var fax: String? = null,
    var website: String? = null,
    @Column(name = "ticker_symbol")
    var tickerSymbol: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_type_id") var accountType: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ownership_id") var ownership: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "industry_id") var industry: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rating_id") var rating: Lookup? = null,
    var employees: Int? = null,
    @Column(name = "annual_revenue", precision = 16, scale = 2)
    var annualRevenue: BigDecimal? = null,
    @Column(name = "sic_code")
    var sicCode: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_account_id") var parentAccount: Account? = null,
    @Column(name = "billing_street") var billingStreet: String? = null,
    @Column(name = "billing_city") var billingCity: String? = null,
    @Column(name = "billing_state") var billingState: String? = null,
    @Column(name = "billing_country") var billingCountry: String? = null,
    @Column(name = "billing_code") var billingCode: String? = null,
    @Column(name = "shipping_street") var shippingStreet: String? = null,
    @Column(name = "shipping_city") var shippingCity: String? = null,
    @Column(name = "shipping_state") var shippingState: String? = null,
    @Column(name = "shipping_country") var shippingCountry: String? = null,
    @Column(name = "shipping_code") var shippingCode: String? = null,
    @Column(columnDefinition = "text")
    var description: String? = null,
) : OwnedEntity()

interface AccountRepository : JpaRepository<Account, Long> {
    @Query(
        value = "select a from Account a " +
            "left join fetch a.accountType left join fetch a.ownership left join fetch a.industry " +
            "left join fetch a.rating left join fetch a.parentAccount left join fetch a.owner " +
            "where (:allOwners = true or a.owner.id in :owners) " +
            "and (:q = '' or lower(a.name) like lower(concat('%', :q, '%')) " +
            "or lower(a.website) like lower(concat('%', :q, '%')))",
        countQuery = "select count(a) from Account a where (:allOwners = true or a.owner.id in :owners) " +
            "and (:q = '' or lower(a.name) like lower(concat('%', :q, '%')) " +
            "or lower(a.website) like lower(concat('%', :q, '%')))",
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        pageable: Pageable,
    ): Page<Account>

    @Query(
        "select a from Account a " +
            "left join fetch a.accountType left join fetch a.ownership left join fetch a.industry " +
            "left join fetch a.rating left join fetch a.parentAccount left join fetch a.owner " +
            "where a.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Account?
}
