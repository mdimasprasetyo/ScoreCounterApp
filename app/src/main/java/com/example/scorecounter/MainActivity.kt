package com.example.scorecounter

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    /** One point in the score history, used for Undo. */
    private data class Snapshot(val left: Int, val right: Int, val highlight: Int)

    private var leftScore = 0
    private var rightScore = 0

    // Last side that scored: HIGHLIGHT_NONE, HIGHLIGHT_LEFT or HIGHLIGHT_RIGHT
    private var highlight = HIGHLIGHT_NONE

    // Every score change pushes the previous state here, so Undo can step back exactly
    private val undoStack = java.util.ArrayDeque<Snapshot>()

    private lateinit var scoreLeft: TextView
    private lateinit var scoreRight: TextView
    private lateinit var leftNameInput: EditText
    private lateinit var rightNameInput: EditText
    private lateinit var sessionInput: EditText
    private lateinit var sessionList: ListView
    private lateinit var cardLeft: MaterialCardView
    private lateinit var cardRight: MaterialCardView
    private lateinit var labelLeftLast: TextView
    private lateinit var labelRightLast: TextView
    private var leftColor = 0
    private var rightColor = 0

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SessionAdapter
    private val sessions = mutableListOf<SessionAdapter.Session>()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("score_prefs", MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Both sides use the same card layout (view_side_card), so look children up inside each card
        cardLeft = findViewById(R.id.card_left)
        cardRight = findViewById(R.id.card_right)
        scoreLeft = cardLeft.findViewById(R.id.side_score)
        scoreRight = cardRight.findViewById(R.id.side_score)
        leftNameInput = cardLeft.findViewById(R.id.side_name)
        rightNameInput = cardRight.findViewById(R.id.side_name)
        labelLeftLast = cardLeft.findViewById(R.id.last_pill)
        labelRightLast = cardRight.findViewById(R.id.last_pill)
        sessionInput = findViewById(R.id.edit_session_name)
        sessionList = findViewById(R.id.session_list)

        leftColor = ContextCompat.getColor(this, R.color.side_left)
        rightColor = ContextCompat.getColor(this, R.color.side_right)
        setUpSide(cardLeft, R.string.side_left, leftColor)
        setUpSide(cardRight, R.string.side_right, rightColor)

        // The live game is stored on every change, so it survives rotation AND the app being killed
        restoreLiveGame()
        savedInstanceState?.getIntArray(KEY_UNDO_STACK)?.let { restoreUndoStack(it) }
        renderScores()

        val keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        applyKeepScreenOn(keepScreenOn)

        adapter = SessionAdapter(this, sessions) {
            saveSessions()
        }
        sessionList.adapter = adapter
        sessionList.emptyView = findViewById(R.id.empty_sessions)

        loadSessions()

        // Tap anywhere on a score card to add a point
        cardLeft.setOnClickListener { onScoreTap(it, true, 1) }
        cardRight.setOnClickListener { onScoreTap(it, false, 1) }

        cardLeft.findViewById<Button>(R.id.side_plus).setOnClickListener { onScoreTap(it, true, 1) }
        cardLeft.findViewById<Button>(R.id.side_minus).setOnClickListener { onScoreTap(it, true, -1) }
        cardRight.findViewById<Button>(R.id.side_plus).setOnClickListener { onScoreTap(it, false, 1) }
        cardRight.findViewById<Button>(R.id.side_minus).setOnClickListener { onScoreTap(it, false, -1) }

        findViewById<Button>(R.id.btn_reset).setOnClickListener { showResetConfirmationDialog() }
        findViewById<Button>(R.id.btn_settings).setOnClickListener { showSettingsDialog() }

        findViewById<Button>(R.id.btn_undo).setOnClickListener {
            undoLastChange(it)
            hideKeyboard()
        }

        findViewById<Button>(R.id.btn_save_session).setOnClickListener {
            saveCurrentSession()
        }

        // Side names are part of the live game too
        leftNameInput.addTextChangedListener(prefWatcher(KEY_LIVE_LEFT_NAME))
        rightNameInput.addTextChangedListener(prefWatcher(KEY_LIVE_RIGHT_NAME))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(KEY_UNDO_STACK, flattenUndoStack())
    }

    // --- Scoring ---

    private fun onScoreTap(view: View, isLeft: Boolean, delta: Int) {
        if (updateScore(isLeft, delta)) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        hideKeyboard()
    }

    /**
     * Applies a +1 / -1 to one side. Returns false when nothing changed (minus at 0).
     * Only a point scored (+1) moves the "last scored" highlight; a correction (-1) leaves it alone.
     */
    private fun updateScore(isLeft: Boolean, delta: Int): Boolean {
        val current = if (isLeft) leftScore else rightScore
        val next = (current + delta).coerceAtLeast(0)
        if (next == current) return false

        pushUndo()
        if (isLeft) leftScore = next else rightScore = next
        if (delta > 0) highlight = if (isLeft) HIGHLIGHT_LEFT else HIGHLIGHT_RIGHT

        renderScores()
        persistLiveScore()
        return true
    }

    private fun undoLastChange(view: View) {
        if (undoStack.isEmpty()) {
            Toast.makeText(this, R.string.nothing_to_undo, Toast.LENGTH_SHORT).show()
            return
        }
        val previous = undoStack.removeLast()
        leftScore = previous.left
        rightScore = previous.right
        highlight = previous.highlight
        renderScores()
        persistLiveScore()
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun pushUndo() {
        undoStack.addLast(Snapshot(leftScore, rightScore, highlight))
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
    }

    private fun renderScores() {
        scoreLeft.text = leftScore.toString()
        scoreRight.text = rightScore.toString()

        styleCard(cardLeft, scoreLeft, labelLeftLast, highlight == HIGHLIGHT_LEFT)
        styleCard(cardRight, scoreRight, labelRightLast, highlight == HIGHLIGHT_RIGHT)
    }

    /** Side colour on the strip, the tag and the +1 button (amber left, sky right, as in Pickles). */
    private fun setUpSide(card: MaterialCardView, tagRes: Int, color: Int) {
        card.findViewById<View>(R.id.side_bar).setBackgroundColor(color)
        card.findViewById<TextView>(R.id.side_tag).apply {
            setText(tagRes)
            setTextColor(color)
        }
        card.findViewById<Button>(R.id.side_plus).backgroundTintList =
            android.content.res.ColorStateList.valueOf(color)
    }

    /**
     * The side that scored last gets a faint wash of its colour and the Volt LAST POINT pill,
     * like the serving row on the Pickles scoreboard.
     */
    private fun styleCard(card: MaterialCardView, score: TextView, label: TextView, isLast: Boolean) {
        val panel = ContextCompat.getColor(this, R.color.panel)
        val sideColor = if (card === cardLeft) leftColor else rightColor
        val white = ContextCompat.getColor(this, R.color.white)
        if (isLast) {
            card.setCardBackgroundColor(ColorUtils.blendARGB(panel, sideColor, 0.12f))
            score.setTextColor(white)
            label.visibility = View.VISIBLE
        } else {
            card.setCardBackgroundColor(panel)
            score.setTextColor(ColorUtils.setAlphaComponent(white, 0xD9))
            label.visibility = View.INVISIBLE
        }
    }

    // --- Sessions ---

    private fun saveCurrentSession() {
        val now = Date()
        var name = sessionInput.text.toString().trim()
        if (name.isEmpty()) {
            name = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(now)
        }

        val leftName = leftNameInput.text.toString().trim().ifEmpty { "Left" }
        val rightName = rightNameInput.text.toString().trim().ifEmpty { "Right" }

        sessions.add(
            0,
            SessionAdapter.Session(name, leftName, rightName, leftScore, rightScore, now.time)
        )
        adapter.notifyDataSetChanged()
        saveSessions()

        clearBoard()
    }

    private fun saveSessions() {
        val json = gson.toJson(sessions)
        prefs.edit { putString(KEY_SESSIONS, json) }
    }

    private fun loadSessions() {
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<SessionAdapter.Session>>() {}.type
            val loaded = gson.fromJson<MutableList<SessionAdapter.Session>>(json, type)
            sessions.clear()
            sessions.addAll(loaded)
        }
        adapter.notifyDataSetChanged()
    }

    // --- Reset ---

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset scores?")
            .setMessage("This will clear scores, names, session input, and highlights. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                clearBoard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Clears scores, names, session name, highlight and undo history (after Save or Reset). */
    private fun clearBoard() {
        leftScore = 0
        rightScore = 0
        highlight = HIGHLIGHT_NONE
        undoStack.clear()
        leftNameInput.text.clear()
        rightNameInput.text.clear()
        sessionInput.text.clear()
        renderScores()
        persistLiveScore()
        hideKeyboard()
    }

    // --- Persistence of the live game ---

    private fun persistLiveScore() {
        prefs.edit {
            putInt(KEY_LIVE_LEFT, leftScore)
            putInt(KEY_LIVE_RIGHT, rightScore)
            putInt(KEY_LIVE_HIGHLIGHT, highlight)
        }
    }

    private fun restoreLiveGame() {
        leftScore = prefs.getInt(KEY_LIVE_LEFT, 0)
        rightScore = prefs.getInt(KEY_LIVE_RIGHT, 0)
        highlight = prefs.getInt(KEY_LIVE_HIGHLIGHT, HIGHLIGHT_NONE)
        leftNameInput.setText(prefs.getString(KEY_LIVE_LEFT_NAME, ""))
        rightNameInput.setText(prefs.getString(KEY_LIVE_RIGHT_NAME, ""))
    }

    private fun prefWatcher(key: String) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            prefs.edit { putString(key, s?.toString().orEmpty()) }
        }
    }

    private fun flattenUndoStack(): IntArray =
        undoStack.flatMap { listOf(it.left, it.right, it.highlight) }.toIntArray()

    private fun restoreUndoStack(flat: IntArray) {
        undoStack.clear()
        for (i in 0 until flat.size / 3) {
            undoStack.addLast(Snapshot(flat[i * 3], flat[i * 3 + 1], flat[i * 3 + 2]))
        }
    }

    // --- Misc ---

    private fun showSettingsDialog() {
        val current = booleanArrayOf(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setMultiChoiceItems(arrayOf(getString(R.string.keep_screen_on)), current) { _, _, checked ->
                prefs.edit { putBoolean(KEY_KEEP_SCREEN_ON, checked) }
                applyKeepScreenOn(checked)
            }
            .setPositiveButton(R.string.done, null)
            .show()
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()

                    // Hide keyboard
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    companion object {
        private const val HIGHLIGHT_NONE = -1
        private const val HIGHLIGHT_LEFT = 0
        private const val HIGHLIGHT_RIGHT = 1
        private const val MAX_UNDO = 200

        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SESSIONS = "saved_sessions"
        private const val KEY_LIVE_LEFT = "live_left_score"
        private const val KEY_LIVE_RIGHT = "live_right_score"
        private const val KEY_LIVE_HIGHLIGHT = "live_highlight"
        private const val KEY_LIVE_LEFT_NAME = "live_left_name"
        private const val KEY_LIVE_RIGHT_NAME = "live_right_name"
        private const val KEY_UNDO_STACK = "undo_stack"
    }
}
