package dev.lassen.herdr

import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.TerminalActionProvider
import com.jediterm.terminal.ui.TerminalPanel
import com.jediterm.terminal.ui.settings.SettingsProvider
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.nio.charset.StandardCharsets
import javax.swing.JPopupMenu
import javax.swing.JScrollBar

/**
 * JediTermWidget lays the scrollbar out at whatever width [createScrollBar] returns as its
 * preferred size, so reporting zero hides it without touching the shared scroll model
 * (mouse wheel / Page Up/Down keep working — herdr has its own scrollback UI anyway).
 */
class ScrollbarlessTerminalWidget(
    columns: Int,
    lines: Int,
    settingsProvider: SettingsProvider,
) : JediTermWidget(columns, lines, settingsProvider) {

    override fun createScrollBar(): JScrollBar = object : JScrollBar() {
        override fun getPreferredSize(): Dimension = Dimension(0, 0)
    }

    // JediTerm's TerminalPanel always pops up its own copy/paste context menu on right-click,
    // regardless of the app's mouse mode, which would otherwise cover up herdr's own right-click
    // handling. Suppressing it is necessary but not sufficient: the bundled TerminalPanel's own
    // AWT-button-code mapping treats the right button as unreportable (maps it to -1, its "no
    // button" sentinel), so JediTerminal silently drops every right mousePressed/mouseReleased
    // before it ever reaches herdr's SGR mouse reporting (it enables
    // `\e[?1000h`/`?1002h`/`?1003h`/`?1006h`). processMouseEvent below forwards right-clicks to
    // herdr manually, encoding the SGR report ourselves, since the library can't.
    override fun createTerminalPanel(
        settingsProvider: SettingsProvider,
        styleState: StyleState,
        textBuffer: TerminalTextBuffer,
    ): TerminalPanel = object : TerminalPanel(settingsProvider, textBuffer, styleState) {
        override fun createPopupMenu(actionProvider: TerminalActionProvider): JPopupMenu =
            object : JPopupMenu() {
                override fun show(invoker: Component, x: Int, y: Int) = Unit
            }

        override fun processMouseEvent(e: MouseEvent) {
            if (e.button == MouseEvent.BUTTON3 &&
                (e.id == MouseEvent.MOUSE_PRESSED || e.id == MouseEvent.MOUSE_RELEASED) &&
                isRemoteMouseAction(e)
            ) {
                sendRightClickSgrReport(e)
            }
            super.processMouseEvent(e)
        }

        private fun sendRightClickSgrReport(e: MouseEvent) {
            val column = e.x / myCharSize.width + 1
            val row = e.y / myCharSize.height + 1
            var buttonCode = 2 // right button, per the SGR mouse protocol
            if (e.isShiftDown) buttonCode = buttonCode or 4
            if (e.isAltDown) buttonCode = buttonCode or 8
            if (e.isControlDown) buttonCode = buttonCode or 16
            val kind = if (e.id == MouseEvent.MOUSE_PRESSED) 'M' else 'm'
            val report = "\u001b[<$buttonCode;$column;$row$kind"
            terminalOutputStream.sendBytes(report.toByteArray(StandardCharsets.US_ASCII), true)
        }
    }
}
