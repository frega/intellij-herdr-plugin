package dev.lassen.herdr

import com.intellij.execution.process.ConsoleHighlighter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.jediterm.core.Color
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.emulator.ColorPalette
import com.jediterm.terminal.ui.AwtTransformers
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Font

// ANSI index -> scheme key, in SGR order (0=black..7=white, 8=bright black..15=bright white).
// Same keys as Settings > Editor > Color Scheme > Console Colors > ANSI Colors, so herdr's
// palette follows whatever the IDE's Console/Terminal already use.
private val ANSI_COLOR_KEYS: List<TextAttributesKey> = listOf(
    ConsoleHighlighter.BLACK,
    ConsoleHighlighter.RED,
    ConsoleHighlighter.GREEN,
    ConsoleHighlighter.YELLOW,
    ConsoleHighlighter.BLUE,
    ConsoleHighlighter.MAGENTA,
    ConsoleHighlighter.CYAN,
    ConsoleHighlighter.GRAY,
    ConsoleHighlighter.DARKGRAY,
    ConsoleHighlighter.RED_BRIGHT,
    ConsoleHighlighter.GREEN_BRIGHT,
    ConsoleHighlighter.YELLOW_BRIGHT,
    ConsoleHighlighter.BLUE_BRIGHT,
    ConsoleHighlighter.MAGENTA_BRIGHT,
    ConsoleHighlighter.CYAN_BRIGHT,
    ConsoleHighlighter.WHITE,
)

/**
 * JediTerm's own DefaultSettingsProvider hardcodes TerminalColor.BLACK-on-WHITE as its default
 * style and a fixed xterm palette, ignoring the IDE entirely. This mirrors what colors/font it
 * would use if it were theme-aware, sourced from the IDE's active EditorColorsScheme (the same
 * "Console Colors" the built-in Console/Terminal read) so herdr matches light or dark themes.
 *
 * Deliberately built on EditorColorsScheme/ConsoleViewContentType/ConsoleHighlighter (public,
 * decades-stable platform-api/ide-core classes) rather than the internal com.intellij.terminal.*
 * theming glue (e.g. TerminalUiSettingsManager) that the bundled Terminal plugin uses internally.
 */
class HerdrTerminalSettingsProvider : DefaultSettingsProvider() {

    private fun scheme(): EditorColorsScheme = EditorColorsManager.getInstance().globalScheme

    private fun foregroundOf(key: TextAttributesKey): java.awt.Color? = scheme().getAttributes(key).foregroundColor

    override fun getTerminalColorPalette(): ColorPalette = object : ColorPalette() {
        override fun getForegroundByColorIndex(colorIndex: Int): Color = colorFor(colorIndex)
        override fun getBackgroundByColorIndex(colorIndex: Int): Color = colorFor(colorIndex)

        private fun colorFor(colorIndex: Int): Color =
            AwtTransformers.fromAwtColor(foregroundOf(ANSI_COLOR_KEYS[colorIndex]) ?: scheme().defaultForeground)!!
    }

    override fun getDefaultForeground(): TerminalColor = AwtTransformers.fromAwtToTerminalColor(
        foregroundOf(ConsoleViewContentType.NORMAL_OUTPUT_KEY) ?: scheme().defaultForeground
    )!!

    override fun getDefaultBackground(): TerminalColor = AwtTransformers.fromAwtToTerminalColor(
        scheme().getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY) ?: scheme().defaultBackground
    )!!

    // Deprecated on the interface, but still what TerminalPanel actually paints the pane background
    // from; its own default implementation hardcodes TextStyle(BLACK, WHITE) rather than chaining to
    // getDefaultForeground()/getDefaultBackground() above (those two delegate to it, not vice versa).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getDefaultStyle(): TextStyle = TextStyle(getDefaultForeground(), getDefaultBackground())

    override fun getSelectionColor(): TextStyle = TextStyle(
        AwtTransformers.fromAwtToTerminalColor(scheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR)),
        AwtTransformers.fromAwtToTerminalColor(scheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR)),
    )

    override fun getFoundPatternColor(): TextStyle {
        val attrs = scheme().getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
        return TextStyle(
            AwtTransformers.fromAwtToTerminalColor(attrs.foregroundColor),
            AwtTransformers.fromAwtToTerminalColor(attrs.backgroundColor),
        )
    }

    override fun getHyperlinkColor(): TextStyle {
        val attrs = scheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)
        return TextStyle(
            AwtTransformers.fromAwtToTerminalColor(attrs.foregroundColor),
            AwtTransformers.fromAwtToTerminalColor(attrs.backgroundColor),
        )
    }

    override fun getTerminalFontSize(): Float = scheme().consoleFontSize2D

    override fun getTerminalFont(): Font = scheme().getFont(EditorFontType.CONSOLE_PLAIN).deriveFont(getTerminalFontSize())
}
