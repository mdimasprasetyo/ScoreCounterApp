# ScoreCounterApp

An Android app written in Kotlin to keep track of scores for two sides. Designed for quick and easy score counting with session saving and customization features.

## Features

- Increment and decrement scores for left and right sides
- Save score sessions with custom or auto-generated timestamp-based names
- Edit session names after saving
- Delete saved sessions using a trash icon (via long-press on session)
- Dark mode toggle
- Responsive and simple UI
- Reset button to reset scores, text input (side name and session name), and clear score highlight (Update 20250621)

## How to Use

1. Adjust scores using the "+" and "-" buttons for each side.
2. Enter a session name or leave it blank to use a default timestamp.
3. Tap "Save Session" to store the current scores.
4. View saved sessions in the list below.
5. Long-press on any saved session to edit its name or delete it.
6. Toggle dark mode using the switch at the top.

## Technical Details

- Written in Kotlin
- Uses SharedPreferences for persisting sessions and dark mode setting
- Custom `SessionAdapter` for displaying and managing saved sessions
