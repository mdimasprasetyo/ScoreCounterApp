package com.example.scorecounter

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private var leftScore = 0
    private var rightScore = 0

    private lateinit var scoreLeft: TextView
    private lateinit var scoreRight: TextView
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

        adapter = SessionAdapter(this, sessions)
        sessionList.adapter = adapter

        findViewById<Button>(R.id.btn_left_up).setOnClickListener { updateScore(true, 1) }
        findViewById<Button>(R.id.btn_left_down).setOnClickListener { updateScore(true, -1) }
        findViewById<Button>(R.id.btn_right_up).setOnClickListener { updateScore(false, 1) }
        findViewById<Button>(R.id.btn_right_down).setOnClickListener { updateScore(false, -1) }

        findViewById<Button>(R.id.btn_save_session).setOnClickListener {
            val name = sessionInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a session name", Toast.LENGTH_SHORT).show()
            } else {
                sessions.add(SessionAdapter.Session(name, leftScore, rightScore))
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateScore(isLeft: Boolean, delta: Int) {
        if (isLeft) {
            leftScore = (leftScore + delta).coerceAtLeast(0)
            scoreLeft.text = leftScore.toString()
        } else {
            rightScore = (rightScore + delta).coerceAtLeast(0)
            scoreRight.text = rightScore.toString()
        }
    }
}
