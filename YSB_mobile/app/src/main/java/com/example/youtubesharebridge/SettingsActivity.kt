package com.example.youtubesharebridge

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
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
            val bubbleEnabled = bubbleSwitch.isChecked

            if (deviceName.isBlank() || securityCode.isBlank() || serverAddress.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!serverAddress.matches(Regex("""^https?://[\w.-]+(:\d+)?(/.*)?$""")) && !serverAddress.matches(Regex("""^[\w.-]+(:\d+)?$"""))) {
                Toast.makeText(this, "Địa chỉ server không hợp lệ", Toast.LENGTH_SHORT).show()
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
} 