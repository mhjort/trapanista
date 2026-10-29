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
  Settings also has the level (A1-B2) and the Claude model (Sonnet 5.5 by default; Opus 5.5 gives the
  best feedback, Haiku 5.5 costs least).
- **Gemini Live (Firebase AI Logic):** voice uses the app's Firebase project, so users need no
  Google key, and the project owner pays for voice usage. Settings has the voice model:
  `gemini-3.8-live` by default, or `gemini-3.1-flash-live-preview`.

## Firebase setup (for voice mode)

The app builds and text mode works without this; voice mode shows a setup message until it's done.
Only `app/google-services.json` and the App Check debug tokens are per-developer; everything else
is in the code.

1. **Create the project.** In the [Firebase console](https://console.firebase.google.com), create a
   project. Google Analytics isn't needed.
2. **Register the Android app.** Add an Android app with package name `app.trapanista` (no SHA-1
   needed). Download `google-services.json` into `app/`; it is git-ignored and must not be
   committed. Skip the console's Gradle and SDK steps: the build applies the google-services
   plugin automatically when the file exists.
3. **Turn on AI Logic.** Open **AI Logic > Get started** and choose the **Gemini Developer API**
   (not Vertex AI); the app uses `GenerativeBackend.googleAI()`. This enables the Firebase AI Logic
   API and the Gemini API. After enabling, allow a few minutes: until the change has propagated,
   some connections still fail.
4. **Set up App Check.** Open **App Check > Get started**; this enables the App Check API.
   - Debug builds use the debug provider. Build and install, start a voice conversation once, then
     read the token:
     ```sh
     adb logcat -d | grep "App Check debug token"
     ```
     Add it under **App Check > Apps > ⋮ on app.trapanista > Manage debug tokens**. Each install
     (emulator, phone) has its own token, and it changes if the app is uninstalled or its data is
     cleared. Treat tokens as secrets and delete them when no longer needed.
   - Release builds use Play Integrity: register `app.trapanista` with the **Play Integrity**
     provider and add the release key's SHA-256 fingerprint before publishing.
5. **Enforce App Check.** Under **App Check > APIs**, enforce it for **Firebase AI Logic** (required
   by Firebase from November 2, 2026).
6. **Rebuild** (`./gradlew installDebug`) and try **Speak** on a scenario.

### Troubleshooting voice

- **"Channel was closed by the server"**: the server rejected the Live API handshake. The SDK
  doesn't show why; the usual causes are AI Logic not enabled or not yet propagated (step 3), the
  App Check API not enabled (step 4), or an unregistered debug token. App Check problems show up in
  Logcat as `Error obtaining AppCheck token` with the reason.
- **Voice shows the setup message**: `app/google-services.json` is missing or the app wasn't
  rebuilt after adding it.
- **Gemini answers itself**: its voice is reaching the mic. The app plays Gemini as call audio so
  the phone's echo cancellation removes it; on the emulator, use headphones.

## Building and running

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug assembleRelease   # both must compile
./gradlew installDebug                    # install on the connected device or emulator
```

With several devices attached, pick one with `ANDROID_SERIAL=<serial> ./gradlew installDebug`
(serials from `adb devices`).

### Phone over USB (Linux)

1. On the phone, enable **Developer options** (tap **Build number** seven times) and turn on
   **USB debugging**.
2. If `adb devices` shows `no permissions`, add a udev rule for the phone's USB vendor ID (from
   `lsusb`; `18d1` is Google's, which many phones use), then replug the phone:
   ```sh
   echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0660", GROUP="plugdev", TAG+="uaccess"' \
     | sudo tee /etc/udev/rules.d/51-android.rules
   sudo udevadm control --reload-rules && sudo udevadm trigger
   ```
3. If it shows `unauthorized`, accept **Allow USB debugging?** on the phone.

### Emulator

Create an AVD with a Google Play system image (API 35), then start it with host audio so voice mode
can use your microphone:

```sh
emulator -avd <name> -allow-host-audio
```

If the mic isn't heard, turn on **⋯ > Microphone > Virtual microphone uses host audio input**. If
the emulator crashes with `BadWindow` on Linux, `QT_QPA_PLATFORM` is probably set to `offscreen`
in your environment; start it with `QT_QPA_PLATFORM=xcb emulator ...`.

## Code layout

All code is Kotlin, under `app/src/main/kotlin/app/trapanista/`:

- `data/Scenario.kt`: the role-play situations; add new ones here
- `data/SettingsStore.kt`: API key (encrypted), level, Claude and Gemini models
- `llm/VoicePartner.kt`: Gemini Live session (prompt, transcripts, `end_scene` function)
- `llm/ReviewClient.kt`: Claude review of a voice transcript
- `llm/TutorClient.kt`: Claude text tutor; `llm/Claude.kt` holds the shared API call
- `ui/`: Jetpack Compose screens and view models
- `TrapanistaApp.kt`, `src/{debug,release}/.../AppCheck.kt`: Firebase and App Check setup
