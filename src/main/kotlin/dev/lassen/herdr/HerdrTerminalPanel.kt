package dev.lassen.herdr

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.pty4j.PtyProcessBuilder
import dev.lassen.herdr.settings.HerdrSessionState
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/** Hosts one embedded herdr terminal, and can be re-pointed at a different `--session` in place. */
class HerdrTerminalPanel(
    private val project: Project,
    val binaryPath: String,
    private val workingDirectory: String,
) : JPanel(BorderLayout()) {

    /** What the panel shows: the terminal, or one of the native placeholders that replace it. */
    enum class Mode { ATTACHED, SHUTTING_DOWN, SUSPENDED, DETACHED }

    private var widget: ScrollbarlessTerminalWidget? = null
    private var placeholder: JComponent? = null

    var currentSession: String = ""
        private set

    var mode: Mode = Mode.ATTACHED
        private set

    /** True while showing the "suspended" placeholder instead of a terminal. */
    val isSuspended: Boolean
        get() = mode == Mode.SUSPENDED

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
        mode = Mode.ATTACHED
        add(newWidget, BorderLayout.CENTER)
        revalidate()
        repaint()

        process.onExit().thenRun {
            ApplicationManager.getApplication().invokeLater { onClientExited(newWidget, sessionName) }
        }
    }

    /**
     * The herdr client exits as soon as its server starts shutting down, e.g. when the session is stopped
     * from another window, the CLI or herdr's own quit. Rather than leave a dead terminal reading
     * "server is shutting down", show the shutting-down screen until the session has stopped, then the
     * suspended one. Exits caused by [detach] are ignored because the widget has already been replaced.
     */
    private fun onClientExited(exitedWidget: ScrollbarlessTerminalWidget, sessionName: String) {
        if (project.isDisposed || widget !== exitedWidget) return
        showShuttingDown(sessionName)
        awaitStopped(sessionName)
    }

    /**
     * Swaps the terminal for a spinner while the session stops, so herdr's shutdown output never shows.
     * [SuspendHerdrSessionAction] stops the session itself and calls [showSuspended] when done.
     */
    fun showShuttingDown(sessionName: String) {
        showPlaceholder(
            JBLabel("herdr session '$sessionName' is shutting down…", AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER),
            Mode.SHUTTING_DOWN,
            sessionName,
        )
    }

    /** Polls until [sessionName] is no longer running, then shows it as suspended (or as detached if it never stops). */
    private fun awaitStopped(sessionName: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS
            var stopped = false
            while (!stopped && System.currentTimeMillis() < deadline) {
                stopped = HerdrCli.listSessions(binaryPath).none { it.name == sessionName && it.running }
                if (!stopped) Thread.sleep(SHUTDOWN_POLL_MS)
            }
            ApplicationManager.getApplication().invokeLater {
                // Something else (reawake, a session switch) took over the panel in the meantime.
                if (project.isDisposed || mode != Mode.SHUTTING_DOWN || currentSession != sessionName) return@invokeLater
                if (stopped) {
                    showSuspended(sessionName)
                    HerdrSessionState.getInstance(project).state.suspended = true
                } else {
                    showDetached(sessionName)
                }
            }
        }
    }

    /** Drops the terminal (without starting [sessionName]) and offers to reawake it. */
    fun showSuspended(sessionName: String) {
        showPlaceholder(
            JBPanelWithEmptyText().apply {
                emptyText.text = "herdr session '$sessionName' is suspended"
                emptyText.appendLine("Its layout and agent sessions are restored when it starts again.")
                emptyText.appendLine("Reawake session", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                    attachSession(sessionName)
                    focusTerminal()
                }
            },
            Mode.SUSPENDED,
            sessionName,
        )
    }

    /** The client exited but the session kept running (e.g. herdr's detach); offer to attach again. */
    private fun showDetached(sessionName: String) {
        showPlaceholder(
            JBPanelWithEmptyText().apply {
                emptyText.text = "Disconnected from herdr session '$sessionName'"
                emptyText.appendLine("Reattach", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                    attachSession(sessionName)
                    focusTerminal()
                }
            },
            Mode.DETACHED,
            sessionName,
        )
    }

    private fun showPlaceholder(component: JComponent, newMode: Mode, sessionName: String) {
        detach()
        placeholder = component
        currentSession = sessionName
        mode = newMode
        add(component, BorderLayout.CENTER)
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

    /** The component that receives keystrokes; changes whenever [attachSession] swaps the widget. */
    val focusTarget: JComponent?
        get() = widget?.terminalPanel

    fun focusTerminal() {
        focusTarget?.let { IdeFocusManager.getInstance(project).requestFocus(it, true) }
    }

    fun dispose() {
        detach()
    }

    private companion object {
        const val SHUTDOWN_POLL_MS = 250L
        const val SHUTDOWN_TIMEOUT_MS = 15_000L
    }
}
