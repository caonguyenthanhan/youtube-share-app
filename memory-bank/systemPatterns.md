# Mẫu hệ thống

## Kiến trúc hệ thống
Ứng dụng YouTube Share Bridge được xây dựng trên Next.js với kiến trúc App Router, kết hợp giữa server components và client components.

### Cấu trúc thư mục chính
- `/app`: Chứa các trang và API routes của ứng dụng
  - `/api`: API endpoints
    - `/connect`: Xử lý kết nối giữa thiết bị
    - `/share`: Xử lý chia sẻ liên kết YouTube
  - `/connect/[code]`: Trang kết nối với mã cụ thể
  - `/unauthorized`: Trang hiển thị khi thiết bị không được phép
- `/components`: Các thành phần UI có thể tái sử dụng
  - `/ui`: Các thành phần UI từ shadcn/ui
- `/hooks`: Custom React hooks
- `/lib`: Tiện ích và dữ liệu

## Quyết định kỹ thuật chính

### 1. Sử dụng Next.js App Router
App Router cung cấp routing dựa trên hệ thống tệp, cho phép tạo các trang động và API routes một cách dễ dàng.

### 2. Sử dụng shadcn/ui cho giao diện người dùng
shadcn/ui cung cấp các thành phần UI có thể tùy chỉnh cao, được xây dựng trên Radix UI và Tailwind CSS.

### 3. Mô phỏng cơ sở dữ liệu trong bộ nhớ
Ứng dụng hiện sử dụng biến trong bộ nhớ để lưu trữ kết nối và liên kết chia sẻ, phù hợp cho mục đích demo.

### 4. Xác thực thiết bị đơn giản
Sử dụng file JSON để lưu trữ danh sách thiết bị được phép và mã bảo mật tương ứng.

## Mối quan hệ thành phần

### Luồng kết nối
1. Trang chủ (`app/page.tsx`) tạo mã kết nối ngẫu nhiên
2. Người dùng quét mã QR hoặc nhập mã trên thiết bị khác
3. Trang kết nối (`app/connect/[code]/page.tsx`) xử lý quá trình kết nối
4. API endpoint (`app/api/connect/route.ts`) lưu trữ thông tin kết nối

### Luồng chia sẻ
1. Thiết bị gửi yêu cầu chia sẻ đến API endpoint (`app/api/share/route.ts`)
2. API kiểm tra tính hợp lệ của thiết bị và liên kết
3. Nếu hợp lệ, liên kết được lưu trữ và gửi đến máy tính
4. Nếu không hợp lệ, người dùng được chuyển hướng đến trang không được phép (`app/unauthorized/page.tsx`)