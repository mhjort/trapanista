# AGENTS.md

Guidance for AI coding agents working on Trapanista.

## What this is

An Android app that teaches Italian through short role-play conversations (restaurant, hotel,
train station, ...). Each scenario has two modes:

- **Voice:** a real-time spoken conversation with Gemini Live through Firebase AI Logic. When
  it ends, Claude reviews the transcript and gives corrections and useful phrases.
- **Text:** a chat with Claude, which plays the scene and corrects each message as it goes.

Claude is BYOK: the user's own Anthropic API key is used directly from the device. Gemini goes
through the app's Firebase project (no user key; protected by App Check). There is no other backend.

## Stack

- **Kotlin only.** All code under `app/src/main/kotlin` (plus `src/debug` / `src/release` for
  build-type-specific App Check), build scripts in Gradle Kotlin DSL (`.kts`).
  Don't add Java sources or Groovy `.gradle` files.
- Jetpack Compose + Material 3 for UI; no XML layouts, no Fragments.
- Single activity (`MainActivity`) with screen state in a `rememberSaveable` string
  (`home`, `settings`, `chat:<id>`, `voice:<id>`); no Navigation library yet.
- Coroutines + `StateFlow` in view models.
- Dependency versions live in `gradle/libs.versions.toml`.
- Claude access via the official Anthropic Java SDK (`com.anthropic:anthropic-java`), beta messages endpoint.
- Gemini Live via Firebase AI Logic (`com.google.firebase:firebase-ai`, versions from the Firebase BoM).
- minSdk 26, compile/targetSdk 35, JDK 17.

## Layout

```
app/src/main/kotlin/app/trapanista/
  MainActivity.kt        screen switching
  TrapanistaApp.kt       Firebase init; firebaseReady is false without google-services.json
  data/Scenario.kt       the role-play situations (add new scenarios here)
  data/SettingsStore.kt  encrypted API key (Android Keystore AES-GCM), level, Claude/Gemini models
  llm/Claude.kt          shared Claude call: structured JSON output, fallbacks, error mapping
  llm/TutorClient.kt     text-mode tutor (system prompt, turn schema, append-only history)
  llm/ReviewClient.kt    review of a voice transcript
  llm/VoicePartner.kt    Gemini Live session, transcripts, end_scene function call
  ui/                    Compose screens, theme, ChatViewModel, VoiceViewModel
app/src/{debug,release}/kotlin/app/trapanista/AppCheck.kt   App Check provider per build type
```

## Build

```sh
export JAVA_HOME=~/.local/jdk   # on the maintainer's machine; any JDK 17 works
./gradlew assembleDebug assembleRelease
```

Always make sure both build types compile before finishing a change. `app/google-services.json`
is git-ignored and optional: without it the google-services plugin isn't applied, the app still
builds, and voice mode shows a setup message.

## Claude API conventions

- Default model is `claude-sonnet-5-5`; Opus 5.5 and Haiku 5.5 are user-selectable (`ClaudeModel`).
  Use model IDs exactly as written, with no date suffixes.
- Don't send `thinking: disabled`, `budget_tokens`, `temperature`/`top_p`/`top_k`, assistant
  prefill, or forced `tool_choice`; current models reject them. Control cost with
  `output_config.effort` (`LOW` for chat turns, `MEDIUM` for reviews).
- Responses are structured JSON. If you change a schema, update its parser, data class and UI
  together. Every object needs `required` listing all properties and `additionalProperties: false`
  (use the `Claude.obj` helper).
- Text-mode history is **append-only**: replay assistant responses with `BetaMessage.toParam()`
  and never edit or drop earlier turns, or thinking-block validation fails.
- Server-side refusal fallbacks (`fallbacksDefault()` + beta `server-side-fallback-2026-07-01`) are
  sent for Opus and Sonnet only; Haiku doesn't support them.
- API calls are blocking; run them on `Dispatchers.IO`.

## Gemini Live conventions

- The Live API in Firebase AI Logic is a developer preview (`@OptIn(PublicPreviewAPI::class)`);
  expect breaking changes when bumping the BoM.
- `session.startAudioConversation(liveAudioConversationConfig { ... })` owns the mic and speaker.
  Always call `stopAudioConversation()` and `close()` when leaving. Check `RECORD_AUDIO` at
  runtime before starting.
- The SDK plays Gemini as media audio, which phones leave out of echo cancellation. `VoicePartner`
  overrides it to voice-communication audio via `initializationHandler` and switches the phone to
  call mode for the session; keep both, or Gemini hears itself and answers its own lines.
- A server-side handshake rejection surfaces only as "Channel was closed by the server"; see the
  README's Firebase setup and troubleshooting.
- Transcript callbacks arrive as fragments on background threads; `VoiceViewModel` merges
  consecutive fragments from the same speaker. Update state with `MutableStateFlow.update`.
- During the scene Gemini doesn't correct the learner; feedback comes from the Claude review.
  Gemini signals the end of the scene by calling the `end_scene` function.

## Security

- Never log, print or hard-code API keys. Keys are only stored via `SettingsStore`.
- Don't commit `google-services.json` or App Check debug tokens.
- The app should only talk to `api.anthropic.com` and Firebase / Google APIs.

## Style

- Follow the existing code: official Kotlin style, small files, sparse comments that explain why.
- Learner-facing UI text is English; scenario titles and placeholders are Italian.
