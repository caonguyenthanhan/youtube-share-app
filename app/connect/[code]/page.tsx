"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Laptop, Smartphone } from "lucide-react"

export default function ConnectPage() {
  const params = useParams()
  const router = useRouter()
  const [isConnecting, setIsConnecting] = useState(true)
  const [isConnected, setIsConnected] = useState(false)
  const code = params.code as string

  useEffect(() => {
    if (!code) return

    const connectDevice = async () => {
      try {
        setIsConnecting(true)
        // Giả lập kết nối
        await new Promise((resolve) => setTimeout(resolve, 1500))
        setIsConnected(true)
        setIsConnecting(false)
      } catch (error) {
        console.error("Connection error:", error)
        setIsConnecting(false)
      }
    }

    connectDevice()
  }, [code])

  const handleContinue = () => {
    router.push("/")
  }

  return (
    <div className="container mx-auto px-4 py-16 flex items-center justify-center min-h-screen">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Kết nối thiết bị</CardTitle>
          <CardDescription>
            Đang kết nối với mã: <span className="font-mono font-bold">{code}</span>
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-center space-y-6">
          {isConnecting ? (
            <div className="flex flex-col items-center space-y-4">
              <div className="animate-pulse flex space-x-8 items-center">
                <Smartphone className="h-12 w-12 text-muted-foreground" />
                <div className="h-1 w-16 bg-muted-foreground rounded-full"></div>
                <Laptop className="h-12 w-12 text-muted-foreground" />
              </div>
              <p className="text-sm text-muted-foreground">Đang thiết lập kết nối...</p>
            </div>
          ) : isConnected ? (
            <div className="flex flex-col items-center space-y-4">
              <div className="flex space-x-8 items-center">
                <Smartphone className="h-12 w-12 text-green-500" />
                <div className="h-1 w-16 bg-green-500 rounded-full"></div>
                <Laptop className="h-12 w-12 text-green-500" />
              </div>
              <p className="text-green-500 font-medium">Kết nối thành công!</p>
              <Button onClick={handleContinue}>Tiếp tục</Button>
            </div>
          ) : (
            <div className="flex flex-col items-center space-y-4">
              <div className="flex space-x-8 items-center">
                <Smartphone className="h-12 w-12 text-red-500" />
                <div className="h-1 w-16 bg-red-500 rounded-full"></div>
                <Laptop className="h-12 w-12 text-red-500" />
              </div>
              <p className="text-red-500">Kết nối thất bại. Vui lòng thử lại.</p>
              <Button variant="outline" onClick={() => window.location.reload()}>
                Thử lại
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
