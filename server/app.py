import os
import secrets
import hashlib
import logging

from datetime import datetime, timedelta, timezone
from typing import Optional

import jwt

from dotenv import load_dotenv

from fastapi import (
    FastAPI,
    HTTPException,
    Depends,
)

from fastapi.responses import HTMLResponse

from fastapi.security import (
    HTTPBearer,
    HTTPAuthorizationCredentials,
)

from pydantic import BaseModel, Field

from passlib.context import CryptContext

from supabase import create_client, Client


# =========================================================
# LOGGING
# =========================================================

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s"
)

logger = logging.getLogger("VoicePhoneControl")


# =========================================================
# ENVIRONMENT
# =========================================================

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")

SUPABASE_SERVICE_ROLE_KEY = os.getenv(
    "SUPABASE_SERVICE_ROLE_KEY"
)

JWT_SECRET = os.getenv("JWT_SECRET")

JWT_ALGORITHM = "HS256"

JWT_EXPIRE_MINUTES = 60 * 24


# =========================================================
# ENV VALIDATION
# =========================================================

if not SUPABASE_URL:

    raise RuntimeError(
        "SUPABASE_URL missing in environment variables"
    )


if not SUPABASE_SERVICE_ROLE_KEY:

    raise RuntimeError(
        "SUPABASE_SERVICE_ROLE_KEY missing in environment variables"
    )


if not JWT_SECRET:

    raise RuntimeError(
        "JWT_SECRET missing in environment variables"
    )


# =========================================================
# SUPABASE
# =========================================================

supabase: Client = create_client(
    SUPABASE_URL,
    SUPABASE_SERVICE_ROLE_KEY
)


# =========================================================
# FASTAPI
# =========================================================

app = FastAPI(
    title="Voice Phone Control Server",
    version="2.0.1"
)


# =========================================================
# SECURITY
# =========================================================

security = HTTPBearer(
    auto_error=False
)

pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto"
)


# =========================================================
# ALLOWED COMMANDS
# =========================================================

ALLOWED_COMMANDS = {

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
# MODELS
# =========================================================

class RegisterRequest(BaseModel):

    username: str = Field(
        min_length=3,
        max_length=50
    )

    password: str = Field(
        min_length=6,
        max_length=128
    )


class LoginRequest(BaseModel):

    username: str

    password: str


class DeviceRegisterRequest(BaseModel):

    device_name: str = "Android Phone"

    device_model: str = "Unknown"

    android_id: Optional[str] = None


class ClaimDeviceRequest(BaseModel):

    device_id: str


class CommandRequest(BaseModel):

    device_id: str

    command: str

    payload: dict = Field(
        default_factory=dict
    )


class DeviceHeartbeatRequest(BaseModel):

    device_id: str


# =========================================================
# PASSWORD
# =========================================================

def hash_password(
    password: str
) -> str:

    # Current database compatibility.
    #
    # Existing users use plain text passwords.
    #
    # Keep this unchanged for compatibility.
    #
    # IMPORTANT:
    # Production security should migrate to bcrypt.

    return password


def verify_password(
    password: str,
    password_hash: str
) -> bool:

    return password == password_hash


# =========================================================
# JWT CREATE
# =========================================================

def create_access_token(
    user_id: str
) -> str:

    expires_at = (
        datetime.now(timezone.utc)
        +
        timedelta(
            minutes=JWT_EXPIRE_MINUTES
        )
    )

    payload = {

        "sub": str(user_id),

        "exp": expires_at,
    }

    return jwt.encode(
        payload,
        JWT_SECRET,
        algorithm=JWT_ALGORITHM
    )


# =========================================================
# JWT VERIFY
# =========================================================

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(
        security
    )
):

    if credentials is None:

        raise HTTPException(
            status_code=401,
            detail="Login token required"
        )

    token = credentials.credentials

    try:

        payload = jwt.decode(
            token,
            JWT_SECRET,
            algorithms=[JWT_ALGORITHM]
        )

    except jwt.ExpiredSignatureError:

        raise HTTPException(
            status_code=401,
            detail="Token expired"
        )

    except jwt.InvalidTokenError:

        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )

    user_id = payload.get("sub")

    if not user_id:

        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )

    try:

        result = (
            supabase
            .table("users")
            .select(
                "id,username,created_at"
            )
            .eq(
                "id",
                user_id
            )
            .limit(1)
            .execute()
        )

    except Exception as exc:

        logger.exception(
            "GET CURRENT USER DATABASE ERROR"
        )

        raise HTTPException(
            status_code=500,
            detail="Could not verify user"
        ) from exc

    users = result.data or []

    if not users:

        raise HTTPException(
            status_code=401,
            detail="User not found"
        )

    return users[0]


