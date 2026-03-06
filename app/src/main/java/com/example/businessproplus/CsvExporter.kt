package com.example.businessproplus

import android.content.Context
import android.net.Uri
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class CsvExporter(private val context: Context) {

    fun exportOrdersToCsv(uri: Uri, orders: List<Order>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    // Write Header
                    writer.write("Order ID,Manual No,Customer,Contact,Item,Qty,Price,Total,Advance,Remaining,Date,Delivery,Status,Paid,Due,Remarks")
                    writer.newLine()

                    // Write Data
                    for (order in orders) {
                        val row = StringBuilder()
                        row.append("${order.id},")
                        row.append("${escapeCsv(order.manualOrderNo)},")
                        row.append("${escapeCsv(order.customerName)},")
                        row.append("${escapeCsv(order.contactNumber)},")
                        row.append("${escapeCsv(order.itemDescription)},")
                        row.append("${order.quantity},")
                        row.append("${order.price},")
                        row.append("${order.total},")
                        row.append("${order.advancePayment},")
                        row.append("${order.remainingPayment},")
                        row.append("${order.orderDate},")
                        row.append("${order.deliveryDate},")
                        row.append("${order.status},")
                        row.append("${if (order.isPaid) "Yes" else "No"},")
                        row.append("${order.dueAmount},")
                        row.append("${escapeCsv(order.remarks)}")
                        
                        writer.write(row.toString())
                        writer.newLine()
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportItemsToCsv(uri: Uri, items: List<Item>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("Item ID,Name,Unit,Purchase Price,Sales Price,Category,Stock,Min Level,Description")
                    writer.newLine()

                    for (item in items) {
                        val row = StringBuilder()
                        row.append("${item.id},")
                        row.append("${escapeCsv(item.itemName)},")
                        row.append("${escapeCsv(item.unit)},")
                        row.append("${item.purchasePrice},")
                        row.append("${item.salesPrice},")
                        row.append("${escapeCsv(item.category)},")
                        row.append("${item.currentStock},")
                        row.append("${item.minStockLevel},")
                        row.append("${escapeCsv(item.itemDescription)}")
                        
                        writer.write(row.toString())
                        writer.newLine()
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportPartiesToCsv(uri: Uri, parties: List<Party>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("Party ID,Type,Company Name,Contact Person,Mobile,GST,Address,Credit Limit,Credit Days")
                    writer.newLine()

                    for (party in parties) {
                        val row = StringBuilder()
                        row.append("${party.id},")
                        row.append("${party.partyType},")
                        row.append("${escapeCsv(party.companyName)},")
                        row.append("${escapeCsv(party.contactPerson)},")
                        row.append("${escapeCsv(party.contactNo)},")
                        row.append("${escapeCsv(party.gstNo)},")
                        row.append("${escapeCsv(party.address)},")
                        row.append("${party.creditLimit},")
                        row.append("${party.creditPeriodDays}")
                        
                        writer.write(row.toString())
                        writer.newLine()
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }
}