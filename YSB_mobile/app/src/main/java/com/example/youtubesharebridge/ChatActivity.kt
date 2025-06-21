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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private lateinit var computerName: String
    private lateinit var targetComputerId: String
    private var sharedText: String? = null
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var isSending = false // Biến để theo dõi trạng thái gửi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Lấy thông tin từ intent
        computerName = intent.getStringExtra("computer_name") ?: "Máy tính"
        targetComputerId = intent.getStringExtra("target_computer_id") ?: ""
        sharedText = intent.getStringExtra("shared_text")

        // Hiển thị tên máy tính
        findViewById<TextView>(R.id.computerNameText).text = computerName
        
        // Thiết lập nút quay lại
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish() // Kết thúc activity hiện tại và quay lại màn hình trước đó
        }

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
            try {
                val message = messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                    messageInput.text.clear()
                }
            } catch (e: Exception) {
                // Xử lý ngoại lệ nếu có
                Toast.makeText(this, "Lỗi khi gửi tin nhắn: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Nếu có shared text, chỉ điền vào ô nhập liệu, không tự động gửi
        // để tránh lỗi khi gửi nhiều lần
    }

    private fun sendMessage(text: String) {
        // Kiểm tra nếu đang trong quá trình gửi, không cho phép gửi tiếp
        if (isSending) {
            Toast.makeText(this, "Đang gửi tin nhắn, vui lòng đợi", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Đánh dấu đang trong quá trình gửi và vô hiệu hóa nút gửi
        isSending = true
        sendButton.isEnabled = false
        
        // Thêm tin nhắn vào danh sách chat
        val message = ChatMessage(text, true) // true = tin nhắn gửi đi
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        
        // Gửi tin nhắn đến server và hiển thị kết quả
        val securityCode = SettingsUtils.getSecurityCode(this)
        val deviceName = SettingsUtils.getDeviceName(this)
        
        if (securityCode.isBlank() || deviceName.isBlank()) {
            val errorMessage = "Thiếu thông tin cấu hình. Vui lòng vào Cài đặt để thiết lập."
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            // Thêm tin nhắn lỗi vào danh sách chat
            val errorChatMessage = ChatMessage(errorMessage, false)
            messages.add(errorChatMessage)
            chatAdapter.notifyItemInserted(messages.size - 1)
            // Đánh dấu đã hoàn thành gửi
            isSending = false
            sendButton.isEnabled = true
            return
        }
        
        // Tạo JSON object để gửi
        val json = JSONObject().apply {
            put("link", text)
            put("device", deviceName)
            put("security_code", securityCode)
            put("target_computer_id", targetComputerId)
        }
        
        // Lấy địa chỉ máy chủ từ cài đặt
        val serverAddressBase = SettingsUtils.getServerAddress(this)
        if (serverAddressBase.isBlank()) {
            val errorMessage = "Chưa cấu hình địa chỉ máy chủ. Vui lòng vào Cài đặt để thiết lập."
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            // Thêm tin nhắn lỗi vào danh sách chat
            val errorChatMessage = ChatMessage(errorMessage, false)
            messages.add(errorChatMessage)
            chatAdapter.notifyItemInserted(messages.size - 1)
            // Đánh dấu đã hoàn thành gửi
            isSending = false
            sendButton.isEnabled = true
            return
        }
        
        // Sử dụng URL đích cố định http://34.61.180.50:8080/set_link thay vì /api/share
        val serverAddress = "http://34.61.180.50:8080/set_link"
        
        // Hiển thị thông tin JSON và server đích
        val serverInfo = try {
            val url = java.net.URL(serverAddress)
            val host = url.host
            val port = if (url.port == -1) "mặc định" else url.port.toString()
            "Gửi đến server: ${url.protocol}://$host:$port${url.path}"
        } catch (e: Exception) {
            "Gửi đến server: $serverAddress"
        }
        
        val jsonInfo = "Nội dung JSON gửi đi:\n${json.toString(4)}"
        val debugMessage = "$serverInfo\n\n$jsonInfo"
        
        // Thêm thông tin debug vào danh sách chat
        val debugChatMessage = ChatMessage(debugMessage, false)
        messages.add(debugChatMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        
        // Sử dụng Thread riêng biệt để thực hiện yêu cầu mạng
        Thread {
            var errorOccurred = false
            var errorMessage = ""
            
            try {
                val client = OkHttpClient()
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(serverAddress)
                    .post(requestBody)
                    .build()
                
                try {
                    val response = client.newCall(request).execute()
                    
                    // Xử lý kết quả
                    if (response.isSuccessful) {
                        val successMessage = "Đã gửi thành công đến $computerName"
                        errorOccurred = false
                        errorMessage = successMessage
                    } else {
                        val errorCode = response.code
                        val errorBody = response.body?.string() ?: "Không có thông tin lỗi"
                        errorOccurred = true
                        errorMessage = if (errorCode == 404) {
                            "Lỗi 404: Không tìm thấy API trên máy chủ.\n\nVui lòng kiểm tra:\n- Địa chỉ máy chủ đúng\n- Đường dẫn API đúng (phải có /api/share)\n- Máy chủ đang chạy và có endpoint /api/share\n\nĐịa chỉ hiện tại: $serverAddress"
                        } else {
                            "Lỗi gửi link: $errorCode\nPhản hồi: $errorBody\nĐịa chỉ máy chủ: $serverAddress"
                        }
                    }
                } catch (e: Exception) {
                    errorOccurred = true
                    errorMessage = "Lỗi kết nối: ${e.message}\n\nVui lòng kiểm tra:\n- Địa chỉ server đúng định dạng (http://34.61.180.50 hoặc http://34.61.180.50:8080)\n- Kết nối internet hoạt động\n- Server đang chạy\n\nĐịa chỉ hiện tại: $serverAddress"
                }
            } catch (e: Exception) {
                errorOccurred = true
                errorMessage = "Lỗi không xác định: ${e.message}\nVui lòng thử lại sau."
            }
            
            // Cập nhật UI trên main thread
            runOnUiThread {
                if (errorOccurred) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
                
                // Thêm tin nhắn phản hồi vào danh sách chat
                val responseMessage = ChatMessage(errorMessage, false)
                messages.add(responseMessage)
                chatAdapter.notifyItemInserted(messages.size - 1)
                
                // Đánh dấu đã hoàn thành gửi và kích hoạt lại nút gửi
                isSending = false
                sendButton.isEnabled = true
            }
        }.start()
    }
}

class ChatAdapter(private val messages: List<ChatMessage>) : 
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    // Định nghĩa các loại view
    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text
        
        // Thiết lập màu sắc và kiểu dáng dựa trên loại tin nhắn
        if (message.isSent) {
            // Tin nhắn gửi đi
            holder.messageText.setBackgroundResource(R.drawable.bg_message_sent)
        } else {
            // Tin nhắn nhận về
            holder.messageText.setBackgroundResource(R.drawable.bg_message_received)
        }
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }
}

data class ChatMessage(
    val text: String,
    val isSent: Boolean
)