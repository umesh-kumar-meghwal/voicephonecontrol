from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
import os
import secrets
import time

app = FastAPI(title="Voice Phone Control Server")

API_TOKEN = os.getenv("API_TOKEN", "change-me")

# =========================================================
# TEMP MULTI-DEVICE STORAGE
# =========================================================
# IMPORTANT:
# Ye abhi in-memory storage hai.
# Vercel/serverless restart hone par data reset ho sakta hai.
# Production ke liye next phase mein Supabase/DB lagayenge.
# =========================================================

devices = {}

# device_id -> command
pending_commands = {}


# =========================================================
# MODELS
# =========================================================

class DeviceRegister(BaseModel):
    device_name: str = Field(default="Android Phone")
    device_model: str = Field(default="Unknown")
    android_id: Optional[str] = None


class Command(BaseModel):
    device_id: str
    command: str
    payload: dict = {}


# =========================================================
# ALLOWED COMMANDS
# =========================================================

ALLOWED = {
    "OPEN_APP",
    "ENTER",
    "BACK",
    "HOME",
    "VOLUME_UP",
    "VOLUME_DOWN",
    "TAKE_SCREENSHOT",
    "LIVE_SCREEN",
    "NOTIFICATION_STATUS",
    "PHONE_STATUS",
}


# =========================================================
# AUTH
# =========================================================

def check_api_token(authorization: str | None):

    if authorization != f"Bearer {API_TOKEN}":
        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )


# =========================================================
# DEVICE ID GENERATOR
# =========================================================

def generate_device_id():

    while True:

        device_id = (
            "VPC-"
            + secrets.token_hex(4).upper()
        )

        if device_id not in devices:
            return device_id


# =========================================================
# HEALTH
# =========================================================

@app.get("/health")
def health():

    return {
        "ok": True,
        "service": "Voice Phone Control",
        "devices": len(devices)
    }


# =========================================================
# REGISTER DEVICE
# Android -> Server
# =========================================================

@app.post("/device/register")
def register_device(
    body: DeviceRegister,
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    device_id = generate_device_id()

    devices[device_id] = {

        "device_id": device_id,

        "device_name": body.device_name,

        "device_model": body.device_model,

        "android_id": body.android_id,

        "online": True,

        "last_seen": time.time()
    }

    return {

        "ok": True,

        "message": "Device registered successfully",

        "device_id": device_id,

        "device": devices[device_id]
    }


# =========================================================
# DEVICE HEARTBEAT
# =========================================================

@app.post("/device/heartbeat")
def device_heartbeat(
    device_id: str,
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    if device_id not in devices:

        raise HTTPException(
            status_code=404,
            detail="Device not registered"
        )

    devices[device_id]["online"] = True

    devices[device_id]["last_seen"] = time.time()

    return {
        "ok": True,
        "device_id": device_id,
        "online": True
    }


# =========================================================
# GET ALL DEVICES
# Dashboard -> Server
# =========================================================

@app.get("/devices")
def get_devices(
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    now = time.time()

    result = []

    for device in devices.values():

        # 30 sec se heartbeat nahi aaya
        is_online = (
            now - device["last_seen"] < 30
        )

        device_copy = dict(device)

        device_copy["online"] = is_online

        result.append(device_copy)

    return {

        "ok": True,

        "count": len(result),

        "devices": result
    }


# =========================================================
# SINGLE DEVICE
# =========================================================

@app.get("/device/{device_id}")
def get_device(
    device_id: str,
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    device = devices.get(device_id)

    if device is None:

        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    return {
        "ok": True,
        "device": device
    }


# =========================================================
# SEND COMMAND
# Dashboard/Laptop -> Selected Phone
# =========================================================

@app.post("/command")
def send_command(
    body: Command,
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    if body.device_id not in devices:

        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    if body.command not in ALLOWED:

        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )

    pending_commands[body.device_id] = {

        "device_id": body.device_id,

        "command": body.command,

        "payload": body.payload,

        "created_at": time.time()
    }

    return {

        "ok": True,

        "message": "Command queued",

        "device_id": body.device_id,

        "command": body.command
    }


# =========================================================
# ANDROID POLLS COMMAND
# =========================================================

@app.get("/command")
def get_command(
    device_id: str,
    authorization: str | None = Header(default=None)
):

    check_api_token(authorization)

    if device_id not in devices:

        raise HTTPException(
            status_code=404,
            detail="Device not registered"
        )

    devices[device_id]["online"] = True

    devices[device_id]["last_seen"] = time.time()

    command = pending_commands.pop(
        device_id,
        None
    )

    if command is None:

        return {

            "ok": True,

            "command": None
        }

    return {

        "ok": True,

        **command
    }
