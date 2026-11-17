package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.BoardTemplate
import com.katoaapps.minkboard.core.BoardTemplateStatus
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.model.PortableBoardSnapshot
import com.katoaapps.minkboard.model.PortableCardSnapshot
import com.katoaapps.minkboard.model.PortableStatusSnapshot
import com.katoaapps.minkboard.model.PortableSubtaskSnapshot
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import java.time.LocalDate

internal data class DemoBoardArchiveFixture(
    val fileName: String,
    val snapshot: PortableWorkspaceSnapshot,
    val template: BoardTemplate?,
)

/**
 * Canonical data behind the encrypted demo archives.
 *
 * Keeping the expected snapshots as source makes the binary fixtures reviewable and lets tests
 * verify every decoded field. The shared passphrase is intentionally only for public demo data.
 */
internal object DemoBoardArchiveFixtures {
    const val PASSPHRASE = "minktest123"
    const val RESOURCE_DIRECTORY = "demo-archives"

    private const val EXPORTED_AT = 1_794_528_000_000L
    private const val CREATED_AT = 1_794_441_600_000L
    private const val SOURCE_APP = "MinkBoard Pro demo fixtures"
    private val firstDueDate = LocalDate.of(2026, 11, 17)

    val all: List<DemoBoardArchiveFixture> = listOf(
        templateFixture(
            fileName = "simple-board-demo.minkboard",
            template = MinkBoardTemplates.Simple,
            titles = listOf(
                "Choose a launch date",
                "Draft the announcement",
                "Collect team feedback",
                "Prepare the demo board",
                "Confirm the final checklist",
                "Review open questions",
                "Share the preview build",
                "Polish the welcome copy",
                "Schedule the walkthrough",
                "Record the short demo",
                "Publish the release notes",
                "Celebrate the launch",
            ),
        ),
        templateFixture(
            fileName = "weekly-client-appointments-demo.minkboard",
            template = MinkBoardTemplates.WeeklyClientAppointments,
            titles = listOf(
                "Avery annual check-in",
                "Jordan intake appointment",
                "Morgan follow-up call",
                "Riley lab review",
                "Cameron medication check",
                "Taylor new client consult",
                "Quinn referral follow-up",
                "Parker care plan review",
                "Casey telehealth visit",
                "Drew insurance callback",
                "Reese progress check",
                "Skyler records request",
                "Rowan reschedule request",
                "Emerson discharge review",
            ),
        ),
        templateFixture(
            fileName = "work-after-work-demo.minkboard",
            template = MinkBoardTemplates.WorkAfterWork,
            titles = listOf(
                "Call the pharmacy",
                "Return the family voicemail",
                "Confirm tomorrow's schedule",
                "Fax the signed referral",
                "Send updated intake forms",
                "Review outstanding messages",
                "Finish the visit summary",
                "Update the care plan",
                "File the lab report",
                "Close the open chart",
                "Verify the authorization",
                "Send the handoff note",
                "Check the document queue",
                "Archive completed paperwork",
                "Prepare the morning call list",
                "Clear the final inbox item",
            ),
        ),
        templateFixture(
            fileName = "agile-development-demo.minkboard",
            template = MinkBoardTemplates.AgileDevelopment,
            titles = listOf(
                "Add board switcher affordance",
                "Repair archive import crash",
                "Replace due date text field",
                "Design the Board Map",
                "Add card detail view",
                "Polish drag auto-scroll",
                "Write archive round-trip tests",
                "Audit protected board access",
                "Add status color previews",
                "Verify duplicate card naming",
                "Improve empty board messaging",
                "Review accessibility labels",
                "Profile overview rendering",
                "Test status deletion flow",
                "Document release checklist",
                "Prepare device acceptance pass",
                "Resolve lint findings",
                "Ship the feedback release",
            ),
        ),
        templateFixture(
            fileName = "house-chores-demo.minkboard",
            template = MinkBoardTemplates.HouseChores,
            titles = listOf(
                "Vacuum the living room",
                "Wash the bedding",
                "Clean the kitchen counters",
                "Take out recycling",
                "Water the houseplants",
                "Wipe down the bathroom",
                "Organize the entry table",
                "Change the air filter",
                "Sweep the porch",
                "Restock cleaning supplies",
                "Fold the laundry",
                "Clean the refrigerator shelf",
                "Sort the mail",
                "Replace the hallway bulb",
                "Tidy the dog supplies",
                "Plan next week's groceries",
            ),
        ),
        templateFixture(
            fileName = "weekly-assignments-demo.minkboard",
            template = MinkBoardTemplates.WeeklyAssignments,
            titles = listOf(
                "Read chapter six",
                "Complete the cell diagram",
                "Review lab safety notes",
                "Submit programming exercise four",
                "Debug the sorting assignment",
                "Prepare for the code review",
                "Outline the comparison essay",
                "Revise the opening paragraph",
                "Add bibliography citations",
                "Study for the biology quiz",
                "Finish the database worksheet",
                "Meet with the writing group",
                "Upload the lab report",
                "Practice presentation slides",
                "Check the course calendar",
            ),
        ),
        templateFixture(
            fileName = "quarterly-goals-demo.minkboard",
            template = MinkBoardTemplates.QuarterlyGoals,
            titles = listOf(
                "Launch the customer newsletter",
                "Reduce support response time",
                "Publish the design system",
                "Complete the accessibility audit",
                "Grow the beta group",
                "Document the release process",
                "Refresh the product screenshots",
                "Finish the onboarding rewrite",
                "Run the performance baseline",
                "Expand the testing matrix",
                "Ship the tablet layout",
                "Review the annual budget",
                "Interview five customers",
                "Create the fall roadmap",
                "Plan the community event",
                "Retire the legacy workflow",
                "Prepare the year-end recap",
                "Set next year's priorities",
            ),
        ),
        customFixture(),
        massiveCustomFixture(),
    )

