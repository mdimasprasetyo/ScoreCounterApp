package com.example.scorecounter

import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var leftScore = 0
    private var rightScore = 0

    private lateinit var scoreLeft: TextView
    private lateinit var scoreRight: TextView
    private lateinit var leftNameInput: EditText
    private lateinit var rightNameInput: EditText
    private lateinit var sessionInput: EditText
    private lateinit var sessionList: ListView
    private lateinit var darkSwitch: Switch

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SessionAdapter
    private val sessions = mutableListOf<SessionAdapter.Session>()

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

        darkSwitch.isChecked = isDark
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_mode", checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        adapter = SessionAdapter(this, sessions) {
            saveSessions()
        }
        sessionList.adapter = adapter

        // Load previously saved sessions
        loadSessions()
        adapter.notifyDataSetChanged()

        findViewById<Button>(R.id.btn_left_up).setOnClickListener { updateScore(true, 1) }
        findViewById<Button>(R.id.btn_left_down).setOnClickListener { updateScore(true, -1) }
        findViewById<Button>(R.id.btn_right_up).setOnClickListener { updateScore(false, 1) }
        findViewById<Button>(R.id.btn_right_down).setOnClickListener { updateScore(false, -1) }

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

            // Hide keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let { view ->
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }

            // Reset highlight after save
            clearScoreHighlights()
        }
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
    }

    private fun clearScoreHighlights() {
        val transparent = ContextCompat.getDrawable(this, android.R.color.transparent)
        scoreLeft.background = transparent
        scoreRight.background = transparent
    }

    private fun saveSessions() {
        val json = Gson().toJson(sessions)
        prefs.edit().putString("saved_sessions", json).apply()
    }

    private fun loadSessions() {
        val json = prefs.getString("saved_sessions", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<SessionAdapter.Session>>() {}.type
            val loaded = Gson().fromJson<MutableList<SessionAdapter.Session>>(json, type)
            sessions.clear()
            sessions.addAll(loaded)
        }
    }
}
