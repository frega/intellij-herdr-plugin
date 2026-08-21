package dev.lassen.herdr

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.lassen.herdr.settings.HerdrSessionState
import dev.lassen.herdr.settings.HerdrSettingsState
import javax.swing.JLabel
import javax.swing.JPanel

class HerdrToolWindowFactory : ToolWindowFactory {

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
            setDisposer(Disposable { terminalPanel.dispose() })
        }
        contentManager.addContent(terminalContent)

        if (HerdrSettingsState.getInstance().state.showAgentsList) {
            val agentsPanel = HerdrAgentsPanel(project, binaryPath) { agent ->
                terminalPanel.attachSession(agent.sessionName)
                sessionState.state.sessionName = agent.sessionName
                HerdrCli.focusAgent(binaryPath, agent.sessionName, agent.paneId)
                contentManager.setSelectedContent(terminalContent)
            }
            val agentsContent = ContentFactory.getInstance().createContent(agentsPanel, "Agents", false).apply {
                setDisposer(agentsPanel)
            }
            contentManager.addContent(agentsContent)
        }

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
