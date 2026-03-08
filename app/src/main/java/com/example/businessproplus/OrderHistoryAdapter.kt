package com.example.businessproplus

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class OrderHistoryAdapter(
    private val context: Context,
    private val onOrderClick: (Order) -> Unit,
    private val onDeleteOrder: (Order) -> Unit
) : PagingDataAdapter<Order, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private var expandedPosition = -1

    fun getItemAt(position: Int): Order? {
        return getItem(position)
    }

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderNo: TextView = view.findViewById(R.id.tvOrderNo)
        val tvOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
        val tvPartyName: TextView = view.findViewById(R.id.tvPartyName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val cardStatusBadge: MaterialCardView = view.findViewById(R.id.cardStatusBadge)
        val layoutDetails: View = view.findViewById(R.id.layoutDetails)
        val tvItemDesc: TextView = view.findViewById(R.id.tvItemDesc)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
        val tvRemainingPayment: TextView = view.findViewById(R.id.tvRemainingPayment)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteOrder)
        val tvMilestoneInfo: TextView = view.findViewById(R.id.tvMilestoneInfo)
        
        val steps = listOf(
            view.findViewById<ImageView>(R.id.step1),
            view.findViewById<ImageView>(R.id.step2),
            view.findViewById<ImageView>(R.id.step3),
            view.findViewById<ImageView>(R.id.step4),
            view.findViewById<ImageView>(R.id.step5)
        )
        val lines = listOf(
            view.findViewById<View>(R.id.line1),
            view.findViewById<View>(R.id.line2),
            view.findViewById<View>(R.id.line3),
            view.findViewById<View>(R.id.line4)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position) ?: return
        val isExpanded = position == expandedPosition

        holder.tvOrderNo.text = "#ORD-${order.id}"
        holder.tvOrderDate.text = order.orderDate
        holder.tvPartyName.text = order.customerName
        holder.tvStatus.text = order.status.uppercase()
        holder.tvItemDesc.text = order.itemDescription
        holder.tvQuantity.text = "${order.quantity}"
        holder.tvTotalAmount.text = "₹${order.total}"
        holder.tvRemainingPayment.text = "₹${order.remainingPayment}"

        val milestoneText = when (order.status) {
            "Completed" -> "Completed on: ${order.completedOn}"
            "Delivered" -> "Delivered on: ${order.deliveredOn}"
            "Working" -> "In Progress - Started: ${order.processedOn}"
            "Delayed" -> "Delayed - In Production"
            "Cancelled" -> "Order Cancelled"
            else -> "Status: Pending"
        }
        holder.tvMilestoneInfo.text = milestoneText

        val activeStep = when (order.status) {
            "Pending" -> 1
            "Working" -> 3
            "Completed" -> 4
            "Delivered" -> 5
            else -> 1
        }

        val activeColor = ContextCompat.getColor(context, R.color.primary)
        val inactiveColor = Color.parseColor("#BDBDBD")

        holder.steps.forEachIndexed { index, img ->
            val stepNum = index + 1
            img.setImageResource(if (stepNum <= activeStep) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background)
            img.setColorFilter(if (stepNum <= activeStep) activeColor else inactiveColor)
        }

        holder.lines.forEachIndexed { index, line ->
            line.setBackgroundColor(if (index + 1 < activeStep) activeColor else inactiveColor)
        }

        val (bgColor, textColor) = when (order.status.lowercase()) {
            "delivered" -> Color.parseColor("#E1F5FE") to Color.parseColor("#01579B")
            "completed" -> Color.parseColor("#E8F5E9") to Color.parseColor("#2E7D32")
            "pending" -> Color.parseColor("#FFF3E0") to Color.parseColor("#EF6C00")
            "working" -> Color.parseColor("#E3F2FD") to Color.parseColor("#1565C0")
            "cancelled" -> Color.parseColor("#FFEBEE") to Color.parseColor("#C62828")
            else -> Color.parseColor("#F5F5F5") to Color.parseColor("#616161")
        }
        holder.cardStatusBadge.setCardBackgroundColor(bgColor)
        holder.tvStatus.setTextColor(textColor)

        holder.layoutDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener {
            val prevExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else holder.bindingAdapterPosition
            if (prevExpanded != -1) notifyItemChanged(prevExpanded)
            if (expandedPosition != -1) notifyItemChanged(expandedPosition)
        }

        holder.btnViewDetails.setOnClickListener { onOrderClick(order) }
        holder.btnDelete.setOnClickListener { onDeleteOrder(order) }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}
