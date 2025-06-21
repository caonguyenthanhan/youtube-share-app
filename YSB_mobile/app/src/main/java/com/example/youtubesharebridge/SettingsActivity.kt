package com.example.youtubesharebridge

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    private lateinit var deviceNameEditText: TextInputEditText
    private lateinit var securityCodeEditText: TextInputEditText
    private lateinit var serverAddressEditText: TextInputEditText
    private lateinit var bubbleSwitch: SwitchCompat
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("YouTubeShareBridge", Context.MODE_PRIVATE)

        // Khởi tạo views
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        securityCodeEditText = findViewById(R.id.securityCodeEditText)
        serverAddressEditText = findViewById(R.id.serverAddressEditText)
        bubbleSwitch = findViewById(R.id.bubbleSwitch)
        saveButton = findViewById(R.id.saveButton)
        
        // Hiển thị trường nhập địa chỉ server, nhưng vẫn ẩn bong bóng chat
        serverAddressEditText.visibility = View.VISIBLE
        findViewById<TextInputLayout>(R.id.serverAddressLayout).visibility = View.VISIBLE
        bubbleSwitch.visibility = View.GONE
        findViewById<TextView>(R.id.bubbleLabel).visibility = View.GONE

        // Load giá trị đã lưu
        deviceNameEditText.setText(sharedPreferences.getString("device_name", ""))
        securityCodeEditText.setText(sharedPreferences.getString("security_code", ""))
        serverAddressEditText.setText(SettingsUtils.getServerAddress(this))
        bubbleSwitch.isChecked = sharedPreferences.getBoolean("bubble_enabled", false)

        // Xử lý sự kiện nút lưu
        saveButton.setOnClickListener {
            val deviceName = deviceNameEditText.text?.toString()?.trim() ?: ""
            val securityCode = securityCodeEditText.text?.toString()?.trim() ?: ""
            val serverAddress = serverAddressEditText.text?.toString()?.trim() ?: ""
            // Tắt chức năng bong bóng
            val bubbleEnabled = false

            if (deviceName.isBlank() || securityCode.isBlank() || serverAddress.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Kiểm tra định dạng địa chỉ máy chủ
            if (!isValidServerUrl(serverAddress)) {
                Toast.makeText(this, "Địa chỉ máy chủ không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lưu cài đặt
            sharedPreferences.edit().apply {
                putString("device_name", deviceName)
                putString("security_code", securityCode)
                putString("server_address", serverAddress)
                putBoolean("bubble_enabled", bubbleEnabled)
                apply()
            }
            
            // Cập nhật cài đặt trong SettingsUtils
            SettingsUtils.setDeviceName(this, deviceName)
            SettingsUtils.setSecurityCode(this, securityCode)
            SettingsUtils.setServerAddress(this, serverAddress)
            SettingsUtils.setBubbleEnabled(this, bubbleEnabled)

            Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
            finish()
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
    
    private fun isValidServerUrl(url: String): Boolean {
        return try {
            // Kiểm tra URL có hợp lệ không
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "http://" + url
            } else {
                url
            }
            
            val urlObj = URL(validUrl)
            
            // Kiểm tra host và port
            val host = urlObj.host
            val port = if (urlObj.port == -1) urlObj.defaultPort else urlObj.port
            
            // Đảm bảo có host và port hợp lệ
            host.isNotEmpty() && port in 1..65535
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi định dạng URL: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}