#!/bin/bash

# MassPing Release Management Script
# Full release automation: version bump, changelog, git flow, GitHub release
# Usage: ./scripts/release.sh [patch|minor|major] [release_notes] [--hotfix]

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BUILD_GRADLE="app/build.gradle.kts"
CHANGELOG="CHANGELOG.md"
README="README.md"
BACKUP_SUFFIX=".backup"
GITHUB_REPO="bilal-/MassPing"
MAIN_BRANCH="main"
# Note: These paths will be dynamically updated based on version in build function

# Build dynamic file paths based on current version
get_build_artifact_paths() {
    local version="$1"

    # Use default gradle naming pattern for build outputs
    APK_DEBUG_PATH="app/build/outputs/apk/debug/app-debug.apk"
    APK_RELEASE_PATH="app/build/outputs/apk/release/app-release.apk"
    AAB_DEBUG_PATH="app/build/outputs/bundle/debug/app-debug.aab"
    AAB_RELEASE_PATH="app/build/outputs/bundle/release/app-release.aab"
}

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

show_usage() {
    echo "Usage: $0 [patch|minor|major] [--hotfix]"
    echo ""
    echo "Examples:"
    echo "  $0 patch"
    echo "  $0 minor" 
    echo "  $0 major"
    echo "  $0 patch --hotfix"
    echo ""
    echo "Version bumping rules:"
    echo "  patch: 1.0.0 → 1.0.1 (bug fixes)"
    echo "  minor: 1.0.0 → 1.1.0 (new features)"
    echo "  major: 1.0.0 → 2.0.0 (breaking changes)"
    echo ""
    echo "Workflow:"
    echo "  • Updates version and changelog"
    echo "  • Creates release branch (release/vX.Y.Z)"
    echo "  • Builds and tests APKs"
    echo "  • Creates GitHub release with APK uploads"
    echo "  • Merges release branch to main"
    echo "  • For hotfixes: works from existing release branch"
}

# Parse current version from build.gradle.kts
get_current_version() {
    local version_name=$(grep "versionName = " "$BUILD_GRADLE" | sed 's/.*versionName = "\([^"]*\)".*/\1/')
    local version_code=$(grep "versionCode = " "$BUILD_GRADLE" | sed 's/.*versionCode = \([0-9]*\).*/\1/')
    echo "$version_name:$version_code"
}

# Calculate next version
calculate_next_version() {
    local current_version="$1"
    local bump_type="$2"

    IFS='.' read -r major minor patch <<< "$current_version"

    case "$bump_type" in
        "patch")
            patch=$((patch + 1))
            ;;
        "minor")
            minor=$((minor + 1))
            patch=0
            ;;
        "major")
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        *)
            log_error "Invalid bump type: $bump_type"
            exit 1
            ;;
    esac

    echo "$major.$minor.$patch"
}

# Update version in build.gradle.kts
update_build_gradle() {
    local new_version="$1"
    local new_version_code="$2"

    log_info "Updating $BUILD_GRADLE..."

    # Create backup
    cp "$BUILD_GRADLE" "$BUILD_GRADLE$BACKUP_SUFFIX"

    # Update versionName and versionCode
    sed -i '' "s/versionName = \"[^\"]*\"/versionName = \"$new_version\"/" "$BUILD_GRADLE"
    sed -i '' "s/versionCode = [0-9]*/versionCode = $new_version_code/" "$BUILD_GRADLE"

    log_success "Updated version to $new_version (code: $new_version_code)"
}

