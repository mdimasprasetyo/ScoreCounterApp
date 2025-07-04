package com.example.scorecounter

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private var leftScore = 0
    private var rightScore = 0

    // Track last highlighted side (true = left, false = right, null = none)
    private var lastHighlightedLeft: Boolean? = null

    private lateinit var scoreLeft: TextView
    private lateinit var scoreRight: TextView
    private lateinit var leftNameInput: EditText
    private lateinit var rightNameInput: EditText
    private lateinit var sessionInput: EditText
    private lateinit var sessionList: ListView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var darkSwitch: Switch

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SessionAdapter
    private val sessions = mutableListOf<SessionAdapter.Session>()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("score_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scoreLeft = findViewById(R.id.score_left)
        scoreRight = findViewById(R.id.score_right)
        leftNameInput = findViewById(R.id.edit_left_name)
        rightNameInput = findViewById(R.id.edit_right_name)
        sessionInput = findViewById(R.id.edit_session_name)
        sessionList = findViewById(R.id.session_list)
        darkSwitch = findViewById(R.id.switch_dark_mode)

        if (savedInstanceState != null) {
            leftScore = savedInstanceState.getInt("leftScore", 0)
            rightScore = savedInstanceState.getInt("rightScore", 0)
            lastHighlightedLeft = if (savedInstanceState.containsKey("lastHighlightedLeft")) {
                savedInstanceState.getBoolean("lastHighlightedLeft")
            } else null
        }

        scoreLeft.text = leftScore.toString()
        scoreRight.text = rightScore.toString()

        lastHighlightedLeft?.let { highlightLastScore(it) }

        darkSwitch.isChecked = isDark
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("dark_mode", checked) }
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        adapter = SessionAdapter(this, sessions) {
            saveSessions()
        }
        sessionList.adapter = adapter

        loadSessions()

        findViewById<Button>(R.id.btn_left_up).setOnClickListener {
            updateScore(true, 1)
            hideKeyboard()
        }
        findViewById<Button>(R.id.btn_left_down).setOnClickListener {
            updateScore(true, -1)
            hideKeyboard()
        }
        findViewById<Button>(R.id.btn_right_up).setOnClickListener {
            updateScore(false, 1)
            hideKeyboard()
        }
        findViewById<Button>(R.id.btn_right_down).setOnClickListener {
            updateScore(false, -1)
            hideKeyboard()
        }

        findViewById<Button>(R.id.btn_save_session).setOnClickListener {
            var name = sessionInput.text.toString().trim()

            if (name.isEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                name = sdf.format(Date())
            }

            val leftName = leftNameInput.text.toString().trim().ifEmpty { "Left" }
            val rightName = rightNameInput.text.toString().trim().ifEmpty { "Right" }

            sessions.add(0, SessionAdapter.Session(name, leftName, rightName, leftScore, rightScore))
            adapter.notifyDataSetChanged()
            saveSessions()

            leftScore = 0
            rightScore = 0
            scoreLeft.text = "0"
            scoreRight.text = "0"
            leftNameInput.text.clear()
            rightNameInput.text.clear()
            sessionInput.text.clear()

            hideKeyboard()
            clearScoreHighlights()
        }

        findViewById<Button>(R.id.btn_reset_scores).setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("leftScore", leftScore)
        outState.putInt("rightScore", rightScore)
        lastHighlightedLeft?.let { outState.putBoolean("lastHighlightedLeft", it) }
    }

    private fun updateScore(isLeft: Boolean, delta: Int) {
        if (isLeft) {
            leftScore = (leftScore + delta).coerceAtLeast(0)
            scoreLeft.text = leftScore.toString()
            highlightLastScore(true)
        } else {
            rightScore = (rightScore + delta).coerceAtLeast(0)
            scoreRight.text = rightScore.toString()
            highlightLastScore(false)
        }
    }

    private fun highlightLastScore(isLeft: Boolean) {
        val highlightDrawable = ContextCompat.getDrawable(this, R.drawable.bg_last_score)
        val transparent = ContextCompat.getDrawable(this, android.R.color.transparent)

        scoreLeft.background = if (isLeft) highlightDrawable else transparent
        scoreRight.background = if (!isLeft) highlightDrawable else transparent

        lastHighlightedLeft = isLeft
    }

    private fun clearScoreHighlights() {
        val transparent = ContextCompat.getDrawable(this, android.R.color.transparent)
        scoreLeft.background = transparent
        scoreRight.background = transparent
        lastHighlightedLeft = null
    }

    private fun saveSessions() {
        val json = gson.toJson(sessions)
        prefs.edit { putString("saved_sessions", json) }
    }

    private fun loadSessions() {
        val json = prefs.getString("saved_sessions", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<SessionAdapter.Session>>() {}.type
            val loaded = gson.fromJson<MutableList<SessionAdapter.Session>>(json, type)
            sessions.clear()
            sessions.addAll(loaded)
        }
        adapter.notifyDataSetChanged()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Scores?")
            .setMessage("This will clear scores, names, session input, and highlights. Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                resetScoresAndInputs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetScoresAndInputs() {
        leftScore = 0
        rightScore = 0
        scoreLeft.text = "0"
        scoreRight.text = "0"
        leftNameInput.text.clear()
        rightNameInput.text.clear()
        sessionInput.text.clear()
        clearScoreHighlights()
        hideKeyboard()
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
}