# =========================================================
# DEVICE ID
# =========================================================

def generate_device_id():

    return (
        "VPC-"
        +
        secrets
        .token_hex(4)
        .upper()
    )


# =========================================================
# DEVICE TOKEN
# =========================================================

def generate_device_token():

    return (
        "vpc_"
        +
        secrets.token_urlsafe(32)
    )


def hash_device_token(
    token: str
) -> str:

    return hashlib.sha256(
        token.encode()
    ).hexdigest()


# =========================================================
# DEVICE AUTHENTICATION
# =========================================================

def authenticate_device(
    device_id: str,
    device_token: str
):

    if not device_id:

        raise HTTPException(
            status_code=400,
            detail="Device ID required"
        )

    if not device_token:

        raise HTTPException(
            status_code=401,
            detail="Device token required"
        )

    token_hash = hash_device_token(
        device_token
    )

    try:

        result = (
            supabase
            .table("devices")
            .select("*")
            .eq(
                "device_id",
                device_id
            )
            .eq(
                "device_token_hash",
                token_hash
            )
            .limit(1)
            .execute()
        )

    except Exception as exc:

        logger.exception(
            "DEVICE AUTHENTICATION DATABASE ERROR | device_id=%s",
            device_id
        )

        raise HTTPException(
            status_code=500,
            detail="Could not authenticate device"
        ) from exc

    if not result.data:

        logger.warning(
            "INVALID DEVICE CREDENTIALS | device_id=%s",
            device_id
        )

        raise HTTPException(
            status_code=401,
            detail="Invalid device credentials"
        )

    return result.data[0]


# =========================================================
# HEALTH
# =========================================================

@app.get("/health")
def health():

    return {

        "ok": True,

        "service": "Voice Phone Control",

        "version": "2.0.1"
    }


# =========================================================
# SUPABASE TEST
# =========================================================

@app.get("/supabase-test")
def supabase_test():

    try:

        result = (
            supabase
            .table("users")
            .select("id")
            .limit(1)
            .execute()
        )

        return {

            "ok": True,

            "supabase": "connected",

            "rows": len(
                result.data or []
            )
        }

    except Exception as exc:

        logger.exception(
            "SUPABASE TEST ERROR"
        )

        raise HTTPException(
            status_code=500,
            detail="Supabase connection failed"
        ) from exc


# =========================================================
# REGISTER USER
# =========================================================

@app.post("/auth/register")
def register_user(
    body: RegisterRequest
):

    username = (
        body.username
        .strip()
        .lower()
    )

    if not username:

        raise HTTPException(
            status_code=400,
            detail="Username required"
        )

    try:

        existing_response = (
            supabase
            .table("users")
            .select("id")
            .eq(
                "username",
                username
            )
            .limit(1)
            .execute()
        )

        existing_rows = (
            existing_response.data
            or []
        )

        if existing_rows:

            raise HTTPException(
                status_code=409,
                detail="Username already exists"
            )

        password_hash = hash_password(
            body.password
        )

        insert_response = (
            supabase
            .table("users")
            .insert({
                "username": username,
                "password_hash": password_hash
            })
            .execute()
        )

        inserted_rows = (
            insert_response.data
            or []
        )

        if not inserted_rows:

            raise HTTPException(
                status_code=500,
                detail="Could not create user"
            )

        user = inserted_rows[0]

        token = create_access_token(
            str(user["id"])
        )

        return {

            "ok": True,

            "message": "Registration successful",

            "token": token,

            "user": {

                "id": user["id"],

                "username": user["username"]
            }
        }

    except HTTPException:

        raise

    except Exception as exc:

        logger.exception(
            "REGISTER USER ERROR | username=%s",
            username
        )

        raise HTTPException(
            status_code=500,
            detail="Could not create user"
        ) from exc