    private fun templateFixture(
        fileName: String,
        template: BoardTemplate,
        titles: List<String>,
    ): DemoBoardArchiveFixture = DemoBoardArchiveFixture(
        fileName = fileName,
        snapshot = workspace(
            boardSlug = template.id.value,
            boardName = template.name,
            statuses = template.statuses,
            titles = titles,
        ),
        template = template,
    )

    private fun customFixture(): DemoBoardArchiveFixture {
        val statuses = listOf(
            BoardTemplateStatus("Ideas", 0xFF7C4DFF.toInt()),
            BoardTemplateStatus("Research", 0xFF536DFE.toInt()),
            BoardTemplateStatus("Ready", 0xFF00ACC1.toInt()),
            BoardTemplateStatus("In Progress", 0xFFFFA000.toInt()),
            BoardTemplateStatus("Blocked", 0xFFE53935.toInt()),
            BoardTemplateStatus("Review", 0xFF8E24AA.toInt()),
            BoardTemplateStatus("Launch Prep", 0xFF3949AB.toInt()),
            BoardTemplateStatus("Scheduled", 0xFF43A047.toInt()),
            BoardTemplateStatus("Done", 0xFFA1D6B2.toInt(), isDoneStatus = true),
        )
        val titles = listOf(
            "Define the launch audience",
            "Collect competitor screenshots",
            "Interview three early testers",
            "Draft the product promise",
            "Choose the campaign theme",
            "Audit the onboarding flow",
            "Plan the teaser sequence",
            "Write the announcement post",
            "Record the feature walkthrough",
            "Prepare the press kit",
            "Build the landing page update",
            "Capture final store screenshots",
            "Review privacy language",
            "Confirm support coverage",
            "Schedule the beta email",
            "Resolve the last blocking bug",
            "Run the release build",
            "Verify the signed bundle",
            "Complete the device matrix",
            "Publish the FAQ refresh",
            "Queue the social posts",
            "Brief the community moderators",
            "Prepare launch-day analytics",
            "Stage the website release",
            "Send the tester thank-you note",
            "Monitor first-day feedback",
            "Triage incoming reports",
            "Write the launch retrospective",
        )
        return DemoBoardArchiveFixture(
            fileName = "product-launch-command-center-demo.minkboard",
            snapshot = workspace(
                boardSlug = "product-launch-command-center",
                boardName = "Product Launch Command Center",
                statuses = statuses,
                titles = titles,
            ),
            template = null,
        )
    }

