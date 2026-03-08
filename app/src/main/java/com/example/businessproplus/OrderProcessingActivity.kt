package com.example.businessproplus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityOrderProcessingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrderProcessingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderProcessingBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private var currentOrder: Order? = null
    private val processDateCalendar = Calendar.getInstance()
    private var isProcessingStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOrderProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.orderProcessingRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val orderId = intent.getIntExtra("ORDER_ID", -1)
        if (orderId == -1) {
            Toast.makeText(this, "Error loading order!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun calculateOhms() {
            val v = binding.etVoltage.text.toString().toDoubleOrNull()
            val i = binding.etAmpere.text.toString().toDoubleOrNull()
            
            if (v != null && i != null && i > 0.0) {
                val r = v / i
                binding.etOhms.setText(String.format(Locale.US, "%.2f", r))
            } else if (i == 0.0) {
                binding.etOhms.setText("0.00")
            }
        }

        binding.etVoltage.doAfterTextChanged { calculateOhms() }
        binding.etAmpere.doAfterTextChanged { calculateOhms() }

        lifecycleScope.launch {
            val (order, categories) = withContext(Dispatchers.IO) {
                val o = db.orderDao().getOrderById(orderId)
                val cats = db.categoryDao().getAllCategories().map { it.categoryName }
                o to cats
            }

            val adapter = ArrayAdapter(this@OrderProcessingActivity, android.R.layout.simple_dropdown_item_1line, categories)
            binding.spinnerSpecCategory.setAdapter(adapter)

            currentOrder = order
            order?.let { o ->
                binding.tvProcessTitle.text = "Processing Order #${o.id}"
                binding.tvOrderItem.text = "Product: ${o.itemDescription} (${o.quantity} Pcs)"
                binding.tvOrderRemarks.text = "Remarks: ${o.remarks.ifEmpty { "No notes" }}"

                setupMediaVisibility(o)

                binding.spinnerSpecCategory.setText(o.heaterType, false)
                binding.etBlockSize.setText(o.blockSize)
                binding.etHolesOrCut.setText(o.holesOrCut)
                binding.etConnectionType.setText(o.connectionType)
                binding.etVoltage.setText(o.voltage)
                binding.etAmpere.setText(o.ampere)
                binding.etOhms.setText(o.ohms)
                binding.etStripeOrWire.setText(o.stripeOrWire)
                binding.etTurns.setText(o.turns)
                binding.etFinalAmpere.setText(o.finalAmpere)

                if (o.processedOn.isNotEmpty()) {
                    try {
                        processDateCalendar.time = sdfDateTime.parse(o.processedOn)!!
                        isProcessingStarted = true
                        binding.btnProcessStart.text = "Started On: ${o.processedOn}"
                    } catch (e: Exception) {}
                }
                updateButtonVisibility(o.status)
            }
        }

        binding.btnViewPhoto.setOnClickListener { openMedia(currentOrder?.photoPath, "image/*") }
        binding.btnPlayVideo.setOnClickListener { openMedia(currentOrder?.videoPath, "video/*") }
        binding.btnPlayAudio.setOnClickListener { openMedia(currentOrder?.audioPath, "audio/*") }

        binding.btnProcessStart.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    processDateCalendar.set(year, month, dayOfMonth, hourOfDay, minute)
                    isProcessingStarted = true
                    val formattedDate = sdfDateTime.format(processDateCalendar.time)
                    binding.btnProcessStart.text = "Started On: $formattedDate"
                    
                    val sharedPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
                    val currentUser = sharedPrefs.getString("USER_NAME", "Unknown")

                    currentOrder?.apply {
                        status = "Working"
                        processedOn = formattedDate
                        processedBy = currentUser ?: "Unknown"
                    }
                    saveOrderToDb()
                    updateButtonVisibility("Working")
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnMarkDelayed.setOnClickListener {
            currentOrder?.status = "Delayed"
            saveOrderToDb()
            Toast.makeText(this, "Order marked as Delayed", Toast.LENGTH_SHORT).show()
            updateButtonVisibility("Delayed")
        }

        binding.btnSaveProgress.setOnClickListener {
            it.isEnabled = false
            currentOrder?.apply {
                heaterType = binding.spinnerSpecCategory.text.toString()
                blockSize = binding.etBlockSize.text.toString()
                holesOrCut = binding.etHolesOrCut.text.toString()
                connectionType = binding.etConnectionType.text.toString()
                voltage = binding.etVoltage.text.toString()
                ampere = binding.etAmpere.text.toString()
                ohms = binding.etOhms.text.toString()
                stripeOrWire = binding.etStripeOrWire.text.toString()
                turns = binding.etTurns.text.toString()
                finalAmpere = binding.etFinalAmpere.text.toString()
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    currentOrder?.let { db.orderDao().updateOrder(it) }
                }
                Toast.makeText(this@OrderProcessingActivity, "Progress Saved!", Toast.LENGTH_SHORT).show()
                it.isEnabled = true
            }
        }

        binding.btnCompleteOrder.setOnClickListener {
            if (!isProcessingStarted) {
                Toast.makeText(this, "Please set a Start Date first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    val completeCal = Calendar.getInstance()
                    completeCal.set(year, month, dayOfMonth, hourOfDay, minute)
                    val formattedDate = sdfDateTime.format(completeCal.time)

                    currentOrder?.apply {
                        status = "Completed"
                        completedOn = formattedDate
                    }

                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            currentOrder?.let { db.orderDao().updateOrder(it) }
                        }
                        Toast.makeText(this@OrderProcessingActivity, "Order Completed!", Toast.LENGTH_SHORT).show()
                        updateButtonVisibility("Completed")
                        sendWhatsAppNotification("Completed")
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnMarkDelivered.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    val deliverCal = Calendar.getInstance()
                    deliverCal.set(year, month, dayOfMonth, hourOfDay, minute)
                    val formattedDate = sdfDateTime.format(deliverCal.time)

                    currentOrder?.apply {
                        status = "Delivered"
                        deliveredOn = formattedDate
                    }

                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            currentOrder?.let { db.orderDao().updateOrder(it) }
                        }
                        Toast.makeText(this@OrderProcessingActivity, "Order Delivered!", Toast.LENGTH_SHORT).show()
                        updateButtonVisibility("Delivered")
                        sendWhatsAppNotification("Delivered")
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnRevokeOrder.setOnClickListener {
            currentOrder?.apply {
                status = "Working"
                completedOn = ""
                deliveredOn = ""
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    currentOrder?.let { db.orderDao().updateOrder(it) }
                }
                Toast.makeText(this@OrderProcessingActivity, "Order Revoked to Working", Toast.LENGTH_SHORT).show()
                updateButtonVisibility("Working")
            }
        }

        binding.btnCancelOrder.setOnClickListener {
            currentOrder?.let { order ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val item = db.itemDao().getItemByName(order.itemDescription)
                        if (item != null) {
                            item.currentStock += order.quantity
                            db.itemDao().updateItem(item)
                        }
                        order.status = "Cancelled"
                        db.orderDao().updateOrder(order)
                    }
                    Toast.makeText(this@OrderProcessingActivity, "Order Cancelled & Stock Restored", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun setupMediaVisibility(order: Order) {
        val hasPhoto = !order.photoPath.isNullOrEmpty()
        val hasVideo = !order.videoPath.isNullOrEmpty()
        val hasAudio = !order.audioPath.isNullOrEmpty()

        if (hasPhoto || hasVideo || hasAudio) {
            binding.tvMediaLabel.visibility = View.VISIBLE
            binding.layoutMediaButtons.visibility = View.VISIBLE
            binding.btnViewPhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
            binding.btnPlayVideo.visibility = if (hasVideo) View.VISIBLE else View.GONE
            binding.btnPlayAudio.visibility = if (hasAudio) View.VISIBLE else View.GONE
        }
    }

    private fun openMedia(path: String?, mimeType: String) {
        if (path.isNullOrEmpty()) return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtonVisibility(status: String) {
        when (status) {
            "Completed" -> {
                binding.btnCompleteOrder.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.VISIBLE
                binding.btnRevokeOrder.visibility = View.VISIBLE
            }
            "Delivered" -> {
                binding.btnCompleteOrder.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.GONE
                binding.btnRevokeOrder.visibility = View.VISIBLE
            }
            else -> {
                binding.btnCompleteOrder.visibility = View.VISIBLE
                binding.btnMarkDelivered.visibility = View.GONE
                binding.btnRevokeOrder.visibility = View.GONE
            }
        }
    }

    private fun saveOrderToDb() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentOrder?.let { db.orderDao().updateOrder(it) }
        }
    }

    private fun sendWhatsAppNotification(status: String) {
        currentOrder?.let { order ->
            try {
                val phone = order.contactNumber.replace(" ", "")
                val message = if (status == "Delivered") {
                    "Hello ${order.customerName}, your order for ${order.itemDescription} has been delivered! Thank you for choosing BusinessPRO+."
                } else {
                    "Hello ${order.customerName}, your order for ${order.itemDescription} is now completed and ready! Total Remaining: ₹${order.remainingPayment}"
                }
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp is not installed!", Toast.LENGTH_LONG).show()
            }
        }
    }
}