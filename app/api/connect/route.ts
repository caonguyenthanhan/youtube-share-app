import { type NextRequest, NextResponse } from "next/server"

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

export async function POST(request: NextRequest) {
  try {
    const { code, deviceType, ip } = await request.json()

    if (!code || !deviceType || !ip) {
      return NextResponse.json({ error: "Missing required fields" }, { status: 400 })
    }

    if (!connections[code]) {
      connections[code] = {
        code,
        computerIp: deviceType === "computer" ? ip : "",
        phoneIp: deviceType === "phone" ? ip : "",
        links: [],
      }
    } else {
      if (deviceType === "computer") {
        connections[code].computerIp = ip
      } else {
        connections[code].phoneIp = ip
      }
    }

    return NextResponse.json({ success: true, connection: connections[code] })
  } catch (error) {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 })
  }
}

export async function GET(request: NextRequest) {
  const url = new URL(request.url)
  const code = url.searchParams.get("code")

  if (!code || !connections[code]) {
    return NextResponse.json({ error: "Connection not found" }, { status: 404 })
  }

  return NextResponse.json({ connection: connections[code] })
}
