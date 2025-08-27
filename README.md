# MassPing

**Author:** Bilal Ahmad  
**Version:** 1.0.0  
**Platform:** Android (API 29+)

A powerful Android application for sending personalized bulk SMS messages to your contacts. MassPing integrates with your Google contacts and local device contacts to enable efficient mass messaging with personalization features.

## 🚀 Features

### Core Functionality
- **📱 Bulk SMS Messaging** - Send personalized messages to multiple contacts simultaneously
- **🔗 Google Contacts Integration** - Sync and access your Google contacts seamlessly
- **📋 Contact Groups Support** - Organize and message entire contact groups
- **✨ Message Personalization** - Use placeholders like `{name}`, `{nickname}`, `{firstname}` for personalized messages
- **📊 Message Preview** - Preview personalized messages before sending
- **📈 Delivery Tracking** - Monitor message delivery status and progress
- **🔄 Background Processing** - Send messages in the background with foreground service

### User Experience
- **🎨 Modern UI** - Built with Jetpack Compose and Material 3 design
- **🔐 Google Sign-In** - Secure authentication with Google account
- **⚡ Real-time Updates** - Live progress tracking and status updates
- **🔔 Smart Notifications** - Progress notifications during bulk sending
- **📱 Responsive Design** - Optimized for various screen sizes

### Technical Features
- **🏗️ MVVM Architecture** - Clean architecture with ViewModels and Repository pattern
- **💾 Local Database** - Room database for offline message storage
- **🔄 Coroutines** - Asynchronous operations with Kotlin Coroutines
- **🎯 Permission Management** - Smart permission handling for SMS and contacts
- **🛡️ Error Handling** - Comprehensive error handling and user feedback

## 🛠️ Technical Stack

### Core Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM with Repository Pattern
- **Database:** Room (SQLite)
- **Async:** Kotlin Coroutines + Flow

### Key Dependencies
- **Jetpack Compose** - Modern Android UI toolkit
- **Room Database** - Local data persistence
- **Google Play Services Auth** - Google Sign-In functionality
- **Google People API** - Google Contacts integration
- **Navigation Compose** - In-app navigation
- **ViewModel Compose** - State management
- **Accompanist Permissions** - Runtime permission handling

## 📋 Requirements

### System Requirements
- **Android OS:** API level 29 (Android 10) or higher
- **Target SDK:** 36
- **Java Version:** 21
- **Kotlin:** 2.2.10

### Permissions Required
- `SEND_SMS` - Send SMS messages
- `READ_CONTACTS` - Access device contacts
- `GET_ACCOUNTS` - Access Google account information
- `READ_PHONE_STATE` - Monitor SMS delivery status
- `FOREGROUND_SERVICE` - Run background SMS service
- `POST_NOTIFICATIONS` - Show progress notifications (API 33+)

### Hardware Features
- **Telephony** - SMS sending capability (optional for tablets)

## 🚀 Getting Started

