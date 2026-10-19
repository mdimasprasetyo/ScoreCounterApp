# ScoreCounterApp

A simple two-sided score counter for Android. Tap a score (or **+** / **−**) for the left or right
side, name each side if you like, and save the result as a session you can rename or delete later. Works for
any game where two sides keep a running score.

- Kotlin, classic Android Views (XML layouts), Material 3, the Pickles look (dark navy, Volt accent,
  Plus Jakarta Sans + Barlow Condensed, both OFL and bundled), minSdk 21 (Android 5.0), targetSdk 34
- Portrait and landscape layouts
- No network permission, no login, no ads, no analytics
- Sessions and settings saved on the device with SharedPreferences (JSON via Gson)
- The side that scored last is highlighted; scores never go below 0
- Undo for every point, haptic feedback, optional keep-screen-on
- The game in progress survives the app being closed or killed

## Project layout

```
ScoreCounterApp/
└── app/                             Android app
    └── src/main/
        ├── java/com/example/scorecounter/
        │   ├── MainActivity.kt      Scores, undo, highlight, save/reset, settings, keep-awake, persistence
        │   └── SessionAdapter.kt    Saved-sessions list, long-press rename / delete, Session model
        └── res/
            ├── layout/              activity_main.xml (portrait), view_side_card.xml, item_session.xml
            ├── layout-land/         activity_main.xml (landscape)
            ├── font/                Plus Jakarta Sans + Barlow Condensed (OFL, licences in assets/)
            └── values/              Pickles palette, strings, dimensions, dark theme
```

### Architecture in one paragraph

`MainActivity` holds the two scores and which side scored last. Every tap, **+** or **−** goes
through `updateScore(isLeft, delta)`, which clamps at 0, pushes the previous state onto the undo
stack, updates the number, moves the highlight on a point, and writes the live game to
SharedPreferences, so it survives rotation and the app being killed. Saved sessions are a list
of `Session(name, leftName, rightName, leftScore, rightScore)`, newest first, stored as one JSON
string in SharedPreferences and rewritten whenever a session is added, renamed or deleted.
`SessionAdapter` renders the list and reports changes back so they are saved straight away.

## Features

| Behaviour | Where |
|---|---|
| Tap anywhere on a score to add a point | `score_left_area` / `score_right_area` |
| **+** / **−** per side, never below 0; **−** at 0 does nothing | `MainActivity.updateScore` |
| Last side to *score* (+1) is highlighted; a correction (−1) doesn't move it | `updateScore` |
| **Undo** steps back through every score change, highlight included (up to 200) | `undoLastChange` |
| Haptic tick on every point and undo | `onScoreTap` |
| Scores, highlight and side names saved on every change; restored after the app is killed | `persistLiveScore`, `restoreLiveGame` |
| Optional side names (default *Left* / *Right*) | Save Session handler |
| Session name optional; blank uses a `yyyy-MM-dd HH:mm:ss` timestamp | Save Session handler |
| Saving a session resets scores, names and highlight for the next game | Save Session handler |
| Each saved session stores its date and time, shown under the score | `Session.savedAt` |
| Long-press a saved session → **Edit Name** or **Delete** (with **Undo** snackbar) | `SessionAdapter.showOptionsDialog` |
| Renaming to a blank name keeps the old name | `SessionAdapter.showEditDialog` |
| **Reset** (with confirmation) clears scores, names, session name and highlight without saving | `showResetConfirmationDialog` |
| Settings → **Keep screen on**, remembered between launches | `showSettingsDialog` |
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
2. Tap a score to add a point (or use **+** / **−**). The side that scored last is highlighted.
   Mis-tap? Hit **Undo**.
3. Optionally type a session name, then tap **Save Session**. The result appears at the top of the
   list and the board clears for the next game.
4. Long-press a saved session to rename or delete it.
5. **Reset** clears the board without saving. **Settings** has **Keep screen on** (stops the phone
   sleeping mid-game).

## Changelog

- v1.1: tap-to-score, Undo, haptics, keep-screen-on; live game survives the app being killed;
  minus no longer moves the highlight; delete has Undo; blank renames ignored; sessions store their date.
- 2025-06-21: **Reset** button clears scores, side names, session name and the score highlight.
