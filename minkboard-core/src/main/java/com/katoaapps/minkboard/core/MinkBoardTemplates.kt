package com.katoaapps.minkboard.core

@JvmInline
value class BoardTemplateId(val value: String)

data class BoardTemplateStatus(
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean = false,
)

data class BoardTemplate(
    val id: BoardTemplateId,
    val name: String,
    val description: String,
    val statuses: List<BoardTemplateStatus>,
)

/**
 * Canonical board seeds inherited from Spencer. Keeping them as immutable data makes template
 * creation testable and prevents UI/controller branches from becoming the template definition.
 */
object MinkBoardTemplates {
    val Simple = BoardTemplate(
        id = BoardTemplateId("simple"),
        name = "Simple Board",
        description = "A simple board with To do, Doing, and Done statuses.",
        statuses = listOf(
            status("To-Do", 0xFFB60071),
            status("Doing", 0xFFCB80AB),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val WeeklyClientAppointments = BoardTemplate(
        id = BoardTemplateId("weekly-client-appointments"),
        name = "Weekly Client Appointments",
        description = "Move clients or appointments through each weekday and into Done.",
        statuses = listOf(
            status("Patients", 0xFF8967B3),
            status("Monday", 0xFFFFB200),
            status("Tuesday", 0xFFEB5B00),
            status("Wednesday", 0xFFE4003A),
            status("Thursday", 0xFFB60071),
            status("Friday", 0xFFCB80AB),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val WorkAfterWork = BoardTemplate(
        id = BoardTemplateId("work-after-work"),
        name = "The Work After Work",
        description = "Keep calls, messages, paperwork, and other follow-up work visible.",
        statuses = listOf(
            status("Calls", 0xFF92C7CF),
            status("Faxes", 0xFFAAD7D9),
            status("Charts", 0xFF7BD3EA),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val AgileDevelopment = BoardTemplate(
        id = BoardTemplateId("agile-development"),
        name = "Agile Dev Project",
        description = "Move work from the backlog through implementation, testing, and review.",
        statuses = listOf(
            status("Backlog", 0xFFF44336),
            status("To do", 0xFFFF5722),
            status("In Progress", 0xFFFF9800),
            status("Testing", 0xFFFFC107),
            status("Under Review", 0xFF03A9F4),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val HouseChores = BoardTemplate(
        id = BoardTemplateId("house-chores"),
        name = "House Chores",
        description = "Collect household work, assign it, and keep completed chores together.",
        statuses = listOf(
            status("Chores", 0xFF03A9F4),
            status("Thing 1", 0xFFF40330),
            status("Thing 2", 0xFFF4C703),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val WeeklyAssignments = BoardTemplate(
        id = BoardTemplateId("weekly-assignments"),
        name = "Weekly Assignments",
        description = "Sort assignments by class or team and mark them Done when finished.",
        statuses = listOf(
            status("Assignments", 0xFFBC9F8B),
            status("BIO 101", 0xFFDEAC80),
            status("CS 220", 0xFFF7C566),
            status("ENG 110", 0xFFF2613F),
            status("Done", 0xFFA1D6B2, done = true),
        ),
    )

    val QuarterlyGoals = BoardTemplate(
        id = BoardTemplateId("quarterly-goals"),
        name = "Quarterly Goals",
        description = "Plan long-running goals by quarter and keep missed goals visible.",
        statuses = listOf(
            status("Goals", 0xFFD3D3D3),
            status("Q1", 0xFF227B94),
            status("Q2", 0xFFFF7EE2),
            status("Q3", 0xFF8EACCD),
            status("Q4", 0xFFB5CFB7),
            status("Missed", 0xFFFF0000),
        ),
    )

    val all: List<BoardTemplate> = listOf(
        Simple,
        WeeklyClientAppointments,
        WorkAfterWork,
        AgileDevelopment,
        HouseChores,
        WeeklyAssignments,
        QuarterlyGoals,
    )

    fun require(id: BoardTemplateId): BoardTemplate = all.firstOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown board template: ${id.value}")

    private fun status(name: String, color: Long, done: Boolean = false) = BoardTemplateStatus(
        name = name,
        colorArgb = color.toInt(),
        isDoneStatus = done,
    )
}