### Prerequisites
1. **Android Studio** - Latest stable version
2. **Java 21** - For development and compilation
3. **Google Console Account** - For Google People API access
4. **Physical Android Device** - For SMS testing (emulator can't send real SMS)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd MassPing
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned MassPing directory

3. **Configure Google API**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one
   - Enable the People API
   - Create OAuth 2.0 credentials for Android
   - Add your app's SHA-1 fingerprint

4. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Development Setup

```bash
# Clean build
./gradlew clean

# Run tests
./gradlew test

# Generate signed APK
./gradlew assembleRelease
```

## 📱 Usage

### First Time Setup
1. **Launch the app** and sign in with your Google account
2. **Grant permissions** for SMS, contacts, and notifications
3. **Sync contacts** from your Google account or use device contacts

### Sending Messages
1. **Navigate to New Message** screen
2. **Select recipients** - individual contacts or entire groups
3. **Compose your message** with personalization placeholders:
   - `{name}` - Contact's display name (nickname if available, otherwise full name)
   - `{nickname}` - Contact's nickname
   - `{firstname}` - Contact's first name
4. **Preview messages** to see personalized content
5. **Send** - Messages will be sent in the background with progress tracking

### Managing Contacts
- **View contacts** from both Google and device storage
- **Create contact groups** for easier bulk messaging
- **Update contact information** including nicknames for personalization

## 🏗️ Project Structure

```
app/src/main/java/dev/bilalahmad/massping/
├── MainActivity.kt                 # Main application entry point
├── data/
│   ├── database/                  # Room database implementation
│   │   ├── MessageDao.kt         # Database access object
│   │   ├── MessageDatabase.kt    # Database configuration
│   │   └── MessageEntities.kt    # Database entities
│   ├── models/                   # Data models
│   │   ├── Contact.kt           # Contact data model
│   │   ├── ContactGroup.kt      # Contact group model
│   │   └── Message.kt           # Message models
│   ├── repository/              # Repository pattern implementation
│   │   └── MassPingRepository.kt # Main data repository
│   └── services/                # Background services
│       ├── BackgroundSmsService.kt      # SMS sending service
│       ├── GoogleContactsService.kt     # Google API integration
│       ├── MessagePersonalizationService.kt # Message personalization
│       └── SmsService.kt               # SMS functionality
├── ui/                          # User interface components
│   ├── MassPingApp.kt          # Main app composition
│   ├── components/             # Reusable UI components
│   ├── screens/                # App screens
│   │   ├── ContactsScreen.kt   # Contacts management
│   │   ├── LoginScreen.kt      # Google Sign-In
│   │   ├── MessagesScreen.kt   # Message history
│   │   └── NewMessageScreen.kt # Message composition
│   ├── theme/                  # Material 3 theming
│   ├── utils/                  # UI utilities
│   └── viewmodels/             # State management
       └── MainViewModel.kt     # Main ViewModel
```

## 🔧 Configuration

### Build Configuration
- **Compile SDK:** 36
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36
- **Version Code:** 1
- **Version Name:** "1.0"

### ProGuard
The app includes ProGuard configuration for release builds to optimize and obfuscate code.

### Packaging
Excludes conflicting META-INF files from Google API dependencies for proper APK generation.

## 🧪 Testing

The project includes both unit tests and instrumented tests:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests
./gradlew test
```

## 🚀 Release Management

MassPing uses an automated release management system with semantic versioning.

### Version Management

The project follows [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH):
- **PATCH**: Bug fixes (1.0.0 → 1.0.1)
- **MINOR**: New features (1.0.0 → 1.1.0)
- **MAJOR**: Breaking changes (1.0.0 → 2.0.0)

### Automated Release Process

Use the release script to automate the entire release workflow:

```bash
# Feature release
./scripts/release.sh minor "Add SMS scheduling and templates"

# Bug fix release
./scripts/release.sh patch "Fix Android 14 permission issues"

# Hotfix from existing release branch
./scripts/release.sh patch "Critical SMS delivery fix" --hotfix

# Major release (manual decision only)
./scripts/release.sh major "Complete architecture overhaul"
```

### What the Release Script Does

The automated release process handles:

1. **📋 Version Management**: Updates `build.gradle.kts` version and code
2. **📝 Changelog**: Updates `CHANGELOG.md` with release notes and date
3. **🌳 Git Flow**: Creates release branches (`release/vX.Y.Z`)
4. **🏗️ Build Process**: Builds and tests both debug and release APKs
5. **🏷️ Git Tags**: Creates and pushes version tags
6. **📦 GitHub Releases**: Creates GitHub releases with APK uploads
7. **🔄 Branch Management**: Merges release branch back to main
8. **📱 APK Distribution**: Uploads signed APKs to GitHub releases

### Prerequisites for Releases

1. **GitHub CLI**: Install and authenticate
   ```bash
   brew install gh
   gh auth login
   ```

2. **Keystore**: Release keystore should be in `~/keystores/massping-release-key.jks`

3. **Clean Working Directory**: Commit or stash any pending changes

### Release Branch Strategy

- **Normal Releases**: `main` → `release/vX.Y.Z` → merge back to `main`
- **Hotfixes**: `release/vX.Y.0` → `release/vX.Y.Z` → selective merge

### Download Releases

All releases are available on GitHub with direct APK downloads:
- **Production**: Download `MassPing-vX.Y.Z-release.apk`
- **Testing**: Download `MassPing-vX.Y.Z-debug.apk`

**Latest Release**: [GitHub Releases](https://github.com/bilal-/MassPing/releases)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Maintain consistent formatting with the existing codebase

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support, feature requests, or bug reports, please create an issue in the repository.

## 🙏 Acknowledgments

- **Google** for the People API and Play Services
- **Android Jetpack** team for Compose and architecture components
- **Kotlin** team for the amazing programming language
- **Material Design** team for the design system

## 📱 Screenshots

*Screenshots coming soon - add screenshots of the app in action here*

## 🔮 Roadmap

Future enhancements planned:
- [ ] MMS support for media messages
- [ ] Message templates and scheduling
- [ ] Message analytics and reporting
- [ ] Multi-language support
- [ ] Dark theme customization
- [ ] Export/import contact groups
- [ ] Message encryption options

---

**Built with ❤️ by Bilal Ahmad**