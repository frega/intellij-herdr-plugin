package dev.lassen.herdr

import com.intellij.terminal.frontend.view.TerminalAllowedActionsProvider

/**
 * The built-in Terminal swallows every keystroke except a fixed allowlist of IDE actions, so without
 * this the "Toggle Herdr" shortcut does nothing while the built-in Terminal has focus.
 */
class HerdrTerminalAllowedActionsProvider : TerminalAllowedActionsProvider {
    override fun getActionIds(): List<String> = listOf("dev.lassen.herdr.OpenHerdr")
}
