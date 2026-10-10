package dev.lassen.herdr

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.pty4j.PtyProcessBuilder
import dev.lassen.herdr.settings.HerdrSessionState
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JPanel

/** Hosts one embedded herdr terminal, and can be re-pointed at a different `--session` in place. */
class HerdrTerminalPanel(
    private val project: Project,
    val binaryPath: String,
    private val workingDirectory: String,
) : JPanel(BorderLayout()) {

    private var widget: ScrollbarlessTerminalWidget? = null
    private var placeholder: JComponent? = null

    var currentSession: String = ""
        private set

    /** True while showing the "suspended" placeholder instead of a terminal. */
    val isSuspended: Boolean
        get() = placeholder != null

    /** Attaching starts the session if it isn't running, so this also clears a suspended state. */
    fun attachSession(sessionName: String) {
        detach()

        HerdrSessionState.getInstance(project).state.suspended = false

        val environment = HashMap(System.getenv()).apply { put("TERM", "xterm-256color") }
        val process = PtyProcessBuilder(arrayOf(binaryPath, "--session", sessionName))
            .setDirectory(workingDirectory)
            .setEnvironment(environment)
            .setInitialColumns(80)
            .setInitialRows(24)
            .start()

        val newWidget = ScrollbarlessTerminalWidget(80, 24, HerdrTerminalSettingsProvider())
        newWidget.addHyperlinkFilter(HerdrLinkFilter(project))
        newWidget.setTtyConnector(HerdrTtyConnector(process, StandardCharsets.UTF_8))
        newWidget.start()

        widget = newWidget
        currentSession = sessionName
        add(newWidget, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    /** Drops the terminal (without starting [sessionName]) and offers to reawake it. */
    fun showSuspended(sessionName: String) {
        detach()

        val newPlaceholder = JBPanelWithEmptyText().apply {
            emptyText.text = "herdr session '$sessionName' is suspended"
            emptyText.appendLine("Its layout and agent sessions are restored when it starts again.")
            emptyText.appendLine("Reawake session", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                attachSession(sessionName)
            }
        }
        placeholder = newPlaceholder
        currentSession = sessionName
        add(newPlaceholder, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun detach() {
        placeholder?.let {
            remove(it)
            placeholder = null
        }
        widget?.let { old ->
            remove(old)
            runCatching { old.ttyConnector?.close() }
            runCatching { old.close() }
            widget = null
        }
    }

    fun dispose() {
        widget?.let {
            runCatching { it.ttyConnector?.close() }
            runCatching { it.close() }
        }
    }
}
