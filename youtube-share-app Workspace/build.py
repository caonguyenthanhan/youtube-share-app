import PyInstaller.__main__
import os

# Get the current directory
current_dir = os.path.dirname(os.path.abspath(__file__))

# Ensure icon.ico exists
icon_path = os.path.join(current_dir, 'icon.ico')
if not os.path.exists(icon_path):
    print("Error: icon.ico not found in the current directory!")
    print(f"Please place icon.ico in: {current_dir}")
    exit(1)

PyInstaller.__main__.run([
    'youtube_share_app.py',
    '--name=ShareLink',
    '--onefile',
    '--windowed',
    f'--icon={icon_path}',
    f'--add-data={icon_path};.',
    '--noconsole',
    '--clean'
]) 