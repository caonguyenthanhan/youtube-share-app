# YouTube Share Bridge

Hệ thống ứng dụng cho phép chia sẻ liên kết video YouTube từ điện thoại di động sang trình duyệt Cốc Cốc trên máy tính một cách nhanh chóng và dễ dàng. Bao gồm ứng dụng web trên máy tính và ứng dụng Android trên điện thoại.

## Tính năng

- Kết nối điện thoại và máy tính thông qua mã kết nối hoặc mã QR
- Chia sẻ liên kết YouTube từ điện thoại lên máy tính
- Xác thực thiết bị được phép chia sẻ thông qua mã bảo mật
- Giao diện người dùng thân thiện trên cả điện thoại và máy tính
- Hỗ trợ chế độ sáng/tối

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
- `/app/src/main/java/com/example/youtubesharebridge`: Mã nguồn chính của ứng dụng
  - `HomeActivity.kt`: Màn hình chính quản lý danh sách máy tính
  - `ShareReceiverActivity.kt`: Xử lý intent chia sẻ từ ứng dụng khác
  - `ChatHeadService.kt`: Dịch vụ hiển thị bong bóng chat nổi
  - `Computer.kt`: Model dữ liệu máy tính đích
- `/app/src/main/res`: Tài nguyên giao diện người dùng

## Cài đặt

### Ứng dụng Web

```bash
# Cài đặt dependencies
npm install

# Chạy server phát triển
npm run dev

# Xây dựng ứng dụng
npm run build

# Chạy ứng dụng đã xây dựng
npm start
```

### Ứng dụng Android

```bash
# Mở dự án trong Android Studio
cd YSB_mobile

# Hoặc build từ command line
./gradlew assembleDebug

# APK sẽ được tạo tại
# YSB_mobile/app/build/outputs/apk/debug/app-debug.apk
```