    private fun massiveCustomFixture(): DemoBoardArchiveFixture {
        val statuses = listOf(
            BoardTemplateStatus("Backlog", 0xFF602C00.toInt()),
            BoardTemplateStatus("Planned", 0xFF536DFE.toInt()),
            BoardTemplateStatus("Ready", 0xFF00ACC1.toInt()),
            BoardTemplateStatus("In Progress", 0xFFFFA000.toInt()),
            BoardTemplateStatus("Blocked", 0xFFE53935.toInt()),
            BoardTemplateStatus("Review", 0xFF8E24AA.toInt()),
            BoardTemplateStatus("Release Ready", 0xFF3949AB.toInt()),
            BoardTemplateStatus("Done", 0xFF43A047.toInt(), isDoneStatus = true),
        )
        val cardsPerStatus = listOf(83, 57, 41, 35, 17, 29, 23, 62)
        val initiatives = listOf(
            "Account foundation",
            "Team workspaces",
            "Project templates",
            "Mobile navigation",
            "Tablet experience",
            "Search and discovery",
            "Notifications",
            "Offline reliability",
            "Import and export",
            "Reporting dashboard",
            "Accessibility",
            "Security review",
            "Performance",
            "Release operations",
            "Customer onboarding",
            "Billing and plans",
            "Data migration",
            "Support operations",
        )
        val workItems = listOf(
            "Define requirements",
            "Map the user flow",
            "Review edge cases",
            "Build the first pass",
            "Add validation",
            "Add empty states",
            "Add error recovery",
            "Instrument performance",
            "Write regression tests",
            "Write integration tests",
            "Run accessibility review",
            "Review security boundaries",
            "Complete device testing",
            "Complete tablet testing",
            "Complete offline testing",
            "Polish final copy",
            "Prepare support guidance",
            "Update documentation",
            "Prepare release notes",
            "Run release acceptance",
        )
        val titles = initiatives.flatMap { initiative ->
            workItems.map { workItem -> "$workItem: $initiative" }
        }
        var sourceIndex = 0
        val boardSlug = "massive-product-roadmap"
        return DemoBoardArchiveFixture(
            fileName = "massive-product-roadmap-demo.minkboard",
            snapshot = PortableWorkspaceSnapshot(
                exportedAtEpochMillis = EXPORTED_AT,
                sourceApp = SOURCE_APP,
                boards = listOf(
                    PortableBoardSnapshot(
                        id = "demo-board-$boardSlug",
                        name = "Massive Product Roadmap",
                        isLocked = false,
                        position = 0,
                        createdAtEpochMillis = CREATED_AT,
                        statuses = statuses.mapIndexed { statusIndex, status ->
                            PortableStatusSnapshot(
                                id = "demo-status-$boardSlug-$statusIndex",
                                name = status.name,
                                colorArgb = status.colorArgb,
                                isDoneStatus = status.isDoneStatus,
                                position = statusIndex,
                                cards = List(cardsPerStatus[statusIndex]) { position ->
                                    val cardIndex = sourceIndex++
                                    card(
                                        boardSlug = boardSlug,
                                        sourceIndex = cardIndex,
                                        position = position,
                                        title = titles[cardIndex],
                                    )
                                },
                            )
                        },
                    ),
                ),
            ),
            template = null,
        )
    }

    private fun workspace(
        boardSlug: String,
        boardName: String,
        statuses: List<BoardTemplateStatus>,
        titles: List<String>,
    ): PortableWorkspaceSnapshot {
        val cardsByStatus = titles.withIndex().groupBy { it.index % statuses.size }
        return PortableWorkspaceSnapshot(
            exportedAtEpochMillis = EXPORTED_AT,
            sourceApp = SOURCE_APP,
            boards = listOf(
                PortableBoardSnapshot(
                    id = "demo-board-$boardSlug",
                    name = boardName,
                    isLocked = false,
                    position = 0,
                    createdAtEpochMillis = CREATED_AT,
                    statuses = statuses.mapIndexed { statusIndex, status ->
                        PortableStatusSnapshot(
                            id = "demo-status-$boardSlug-$statusIndex",
                            name = status.name,
                            colorArgb = status.colorArgb,
                            isDoneStatus = status.isDoneStatus,
                            position = statusIndex,
                            cards = cardsByStatus.getValue(statusIndex).mapIndexed { position, card ->
                                card(
                                    boardSlug = boardSlug,
                                    sourceIndex = card.index,
                                    position = position,
                                    title = card.value,
                                )
                            },
                        )
                    },
                ),
            ),
        )
    }

    private fun card(
        boardSlug: String,
        sourceIndex: Int,
        position: Int,
        title: String,
    ): PortableCardSnapshot {
        val createdAt = CREATED_AT + sourceIndex * 60_000L
        return PortableCardSnapshot(
            id = "demo-card-$boardSlug-$sourceIndex",
            title = title,
            details = "Demo item ${sourceIndex + 1}. Add an owner, confirm the next step, and move " +
                "the card when the work changes.",
            dueDateIso = if (sourceIndex % 3 == 0) {
                firstDueDate.plusDays(sourceIndex.toLong()).toString()
            } else {
                null
            },
            isMajor = sourceIndex % 5 == 0,
            position = position,
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = createdAt + (sourceIndex % 4) * 15_000L,
            subtasks = if (sourceIndex % 4 == 0) {
                listOf(
                    PortableSubtaskSnapshot(
                        id = "demo-subtask-$boardSlug-$sourceIndex-0",
                        text = "Confirm the owner",
                        isCompleted = sourceIndex % 8 == 0,
                        position = 0,
                    ),
                    PortableSubtaskSnapshot(
                        id = "demo-subtask-$boardSlug-$sourceIndex-1",
                        text = "Capture the next step",
                        isCompleted = false,
                        position = 1,
                    ),
                )
            } else {
                emptyList()
            },
        )
    }
}
