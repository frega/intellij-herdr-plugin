package dev.lassen.herdr

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
import dev.lassen.herdr.settings.HerdrSettingsState
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * One-time notice for the 0.8.0 change of the default "Toggle Herdr" shortcut from Ctrl+Alt+H (which
 * clashes with Call Hierarchy) to Alt+Shift+H, offering to keep the old key as well. Not shown on macOS,
 * where the old default arrived as ⌥⌘H and was swallowed by the OS's "Hide Others".
 */
class HerdrShortcutNotice : ProjectActivity {

    override suspend fun execute(project: Project) {
        val settings = HerdrSettingsState.getInstance().state
        if (settings.shortcutNoticeShown || SystemInfo.isMac) return
        settings.shortcutNoticeShown = true
        if (OLD_SHORTCUT in KeymapManagerEx.getInstanceEx().activeKeymap.getShortcuts(ACTION_ID)) return

        NotificationGroupManager.getInstance().getNotificationGroup("Herdr")
            .createNotification(
                "Toggle Herdr is now Alt+Shift+H",
                "The previous default, Ctrl+Alt+H, clashed with Call Hierarchy. " +
                    "You can keep it as well; Call Hierarchy then loses Ctrl+Alt+H.",
                NotificationType.INFORMATION,
            )
            .addAction(NotificationAction.createSimpleExpiring("Keep Ctrl+Alt+H too") { keepOldShortcut() })
            .notify(project)
    }

    private companion object {
        const val ACTION_ID = "dev.lassen.herdr.OpenHerdr"

        val OLD_SHORTCUT = KeyboardShortcut(
            KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK),
            null,
        )

        /** Binds Ctrl+Alt+H to Toggle Herdr in the active keymap, taking it away from any other action. */
        fun keepOldShortcut() {
            val manager = KeymapManagerEx.getInstanceEx()
            var keymap = manager.activeKeymap
            // Bundled keymaps are read-only; edit a copy, as the Keymap settings page does.
            if (!keymap.canModify()) {
                val names = manager.allKeymaps.map { it.name }.toSet()
                val base = "${keymap.presentableName} copy"
                val name = generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base $it" }.first { it !in names }
                keymap = keymap.deriveKeymap(name)
                manager.schemeManager.addScheme(keymap)
                manager.setActiveKeymap(keymap)
            }
            keymap.getActionIds(OLD_SHORTCUT).filter { it != ACTION_ID }.forEach { keymap.removeShortcut(it, OLD_SHORTCUT) }
            keymap.addShortcut(ACTION_ID, OLD_SHORTCUT)
        }
    }
}
