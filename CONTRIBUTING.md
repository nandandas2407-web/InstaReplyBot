# Contributing to InstaReply Bot

Thank you for your interest in contributing to InstaReply Bot! This document provides guidelines for contributing to this open-source Instagram DM auto-responder project.

## How to Contribute

### Reporting Bugs

1. Check [existing issues](https://github.com/nandandas2407-web/InstaReplyBot/issues) to avoid duplicates
2. Open a new issue with:
   - Clear title describing the bug
   - Steps to reproduce
   - Expected vs actual behavior
   - Device model and Android version
   - App version

### Suggesting Features

1. Open an issue with the **feature request** label
2. Describe the use case and how it would benefit users
3. Include mockups or examples if applicable

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes following the code style below
4. Test thoroughly on a real device
5. Commit with a clear message
6. Push to your fork and submit a PR

## Code Style

- **Language:** Kotlin
- **Architecture:** MVVM with Room database
- **Formatting:** Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Naming:**
  - Classes: PascalCase
  - Functions/variables: camelCase
  - Constants: UPPER_SNAKE_CASE

## Development Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/nandandas2407-web/InstaReplyBot.git
   ```
2. Open in Android Studio (latest stable)
3. Sync Gradle and build
4. Run on device or emulator (Android 8.0+)

## Testing

- Test on real devices when possible
- Verify notification listener permissions work correctly
- Test with multiple AI providers (free and paid)
- Ensure the one-reply-per-message guarantee holds

## Questions?

Open an issue with the **question** label or reach out via [GitHub Discussions](https://github.com/nandandas2407-web/InstaReplyBot/discussions).
