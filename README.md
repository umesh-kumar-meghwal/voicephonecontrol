# VoicePhoneControl

Architecture:
Laptop microphone -> Python client -> FastAPI server -> Android app

This starter project uses predefined commands only. It does NOT expose ADB over the internet.

## Commands
- OPEN_APP (currently example payload)
- BACK
- HOME
- VOLUME_UP
- VOLUME_DOWN

## Run server
cd server
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000

Set the same API token in server/.env and laptop/config.py.

## Laptop
cd laptop
pip install -r requirements.txt
python main.py

## Android
Open `android/VoiceControl` in Android Studio and run it on your phone.
The Android implementation is a starter: extend CommandHandler with Android APIs/Accessibility Service only for commands you explicitly want to support.
