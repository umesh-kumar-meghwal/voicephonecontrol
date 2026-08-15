from fastapi import FastAPI, Header, HTTPException, Response, Request
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from fastapi.responses import FileResponse
import os
import requests
import secrets
import hashlib
import time

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
# LOGIN AUTHENTICATION
# =========================================================

SESSION_COOKIE = "vpc_session"

SESSION_MAX_AGE = 60 * 60 * 24  # 24 hours

LOGIN_TABLE = "login"

# Temporary in-memory sessions.
# For single Vercel instance this is simple,
# but sessions can disappear on server restart.
sessions = {}



def create_session(username: str):
    session_id = secrets.token_urlsafe(32)

    sessions[session_id] = {
        "username": username,
        "expires": time.time() + SESSION_MAX_AGE
    }

    return session_id


def get_current_user(request: Request):

    session_id = request.cookies.get(
        SESSION_COOKIE
    )

    if not session_id:
        return None

    session = sessions.get(session_id)

    if not session:
        return None

    if session["expires"] < time.time():

        sessions.pop(
            session_id,
            None
        )

        return None

    return session["username"]


def require_login(request: Request):

    username = get_current_user(request)

    if not username:

        raise HTTPException(
            status_code=401,
            detail="Login required"
        )

    return username


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
    
class LoginRequest(BaseModel):

    username: str
    password: str


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
    "START_MIC",
    "STOP_MIC",
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
# LOGIN
# =========================================================

@app.get("/login.html")
def login_page():
    from fastapi.responses import FileResponse

    login_file = os.path.abspath(
        os.path.join(
            os.path.dirname(__file__),
            "..",
            "login.html"
        )
    )

    if not os.path.isfile(login_file):
        raise HTTPException(
            status_code=404,
            detail="login.html not found"
        )

    return FileResponse(login_file)


@app.post("/api/login")
def login(
    body: LoginRequest,
    response: Response
):

    username = body.username.strip()

    password = body.password

    if not username or not password:

        raise HTTPException(
            status_code=400,
            detail="Username and password are required"
        )

    try:

        headers = supabase_headers()

        response_db = requests.get(

            f"{SUPABASE_URL}/rest/v1/{LOGIN_TABLE}"
            "?select=id,username,password"
            f"&username=eq.{requests.utils.quote(username, safe='')}"
            "&limit=1",

            headers=headers,

            timeout=10
        )

        if response_db.status_code != 200:

            raise HTTPException(
                status_code=500,
                detail="Login service unavailable"
            )

        rows = response_db.json()

        if not rows:

            raise HTTPException(
                status_code=401,
                detail="Invalid username or password"
            )

        user = rows[0]

        stored_hash = user.get("password")

        if not stored_hash:

            raise HTTPException(
                status_code=401,
                detail="Invalid username or password"
            )

        password_hash = password

        if not secrets.compare_digest(
            password_hash,
            stored_hash
        ):

            raise HTTPException(
                status_code=401,
                detail="Invalid username or password"
            )

        session_id = create_session(username)

        response.set_cookie(

            key=SESSION_COOKIE,

            value=session_id,

            max_age=SESSION_MAX_AGE,

            httponly=True,

            secure=True,

            samesite="lax",

            path="/"

        )

        return {

            "ok": True,

            "message": "Login successful",

            "username": username

        }

    except HTTPException:

        raise

    except Exception:

        raise HTTPException(
            status_code=500,
            detail="Login service error"
        )

# =========================================================
# CHECK LOGIN SESSION
# =========================================================

@app.get("/api/auth/me")
def auth_me(
    request: Request
):

    username = get_current_user(request)

    if not username:

        raise HTTPException(
            status_code=401,
            detail="Not authenticated"
        )

    return {

        "ok": True,

        "authenticated": True,

        "username": username

    }

# =========================================================
# LOGOUT
# =========================================================

@app.post("/api/logout")
def logout(
    request: Request,
    response: Response
):

    session_id = request.cookies.get(
        SESSION_COOKIE
    )

    if session_id:

        sessions.pop(
            session_id,
            None
        )

    response.delete_cookie(
        key=SESSION_COOKIE,
        path="/"
    )

    return {

        "ok": True,

        "message": "Logged out"

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