# =========================================================
# LOGIN
# =========================================================

@app.post("/auth/login")
def login_user(
    body: LoginRequest
):

    username = (
        body.username
        .strip()
        .lower()
    )

    try:

        result = (
            supabase
            .table("users")
            .select("*")
            .eq(
                "username",
                username
            )
            .limit(1)
            .execute()
        )

    except Exception as exc:

        logger.exception(
            "LOGIN DATABASE ERROR | username=%s",
            username
        )

        raise HTTPException(
            status_code=500,
            detail="Could not connect to database"
        ) from exc

    users = result.data or []

    if not users:

        raise HTTPException(
            status_code=401,
            detail="Invalid username or password"
        )

    user = users[0]

    valid = verify_password(
        body.password,
        user["password_hash"]
    )

    if not valid:

        raise HTTPException(
            status_code=401,
            detail="Invalid username or password"
        )

    token = create_access_token(
        str(user["id"])
    )

    return {

        "ok": True,

        "message": "Login successful",

        "token": token,

        "user": {

            "id": user["id"],

            "username": user["username"]
        }
    }


# =========================================================
# CURRENT USER
# =========================================================

@app.get("/auth/me")
def current_user(
    user=Depends(get_current_user)
):

    return {

        "ok": True,

        "user": user
    }


# =========================================================
# REGISTER DEVICE
# =========================================================

@app.post("/device/register")
def register_device(
    body: DeviceRegisterRequest
):

    device_id = generate_device_id()

    device_token = generate_device_token()

    device_token_hash = hash_device_token(
        device_token
    )

    try:

        # -------------------------------------------------
        # CHECK UNIQUE DEVICE ID
        # -------------------------------------------------

        for _ in range(5):

            existing = (
                supabase
                .table("devices")
                .select("id")
                .eq(
                    "device_id",
                    device_id
                )
                .limit(1)
                .execute()
            )

            if not existing.data:

                break

            device_id = generate_device_id()

        else:

            raise HTTPException(
                status_code=500,
                detail="Could not generate unique device ID"
            )

        # -------------------------------------------------
        # INSERT DEVICE
        # -------------------------------------------------

        result = (
            supabase
            .table("devices")
            .insert({

                "device_id": device_id,

                "device_token_hash":
                    device_token_hash,

                "device_name":
                    body.device_name,

                "device_model":
                    body.device_model,

                "android_id":
                    body.android_id,

                "online": False,

                "user_id": None
            })
            .execute()
        )

        if not result.data:

            raise HTTPException(
                status_code=500,
                detail="Could not register device"
            )

        device = result.data[0]

        logger.info(
            "DEVICE REGISTERED | device_id=%s | model=%s",
            device["device_id"],
            body.device_model
        )

        return {

            "ok": True,

            "message":
                "Device registered successfully",

            "device_id":
                device["device_id"],

            "device_token":
                device_token,

            "device": {

                "id":
                    device["id"],

                "device_id":
                    device["device_id"],

                "device_name":
                    device["device_name"],

                "device_model":
                    device["device_model"],

                "online":
                    device["online"]
            }
        }

    except HTTPException:

        raise

    except Exception as exc:

        logger.exception(
            "DEVICE REGISTER ERROR"
        )

        raise HTTPException(
            status_code=500,
            detail="Could not register device"
        ) from exc


# =========================================================
# CLAIM DEVICE
# =========================================================

