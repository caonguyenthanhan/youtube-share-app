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

    fun getServerAddress(context: Context): String =
        prefs(context).getString(KEY_SERVER_ADDRESS, "") ?: ""

    fun isBubbleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BUBBLE_ENABLED, false)

    fun setDeviceName(context: Context, value: String) =
        prefs(context).edit().putString(KEY_DEVICE_NAME, value).apply()

    fun setSecurityCode(context: Context, value: String) =
        prefs(context).edit().putString(KEY_SECURITY_CODE, value).apply()

    fun setServerAddress(context: Context, value: String) =
        prefs(context).edit().putString(KEY_SERVER_ADDRESS, value).apply()

    fun setBubbleEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BUBBLE_ENABLED, value).apply()
} 