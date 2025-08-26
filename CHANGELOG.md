# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2025-08-27
### Added
- ZIP and TAR.GZ distribution packages for cross-platform deployment
- Automated build script (`scripts/build_release.bat`) for creating distribution packages
- Updated README with installation instructions for different platforms
- Enhanced build.gradle with custom distribution tasks

### Changed
- **Distribution method changed from jpackage to ZIP/TAR.GZ format**
- Improved cross-platform compatibility (Windows ZIP, Linux/macOS TAR.GZ)
- Table header vertical divider color unified in Dark Mode to `#404040`
- ComboBox and Tab fonts now prioritize Japanese fonts (Yu Gothic UI / Meiryo / Noto Sans JP) to prevent mojibake
- Dark mode fillers and column-resize guide line aligned with table grid color
- Simplified deployment process - no longer requires WiX toolset or jpackage configuration

### Fixed
- Garbled text in dropdown (unselected) and tabs in Dark Mode
- Unnatural thick vertical line in table header (color unified to match grid)
- JDK dependency issues with jpackage by switching to standard Java distribution format

### Removed
- jpackage plugin dependency and related configuration
- WiX toolset requirement for Windows installer creation

## [Unreleased]
- Future improvements: optional JRE-bundled installer using jpackage (when environment permits)
- Code signing for enhanced security
