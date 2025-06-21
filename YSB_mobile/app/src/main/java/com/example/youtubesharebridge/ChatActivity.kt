package com.example.youtubesharebridge

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class ChatActivity : AppCompatActivity() {
    private lateinit var computerName: String
    private lateinit var targetComputerId: String
    private var sharedText: String? = null
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Lấy thông tin từ intent
        computerName = intent.getStringExtra("computer_name") ?: "Máy tính"
        targetComputerId = intent.getStringExtra("target_computer_id") ?: ""
        sharedText = intent.getStringExtra("shared_text")

        // Hiển thị tên máy tính
        findViewById<TextView>(R.id.computerNameText).text = computerName

        // Khởi tạo RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // Khởi tạo input và nút gửi
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        // Nếu có shared text, tự động điền vào input
        if (!sharedText.isNullOrEmpty()) {
            messageInput.setText(sharedText)
        }

        // Xử lý sự kiện gửi tin nhắn
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageInput.text.clear()
            }
        }

        // Nếu có shared text, tự động gửi
        if (!sharedText.isNullOrEmpty()) {
            sendMessage(sharedText!!)
        }
    }

    private fun sendMessage(text: String) {
        val message = ChatMessage(text, true) // true = tin nhắn gửi đi
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        
        // TODO: Gửi tin nhắn đến server
        Toast.makeText(this, "Đã gửi đến $computerName", Toast.LENGTH_SHORT).show()
    }
}

class ChatAdapter(private val messages: List<ChatMessage>) : 
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text
        // TODO: Thêm xử lý hiển thị tin nhắn gửi/nhận
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }
}

data class ChatMessage(
    val text: String,
    val isSent: Boolean
) 