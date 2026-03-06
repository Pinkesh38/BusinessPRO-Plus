package com.example.businessproplus

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class UserAdapter(
    private val context: Context,
    private var users: List<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvUserDetails: TextView = view.findViewById(R.id.tvUserDetails)
        val tvUserRole: TextView = view.findViewById(R.id.tvUserRole)
        val cardRoleBadge: MaterialCardView = view.findViewById(R.id.cardRoleBadge)
        val ivUserAvatar: ImageView = view.findViewById(R.id.ivUserAvatar)
        val btnEditUser: ImageButton = view.findViewById(R.id.btnEditUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvUsername.text = user.username
        holder.tvUserDetails.text = user.mobileNo.ifEmpty { "No mobile number" }
        holder.tvUserRole.text = user.role.uppercase()

        // Role Badge Color Coding
        val (bgColor, textColor) = when (user.role.lowercase()) {
            "admin" -> Color.parseColor("#E3F2FD") to Color.parseColor("#1565C0") // Blue
            else -> Color.parseColor("#F5F5F5") to Color.parseColor("#616161") // Gray/Staff
        }
        holder.cardRoleBadge.setCardBackgroundColor(bgColor)
        holder.tvUserRole.setTextColor(textColor)

        // Placeholder for Avatar
        holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery)

        holder.btnEditUser.setOnClickListener { onEditClick(user) }
        
        holder.itemView.setOnLongClickListener {
            onDeleteClick(user)
            true
        }
    }

    override fun getItemCount() = users.size

    fun updateList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}