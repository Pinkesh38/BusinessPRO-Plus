package com.example.businessproplus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class PartyAdapter(
    private val context: Context,
    private var parties: List<Party>,
    private val onEditClick: (Party) -> Unit,
    private val onViewOrdersClick: (Party) -> Unit,
    private val onDeleteClick: (Party) -> Unit
) : RecyclerView.Adapter<PartyAdapter.PartyViewHolder>() {

    inner class PartyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompanyName: TextView = view.findViewById(R.id.tvCompanyName)
        val tvContactInfo: TextView = view.findViewById(R.id.tvContactInfo)
        val tvLastOrder: TextView = view.findViewById(R.id.tvLastOrder)
        val tvCreditLimit: TextView = view.findViewById(R.id.tvCreditLimit)
        val btnCall: MaterialButton = view.findViewById(R.id.btnCall)
        val btnSMS: MaterialButton = view.findViewById(R.id.btnSMS)
        val btnWhatsApp: MaterialButton = view.findViewById(R.id.btnWhatsApp)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnViewOrders: MaterialButton = view.findViewById(R.id.btnViewOrders)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteParty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_party, parent, false)
        return PartyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartyViewHolder, position: Int) {
        val party = parties[position]

        holder.tvCompanyName.text = party.companyName
        holder.tvContactInfo.text = "${party.contactNo} • ${party.contactPerson}"
        holder.tvLastOrder.text = if (party.lastOrderDate.isNotEmpty()) party.lastOrderDate else "No orders yet"
        holder.tvCreditLimit.text = "₹${String.format("%.0f", party.creditLimit)}"

        // 🛡️ UI IMPROVEMENT: Click anywhere on the item to edit
        holder.itemView.setOnClickListener { onEditClick(party) }

        holder.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${party.contactNo}")
            }
            context.startActivity(intent)
        }

        holder.btnSMS.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:${party.contactNo}")
            }
            context.startActivity(intent)
        }

        holder.btnWhatsApp.setOnClickListener {
            try {
                val phone = party.contactNo.replace(" ", "").replace("+", "")
                val url = "https://api.whatsapp.com/send?phone=$phone"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.btnEdit.setOnClickListener { onEditClick(party) }
        holder.btnViewOrders.setOnClickListener { onViewOrdersClick(party) }
        holder.btnDelete.setOnClickListener { onDeleteClick(party) }
    }

    override fun getItemCount() = parties.size

    fun updateList(newParties: List<Party>) {
        parties = newParties
        notifyDataSetChanged()
    }
}
