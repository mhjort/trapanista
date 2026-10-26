# Trapanista

An Android app for practising Italian through short role-play conversations in everyday
situations: ordering at a restaurant, booking a hotel room, buying a train ticket, and so on.

Each turn the tutor (Claude) replies in character in Italian, at your CEFR level, and also:

- corrects mistakes in your last message (shown under your message)
- gives the English translation (tap the tutor's message)
- suggests 2-3 things you could say next (tap one to fill the input)
- marks the scene complete when you reach the goal

## Bring your own key

The app calls the Anthropic API directly with your own key. Get one at
[console.anthropic.com](https://console.anthropic.com) and paste it in **Settings**. It is encrypted with
an Android Keystore key and only ever sent to Anthropic. You can also choose your level (A1-B2)
and the model (Opus 5.5 by default; Sonnet 5.5 and Haiku 5.5 cost less).

## Building

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Code layout

All code is Kotlin, under `app/src/main/kotlin/app/trapanista/`:

- `data/Scenario.kt`: the role-play situations; add new ones here
- `data/SettingsStore.kt`: API key (encrypted), level and model
- `llm/TutorClient.kt`: system prompt, JSON output schema and the Claude API call
- `ui/`: Jetpack Compose screens and the chat view model
