package dev.lassen.herdr

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import dev.lassen.herdr.settings.HerdrSessionState

/**
 * Suspends the attached herdr session (after confirming) or reawakes it.
 *
 * Suspending is `herdr session stop`: herdr persists workspaces, layout, working directories and agent
 * session refs, and restores them (resuming the agents) when the session next starts, which is what
 * reawaking does by attaching again.
 */
class SuspendHerdrSessionAction(
    private val project: Project,
    private val binaryPath: String,
    private val panel: HerdrTerminalPanel,
) : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = panel.mode != HerdrTerminalPanel.Mode.SHUTTING_DOWN
        if (panel.isSuspended) {
            e.presentation.text = "Reawake Herdr Session"
            e.presentation.description = "Start the session again, restoring its layout and agents"
            e.presentation.icon = AllIcons.Actions.Execute
        } else {
            e.presentation.text = "Suspend Herdr Session"
            e.presentation.description = "Stop the session to free resources; reawake it later where you left off"
            e.presentation.icon = AllIcons.Actions.Pause
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (panel.isSuspended) {
            panel.attachSession(panel.currentSession)
            panel.focusTerminal()
            return
        }

        val sessionName = panel.currentSession
        ApplicationManager.getApplication().executeOnPooledThread {
            val summary = HerdrCli.sessionSummary(binaryPath, sessionName)
            ApplicationManager.getApplication().invokeLater {
                if (confirm(sessionName, summary)) suspend(sessionName, summary.running)
            }
        }
    }

    private fun confirm(sessionName: String, summary: HerdrCli.SessionSummary): Boolean {
        return MessageDialogBuilder.yesNo("Suspend herdr session '$sessionName'?", describe(summary))
            .yesText("Suspend")
            .noText("Cancel")
            .icon(if (summary.busyAgents > 0) AllIcons.General.WarningDialog else AllIcons.General.QuestionDialog)
            .ask(project)
    }

    private fun suspend(sessionName: String, running: Boolean) {
        panel.showShuttingDown(sessionName)
        ApplicationManager.getApplication().executeOnPooledThread {
            val error = if (running) HerdrCli.stopSession(binaryPath, sessionName) else null
            ApplicationManager.getApplication().invokeLater {
                if (error != null) {
                    NotificationGroupManager.getInstance().getNotificationGroup("Herdr")
                        .createNotification("Could not suspend herdr session '$sessionName': $error", NotificationType.ERROR)
                        .notify(project)
                    // The session is still running; bring its terminal back.
                    panel.attachSession(sessionName)
                    return@invokeLater
                }
                panel.showSuspended(sessionName)
                HerdrSessionState.getInstance(project).state.suspended = true
            }
        }
    }

    companion object {
        /** What suspending closes and what reawaking brings back; shared with [HerdrProjectCloseHandler]. */
        fun describe(summary: HerdrCli.SessionSummary): String = buildString {
            if (summary.running) {
                append("${plural(summary.workspaces, "workspace")}, ${plural(summary.tabs, "tab")} will be closed.")
                if (summary.busyAgents > 0) {
                    append("\n\n${plural(summary.busyAgents, "agent")} still working or waiting for input will be interrupted.")
                }
                append("\n\nAny other window or terminal attached to this session is disconnected too.")
            } else {
                append("The session isn't running.")
            }
            append("\n\nYou can reawake it later: layouts are kept and coding agents (e.g. Claude Code) resume their ")
            append("conversations. Running commands (dev servers, watchers, builds) and scrollback are not kept.")
        }

        private fun plural(count: Int, noun: String) = if (count == 1) "1 $noun" else "$count ${noun}s"
    }
}
