<p align="center">
  <img src="https://raw.githubusercontent.com/nandandas2407-web/InstaReplyBot/main/.github/readme/hero.svg" width="100%" alt="InstaReply Bot — AI auto-replies to your Instagram DMs"/>
</p>

<p align="center">
  <a href="https://github.com/nandandas2407-web/InstaReplyBot/actions/workflows/build.yml">
    <img src="https://github.com/nandandas2407-web/InstaReplyBot/actions/workflows/build.yml/badge.svg" alt="Build status"/>
  </a>
  <a href="https://github.com/nandandas2407-web/InstaReplyBot/releases/latest">
    <img src="https://img.shields.io/badge/release-v0.4-orange?logo=android&logoColor=white" alt="Release"/>
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android&logoColor=white" alt="Android 8.0+"/>
  <img src="https://img.shields.io/badge/AI-Gemini%20%7C%20Groq%20%7C%20OpenRouter%20%7C%20Zen-833ab4" alt="AI providers"/>
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License"/>
</p>

> **Never miss a DM again.** InstaReply Bot watches your Instagram notifications and answers with an AI reply written in your voice — automatically, in the background, with **zero Instagram login** and **free AI models** included.

---

## 🚀 What it does (in one picture)

<p align="center">
  <img src="https://raw.githubusercontent.com/nandandas2407-web/InstaReplyBot/main/.github/readme/flow.svg" width="100%" alt="How InstaReply Bot works"/>
</p>

| | |
|---|---|
| ✅ No Instagram login or password | ✅ No screen / no typing needed (primary path) |
| ✅ One reply per message — never 2, never 0 when the model answers | ✅ Works with screen off |
| ✅ Free providers: **Groq**, **OpenCode Zen** | ✅ Multi-provider: Gemini, OpenRouter, NVIDIA, OpenAI, Custom |

---

## 📥 1. Install

