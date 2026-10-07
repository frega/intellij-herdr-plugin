package dev.lassen.herdr

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import dev.lassen.herdr.settings.HerdrSessionState

private const val NEW_SESSION_ENTRY = "New session…"

class SwitchHerdrSessionAction(
    private val project: Project,
    private val binaryPath: String,
    private val panel: HerdrTerminalPanel,
) : AnAction("Switch Herdr Session", "Attach this tool window to a different herdr session", AllIcons.Vcs.Branch) {

    override fun actionPerformed(e: AnActionEvent) {
        val entries = HerdrCli.listSessions(binaryPath).map { it.name } + NEW_SESSION_ENTRY
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(entries)
            .setTitle("Attach to Herdr Session (current: ${panel.currentSession})")
            .setItemChosenCallback { choice -> attach(choice) }
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    private fun attach(choice: String) {
        val sessionName = if (choice == NEW_SESSION_ENTRY) {
            Messages.showInputDialog(project, "Session name:", "New Herdr Session", null)
        } else {
            choice
        }
        if (!sessionName.isNullOrBlank()) {
            panel.attachSession(sessionName)
            panel.focusTerminal()
            HerdrSessionState.getInstance(project).state.sessionName = sessionName
        }
    }
}
