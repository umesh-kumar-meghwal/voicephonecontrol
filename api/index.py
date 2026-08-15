from fastapi import FastAPI, Header, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import os
import requests


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
# SUPABASE CONFIG
# =========================================================

SUPABASE_URL = os.getenv(
    "SUPABASE_URL",
    ""
).rstrip("/")

SUPABASE_SERVICE_ROLE_KEY = os.getenv(
    "SUPABASE_SERVICE_ROLE_KEY",
    ""
)

STATUS_TABLE = "phone_status"

SCREENSHOT_TABLE = "screenshots"


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
    "MUTE",
    "RECENTS",
    "WAKE_SCREEN",
}


# =========================================================
# TEMPORARY MEMORY
# =========================================================

pending_command = None

pending_screenshot = None


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
# SUPABASE HEADERS
# =========================================================

def supabase_headers():

    if not SUPABASE_URL:

        raise HTTPException(
            status_code=500,
            detail="SUPABASE_URL is not configured"
        )

    if not SUPABASE_SERVICE_ROLE_KEY:

        raise HTTPException(
            status_code=500,
            detail="SUPABASE_SERVICE_ROLE_KEY is not configured"
        )

    return {
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": f"Bearer {SUPABASE_SERVICE_ROLE_KEY}",
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Prefer": "return=representation"
    }


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
# LAPTOP / WEB -> SERVER
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

    # -----------------------------------------------------
    # KEEP EXISTING TEMPORARY SCREENSHOT
    # -----------------------------------------------------

    pending_screenshot = {

        "filename": body.filename,

        "image": body.image
    }

    # -----------------------------------------------------
    # SAVE SCREENSHOT TO SUPABASE
    # -----------------------------------------------------

    try:

        headers = supabase_headers()

        data = {

            "filename": body.filename,

            "image": body.image

        }

        response = requests.post(

            f"{SUPABASE_URL}/rest/v1/{SCREENSHOT_TABLE}",

            headers={
                **headers,
                "Prefer": "return=representation"
            },

            json=data,

            timeout=30
        )

        if response.status_code not in (200, 201):

            raise HTTPException(

                status_code=500,

                detail=(
                    "Supabase screenshot upload failed: "
                    f"{response.status_code} "
                    f"{response.text}"
                )
            )

        return {

            "ok": True,

            "message": "Screenshot uploaded and saved",

            "filename": body.filename

        }

    except HTTPException:

        raise

    except Exception as e:

        raise HTTPException(

            status_code=500,

            detail=f"Screenshot save error: {str(e)}"

        )


# =========================================================
# LAPTOP -> SERVER
# DOWNLOAD LATEST SCREENSHOT
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
# WEB -> SERVER
# GET ALL SCREENSHOTS FROM SUPABASE
# =========================================================

@app.get("/api/screenshots")
def get_screenshots(

    authorization: str | None = Header(
        default=None
    )

):

    # Authentication

    check_auth(
        authorization
    )

    try:

        headers = supabase_headers()

        response = requests.get(

            f"{SUPABASE_URL}/rest/v1/{SCREENSHOT_TABLE}"
            "?select=id,filename,image,created_at"
            "&order=created_at.desc",

            headers=headers,

            timeout=30
        )

        if response.status_code != 200:

            raise HTTPException(

                status_code=500,

                detail=(
                    "Supabase screenshot read failed: "
                    f"{response.status_code} "
                    f"{response.text}"
                )
            )

        rows = response.json()

        return {

            "ok": True,

            "screenshots": rows

        }

    except HTTPException:

        raise

    except Exception as e:

        raise HTTPException(

            status_code=500,

            detail=f"Screenshot gallery error: {str(e)}"

        )

# =========================================================
# DELETE SCREENSHOT
# =========================================================

@app.delete("/api/screenshot/{screenshot_id}")
def delete_screenshot(
    screenshot_id: int,
    authorization: str | None = Header(default=None)
):

    check_auth(authorization)

    try:

        headers = supabase_headers()

        response = requests.delete(

            f"{SUPABASE_URL}/rest/v1/{SCREENSHOT_TABLE}?id=eq.{screenshot_id}",

            headers=headers,

            timeout=10
        )

        if response.status_code not in (200, 204):

            raise HTTPException(

                status_code=500,

                detail=(
                    "Screenshot delete failed: "
                    f"{response.status_code} "
                    f"{response.text}"
                )
            )

        return {

            "ok": True,

            "message": "Screenshot deleted",

            "id": screenshot_id
        }

    except HTTPException:

        raise

    except Exception as e:

        raise HTTPException(

            status_code=500,

            detail=f"Screenshot delete error: {str(e)}"
        )
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

    # Authentication

    check_auth(
        authorization
    )

    try:

        headers = supabase_headers()

        data = {

            "id": 1,

            "battery": body.battery,

            "charging": body.charging,

            "network": body.network,

            "android": body.android

        }

        # -------------------------------------------------
        # UPSERT STATUS
        # -------------------------------------------------

        response = requests.post(

            f"{SUPABASE_URL}/rest/v1/{STATUS_TABLE}?on_conflict=id",

            headers={
                **headers,
                "Prefer": "resolution=merge-duplicates,return=representation"
            },

            json=data,

            timeout=10
        )

        if response.status_code not in (200, 201):

            raise HTTPException(

                status_code=500,

                detail=(
                    "Supabase status upload failed: "
                    f"{response.status_code} "
                    f"{response.text}"
                )
            )

        return {

            "ok": True,

            "message": "Phone status received",

            "status": data
        }

    except HTTPException:

        raise

    except Exception as e:

        raise HTTPException(

            status_code=500,

            detail=f"Phone status upload error: {str(e)}"
        )


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

    # Authentication

    check_auth(
        authorization
    )

    try:

        headers = supabase_headers()

        # -------------------------------------------------
        # GET LATEST PHONE STATUS
        # -------------------------------------------------

        response = requests.get(

            f"{SUPABASE_URL}/rest/v1/{STATUS_TABLE}"
            "?id=eq.1"
            "&select=id,battery,charging,network,android,updated_at",

            headers=headers,

            timeout=10
        )

        if response.status_code != 200:

            raise HTTPException(

                status_code=500,

                detail=(
                    "Supabase status read failed: "
                    f"{response.status_code} "
                    f"{response.text}"
                )
            )

        rows = response.json()

        # -------------------------------------------------
        # STATUS NOT AVAILABLE
        # -------------------------------------------------

        if not rows:

            return {

                "ok": False,

                "status": None
            }

        row = rows[0]

        status = {

            "battery": row.get("battery"),

            "charging": row.get("charging"),

            "network": row.get("network"),

            "android": row.get("android")
        }

        # Status consume/delete nahi hoga

        return {

            "ok": True,

            "status": status
        }

    except HTTPException:

        raise

    except Exception as e:

        raise HTTPException(

            status_code=500,

            detail=f"Phone status read error: {str(e)}"
        )
