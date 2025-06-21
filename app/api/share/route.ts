import { type NextRequest, NextResponse } from "next/server"
import fs from 'fs';
import path from 'path';

// Giả lập cơ sở dữ liệu kết nối
const connections: Record<
  string,
  {
    code: string
    computerIp: string
    phoneIp: string
    links: string[]
  }
> = {}

// Đọc file thiết bị đã đăng ký
const getRegisteredDevices = () => {
  const filePath = path.join(process.cwd(), 'lib', 'registered-devices.json');
  const fileContents = fs.readFileSync(filePath, 'utf8');
  return JSON.parse(fileContents).devices;
};

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { link, device, security_code } = body;
    const clientIP = request.headers.get('x-forwarded-for') || 'Unknown IP';

    // Kiểm tra các trường bắt buộc
    if (!link || !device || !security_code) {
      return NextResponse.json({
        success: false,
        message: 'Missing required fields',
        clientIP
      }, { status: 400 });
    }

    // Kiểm tra định dạng link YouTube
    if (!link.includes('youtube.com') && !link.includes('youtu.be')) {
      return NextResponse.json({
        success: false,
        message: 'Invalid YouTube link',
        clientIP
      }, { status: 400 });
    }

    // Kiểm tra thiết bị và mã bảo mật
    const registeredDevices = getRegisteredDevices();
    const isDeviceRegistered = registeredDevices.some(
      (d: any) => d.name === device && d.security_code === security_code
    );

    if (!isDeviceRegistered) {
      return NextResponse.json({
        success: false,
        message: 'Unauthorized device or invalid security code',
        clientIP
      }, { status: 401 });
    }

    // Nếu mọi thứ hợp lệ, trả về link YouTube
    return NextResponse.json({
      success: true,
      message: 'Link shared successfully',
      link
    });

  } catch (error) {
    console.error('Error processing request:', error);
    return NextResponse.json({
      success: false,
      message: 'Internal server error'
    }, { status: 500 });
  }
}

export async function GET(request: NextRequest) {
  const url = new URL(request.url)
  const code = url.searchParams.get("code")

  if (!code || !connections[code]) {
    return NextResponse.json({ error: "Connection not found" }, { status: 404 })
  }

  return NextResponse.json({ links: connections[code].links })
}
