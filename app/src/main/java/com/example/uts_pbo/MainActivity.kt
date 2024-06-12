package com.example.uts_pbo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uts_pbo.adapter.UserAdapter
import com.example.uts_pbo.databinding.ActivityMainBinding
import com.example.uts_pbo.model.User
import com.google.firebase.database.*
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var userList: MutableList<User>
    private lateinit var userAdapter: UserAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        database = FirebaseDatabase.getInstance("https://testutspbo-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users")
        userList = mutableListOf()
        userAdapter = UserAdapter(userList, { user -> sendWhatsAppNotification(user) }, { user -> deleteUser((user))})

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsers.adapter = userAdapter

        binding.buttonAddUser.setOnClickListener {
            addUser()
            Log.d("MainActivity", "User telah masuk")
        }

        loadUsers()
    }


    private fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = "^\\+?[1-9]\\d{1,14}$".toRegex()
        return phone.matches(phoneRegex)
    }

    private fun addUser() {
        val name = binding.editTextName.text.toString().trim()
        var phone = binding.editTextPhone.text.toString().trim()
        val plate = binding.editTextPlate.text.toString().trim()

        if (phone.isEmpty()) {
            Toast.makeText(this, "Nomor Telefon Harus Di isi", Toast.LENGTH_SHORT).show()
            return
        }

        if (!phone.startsWith("+")) {
            phone = "+62$phone"
        }

        val id = database.push().key ?: run {
            Log.e("MainActivity", "Gagal push ke database")
            return
        }
        val user = User(id, name, phone, plate)

        database.child(id).setValue(user).addOnCompleteListener { it ->
            if (it.isSuccessful) {
                Toast.makeText(this, "User berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                binding.editTextName.text.clear()
                binding.editTextPhone.text.clear()
                binding.editTextPlate.text.clear()
                Log.d("MainActivity", "User $name dan $phone dan $plate")
            } else {
                Toast.makeText(this, "Gagal menambahkan user", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "User $name dan $phone dan $plate")
//                Log.e("FirebaseError", task.exception?.message ?: "Unkown error")
            }
        }
    }

    private fun loadUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(User::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                Log.e("Main Activity", "Gagal memuat data: ${error.message}")
            }
        })
    }


    private fun sendWhatsAppNotification(user: User) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=${user.phone}&text=Mobil dengan plat nomor ${user.plate.uppercase()} sudah selesai dicuci. Terima kasih")
        startActivity(intent)
    }


    private fun deleteUser(user: User) {
        database.child(user.id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal dihapus", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

