package com.example.businessproplus

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.businessproplus.databinding.ActivityManageUsersBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private lateinit var adapter: UserAdapter
    private var allUsersList: List<User> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🛡️ SECURITY GUARD: Verify Admin role immediately
        val sharedPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("USER_ROLE", "Staff")
        if (userRole != "Admin") {
            Toast.makeText(this, "Access Denied: Admin privileges required.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadUsers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            this,
            emptyList(),
            onEditClick = { user ->
                showChangeRoleDialog(user)
            },
            onDeleteClick = { user ->
                showDeleteUserDialog(user)
            }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddUser.setOnClickListener {
            showAddUserDialog()
        }

        binding.btnActivityLog.setOnClickListener {
            startActivity(Intent(this, ErrorLogActivity::class.java))
        }

        binding.etSearchUser.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                filterUsers(text.toString())
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etNewUserName)
        val etPin = dialogView.findViewById<TextInputEditText>(R.id.etNewUserPin)
        val spinnerRole = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerNewUserRole)

        val roles = arrayOf("Staff", "Admin")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        spinnerRole.setAdapter(adapterRoles)
        spinnerRole.setText("Staff", false)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add New User")
            .setPositiveButton("Create Account") { dialog, _ ->
                val name = etName.text.toString().trim()
                val pin = etPin.text.toString().trim()
                val role = spinnerRole.text.toString()

                if (name.isEmpty() || pin.length != 4) {
                    Toast.makeText(this, "Enter name and a 4-digit PIN", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveNewUser(name, pin, role)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewUser(name: String, pin: String, role: String) {
        lifecycleScope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                // Check if PIN already exists for security
                val existing = db.userDao().getUserByPin(pin)
                if (existing != null) return@withContext false

                val newUser = User(username = name, pinCode = pin, role = role)
                db.userDao().insertUser(newUser)

                // LOG THE ACTION
                val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val adminName = getSharedPreferences("BusinessProPrefs", MODE_PRIVATE).getString("USER_NAME", "Admin") ?: "Admin"
                db.userActivityDao().insertActivity(UserActivity(userId = 0, userName = adminName, action = "Created User: $name", timestamp = timestamp))
                true
            }

            if (isSuccess) {
                Toast.makeText(this@ManageUsersActivity, "User Created Successfully", Toast.LENGTH_SHORT).show()
                loadUsers()
            } else {
                Toast.makeText(this@ManageUsersActivity, "Error: This PIN is already in use!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            allUsersList = withContext(Dispatchers.IO) { db.userDao().getAllUsers() }
            filterUsers(binding.etSearchUser.text.toString())
        }
    }

    private fun filterUsers(query: String) {
        val filtered = allUsersList.filter { 
            it.username.contains(query, ignoreCase = true) || it.role.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }

    private fun showChangeRoleDialog(user: User) {
        if (user.id == 1) {
            Toast.makeText(this, "Master Admin role cannot be changed!", Toast.LENGTH_SHORT).show()
            return
        }

        val newRole = if (user.role == "Admin") "Staff" else "Admin"
        AlertDialog.Builder(this)
            .setTitle("Change Role")
            .setMessage("Change ${user.username}'s role to $newRole?")
            .setPositiveButton("Update") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.userDao().updateUserRole(user.id, newRole)
                    }
                    loadUsers()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteUserDialog(user: User) {
        if (user.id == 1) {
            Toast.makeText(this, "Master Admin cannot be deleted!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Permanently remove ${user.username}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.userDao().deleteUser(user)
                    }
                    loadUsers()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