@app.post("/devices/claim")
def claim_device(
    body: ClaimDeviceRequest,
    user=Depends(get_current_user)
):

    try:

        result = (
            supabase
            .table("devices")
            .select(
                "id,user_id,device_id"
            )
            .eq(
                "device_id",
                body.device_id
            )
            .limit(1)
            .execute()
        )

        if not result.data:

            raise HTTPException(
                status_code=404,
                detail="Device not found"
            )

        device = result.data[0]

        if device["user_id"]:

            if (
                str(device["user_id"])
                ==
                str(user["id"])
            ):

                return {

                    "ok": True,

                    "message":
                        "Device already belongs to you"
                }

            raise HTTPException(
                status_code=409,
                detail=
                    "Device already belongs to another user"
            )

        update_result = (
            supabase
            .table("devices")
            .update({
                "user_id": user["id"]
            })
            .eq(
                "id",
                device["id"]
            )
            .is_(
                "user_id",
                "null"
            )
            .execute()
        )

        if not update_result.data:

            raise HTTPException(
                status_code=409,
                detail="Device could not be claimed"
            )

        logger.info(
            "DEVICE CLAIMED | device_id=%s",
            body.device_id
        )

        return {

            "ok": True,

            "message":
                "Device added successfully",

            "device_id":
                body.device_id
        }

    except HTTPException:

        raise

    except Exception as exc:

        logger.exception(
            "CLAIM DEVICE ERROR | device_id=%s",
            body.device_id
        )

        raise HTTPException(
            status_code=500,
            detail="Could not claim device"
        ) from exc


# =========================================================
# LIST DEVICES
# =========================================================

@app.get("/devices")
def list_devices(
    user=Depends(get_current_user)
):

    try:

        result = (
            supabase
            .table("devices")
            .select(
                "id,device_id,device_name,"
                "device_model,online,last_seen,"
                "created_at"
            )
            .eq(
                "user_id",
                user["id"]
            )
            .order(
                "created_at",
                desc=True
            )
            .execute()
        )

        return {

            "ok": True,

            "count":
                len(result.data or []),

            "devices":
                result.data or []
        }

    except Exception as exc:

        logger.exception(
            "LIST DEVICES ERROR | user_id=%s",
            user["id"]
        )

        raise HTTPException(
            status_code=500,
            detail="Could not load devices"
        ) from exc


# =========================================================
# GET DEVICE
# =========================================================

@app.get(
    "/devices/{device_id}"
)
def get_device(
    device_id: str,
    user=Depends(get_current_user)
):

    try:

        result = (
            supabase
            .table("devices")
            .select(
                "id,device_id,device_name,"
                "device_model,online,last_seen,"
                "created_at"
            )
            .eq(
                "device_id",
                device_id
            )
            .eq(
                "user_id",
                user["id"]
            )
            .limit(1)
            .execute()
        )

        if not result.data:

            raise HTTPException(
                status_code=404,
                detail="Device not found"
            )

        return {

            "ok": True,

            "device":
                result.data[0]
        }

    except HTTPException:

        raise

    except Exception as exc:

        logger.exception(
            "GET DEVICE ERROR | device_id=%s",
            device_id
        )

        raise HTTPException(
            status_code=500,
            detail="Could not load device"
        ) from exc


# =========================================================
# SEND COMMAND
# =========================================================

