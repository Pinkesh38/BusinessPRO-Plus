package com.example.businessproplus

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityNewOrderBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class NewOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewOrderBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val orderDateCalendar = Calendar.getInstance()
    private val promiseDateCalendar = Calendar.getInstance()
    
    private var isEditMode = false
    private var existingOrderId = -1
    private var searchJob: Job? = null

    // 🛡️ QA LIMITS: Prevent arithmetic overflows
    private val MAX_PRICE = 10000000.0 
    private val MAX_QTY = 1000000 

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
            binding.toolbar.title = "Edit Bill #$existingOrderId"
            binding.btnSaveOrder.text = "Update Bill"
            loadOrderData(existingOrderId)
        } else {
            binding.btnOrderDate.text = "Bill Date: ${sdfDate.format(orderDateCalendar.time)}"
        }

        setupListeners(sdfDate)
        setupItemAutofill()
    }

    private fun setupItemAutofill() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val items = db.itemDao().getAllItems()
                val itemNames = items.map { it.itemName }
                withContext(Dispatchers.Main) {
                    if (!isFinishing) {
                        val adapter = ArrayAdapter(this@NewOrderActivity, android.R.layout.simple_dropdown_item_1line, itemNames)
                        binding.etItemDescription.setAdapter(adapter)
                        
                        binding.etItemDescription.setOnItemClickListener { _, _, position, _ ->
                            val selectedName = adapter.getItem(position)
                            val selectedItem = items.find { it.itemName == selectedName }
                            selectedItem?.let {
                                binding.etPrice.setText(it.salesPrice.toString())
                                Toast.makeText(applicationContext, "In Stock: ${it.currentStock}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) { }
        }
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
                if (party != null && !isFinishing) {
                    binding.etContactNo.setText(party.contactNo)
                }
            }
        }

        binding.btnOrderDate.setOnClickListener {
            val picker = DatePickerDialog(this, { _, year, month, day ->
                orderDateCalendar.set(year, month, day)
                binding.btnOrderDate.text = "Bill Date: ${sdfDate.format(orderDateCalendar.time)}"
            }, orderDateCalendar.get(Calendar.YEAR), orderDateCalendar.get(Calendar.MONTH), orderDateCalendar.get(Calendar.DAY_OF_MONTH))
            picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }

        binding.btnDeliveryDate.setOnClickListener {
            val picker = DatePickerDialog(this, { _, year, month, day ->
                promiseDateCalendar.set(year, month, day)
                binding.btnDeliveryDate.text = "Promise Date: ${sdfDate.format(promiseDateCalendar.time)}"
            }, promiseDateCalendar.get(Calendar.YEAR), promiseDateCalendar.get(Calendar.MONTH), promiseDateCalendar.get(Calendar.DAY_OF_MONTH))
            picker.datePicker.minDate = System.currentTimeMillis() - 1000
            picker.show()
        }

        binding.etQuantity.doAfterTextChanged { calculateFinances() }
        binding.etPrice.doAfterTextChanged { calculateFinances() }
        binding.etAdvancePayment.doAfterTextChanged { calculateFinances() }

        binding.btnSaveOrder.setOnClickListener {
            if (binding.btnSaveOrder.isEnabled) {
                binding.btnSaveOrder.isEnabled = false
                validateAndSave()
            }
        }
    }

    private fun calculateFinances() {
        val qty = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val paid = binding.etAdvancePayment.text.toString().toDoubleOrNull() ?: 0.0
        
        if (qty > MAX_QTY || price > MAX_PRICE) {
            binding.tvTotal.text = "INVALID"
            binding.tvRemaining.text = "INVALID"
            return
        }

        val total = qty * price
        val remaining = total - paid
        
        binding.tvTotal.text = String.format("₹%.2f", total)
        binding.tvRemaining.text = String.format("₹%.2f", remaining)
    }

    private fun validateAndSave() {
        val customerName = binding.etCustomerName.text.toString().trim()
        val contactNo = binding.etContactNo.text.toString().trim()
        val itemDesc = binding.etItemDescription.text.toString().trim()
        val qtyOrdered = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val paidAmount = binding.etAdvancePayment.text.toString().toDoubleOrNull() ?: 0.0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if (customerName.isEmpty() || itemDesc.isEmpty() || qtyOrdered <= 0) {
            Toast.makeText(this, "Mandatory fields: Customer, Item, and Qty > 0", Toast.LENGTH_SHORT).show()
            binding.btnSaveOrder.isEnabled = true
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val formattedOrderDate = sdf.format(orderDateCalendar.time)
                
                // 1. Auto-create or Update Party
                val existingParty = db.partyDao().getPartyByName(customerName)
                if (existingParty == null) {
                    db.partyDao().insertParty(Party(
                        partyType = "Customer", 
                        companyName = customerName, 
                        contactPerson = customerName, 
                        contactNo = contactNo, 
                        address = "", 
                        creditLimit = 0.0, 
                        creditPeriodDays = 0, 
                        notes = "Auto-created during billing",
                        lastOrderDate = formattedOrderDate
                    ))
                } else {
                    existingParty.lastOrderDate = formattedOrderDate
                    db.partyDao().updateParty(existingParty)
                }

                val total = qtyOrdered * price
                val remaining = total - paidAmount
                val promiseDate = if (binding.btnDeliveryDate.text.contains("Promise Date:")) {
                    sdf.format(promiseDateCalendar.time)
                } else ""

                val order = Order(
                    id = if (isEditMode) existingOrderId else 0,
                    customerName = customerName,
                    contactNumber = contactNo,
                    itemDescription = itemDesc,
                    quantity = qtyOrdered,
                    price = price,
                    total = total,
                    advancePayment = paidAmount,
                    remainingPayment = remaining,
                    orderDate = formattedOrderDate,
                    deliveryDate = promiseDate,
                    paymentPromiseDate = promiseDate,
                    status = if (remaining <= 0) "Completed" else "Pending",
                    isPaid = remaining <= 0,
                    dueAmount = remaining,
                    remarks = binding.etOrderRemarks.text.toString().trim()
                )
                
                // 2. Transactional Update (Deduct Stock and Save Bill)
                db.orderDao().createOrderWithStockUpdate(order, db.itemDao())

                withContext(Dispatchers.Main) {
                    if (!isFinishing) {
                        Toast.makeText(applicationContext, "Sale Completed successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSaveOrder.isEnabled = true
                    Toast.makeText(applicationContext, "Save Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadOrderData(orderId: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            val order = db.orderDao().getOrderById(orderId)
            withContext(Dispatchers.Main) {
                order?.let {
                    binding.etCustomerName.setText(it.customerName)
                    binding.etContactNo.setText(it.contactNumber)
                    binding.etItemDescription.setText(it.itemDescription)
                    binding.etQuantity.setText(it.quantity.toString())
                    binding.etPrice.setText(it.price.toString())
                    binding.etAdvancePayment.setText(it.advancePayment.toString())
                    binding.etOrderRemarks.setText(it.remarks)
                    
                    try {
                        orderDateCalendar.time = sdf.parse(it.orderDate)!!
                        binding.btnOrderDate.text = "Bill Date: ${it.orderDate}"
                        
                        if (it.paymentPromiseDate.isNotEmpty()) {
                            promiseDateCalendar.time = sdf.parse(it.paymentPromiseDate)!!
                            binding.btnDeliveryDate.text = "Promise Date: ${it.paymentPromiseDate}"
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }
}