# Move unreleased items to new release entry in CHANGELOG.md
update_changelog() {
    local version="$1"
    local date=$(date +%Y-%m-%d)

    log_info "Updating $CHANGELOG..."

    # Create backup
    cp "$CHANGELOG" "$CHANGELOG$BACKUP_SUFFIX"

    # Create temporary file with new entry
    local temp_file=$(mktemp)

    # Move content from [Unreleased] to new version section
    awk -v version="$version" -v date="$date" '
    /## \[Unreleased\]/ {
        print $0
        print ""
        print "## [" version "] - " date
        in_unreleased = 1
        next
    }
    /## \[.*\] - [0-9]/ && in_unreleased == 1 {
        # If we are still in unreleased section and hit another version, end unreleased
        print ""  # Add blank line before next version
        in_unreleased = 0
    }
    /^\[Unreleased\]:/ {
        print "[Unreleased]: https://github.com/bilal-/MassPing/compare/v" version "...HEAD"
        print "[" version "]: https://github.com/bilal-/MassPing/releases/tag/v" version
        next
    }
    {
        print $0
    }' "$CHANGELOG" > "$temp_file"

    mv "$temp_file" "$CHANGELOG"
    log_success "Moved unreleased changes to version $version"
}

# Update version in README.md
update_readme() {
    local version="$1"

    log_info "Updating $README version..."

    # Create backup
    cp "$README" "$README$BACKUP_SUFFIX"

    # Update version in README.md
    sed -i '' "s/\*\*Version:\*\* [0-9]*\.[0-9]*\.[0-9]*/\*\*Version:\*\* $version/" "$README"

    log_success "Updated README.md version to $version"
}

# Build and test
build_and_test() {
    log_info "Building and testing..."

    if ./gradlew assembleDebug assembleRelease; then
        log_success "Build completed successfully"
    else
        log_error "Build failed! Rolling back changes..."
        rollback_changes
        exit 1
    fi
}

# Rollback changes if something goes wrong
rollback_changes() {
    log_warning "Rolling back changes..."

    if [ -f "$BUILD_GRADLE$BACKUP_SUFFIX" ]; then
        mv "$BUILD_GRADLE$BACKUP_SUFFIX" "$BUILD_GRADLE"
        log_success "Restored $BUILD_GRADLE"
    fi

    if [ -f "$CHANGELOG$BACKUP_SUFFIX" ]; then
        mv "$CHANGELOG$BACKUP_SUFFIX" "$CHANGELOG"
        log_success "Restored $CHANGELOG"
    fi

    if [ -f "$README$BACKUP_SUFFIX" ]; then
        mv "$README$BACKUP_SUFFIX" "$README"
        log_success "Restored $README"
    fi
}

# Clean up backup files
cleanup_backups() {
    rm -f "$BUILD_GRADLE$BACKUP_SUFFIX"
    rm -f "$CHANGELOG$BACKUP_SUFFIX"
    rm -f "$README$BACKUP_SUFFIX"
}

# Check if GitHub CLI is installed
check_github_cli() {
    if ! command -v gh &> /dev/null; then
        log_error "GitHub CLI (gh) is not installed. Please install it:"
        log_info "  brew install gh"
        log_info "  or visit: https://cli.github.com/"
        exit 1
    fi

    # Check if authenticated
    if ! gh auth status &> /dev/null; then
        log_error "GitHub CLI is not authenticated. Please run:"
        log_info "  gh auth login"
        exit 1
    fi

    # Test GitHub API access
    if ! gh api user &> /dev/null; then
        log_error "GitHub API access failed. Please check your authentication:"
        log_info "  gh auth refresh"
        exit 1
    fi
}

# Create or switch to release branch
manage_release_branch() {
    local version="$1"
    local is_hotfix="$2"
    local branch_name="release/v$version"
    local base_version

    if [ "$is_hotfix" = true ]; then
        # For hotfix, find existing release branch for base version
        base_version=$(echo "$version" | sed 's/\.[0-9]*$//')  # Remove patch number
        local existing_branch="release/v${base_version}.0"

        if git show-ref --verify --quiet "refs/heads/$existing_branch"; then
            log_info "Checking out existing release branch: $existing_branch" >&2
            git checkout "$existing_branch" >&2

            # Create hotfix branch from release branch
            branch_name="release/v$version"
            log_info "Creating hotfix branch: $branch_name" >&2
            git checkout -b "$branch_name" >&2
        else
            log_warning "No existing release branch found for hotfix. Creating new release branch." >&2
            git checkout -b "$branch_name" "$MAIN_BRANCH" >&2
        fi
    else
        # Normal release: create new branch from main
        log_info "Creating release branch: $branch_name" >&2
        git checkout "$MAIN_BRANCH" >&2
        git pull origin "$MAIN_BRANCH" >&2
        git checkout -b "$branch_name" >&2
    fi

    echo "$branch_name"
}

