# YouTube Share Bridge

Hệ thống ứng dụng cho phép chia sẻ liên kết video YouTube từ điện thoại di động sang trình duyệt Cốc Cốc trên máy tính một cách nhanh chóng và dễ dàng. Bao gồm ứng dụng web trên máy tính, ứng dụng Android trên điện thoại và ứng dụng desktop ShareLink.

## Tính năng

- Kết nối điện thoại và máy tính thông qua mã kết nối hoặc mã QR
- Chia sẻ liên kết YouTube từ điện thoại lên máy tính
- Xác thực thiết bị được phép chia sẻ thông qua mã bảo mật
- Giao diện người dùng thân thiện trên cả điện thoại và máy tính
- Hỗ trợ chế độ sáng/tối
- Tự động khởi động cùng Windows (ứng dụng ShareLink)
- Hiển thị cảnh báo bảo mật khi có yêu cầu chia sẻ không hợp lệ

## Công nghệ sử dụng

### Ứng dụng Web
- Next.js 15.2.4
- React 19
- TypeScript
- Tailwind CSS
- shadcn/ui components

### Ứng dụng Android
- Kotlin
- Material Design
- OkHttp cho kết nối HTTP
- SharedPreferences cho lưu trữ cài đặt
- Floating Chat Head cho chia sẻ nhanh

### Ứng dụng Desktop ShareLink
- Python 3.8+
- PyQt5 cho giao diện người dùng
- Requests cho kết nối HTTP
- Windows Registry cho tự động khởi động

## Cấu trúc dự án

### Ứng dụng Web
- `/app`: Chứa các trang và API routes của ứng dụng
  - `/api`: API endpoints
    - `/connect`: Xử lý kết nối giữa thiết bị
    - `/share`: Xử lý chia sẻ liên kết YouTube
  - `/connect/[code]`: Trang kết nối với mã cụ thể
  - `/unauthorized`: Trang hiển thị khi thiết bị không được phép
- `/components`: Các thành phần UI có thể tái sử dụng
- `/hooks`: Custom React hooks
- `/lib`: Tiện ích và dữ liệu

### Ứng dụng Android
- `/YSB_mobile/app/src/main/java/com/example/youtubesharebridge`: Mã nguồn chính của ứng dụng
  - `HomeActivity.kt`: Màn hình chính quản lý danh sách máy tính
  - `ShareReceiverActivity.kt`: Xử lý intent chia sẻ từ ứng dụng khác
  - `ChatActivity.kt`: Giao diện chat với máy tính
  - `SettingsActivity.kt`: Cài đặt ứng dụng
- `/YSB_mobile/app/src/main/res`: Tài nguyên giao diện người dùng

### Ứng dụng Desktop ShareLink
- `/youtube-share-app Workspace`: Thư mục chứa mã nguồn ứng dụng desktop
  - `youtube_share_app.py`: Mã nguồn chính của ứng dụng
  - `setup_autostart.py`: Script cài đặt tự động khởi động
  - `build.py`: Script đóng gói ứng dụng
  - `dist/ShareLink.exe`: Tệp thực thi của ứng dụng

## Cài đặt

### Ứng dụng Web
1. Clone repository
2. Cài đặt dependencies: `npm install`
3. Chạy ứng dụng: `npm run dev`

### Ứng dụng Android
1. Mở thư mục `/YSB_mobile` trong Android Studio
2. Sync Gradle và build ứng dụng
3. Cài đặt trên thiết bị Android hoặc máy ảo

### Ứng dụng Desktop ShareLink
1. Cài đặt Python 3.8 hoặc cao hơn
2. Cài đặt các thư viện cần thiết: `pip install -r requirements.txt`
3. Chạy script cài đặt tự động khởi động: `python setup_autostart.py`
4. Hoặc sử dụng tệp thực thi `dist/ShareLink.exe`

## Sử dụng

### Chia sẻ liên kết từ điện thoại sang máy tính
1. Mở ứng dụng web trên máy tính và nhận mã kết nối
2. Mở ứng dụng Android trên điện thoại và nhập mã kết nối
3. Tìm video YouTube trên điện thoại và chia sẻ đến ứng dụng
4. Video sẽ tự động mở trên máy tính

### Ứng dụng Desktop ShareLink
- Ứng dụng sẽ tự động chạy khi khởi động Windows
- Biểu tượng ứng dụng sẽ xuất hiện trong khay hệ thống
- Click chuột phải vào biểu tượng để xem menu
- Chọn "Cấu hình" để thay đổi ID máy tính và IP máy chủ
- Chọn "Exit" để thoát ứng dụng

## Bảo mật

- Ứng dụng sử dụng mã kết nối ngẫu nhiên để xác thực thiết bị
- Chỉ thiết bị đã đăng ký mới có thể chia sẻ liên kết
- Ứng dụng desktop hiển thị cảnh báo bảo mật khi có yêu cầu chia sẻ không hợp lệ

## Đóng góp

Mọi đóng góp đều được hoan nghênh! Vui lòng tạo issue hoặc pull request để đóng góp vào dự án.