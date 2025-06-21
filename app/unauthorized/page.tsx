'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertTriangle } from "lucide-react";

export default function UnauthorizedPage() {
  const [clientIP, setClientIP] = useState<string>('');

  useEffect(() => {
    // Lấy IP từ URL parameters
    const params = new URLSearchParams(window.location.search);
    const ip = params.get('ip') || 'Unknown IP';
    setClientIP(ip);
  }, []);

  return (
    <main className="container mx-auto px-4 py-8">
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <Card className="w-full max-w-md">
          <CardHeader>
            <div className="flex items-center space-x-2 text-red-500">
              <AlertTriangle className="h-6 w-6" />
              <CardTitle>Unauthorized Access</CardTitle>
            </div>
            <CardDescription>
              This device is not authorized to share YouTube links.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">
                Device IP: {clientIP}
              </p>
              <p className="text-sm">
                Please contact the administrator if you believe this is a mistake.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  );
} 