<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

## [Unreleased]

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
