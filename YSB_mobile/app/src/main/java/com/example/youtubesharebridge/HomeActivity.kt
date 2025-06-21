package com.example.youtubesharebridge

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.text.InputType

class HomeActivity : AppCompatActivity() {
    private lateinit var computerList: MutableList<Computer>
    private lateinit var adapter: ComputerAdapter
    private lateinit var deviceName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Đọc danh sách máy tính từ file JSON
        computerList = loadComputerList().toMutableList()

        // Xử lý share từ ứng dụng khác
        handleIntent(intent)

        // Lấy tên thiết bị từ SharedPreferences
        val prefs = getSharedPreferences("YouTubeShareBridge", Context.MODE_PRIVATE)
        deviceName = prefs.getString("device_name", "Thiết bị của bạn") ?: "Thiết bị của bạn"

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.homeToolbar)
        toolbar.title = deviceName
        toolbar.inflateMenu(R.menu.menu_home)
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            } else false
        }

        // computerList đã được khởi tạo ở đầu onCreate

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.computerGridView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ComputerAdapter(computerList,
            onClick = { computer ->
                // Gọi hàm gửi link hoặc chuyển sang trang chat/log
                sendLinkToComputer(computer)
            },
            onDelete = { computer, position ->
                confirmDelete(computer, position)
            }
        )
        recyclerView.adapter = adapter
        println("Initial computer list size: ${computerList.size}") // Debug log
        adapter.notifyDataSetChanged() // Thêm dòng này để đảm bảo cập nhật UI

        // Nút thêm máy mới
        findViewById<FloatingActionButton>(R.id.addComputerButton).setOnClickListener {
            val inputName = TextInputEditText(this)
            inputName.hint = "Tên máy (hiển thị)"
            inputName.inputType = InputType.TYPE_CLASS_TEXT
            val inputId = TextInputEditText(this)
            inputId.hint = "Mã máy (target_computer_id)"
            inputId.inputType = InputType.TYPE_CLASS_TEXT
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(32, 16, 32, 0)
            layout.addView(inputName)
            layout.addView(inputId)
            AlertDialog.Builder(this)
                .setTitle("Thêm máy mới")
                .setView(layout)
                .setPositiveButton("Thêm") { _, _ ->
                    val name = inputName.text?.toString()?.trim() ?: ""
                    val id = inputId.text?.toString()?.trim() ?: ""
                    if (name.isBlank() || id.isBlank()) {
                        Toast.makeText(this, "Vui lòng nhập đủ tên và mã máy", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (computerList.any { it.targetComputerId == id }) {
                        Toast.makeText(this, "Mã máy đã tồn tại", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val newComputer = Computer(name, id)
                    computerList.add(newComputer)
                    println("Added new computer: $name, $id") // Debug log
                    println("Current list size: ${computerList.size}") // Debug log
                    saveComputerList(computerList)
                    println("Saved computer list") // Debug log
                    adapter.notifyDataSetChanged()
                    println("Notified adapter") // Debug log
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                showComputerSelectionDialog(sharedText)
            }
        }
    }

    private fun showComputerSelectionDialog(sharedText: String) {
        if (computerList.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một máy tính", Toast.LENGTH_LONG).show()
            return
        }

        val selectedComputers = mutableSetOf<Computer>()
        val items = computerList.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(items.size) { false }

        AlertDialog.Builder(this)
            .setTitle("Chọn máy tính để gửi")
            .setMultiChoiceItems(items, checkedItems) { _, index, checked ->
                if (checked) {
                    selectedComputers.add(computerList[index])
                } else {
                    selectedComputers.remove(computerList[index])
                }
            }
            .setPositiveButton("Gửi") { _, _ ->
                if (selectedComputers.isNotEmpty()) {
                    for (computer in selectedComputers) {
                        sendLinkToComputer(computer, sharedText)
                    }
                    // Thông báo đã chuyển sang ChatActivity để hiển thị kết quả gửi chi tiết
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun sendLinkToComputer(computer: Computer, sharedText: String? = null) {
        try {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("computer_name", computer.name)
            intent.putExtra("target_computer_id", computer.targetComputerId)
            if (sharedText != null) {
                intent.putExtra("shared_text", sharedText)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Xử lý lỗi khi chuyển màn hình
            Toast.makeText(this, "Lỗi khi mở màn hình chat: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun confirmDelete(computer: Computer, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa máy?")
            .setMessage("Bạn có chắc muốn xóa ${computer.name} không?")
            .setPositiveButton("Xóa") { _: DialogInterface, _: Int ->
                try {
                    computerList.removeAt(position)
                    saveComputerList(computerList)
                    adapter.notifyItemRemoved(position)
                    Toast.makeText(this, "Đã xóa ${computer.name}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    println("Error deleting computer: ${e.message}")
                    Toast.makeText(this, "Lỗi khi xóa máy", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadComputerList(): List<Computer> {
        val file = File(filesDir, "ComputerList.json")
        try {
            if (!file.exists()) {
                // Copy file mẫu từ assets nếu chưa có
                assets.open("ComputerList.json").use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val json = file.readText()
            println("Loaded JSON: $json") // Debug log
            val arr = try { JSONArray(json) } catch (e: Exception) { 
                println("Error parsing JSON: ${e.message}") // Debug log
                JSONArray() 
            }
            val list = mutableListOf<Computer>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Computer(obj.getString("name"), obj.getString("target_computer_id")))
            }
            println("Loaded ${list.size} computers") // Debug log
            return list
        } catch (e: Exception) {
            println("Error loading computer list: ${e.message}") // Debug log
            return emptyList()
        }
    }

    private fun saveComputerList(list: List<Computer>) {
        try {
            val arr = JSONArray()
            for (c in list) {
                val obj = org.json.JSONObject()
                obj.put("name", c.name)
                obj.put("target_computer_id", c.targetComputerId)
                arr.put(obj)
            }
            val file = File(filesDir, "ComputerList.json")
            file.writeText(arr.toString())
            println("Saved to file: ${file.absolutePath}") // Debug log
            println("File content: ${file.readText()}") // Debug log
        } catch (e: Exception) {
            println("Error saving computer list: ${e.message}") // Debug log
            e.printStackTrace()
        }
    }
}

class ComputerAdapter(
    private val computers: List<Computer>,
    val onClick: (Computer) -> Unit,
    val onDelete: (Computer, Int) -> Unit
) : RecyclerView.Adapter<ComputerAdapter.ComputerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComputerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_computer, parent, false)
        return ComputerViewHolder(view)
    }
    override fun getItemCount() = computers.size
    override fun onBindViewHolder(holder: ComputerViewHolder, position: Int) {
        val computer = computers[position]
        holder.button.text = computer.name
        holder.button.setOnClickListener { onClick(computer) }
        holder.deleteButton.setOnClickListener { onDelete(computer, position) }
    }
    class ComputerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button = view.findViewById(R.id.computerButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }
}