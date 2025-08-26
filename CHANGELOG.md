# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2025-08-27
### Added
- ZIP and TAR distribution packages for cross-platform deployment
- Automated build script (`scripts/build_release.bat`) for creating distribution packages
- **SHA-256 checksum generation** - Automatic generation of `.sha256` files for distribution integrity verification
- Updated README with installation instructions for different platforms
- Enhanced build.gradle with custom distribution tasks and checksum generation

### Changed
- **Distribution method changed from jpackage to ZIP/TAR format**
- Improved cross-platform compatibility (Windows ZIP, Linux/macOS TAR)
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

### Security
- Added SHA-256 checksums for all distribution files to ensure download integrity
- Users can verify file integrity using: `certutil -hashfile <filename> SHA256` (Windows) or `sha256sum -c <filename>.sha256` (Linux/macOS)

## [Unreleased]
- Future improvements: optional JRE-bundled installer using jpackage (when environment permits)
- Code signing for enhanced security
