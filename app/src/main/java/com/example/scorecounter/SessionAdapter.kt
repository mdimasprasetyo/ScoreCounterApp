package com.example.scorecounter

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class SessionAdapter(
    private val context: Context,
    private val sessions: MutableList<Session>
) : BaseAdapter() {

    override fun getCount(): Int = sessions.size

    override fun getItem(position: Int): Any = sessions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        val session = sessions[position]
        text1.text = session.name
        text2.text = context.getString(
            R.string.session_score_text,
            session.leftName,
            session.leftScore,
            session.rightName,
            session.rightScore
        )

        view.setOnLongClickListener {
            showOptionsDialog(position)
            true
        }

        return view
    }

    private fun showOptionsDialog(index: Int) {
        val options = arrayOf("Edit Name", "Delete")
        AlertDialog.Builder(context)
            .setTitle("Session Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(index)
                    1 -> {
                        sessions.removeAt(index)
                        notifyDataSetChanged()
                    }
                }
            }.show()
    }

    private fun showEditDialog(index: Int) {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(sessions[index].name)

        AlertDialog.Builder(context)
            .setTitle("Edit Session Name")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                sessions[index].name = input.text.toString()
                notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class Session(
        var name: String,
        val leftName: String,
        val rightName: String,
        val leftScore: Int,
        val rightScore: Int
    )
}
