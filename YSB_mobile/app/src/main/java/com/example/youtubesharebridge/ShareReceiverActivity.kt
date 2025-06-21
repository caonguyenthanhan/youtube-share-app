package com.example.youtubesharebridge

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Xử lý intent chia sẻ
        if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    val permIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    permIntent.data = android.net.Uri.parse("package:$packageName")
                    permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(permIntent)
                    Toast.makeText(this, "Vui lòng cấp quyền vẽ trên ứng dụng khác để sử dụng bong bóng chat!", Toast.LENGTH_LONG).show()
                } else {
                    val serviceIntent = Intent(this, ChatHeadService::class.java).apply {
                        action = ChatHeadService.ACTION_SET_LINK
                        putExtra("link", sharedText)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    // Đảm bảo bong bóng chat luôn hiển thị
                    val showIntent = Intent(this, ChatHeadService::class.java).apply {
                        action = ChatHeadService.ACTION_SHOW
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(showIntent)
                    } else {
                        startService(showIntent)
                    }
                }
            }
        }
        
        // Đóng Activity sau khi xử lý xong
        finish()
    }
} 