from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
import os


# =========================================================
# APP
# =========================================================

app = FastAPI(
    title="Voice Phone Control API"
)


# =========================================================
# API TOKEN
# =========================================================

API_TOKEN = os.getenv(
    "API_TOKEN",
    ""
)


# =========================================================
# MODELS
# =========================================================

class Command(BaseModel):

    command: str
    payload: dict = {}


class Screenshot(BaseModel):

    filename: str
    image: str


class PhoneStatus(BaseModel):

    battery: int
    charging: bool
    network: bool
    android: str


# =========================================================
# ALLOWED COMMANDS
# =========================================================
ALLOWED_COMMANDS = {
    "OPEN_APP",
    "BACK",
    "HOME",
    "VOLUME_UP",
    "VOLUME_DOWN",
    "TAKE_SCREENSHOT",
    "PHONE_STATUS",
    "LIVE_SCREEN",
}


# =========================================================
# TEMPORARY MEMORY
# =========================================================

pending_command = None

pending_screenshot = None

pending_status = None


# =========================================================
# AUTHENTICATION
# =========================================================

def check_auth(
    authorization: str | None
):

    if not API_TOKEN:

        raise HTTPException(
            status_code=500,
            detail="API_TOKEN is not configured"
        )


    if authorization != f"Bearer {API_TOKEN}":

        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )


# =========================================================
# HEALTH
# =========================================================

@app.get("/api/health")
def health():

    return {
        "ok": True,
        "message": "Voice Phone Control API running"
    }


# =========================================================
# LAPTOP -> SERVER
# SEND COMMAND
# =========================================================

@app.post("/api/command")
def send_command(

    body: Command,

    authorization: str | None = Header(
        default=None
    )

):

    global pending_command
    global pending_status


    # Authentication

    check_auth(
        authorization
    )


    # Check command

    if body.command not in ALLOWED_COMMANDS:

        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )


    # Queue command

    pending_command = {

        "command": body.command,

        "payload": body.payload
    }


    # New status request
    # Remove old status

    if body.command == "PHONE_STATUS":

        pending_status = None


    return {

        "ok": True,

        "message": "Command queued",

        "command": body.command,

        "payload": body.payload
    }


# =========================================================
# ANDROID -> SERVER
# GET COMMAND
# =========================================================

@app.get("/api/command")
def get_command(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_command


    # Authentication

    check_auth(
        authorization
    )


    # Get command

    command = pending_command


    # Consume command

    pending_command = None


    # No command

    if command is None:

        return {

            "ok": True,

            "command": None,

            "payload": {}
        }


    # Return command

    return {

        "ok": True,

        **command
    }


# =========================================================
# ANDROID -> SERVER
# UPLOAD SCREENSHOT
# =========================================================

@app.post("/api/screenshot")
def upload_screenshot(

    body: Screenshot,

    authorization: str | None = Header(
        default=None
    )

):

    global pending_screenshot


    # Authentication

    check_auth(
        authorization
    )


    # Store screenshot

    pending_screenshot = {

        "filename": body.filename,

        "image": body.image
    }


    return {

        "ok": True,

        "message": "Screenshot uploaded",

        "filename": body.filename
    }


# =========================================================
# LAPTOP -> SERVER
# DOWNLOAD SCREENSHOT
# =========================================================

@app.get("/api/screenshot")
def get_screenshot(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_screenshot


    # Authentication

    check_auth(
        authorization
    )


    # Screenshot not available

    if pending_screenshot is None:

        return {

            "ok": False,

            "image": None,

            "filename": None
        }


    # Get screenshot

    screenshot = pending_screenshot


    # Consume screenshot

    pending_screenshot = None


    return {

        "ok": True,

        "filename": screenshot["filename"],

        "image": screenshot["image"]
    }


# =========================================================
# ANDROID -> SERVER
# UPLOAD PHONE STATUS
# =========================================================

@app.post("/api/status")
def upload_status(

    body: PhoneStatus,

    authorization: str | None = Header(
        default=None
    )

):

    global pending_status


    # Authentication

    check_auth(
        authorization
    )


    # Store phone status

    pending_status = {

        "battery": body.battery,

        "charging": body.charging,

        "network": body.network,

        "android": body.android
    }


    return {

        "ok": True,

        "message": "Phone status received"
    }


# =========================================================
# LAPTOP -> SERVER
# GET PHONE STATUS
# =========================================================

@app.get("/api/status")
def get_status(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_status


    # Authentication

    check_auth(
        authorization
    )


    # Status not available

    if pending_status is None:

        return {

            "ok": False,

            "status": None
        }


    # Get status

    status = pending_status


    # Consume status

    pending_status = None


    return {

        "ok": True,

        "status": status
    }