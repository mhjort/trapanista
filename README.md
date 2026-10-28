# Trapanista

An Android app for practising Italian through short role-play conversations in everyday
situations: ordering at a restaurant, booking a hotel room, buying a train ticket, and so on.
Each situation can be practised by voice or by text.

**Speak** (Gemini Live): a real-time spoken conversation. Gemini plays the other person in
Italian and the transcript appears as you talk. Interrupting is allowed. When you stop, Claude
reviews the transcript: what went well, corrections for each line you said, and useful phrases.

**Type** (Claude): a text chat. Each turn the tutor replies in character, and also:

- corrects mistakes in your last message (shown under your message)
- gives the English translation (tap the tutor's message)
- suggests 2-3 things you could say next (tap one to fill the input)
- marks the scene complete when you reach the goal

## Keys and accounts

- **Claude (bring your own key):** text chat and voice feedback call the Anthropic API directly
  with the user's own key. Get one at [console.anthropic.com](https://console.anthropic.com) and
  paste it in **Settings**. It is encrypted with an Android Keystore key and only sent to Anthropic.
  Settings also has the level (A1-B2) and the Claude model (Opus 5.5 by default; Sonnet 5.5 and
  Haiku 5.5 cost less).
- **Gemini Live (Firebase AI Logic):** voice uses the app's Firebase project, so users need no
  Google key, and the project owner pays for voice usage. Settings has the voice model:
  `gemini-3.8-live` by default, or `gemini-3.1-flash-live-preview`.

## Firebase setup (for voice mode)

The app builds and text mode works without this; voice mode shows a setup message until it's done.

1. In the [Firebase console](https://console.firebase.google.com), create a project and add an
   Android app with package name `app.trapanista`.
2. Download `google-services.json` into `app/`. It is git-ignored.
3. Under **AI Logic**, run the guided setup with the **Gemini Developer API**.
4. App Check is enforced for AI Logic. For debug builds, run the app once, copy the debug token
   from Logcat (tag `DebugAppCheckProvider`), and add it under **App Check > Apps > Manage debug
   tokens**. Release builds use Play Integrity; register the app's SHA-256 fingerprint before
   publishing.

## Building

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Code layout

All code is Kotlin, under `app/src/main/kotlin/app/trapanista/`:

- `data/Scenario.kt`: the role-play situations; add new ones here
- `data/SettingsStore.kt`: API key (encrypted), level, Claude and Gemini models
- `llm/VoicePartner.kt`: Gemini Live session (prompt, transcripts, `end_scene` function)
- `llm/ReviewClient.kt`: Claude review of a voice transcript
- `llm/TutorClient.kt`: Claude text tutor; `llm/Claude.kt` holds the shared API call
- `ui/`: Jetpack Compose screens and view models
- `TrapanistaApp.kt`, `src/{debug,release}/.../AppCheck.kt`: Firebase and App Check setup