@app.post("/command")
def send_command(
    body: CommandRequest,
    user=Depends(get_current_user)
):

    command_name = (
        body.command
        .strip()
        .upper()
    )

    if command_name not in ALLOWED_COMMANDS:

        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )

    try:

        # -------------------------------------------------
        # VERIFY DEVICE BELONGS TO USER
        # -------------------------------------------------

        device_result = (
            supabase
            .table("devices")
            .select("id")
            .eq(
                "device_id",
                body.device_id
            )
            .eq(
                "user_id",
                user["id"]
            )
            .limit(1)
            .execute()
        )

        if not device_result.data:

            raise HTTPException(
                status_code=404,
                detail="Device not found"
            )

        device_uuid = (
            device_result.data[0]["id"]
        )

        # -------------------------------------------------
        # INSERT COMMAND
        # -------------------------------------------------

        result = (
            supabase
            .table("commands")
            .insert({

                "user_id":
                    user["id"],

                "device_id":
                    device_uuid,

                "command":
                    command_name,

                "payload":
                    body.payload,

                "status":
                    "pending"
            })
            .execute()
        )

        if not result.data:

            raise HTTPException(
                status_code=500,
                detail="Could not queue command"
            )

        command = result.data[0]

        logger.info(
            "COMMAND QUEUED | device_id=%s | command=%s",
            body.device_id,
            command_name
        )

        return {

            "ok": True,

            "message":
                "Command queued",

            "command_id":
                command["id"],

            "device_id":
                body.device_id,

            "command":
                command_name
        }

    except HTTPException:

        raise

    except Exception as exc:

        logger.exception(
            "SEND COMMAND ERROR | device_id=%s | command=%s",
            body.device_id,
            command_name
        )

        raise HTTPException(
            status_code=500,
            detail="Could not queue command"
        ) from exc


# =========================================================
# DEVICE GET COMMAND
# =========================================================

@app.get("/device/command")
def device_get_command(
    device_id: str,
    device_token: str
):

    # -----------------------------------------------------
    # AUTHENTICATE DEVICE
    # -----------------------------------------------------

    device = authenticate_device(
        device_id,
        device_token
    )

    now = datetime.now(
        timezone.utc
    ).isoformat()

    try:

        # -------------------------------------------------
        # MARK DEVICE ONLINE
        # -------------------------------------------------

        device_update = (
            supabase
            .table("devices")
            .update({

                "online": True,

                "last_seen": now
            })
            .eq(
                "id",
                device["id"]
            )
            .execute()
        )

        if not device_update.data:

            logger.warning(
                "DEVICE ONLINE UPDATE RETURNED NO DATA | device_id=%s",
                device_id
            )

        # -------------------------------------------------
        # FIND PENDING COMMAND
        # -------------------------------------------------

        result = (
            supabase
            .table("commands")
            .select("*")
            .eq(
                "device_id",
                device["id"]
            )
            .eq(
                "status",
                "pending"
            )
            .order(
                "created_at",
                desc=False
            )
            .limit(1)
            .execute()
        )

        # -------------------------------------------------
        # NO COMMAND
        # -------------------------------------------------

        if not result.data:

            return {

                "ok": True,

                "command": None
            }

        command = result.data[0]

        # -------------------------------------------------
        # MARK COMMAND DELIVERED
        # -------------------------------------------------

        delivered_result = (
            supabase
            .table("commands")
            .update({

                "status":
                    "delivered",

                "delivered_at":
                    now
            })
            .eq(
                "id",
                command["id"]
            )
            .eq(
                "status",
                "pending"
            )
            .execute()
        )

        if not delivered_result.data:

            logger.warning(
                "COMMAND DELIVERY UPDATE RETURNED NO DATA | command_id=%s | device_id=%s",
                command["id"],
                device_id
            )

        logger.info(
            "COMMAND DELIVERED | device_id=%s | command=%s | command_id=%s",
            device_id,
            command["command"],
            command["id"]
        )

        return {

            "ok": True,

            "command":
                command["command"],

            "payload":
                command.get(
                    "payload",
                    {}
                ),

            "command_id":
                command["id"]
        }

    except Exception as exc:

        logger.exception(
            "DEVICE GET COMMAND ERROR | device_id=%s",
            device_id
        )

        raise HTTPException(
            status_code=500,
            detail="Could not process device command"
        ) from exc


# =========================================================
# HEARTBEAT
# =========================================================

