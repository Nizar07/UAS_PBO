package com.example.uts_pbo

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uts_pbo.adapter.UserAdapter
import com.example.uts_pbo.databinding.ActivityMainBinding
import com.example.uts_pbo.model.User
import com.google.firebase.database.*
import android.util.Log
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var userList: MutableList<User>
    private lateinit var userAdapter: UserAdapter

    companion object {
        const val REQUEST_CONTACT_PICKER = 1
        const val REQUEST_CONTACT_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        database = FirebaseDatabase.getInstance("https://testutspbo-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users")
        userList = mutableListOf()
        userAdapter = UserAdapter(userList, { user -> sendWhatsAppNotification(user) }, { user -> deleteUser(user) })

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsers.adapter = userAdapter

        binding.buttonAddUser.setOnClickListener {
            addUser()
            Log.d("MainActivity", "User telah masuk")
        }

        binding.ButtonAddContact.setOnClickListener {
            checkContactPermission()
        }

        loadUsers()
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSION)
        } else {
            pickContact()
        }
    }

    private fun pickContact() {
        val contactPickerIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(contactPickerIntent, REQUEST_CONTACT_PICKER)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACT_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                pickContact()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONTACT_PICKER && resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = data?.data
            contactUri?.let {
                val cursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    if (hasPhoneNumber > 0) {
                        val phonesCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )

                        phonesCursor?.let {
                            if (phonesCursor.moveToFirst()) {
                                val phone = phonesCursor.getString(phonesCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                addContactToDatabase(name, phone)
                            }
                            phonesCursor.close()
                        }
                    }
                }
                cursor?.close()
            }
        }
    }

    private fun addContactToDatabase(name: String, phone: String) {
        val id = database.push().key ?: run {
            Log.e("MainActivity", "Failed to push to database")
            return
        }
        val user = User(id, name, phone, "")

        database.child(id).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
            }
        }
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
