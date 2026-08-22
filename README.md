# InstaReply Bot — AI-Powered Instagram DM Auto-Responder for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/nandandas2407-web/InstaReplyBot/main/.github/readme/hero.svg" width="100%" alt="InstaReply Bot — AI auto-reply to Instagram DMs automatically on Android"/>
</p>

<p align="center">
  <a href="https://github.com/nandandas2407-web/InstaReplyBot/actions/workflows/build.yml">
    <img src="https://github.com/nandandas2407-web/InstaReplyBot/actions/workflows/build.yml/badge.svg" alt="Build Status - Android CI"/>
  </a>
  <a href="https://github.com/nandandas2407-web/InstaReplyBot/releases/latest">
    <img src="https://img.shields.io/github/v/release/nandandas2407-web/InstaReplyBot?logo=android&logoColor=white&color=orange" alt="Latest Release - Instagram Auto Reply Bot"/>
  </a>
  <img src="https://img.shields.io/badge/Android-8.0+-3ddc84?logo=android&logoColor=white" alt="Android 8.0+ Required"/>
  <img src="https://img.shields.io/badge/AI-Gemini%20%7C%20Groq%20%7C%20OpenRouter%20%7C%20OpenCode%20Zen-833ab4" alt="AI Providers - Free and Paid"/>
  <img src="https://img.shields.io/github/license/nandandas2407-web/InstaReplyBot" alt="MIT License - Open Source Instagram Bot"/>
  <img src="https://img.shields.io/github/stars/nandandas2407-web/InstaReplyBot?style=social" alt="GitHub Stars"/>
</p>

<p align="center">
  <b>Never miss an Instagram DM again.</b> InstaReply Bot is an open-source Android app that watches your Instagram notifications and replies with AI-generated messages — automatically, in the background, with <b>zero Instagram login</b> required.
</p>

---

## What is InstaReply Bot?

**InstaReply Bot** is a free, open-source **Instagram DM auto-responder** for Android. It uses **AI models** (Gemini, Groq, OpenRouter, and more) to read your incoming Instagram direct messages and send personalized replies — all without requiring your Instagram password or screen access.

**Perfect for:**
- Small business owners who need instant Instagram DM responses
- Content creators managing high-volume Instagram inboxes
- Anyone who wants automated Instagram replies powered by AI
- Personal use — never leave a friend on "seen" again

---

## Key Features

| Feature | Description |
|---------|-------------|
| **No Instagram Login Required** | Works entirely through Android notification access — your credentials stay safe |
| **AI-Powered Replies** | Uses Gemini, Groq (free), OpenCode Zen (free), OpenRouter, NVIDIA, and OpenAI |
| **Fully Automated** | Runs as a background service — screen-off, no manual input needed |
| **One Reply Per Message Guarantee** | Deduplication engine ensures every message gets exactly one response |
| **Custom Reply Rules** | Filter by sender, keyword patterns, regex, time-of-day, and daily limits |
| **Free AI Models Included** | Groq and OpenCode Zen offer free API tiers — no credit card needed |
| **Human-Like Delays** | Configurable response delay for natural conversation feel |
| **Group Chat Support** | Optionally auto-reply in Instagram group conversations |
| **Activity Dashboard** | Track reply stats and weekly activity in-app |

---

## How It Works

<p align="center">
  <img src="https://raw.githubusercontent.com/nandandas2407-web/InstaReplyBot/main/.github/readme/flow.svg" width="100%" alt="How InstaReply Bot works - Instagram DM auto-reply flow diagram"/>
</p>

1. **Detect** — InstaReply Bot's notification listener picks up incoming Instagram DMs
2. **Analyze** — Your custom rules decide who gets replied to and when
3. **Generate** — The selected AI provider generates a personalized response in your style
4. **Send** — The reply is delivered via Instagram's notification Reply action (or Accessibility fallback)

---

## Getting Started

### 1. Install the APK

