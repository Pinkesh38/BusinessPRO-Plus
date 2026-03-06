package com.example.businessproplus

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityNewOrderBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NewOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewOrderBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val orderDateCalendar = Calendar.getInstance()
    private var photoUri: Uri? = null
    private var videoUri: Uri? = null
    
    private var photoPath: String? = null
    private var videoPath: String? = null
    private var audioPath: String? = null
    
    private var isEditMode = false
    private var existingOrderId = -1
    private var initialQuantity = 0
    private var searchJob: Job? = null

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private val MAX_PRICE = 10000000.0 // 1 Crore limit for UI stability
    private val MAX_QTY = 1000000 // 1 Million limit

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!cameraGranted || !audioGranted) {
            Toast.makeText(this, "Media permissions denied.", Toast.LENGTH_LONG).show()
        }
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
        binding = ActivityNewOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        binding.toolbar.setNavigationOnClickListener { finish() }

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        existingOrderId = intent.getIntExtra("ORDER_ID", -1)
        if (existingOrderId != -1) {
            isEditMode = true
            binding.toolbar.title = "Edit Order #$existingOrderId"
            binding.btnSaveOrder.text = "Update Order"
            loadOrderData(existingOrderId)
        } else {
            binding.btnOrderDate.text = sdfDate.format(orderDateCalendar.time)
        }

        setupListeners(sdfDate)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.newOrderRootContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, systemBars.top, 0, 0)
            binding.stickyBottomPanel.setPadding(0, 0, 0, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    private fun setupListeners(sdfDate: SimpleDateFormat) {
        binding.btnPlusQty.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
            if (current < MAX_QTY) binding.etQuantity.setText((current + 1).toString())
        }
        binding.btnMinusQty.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
            if (current > 1) binding.etQuantity.setText((current - 1).toString())
        }

        binding.etCustomerName.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(500)
                val party = withContext(Dispatchers.IO) {
                    db.partyDao().getPartyByName(text.toString().trim())
                }
                if (party != null) binding.etContactNo.setText(party.contactNo)
            }
        }

        binding.btnOrderDate.setOnClickListener {
            val picker = DatePickerDialog(this, { _, year, month, day ->
                orderDateCalendar.set(year, month, day)
                binding.btnOrderDate.text = sdfDate.format(orderDateCalendar.time)
            }, orderDateCalendar.get(Calendar.YEAR), orderDateCalendar.get(Calendar.MONTH), orderDateCalendar.get(Calendar.DAY_OF_MONTH))
            
            // 🛡️ QA FIX: Prevent "Time Traveler" fraud. Order date cannot be in future (optional) 
            // or too far in past. Let's allow past but prevent future for Order Date.
            picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }

        binding.btnDeliveryDate.setOnClickListener {
            val currentDelCal = Calendar.getInstance()
            val picker = DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    binding.btnDeliveryDate.text = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, minute)
                }, currentDelCal.get(Calendar.HOUR_OF_DAY), currentDelCal.get(Calendar.MINUTE), false).show()
            }, currentDelCal.get(Calendar.YEAR), currentDelCal.get(Calendar.MONTH), currentDelCal.get(Calendar.DAY_OF_MONTH))
            
            // 🛡️ QA FIX: Delivery date must be Today or in Future
            picker.datePicker.minDate = System.currentTimeMillis() - 1000
            picker.show()
        }

        binding.etQuantity.doAfterTextChanged { calculateFinances() }
        binding.etPrice.doAfterTextChanged { calculateFinances() }
        binding.etAdvancePayment.doAfterTextChanged { calculateFinances() }

        binding.btnPhoto.setOnClickListener { handleMedia("photo") }
        binding.btnVideo.setOnClickListener { handleMedia("video") }
        binding.btnAudio.setOnClickListener { handleVoiceRecording() }

        binding.btnSaveOrder.setOnClickListener { validateAndSave() }
    }

    private fun handleVoiceRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }

        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val file = createMediaFile("VOICE_", ".m4a")
        audioPath = file.absolutePath
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioPath)
            try {
                prepare()
                start()
                isRecording = true
                binding.btnAudio.text = "Recording..."
                binding.btnAudio.icon = ContextCompat.getDrawable(this@NewOrderActivity, android.R.drawable.ic_media_pause)
                Toast.makeText(this@NewOrderActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                // Fail silently or log
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                // Fail silently or log
            }
        }
        mediaRecorder = null
        if (isRecording) {
            isRecording = false
            binding.btnAudio.text = "Voice"
            binding.btnAudio.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_btn_speak_now)
            Toast.makeText(this, "Voice Attached", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateFinances() {
        val qty = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val advance = binding.etAdvancePayment.text.toString().toDoubleOrNull() ?: 0.0
        
        // 🛡️ QA FIX: Prevent Billionaire Input overflows
        if (qty > MAX_QTY || price > MAX_PRICE) {
            binding.tvTotal.text = "INVALID"
            binding.tvRemaining.text = "INVALID"
            return
        }

        val total = qty * price
        val remaining = total - advance
        
        binding.tvTotal.text = String.format("₹%.2f", total)
        binding.tvRemaining.text = String.format("₹%.2f", remaining)
    }

    private fun handleMedia(type: String) {
        if (checkPermissions()) {
            val file = createMediaFile(if (type == "photo") "IMG_" else "VID_", if (type == "photo") ".jpg" else ".mp4")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            if (type == "photo") { photoPath = file.absolutePath; photoUri = uri; takePhotoLauncher.launch(uri) }
            else { videoPath = file.absolutePath; videoUri = uri; takeVideoLauncher.launch(uri) }
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun validateAndSave() {
        if (isRecording) stopRecording()

        val customerName = binding.etCustomerName.text.toString().trim()
        val itemDesc = binding.etItemDescription.text.toString().trim()
        val qtyText = binding.etQuantity.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val advanceText = binding.etAdvancePayment.text.toString().trim()

        val qtyOrdered = qtyText.toIntOrNull() ?: 0
        val price = priceText.toDoubleOrNull() ?: 0.0
        val advance = advanceText.toDoubleOrNull() ?: 0.0

        if (customerName.isEmpty() || itemDesc.isEmpty() || qtyOrdered <= 0) {
            Toast.makeText(this, "Mandatory fields: Customer, Item, and Qty > 0", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 🛡️ QA FIX: Final Validation for extreme inputs
        if (qtyOrdered > MAX_QTY || price > MAX_PRICE || advance > (qtyOrdered * price)) {
            Toast.makeText(this, "Invalid financial values or quantity too high.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveOrder.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (db.partyDao().getPartyByName(customerName) == null) {
                    db.partyDao().insertParty(Party(partyType = "Customer", companyName = customerName, contactPerson = customerName, contactNo = binding.etContactNo.text.toString(), address = "", creditLimit = 0.0, creditPeriodDays = 0, notes = "Auto-created"))
                }

                val item = db.itemDao().getItemByName(itemDesc)
                if (item != null) {
                    val stockChange = if (isEditMode) qtyOrdered - initialQuantity else qtyOrdered
                    if (item.currentStock < stockChange) {
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(this@NewOrderActivity, "Stock error: Only ${item.currentStock} left!", Toast.LENGTH_LONG).show()
                            binding.btnSaveOrder.isEnabled = true
                        }
                        return@launch
                    }
                    item.currentStock -= stockChange
                    db.itemDao().updateItem(item)
                }

                val order = Order(
                    id = if (isEditMode) existingOrderId else 0,
                    customerName = customerName,
                    contactNumber = binding.etContactNo.text.toString(),
                    itemDescription = itemDesc,
                    quantity = qtyOrdered,
                    price = price,
                    total = qtyOrdered * price,
                    advancePayment = advance,
                    remainingPayment = (qtyOrdered * price) - advance,
                    orderDate = binding.btnOrderDate.text.toString(),
                    deliveryDate = binding.btnDeliveryDate.text.toString(),
                    photoPath = photoPath,
                    videoPath = videoPath,
                    audioPath = audioPath,
                    remarks = binding.etOrderRemarks.text.toString().trim()
                )
                
                if (isEditMode) db.orderDao().updateOrder(order) else db.orderDao().insertOrder(order)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewOrderActivity, "Order Saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSaveOrder.isEnabled = true
                    Toast.makeText(this@NewOrderActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadOrderData(orderId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val order = db.orderDao().getOrderById(orderId)
            withContext(Dispatchers.Main) {
                order?.let {
                    initialQuantity = it.quantity
                    binding.etCustomerName.setText(it.customerName)
                    binding.etContactNo.setText(it.contactNumber)
                    binding.etItemDescription.setText(it.itemDescription)
                    binding.etQuantity.setText(it.quantity.toString())
                    binding.etPrice.setText(it.price.toString())
                    binding.etAdvancePayment.setText(it.advancePayment.toString())
                    binding.etOrderRemarks.setText(it.remarks)
                    binding.btnOrderDate.text = it.orderDate
                    binding.btnDeliveryDate.text = it.deliveryDate
                    photoPath = it.photoPath
                    videoPath = it.videoPath
                    audioPath = it.audioPath
                }
            }
        }
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun createMediaFile(prefix: String, suffix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("${prefix}${timeStamp}_", suffix, getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}