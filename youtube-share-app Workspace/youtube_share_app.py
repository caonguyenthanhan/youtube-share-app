import sys
import os
import json
import time
import requests
import webbrowser
import datetime
from PyQt5.QtWidgets import (QApplication, QSystemTrayIcon, QMenu, QAction, 
                           QMessageBox, QWidget, QPushButton, QVBoxLayout, QLabel,
                           QDialog, QLineEdit, QFormLayout)
from PyQt5.QtGui import QIcon
from PyQt5.QtCore import QTimer, Qt

class ConfigDialog(QDialog):
    def __init__(self, current_computer_id="", current_server_ip="34.61.180.50:8080", parent=None):
        super().__init__(parent)
        self.setWindowTitle('Cấu hình Share Link')
        self.setModal(True)
        
        # Create form layout
        layout = QFormLayout()
        
        # Add computer ID input
        self.computer_id_input = QLineEdit()
        self.computer_id_input.setText(current_computer_id)
        self.computer_id_input.setPlaceholderText("Ví dụ: MAY_TINH_B")
        layout.addRow("Mã máy tính:", self.computer_id_input)
        
        # Add server IP input
        self.server_ip_input = QLineEdit()
        self.server_ip_input.setText(current_server_ip)
        self.server_ip_input.setPlaceholderText("Ví dụ: 34.136.11.166")
        layout.addRow("IP Server:", self.server_ip_input)
        
        # Add buttons
        button_layout = QVBoxLayout()
        ok_button = QPushButton("OK")
        ok_button.clicked.connect(self.accept)
        button_layout.addWidget(ok_button)
        
        layout.addRow("", button_layout)
        self.setLayout(layout)