# Build APKs and AABs for release
build_release_artifacts() {
    local version="$1"
    log_info "Building release APKs and AABs..."

    # Set up dynamic file paths based on version
    get_build_artifact_paths "$version"

    # Clean and build both APKs and AABs
    ./gradlew clean assembleDebug assembleRelease bundleDebug bundleRelease

    # Verify APKs exist
    if [ ! -f "$APK_DEBUG_PATH" ] || [ ! -f "$APK_RELEASE_PATH" ]; then
        log_error "APK build failed - files not found!"
        log_error "Expected: $APK_DEBUG_PATH"
        log_error "Expected: $APK_RELEASE_PATH"
        exit 1
    fi

    # Verify AABs exist
    if [ ! -f "$AAB_DEBUG_PATH" ] || [ ! -f "$AAB_RELEASE_PATH" ]; then
        log_error "AAB build failed - files not found!"
        log_error "Expected: $AAB_DEBUG_PATH"
        log_error "Expected: $AAB_RELEASE_PATH"
        exit 1
    fi

    # Show build artifacts info
    local apk_debug_size=$(ls -lh "$APK_DEBUG_PATH" | awk '{print $5}')
    local apk_release_size=$(ls -lh "$APK_RELEASE_PATH" | awk '{print $5}')
    local aab_debug_size=$(ls -lh "$AAB_DEBUG_PATH" | awk '{print $5}')
    local aab_release_size=$(ls -lh "$AAB_RELEASE_PATH" | awk '{print $5}')

    log_success "📱 APKs Built:"
    log_success "  Debug APK: $apk_debug_size"
    log_success "  Release APK: $apk_release_size"
    log_success "📦 AABs Built:"
    log_success "  Debug AAB: $aab_debug_size"
    log_success "  Release AAB: $aab_release_size"
}

# Extract release notes from CHANGELOG
get_release_notes() {
    local version="$1"
    
    # Extract content between the version header and the next version header
    awk -v version="$version" '
    /## \['"$version"'\] - [0-9]/ {
        in_version = 1
        next
    }
    /## \[.*\] - [0-9]/ && in_version == 1 {
        in_version = 0
    }
    in_version == 1 && !/^$/ {
        print $0
    }' "$CHANGELOG" | sed '/^$/d' # Remove empty lines
}

