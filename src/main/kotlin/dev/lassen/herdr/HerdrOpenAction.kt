package dev.lassen.herdr

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class HerdrOpenAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herdr") ?: return
        // Toggle: hide when herdr already has focus; otherwise show it (if needed) and focus the terminal.
        if (toolWindow.isVisible && toolWindow.isActive) {
            toolWindow.hide(null)
            return
        }
        // Content is created lazily on first show, so look up the terminal tab only once activated.
        toolWindow.activate({
            val contentManager = toolWindow.contentManager
            val terminalContent = contentManager.contents.find { it.component is HerdrTerminalPanel } ?: return@activate
            contentManager.setSelectedContent(terminalContent, true)
            (terminalContent.component as HerdrTerminalPanel).focusTerminal()
        }, true, true)
    }
}