class ShareLinkApp(QWidget):
    def __init__(self):
        super().__init__()
        
        # Đường dẫn đến file cấu hình
        self.CONFIG_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'config.ini')
        
        # Kiểm tra xem ứng dụng có được khởi động cùng Windows không
        is_startup = self.is_startup_launch()
        
        # Kiểm tra và tạo file cấu hình nếu chưa tồn tại
        if os.path.exists(self.CONFIG_FILE):
            import configparser
            config = configparser.ConfigParser()
            config.read(self.CONFIG_FILE)
            
            try:
                self.MY_OWN_COMPUTER_ID = config['DEFAULT']['computer_id']
                self.SERVER_IP = config['DEFAULT']['server_ip']
            except (KeyError, configparser.NoSectionError):
                # Nếu file cấu hình không hợp lệ
                if is_startup:
                    # Nếu khởi động cùng Windows, sử dụng giá trị mặc định
                    self.MY_OWN_COMPUTER_ID = "DEFAULT_PC"
                    self.SERVER_IP = "34.61.180.50:8080"
                    # Lưu cấu hình mặc định
                    self.save_config()
                else:
                    # Nếu không phải khởi động cùng Windows, hiển thị hộp thoại cấu hình
                    self.show_initial_config_dialog()
        else:
            # Nếu file cấu hình không tồn tại
            if is_startup:
                # Nếu khởi động cùng Windows, sử dụng giá trị mặc định
                self.MY_OWN_COMPUTER_ID = "DEFAULT_PC"
                self.SERVER_IP = "34.61.180.50:8080"
                # Lưu cấu hình mặc định
                self.save_config()
            else:
                # Nếu không phải khởi động cùng Windows, hiển thị hộp thoại cấu hình
                self.show_initial_config_dialog()
            
        self.initUI()
        self.setup_tray()
        self.setup_timer()
        
        # Configuration
        # Đảm bảo URL bao gồm cổng 8080
        if ":" not in self.SERVER_IP:
            self.SERVER_IP = f"{self.SERVER_IP}:8080"
        self.GET_STATUS_URL = f"http://{self.SERVER_IP}/get_status"
        self.WARNINGS_DIR = "security_warnings"
        self.processed_timestamps = set()
        
        # Create warnings directory if it doesn't exist
        if not os.path.exists(self.WARNINGS_DIR):
            os.makedirs(self.WARNINGS_DIR)
    
    def save_config(self):
        """Lưu cấu hình hiện tại vào file."""
        import configparser
        config = configparser.ConfigParser()
        config['DEFAULT'] = {
            'computer_id': self.MY_OWN_COMPUTER_ID,
            'server_ip': self.SERVER_IP
        }
        with open(self.CONFIG_FILE, 'w') as configfile:
            config.write(configfile)
    
    def show_initial_config_dialog(self):
        """Hiển thị hộp thoại cấu hình ban đầu."""
        dialog = ConfigDialog("", "34.61.180.50:8080", self)
        if dialog.exec_() == QDialog.Accepted:
            self.MY_OWN_COMPUTER_ID = dialog.computer_id_input.text().strip()
            self.SERVER_IP = dialog.server_ip_input.text().strip()
            
            if not self.MY_OWN_COMPUTER_ID or not self.SERVER_IP:
                QMessageBox.critical(self, "Lỗi", "Vui lòng nhập đầy đủ thông tin cấu hình!")
                sys.exit(1)
                
            # Lưu cấu hình
            self.save_config()
        else:
            sys.exit(0)

    def initUI(self):
        self.setWindowTitle('Share Link')
        self.setGeometry(300, 300, 300, 200)
        
        # Set application icon
        icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icon.ico')
        app_icon = QIcon(icon_path)
        self.setWindowIcon(app_icon)
        QApplication.setWindowIcon(app_icon)
        
        # Create main window layout
        layout = QVBoxLayout()
        
        # Add status label
        self.status_label = QLabel(f"Share Link đang chạy...\nMáy tính: {self.MY_OWN_COMPUTER_ID}\nServer: {self.SERVER_IP}")
        self.status_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.status_label)
        
        # Add config button
        config_btn = QPushButton("Thay đổi cấu hình")
        config_btn.clicked.connect(self.show_config_dialog)
        layout.addWidget(config_btn)
        
        # Add minimize button
        minimize_btn = QPushButton("Minimize to Tray")
        minimize_btn.clicked.connect(self.hide)
        layout.addWidget(minimize_btn)
        
        # Add exit button
        exit_btn = QPushButton("Exit")
        exit_btn.clicked.connect(self.quit_application)
        layout.addWidget(exit_btn)
        
        self.setLayout(layout)
        
        # Set window flags to keep it in taskbar
        self.setWindowFlags(Qt.Window | Qt.WindowStaysOnTopHint)
        
        # Hide window initially if started with Windows
        if self.is_startup_launch():
            self.hide()
        else:
            self.show()

    def is_startup_launch(self):
        """Check if the application was launched at Windows startup"""
        try:
            import winreg
            key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, 
                                r"Software\Microsoft\Windows\CurrentVersion\Run", 
                                0, winreg.KEY_READ)
            try:
                value, _ = winreg.QueryValueEx(key, "ShareLink")
                return True
            except WindowsError:
                return False
            finally:
                winreg.CloseKey(key)
        except:
            return False

    def setup_tray(self):
        # Create system tray icon
        self.tray_icon = QSystemTrayIcon(self)
        icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icon.ico')
        app_icon = QIcon(icon_path)
        self.tray_icon.setIcon(app_icon)
        
        # Create tray menu
        tray_menu = QMenu()
        
        # Add actions
        show_action = QAction("Show", self)
        quit_action = QAction("Exit", self)
        
        show_action.triggered.connect(self.show)
        quit_action.triggered.connect(self.quit_application)
        
        tray_menu.addAction(show_action)
        tray_menu.addAction(quit_action)
        
        self.tray_icon.setContextMenu(tray_menu)
        self.tray_icon.show()
        
        # Connect double click to show window
        self.tray_icon.activated.connect(self.tray_icon_activated)

    def tray_icon_activated(self, reason):
        if reason == QSystemTrayIcon.DoubleClick:
            self.show()
            self.activateWindow()

    def setup_timer(self):
        # Create timer for checking server status
        self.timer = QTimer()
        self.timer.timeout.connect(self.check_server_status)
        self.timer.start(5000)  # Check every 5 seconds

    def check_server_status(self):
        try:
            response = requests.get(self.GET_STATUS_URL)
            response.raise_for_status()
            data = response.json()
            
            status = data.get('status')
            timestamp = data.get('timestamp')
            
            if not status or status in ["no_attempt", "no_new_attempt"]:
                return
                
            if timestamp and timestamp in self.processed_timestamps:
                return
                
            if timestamp:
                self.processed_timestamps.add(timestamp)
                if len(self.processed_timestamps) > 100:
                    oldest_timestamp = min(self.processed_timestamps)
                    self.processed_timestamps.remove(oldest_timestamp)
            
            if status == "success":
                link_to_open = data.get('link')
                received_target_computer_id = data.get('target_computer_id')
                device_name = data.get('device', 'N/A')
                
                if link_to_open:
                    if received_target_computer_id == self.MY_OWN_COMPUTER_ID:
                        self.tray_icon.showMessage(
                            "Share Link",
                            f"Opening link from device: {device_name}",
                            QSystemTrayIcon.Information,
                            2000
                        )
                        webbrowser.open(link_to_open)
                    elif received_target_computer_id:
                        self.tray_icon.showMessage(
                            "Share Link",
                            f"Link intended for {received_target_computer_id}, but this is {self.MY_OWN_COMPUTER_ID}. Ignoring.",
                            QSystemTrayIcon.Information,
                            2000
                        )
                    else:
                        self.tray_icon.showMessage(
                            "Share Link",
                            "Received link has no target computer information. Check server configuration.",
                            QSystemTrayIcon.Warning,
                            2000
                        )
                    
            elif status == "failure":
                warning_page_path = self.generate_warning_page(data)
                if warning_page_path:
                    self.tray_icon.showMessage(
                        "Security Warning",
                        "A security warning has been generated",
                        QSystemTrayIcon.Warning,
                        2000
                    )
                    webbrowser.open(f"file://{os.path.realpath(warning_page_path)}")
                    
        except Exception as e:
            self.tray_icon.showMessage(
                "Error",
                f"Connection error: {str(e)}",
                QSystemTrayIcon.Critical,
                2000
            )

    def generate_warning_page(self, alert_info):
        """Tạo một file HTML cảnh báo và trả về đường dẫn của nó."""
        timestamp_str = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = os.path.join(self.WARNINGS_DIR, f"warning_{timestamp_str}.html")

        reason = alert_info.get("reason", "Không rõ lý do.")
        ip_address = alert_info.get("ip_address", "Không rõ IP.")
        attempted_device = alert_info.get("attempted_device", "Không rõ thiết bị.")
        alert_timestamp_utc_str = alert_info.get("timestamp", "Không rõ thời gian.")
        target_computer_id_attempted = alert_info.get("target_computer_id_attempted", "Không rõ đích.")

        try:
            alert_timestamp_utc = datetime.datetime.fromisoformat(alert_timestamp_utc_str.replace("Z", "+00:00"))
            alert_timestamp_local = alert_timestamp_utc.astimezone(datetime.datetime.now().astimezone().tzinfo)
            formatted_time = alert_timestamp_local.strftime('%Y-%m-%d %H:%M:%S %Z')
        except:
            formatted_time = alert_timestamp_utc_str

        html_content = f"""
        <!DOCTYPE html>
        <html lang="vi">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>CẢNH BÁO BẢO MẬT</title>
            <style>
                body {{
                    font-family: 'Segoe UI', Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f8f9fa;
                    color: #333;
                    line-height: 1.6;
                }}
                .container {{
                    max-width: 800px;
                    margin: 40px auto;
                    padding: 30px;
                    background-color: #fff;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }}
                .warning-header {{
                    background-color: #dc3545;
                    color: white;
                    padding: 20px;
                    border-radius: 8px;
                    margin-bottom: 20px;
                    display: flex;
                    align-items: center;
                    gap: 15px;
                }}
                .warning-icon {{
                    font-size: 2.5em;
                }}
                .warning-title {{
                    margin: 0;
                    font-size: 1.8em;
                }}
                .warning-content {{
                    background-color: #fff3cd;
                    border: 1px solid #ffeeba;
                    padding: 20px;
                    border-radius: 8px;
                    margin-bottom: 20px;
                }}
                .info-item {{
                    margin-bottom: 15px;
                    padding: 10px;
                    background-color: #f8f9fa;
                    border-radius: 5px;
                }}
                .info-label {{
                    font-weight: bold;
                    color: #495057;
                    display: block;
                    margin-bottom: 5px;
                }}
                .info-value {{
                    color: #212529;
                }}
                .footer {{
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #dee2e6;
                    color: #6c757d;
                    font-size: 0.9em;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="warning-header">
                    <span class="warning-icon">⚠️</span>
                    <h1 class="warning-title">CẢNH BÁO BẢO MẬT</h1>
                </div>
                
                <div class="warning-content">
                    <p>Phát hiện một lần thử truy cập hoặc gửi link không được phép đến server của bạn.</p>
                </div>

                <div class="info-item">
                    <span class="info-label">Lý do:</span>
                    <span class="info-value">{reason}</span>
                </div>

                <div class="info-item">
                    <span class="info-label">Địa chỉ IP của người gửi:</span>
                    <span class="info-value">{ip_address}</span>
                </div>

                <div class="info-item">
                    <span class="info-label">Thiết bị đã thử:</span>
                    <span class="info-value">{attempted_device}</span>
                </div>

                <div class="info-item">
                    <span class="info-label">Máy tính đích đã thử:</span>
                    <span class="info-value">{target_computer_id_attempted}</span>
                </div>

                <div class="info-item">
                    <span class="info-label">Thời gian xảy ra:</span>
                    <span class="info-value">{formatted_time}</span>
                </div>

                <div class="footer">
                    <p>Vui lòng kiểm tra lại thiết bị và mã bảo mật nếu đây là bạn. Nếu không, đây có thể là một truy cập trái phép.</p>
                    <p>Nếu bạn cần hỗ trợ, vui lòng liên hệ với quản trị viên hệ thống.</p>
                </div>
            </div>
        </body>
        </html>
        """
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(html_content)
            return filename
        except Exception as e:
            print(f"Lỗi khi tạo file cảnh báo: {e}")
            return None

    def quit_application(self):
        reply = QMessageBox.question(
            self, 'Confirm Exit',
            "Are you sure you want to exit?",
            QMessageBox.Yes | QMessageBox.No,
            QMessageBox.No
        )
        
        if reply == QMessageBox.Yes:
            self.tray_icon.hide()
            QApplication.quit()

    def show_config_dialog(self):
        """Hiển thị hộp thoại cấu hình để thay đổi thông tin."""
        dialog = ConfigDialog(self.MY_OWN_COMPUTER_ID, self.SERVER_IP, self)
        if dialog.exec_():
            new_computer_id = dialog.computer_id_input.text().strip()
            new_server_ip = dialog.server_ip_input.text().strip()
            
            # Kiểm tra xem thông tin có thay đổi không
            if new_computer_id != self.MY_OWN_COMPUTER_ID or new_server_ip != self.SERVER_IP:
                # Cập nhật thông tin
                self.MY_OWN_COMPUTER_ID = new_computer_id
                self.SERVER_IP = new_server_ip
                
                # Đảm bảo URL bao gồm cổng 8080
                if ":" not in self.SERVER_IP:
                    self.SERVER_IP = f"{self.SERVER_IP}:8080"
                
                # Cập nhật URL
                self.GET_STATUS_URL = f"http://{self.SERVER_IP}/get_status"
                
                # Lưu thông tin mới vào file cấu hình
                self.save_config()
                
                # Cập nhật nhãn trạng thái
                self.status_label.setText(f"Share Link đang chạy...\nMáy tính: {self.MY_OWN_COMPUTER_ID}\nServer: {self.SERVER_IP}")
                
                # Hiển thị thông báo
                self.tray_icon.showMessage(
                    "Cấu hình đã được cập nhật",
                    f"ID máy tính: {self.MY_OWN_COMPUTER_ID}\nĐịa chỉ máy chủ: {self.SERVER_IP}",
                    QSystemTrayIcon.Information,
                    2000
                )
    
    def closeEvent(self, event):
        event.ignore()
        self.hide()
        self.tray_icon.showMessage(
            "Share Link",
            "Application minimized to tray. Double click the tray icon to show again.",
            QSystemTrayIcon.Information,
            2000
        )

def main():
    app = QApplication(sys.argv)
    
    # Set application icon globally
    icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icon.ico')
    app_icon = QIcon(icon_path)
    app.setWindowIcon(app_icon)
    
    ex = ShareLinkApp()
    sys.exit(app.exec_())

if __name__ == '__main__':
    main()