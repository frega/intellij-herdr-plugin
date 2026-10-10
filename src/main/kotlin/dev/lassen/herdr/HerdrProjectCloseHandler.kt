package dev.lassen.herdr

import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import dev.lassen.herdr.settings.HerdrSessionState
import dev.lassen.herdr.settings.HerdrSettingsState

/**
 * Offers to suspend the project's herdr session when the project closes (which includes quitting the IDE),
 * so its agents and shells don't keep running unattended. Cancel aborts the close.
 */
class HerdrProjectCloseHandler : ProjectCloseHandler {

    override fun canClose(project: Project): Boolean {
        val settings = HerdrSettingsState.getInstance().state
        if (!settings.askToSuspendOnClose) return true
        if ((project.getUserData(KEEP_RUNNING_UNTIL) ?: 0L) > System.currentTimeMillis()) return true

        val panel = ToolWindowManager.getInstance(project).getToolWindow("Herdr")
            ?.contentManager?.contents
            ?.firstNotNullOfOrNull { it.component as? HerdrTerminalPanel }
            ?.takeIf { it.mode == HerdrTerminalPanel.Mode.ATTACHED }
            ?: return true
        val sessionName = panel.currentSession

        val summary = ProgressManager.getInstance().runProcessWithProgressSynchronously<HerdrCli.SessionSummary, Exception>(
            { HerdrCli.sessionSummary(panel.binaryPath, sessionName) },
            "Checking herdr session '$sessionName'",
            false,
            project,
        )
        if (!summary.running) return true

        val choice = MessageDialogBuilder.yesNoCancel(
            "Suspend herdr session '$sessionName'?",
            "${SuspendHerdrSessionAction.describe(summary)}\n\nKeep Running leaves it running in the background.",
        )
            .yesText("Suspend")
            .noText("Keep Running")
            .cancelText("Cancel")
            .icon(if (summary.busyAgents > 0) AllIcons.General.WarningDialog else AllIcons.General.QuestionDialog)
            .doNotAsk(object : DoNotAskOption.Adapter() {
                override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
                    if (isSelected && exitCode != Messages.CANCEL) settings.askToSuspendOnClose = false
                }
            })
            .show(project)

        return when (choice) {
            Messages.YES -> suspend(project, panel, sessionName)
            Messages.NO -> {
                project.putUserData(KEEP_RUNNING_UNTIL, System.currentTimeMillis() + KEEP_RUNNING_MEMORY_MS)
                true
            }
            else -> false
        }
    }

    /** Stops the session; returns whether the project may close. */
    private fun suspend(project: Project, panel: HerdrTerminalPanel, sessionName: String): Boolean {
        // Detach first so the panel doesn't react to its herdr client exiting.
        panel.showShuttingDown(sessionName)
        val error = ProgressManager.getInstance().runProcessWithProgressSynchronously<String?, Exception>(
            { HerdrCli.stopSession(panel.binaryPath, sessionName) },
            "Suspending herdr session '$sessionName'",
            false,
            project,
        )
        if (error == null) {
            panel.showSuspended(sessionName)
            HerdrSessionState.getInstance(project).state.suspended = true
            return true
        }
        val closeAnyway = MessageDialogBuilder.yesNo(
            "Could not suspend herdr session '$sessionName'",
            "$error\n\nThe session is still running. Close the project anyway?",
        )
            .yesText("Close Anyway")
            .noText("Cancel")
            .icon(AllIcons.General.ErrorDialog)
            .ask(project)
        if (!closeAnyway) panel.attachSession(sessionName)
        return closeAnyway
    }

    private companion object {
        /**
         * Quitting the IDE asks every handler twice per project: once while checking whether the app can
         * exit, then again while closing the project. Remember "Keep Running" briefly so the second call
         * doesn't ask again. (Suspend needs no memory: the session is no longer attached by then.)
         */
        val KEEP_RUNNING_UNTIL = Key.create<Long>("herdr.keepRunningUntil")
        const val KEEP_RUNNING_MEMORY_MS = 60_000L
    }
}
