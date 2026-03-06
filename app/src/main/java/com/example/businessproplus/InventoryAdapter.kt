package com.example.businessproplus

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class InventoryAdapter(
    private val context: Context,
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit // Added Delete Callback
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    inner class InventoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvStockCount: TextView = view.findViewById(R.id.tvStockCount)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val viewStockIndicator: View = view.findViewById(R.id.viewStockIndicator)
        val layoutLowStockWarning: View = view.findViewById(R.id.layoutLowStockWarning)
        val progressStock: LinearProgressIndicator = view.findViewById(R.id.progressStock)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteItem) // New Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvItemName.text = item.itemName
        holder.tvCategory.text = item.category
        holder.tvStockCount.text = "${item.currentStock} ${item.unit}"
        holder.tvPrice.text = "₹${String.format("%.2f", item.salesPrice)}"

        val isLowStock = item.currentStock <= item.minStockLevel
        
        // Stock Indicator Dot
        val indicatorColor = if (isLowStock) R.color.accent_red else R.color.accent_green
        holder.viewStockIndicator.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, indicatorColor))

        // Low Stock Warning
        holder.layoutLowStockWarning.visibility = if (isLowStock) View.VISIBLE else View.GONE

        // Progress Bar
        val maxProgress = if (item.openingStock > 0) item.openingStock else (item.currentStock + 10)
        val progress = (item.currentStock.toFloat() / (if (maxProgress > 0) maxProgress else 1) * 100).toInt()
        holder.progressStock.progress = progress
        holder.progressStock.setIndicatorColor(ContextCompat.getColor(context, indicatorColor))

        holder.itemView.setOnClickListener { onItemClick(item) }
        
        // Wire up delete button
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }
}