# AGENTS.md

Guidance for AI coding agents working on Trapanista.

## What this is

An Android app that teaches Italian through short role-play conversations (restaurant, hotel,
train station, ...). Claude plays the other person in the scene, corrects the learner and suggests
replies. It is BYOK: the user's own Anthropic API key is used directly from the device; there is
no backend.

## Stack

- **Kotlin only.** All code under `app/src/main/kotlin`, build scripts in Gradle Kotlin DSL (`.kts`).
  Don't add Java sources or Groovy `.gradle` files.
- Jetpack Compose + Material 3 for UI; no XML layouts, no Fragments.
- Single activity (`MainActivity`) with screen state in a `rememberSaveable` string; no Navigation library yet.
- Coroutines + `StateFlow` in view models.
- Dependency versions live in `gradle/libs.versions.toml`.
- Claude access via the official Anthropic Java SDK (`com.anthropic:anthropic-java`), beta messages endpoint.
- minSdk 26, compile/targetSdk 35, JDK 17.

## Layout

```
app/src/main/kotlin/app/trapanista/
  MainActivity.kt        screen switching
  data/Scenario.kt       the role-play situations (add new scenarios here)
  data/SettingsStore.kt  encrypted API key (Android Keystore AES-GCM), level, model
  llm/TutorClient.kt     system prompt, JSON output schema, API call, response parsing
  ui/                    Compose screens, theme, ChatViewModel
```

## Build

```sh
export JAVA_HOME=~/.local/jdk   # on the maintainer's machine; any JDK 17 works
./gradlew assembleDebug
```

Always make sure `./gradlew assembleDebug` passes before finishing a change.

## Claude API conventions

- Default model is `claude-opus-5-5`; Sonnet 5.5 and Haiku 5.5 are user-selectable (`TutorModel`).
  Use model IDs exactly as written, with no date suffixes.
- Don't send `thinking: disabled`, `budget_tokens`, `temperature`/`top_p`/`top_k`, assistant
  prefill, or forced `tool_choice`; current models reject them. Control cost with
  `output_config.effort` (currently `LOW`).
- Each tutor turn is returned as structured JSON (`TURN_SCHEMA`). If you change the schema, update
  `parseTurn`, the `TutorTurn` data class and the UI together. Every object in the schema
  needs `required` listing all properties and `additionalProperties: false`.
- Conversation history is **append-only**: replay assistant responses with
  `BetaMessage.toParam()` and never edit or drop earlier turns, or thinking-block validation fails.
- Server-side refusal fallbacks (`fallbacksDefault()` + beta `server-side-fallback-2026-07-01`) are
  sent for Opus and Sonnet only; Haiku doesn't support them.
- API calls are blocking; run them on `Dispatchers.IO`.

## Security

- Never log, print or hard-code API keys. Keys are only stored via `SettingsStore`.
- The app must not talk to any server other than `api.anthropic.com`.

## Style

- Follow the existing code: official Kotlin style, small files, sparse comments that explain why.
- Learner-facing UI text is English; scenario titles and placeholders are Italian.
