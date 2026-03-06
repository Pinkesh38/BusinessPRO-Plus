package com.example.businessproplus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.util.*

class PdfExporter(private val context: Context) {

    fun exportOrdersToPdf(uri: Uri, reportTitle: String, orders: List<Order>): Boolean {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.parseColor("#1E3A8A") }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        val normalPaint = Paint().apply { textSize = 10f; color = Color.BLACK }
        val labelPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        canvas.drawText(reportTitle, 40f, 50f, titlePaint)
        canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, 70f, normalPaint)
        
        var yPosition = 100f
        val margin = 40f
        val pageWidth = 595f

        for (order in orders) {
            // Check if we need a new page (rough estimate of order block height)
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            // --- Order Header ---
            canvas.drawText("ORDER #${order.id} - ${order.status.uppercase()}", margin, yPosition, headerPaint)
            yPosition += 20f

            // Row 1: Customer Info
            canvas.drawText("Customer:", margin, yPosition, labelPaint)
            canvas.drawText(order.customerName, margin + 60f, yPosition, normalPaint)
            canvas.drawText("Contact:", margin + 250f, yPosition, labelPaint)
            canvas.drawText(order.contactNumber, margin + 300f, yPosition, normalPaint)
            yPosition += 15f

            // Row 2: Item Info
            canvas.drawText("Item:", margin, yPosition, labelPaint)
            canvas.drawText(order.itemDescription, margin + 60f, yPosition, normalPaint)
            canvas.drawText("Qty:", margin + 250f, yPosition, labelPaint)
            canvas.drawText(order.quantity.toString(), margin + 300f, yPosition, normalPaint)
            yPosition += 15f

            // Row 3: Pricing
            canvas.drawText("Price:", margin, yPosition, labelPaint)
            canvas.drawText("₹${order.price}", margin + 60f, yPosition, normalPaint)
            canvas.drawText("Total:", margin + 150f, yPosition, labelPaint)
            canvas.drawText("₹${order.total}", margin + 200f, yPosition, normalPaint)
            canvas.drawText("Advance:", margin + 300f, yPosition, labelPaint)
            canvas.drawText("₹${order.advancePayment}", margin + 360f, yPosition, normalPaint)
            canvas.drawText("Due:", margin + 450f, yPosition, labelPaint)
            canvas.drawText("₹${order.remainingPayment}", margin + 480f, yPosition, normalPaint)
            yPosition += 20f

            // Row 4: Dates
            canvas.drawText("Order Date:", margin, yPosition, labelPaint)
            canvas.drawText(order.orderDate, margin + 70f, yPosition, normalPaint)
            canvas.drawText("Delivery By:", margin + 200f, yPosition, labelPaint)
            canvas.drawText(order.deliveryDate, margin + 270f, yPosition, normalPaint)
            yPosition += 15f

            // Row 5: Process Dates
            canvas.drawText("Started On:", margin, yPosition, labelPaint)
            canvas.drawText(order.processedOn.ifEmpty { "-" }, margin + 70f, yPosition, normalPaint)
            canvas.drawText("Completed On:", margin + 200f, yPosition, labelPaint)
            canvas.drawText(order.completedOn.ifEmpty { "-" }, margin + 280f, yPosition, normalPaint)
            canvas.drawText("Delivered On:", margin + 400f, yPosition, labelPaint)
            canvas.drawText(order.deliveredOn.ifEmpty { "-" }, margin + 480f, yPosition, normalPaint)
            yPosition += 25f

            // --- Manufacturing Specs ---
            canvas.drawText("MANUFACTURING SPECIFICATIONS", margin, yPosition, labelPaint)
            yPosition += 15f

            val isPending = order.status.equals("Pending", ignoreCase = true)
            
            // Spec Row 1
            canvas.drawText("Category:", margin, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.heaterType, margin + 60f, yPosition, normalPaint)
            canvas.drawText("Block Size:", margin + 150f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.blockSize, margin + 210f, yPosition, normalPaint)
            canvas.drawText("Holes/Cut:", margin + 300f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.holesOrCut, margin + 360f, yPosition, normalPaint)
            yPosition += 15f

            // Spec Row 2
            canvas.drawText("Conn Type:", margin, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.connectionType, margin + 60f, yPosition, normalPaint)
            canvas.drawText("Volts:", margin + 150f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.voltage, margin + 210f, yPosition, normalPaint)
            canvas.drawText("Amps:", margin + 300f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.ampere, margin + 360f, yPosition, normalPaint)
            canvas.drawText("Ohms:", margin + 450f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.ohms, margin + 490f, yPosition, normalPaint)
            yPosition += 15f

            // Spec Row 3
            canvas.drawText("Stripe/Wire:", margin, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.stripeOrWire, margin + 70f, yPosition, normalPaint)
            canvas.drawText("Turns:", margin + 200f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.turns, margin + 240f, yPosition, normalPaint)
            canvas.drawText("Final Amps:", margin + 300f, yPosition, normalPaint)
            canvas.drawText(if (isPending) "" else order.finalAmpere, margin + 370f, yPosition, normalPaint)
            yPosition += 20f

            // Remarks
            canvas.drawText("Technical Remarks:", margin, yPosition, labelPaint)
            val remarks = order.remarks.ifEmpty { "None" }
            canvas.drawText(remarks, margin + 110f, yPosition, normalPaint)
            yPosition += 20f

            // Separator Line
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 30f
        }

        pdfDocument.finishPage(page)

        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}