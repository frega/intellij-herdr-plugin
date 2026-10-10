<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

## [Unreleased]

### Added

- "Herdr" now shows up in the built-in Terminal tool window's "New Predefined Session" picker (the dropdown next to its `+` button) ([#3](https://github.com/frega/intellij-herdr-plugin/issues/3)), so you can open the current project's herdr session as a tab of the default terminal, the same way CLI-agent plugins like Junie or Claude Code do, instead of only in Herdr's own tool window.

### Fixed

- The "Open Herdr" shortcut now moves keyboard focus into the embedded herdr terminal instead of only bringing the tool window to the front ([#2](https://github.com/frega/intellij-herdr-plugin/issues/2)). Switching sessions or picking an agent from the Agents tab focuses the terminal as well.

### Changed

- The shortcut now toggles the tool window: pressing it while the herdr terminal has focus hides the tool window and returns focus to the editor. The action is renamed from "Open Herdr" to "Toggle Herdr"; its ID is unchanged, so custom keymap bindings keep working.
- New default shortcut: `Alt+Shift+H` on Windows/Linux and `⌃⌥⌘H` on macOS ([#1](https://github.com/frega/intellij-herdr-plugin/issues/1)). The old `Ctrl+Alt+H` took over Call Hierarchy on Windows/Linux, and on macOS became `⌥⌘H`, which the OS reserves for "Hide Others". The new one is unbound in all bundled keymaps; macOS uses Cmd because Option+letter types characters there.
- The shortcut now also works while the IDE's built-in Terminal has focus, which otherwise keeps every key except a fixed set of IDE actions. Uses the Terminal plugin's experimental `allowedActionsProvider` extension point, through an optional dependency, so Herdr still loads with the Terminal plugin disabled.

## [0.7.5] - 2026-09-21

### Fixed

- Shift+Enter in the embedded herdr terminal now inserts a newline instead of submitting early, matching the IDE's own built-in Terminal.

## [0.7.4] - 2026-09-21

### Fixed

- The embedded herdr terminal now follows the IDE's active color scheme (light or dark) instead of always rendering black text on a white background, regardless of theme.

## [0.7.3] - 2026-09-18

### Fixed

- The embedded herdr terminal no longer fails to start on IntelliJ Platform 2026.3+ builds (e.g. IU-263.5153.40) with `NoSuchMethodError`/unresolved-constructor errors. 0.7.2 relied on `com.intellij.terminal.pty.PtyProcessTtyConnector`, an internal platform class whose constructor signature changed between build 262 and 263; the plugin now implements its own minimal `TtyConnector` directly on the stable pty4j/JediTerm APIs instead.

## [0.7.2] - 2026-09-18

### Added

- "Connect All to IDE" title-bar action: runs `/ide` in every running Claude Code agent pane across all herdr sessions, so they connect to this IDE without typing `/ide` manually in each one.

### Fixed

- Clickable URLs in the embedded herdr terminal no longer swallow trailing punctuation from surrounding prose/markdown (e.g. a closing `)` when an agent writes `Report here (https://example.com/x)`), while URLs that legitimately end in a balanced bracket (e.g. a Wikipedia `_(disambiguation)` link) are left untouched.

## [0.7.1] - 2026-09-13

### Fixed

- Right-clicks in the embedded herdr terminal now actually reach herdr's own mouse handling. The 0.6.0 fix only stopped JediTerm's built-in copy/paste popup from covering it up; the bundled JediTerm's own button-code mapping treats the right button as unreportable, so it was silently dropping every right-click before it ever reached herdr's SGR mouse reporting. Right-clicks are now encoded and forwarded manually.

## [0.7.0] - 2026-08-22

### Added

- The Herdr logo now shows to the left of the "Terminal" and "Agents" tab labels in the tool window.

### Fixed

- Actually stopped Kotlin from generating `ToolWindowFactory` compatibility-bridge overrides for deprecated/experimental members (0.6.2 only suppressed the compiler warning about them; the bridges — including two for methods never even referenced in source — were still being emitted). Compiling with `-Xjvm-default=all` avoids them entirely, so the explicit override boilerplate could be removed too.

## [0.6.2] - 2026-08-22

### Changed

- Description/README no longer refer to PhpStorm exclusively; the plugin only relies on generic IntelliJ Platform APIs and should work in any IntelliJ Platform IDE.
- Install instructions now point to the GitLab Releases page instead of the wiki, with a screenshot of the "Install Plugin from Disk" menu.

### Fixed

- Removed JetBrains Marketplace-flagged deprecated/experimental `ToolWindowFactory` API usages (Kotlin was auto-generating bridge overrides that delegated into those APIs even though the plugin never called them); no behavior change.

## [0.6.1] - 2026-08-22

### Fixed

- Toggling "Show Agents list tab" in Settings now updates the Agents tab immediately in already-open projects, instead of only taking effect for newly opened ones.

## [0.6.0] - 2026-08-21

### Added

- Settings → Tools → Herdr: "Show Agents list tab" checkbox (off by default) to control whether the "Agents" tab appears in the Herdr tool window.

### Fixed

- Right-clicks in the embedded herdr terminal now reach herdr's own mouse handling instead of being swallowed by JediTerm's built-in copy/paste context menu.

## [0.5.1] - 2026-08-20

### Changed

- Settings → Tools → Herdr now shows whether the herdr binary was auto-detected (and where), or that it wasn't and needs to be set manually.
- Settings panel content is pinned to the top instead of appearing vertically centered on the page.

## [0.5.0] - 2026-08-20

### Added

- "Agents" tab in the Herdr tool window: a flat, auto-refreshing list of every herdr agent across all running sessions, sorted by attention (blocked → done → working → unknown → idle).
- Balloon notification when an agent newly becomes `blocked` or `done`.
- Double-clicking an agent in that list switches the Terminal tab to its session and focuses that pane in herdr.

## [0.4.0] - 2026-08-20

### Added

- Clickable links in the embedded herdr terminal: `http(s)://` URLs open in the system browser, and `path` or `path:line[:col]` tokens that resolve to a real file under the project root open in the PhpStorm editor at that line/column.

## [0.3.0] - 2026-08-20

### Changed

- Settings → Tools → Herdr: replaced the plain text field (which stretched to fill the whole panel height) with a single-line field plus a native file browse button.

## [0.2.0] - 2026-08-20

### Added

- Herdr tool window now attaches with `--session <name>`, defaulting to the project name and persisted per project.
- "Switch Herdr Session" title-bar action to list running herdr sessions and attach to one, or start a new named session.
- Default keyboard shortcut (`Ctrl+Alt+H`) to open/focus the Herdr tool window.

### Changed

- Hid JediTerm's scrollbar in the embedded terminal, since herdr's own TUI already manages scrollback.

## [0.1.0] - 2026-08-20

### Added

- Initial release: dedicated "Herdr" tool window embedding herdr via pty4j/JediTerm, separate from the built-in Terminal tool window.
- herdr binary auto-detection (PATH, common install dirs, login-shell fallback), with a Settings → Tools → Herdr override field.
