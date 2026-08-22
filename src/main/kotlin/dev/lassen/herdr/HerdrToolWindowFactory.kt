package dev.lassen.herdr

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import dev.lassen.herdr.settings.HerdrSessionState
import dev.lassen.herdr.settings.HerdrSettingsState
import javax.swing.JLabel
import javax.swing.JPanel

class HerdrToolWindowFactory : ToolWindowFactory {

    companion object {
        private const val TOOL_WINDOW_ID = "Herdr"

        /** Shown to the left of each content tab's label; IntelliJ picks the _dark variant automatically. */
        private val TAB_ICON = IconLoader.getIcon("/icons/herdr_logo.svg", HerdrToolWindowFactory::class.java)

        /** Called after the "Show Agents list tab" setting changes, to reflect it in already-open projects. */
        fun syncAgentsTabForAllProjects() {
            for (project in ProjectManager.getInstance().openProjects) {
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: continue
                syncAgentsTab(project, toolWindow)
            }
        }

        private fun syncAgentsTab(project: Project, toolWindow: ToolWindow) {
            val contentManager = toolWindow.contentManager
            val terminalContent = contentManager.contents.find { it.component is HerdrTerminalPanel } ?: return
            val terminalPanel = terminalContent.component as HerdrTerminalPanel
            val existingAgentsContent = contentManager.contents.find { it.component is HerdrAgentsPanel }

            if (HerdrSettingsState.getInstance().state.showAgentsList) {
                if (existingAgentsContent != null) return
                val sessionState = HerdrSessionState.getInstance(project)
                val agentsPanel = HerdrAgentsPanel(project, terminalPanel.binaryPath) { agent ->
                    terminalPanel.attachSession(agent.sessionName)
                    sessionState.state.sessionName = agent.sessionName
                    HerdrCli.focusAgent(terminalPanel.binaryPath, agent.sessionName, agent.paneId)
                    contentManager.setSelectedContent(terminalContent)
                }
                val agentsContent = ContentFactory.getInstance().createContent(agentsPanel, "Agents", false).apply {
                    icon = TAB_ICON
                    setDisposer(agentsPanel)
                }
                contentManager.addContent(agentsContent)
            } else {
                existingAgentsContent?.let { contentManager.removeContent(it, true) }
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val binaryPath = HerdrBinaryLocator.resolve(HerdrSettingsState.getInstance().state.binaryPath)
        if (binaryPath == null) {
            toolWindow.contentManager.addContent(createBinaryNotFoundContent())
            return
        }

        val workingDirectory = project.basePath ?: System.getProperty("user.home")
        val terminalPanel = HerdrTerminalPanel(project, binaryPath, workingDirectory)

        val sessionState = HerdrSessionState.getInstance(project)
        val sessionName = sessionState.state.sessionName.ifBlank { project.name }
        terminalPanel.attachSession(sessionName)
        sessionState.state.sessionName = sessionName

        val contentManager = toolWindow.contentManager
        val terminalContent = ContentFactory.getInstance().createContent(terminalPanel, "Terminal", false).apply {
            icon = TAB_ICON
            setDisposer(Disposable { terminalPanel.dispose() })
        }
        contentManager.addContent(terminalContent)

        syncAgentsTab(project, toolWindow)

        toolWindow.setTitleActions(listOf(SwitchHerdrSessionAction(project, binaryPath, terminalPanel)))
    }

    private fun createBinaryNotFoundContent() = run {
        val panel = JPanel().apply {
            add(JLabel("herdr binary not found. Set its path in Settings → Tools → Herdr."))
        }
        ContentFactory.getInstance().createContent(panel, null, false)
    }

    override fun shouldBeAvailable(project: Project) = true
}
