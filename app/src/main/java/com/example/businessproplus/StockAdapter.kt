package com.example.businessproplus

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StockAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : ListAdapter<Item, StockAdapter.StockViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick, onDeleteClick)
    }

    class StockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        private val tvItemDetails: TextView = view.findViewById(R.id.tvItemDetails)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteItem)

        fun bind(item: Item, onClick: (Item) -> Unit, onDelete: (Item) -> Unit) {
            tvItemName.text = "${item.itemName} (${item.category})"
            tvItemDetails.text = "Price: ₹${item.salesPrice} / ${item.unit} | Stock: ${item.currentStock}"
            
            // 🛡️ UI UX: Highlight low stock items
            if (item.currentStock < item.minStockLevel) {
                itemView.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset for recycled views
            }

            itemView.setOnClickListener { onClick(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem == newItem
    }
}
