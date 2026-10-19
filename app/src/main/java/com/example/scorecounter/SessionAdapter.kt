package com.example.scorecounter

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.Date

class SessionAdapter(
    private val context: Context,
    private val sessions: MutableList<Session>,
    private val onSessionsChanged: () -> Unit
) : BaseAdapter() {

    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

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

        // Sessions saved before v1.1 have no savedAt, so they show the score only
        val savedAt = session.savedAt
        text2.text = if (savedAt != null) {
            context.getString(
                R.string.session_score_text_dated,
                session.leftName,
                session.leftScore,
                session.rightName,
                session.rightScore,
                dateFormat.format(Date(savedAt))
            )
        } else {
            context.getString(
                R.string.session_score_text,
                session.leftName,
                session.leftScore,
                session.rightName,
                session.rightScore
            )
        }

        view.setOnLongClickListener {
            // Anchor the Snackbar to the screen content so it survives the row being recycled
            val anchor = (context as? android.app.Activity)?.findViewById<View>(android.R.id.content) ?: it
            showOptionsDialog(position, anchor)
            true
        }

        return view
    }

    private fun showOptionsDialog(index: Int, anchor: View) {
        val options = arrayOf("Edit Name", "Delete")
        AlertDialog.Builder(context)
            .setTitle("Session Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(index)
                    1 -> deleteWithUndo(index, anchor)
                }
            }.show()
    }

    /** Deletes right away, but offers Undo for a few seconds. */
    private fun deleteWithUndo(index: Int, anchor: View) {
        if (index !in sessions.indices) return
        val removed = sessions.removeAt(index)
        onSessionsChanged()
        notifyDataSetChanged()

        Snackbar.make(anchor, R.string.session_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                sessions.add(index.coerceAtMost(sessions.size), removed)
                onSessionsChanged()
                notifyDataSetChanged()
            }
            .show()
    }

    private fun showEditDialog(index: Int) {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(sessions[index].name)

        AlertDialog.Builder(context)
            .setTitle("Edit Session Name")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                // A blank name would leave an empty row, so keep the old name instead
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && index in sessions.indices) {
                    sessions[index].name = newName
                    onSessionsChanged()
                    notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class Session(
        var name: String,
        val leftName: String,
        val rightName: String,
        val leftScore: Int,
        val rightScore: Int,
        val savedAt: Long? = null
    )
}
