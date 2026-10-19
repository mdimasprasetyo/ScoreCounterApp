# ScoreCounterApp

A simple two-sided score counter for Android. Tap **+** or **−** for the left or right side, name
each side if you like, and save the result as a session you can rename or delete later. Works for
any game where two sides keep a running score.

- Kotlin, classic Android Views (XML layouts), Material Components, minSdk 21 (Android 5.0), targetSdk 34
- Portrait and landscape layouts
- No network permission, no login, no ads, no analytics
- Sessions and the dark-mode setting saved on the device with SharedPreferences (JSON via Gson)
- The side that scored last is highlighted; scores never go below 0

## Project layout

```
ScoreCounterApp/
└── app/                             Android app
    └── src/main/
        ├── java/com/example/scorecounter/
        │   ├── MainActivity.kt      Scores, highlight, save/reset, dark mode, persistence
        │   └── SessionAdapter.kt    Saved-sessions list, long-press rename / delete, Session model
        └── res/
            ├── layout/              activity_main.xml (portrait)
            ├── layout-land/         activity_main.xml (landscape)
            ├── values/, values-night/   Colours, strings, dimensions, theme (light and dark)
            └── drawable/            bg_last_score.xml (last-scored highlight)
```

### Architecture in one paragraph

`MainActivity` holds the two scores and which side scored last. Every **+** / **−** goes through
`updateScore(isLeft, delta)`, which clamps at 0, updates the number and moves the highlight. Scores
and the highlight survive screen rotation through `onSaveInstanceState`. Saved sessions are a list
of `Session(name, leftName, rightName, leftScore, rightScore)`, newest first, stored as one JSON
string in SharedPreferences and rewritten whenever a session is added, renamed or deleted.
`SessionAdapter` renders the list and reports changes back so they are saved straight away.

## Features

| Behaviour | Where |
|---|---|
| **+** / **−** per side, never below 0 | `MainActivity.updateScore` |
| Last side to score is highlighted | `highlightLastScore` |
| Optional side names (default *Left* / *Right*) | Save Session handler |
| Session name optional; blank uses a `yyyy-MM-dd HH:mm:ss` timestamp | Save Session handler |
| Saving a session resets scores, names and highlight for the next game | Save Session handler |
| Long-press a saved session → **Edit Name** or **Delete** | `SessionAdapter.showOptionsDialog` |
| **Reset** (with confirmation) clears scores, names, session name and highlight without saving | `showResetConfirmationDialog` |
| Dark mode switch, remembered between launches | `onCreate` |
| Tapping outside a text field hides the keyboard | `dispatchTouchEvent` |

## Build

Requirements: JDK 17 and the Android SDK (platform 34). The project uses AGP 8.13.2, Kotlin 1.9.23
and Gradle 8.13 (wrapper included).

### Android Studio
1. *File → Open…* and choose the `ScoreCounterApp` folder.
2. Let Gradle sync, then press **Run** with a device or emulator selected.

### Command line
```bash
cd ScoreCounterApp
# point Gradle at your SDK once
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # app/build/outputs/apk/release/ (unsigned, not minified)
```

The release build is not signed. Before publishing, create your own keystore and add a
`signingConfig` in `app/build.gradle`.

## Install on a phone

**From the APK file**
1. Copy `app-debug.apk` to the phone.
2. Open it from the Files app. If asked, allow *Install unknown apps* for that app.
3. Tap **Install**, then open **Score Counter**.

**With USB debugging**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Using the app

1. Optionally type a name for each side.
2. Tap **+** or **−** under each score. The side that scored last is highlighted.
3. Optionally type a session name, then tap **Save Session**. The result appears at the top of the
   list and the board clears for the next game.
4. Long-press a saved session to rename or delete it.
5. **Reset** clears the board without saving. The **Dark Mode** switch at the top changes the theme.

## Changelog

- 2025-06-21: **Reset** button clears scores, side names, session name and the score highlight.
