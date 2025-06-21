import os
import sys
import winreg
import shutil
from pathlib import Path

def setup_autostart():
    # Get the path of the current script
    current_dir = os.path.dirname(os.path.abspath(__file__))
    app_path = os.path.join(current_dir, "ShareLink.exe")  # Use .exe instead of .py
    
    try:
        # Add to Windows startup registry
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, 
                            r"Software\Microsoft\Windows\CurrentVersion\Run", 
                            0, winreg.KEY_SET_VALUE)
        
        winreg.SetValueEx(key, "ShareLink", 0, winreg.REG_SZ, app_path)
        winreg.CloseKey(key)
        
        print("Autostart setup completed successfully!")
        print("ShareLink will now start automatically with Windows (hidden)")
        
    except Exception as e:
        print(f"Error setting up autostart: {e}")
        print("Please run this script as administrator")

if __name__ == "__main__":
    setup_autostart() 