package com.example.youtubesharebridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatHeadService : Service() {
    private var windowManager: WindowManager? = null
    private var chatHeadView: View? = null
    private var chatHeadParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var badgeView: TextView? = null
    private var sharedLink: String? = null
    private var hasPendingLink = false
    private var computers: List<Computer> = emptyList()

    companion object {
        private const val CHANNEL_ID = "ChatHeadService"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SHOW = "com.example.youtubesharebridge.SHOW_CHAT_HEAD"
        const val ACTION_SET_LINK = "com.example.youtubesharebridge.SET_LINK"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlays()) {
            requestOverlayPermission()
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_SHOW -> showChatHead()
            ACTION_SET_LINK -> {
                sharedLink = intent.getStringExtra("link")
                hasPendingLink = !sharedLink.isNullOrBlank()
                updateBadge()
            }
            else -> showChatHead()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        computers = loadComputerList()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (chatHeadView != null) {
            windowManager?.removeView(chatHeadView)
            chatHeadView = null
        }
    }

    private fun showChatHead() {
        if (chatHeadView != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        chatHeadView = LayoutInflater.from(this).inflate(R.layout.chat_head, null)
        badgeView = chatHeadView?.findViewById(R.id.badgeTextView)
        updateBadge()
        chatHeadParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
        chatHeadView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = chatHeadParams?.x ?: 0
                    initialY = chatHeadParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    chatHeadParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    chatHeadParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(chatHeadView, chatHeadParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - initialTouchX) < 5 && Math.abs(event.rawY - initialTouchY) < 5) {
                        showComputerMenu()
                    }
                    true
                }
                else -> false
            }
        }
        windowManager?.addView(chatHeadView, chatHeadParams)
    }

    private fun updateBadge() {
        badgeView?.visibility = if (hasPendingLink) View.VISIBLE else View.GONE
        badgeView?.text = if (hasPendingLink) "!" else ""
    }

    private fun showComputerMenu() {
        if (computers.isEmpty()) {
            Toast.makeText(this, "Không có máy nào", Toast.LENGTH_SHORT).show()
            return
        }
        val names = computers.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chọn máy để gửi link")
            .setItems(names) { _, which ->
                val computer = computers[which]
                if (hasPendingLink && !sharedLink.isNullOrBlank()) {
                    processSharedLinkForComputer(computer.name)
                    hasPendingLink = false
                    sharedLink = null
                    updateBadge()
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun processSharedLinkForComputer(computerName: String) {
        val computer = computers.find { it.name == computerName }
        val link = sharedLink
        if (computer != null && !link.isNullOrBlank()) {
            sendLinkToComputer(link, computer)
            Toast.makeText(this, "Đã gửi link cho ${computer.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Không tìm thấy máy hoặc link rỗng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendLinkToComputer(link: String, computer: Computer) {
        val securityCode = SettingsUtils.getSecurityCode(this)
        val deviceName = SettingsUtils.getDeviceName(this)
        val serverAddress = SettingsUtils.getServerAddress(this)
        if (securityCode.isBlank() || deviceName.isBlank() || serverAddress.isBlank()) {
            Toast.makeText(this, "Thiếu thông tin cấu hình", Toast.LENGTH_SHORT).show()
            return
        }
        val json = JSONObject().apply {
            put("link", link)
            put("device", deviceName)
            put("security_code", securityCode)
            put("target_computer_id", computer.targetComputerId)
        }
        val client = OkHttpClient()
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = if (serverAddress.startsWith("http")) "http://$serverAddress" else serverAddress
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    runOnUiThreadSafe { Toast.makeText(this, "Lỗi gửi link: ${response.code}", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                runOnUiThreadSafe { Toast.makeText(this, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun runOnUiThreadSafe(action: () -> Unit) {
        val handler = android.os.Handler(mainLooper)
        handler.post { action() }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = android.net.Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Toast.makeText(this, "Vui lòng cấp quyền vẽ trên ứng dụng khác", Toast.LENGTH_LONG).show()
    }

    private fun loadComputerList(): List<Computer> {
        val file = File(filesDir, "ComputerList.json")
        if (!file.exists()) {
            assets.open("ComputerList.json").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        val json = file.readText()
        val arr = JSONArray(json)
        val list = mutableListOf<Computer>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Computer(obj.getString("name"), obj.getString("target_computer_id")))
        }
        return list
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Head Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YouTube Share Bridge")
            .setContentText("Bóng chat đang chạy")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
} 