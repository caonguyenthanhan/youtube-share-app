import requests
import webbrowser
import time
import os
import json # Thêm thư viện json
import datetime # Thêm thư viện datetime

# Thay thế bằng địa chỉ IP Public của VM trên GCP của bạn
SERVER_IP = "34.136.11.166" # <<<< NHỚ THAY ĐỔI IP NÀY
GET_STATUS_URL = f"http://{SERVER_IP}:8080/get_status"

# Thư mục để lưu các trang cảnh báo (tùy chọn)
WARNINGS_DIR = "security_warnings"
if not os.path.exists(WARNINGS_DIR):
    os.makedirs(WARNINGS_DIR)

# File để lưu link đã mở gần nhất, giúp tránh mở lại cùng một link thành công
# (Cơ chế này có thể không cần thiết nữa nếu server xóa link sau khi gửi)
# LAST_SUCCESSFUL_LINK_FILE = "last_successful_link.txt"

def generate_warning_page(alert_info):
    """Tạo một file HTML cảnh báo và trả về đường dẫn của nó."""
    timestamp_str = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = os.path.join(WARNINGS_DIR, f"warning_{timestamp_str}.html")

    reason = alert_info.get("reason", "Không rõ lý do.")
    ip_address = alert_info.get("ip_address", "Không rõ IP.")
    attempted_device = alert_info.get("attempted_device", "Không rõ thiết bị.")
    alert_timestamp_utc_str = alert_info.get("timestamp", "Không rõ thời gian.")

    # Chuyển đổi UTC timestamp từ server sang local time (nếu có thể)
    try:
        alert_timestamp_utc = datetime.datetime.fromisoformat(alert_timestamp_utc_str.replace("Z", "+00:00"))
        alert_timestamp_local = alert_timestamp_utc.astimezone(datetime.datetime.now().astimezone().tzinfo)
        formatted_time = alert_timestamp_local.strftime('%Y-%m-%d %H:%M:%S %Z')
    except:
        formatted_time = alert_timestamp_utc_str # Giữ nguyên nếu không parse được

    html_content = f"""
    <!DOCTYPE html>
    <html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CẢNH BÁO BẢO MẬT</title>
        <style>
            body {{ font-family: Arial, sans-serif; margin: 20px; background-color: #fff3cd; color: #856404; }}
            .container {{ background-color: #ffeeba; border: 1px solid #ffdb58; padding: 20px; border-radius: 5px; }}
            h1 {{ color: #d9534f; }}
            p {{ line-height: 1.6; }}
            strong {{ color: #000; }}
        </style>
    </head>
    <body>
        <div class="container">
            <h1>⚠️ CẢNH BÁO BẢO MẬT!</h1>
            <p>Phát hiện một lần thử truy cập hoặc gửi link không được phép đến server của bạn.</p>
            <p><strong>Lý do:</strong> {reason}</p>
            <p><strong>Địa chỉ IP của người gửi:</strong> {ip_address}</p>
            <p><strong>Thiết bị đã thử (nếu có):</strong> {attempted_device}</p>
            <p><strong>Thời gian xảy ra (UTC từ server, chuyển đổi nếu có thể):</strong> {formatted_time}</p>
            <p>Vui lòng kiểm tra lại thiết bị và mã bảo mật nếu đây là bạn. Nếu không, đây có thể là một truy cập trái phép.</p>
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

print(f"Đang theo dõi trạng thái từ server: {GET_STATUS_URL}")
print("Nhấn Ctrl+C để dừng.")

processed_timestamps = set() # Để tránh xử lý cùng một cảnh báo/link nhiều lần nếu server không clear kịp

while True:
    try:
        response = requests.get(GET_STATUS_URL)
        response.raise_for_status()  # Báo lỗi nếu status code là 4xx hoặc 5xx

        data = response.json()

        status = data.get('status')
        timestamp = data.get('timestamp') # Lấy timestamp để tránh xử lý lặp

        if not status or status in ["no_attempt", "no_new_attempt"]:
            # print("Không có link hoặc cảnh báo mới.")
            time.sleep(5) # Đợi 5 giây rồi thử lại
            continue

        if timestamp and timestamp in processed_timestamps:
            # print(f"Đã xử lý thông báo lúc {timestamp}, bỏ qua.")
            time.sleep(5)
            continue

        if timestamp:
            processed_timestamps.add(timestamp)
            # Giới hạn kích thước của set để tránh dùng quá nhiều bộ nhớ
            if len(processed_timestamps) > 100:
                oldest_timestamp = min(processed_timestamps) # Cần cách lấy min phù hợp hơn nếu timestamp không sắp xếp được
                processed_timestamps.remove(oldest_timestamp)


        print(f"\n--- Nhận được trạng thái mới ({datetime.datetime.now()}) ---")
        print(json.dumps(data, indent=2, ensure_ascii=False)) # In ra để debug

        if status == "success":
            link_to_open = data.get('link')
            device_name = data.get('device', 'N/A')
            if link_to_open:
                print(f"Thành công! Mở link: {link_to_open} (từ thiết bị: {device_name})")
                webbrowser.open(link_to_open)
                # Cơ chế tránh mở lại link cũ có thể không cần thiết nếu server clear sau khi gửi
            else:
                print("Trạng thái thành công nhưng không có link được cung cấp.")

        elif status == "failure":
            print("Xác thực thất bại! Đang tạo trang cảnh báo...")
            warning_page_path = generate_warning_page(data)
            if warning_page_path:
                print(f"Trang cảnh báo đã được tạo: {warning_page_path}")
                webbrowser.open(f"file://{os.path.realpath(warning_page_path)}")
            else:
                print("Không thể tạo trang cảnh báo.")

        elif status == "error":
            print(f"Lỗi từ server: {data.get('message', 'Không rõ lỗi')}")

        else:
            print(f"Trạng thái không xác định từ server: {status}")

    except requests.exceptions.RequestException as e:
        print(f"Lỗi kết nối đến server: {e}")
    except json.JSONDecodeError:
        print("Lỗi: Phản hồi từ server không phải là JSON hợp lệ.")
    except Exception as e:
        print(f"Lỗi không xác định trong script máy tính: {e}")

    time.sleep(10) # Kiểm tra mỗi 10 giây