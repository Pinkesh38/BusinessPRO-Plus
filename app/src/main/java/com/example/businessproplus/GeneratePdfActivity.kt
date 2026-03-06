package com.example.businessproplus

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeneratePdfActivity : AppCompatActivity() {

    private var pendingOrdersForPdf: List<Order> = emptyList()
    private var pendingReportTitle = ""

    // The Launcher that asks where to save the file on your phone
    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            drawAndSavePdf(uri, pendingReportTitle, pendingOrdersForPdf)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_pdf)
        // 1. Turn on the built-in Android Back Arrow!
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val spinnerPdfType = findViewById<Spinner>(R.id.spinnerPdfType)
        val etPdfSearchQuery = findViewById<EditText>(R.id.etPdfSearchQuery)
        val btnCreatePdf = findViewById<Button>(R.id.btnCreatePdf)

        val reportTypes = arrayOf("Customer Report", "Monthly Orders (MM/YYYY)", "Items with Tech Data")
        spinnerPdfType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reportTypes)

        btnCreatePdf.setOnClickListener {
            val query = etPdfSearchQuery.text.toString().trim()
            val type = spinnerPdfType.selectedItem.toString()

            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a search value!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Fetch the data in the background based on what they selected
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)

                when (type) {
                    "Customer Report" -> {
                        pendingOrdersForPdf = db.orderDao().getOrdersForCustomer(query)
                        pendingReportTitle = "Customer Report: $query"
                    }
                    "Monthly Orders (MM/YYYY)" -> {
                        pendingOrdersForPdf = db.orderDao().getOrdersByMonth(query)
                        pendingReportTitle = "Monthly Report: $query"
                    }
                    "Items with Tech Data" -> {
                        pendingOrdersForPdf = db.orderDao().getOrdersByItem(query)
                        pendingReportTitle = "Technical Report for Item: $query"
                    }
                }

                // 2. Switch back to main screen to trigger the Save File popup
                withContext(Dispatchers.Main) {
                    if (pendingOrdersForPdf.isEmpty()) {
                        Toast.makeText(this@GeneratePdfActivity, "No data found for this search!", Toast.LENGTH_SHORT).show()
                    } else {
                        // This opens the Android file picker!
                        createPdfLauncher.launch("BusinessPRO_${query.replace("/", "-")}_Report.pdf")
                    }
                }
            }
        }
    }

    // 2. This function listens for the top Back Arrow being clicked
    override fun onSupportNavigateUp(): Boolean {
        finish() // This safely closes the current screen and drops you back to the Dashboard!
        return true
    }

    // 3. The actual PDF Drawing Engine
    private fun drawAndSavePdf(uri: Uri, reportTitle: String, orders: List<Order>) {
        val pdfDocument = PdfDocument()

        // Set up our "Pens"
        val normalPaint = Paint().apply { textSize = 12f; color = Color.BLACK }
        val boldPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.parseColor("#D32F2F") }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // Standard A4 Size
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        // Draw Title
        canvas.drawText(reportTitle, 20f, 40f, titlePaint)
        var yPosition = 80f // Start drawing orders further down the page

        // Loop through every order and draw it line by line!
        for (order in orders) {

            // If we hit the bottom of the A4 page, finish it and get a new blank page!
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 40f // Reset back to the top of the new page
            }

            // Simple text cutter for customer name
            val shortName = order.customerName.take(30)

            // Draw Order Details
            canvas.drawText("Order #${order.id} | Date: ${order.orderDate} | Customer: $shortName", 20f, yPosition, boldPaint)
            yPosition += 20f
            canvas.drawText("Item: ${order.itemDescription} | Qty: ${order.quantity} | Total: ₹${order.total}", 20f, yPosition, normalPaint)
            yPosition += 20f

            // Draw Technical Data (Great for the manufacturing team!)
            val techString = "Tech Specs: ${order.heaterType} | ${order.voltage}V | ${order.ampere}A | ${order.ohms}Ω | Size: ${order.blockSize}"
            canvas.drawText(techString, 20f, yPosition, normalPaint)

            // Add space before the next order
            yPosition += 40f
        }

        // Finish the last page
        pdfDocument.finishPage(page)

        // Save the file to the chosen location!
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(this, "PDF Generated and Saved Successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving PDF!", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close() // Close the tool to save phone memory
        }
    }
}
