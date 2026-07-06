package com.pangreksa.crm.timeline

import com.pangreksa.crm.activity.call.service.CallService
import com.pangreksa.crm.activity.meeting.service.MeetingService
import com.pangreksa.crm.activity.task.service.TaskService
import com.pangreksa.crm.audit.AuditService
import com.pangreksa.crm.note.web.NoteService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/** One entry in a record's activity timeline. [kind] drives the frontend icon/label. */
data class TimelineItem(
    val kind: String, // audit | task | meeting | call | note
    val at: LocalDateTime,
    val title: String,
    val subtitle: String?,
    val actor: String?,
    val refModule: String?,
    val refId: Long?,
)

@Service
class TimelineService(
    private val audit: AuditService,
    private val tasks: TaskService,
    private val meetings: MeetingService,
    private val calls: CallService,
    private val notes: NoteService,
) {
    @Transactional(readOnly = true)
    fun forRecord(type: String, id: Long): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        audit.forRecord(type, id).forEach { e ->
            val summary = e.changes.values.firstOrNull()?.toString()
            items += TimelineItem("audit", e.at, auditTitle(e.action), summary, e.actorName, null, null)
        }
        tasks.forWhat(type, id).forEach { t ->
            items += TimelineItem("task", t.createdAt, t.subject, t.status?.label, t.owner?.fullName, "tasks", t.id)
        }
        meetings.forWhat(type, id).forEach { m ->
            items += TimelineItem("meeting", m.createdAt, m.title, m.status?.label, m.owner?.fullName, "meetings", m.id)
        }
        calls.forWhat(type, id).forEach { c ->
            items += TimelineItem("call", c.createdAt, c.subject, c.callType?.label, c.owner?.fullName, "calls", c.id)
        }
        notes.forParent(type, id).forEach { n ->
            items += TimelineItem("note", n.createdAt, n.body, null, n.owner?.fullName, null, null)
        }

        return items.sortedByDescending { it.at }
    }

    private fun auditTitle(action: String) = when (action) {
        "created" -> "Record created"
        "updated" -> "Record updated"
        "deleted" -> "Record deleted"
        else -> action.replaceFirstChar { it.uppercase() }
    }
}

@RestController
@RequestMapping("/api/timeline")
class TimelineController(private val service: TimelineService) {
    @GetMapping
    fun forRecord(@RequestParam type: String, @RequestParam id: Long): List<TimelineItem> =
        service.forRecord(type, id)
}
