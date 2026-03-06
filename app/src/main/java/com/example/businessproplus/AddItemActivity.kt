package com.example.businessproplus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityAddItemBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val viewModel: AddItemViewModel by viewModels()
    private var currentItemId: Int = 0
    
    private var photoPath: String? = null
    private var videoPath: String? = null
    private var photoUri: Uri? = null
    private var videoUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) Toast.makeText(this, "Camera permission required for media.", Toast.LENGTH_SHORT).show()
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) Toast.makeText(this, "Photo Attached", Toast.LENGTH_SHORT).show()
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) Toast.makeText(this, "Video Attached", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.addItemRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupDropdowns()
        setupObservers()

        // EDIT MODE: Load existing item if ID is provided
        currentItemId = intent.getIntExtra("ITEM_ID", 0)
        if (currentItemId != 0) {
            binding.toolbar.title = "Modify Item Details"
            binding.btnSaveItem.text = "Update Item"
            binding.btnSaveAndNew.visibility = android.view.View.GONE
            viewModel.loadItem(currentItemId)
        }

        binding.btnSaveItem.setOnClickListener { validateAndSave(true) }
        binding.btnSaveAndNew.setOnClickListener { validateAndSave(false) }
        
        // 🛡️ FIX: Media button listeners
        binding.btnItemPhoto.setOnClickListener { handleMedia("photo") }
        binding.btnItemVideo.setOnClickListener { handleMedia("video") }
        
        viewModel.loadCategories()
    }

    private fun handleMedia(type: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }

        val file = createMediaFile(if (type == "photo") "IMG_" else "VID_", if (type == "photo") ".jpg" else ".mp4")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        
        if (type == "photo") {
            photoPath = file.absolutePath
            photoUri = uri
            takePhotoLauncher.launch(uri)
        } else {
            videoPath = file.absolutePath
            videoUri = uri
            takeVideoLauncher.launch(uri)
        }
    }

    private fun createMediaFile(prefix: String, suffix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("${prefix}${timeStamp}_", suffix, getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    private fun setupDropdowns() {
        val units = resources.getStringArray(R.array.unit_options)
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        binding.spinnerUnit.setAdapter(unitAdapter)
        
        // Default unit if empty
        binding.spinnerUnit.setText("Pieces", false)
    }

    private fun setupObservers() {
        viewModel.categories.observe(this) { categories ->
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            binding.spinnerCategory.setAdapter(categoryAdapter)
        }

        viewModel.existingItem.observe(this) { item ->
            item?.let {
                binding.etItemName.setText(it.itemName)
                binding.spinnerCategory.setText(it.category, false)
                binding.spinnerUnit.setText(it.unit, false)
                binding.etSalesPrice.setText(it.salesPrice.toString())
                binding.etDiscount.setText(it.discountPercent.toString())
                binding.etQuantity.setText(it.currentStock.toString())
                binding.etMinStock.setText(it.minStockLevel.toString())
                binding.etItemDesc.setText(it.itemDescription)
                photoPath = it.photoPath
                videoPath = it.videoPath
            }
        }

        viewModel.itemSavedEvent.observe(this) { saved ->
            if (saved) {
                Toast.makeText(this, "Item Saved Successfully!", Toast.LENGTH_SHORT).show()
                hideKeyboard()
                viewModel.resetSavedEvent()
                
                binding.btnSaveItem.isEnabled = true
                binding.btnSaveAndNew.isEnabled = true
                
                if (currentItemId != 0) {
                    finish() // Close if we were editing
                }
            }
        }
    }

    private fun validateAndSave(closeAfterSaving: Boolean) {
        val name = binding.etItemName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.tilItemName.error = "Item Name Required"
            binding.etItemName.requestFocus()
            return
        }
        
        if (name.length > 100) {
            binding.tilItemName.error = "Name too long"
            return
        }

        val priceText = binding.etSalesPrice.text.toString()
        val price = priceText.toDoubleOrNull() ?: 0.0
        
        if (priceText.isNotEmpty() && price < 0) {
            Toast.makeText(this, "Price cannot be negative", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveItem.isEnabled = false
        binding.btnSaveAndNew.isEnabled = false

        val category = binding.spinnerCategory.text.toString().ifEmpty { "General" }
        val unit = binding.spinnerUnit.text.toString().ifEmpty { "Pcs" }
        val discount = binding.etDiscount.text.toString().toDoubleOrNull() ?: 0.0
        val stock = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val minStock = binding.etMinStock.text.toString().toIntOrNull() ?: 5
        val description = binding.etItemDesc.text.toString().trim()

        // 🛡️ FIX: Passing photoPath and videoPath to ViewModel
        viewModel.saveItem(currentItemId, name, unit, price, discount, category, description, stock, minStock, photoPath, videoPath)

        if (closeAfterSaving && currentItemId == 0) {
            lifecycleScope.launch {
                delay(200)
                finish()
            }
        } else if (currentItemId == 0) {
            clearFields()
        }
    }

    private fun clearFields() {
        binding.apply {
            etItemName.text?.clear()
            etSalesPrice.text?.clear()
            etDiscount.text?.clear()
            etQuantity.text?.clear()
            etMinStock.text?.clear()
            etItemDesc.text?.clear()
            etItemName.requestFocus()
            tilItemName.error = null
            photoPath = null
            videoPath = null
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
