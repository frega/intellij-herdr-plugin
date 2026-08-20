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
        val panel = HerdrTerminalPanel(binaryPath, workingDirectory)

        val sessionState = HerdrSessionState.getInstance(project)
        val sessionName = sessionState.state.sessionName.ifBlank { project.name }
        panel.attachSession(sessionName)
        sessionState.state.sessionName = sessionName

        val content = ContentFactory.getInstance().createContent(panel, null, false).apply {
            setDisposer(Disposable { panel.dispose() })
        }
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(listOf(SwitchHerdrSessionAction(project, binaryPath, panel)))
    }

    private fun createBinaryNotFoundContent() = run {
        val panel = JPanel().apply {
            add(JLabel("herdr binary not found. Set its path in Settings → Tools → Herdr."))
        }
        ContentFactory.getInstance().createContent(panel, null, false)
    }

    override fun shouldBeAvailable(project: Project) = true
}
