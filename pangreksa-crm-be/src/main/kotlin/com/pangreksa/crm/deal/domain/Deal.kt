package com.pangreksa.crm.deal.domain

import com.pangreksa.crm.account.domain.Account
import com.pangreksa.crm.base.OwnedEntity
import com.pangreksa.crm.contact.domain.Contact
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
import java.time.LocalDate

@Entity
@Table(name = "deals")
class Deal(
    @Column(nullable = false) var name: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stage_id") var stage: Lookup? = null,
    @Column(precision = 16, scale = 2) var amount: BigDecimal? = null,
    @Column(name = "closing_date") var closingDate: LocalDate? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id") var account: Account? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contact_id") var contact: Contact? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "type_id") var type: Lookup? = null,
    var probability: Int? = null,
    @Column(name = "expected_revenue", precision = 16, scale = 2) var expectedRevenue: BigDecimal? = null,
    @Column(name = "next_step") var nextStep: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_source_id") var leadSource: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reason_for_loss_id") var reasonForLoss: Lookup? = null,
    @Column(columnDefinition = "text") var description: String? = null,
) : OwnedEntity()

interface DealRepository : JpaRepository<Deal, Long> {
    @Query(
        value = "select d from Deal d " +
            "left join fetch d.stage left join fetch d.type left join fetch d.account " +
            "left join fetch d.contact left join fetch d.leadSource left join fetch d.reasonForLoss " +
            "left join fetch d.owner " +
            "where (:allOwners = true or d.owner.id in :owners) " +
            "and (:q = '' or lower(d.name) like lower(concat('%', :q, '%')) " +
            "or lower(d.account.name) like lower(concat('%', :q, '%')))",
        countQuery = "select count(d) from Deal d where (:allOwners = true or d.owner.id in :owners) " +
            "and (:q = '' or lower(d.name) like lower(concat('%', :q, '%')) " +
            "or lower(d.account.name) like lower(concat('%', :q, '%')))",
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        pageable: Pageable,
    ): Page<Deal>

    @Query(
        "select d from Deal d " +
            "left join fetch d.stage left join fetch d.type left join fetch d.account " +
            "left join fetch d.contact left join fetch d.leadSource left join fetch d.reasonForLoss " +
            "left join fetch d.owner where d.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Deal?
}
