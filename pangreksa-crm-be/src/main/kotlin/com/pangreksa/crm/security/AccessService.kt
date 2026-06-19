package com.pangreksa.crm.security

import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.user.domain.AppUser
import com.pangreksa.crm.user.domain.RoleRepository
import com.pangreksa.crm.user.domain.UserRepository
import org.springframework.stereotype.Service

/**
 * Row-level visibility via the role hierarchy: a user sees records owned by themselves and by users
 * whose role is a strict descendant of their role. [Permissions.VIEW_ALL] (admin) sees everything.
 */
@Service
class AccessService(
    private val roles: RoleRepository,
    private val users: UserRepository,
) {
    /** Owner ids the current user may see, or `null` for "all" (VIEW_ALL). */
    fun accessibleOwnerIds(): Set<Long>? {
        val p = CurrentUser.require()
        if (Permissions.VIEW_ALL in p.permissions) return null
        val ids = mutableSetOf(p.userId)
        val descendantRoleIds = descendantRoleIds(p.roleId)
        if (descendantRoleIds.isNotEmpty()) {
            users.findByRoleIdIn(descendantRoleIds).forEach { it.id?.let(ids::add) }
        }
        return ids
    }

    /** The current authenticated user entity (record owner on create). */
    fun currentUser(): AppUser {
        val p = CurrentUser.require()
        return users.findById(p.userId).orElseThrow { ValidationException(null, "User not found") }
    }

    fun canAccess(ownerId: Long?): Boolean {
        val ids = accessibleOwnerIds() ?: return true
        return ownerId != null && ownerId in ids
    }

    private fun descendantRoleIds(rootRoleId: Long?): Set<Long> {
        if (rootRoleId == null) return emptySet()
        val childrenByParent = roles.findAll()
            .filter { it.parentRole?.id != null }
            .groupBy({ it.parentRole!!.id!! }, { it.id!! })
        val out = mutableSetOf<Long>()
        val queue = ArrayDeque(childrenByParent[rootRoleId] ?: emptyList())
        while (queue.isNotEmpty()) {
            val r = queue.removeFirst()
            if (out.add(r)) queue.addAll(childrenByParent[r] ?: emptyList())
        }
        return out
    }
}