Download the latest release from the **[Releases page](https://github.com/nandandas2407-web/InstaReplyBot/releases/latest)** and install on your Android device (Android 8.0+).

> **Play Protect warning?** Tap **Install anyway** or temporarily disable Play Protect scanning — the app is built from source and safe.

### 2. Grant Permissions

| Permission | Purpose | Setup |
|------------|---------|-------|
| **Notification Access** | Reads incoming Instagram DM notifications | Grant via Settings → find InstaReply Bot → enable |
| **Accessibility Service** *(optional)* | Fallback reply method if notification Reply unavailable | Enable via Settings → Accessibility → InstaReply Bot |

### 3. Configure Your AI Provider

Choose a free or paid AI provider in the app's Settings:

#### Free AI Providers (No Credit Card Required)

| Provider | Setup | Models |
|----------|-------|--------|
| **[Groq](https://console.groq.com)** | Get free API key → paste in Settings | `openai/gpt-oss-20b`, `llama-3.3-70b-versatile`, `qwen/qwen3.6-27b` |
| **[OpenCode Zen](https://opencode.ai)** | Get free API key → paste in Settings | `opencode/deepseek-v4-flash-free`, `opencode/mimo-v2.5-free` |
| **[Google Gemini](https://aistudio.google.com/apikey)** | Free tier available | `gemini-3.6-flash`, `gemini-3.5-flash` |

#### Paid AI Providers

| Provider | Models |
|----------|--------|
| **[OpenRouter](https://openrouter.ai/keys)** | Hundreds of models including free tiers |
| **[NVIDIA NIM](https://build.nvidia.com)** | Llama, Nemotron, DeepSeek |
| **[OpenAI](https://platform.openai.com/api-keys)** | GPT-5.x series |
| **Custom Endpoint** | Any OpenAI-compatible API |

### 4. Create Auto-Reply Rules

Define who gets answered and how:

| Setting | Options |
|---------|---------|
| **Trigger Pattern** | `CONTAINS`, `EXACT`, `STARTS_WITH`, `ENDS_WITH`, `REGEX`, or `ANY` |
| **AI Provider** | Select which provider answers |
| **Reply Template** | Fixed text fallback (optional) |
| **Delay** | Human-like pause before replying (default: 3 seconds) |
| **Daily Cap** | Maximum replies per rule per day |
| **Specific Contacts** | Limit replies to certain senders |
| **Ignored Contacts** | Block specific people |

---

## Architecture

```
com.instareply/
├── ai/          AI providers (Gemini, Groq, OpenRouter, OpenAI, Zen, Custom)
├── service/     Notification listener, Accessibility fallback, ReplyEngine, Foreground service
├── data/        Room database (Rules, Contacts, Reply logs)
├── ui/          Main dashboard, Settings, Rule editor
└── util/        Preferences manager, Provider configuration
```

**Tech Stack:** Kotlin · Coroutines · Room Database · OkHttp · Material Design · Gradle · GitHub Actions CI

---

## Troubleshooting

<details>
<summary><b>"No reply at all — empty content error"</b></summary>

Your model returned reasoning text but no actionable reply. The app auto-retries with a larger token budget. If it persists, switch to a non-reasoning model (e.g., `llama-3.3-70b-versatile` on Groq) or increase **Max Output Tokens** in Settings.
</details>

<details>
<summary><b>"AI doesn't reply to 2nd or 3rd message"</b></summary>

Check the **"Last attempt"** status on the home screen. Common causes: daily cap reached, provider rate-limited, or API key invalid. Test your key in Settings.
</details>

<details>
<summary><b>"Replies arrive late"</b></summary>

AI generation takes 2-5 seconds (normal). Increase/decrease rule **Delay** to taste. Use Groq for fastest response times. Slow networks may hit the 30s timeout.
</details>

<details>
<summary><b>"Duplicate replies for one message"</b></summary>

This was fixed in v0.3 with the one-reply-per-message guarantee. Update to the latest release.
</details>

<details>
<summary><b>"API key test fails"</b></summary>

Copy-paste the key without extra spaces. Free tiers have rate limits — wait a minute and retry. Built-in providers have pre-configured endpoints.
</details>

---

## Contributing

Contributions are welcome! Whether it's bug reports, feature requests, or pull requests — check the [issues page](https://github.com/nandandas2407-web/InstaReplyBot/issues) to get started.

---

## Disclaimer

Automated messaging may violate Instagram's Terms of Service. This project is for **personal and educational use**. Respect platform rules and consider disclosure when replying on business accounts.

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=nandandas2407-web/InstaReplyBot&type=Date)](https://star-history.com/#nandandas2407-web/InstaReplyBot&Date)

---

## License

[MIT License](LICENSE) — free to use, modify, and distribute.
