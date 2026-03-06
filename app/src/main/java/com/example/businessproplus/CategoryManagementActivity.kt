package com.example.businessproplus

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityCategoryManagementBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManagementBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private var categoryList: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.categoryRootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnAddCategory.setOnClickListener {
            val catName = binding.etNewCategory.text.toString().trim()
            if (catName.isEmpty()) {
                Toast.makeText(this, "Enter a category name!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val exists = withContext(Dispatchers.IO) {
                    db.categoryDao().getAllCategories().any { it.categoryName.equals(catName, ignoreCase = true) }
                }

                if (exists) {
                    Toast.makeText(this@CategoryManagementActivity, "Category already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    withContext(Dispatchers.IO) {
                        db.categoryDao().insertCategory(Category(categoryName = catName))
                    }
                    binding.etNewCategory.text?.clear()
                    Toast.makeText(this@CategoryManagementActivity, "Category Added!", Toast.LENGTH_SHORT).show()
                    loadCategories()
                }
            }
        }

        binding.lvCategories.setOnItemLongClickListener { _, _, position, _ ->
            if (position < categoryList.size) {
                val catToDelete = categoryList[position]
                AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Delete '${catToDelete.categoryName}'?")
                    .setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                db.categoryDao().deleteCategory(catToDelete)
                            }
                            loadCategories()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                db.categoryDao().getAllCategories()
            }
            categoryList = list
            val displayList = list.map { it.categoryName }
            binding.lvCategories.adapter = ArrayAdapter(this@CategoryManagementActivity, android.R.layout.simple_list_item_1, displayList)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
