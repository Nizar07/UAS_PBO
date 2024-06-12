package com.example.uts_pbo.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_pbo.R
import com.example.uts_pbo.model.User
//import kotlinx.android.synthetic.main.item_user.view.*
import com.example.uts_pbo.databinding.ItemUserBinding
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.database.FirebaseDatabase


class UserAdapter(

    //private var database: DatabaseReference,
    private val users: List<User>,
    private val onNotifyClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textViewName.text = "Nama : " + user.name
            binding.textViewPhone.text = "Nomor Hp : " + user.phone
            binding.textViewPlate.text = "Plat Mobil : " + user.plate
            binding.buttonNotify.setOnClickListener {onNotifyClick(user)}
            binding.buttonDeleted.setOnClickListener {onDeleteClick(user)}
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context))
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size
}
