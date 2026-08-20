<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

## [Unreleased]

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
