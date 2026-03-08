package com.example.businessproplus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.util.*

class PdfExporter(context: Context) {

    private val appContext = context.applicationContext

    fun exportOrdersToPdf(uri: Uri, reportTitle: String, orders: List<Order>): Boolean {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.parseColor("#1E3A8A") }
        val headerPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val normalPaint = Paint().apply { textSize = 18f; color = Color.BLACK }
        val labelPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1.5f }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        canvas.drawText(reportTitle, 40f, 50f, titlePaint)
        canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, 80f, normalPaint)
        
        var yPosition = 120f
        val margin = 40f
        val pageWidth = 595f

        for (order in orders) {
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            canvas.drawText("BILL #${order.id} - ${order.status.uppercase()}", margin, yPosition, headerPaint)
            yPosition += 30f

            canvas.drawText("Customer:", margin, yPosition, labelPaint)
            canvas.drawText(order.customerName, margin + 110f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Item:", margin, yPosition, labelPaint)
            canvas.drawText("${order.itemDescription} (${order.quantity} Pcs)", margin + 110f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Total:", margin, yPosition, labelPaint)
            canvas.drawText("₹${order.total}", margin + 110f, yPosition, normalPaint)
            canvas.drawText("Paid:", margin + 300f, yPosition, labelPaint)
            canvas.drawText("₹${order.advancePayment}", margin + 380f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Balance:", margin, yPosition, labelPaint)
            canvas.drawText("₹${order.remainingPayment}", margin + 110f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Date:", margin, yPosition, labelPaint)
            canvas.drawText(order.orderDate, margin + 110f, yPosition, normalPaint)
            if (order.paymentPromiseDate.isNotEmpty()) {
                canvas.drawText("Promise:", margin + 300f, yPosition, labelPaint)
                canvas.drawText(order.paymentPromiseDate, margin + 400f, yPosition, normalPaint)
            }
            yPosition += 30f

            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 40f
        }

        pdfDocument.finishPage(page)

        return try {
            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            pdfDocument.close()
        }
    }

    fun exportStockToPdf(uri: Uri, reportTitle: String, items: List<Item>): Boolean {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.parseColor("#1E3A8A") }
        val headerPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val normalPaint = Paint().apply { textSize = 18f; color = Color.BLACK }
        val labelPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1.5f }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        canvas.drawText(reportTitle, 40f, 50f, titlePaint)
        canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, 80f, normalPaint)
        
        var yPosition = 120f
        val margin = 40f
        val pageWidth = 595f

        for (item in items) {
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            canvas.drawText(item.itemName.uppercase(), margin, yPosition, headerPaint)
            yPosition += 30f

            canvas.drawText("Category:", margin, yPosition, labelPaint)
            canvas.drawText(item.category, margin + 110f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Stock:", margin, yPosition, labelPaint)
            canvas.drawText("${item.currentStock} ${item.unit}", margin + 110f, yPosition, normalPaint)
            canvas.drawText("Min Level:", margin + 300f, yPosition, labelPaint)
            canvas.drawText(item.minStockLevel.toString(), margin + 420f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Sales Price:", margin, yPosition, labelPaint)
            canvas.drawText("₹${item.salesPrice}", margin + 130f, yPosition, normalPaint)
            canvas.drawText("Purchase:", margin + 300f, yPosition, labelPaint)
            canvas.drawText("₹${item.purchasePrice}", margin + 420f, yPosition, normalPaint)
            yPosition += 25f

            canvas.drawText("Description:", margin, yPosition, labelPaint)
            val desc = if (item.itemDescription.length > 35) item.itemDescription.substring(0, 32) + "..." else item.itemDescription
            canvas.drawText(desc, margin + 130f, yPosition, normalPaint)
            yPosition += 30f

            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 40f
        }

        pdfDocument.finishPage(page)

        return try {
            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            pdfDocument.close()
        }
    }

    fun exportCustomersWithHistoryToPdf(uri: Uri, reportTitle: String, parties: List<Party>, db: AppDatabase): Boolean {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.parseColor("#1E3A8A") }
        val headerPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val normalPaint = Paint().apply { textSize = 18f; color = Color.BLACK }
        val labelPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1.5f }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        canvas.drawText(reportTitle, 40f, 50f, titlePaint)
        canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, 80f, normalPaint)
        
        var yPosition = 120f
        val margin = 40f
        val pageWidth = 595f

        // This function needs to be called from a background thread
        // as it will perform DB queries for each party's order history.
        for (party in parties) {
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            canvas.drawText(party.companyName.uppercase(), margin, yPosition, headerPaint)
            yPosition += 30f

            canvas.drawText("Contact:", margin, yPosition, labelPaint)
            canvas.drawText(party.contactNo, margin + 110f, yPosition, normalPaint)
            yPosition += 25f

            // Fetch order history for this specific customer synchronously (caller should be in coroutine)
            val history = run {
                // Since Room doesn't allow sync calls on main, and we're likely in a background thread
                // We use a run blocking or just expect the caller to have provided data.
                // For simplicity, we'll fetch them here.
                val orders = kotlinx.coroutines.runBlocking { db.orderDao().getOrdersForCustomer(party.companyName) }
                orders
            }

            if (history.isEmpty()) {
                canvas.drawText("No Order History Found", margin + 20f, yPosition, normalPaint)
                yPosition += 30f
            } else {
                canvas.drawText("Recent Orders:", margin, yPosition, labelPaint)
                yPosition += 25f
                for (order in history.take(5)) { // Limit to last 5 for PDF brevity
                    if (yPosition > 780f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                    }
                    canvas.drawText("${order.orderDate}: ${order.itemDescription} - ₹${order.total}", margin + 20f, yPosition, normalPaint)
                    yPosition += 25f
                }
            }

            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 40f
        }

        pdfDocument.finishPage(page)

        return try {
            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            pdfDocument.close()
        }
    }
}