package dev.lassen.herdr

import com.intellij.openapi.project.Project
import com.intellij.terminal.pty.PtyProcessTtyConnector
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.pty4j.PtyProcessBuilder
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JPanel

/** Hosts one embedded herdr terminal, and can be re-pointed at a different `--session` in place. */
class HerdrTerminalPanel(
    private val project: Project,
    private val binaryPath: String,
    private val workingDirectory: String,
) : JPanel(BorderLayout()) {

    private var widget: ScrollbarlessTerminalWidget? = null

    var currentSession: String = ""
        private set

    fun attachSession(sessionName: String) {
        widget?.let { old ->
            remove(old)
            runCatching { old.ttyConnector?.close() }
            runCatching { old.close() }
        }

        val environment = HashMap(System.getenv()).apply { put("TERM", "xterm-256color") }
        val process = PtyProcessBuilder(arrayOf(binaryPath, "--session", sessionName))
            .setDirectory(workingDirectory)
            .setEnvironment(environment)
            .setInitialColumns(80)
            .setInitialRows(24)
            .start()

        val newWidget = ScrollbarlessTerminalWidget(80, 24, DefaultSettingsProvider())
        newWidget.addHyperlinkFilter(HerdrLinkFilter(project))
        newWidget.setTtyConnector(PtyProcessTtyConnector(process, StandardCharsets.UTF_8))
        newWidget.start()

        widget = newWidget
        currentSession = sessionName
        add(newWidget, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    fun dispose() {
        widget?.let {
            runCatching { it.ttyConnector?.close() }
            runCatching { it.close() }
        }
    }
}
