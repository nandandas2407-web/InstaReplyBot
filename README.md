# InstaReplyBot

Android Instagram auto-reply bot that reads notifications via `NotificationListenerService` and sends AI-generated replies using configurable providers.

## Features

- **Notification-based**: Reads Instagram DMs through Android notification listener (no login required)
- **AI Providers**: Supports Gemini, OpenRouter, NVIDIA NIM, OpenAI, OpenCode, and custom OpenAI-compatible APIs
- **Personalized Replies**: Configure your name, location, and bio for context-aware responses
- **Rule System**: Create multiple rules with different triggers (exact match, contains, regex, etc.)
- **Rate Limiting**: Control max replies per day per rule
- **Contact Filtering**: Apply rules to all contacts, specific contacts, or ignore certain contacts
- **Delay Config**: Set reply delays to appear more natural
- **Accessibility Service**: Types and sends replies directly in Instagram

## Setup

1. Install the APK on your Android device
2. Grant **Notification Listener** permission when prompted
3. Enable the **Accessibility Service** for auto-typing replies
4. Go to Settings and configure your identity (name, location, bio)
5. Add your AI provider API key(s)
6. Create rules with trigger patterns and AI provider selection

## Architecture

```
app/src/main/java/com/instareply/
├── InstaReplyApp.kt              # Application class
├── ai/
│   ├── AiProvider.kt             # Interface for AI providers
│   ├── AiProviderFactory.kt      # Factory for creating providers
│   ├── GeminiProvider.kt         # Google Gemini API
│   └── OpenAiCompatibleProvider.kt # OpenAI-compatible APIs
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt        # Room database
│   │   ├── RuleDao.kt
│   │   ├── ContactDao.kt
│   │   └── ReplyLogDao.kt
│   └── model/
│       ├── Rule.kt
│       ├── Contact.kt
│       ├── ReplyLog.kt
│       └── AiConfig.kt
├── service/
│   ├── InstaNotificationListener.kt  # Reads notifications
│   ├── InstaAccessibilityService.kt  # Types+sends replies
│   ├── ReplyEngine.kt                # AI reply generation
│   ├── ReplyService.kt               # Foreground service
│   └── BootReceiver.kt               # Auto-start on boot
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── RuleAdapter.kt
│   ├── settings/
│   │   └── SettingsActivity.kt
│   └── rules/
│       └── RuleEditorActivity.kt
└── util/
    └── PrefsManager.kt
```

## Supported AI Providers

| Provider | Base URL |
|----------|----------|
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta` |
| OpenRouter | `https://openrouter.ai/api/v1` |
| NVIDIA NIM | `https://integrate.api.nvidia.com/v1` |
| OpenAI | `https://api.openai.com/v1` |
| OpenCode | `https://api.opencode.ai/v1` |

## Permissions Required

- `INTERNET` - API calls to AI providers
- `RECEIVE_BOOT_COMPLETED` - Auto-start on boot
- `FOREGROUND_SERVICE` - Background service
- `POST_NOTIFICATIONS` - Status notifications
- Notification Listener access
- Accessibility Service access

## Tech Stack

- Kotlin
- Room Database
- OkHttp + Gson
- Material Design
- Coroutines
- ViewBinding

## License

MIT
