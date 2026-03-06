package com.example.businessproplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityMissedSellingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MissedSellingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMissedSellingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMissedSellingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.missedSellingRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnAddNewMissed.setOnClickListener {
            startActivity(Intent(this, AddMissedItemActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadMissedItems()
    }

    private fun loadMissedItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val missedList = AppDatabase.getDatabase(applicationContext).missedItemDao().getAllMissedItems()
            val displayList = missedList.map { "Item: ${it.itemDescription}\nAsked on: ${it.dateAsked}" }

            withContext(Dispatchers.Main) {
                binding.lvMissedItems.adapter = ArrayAdapter(this@MissedSellingActivity, android.R.layout.simple_list_item_1, displayList)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}