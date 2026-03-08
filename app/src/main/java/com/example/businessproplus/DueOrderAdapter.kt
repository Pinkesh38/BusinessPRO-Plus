package com.example.businessproplus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DueOrderAdapter(
    private val orders: List<Order>,
    private val onRemindClick: (Order) -> Unit,
    private val onCollectClick: (Order) -> Unit
) : RecyclerView.Adapter<DueOrderAdapter.DueViewHolder>() {

    class DueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCustomer: TextView = view.findViewById(R.id.tvDueCustomer)
        val tvAmount: TextView = view.findViewById(R.id.tvDueAmount)
        val tvItem: TextView = view.findViewById(R.id.tvDueItem)
        val tvPromiseDate: TextView = view.findViewById(R.id.tvPromiseDate)
        val btnRemind: MaterialButton = view.findViewById(R.id.btnRemind)
        val btnCollect: MaterialButton = view.findViewById(R.id.btnCollect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_due_order, parent, false)
        return DueViewHolder(view)
    }

    override fun onBindViewHolder(holder: DueViewHolder, position: Int) {
        val order = orders[position]
        holder.tvCustomer.text = order.customerName
        holder.tvAmount.text = "₹${order.remainingPayment}"
        holder.tvItem.text = order.itemDescription
        holder.tvPromiseDate.text = "Promised: ${order.paymentPromiseDate.ifEmpty { "Not Set" }}"
        
        holder.btnRemind.setOnClickListener { onRemindClick(order) }
        holder.btnCollect.setOnClickListener { onCollectClick(order) }
    }

    override fun getItemCount() = orders.size
}
