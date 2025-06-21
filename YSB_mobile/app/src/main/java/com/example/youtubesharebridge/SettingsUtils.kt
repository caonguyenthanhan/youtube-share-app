package com.example.youtubesharebridge

import android.content.Context
import android.content.SharedPreferences

object SettingsUtils {
    private const val PREFS_NAME = "YouTubeShareBridgePrefs"
    private const val KEY_DEVICE_NAME = "pref_device_name"
    private const val KEY_SECURITY_CODE = "pref_security_code"
    private const val KEY_SERVER_ADDRESS = "pref_server_address"
    private const val KEY_BUBBLE_ENABLED = "pref_floating_bubble_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceName(context: Context): String =
        prefs(context).getString(KEY_DEVICE_NAME, "") ?: ""

    fun getSecurityCode(context: Context): String =
        prefs(context).getString(KEY_SECURITY_CODE, "") ?: ""

    fun getServerAddress(context: Context): String {
        val address = prefs(context).getString(KEY_SERVER_ADDRESS, "") ?: ""
        if (address.isBlank()) return ""
        
        // Đảm bảo địa chỉ server có định dạng đúng
        var formattedAddress = if (!address.startsWith("http://") && !address.startsWith("https://")) {
            "http://" + address
        } else {
            address
        }
        
        // Đảm bảo địa chỉ kết thúc với /api/share nếu chưa có
        if (!formattedAddress.endsWith("/api/share")) {
            // Xóa dấu / ở cuối nếu có
            if (formattedAddress.endsWith("/")) {
                formattedAddress = formattedAddress.substring(0, formattedAddress.length - 1)
            }
            formattedAddress += "/api/share"
        }
        
        return formattedAddress
    }

    fun isBubbleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BUBBLE_ENABLED, false)

    fun setDeviceName(context: Context, value: String) =
        prefs(context).edit().putString(KEY_DEVICE_NAME, value).apply()

    fun setSecurityCode(context: Context, value: String) =
        prefs(context).edit().putString(KEY_SECURITY_CODE, value).apply()

    fun setServerAddress(context: Context, value: String) {
        if (value.isBlank()) {
            prefs(context).edit().putString(KEY_SERVER_ADDRESS, "").apply()
            return
        }
        
        // Đảm bảo địa chỉ server có định dạng đúng
        var formattedValue = if (!value.startsWith("http://") && !value.startsWith("https://")) {
            "http://" + value
        } else {
            value
        }
        
        // Kiểm tra xem người dùng đã nhập đường dẫn API chưa
        // Nếu chưa có, không tự động thêm vào khi lưu để tránh gây nhầm lẫn
        // Đường dẫn API sẽ được thêm khi gọi getServerAddress()
        
        prefs(context).edit().putString(KEY_SERVER_ADDRESS, formattedValue).apply()
    }

    fun setBubbleEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BUBBLE_ENABLED, value).apply()
}