# Create GitHub release
create_github_release() {
    local version="$1"
    local is_hotfix="$2"

    log_info "Creating GitHub release v$version..."

    # Set up dynamic file paths based on version
    get_build_artifact_paths "$version"

    # Get app name dynamically
    local app_name=$(grep "rootProject.name" settings.gradle.kts 2>/dev/null | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    if [ -z "$app_name" ]; then
        app_name="MassPing"  # Fallback if rootProject.name not found
    fi

    # Get release notes from CHANGELOG
    local release_notes=$(get_release_notes "$version")
    if [ -z "$release_notes" ]; then
        release_notes="Release v$version"
    fi

    # Prepare release notes
    local full_notes="$release_notes"
    if [ "$is_hotfix" = true ]; then
        full_notes="🔥 **Hotfix Release**

$release_notes

---

**Installation Options:**
- 📱 **APK Files** (Direct install): Download \`${app_name}-v$version-release.apk\` or \`${app_name}-v$version-debug.apk\`
- 📦 **AAB Files** (Google Play): Download \`${app_name}-v$version-release.aab\` for Play Store submission

**What's Changed:**
See [CHANGELOG.md](https://github.com/$GITHUB_REPO/blob/v$version/CHANGELOG.md) for detailed changes.

**Full Changelog:** https://github.com/$GITHUB_REPO/compare/v$(get_previous_version)...v$version"
    else
        full_notes="🚀 **Release v$version**

$release_notes

---

**Installation Options:**
- 📱 **APK Files** (Direct install): Download \`${app_name}-v$version-release.apk\` or \`${app_name}-v$version-debug.apk\`
- 📦 **AAB Files** (Google Play): Download \`${app_name}-v$version-release.aab\` for Play Store submission

**What's Changed:**
See [CHANGELOG.md](https://github.com/$GITHUB_REPO/blob/v$version/CHANGELOG.md) for detailed changes.

**Full Changelog:** https://github.com/$GITHUB_REPO/compare/v$(get_previous_version)...v$version"
    fi

    # Verify all build artifacts exist before creating release
    local missing_files=()

    [ ! -f "$APK_RELEASE_PATH" ] && missing_files+=("Release APK: $APK_RELEASE_PATH")
    [ ! -f "$APK_DEBUG_PATH" ] && missing_files+=("Debug APK: $APK_DEBUG_PATH")
    [ ! -f "$AAB_RELEASE_PATH" ] && missing_files+=("Release AAB: $AAB_RELEASE_PATH")
    [ ! -f "$AAB_DEBUG_PATH" ] && missing_files+=("Debug AAB: $AAB_DEBUG_PATH")

    if [ ${#missing_files[@]} -ne 0 ]; then
        log_error "Build artifacts missing:"
        for file in "${missing_files[@]}"; do
            log_error "  $file"
        done
        return 1
    fi

    # Create release with both APK and AAB uploads
    log_info "Uploading APKs and AABs to GitHub release..."
    if gh release create "v$version" \
        "$APK_RELEASE_PATH#${app_name}-v$version-release.apk" \
        "$APK_DEBUG_PATH#${app_name}-v$version-debug.apk" \
        "$AAB_RELEASE_PATH#${app_name}-v$version-release.aab" \
        "$AAB_DEBUG_PATH#${app_name}-v$version-debug.aab" \
        --title "$app_name v$version" \
        --notes "$full_notes" \
        --draft=false \
        --latest; then

        log_success "GitHub release created: https://github.com/$GITHUB_REPO/releases/tag/v$version"
        log_info "📱 APK Downloads (Direct Install):"
        log_info "  Release: https://github.com/$GITHUB_REPO/releases/download/v$version/${app_name}-v$version-release.apk"
        log_info "  Debug: https://github.com/$GITHUB_REPO/releases/download/v$version/${app_name}-v$version-debug.apk"
        log_info "📦 AAB Downloads (Google Play Store):"
        log_info "  Release: https://github.com/$GITHUB_REPO/releases/download/v$version/${app_name}-v$version-release.aab"
        log_info "  Debug: https://github.com/$GITHUB_REPO/releases/download/v$version/${app_name}-v$version-debug.aab"
        return 0
    else
        log_error "Failed to create GitHub release"
        log_warning "You can create it manually at: https://github.com/$GITHUB_REPO/releases/new?tag=v$version"
        return 1
    fi
}

# Get previous version for changelog comparison
get_previous_version() {
    git tag --sort=-version:refname | head -n 2 | tail -n 1 | sed 's/^v//' || echo "1.0.0"
}

# Merge release branch back to main
merge_release_to_main() {
    local version="$1"
    local branch_name="release/v$version"

    log_info "Merging release branch to main..."

    git checkout "$MAIN_BRANCH"
    git pull origin "$MAIN_BRANCH"
    git merge --no-ff "$branch_name" -m "Merge release v$version"

    # Push main branch
    git push origin "$MAIN_BRANCH"

    # Push release branch for future hotfixes
    git push origin "$branch_name"

    log_success "Release branch merged and pushed"
}

# Push git tag
push_git_tag() {
    local version="$1"

    log_info "Pushing git tag v$version..."
    git push origin "v$version"
    log_success "Tag pushed to GitHub"
}

# This function is no longer used - release logic moved to main()

# Main function
main() {
    # Check if we're in the right directory
    if [ ! -f "$BUILD_GRADLE" ]; then
        log_error "Could not find $BUILD_GRADLE. Please run this script from the project root."
        exit 1
    fi

    # Get app name dynamically
    local app_name=$(grep "rootProject.name" settings.gradle.kts 2>/dev/null | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
    if [ -z "$app_name" ]; then
        app_name="MassPing"  # Fallback if rootProject.name not found
    fi

    # Parse arguments
    local bump_type="$1"
    local is_hotfix=false

    # Check for hotfix flag
    if [ "$2" = "--hotfix" ]; then
        is_hotfix=true
    fi

    if [ -z "$bump_type" ]; then
        show_usage
        exit 1
    fi

    # Validate bump type
    if [[ ! "$bump_type" =~ ^(patch|minor|major)$ ]]; then
        log_error "Invalid bump type: $bump_type"
        show_usage
        exit 1
    fi

    # Check prerequisites
    check_github_cli

    # Get current version info
    local version_info=$(get_current_version)
    local current_version=$(echo "$version_info" | cut -d: -f1)
    local current_version_code=$(echo "$version_info" | cut -d: -f2)

    log_info "Current version: $current_version (code: $current_version_code)"

    # Calculate next version
    local new_version=$(calculate_next_version "$current_version" "$bump_type")
    local new_version_code=$((current_version_code + 1))

    log_info "New version: $new_version (code: $new_version_code)"
    if [ "$is_hotfix" = true ]; then
        log_warning "🔥 HOTFIX RELEASE"
    fi

    # Confirm with user
    if [ -t 0 ]; then  # Check if running interactively
        echo -n "Proceed with release? (y/N): "
        read -r confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            log_info "Release cancelled"
            exit 0
        fi
    fi

    # Store original branch
    local original_branch=$(git symbolic-ref --short HEAD)

    # Create/switch to release branch
    local release_branch=$(manage_release_branch "$new_version" "$is_hotfix")

    # Update files on release branch
    update_build_gradle "$new_version" "$new_version_code"
    update_changelog "$new_version"
    update_readme "$new_version"

    # Commit changes to release branch
    git add "$BUILD_GRADLE" "$CHANGELOG" "$README"
    git commit -m "Release v$new_version

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"

    # Create git tag
    git tag -a "v$new_version" -m "Release v$new_version"

    # Push the release branch first (needed for GitHub release)
    log_info "Pushing release branch to GitHub..."
    git push origin "$release_branch"

    # Push tag to GitHub
    push_git_tag "$new_version"

    # Build APKs and AABs
    build_release_artifacts "$new_version"

    # Create GitHub release with APK uploads
    if create_github_release "$new_version" "$is_hotfix"; then
        log_success "GitHub release published!"
    else
        log_error "Failed to create GitHub release"
        # Continue anyway - we can create it manually
    fi

    # Merge release branch to main (unless it's a hotfix from an old release)
    if [ "$is_hotfix" != true ] || [[ "$new_version" =~ ^$(echo "$current_version" | sed 's/\.[0-9]*$//').*$ ]]; then
        merge_release_to_main "$new_version"
    else
        # For old hotfixes, just push the release branch
        git push origin "$release_branch"
        log_info "Hotfix branch pushed. Manual merge to main may be needed."
    fi

    # Clean up
    cleanup_backups

    log_success "🎉 Release v$new_version completed successfully!"
    echo ""
    echo "✅ What was done:"
    echo "  • Created release branch: $release_branch"
    echo "  • Updated version to $new_version (code: $new_version_code)"
    echo "  • Updated CHANGELOG.md with release notes"
    echo "  • Updated README.md version"
    echo "  • Built and tested APKs and AABs"
    echo "  • Created GitHub release: https://github.com/$GITHUB_REPO/releases/tag/v$new_version"
    echo "  • Uploaded APK and AAB files to GitHub"
    if [ "$is_hotfix" != true ]; then
        echo "  • Merged to main branch"
    fi
    echo ""
    echo "📦 Downloads:"
    echo "  APK (Direct Install): https://github.com/$GITHUB_REPO/releases/download/v$new_version/${app_name}-v$new_version-release.apk"
    echo "  AAB (Google Play): https://github.com/$GITHUB_REPO/releases/download/v$new_version/${app_name}-v$new_version-release.aab"
}

# Run main function
main "$@"