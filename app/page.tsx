"use client"

import { useState, useEffect } from "react"
import { QRCodeSVG } from "qrcode.react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Smartphone, Laptop, Share2, Youtube } from "lucide-react"

export default function Home() {
  const [serverUrl, setServerUrl] = useState("http://192.168.1.81:4000")
  const [connectionCode, setConnectionCode] = useState("")
  const [isConnected, setIsConnected] = useState(false)
  const [sharedLinks, setSharedLinks] = useState<string[]>([])

  // Generate a random connection code when the component mounts
  useEffect(() => {
    const code = Math.random().toString(36).substring(2, 8).toUpperCase()
    setConnectionCode(code)
  }, [])

  // Simulate receiving a shared link
  const handleShareLink = () => {
    const demoYoutubeLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    setSharedLinks([demoYoutubeLink, ...sharedLinks])
  }

  // Simulate sending a link to the computer
  const sendToComputer = (link: string) => {
    alert(`Link "${link}" sent to Cốc Cốc browser on your computer!`)
  }

  return (
    <main className="container mx-auto px-4 py-8">
      <div className="flex flex-col items-center justify-center space-y-6">
        <div className="flex items-center space-x-2">
          <Youtube className="h-8 w-8 text-red-500" />
          <h1 className="text-2xl font-bold">YouTube Share Bridge</h1>
        </div>

        <Tabs defaultValue="phone" className="w-full max-w-md">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="phone">
              <div className="flex items-center space-x-2">
                <Smartphone className="h-4 w-4" />
                <span>Điện thoại</span>
              </div>
            </TabsTrigger>
            <TabsTrigger value="computer">
              <div className="flex items-center space-x-2">
                <Laptop className="h-4 w-4" />
                <span>Máy tính</span>
              </div>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="phone">
            <Card>
              <CardHeader>
                <CardTitle>Chia sẻ từ điện thoại</CardTitle>
                <CardDescription>
                  Cài đặt ứng dụng này trên điện thoại Android của bạn để chia sẻ video YouTube.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-col items-center space-y-2">
                  <p className="text-sm text-muted-foreground">Mã kết nối của bạn:</p>
                  <div className="text-2xl font-bold tracking-wider">{connectionCode}</div>
                </div>

                <div className="flex flex-col items-center space-y-2">
                  <p className="text-sm text-muted-foreground">Trạng thái:</p>
                  <div className={`text-sm font-medium ${isConnected ? "text-green-500" : "text-amber-500"}`}>
                    {isConnected ? "Đã kết nối với máy tính" : "Chưa kết nối"}
                  </div>
                </div>

                <Button className="w-full" onClick={() => setIsConnected(!isConnected)}>
                  {isConnected ? "Ngắt kết nối" : "Kết nối với máy tính"}
                </Button>

                <div className="border-t pt-4">
                  <Button variant="outline" className="w-full flex items-center space-x-2" onClick={handleShareLink}>
                    <Share2 className="h-4 w-4" />
                    <span>Mô phỏng chia sẻ từ YouTube</span>
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="computer">
            <Card>
              <CardHeader>
                <CardTitle>Nhận trên máy tính</CardTitle>
                <CardDescription>
                  Mở trang web này trên máy tính để nhận video được chia sẻ từ điện thoại.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-col items-center space-y-4">
                  <p className="text-sm text-muted-foreground">Quét mã QR hoặc nhập mã kết nối:</p>
                  <div className="bg-white p-2 rounded-lg">
                    <QRCodeSVG value={`${serverUrl}/connect/${connectionCode}`} size={150} />
                  </div>
                </div>

                <div className="flex space-x-2">
                  <Input
                    placeholder="Nhập mã kết nối"
                    value={connectionCode}
                    onChange={(e) => setConnectionCode(e.target.value.toUpperCase())}
                    className="text-center font-mono"
                    maxLength={6}
                  />
                  <Button onClick={() => setIsConnected(!isConnected)}>
                    {isConnected ? "Ngắt kết nối" : "Kết nối"}
                  </Button>
                </div>
              </CardContent>

              <CardFooter className="flex flex-col">
                <div className="w-full">
                  <h3 className="text-sm font-medium mb-2">Video đã chia sẻ:</h3>
                  {sharedLinks.length > 0 ? (
                    <div className="space-y-2">
                      {sharedLinks.map((link, index) => (
                        <div key={index} className="flex items-center justify-between p-2 bg-muted rounded-md">
                          <div className="truncate flex-1 text-sm">{link}</div>
                          <Button variant="ghost" size="sm" onClick={() => sendToComputer(link)}>
                            Mở trong Cốc Cốc
                          </Button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground">Chưa có video nào được chia sẻ.</p>
                  )}
                </div>
              </CardFooter>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </main>
  )
}
