package com.example.to_do_list

import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

data class Task(
    val title: String,
    val description: String,
    val priority: String,
    var isCompleted: Boolean = false
)

class TaskAdapter(private val context: Context, private val tasks: MutableList<Task>) :
    ArrayAdapter<Task>(context, R.layout.task_item, tasks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.task_item, parent, false)

        val task = tasks[position]
        val titleView = view.findViewById<TextView>(R.id.taskTitle)
        val descView = view.findViewById<TextView>(R.id.taskDescription)
        val priorityLabel = view.findViewById<TextView>(R.id.priorityLabel)
        val priorityBar = view.findViewById<View>(R.id.priorityBar)
        val checkbox = view.findViewById<CheckBox>(R.id.taskCheckbox)

        titleView.text = task.title
        descView.text = task.description
        priorityLabel.text = task.priority

        when (task.priority) {
            "High" -> {
                priorityLabel.setTextColor(0xFFFF5252.toInt())
                priorityBar.setBackgroundColor(0xFFFF5252.toInt())
            }
            "Medium" -> {
                priorityLabel.setTextColor(0xFFFFA726.toInt())
                priorityBar.setBackgroundColor(0xFFFFA726.toInt())
            }
            "Low" -> {
                priorityLabel.setTextColor(0xFF42A5F5.toInt())
                priorityBar.setBackgroundColor(0xFF42A5F5.toInt())
            }
        }

        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = task.isCompleted
        if (task.isCompleted) {
            titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            titleView.setTextColor(0xFF999999.toInt())
        } else {
            titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            titleView.setTextColor(0xFF1E1E1E.toInt())
        }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            task.isCompleted = isChecked
            notifyDataSetChanged()
        }

        return view
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var taskInput: EditText
    private lateinit var taskDesc: EditText
    private lateinit var addButton: Button
    private lateinit var taskListView: ListView
    private lateinit var priorityGroup: RadioGroup
    private lateinit var summaryText: TextView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private val PREFS_NAME = "TodoPrefs"
    private val TASKS_KEY = "tasks"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskInput = findViewById(R.id.taskInput)
        taskDesc = findViewById(R.id.taskDesc)
        addButton = findViewById(R.id.addButton)
        taskListView = findViewById(R.id.taskListView)
        priorityGroup = findViewById(R.id.priorityGroup)
        summaryText = findViewById(R.id.summaryText)

        loadTasks()
        taskAdapter = TaskAdapter(this, taskList)
        taskListView.adapter = taskAdapter
        updateSummary()

        addButton.setOnClickListener { addTask() }

        // Tap task → show Edit/Delete options
        taskListView.setOnItemClickListener { _, _, position, _ ->
            val task = taskList[position]
            val options = arrayOf("✏️ Edit Task", "🗑️ Delete Task")

            AlertDialog.Builder(this)
                .setTitle(task.title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> editTask(position)
                        1 -> deleteTask(position)
                    }
                }
                .show()
        }
    }

    private fun editTask(position: Int) {
        val task = taskList[position]
        val dialogView = layoutInflater.inflate(R.layout.activity_main, null)

        val editTitle = EditText(this)
        editTitle.setText(task.title)
        editTitle.hint = "Edit title"

        val editDesc = EditText(this)
        editDesc.setText(task.description)
        editDesc.hint = "Edit description"

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)
        layout.addView(editTitle)
        layout.addView(editDesc)

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = editTitle.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    taskList[position] = Task(newTitle, editDesc.text.toString().trim(), task.priority, task.isCompleted)
                    taskAdapter.notifyDataSetChanged()
                    saveTasks()
                    updateSummary()
                    Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                taskList.removeAt(position)
                taskAdapter.notifyDataSetChanged()
                saveTasks()
                updateSummary()
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTask() {
        val title = taskInput.text.toString().trim()
        val desc = taskDesc.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (priorityGroup.checkedRadioButtonId) {
            R.id.radioHigh -> "High"
            R.id.radioLow -> "Low"
            else -> "Medium"
        }

        taskList.add(Task(title, desc, priority))
        taskAdapter.notifyDataSetChanged()
        taskInput.text.clear()
        taskDesc.text.clear()
        saveTasks()
        updateSummary()
        Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show()
    }

    private fun updateSummary() {
        val pending = taskList.count { !it.isCompleted }
        summaryText.text = "$pending task(s) pending"
    }

    private fun saveTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        val taskStrings = taskList.map { "${it.title}|${it.description}|${it.priority}|${it.isCompleted}" }
        editor.putString(TASKS_KEY, taskStrings.joinToString("||"))
        editor.apply()
    }

    private fun loadTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val tasksString = prefs.getString(TASKS_KEY, "") ?: ""
        if (tasksString.isNotEmpty()) {
            tasksString.split("||").forEach { taskStr ->
                val parts = taskStr.split("|")
                if (parts.size == 4) {
                    taskList.add(Task(parts[0], parts[1], parts[2], parts[3].toBoolean()))
                }
            }
        }
    }
}