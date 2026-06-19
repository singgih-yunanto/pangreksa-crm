package com.pangreksa.crm.contact.domain

import com.pangreksa.crm.account.domain.Account
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
import java.time.LocalDate

@Entity
@Table(name = "contacts")
class Contact(
    @Column(name = "first_name") var firstName: String? = null,
    @Column(name = "last_name", nullable = false) var lastName: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id") var account: Account? = null,
    var title: String? = null,
    var department: String? = null,
    var email: String? = null,
    @Column(name = "secondary_email") var secondaryEmail: String? = null,
    var phone: String? = null,
    var mobile: String? = null,
    @Column(name = "home_phone") var homePhone: String? = null,
    @Column(name = "other_phone") var otherPhone: String? = null,
    var fax: String? = null,
    @Column(name = "date_of_birth") var dateOfBirth: LocalDate? = null,
    var assistant: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_source_id") var leadSource: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reporting_to_id") var reportingTo: Contact? = null,
    @Column(name = "mailing_street") var mailingStreet: String? = null,
    @Column(name = "mailing_city") var mailingCity: String? = null,
    @Column(name = "mailing_state") var mailingState: String? = null,
    @Column(name = "mailing_country") var mailingCountry: String? = null,
    @Column(name = "mailing_code") var mailingCode: String? = null,
    @Column(name = "other_street") var otherStreet: String? = null,
    @Column(name = "other_city") var otherCity: String? = null,
    @Column(name = "other_state") var otherState: String? = null,
    @Column(name = "other_country") var otherCountry: String? = null,
    @Column(name = "other_code") var otherCode: String? = null,
    @Column(name = "email_opt_out", nullable = false) var emailOptOut: Boolean = false,
    @Column(columnDefinition = "text") var description: String? = null,
) : OwnedEntity()

interface ContactRepository : JpaRepository<Contact, Long> {
    @Query(
        value = "select c from Contact c " +
            "left join fetch c.account left join fetch c.leadSource left join fetch c.reportingTo " +
            "left join fetch c.owner " +
            "where (:allOwners = true or c.owner.id in :owners) " +
            "and (:q = '' or lower(c.lastName) like lower(concat('%', :q, '%')) " +
            "or lower(c.firstName) like lower(concat('%', :q, '%')) " +
            "or lower(c.email) like lower(concat('%', :q, '%')) " +
            "or lower(c.account.name) like lower(concat('%', :q, '%')))",
        countQuery = "select count(c) from Contact c where (:allOwners = true or c.owner.id in :owners) " +
            "and (:q = '' or lower(c.lastName) like lower(concat('%', :q, '%')) " +
            "or lower(c.firstName) like lower(concat('%', :q, '%')) " +
            "or lower(c.email) like lower(concat('%', :q, '%')) " +
            "or lower(c.account.name) like lower(concat('%', :q, '%')))",
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        pageable: Pageable,
    ): Page<Contact>

    @Query(
        "select c from Contact c " +
            "left join fetch c.account left join fetch c.leadSource left join fetch c.reportingTo " +
            "left join fetch c.owner where c.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Contact?
}
