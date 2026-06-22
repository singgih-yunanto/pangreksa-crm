package com.pangreksa.crm.config

import com.pangreksa.crm.account.domain.Account
import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.config.domain.Configuration
import com.pangreksa.crm.config.domain.ConfigurationRepository
import com.pangreksa.crm.contact.domain.Contact
import com.pangreksa.crm.contact.domain.ContactRepository
import com.pangreksa.crm.deal.domain.Deal
import com.pangreksa.crm.deal.domain.DealRepository
import com.pangreksa.crm.lead.domain.Lead
import com.pangreksa.crm.lead.domain.LeadRepository
import com.pangreksa.crm.lookup.domain.Lookup
import com.pangreksa.crm.lookup.domain.LookupRepository
import com.pangreksa.crm.security.Permissions
import com.pangreksa.crm.user.domain.AppUser
import com.pangreksa.crm.user.domain.Role
import com.pangreksa.crm.user.domain.RoleRepository
import com.pangreksa.crm.user.domain.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Seeds the baseline (configuration, lookups, Admin role, admin user) on first run — always.
 * Demo/sample data (Sales Manager/Rep roles, demo users, and sample CRM records) is seeded
 * only when `pangreksa.seed=true` (defaults false), e.g. in demo environments.
 */
@Component
class DataSeeder(
    private val configs: ConfigurationRepository,
    private val lookups: LookupRepository,
    private val roles: RoleRepository,
    private val users: UserRepository,
    private val accounts: AccountRepository,
    private val contacts: ContactRepository,
    private val leads: LeadRepository,
    private val deals: DealRepository,
    private val encoder: PasswordEncoder,
    @Value("\${pangreksa.seed:false}") private val seedSample: Boolean,
) : CommandLineRunner {

    private val lk = mutableMapOf<String, Lookup>()
    private fun put(category: String, code: String, sort: Int, extra: Map<String, Any?> = emptyMap()): Lookup {
        val l = lookups.save(Lookup(category, code, code, sort, true, extra.toMutableMap()))
        lk["$category:$code"] = l
        return l
    }
    private fun ref(category: String, code: String) = lk["$category:$code"]

    /** Re-hydrate the in-memory lookup map from the DB (no-op when the baseline ran this run). */
    private fun loadLookups() {
        if (lk.isEmpty()) lookups.findAll().forEach { lk["${it.category}:${it.code}"] = it }
    }

    // Canonical ordered codes shared by baseline (creation) and sample (cycling).
    private val dealStageCodes = listOf(
        "Qualification", "Needs Analysis", "Value Proposition",
        "Identify Decision Makers", "Proposal/Price Quote", "Negotiation/Review",
    )
    private val dealStageProbabilities = listOf(10, 20, 40, 60, 75, 90)
    private val leadStatusCodes = listOf(
        "Not Contacted", "Attempted to Contact", "Contact in Future", "Contacted",
        "Pre-Qualified", "Not Qualified", "Junk Lead", "Lost Lead",
    )
    private val sampleSourceCodes = listOf("Advertisement", "Cold Call", "Trade Show", "Web Download", "Partner", "Chat")

    override fun run(vararg args: String) {
        if (users.count() == 0L) seedBaseline()
        if (seedSample && accounts.count() == 0L) seedSample()
    }

    /** Configuration, lookups, Admin role, and the admin user — always seeded on first run. */
    private fun seedBaseline() {
        // ---- Configuration ----
        configs.save(Configuration("currency", "IDR", "Currency"))
        configs.save(Configuration("decimal_places", "0", "Decimal places"))
        configs.save(Configuration("locale", "id-ID", "Locale"))

        // ---- Lookups ----
        dealStageCodes.forEachIndexed { i, s ->
            put("deal_stage", s, i, mapOf("probability" to dealStageProbabilities[i], "closed" to false))
        }
        put("deal_stage", "Closed Won", 6, mapOf("probability" to 100, "closed" to true, "won" to true))
        put("deal_stage", "Closed Lost", 7, mapOf("probability" to 0, "closed" to true, "won" to false))
        put("deal_stage", "Closed Lost to Competition", 8, mapOf("probability" to 0, "closed" to true, "won" to false))

        listOf("Existing Business", "New Business").forEachIndexed { i, s -> put("deal_type", s, i) }

        val closedStatuses = setOf("Not Qualified", "Junk Lead", "Lost Lead")
        leadStatusCodes.forEachIndexed { i, s -> put("lead_status", s, i, mapOf("closed" to (s in closedStatuses))) }

        listOf("Advertisement", "Cold Call", "Employee Referral", "External Referral", "Online Store",
            "Partner", "Public Relations", "Trade Show", "Web Download", "Web Research", "Chat")
            .forEachIndexed { i, s -> put("lead_source", s, i) }
        listOf("Analyst", "Competitor", "Customer", "Distributor", "Integrator", "Investor", "Partner",
            "Press", "Prospect", "Reseller", "Supplier", "Vendor", "Other")
            .forEachIndexed { i, s -> put("account_type", s, i) }
        listOf("Other", "Private", "Public", "Subsidiary", "Partnership", "Government")
            .forEachIndexed { i, s -> put("ownership", s, i) }
        listOf("ASP", "ERP", "Government/Military", "Large Enterprise", "MSP", "Network Equipment",
            "Service Provider", "Small/Medium Enterprise", "Storage Equipment", "Systems Integrator", "Software")
            .forEachIndexed { i, s -> put("industry", s, i) }
        listOf("Acquired", "Active", "Market Failed", "Project Cancelled", "Shut Down")
            .forEachIndexed { i, s -> put("rating", s, i) }
        listOf("Mr.", "Mrs.", "Ms.", "Dr.", "Prof.").forEachIndexed { i, s -> put("salutation", s, i) }
        listOf("Expectation Mismatch", "Price", "Unqualified Customer", "Lack of response",
            "Missed Follow Ups", "Wrong Target", "Lost to Competitor")
            .forEachIndexed { i, s -> put("reason_for_loss", s, i) }

        // ---- Admin role + admin user ----
        val admin = roles.save(Role("Admin", "Full access", null, Permissions.ALL.toMutableSet()))
        users.save(
            AppUser(
                username = "admin", fullName = "Admin", email = "admin@pangreksa.test",
                passwordHash = encoder.encode("admin")!!, active = true, role = admin,
            ),
        )
    }

    /** Demo roles/users and sample CRM records — only when `pangreksa.seed=true`. */
    private fun seedSample() {
        loadLookups()

        // ---- Roles (hierarchy: Admin > Sales Manager > Sales Rep) ----
        val admin = roles.findByName("Admin") ?: error("Admin role missing — baseline not seeded")
        val managerPerms = (Permissions.MODULES.flatMap { Permissions.crud(it) }).toMutableSet()
        val manager = roles.save(Role("Sales Manager", "Manages a sales team", admin, managerPerms))
        val repPerms = Permissions.MODULES.flatMap { listOf("${it}_VIEW", "${it}_CREATE", "${it}_EDIT") }.toMutableSet()
        val rep = roles.save(Role("Sales Rep", "Owns their own records", manager, repPerms))

        // ---- Users ----
        fun user(username: String, fullName: String, role: Role) = users.save(
            AppUser(
                username = username, fullName = fullName, email = "$username@pangreksa.test",
                passwordHash = encoder.encode(username)!!, active = true, role = role,
            ),
        )
        val singgih = user("singgih", "Singgih Yunanto", manager)
        val dewi = user("dewi", "Dewi Anggraini", rep)
        val bima = user("bima", "Bima Eka", rep)
        val repUsers = listOf(singgih, dewi, bima)

        // ---- Accounts ----
        data class A(val name: String, val type: String, val industry: String, val own: String, val emp: Int, val rev: String)
        val accSpecs = listOf(
            A("Acme Robotics", "Customer", "Storage Equipment", "Private", 220, "12500000"),
            A("Globex Holdings", "Prospect", "ERP", "Public", 1200, "84000000"),
            A("Initech Systems", "Customer", "MSP", "Private", 90, "6400000"),
            A("Umbrella Trading", "Reseller", "Service Provider", "Subsidiary", 540, "31000000"),
            A("Soylent Corp", "Partner", "Large Enterprise", "Public", 3100, "210000000"),
            A("Hooli Cloud", "Prospect", "ASP", "Private", 760, "52000000"),
            A("Stark Industrial", "Customer", "Network Equipment", "Public", 5400, "480000000"),
            A("Wayne Logistics", "Prospect", "Systems Integrator", "Private", 410, "28000000"),
        )
        val acc = accSpecs.mapIndexed { i, a ->
            accounts.save(Account(
                name = a.name, accountType = ref("account_type", a.type), industry = ref("industry", a.industry),
                ownership = ref("ownership", a.own), rating = ref("rating", "Active"), employees = a.emp,
                annualRevenue = BigDecimal(a.rev), phone = "+62-21-555-0100",
                website = "https://${a.name.substringBefore(' ').lowercase()}.test",
                billingCity = "Jakarta", billingCountry = "Indonesia",
            ).also { it.owner = repUsers[i % repUsers.size] })
        }

        // ---- Contacts ----
        val ctSpecs = listOf(
            Triple("Tanaka", "Hiro", 0), Triple("Merced", "James", 1), Triple("Kidman", "Carissa", 2),
            Triple("Sweely", "Tresa", 3), Triple("Hirpara", "Felix", 4), Triple("Lace", "Kayleigh", 5),
            Triple("Frey", "Theola", 6), Triple("Kitzman", "Chau", 7), Triple("Maclead", "Chris", 0),
            Triple("Tjepkema", "Yvonne", 1),
        )
        val ct = ctSpecs.mapIndexed { i, (last, first, ai) ->
            contacts.save(Contact(
                firstName = first, lastName = last, account = acc[ai], title = "Contact",
                email = "${first.lowercase()}.${last.lowercase()}@example.test",
                phone = "+62-811-555-${1000 + i}", leadSource = ref("lead_source", "Partner"),
            ).also { it.owner = repUsers[i % repUsers.size] })
        }

        // ---- Leads ----
        val leadSpecs = listOf(
            Triple("Pirzada", "Aisha", "Northwind Traders"), Triple("Okafor", "Emeka", "Fabrikam Inc"),
            Triple("Santos", "Lucia", "Contoso Ltd"), Triple("Brandt", "Niklas", "Tailspin Toys"),
            Triple("Whitfield", "Mara", "Adventure Works"), Triple("Ibrahim", "Yusuf", "Wide World Imports"),
            Triple("Delgado", "Paula", "Proseware"), Triple("Novak", "Petr", "Litware"),
            Triple("Hassan", "Layla", "Coho Vineyard"), Triple("Berg", "Sven", "Alpine Ski House"),
        )
        leadSpecs.forEachIndexed { i, (last, first, company) ->
            leads.save(Lead(
                firstName = first, lastName = last, company = company, title = "Decision Maker",
                email = "${first.lowercase()}@${company.substringBefore(' ').lowercase()}.test",
                phone = "+62-812-555-${1000 + i}",
                leadStatus = ref("lead_status", leadStatusCodes[i % leadStatusCodes.size]),
                leadSource = ref("lead_source", sampleSourceCodes[i % sampleSourceCodes.size]),
                rating = ref("rating", "Active"), annualRevenue = BigDecimal((i + 2) * 1_250_000),
            ).also { it.owner = repUsers[i % repUsers.size] })
        }

        // ---- Deals ----
        val dealSpecs = listOf(
            "Acme — platform expansion" to (0 to 120000.0), "Globex — ERP rollout" to (1 to 480000.0),
            "Initech — managed services" to (2 to 64000.0), "Umbrella — renewal 2026" to (3 to 96000.0),
            "Soylent — enterprise tier" to (4 to 310000.0), "Hooli — pilot to prod" to (5 to 52000.0),
            "Stark — network upgrade" to (6 to 540000.0), "Wayne — integration suite" to (7 to 78000.0),
            "Acme — add-on licenses" to (0 to 24000.0), "Globex — support uplift" to (1 to 38000.0),
            "Initech — Q3 expansion" to (2 to 145000.0), "Soylent — analytics module" to (4 to 210000.0),
            "Hooli — data migration" to (5 to 41000.0), "Stark — security add-on" to (6 to 88000.0),
        )
        dealSpecs.forEachIndexed { i, (name, pair) ->
            val (ai, amt) = pair
            val stage = ref("deal_stage", dealStageCodes[i % dealStageCodes.size])!!
            val prob = (stage.extra["probability"] as? Number)?.toInt() ?: 0
            val amount = BigDecimal.valueOf(amt)
            deals.save(Deal(
                name = name, stage = stage, amount = amount,
                closingDate = LocalDate.of(2026, 9, 1).plusDays((i * 9).toLong()),
                account = acc[ai], contact = ct.getOrNull(i % ct.size),
                type = ref("deal_type", if (i % 2 == 0) "New Business" else "Existing Business"),
                probability = prob,
                expectedRevenue = amount.multiply(BigDecimal(prob)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP),
                leadSource = ref("lead_source", sampleSourceCodes[i % sampleSourceCodes.size]),
            ).also { it.owner = repUsers[i % repUsers.size] })
        }
    }
}
