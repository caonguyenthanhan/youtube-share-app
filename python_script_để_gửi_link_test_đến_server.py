# -*- coding: utf-8 -*-
"""Python Script để Gửi Link Test đến Server

Automatically generated by Colab.

Original file is located at
    https://colab.research.google.com/drive/1rvUicB54kxYSPALl0oarCQEIN0qXxf_G
"""

!pip install requests

import requests
import json

# --- CẤU HÌNH GỬI TEST ---
# Thay đổi các giá trị này để khớp với thiết lập của bạn

# 1. IP Public của server trên GCP (và cổng nếu bạn dùng cổng khác 80)
# Ví dụ IP bạn đã dùng trước đây là '34.136.11.166' và cổng '8080'
SERVER_URL = "http://34.61.180.50"

# 2. Link bất kỳ bạn muốn gửi để test
LINK_TO_SEND = "https://www.youtube.com/watch?v=MM1ZMYOvUzA"

# 3. Thông tin thiết bị di động (phải khớp với file authorized_devices.json trên server)
# Ví dụ bạn đã cấu hình thiết bị "MyPhone" với mã "2003_09_29"
DEVICE_NAME = ""
SECURITY_CODE = ""

# 4. Mã máy tính đích (phải khớp với mã bạn đã đặt cho script fetch_and_open.py trên máy nhận)
TARGET_COMPUTER_ID = "" # Ví dụ bạn muốn gửi đến máy B

# ----------------------------

def send_test_link():
    """
    Hàm này tạo và gửi một yêu cầu POST chứa link đến server.
    """
    # Xây dựng URL đầy đủ cho endpoint /set_link
    endpoint_url = f"{SERVER_URL}/set_link"

    # Tạo payload (dữ liệu) dưới dạng một dictionary Python
    payload = {
        "link": LINK_TO_SEND,
        "device": DEVICE_NAME,
        "security_code": SECURITY_CODE,
        "target_computer_id": TARGET_COMPUTER_ID
    }

    # In ra thông tin sắp gửi để kiểm tra
    print("--- Đang chuẩn bị gửi dữ liệu sau đến server ---")
    print(f"URL đích: {endpoint_url}")
    print("Dữ liệu (Payload):")
    # Dùng json.dumps để in ra dạng chuỗi JSON đẹp mắt
    print(json.dumps(payload, indent=4, ensure_ascii=False))
    print("-------------------------------------------------")

    try:
        # Gửi yêu cầu POST với header chỉ định nội dung là JSON
        # timeout=10 để yêu cầu sẽ bị hủy nếu server không phản hồi trong 10 giây
        response = requests.post(
            endpoint_url,
            json=payload,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )

        # In ra kết quả phản hồi từ server
        print("\n--- Kết quả phản hồi từ Server ---")
        print(f"Mã trạng thái (Status Code): {response.status_code}")

        # Thử giải mã JSON từ phản hồi và in ra
        try:
            print("Nội dung phản hồi (JSON):")
            print(json.dumps(response.json(), indent=4, ensure_ascii=False))
        except json.JSONDecodeError:
            print("Nội dung phản hồi (không phải JSON):")
            print(response.text)

    except requests.exceptions.RequestException as e:
        # Xử lý các lỗi liên quan đến kết nối mạng (ví dụ: sai IP, server không chạy, không có mạng)
        print("\n--- ĐÃ XẢY RA LỖI KẾT NỐI ---")
        print(f"Không thể kết nối đến server. Vui lòng kiểm tra lại:")
        print(f"- Địa chỉ server '{SERVER_URL}' có đúng không?")
        print(f"- Server trên GCP có đang chạy không?")
        print(f"- Firewall trên GCP đã mở cổng 8080 chưa?")
        print(f"- Máy tính của bạn có kết nối internet không?")
        print(f"Chi tiết lỗi: {e}")

if __name__ == "__main__":
    send_test_link()