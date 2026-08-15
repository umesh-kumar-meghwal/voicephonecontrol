from fastapi import FastAPI, Header, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import os


# =========================================================
# APP FOR UMESH DEVELOPER
# =========================================================

app = FastAPI(
    title="Voice Phone Control API"
)
WEB_DIR = os.path.abspath(
    os.path.join(
        os.path.dirname(__file__),
        "..",
        "web"
    )
)

if os.path.isdir(WEB_DIR):
    app.mount(
        "/web",
        StaticFiles(
            directory=WEB_DIR,
            html=True
        ),
        name="web"
    )


# =========================================================
# API TOKEN FOR UMESH DEVELOPER
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
# ALLOWED COMMANDS FOR UMESH DEVELOPER
# =========================================================
ALLOWED_COMMANDS = {
    "HOME",
    "BACK",
    "VOLUME_UP",
    "VOLUME_DOWN",
    "TAKE_SCREENSHOT",
    "PHONE_STATUS",
    "OPEN_APP",
    "LIVE_SCREEN",
    "NOTIFICATION_STATUS",
    "ENTER",

    # NEW
    "UP",
    "DOWN",
    "TAB",
    "LEFT",
    "RIGHT",
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
# SEND COMMAND FOR UMESH DEVELOPER
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


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Check command FOR UMESH DEVELOPER

    if body.command not in ALLOWED_COMMANDS:

        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )


    # Queue command FOR UMESH DEVELOPER

    pending_command = {

        "command": body.command,

        "payload": body.payload
    }


    # New status request FOR UMESH DEVELOPER
    # Remove old status FOR UMESH DEVELOPER

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
# GET COMMAND FOR UMESH DEVELOPER
# =========================================================

@app.get("/api/command")
def get_command(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_command


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Get command FOR UMESH DEVELOPER

    command = pending_command


    # Consume command FOR UMESH DEVELOPER

    pending_command = None


    # No command FOR UMESH DEVELOPER

    if command is None:

        return {

            "ok": True,

            "command": None,

            "payload": {}
        }


    # Return command FOR UMESH DEVELOPER

    return {

        "ok": True,

        **command
    }


# =========================================================
# ANDROID -> SERVER
# UPLOAD SCREENSHOT FOR UMESH DEVELOPER
# =========================================================

@app.post("/api/screenshot")
def upload_screenshot(

    body: Screenshot,

    authorization: str | None = Header(
        default=None
    )

):

    global pending_screenshot


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Store screenshot FOR UMESH DEVELOPER

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
# DOWNLOAD SCREENSHOT  FOR UMESH DEVELOPER
# =========================================================

@app.get("/api/screenshot")
def get_screenshot(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_screenshot


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Screenshot not available FOR UMESH DEVELOPER

    if pending_screenshot is None:

        return {

            "ok": False,

            "image": None,

            "filename": None
        }


    # Get screenshot  FOR UMESH DEVELOPER

    screenshot = pending_screenshot


    # Consume screenshot   FOR UMESH DEVELOPER

    pending_screenshot = None


    return {

        "ok": True,

        "filename": screenshot["filename"],

        "image": screenshot["image"]
    }


# =========================================================
# ANDROID -> SERVER
# UPLOAD PHONE STATUS   FOR UMESH DEVELOPER
# =========================================================

@app.post("/api/status")
def upload_status(

    body: PhoneStatus,

    authorization: str | None = Header(
        default=None
    )

):

    global pending_status


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Store phone status   FOR UMESH DEVELOPER

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
# GET PHONE STATUS FOR UMESH DEVELOPER
# =========================================================

@app.get("/api/status")
def get_status(

    authorization: str | None = Header(
        default=None
    )

):

    global pending_status


    # Authentication FOR UMESH DEVELOPER

    check_auth(
        authorization
    )


    # Status not available FOR UMESH DEVELOPER

    if pending_status is None:

        return {

            "ok": False,

            "status": None
        }

 
    # Get status FOR UMESH DEVELOPER

    status = pending_status


    # Consume status FOR UMESH DEVELOPER

    pending_status = None


    return {

        "ok": True,

        "status": status
    }