@app.post("/device/heartbeat")
def device_heartbeat(
    body: DeviceHeartbeatRequest,
    device_token: str
):

    # -----------------------------------------------------
    # AUTHENTICATE DEVICE
    # -----------------------------------------------------

    device = authenticate_device(
        body.device_id,
        device_token
    )

    now = datetime.now(
        timezone.utc
    ).isoformat()

    try:

        # -------------------------------------------------
        # UPDATE ONLINE STATUS
        # -------------------------------------------------

        result = (
            supabase
            .table("devices")
            .update({

                "online": True,

                "last_seen": now
            })
            .eq(
                "id",
                device["id"]
            )
            .execute()
        )

        if not result.data:

            logger.warning(
                "HEARTBEAT UPDATE RETURNED NO DATA | device_id=%s",
                body.device_id
            )

        logger.info(
            "HEARTBEAT SUCCESS | device_id=%s",
            body.device_id
        )

        return {

            "ok": True,

            "device_id":
                body.device_id,

            "online": True
        }

    except Exception as exc:

        logger.exception(
            "HEARTBEAT DATABASE ERROR | device_id=%s",
            body.device_id
        )

        raise HTTPException(
            status_code=500,
            detail="Could not update heartbeat"
        ) from exc


# =========================================================
# DASHBOARD HTML
# =========================================================

