# Changelog

All notable changes to MassPing will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2025-08-28

### Added
- Intelligent SMS message splitting with natural boundary detection
- Real-time progress tracking showing current recipient and message part
- Wake lock functionality to keep screen on during SMS sending
- Auto-navigation to New Message tab when editing templates
- Enhanced message composition with unlimited character input
- Sequential message part delivery per recipient

### Changed
- Removed 160-character limit from message composition UI
- Enhanced progress display with detailed sending status
- Improved template copying and editing workflow
- Better phone number validation and normalization

### Fixed
- Template editing issues when copying long messages from history
- LaunchedEffect dependency causing state update problems
- Trailing whitespace cleanup across all source files

### Technical Improvements
- Added WAKE_LOCK permission for screen management during SMS sending
- Enhanced MessagePersonalizationService with intelligent text splitting algorithm
- Updated SmsService with grouped message sending and wake lock functionality
- Improved UI feedback with real-time "Sending to NUMBER (Part X/Y)" status
- Better error handling and validation in SMS processing


## [1.1.0] - 2025-08-27

### Changed
- Background sms sending and fix status tracking


### Added
- Background SMS sending with foreground service and rich notifications
- Configurable SMS settings with delay (1-30 seconds) and timeout (5-60 seconds) controls
- Real-time UI updates that work in both foreground and background modes
- Message history creation and persistent storage in Room database
- Immediate send button responsiveness with instant status updates
- Progress notifications showing percentage, sent count, and remaining messages
- Settings screen with intuitive sliders for SMS configuration

### Changed
- Redesigned SMS architecture to use single SmsService instance with singleton repository pattern
- Improved message status tracking with StateFlow for reactive UI updates
- Enhanced notification system with detailed progress and completion status
- Updated SmsService to use modern `context.getSystemService(SmsManager::class.java)` API
- Fixed LinearProgressIndicator deprecation warnings with lambda syntax

### Fixed
- UI not updating when using background SMS service
- Message history not being saved after SMS completion
- Status updates not flowing between background service and main app
- Send button responsiveness issues
- Duplicate SMS service instances causing status update conflicts

### Technical Improvements
- Implemented singleton repository pattern for consistent state management
- Removed fragile broadcast communication in favor of shared StateFlow
- Added comprehensive logging for debugging status update flow
- Eliminated trailing whitespaces across all source files
- Simplified background service architecture with direct repository integration

## [1.0.2] - 2025-08-27

### Changed
- Re-build with target sdk set to 36


## [1.0.1] - 2025-08-27

### Changed
- Initial release of MassPing Android app


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
[1.0.2]: https://github.com/bilal-/MassPing/releases/tag/v1.0.2
[1.0.1]: https://github.com/bilal-/MassPing/releases/tag/v1.0.1
[1.2.0]: https://github.com/bilal-/MassPing/releases/tag/v1.2.0
[1.1.0]: https://github.com/bilal-/MassPing/releases/tag/v1.1.0
[1.0.0]: https://github.com/bilal-/MassPing/releases/tag/v1.0.0
