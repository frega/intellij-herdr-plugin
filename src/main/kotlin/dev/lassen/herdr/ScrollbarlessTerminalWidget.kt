package dev.lassen.herdr

import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.SettingsProvider
import java.awt.Dimension
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
}