DASHBOARD_HTML = r"""
<!DOCTYPE html>

<html lang="en">

<head>

<meta charset="UTF-8">

<meta
name="viewport"
content="width=device-width, initial-scale=1.0"
>

<title>
VoicePhoneControl Dashboard
</title>

<style>

* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #0b1020;
    color: white;
}

.container {
    width: 92%;
    max-width: 1100px;
    margin: 40px auto;
}

.card {
    background: #151c32;
    border: 1px solid #283452;
    border-radius: 18px;
    padding: 25px;
    margin-bottom: 20px;
}

input {
    width: 100%;
    padding: 13px;
    margin: 7px 0;
    border-radius: 10px;
    border: 1px solid #303b5c;
    background: #0d1428;
    color: white;
}

button {
    padding: 11px 17px;
    border: 0;
    border-radius: 10px;
    cursor: pointer;
    font-weight: bold;
    margin: 4px;
}

.device {
    padding: 18px;
    border: 1px solid #303b5c;
    border-radius: 14px;
    margin-top: 14px;
}

.online {
    color: #4dff88;
}

.offline {
    color: #ff6464;
}

.hidden {
    display: none;
}

.commands {
    display: flex;
    flex-wrap: wrap;
    margin-top: 15px;
}

.message {
    margin-top: 12px;
}

</style>

</head>

<body>

<div class="container">

<!-- AUTH -->

<div
id="authBox"
class="card"
>

<h1>
VoicePhoneControl
</h1>

<p>
Login or create your account.
</p>

<input
id="username"
placeholder="Username"
/>

<input
id="password"
type="password"
placeholder="Password"
/>

<button
onclick="registerUser()"
>
Register
</button>

<button
onclick="loginUser()"
>
Login
</button>

<p
id="authMessage"
class="message"
></p>

</div>


<!-- DASHBOARD -->

<div
id="dashboardBox"
class="hidden"
>

<div class="card">

<h1>
Dashboard
</h1>

<p>
Welcome,
<strong id="usernameDisplay"></strong>
</p>

<button onclick="logout()">
Logout
</button>

</div>


<!-- ADD DEVICE -->

<div class="card">

<h2>
Add Device
</h2>

<p>
Enter the Device ID shown by the Android app.
</p>

<input
id="deviceIdInput"
placeholder="VPC-XXXXXXXX"
/>

<button onclick="claimDevice()">
Add Device
</button>

<p
id="deviceMessage"
class="message"
></p>

</div>


<!-- DEVICES -->

<div class="card">

<h2>
My Devices
</h2>

<button onclick="loadDevices()">
Refresh
</button>

<div id="devices">
No devices loaded.
</div>

</div>

</div>

</div>


<script>

const API = "";

let token =
localStorage.getItem("vpc_token");


function authHeaders() {

    return {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
    };
}


function showDashboard() {

    document
        .getElementById("authBox")
        .classList
        .add("hidden");

    document
        .getElementById("dashboardBox")
        .classList
        .remove("hidden");

    loadMe();
    loadDevices();
}


function showAuth() {

    document
        .getElementById("authBox")
        .classList
        .remove("hidden");

    document
        .getElementById("dashboardBox")
        .classList
        .add("hidden");
}


function clearToken() {

    token = null;

    localStorage.removeItem(
        "vpc_token"
    );
}


async function registerUser() {

    const username =
        document
        .getElementById("username")
        .value
        .trim();

    const password =
        document
        .getElementById("password")
        .value;

    if (!username || !password) {

        setAuthMessage(
            "Username and password required."
        );

        return;
    }

    try {

        const response =
            await fetch(
                API + "/auth/register",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({
                            username,
                            password
                        })
                }
            );

        const data =
            await response.json();

        if (!response.ok) {

            setAuthMessage(
                data.detail ||
                "Registration failed."
            );

            return;
        }

        token = data.token;

        localStorage.setItem(
            "vpc_token",
            token
        );

        setAuthMessage(
            "Registration successful."
        );

        showDashboard();

    } catch (error) {

        setAuthMessage(
            "Server connection failed."
        );
    }
}


async function loginUser() {

    const username =
        document
        .getElementById("username")
        .value
        .trim();

    const password =
        document
        .getElementById("password")
        .value;

    if (!username || !password) {

        setAuthMessage(
            "Username and password required."
        );

        return;
    }

    try {

        const response =
            await fetch(
                API + "/auth/login",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({
                            username,
                            password
                        })
                }
            );

        const data =
            await response.json();

        if (!response.ok) {

            setAuthMessage(
                data.detail ||
                "Login failed."
            );

            return;
        }

        token = data.token;

        localStorage.setItem(
            "vpc_token",
            token
        );

        showDashboard();

    } catch (error) {

        setAuthMessage(
            "Server connection failed."
        );
    }
}


async function loadMe() {

    if (!token) {

        showAuth();

        return false;
    }

    try {

        const response =
            await fetch(
                API + "/auth/me",
                {
                    headers: {
                        "Authorization":
                            "Bearer " + token
                    }
                }
            );

        if (response.status === 401) {

            clearToken();

            showAuth();

            setAuthMessage(
                "Session expired. Please login again."
            );

            return false;
        }

        if (!response.ok) {

            showAuth();

            return false;
        }

        const data =
            await response.json();

        document
            .getElementById(
                "usernameDisplay"
            )
            .textContent =
            data.user.username;

        return true;

    } catch (error) {

        return false;
    }
}


async function claimDevice() {

    if (!token) {

        showAuth();

        return;
    }

    const deviceId =
        document
        .getElementById(
            "deviceIdInput"
        )
        .value
        .trim()
        .toUpperCase();

    if (!deviceId) {

        setDeviceMessage(
            "Device ID required."
        );

        return;
    }

    try {

        const response =
            await fetch(
                API + "/devices/claim",
                {
                    method: "POST",

                    headers:
                        authHeaders(),

                    body:
                        JSON.stringify({
                            device_id:
                                deviceId
                        })
                }
            );

        const data =
            await response.json();

        if (response.status === 401) {

            clearToken();

            showAuth();

            setAuthMessage(
                "Session expired. Please login again."
            );

            return;
        }

        if (!response.ok) {

            setDeviceMessage(
                data.detail ||
                "Could not add device."
            );

            return;
        }

        setDeviceMessage(
            "Device added successfully."
        );

        document
            .getElementById(
                "deviceIdInput"
            )
            .value = "";

        loadDevices();

    } catch (error) {

        setDeviceMessage(
            "Server connection failed."
        );
    }
}


async function loadDevices() {

    const container =
        document
        .getElementById(
            "devices"
        );

    if (!token) {

        showAuth();

        return;
    }

    container.innerHTML =
        "Loading...";

    try {

        const response =
            await fetch(
                API + "/devices",
                {
                    headers: {
                        "Authorization":
                            "Bearer " + token
                    }
                }
            );

        if (response.status === 401) {

            clearToken();

            container.innerHTML =
                "";

            showAuth();

            setAuthMessage(
                "Invalid or expired token. Please login again."
            );

            return;
        }

        if (!response.ok) {

            container.innerHTML =
                "Could not load devices.";

            return;
        }

        const data =
            await response.json();

        if (
            !data.devices ||
            data.devices.length === 0
        ) {

            container.innerHTML =
                "<p>No devices added yet.</p>";

            return;
        }

        container.innerHTML = "";

        data.devices.forEach(
            device => {

                const div =
                    document.createElement(
                        "div"
                    );

                div.className =
                    "device";

                const status =
                    device.online

                    ?

                    '<span class="online">● Online</span>'

                    :

                    '<span class="offline">● Offline</span>';

                div.innerHTML = `

                    <h3>
                        📱
                        ${escapeHtml(
                            device.device_name
                        )}
                    </h3>

                    <p>
                        Device ID:
                        <strong>
                            ${escapeHtml(
                                device.device_id
                            )}
                        </strong>
                    </p>

                    <p>
                        Model:
                        ${escapeHtml(
                            device.device_model
                        )}
                    </p>

                    <p>
                        Status:
                        ${status}
                    </p>

                    <div class="commands">

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'HOME'
                        )"
                        >
                            HOME
                        </button>

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'BACK'
                        )"
                        >
                            BACK
                        </button>

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'VOLUME_UP'
                        )"
                        >
                            VOL +
                        </button>

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'VOLUME_DOWN'
                        )"
                        >
                            VOL -
                        </button>

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'TAKE_SCREENSHOT'
                        )"
                        >
                            SCREENSHOT
                        </button>

                        <button
                        onclick="sendCommand(
                            '${escapeHtml(device.device_id)}',
                            'PHONE_STATUS'
                        )"
                        >
                            PHONE STATUS
                        </button>

                    </div>
                `;

                container.appendChild(
                    div
                );
            }
        );

    } catch (error) {

        container.innerHTML =
            "Server connection failed.";
    }
}


async function sendCommand(
    deviceId,
    command
) {

    if (!token) {

        showAuth();

        return;
    }

    try {

        const response =
            await fetch(
                API + "/command",
                {
                    method: "POST",

                    headers:
                        authHeaders(),

                    body:
                        JSON.stringify({
                            device_id:
                                deviceId,

                            command:
                                command,

                            payload: {}
                        })
                }
            );

        const data =
            await response.json();

        if (response.status === 401) {

            clearToken();

            showAuth();

            setAuthMessage(
                "Session expired. Please login again."
            );

            return;
        }

        if (!response.ok) {

            alert(
                data.detail ||
                "Command failed."
            );

            return;
        }

        alert(
            command +
            " queued for " +
            deviceId
        );

    } catch (error) {

        alert(
            "Server connection failed."
        );
    }
}


function setAuthMessage(
    message
) {

    document
        .getElementById(
            "authMessage"
        )
        .textContent =
        message;
}


function setDeviceMessage(
    message
) {

    document
        .getElementById(
            "deviceMessage"
        )
        .textContent =
        message;
}


function escapeHtml(
    value
) {

    return String(value)

        .replaceAll(
            "&",
            "&amp;"
        )

        .replaceAll(
            "<",
            "&lt;"
        )

        .replaceAll(
            ">",
            "&gt;"
        )

        .replaceAll(
            '"',
            "&quot;"
        )

        .replaceAll(
            "'",
            "&#039;"
        );
}


function logout() {

    clearToken();

    document
        .getElementById(
            "usernameDisplay"
        )
        .textContent = "";

    showAuth();

    setAuthMessage(
        "Logged out successfully."
    );
}


// =========================================================
// PAGE START
// =========================================================

async function initDashboard() {

    if (!token) {

        showAuth();

        return;
    }

    const valid =
        await loadMe();

    if (valid) {

        showDashboard();

    } else {

        showAuth();
    }
}


initDashboard();

</script>

</body>

</html>
"""


# =========================================================
# DASHBOARD ROUTE
# =========================================================

@app.get(
    "/",
    response_class=HTMLResponse
)
def dashboard():

    return DASHBOARD_HTML