Grab the latest APK from the **[Releases page](https://github.com/nandandas2407-web/InstaReplyBot/releases/latest)** and install it on your phone.

<details>
<summary><b>⚠️ “Play Protect blocked / unknown sources”?</b> (tap to expand)</summary>

1. Tap **Install anyway** (or **More details → Install anyway**).
2. If Play Protect complains again, open **Play Store → Profile → Play Protect → Settings** and temporarily turn off *Scan apps with Play Protect* — you can re-enable it after install.

> The app is built from source in this repo. No Play Store needed.
</details>

---

## 🔑 2. Grant the two permissions

Both permissions are shown on the home screen with buttons — this is the only setup the app needs.

| Permission | Why | How |
|---|---|---|
| 🔔 **Notification access** | Reads incoming Instagram DMs | Press **Grant Notification Access** → find *InstaReply Bot* → enable |
| ♿ **Accessibility service** *(optional)* | Fallback send path (auto-typing) | Press **Enable Accessibility Service** → *InstaReply Bot* → enable |

<details>
<summary><b>🤔 Why is Accessibility optional?</b></summary>

Most Android apps (including this one) used to need Accessibility to type into Instagram. InstaReply Bot's **primary path doesn't**: it fires Instagram's *own* notification **Reply action** with the AI text injected — exactly like tapping “Reply” in the notification shade, but automated. Accessibility is only used if Instagram doesn't attach a Reply action.

Enable both if in doubt — the app always prefers the background path.
</details>

---

## 🧠 3. Pick your AI provider (free options below)

Open **Settings** from the home screen. Fill in at least one provider's API key, pick its model, then set **Max Output Tokens** (default `200` is fine — the app auto-raises it for reasoning models).

### 💸 Free providers (no card, no charge)

<details open>
<summary><b>🟢 Groq — blazing fast & free</b></summary>

1. Get a free key at [console.groq.com](https://console.groq.com) (sign in with Google/GitHub — no payment).
2. Paste it into the **Groq (Free)** field in Settings.
3. Pick a model:

| Model | Notes |
|---|---|
| `openai/gpt-oss-20b` | Fast, smart — great default |
| `openai/gpt-oss-120b` | Smarter, heavier reasoning |
| `llama-3.3-70b-versatile` | Excellent conversational quality |
| `llama-3.1-8b-instant` | Smallest, fastest |
| `qwen/qwen3.6-27b` | Multilingual |

4. Press **Test Key** — it should toast *“Groq API key is valid”*.
</details>

<details>
<summary><b>🔵 OpenCode Zen — curated free gateway</b></summary>

1. Get a key from [opencode.ai](https://opencode.ai) (free tier available).
2. Paste it into the **OpenCode** field, pick a model:

| Model | Notes |
|---|---|
| `opencode/deepseek-v4-flash-free` | Fast, lightweight — default |
| `opencode/big-pickle` | Stealth coding model |
| `opencode/mimo-v2.5-free` | Context processing |
| `opencode/nemotron-3-ultra-free` | Heavy reasoning |
| `opencode/north-mini-code-free` | Logic tasks |
| `opencode/minimax-m2.5-free` | Multi-agent planning |

3. **Test Key** → should succeed. Endpoint: `https://opencode.ai/zen/v1/chat/completions`.
</details>

<details>
<summary><b>🟣 Paid (but powerful) providers</b></summary>

| Provider | Key from | Models |
|---|---|---|
| **Gemini** | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) *(free tier exists!)* | `gemini-3.6-flash`, `gemini-3.5-flash`, … |
| **OpenRouter** | [openrouter.ai/keys](https://openrouter.ai/keys) | hundreds, incl. `:free` models |
| **NVIDIA NIM** | [build.nvidia.com](https://build.nvidia.com) | Llama, Nemotron, DeepSeek… |
| **OpenAI** | [platform.openai.com](https://platform.openai.com/api-keys) | GPT-5.x |
| **Custom** | your own | any OpenAI-compatible endpoint |

> ⚠️ **Tip:** if your replies arrive **empty** or *“Model returned empty content”*, your model is a *reasoning* model (like `gpt-oss-*`) — the app now auto-retries with a bigger token budget, so it usually just works. If it still fails, raise **Max Output Tokens** in Settings.
</details>

---

## 📜 4. Create your auto-reply rules

Rules decide **who** gets answered, **when**, and **how**. Tap **Add Rule** on the home screen.

| Setting | What it does |
|---|---|
| **Rule name** | Just a label |
| **Trigger pattern** + **Match type** | `CONTAINS`, `EXACT`, `STARTS_WITH`, `ENDS_WITH`, `REGEX`, or `ANY` (answer everything) |
| **AI provider** | Which provider answers (must have a key set!) |
| **Reply template** | *(optional)* a fixed text reply instead of AI |
| **Delay (ms)** | Human-like pause before replying (default `3000`) |
| **Max replies / day** | Per-rule daily cap — successful replies only |
| **Specific / Ignored contacts** | Limit or block people (comma-separated) |
| **Groups** | Optionally also handle group chats |

<details>
<summary><b>🎯 Example rules that actually work</b></summary>

- **Answer everyone:** pattern empty, match type `ANY`, delay `3000`, max `50/day`.
- **Only besties:** match `ANY` + Specific contacts `Alice, Bob` → everyone else is ignored.
- **Price questions:** match `CONTAINS` pattern `price|cost|how much`, template `“DM me, happy to help with pricing! 😊”`.
- **Be quiet at night:** lower the per-day cap, or use a stricter pattern.
</details>

---

## 🛡️ 5. The “one reply per message” guarantee

InstaReply Bot can **never** answer the same message twice, even if Instagram re-posts the notification while the AI is still thinking:

1. **60-second dedupe** on sender + message.
2. **Persisted log check** — a successful reply to that exact message is remembered for 10 minutes (survives restarts).
3. **Send discipline** — the AI text is typed once and Send is clicked at most once.

---

## 🔧 Troubleshooting

<details>
<summary><b>“No reply at all — Last attempt failed: …empty content…”</b></summary>

Your model returned reasoning but no answer (typical of `gpt-oss` / reasoning models). The app retries automatically with a 4096-token budget. If it still shows this, switch models in Settings (e.g. `llama-3.3-70b-versatile` on Groq) or raise **Max Output Tokens**.
</details>

<details>
<summary><b>“AI doesn't answer on the 2nd / 3rd message”</b></summary>

Check the **“Last attempt”** line on the home screen — it shows the exact failure reason. Most common causes: the rule's daily cap was hit, or the provider is rate-limited. Raise the cap, or test the key in Settings (it toasts a clear verdict).
</details>

<details>
<summary><b>“Replies arrive very late”</b></summary>

- AI generation takes a few seconds — that's normal.
- Increase/decrease the rule **Delay** to taste.
- A retry after an empty response adds one extra API call (rare).
- On slow networks, generation can reach the 30s timeout — use a fast provider (Groq is the fastest).
</details>

<details>
<summary><b>“I got 2 replies for one message”</b></summary>

Should be impossible since v0.3 (see guarantee above). If you still see it, update to the latest release — older builds lacked the persisted check.
</details>

<details>
<summary><b>“Key test fails”</b></summary>

- Copy-paste the key without spaces.
- Free tiers have rate limits — wait a minute and retry.
- Some providers need the exact endpoint; the built-in ones are pre-configured.
</details>

---

## 🏗️ Tech & architecture

| | |
|---|---|
| Language | Kotlin + Coroutines |
| Storage | Room (rules, contacts, reply log) |
| Networking | OkHttp + Gson |
| UI | Material Components + ViewBinding |
| Min / Target SDK | 26 / 34 |
| Build | Gradle 8.7 · GitHub Actions (every push builds an APK) |

```
com.instareply/
├── ai/        GeminiProvider · OpenAiCompatibleProvider (Groq, OpenRouter, NVIDIA, OpenAI, Zen, Custom)
├── service/   Notification listener · Accessibility fallback · ReplyEngine · Foreground service · Boot receiver
├── data/      Room: RuleDao · ContactDao · ReplyLogDao
├── ui/        Main (stats + toggles) · Settings (keys, models, max tokens) · Rules editor
└── util/      PrefsManager · shared provider list
```

---

## ⚠️ Use responsibly

Automated messaging may violate Instagram's Terms of Service. This project is for personal/educational use — respect platform rules, and consider disclosure when replying on a business account.

## 📄 License

MIT — build on it freely.
