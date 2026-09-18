package dev.lassen.herdr

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

private const val CLAUDE_TOOL = "claude"

/** Runs `/ide` in every running Claude Code agent pane, across all herdr sessions. */
class ConnectAgentsToIdeAction(
    private val project: Project,
    private val binaryPath: String,
) : AnAction("Connect All to IDE", "Runs /ide in every running Claude Code agent pane", AllIcons.Nodes.Plugin) {

    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val agents = HerdrCli.listAllAgents(binaryPath).filter { it.tool == CLAUDE_TOOL }
            agents.forEach { HerdrCli.connectAgentToIde(binaryPath, it.sessionName, it.paneId) }
            notify(agents.size)
        }
    }

    private fun notify(count: Int) {
        val message = if (count == 0) "No running Claude Code agents found" else "Connecting $count Claude Code agent(s) to the IDE"
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance().getNotificationGroup("Herdr")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
