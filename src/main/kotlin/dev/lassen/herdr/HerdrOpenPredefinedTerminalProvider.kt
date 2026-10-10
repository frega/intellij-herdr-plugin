package dev.lassen.herdr

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import dev.lassen.herdr.settings.HerdrSessionState
import dev.lassen.herdr.settings.HerdrSettingsState
import org.jetbrains.plugins.terminal.ui.OpenPredefinedTerminalActionProvider

/**
 * Adds "Herdr" to the built-in Terminal tool window's "New Predefined Session" picker, the same way
 * CLI-agent plugins like Junie or Claude Code let you open their session as a tab of the default terminal
 * instead of (or alongside) a dedicated tool window.
 */
class HerdrOpenPredefinedTerminalProvider : OpenPredefinedTerminalActionProvider {
    override fun listOpenPredefinedTerminalActions(project: Project): List<AnAction> {
        HerdrBinaryLocator.resolve(HerdrSettingsState.getInstance().state.binaryPath) ?: return emptyList()
        return listOf(OpenHerdrInTerminalAction())
    }
}

private class OpenHerdrInTerminalAction : DumbAwareAction("Herdr", null, HerdrToolWindowFactory.TAB_ICON) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val binaryPath = HerdrBinaryLocator.resolve(HerdrSettingsState.getInstance().state.binaryPath) ?: return
        val contentManager = e.getData(PlatformDataKeys.TOOL_WINDOW_CONTENT_MANAGER) ?: return

        val workingDirectory = project.basePath ?: System.getProperty("user.home")
        val terminalPanel = HerdrTerminalPanel(project, binaryPath, workingDirectory)

        val sessionState = HerdrSessionState.getInstance(project)
        val sessionName = sessionState.state.sessionName.ifBlank { project.name }
        terminalPanel.attachSession(sessionName)
        sessionState.state.sessionName = sessionName

        val content = ContentFactory.getInstance().createContent(terminalPanel, "Herdr", false).apply {
            icon = HerdrToolWindowFactory.TAB_ICON
            isCloseable = true
            setDisposer(Disposable { terminalPanel.dispose() })
            setPreferredFocusedComponent { terminalPanel.focusTarget }
        }
        contentManager.addContent(content)
        contentManager.setSelectedContent(content, true)
    }
}
