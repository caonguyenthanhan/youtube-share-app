package com.example.youtubesharebridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.content.Context
import android.content.SharedPreferences
import android.app.Activity
import com.google.android.material.textfield.TextInputEditText
import android.net.Uri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sharedLinkTextView: TextView
    private lateinit var deviceNameEditText: TextInputEditText
    private lateinit var securityCodeEditText: TextInputEditText
    private lateinit var targetComputerIdEditText: TextInputEditText
    private lateinit var serverIpEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var serverChoiceGroup: RadioGroup
    private lateinit var localServerRadio: RadioButton
    private lateinit var remoteServerRadio: RadioButton
    private var sharedLink = ""
    private lateinit var sharedPreferences: SharedPreferences
    private var isFromShare = false
    private lateinit var manualLinkEditText: TextInputEditText
    private lateinit var sendManualLinkButton: Button
    private lateinit var logRecyclerView: RecyclerView
    private lateinit var logAdapter: LogAdapter
    private val logList = mutableListOf<Pair<String, Boolean>>() // Pair<log, isSuccess>

    companion object {
        private const val CHANNEL_ID = "YouTubeShareBridge"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("YouTubeShareBridge", Context.MODE_PRIVATE)

        // Khởi tạo views
        sharedLinkTextView = findViewById(R.id.sharedLinkTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        securityCodeEditText = findViewById(R.id.securityCodeEditText)
        targetComputerIdEditText = findViewById(R.id.targetComputerIdEditText)
        serverIpEditText = findViewById(R.id.serverIpEditText)
        saveButton = findViewById(R.id.saveButton)
        serverChoiceGroup = findViewById(R.id.serverChoiceGroup)
        localServerRadio = findViewById(R.id.localServerRadio)
        remoteServerRadio = findViewById(R.id.remoteServerRadio)
        manualLinkEditText = findViewById(R.id.manualLinkEditText)
        sendManualLinkButton = findViewById(R.id.sendManualLinkButton)
        logRecyclerView = findViewById(R.id.logRecyclerView)
        logAdapter = LogAdapter(logList)
        logRecyclerView.adapter = logAdapter
        logRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load giá trị đã lưu
        deviceNameEditText.setText(sharedPreferences.getString("device_name", ""))
        securityCodeEditText.setText(sharedPreferences.getString("security_code", ""))
        targetComputerIdEditText.setText(sharedPreferences.getString("target_computer_id", ""))
        serverIpEditText.setText(sharedPreferences.getString("server_ip", "192.168.56.1:4000"))

        // Load lựa chọn server đã lưu
        val isLocalServer = sharedPreferences.getBoolean("is_local_server", true)
        localServerRadio.isChecked = isLocalServer
        remoteServerRadio.isChecked = !isLocalServer

        // Xử lý sự kiện thay đổi lựa chọn server
        serverChoiceGroup.setOnCheckedChangeListener { _, checkedId ->
            val isLocal = checkedId == R.id.localServerRadio
            sharedPreferences.edit().putBoolean("is_local_server", isLocal).apply()
            // Ẩn/hiện trường IP server dựa vào lựa chọn
            serverIpEditText.visibility = if (isLocal) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Xử lý sự kiện nút lưu
        saveButton.setOnClickListener {
            val deviceName = deviceNameEditText.text.toString()
            val securityCode = securityCodeEditText.text.toString()
            val targetComputerId = targetComputerIdEditText.text.toString()
            val serverIp = serverIpEditText.text.toString()

            if (deviceName.isBlank() || securityCode.isBlank() || targetComputerId.isBlank() || (localServerRadio.isChecked && serverIp.isBlank())) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra định dạng IP nếu đang chọn gửi đến máy
            if (localServerRadio.isChecked && !isValidServerAddress(serverIp)) {
                Toast.makeText(this, "Địa chỉ IP không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lưu cài đặt
            sharedPreferences.edit().apply {
                putString("device_name", deviceName)
                putString("security_code", securityCode)
                putString("target_computer_id", targetComputerId)
                putString("server_ip", serverIp)
                apply()
            }

            Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
        }

        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        sendManualLinkButton.setOnClickListener {
            val manualLink = manualLinkEditText.text?.toString()?.trim()
            if (manualLink.isNullOrBlank()) {
                Toast.makeText(this, "Vui lòng nhập liên kết", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addLogToChat(manualLink, true) // Giả sử gửi thành công, sẽ cập nhật lại sau khi gửi thực tế
            val isLocalServer = sharedPreferences.getBoolean("is_local_server", true)
            if (isLocalServer) {
                sendLinkToLocalServer(manualLink)
            } else {
                sendLinkToRemoteServer(manualLink)
            }
        }
    }

    private fun isValidServerAddress(address: String): Boolean {
        return try {
            // Kiểm tra định dạng IP:PORT
            val parts = address.split(":")
            if (parts.size != 2) return false

            val ip = parts[0]
            val port = parts[1].toInt()

            // Kiểm tra port
            if (port !in 1..65535) return false

            // Kiểm tra IP
            val ipParts = ip.split(".")
            if (ipParts.size != 4) return false

            ipParts.all { part ->
                part.toIntOrNull()?.let { it in 0..255 } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getDeviceName(): String {
        return sharedPreferences.getString("device_name", Build.MODEL) ?: Build.MODEL
    }

    private fun getSecurityCode(): String {
        return sharedPreferences.getString("security_code", "") ?: ""
    }

    private fun getLocalServerUrl(): String {
        val serverIp = sharedPreferences.getString("server_ip", "192.168.56.1:4000") ?: "192.168.56.1:4000"
        return "http://$serverIp"
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo notification channel cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YouTube Share Bridge",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Khi nhấn vào notification, mở giao diện xin quyền overlay
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(getColor(R.color.ic_launcher_background))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Nếu mở từ share intent, đóng app và quay về app trước đó
        if (isFromShare) {
            finish()
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestDrawOverlaysPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun sendLogToChatHead(log: String, isSuccess: Boolean) {
        val intent = Intent(this, ChatHeadService::class.java)
        intent.action = "com.example.youtubesharebridge.ADD_LOG"
        intent.putExtra("log", log)
        intent.putExtra("success", isSuccess)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun sendLinkToLocalServer(link: String) {
        val securityCode = getSecurityCode()
        if (securityCode.isBlank()) {
            if (canDrawOverlays()) {
                sendLogToChatHead("Nội dung log", false)
            } else {
                showNotification("Lỗi", "Vui lòng cài đặt quyền vẽ trên các ứng dụng khác trước")
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("link", link)
                    put("device", getDeviceName())
                    put("security_code", securityCode)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(getLocalServerUrl())
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        if (canDrawOverlays()) {
                            sendLogToChatHead("Nội dung log", true)
                        } else {
                            showNotification("Thành công", "Đã gửi link đến máy tính!")
                        }
                    } else {
                        if (canDrawOverlays()) {
                            sendLogToChatHead("Lỗi gửi link: ${response.code}", false)
                        } else {
                            showNotification("Lỗi", "Lỗi gửi link: ${response.code}. Vui lòng kiểm tra lại địa chỉ IP máy tính.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (canDrawOverlays()) {
                    sendLogToChatHead("Lỗi kết nối: ${e.message}", false)
                } else {
                    showNotification("Lỗi", "Lỗi kết nối: ${e.message}. Vui lòng kiểm tra lại địa chỉ IP máy tính.")
                }
            }
        }
    }

    private fun sendLinkToRemoteServer(link: String) {
        val securityCode = getSecurityCode()
        if (securityCode.isBlank()) {
            if (canDrawOverlays()) {
                sendLogToChatHead("Nội dung log", false)
            } else {
                showNotification("Lỗi", "Vui lòng cài đặt quyền vẽ trên các ứng dụng khác trước")
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("link", link)
                    put("device", getDeviceName())
                    put("security_code", securityCode)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://34.136.11.166:8080/set_link")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        if (canDrawOverlays()) {
                            sendLogToChatHead("Nội dung log", true)
                        } else {
                            showNotification("Thành công", "Đã gửi link đến server!")
                        }
                    } else {
                        if (canDrawOverlays()) {
                            sendLogToChatHead("Lỗi gửi link: ${response.code}", false)
                        } else {
                            showNotification("Lỗi", "Lỗi gửi link: ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (canDrawOverlays()) {
                    sendLogToChatHead("Lỗi kết nối: ${e.message}", false)
                } else {
                    showNotification("Lỗi", "Lỗi kết nối: ${e.message}")
                }
            }
        }
    }

    private fun addLogToChat(log: String, isSuccess: Boolean) {
        if (logList.size >= 20) logList.removeAt(0)
        logList.add(Pair(log, isSuccess))
        logAdapter.notifyDataSetChanged()
        logRecyclerView.scrollToPosition(logList.size - 1)
        // Đồng bộ với bóng chat nếu có quyền
        if (canDrawOverlays()) {
            sendLogToChatHead(log, isSuccess)
        }
    }
}

class LogAdapter(private val logs: List<Pair<String, Boolean>>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val (log, isSuccess) = logs[position]
        holder.textView.text = if (isSuccess) "✔ $log" else "✖ $log"
        holder.textView.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSuccess) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
    }

    override fun getItemCount() = logs.size
}