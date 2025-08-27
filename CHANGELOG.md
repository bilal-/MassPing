# Changelog

All notable changes to MassPing will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2025-08-26

### Changed
- Initial release of Mass Ping app


## [1.1.0] - 2024-08-27

### Added
- Automated release management system with GitHub integration
- Professional release workflow with semantic versioning
- Release branch strategy for hotfixes and feature releases
- CHANGELOG.md with structured release documentation


## [1.0.0] - 2024-08-27

### Added
- Initial implementation of MassPing Android app
- Mass SMS broadcasting functionality with personalized messages
- Contact management with multi-SIM support
- Modern Material 3 UI built with Jetpack Compose
- MVVM architecture with Repository pattern
- Room database for local contact caching and message history
- Runtime permission handling for SMS and contacts
- Background services for reliable message delivery
- Message personalization with dynamic placeholders (name, phone, etc.)
- Multi-SIM and phone account support
- Professional branding with signal wave logo design
- Debug and release build variants with signing configuration
- Comprehensive project documentation

### Technical Details
- **Architecture**: Clean MVVM with Repository pattern
- **UI**: Jetpack Compose with Material 3
- **Database**: Room for local storage
- **Concurrency**: Kotlin Coroutines
- **Dependencies**: Navigation, Permissions, Gson
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Android 14 (API 36)

[Unreleased]: https://github.com/bilal-/MassPing/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/bilal-/MassPing/releases/tag/v1.2.0
[1.1.0]: https://github.com/bilal-/MassPing/releases/tag/v1.1.0
[1.0.0]: https://github.com/bilal-/MassPing/releases/tag/v1.0.0
