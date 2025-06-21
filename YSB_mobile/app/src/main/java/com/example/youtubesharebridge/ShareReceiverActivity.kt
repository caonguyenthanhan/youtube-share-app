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
        
        try {
            // Xử lý intent chia sẻ
            if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    // Thay vì sử dụng bong bóng chat, mở trực tiếp HomeActivity để chọn máy tính
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.action = Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_TEXT, sharedText)
                    // Không sử dụng FLAG_ACTIVITY_NEW_TASK để tránh lỗi
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    
                    // Hiển thị thông báo
                    Toast.makeText(this, "Đã nhận liên kết. Vui lòng chọn máy tính để gửi.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Xử lý lỗi khi chuyển màn hình
            Toast.makeText(this, "Lỗi khi xử lý liên kết: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            // Đóng Activity sau khi xử lý xong
            finish()
        }
    }
}