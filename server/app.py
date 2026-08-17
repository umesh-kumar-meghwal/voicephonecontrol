import os


import secrets
import hashlib
from datetime import datetime, timedelta, timezone
from typing import Optional

import jwt

from dotenv import load_dotenv

from fastapi import (
    FastAPI,
    Header,
    HTTPException,
    Depends,
)
from fastapi.responses import HTMLResponse

from pydantic import BaseModel, Field

from passlib.context import CryptContext

from supabase import create_client, Client
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

security = HTTPBearer()
# =========================================================
# ENVIRONMENT
# =========================================================

load_dotenv()


SUPABASE_URL = os.getenv(
    "SUPABASE_URL"
)

SUPABASE_SERVICE_ROLE_KEY = os.getenv(
    "SUPABASE_SERVICE_ROLE_KEY"
)

JWT_SECRET = os.getenv(
    "JWT_SECRET"
)


if not SUPABASE_URL:

    raise RuntimeError(
        "SUPABASE_URL missing in .env"
    )


if not SUPABASE_SERVICE_ROLE_KEY:

    raise RuntimeError(
        "SUPABASE_SERVICE_ROLE_KEY missing in .env"
    )


if not JWT_SECRET:

    raise RuntimeError(
        "JWT_SECRET missing in .env"
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
    version="2.0.0"
)


# =========================================================
# PASSWORD / JWT
# =========================================================

pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto"
)


JWT_ALGORITHM = "HS256"

JWT_EXPIRE_MINUTES = 60 * 24


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

    payload: dict = {}


class DeviceHeartbeatRequest(BaseModel):

    device_id: str


# =========================================================
# PASSWORD FUNCTIONS
# =========================================================


def hash_password(
    password: str
) -> str:

    return pwd_context.hash(
        password
    )


def verify_password(
    password: str,
    password_hash: str
) -> bool:

    return pwd_context.verify(
        password,
        password_hash
    )


# =========================================================
# JWT
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

        "sub": user_id,

        "exp": expires_at,
    }

    return jwt.encode(
        payload,
        JWT_SECRET,
        algorithm=JWT_ALGORITHM
    )


def get_current_user(
    authorization: Optional[str] =
    Header(default=None)
):

    if not authorization:

        raise HTTPException(
            status_code=401,
            detail="Authorization required"
        )


    if not authorization.startswith(
        "Bearer "
    ):

        raise HTTPException(
            status_code=401,
            detail="Invalid authorization format"
        )


    token = authorization[
        7:
    ]


    try:

        payload = jwt.decode(
            token,
            JWT_SECRET,
            algorithms=[
                JWT_ALGORITHM
            ]
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


    user_id = payload.get(
        "sub"
    )


    if not user_id:

        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )


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
        .maybe_single()
        .execute()
    )


    if not result.data:

        raise HTTPException(
            status_code=401,
            detail="User not found"
        )


    return result.data


# =========================================================
# DEVICE TOKEN
# =========================================================


def generate_device_id():

    return (
        "VPC-"
        +
        secrets
        .token_hex(4)
        .upper()
    )


def generate_device_token():

    return (
        "vpc_"
        +
        secrets
        .token_urlsafe(32)
    )


def hash_device_token(
    token: str
) -> str:

    return hashlib.sha256(
        token.encode()
    ).hexdigest()


def authenticate_device(
    device_id: str,
    device_token: str
):

    token_hash = hash_device_token(
        device_token
    )


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
        .maybe_single()
        .execute()
    )


    if not result.data:

        raise HTTPException(
            status_code=401,
            detail="Invalid device credentials"
        )


    return result.data


# =========================================================
# HEALTH
# =========================================================


@app.get("/health")
def health():

    return {

        "ok": True,

        "service":
            "Voice Phone Control",

        "version":
            "2.0.0"
    }


# =========================================================
# SUPABASE TEST
# =========================================================


@app.get("/supabase-test")
def supabase_test():

    result = (
        supabase
        .table("users")
        .select("id")
        .limit(1)
        .execute()
    )


    return {

        "ok": True,

        "supabase":
            "connected",

        "rows":
            len(result.data)
    }


# =========================================================
# REGISTER USER
# =========================================================
@app.post("/auth/register")
def register_user(
    body: RegisterRequest
):
    username = body.username.strip().lower()

    if not username:
        raise HTTPException(
            status_code=400,
            detail="Username required"
        )

    # Check whether username already exists
    existing_response = (
        supabase
        .table("users")
        .select("id")
        .eq("username", username)
        .execute()
    )

    existing_rows = existing_response.data or []

    if len(existing_rows) > 0:
        raise HTTPException(
            status_code=409,
            detail="Username already exists"
        )

    # Hash password
    password_hash = hash_password(
        body.password
    )

    # Create user
    insert_response = (
        supabase
        .table("users")
        .insert({
            "username": username,
            "password_hash": password_hash
        })
        .execute()
    )

    inserted_rows = insert_response.data or []

    if len(inserted_rows) == 0:
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


    result = (
        supabase
        .table("users")
        .select("*")
        .eq(
            "username",
            username
        )
        .maybe_single()
        .execute()
    )


    user = result.data


    if not user:

        raise HTTPException(
            status_code=401,
            detail="Invalid username or password"
        )


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

        "message":
            "Login successful",

        "token":
            token,

        "user": {

            "id":
                user["id"],

            "username":
                user["username"]
        }
    }


# =========================================================
# CURRENT USER
# =========================================================
@app.get("/auth/me")
def current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    token = credentials.credentials

    try:
        payload = jwt.decode(
            token,
            JWT_SECRET,
            algorithms=["HS256"]
        )

        user_id = payload.get("sub")

        if not user_id:
            raise HTTPException(
                status_code=401,
                detail="Invalid token"
            )

    except JWTError:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired token"
        )

    response = (
        supabase
        .table("users")
        .select("id, username, created_at")
        .eq("id", user_id)
        .execute()
    )

    users = response.data or []

    if not users:
        raise HTTPException(
            status_code=404,
            detail="User not found"
        )

    return {
        "ok": True,
        "user": users[0]
    }

# =========================================================
# REGISTER DEVICE
# Android App -> Server
# =========================================================

@app.post("/device/register")
def register_device(
    body: DeviceRegisterRequest
):
    # Generate unique public Device ID
    device_id = generate_device_id()

    # Generate secret device token
    device_token = generate_device_token()

    # Store only hash of device token in database
    device_token_hash = hash_device_token(
        device_token
    )

    # Make sure generated device_id is unique
    for _ in range(5):

        existing = (
            supabase
            .table("devices")
            .select("id")
            .eq(
                "device_id",
                device_id
            )
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

    # Create device
    result = (
        supabase
        .table("devices")
        .insert({
            "device_id": device_id,
            "device_token_hash": device_token_hash,
            "device_name": body.device_name,
            "device_model": body.device_model,
            "android_id": body.android_id,
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

    return {
        "ok": True,
        "message": "Device registered successfully",

        # Public ID - dashboard par use hoga
        "device_id": device["device_id"],

        # Secret - Android app ko securely save karna hai
        "device_token": device_token,

        "device": {
            "id": device["id"],
            "device_id": device["device_id"],
            "device_name": device["device_name"],
            "device_model": device["device_model"],
            "online": device["online"]
        }
    }
    
# =========================================================
# CLAIM DEVICE
# =========================================================


@app.post("/devices/claim")
def claim_device(
    body: ClaimDeviceRequest,

    user=Depends(
        get_current_user
    )
):

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
        .maybe_single()
        .execute()
    )


    if not result.data:

        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )


    device = result.data


    if device["user_id"]:

        if str(
            device["user_id"]
        ) == str(
            user["id"]
        ):

            return {

                "ok": True,

                "message":
                    "Device already belongs to you"
            }


        raise HTTPException(
            status_code=409,
            detail="Device already belongs to another user"
        )


    update_result = (
        supabase
        .table("devices")
        .update({

            "user_id":
                user["id"]
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


    return {

        "ok": True,

        "message":
            "Device added successfully",

        "device_id":
            body.device_id
    }


# =========================================================
# LIST USER DEVICES
# =========================================================


@app.get("/devices")
def list_devices(
    user=Depends(
        get_current_user
    )
):

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
            len(result.data),

        "devices":
            result.data
    }


# =========================================================
# GET ONE DEVICE
# =========================================================


@app.get(
    "/devices/{device_id}"
)
def get_device(
    device_id: str,

    user=Depends(
        get_current_user
    )
):

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
        .maybe_single()
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
            result.data
    }


# =========================================================
# SEND COMMAND
# Dashboard -> Selected Device
# =========================================================


@app.post("/command")
def send_command(
    body: CommandRequest,

    user=Depends(
        get_current_user
    )
):

    command_name = (
        body.command
        .strip()
        .upper()
    )


    if command_name not in \
            ALLOWED_COMMANDS:

        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )


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
        .maybe_single()
        .execute()
    )


    if not device_result.data:

        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )


    device_uuid = (
        device_result.data["id"]
    )


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


# =========================================================
# DEVICE POLLS COMMAND
# =========================================================


@app.get("/device/command")
def device_get_command(
    device_id: str,
    device_token: str
):

    device = authenticate_device(
        device_id,
        device_token
    )


    now = datetime.now(
        timezone.utc
    ).isoformat()


    (
        supabase
        .table("devices")
        .update({

            "online":
                True,

            "last_seen":
                now
        })
        .eq(
            "id",
            device["id"]
        )
        .execute()
    )


    # Get oldest pending command
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


    if not result.data:

        return {

            "ok": True,

            "command":
                None
        }


    command = result.data[0]


    # Mark delivered
    (
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
        .execute()
    )


    return {

        "ok": True,

        "command":
            command["command"],

        "payload":
            command["payload"],

        "command_id":
            command["id"]
    }


# =========================================================
# DEVICE HEARTBEAT
# =========================================================


@app.post("/device/heartbeat")
def device_heartbeat(
    body: DeviceHeartbeatRequest,
    device_token: str
):

    device = authenticate_device(
        body.device_id,
        device_token
    )


    now = datetime.now(
        timezone.utc
    ).isoformat()


    (
        supabase
        .table("devices")
        .update({

            "online":
                True,

            "last_seen":
                now
        })
        .eq(
            "id",
            device["id"]
        )
        .execute()
    )


    return {

        "ok": True,

        "device_id":
            body.device_id,

        "online":
            True
    }


# =========================================================
# SIMPLE DASHBOARD
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

    font-family:
        Arial,
        sans-serif;

    background:
        #0b1020;

    color:
        white;
}

.container {

    width: 92%;

    max-width:
        1100px;

    margin:
        40px auto;
}

.card {

    background:
        #151c32;

    border:
        1px solid #283452;

    border-radius:
        18px;

    padding:
        25px;

    margin-bottom:
        20px;
}

input {

    width: 100%;

    padding:
        13px;

    margin:
        7px 0;

    border-radius:
        10px;

    border:
        1px solid #303b5c;

    background:
        #0d1428;

    color:
        white;
}

button {

    padding:
        11px 17px;

    border:
        0;

    border-radius:
        10px;

    cursor:
        pointer;

    font-weight:
        bold;

    margin:
        4px;
}

.device {

    padding:
        18px;

    border:
        1px solid #303b5c;

    border-radius:
        14px;

    margin-top:
        14px;
}

.online {

    color:
        #4dff88;
}

.offline {

    color:
        #ff6464;
}

.hidden {

    display:
        none;
}

.commands {

    display:
        flex;

    flex-wrap:
        wrap;

    margin-top:
        15px;
}

.message {

    margin-top:
        12px;
}

</style>

</head>


<body>


<div class="container">


<!-- =====================================================
     AUTH
====================================================== -->

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



<!-- =====================================================
     DASHBOARD
====================================================== -->

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
<strong
id="usernameDisplay"
>
</strong>
</p>


<button
onclick="logout()"
>
Logout
</button>

</div>



<!-- =====================================================
     ADD DEVICE
====================================================== -->

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


<button
onclick="claimDevice()"
>
Add Device
</button>


<p
id="deviceMessage"
class="message"
></p>

</div>



<!-- =====================================================
     DEVICES
====================================================== -->

<div class="card">

<h2>
My Devices
</h2>


<button
onclick="loadDevices()"
>
Refresh
</button>


<div
id="devices"
>
Loading...
</div>

</div>


</div>

</div>


<script>


const API = "";


let token =
localStorage.getItem(
    "vpc_token"
);



function authHeaders() {

    return {

        "Content-Type":
            "application/json",

        "Authorization":
            "Bearer " + token
    };
}



function showDashboard() {

    document
        .getElementById(
            "authBox"
        )
        .classList
        .add("hidden");


    document
        .getElementById(
            "dashboardBox"
        )
        .classList
        .remove("hidden");


    loadMe();

    loadDevices();
}



function showAuth() {

    document
        .getElementById(
            "authBox"
        )
        .classList
        .remove("hidden");


    document
        .getElementById(
            "dashboardBox"
        )
        .classList
        .add("hidden");
}



async function registerUser() {

    const username =
        document
        .getElementById(
            "username"
        )
        .value
        .trim();


    const password =
        document
        .getElementById(
            "password"
        )
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
                API +
                "/auth/register",
                {

                    method:
                        "POST",

                    headers: {

                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({

                            username:
                                username,

                            password:
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


        token =
            data.token;


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



async function loginUser() {

    const username =
        document
        .getElementById(
            "username"
        )
        .value
        .trim();


    const password =
        document
        .getElementById(
            "password"
        )
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
                API +
                "/auth/login",
                {

                    method:
                        "POST",

                    headers: {

                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({

                            username:
                                username,

                            password:
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


        token =
            data.token;


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

    try {

        const response =
            await fetch(
                API +
                "/auth/me",
                {

                    headers: {

                        "Authorization":
                            "Bearer " +
                            token
                    }
                }
            );


        if (!response.ok) {

            logout();

            return;
        }


        const data =
            await response.json();


        document
            .getElementById(
                "usernameDisplay"
            )
            .textContent =
            data.user.username;

    } catch (error) {

        logout();
    }
}



async function claimDevice() {

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
                API +
                "/devices/claim",
                {

                    method:
                        "POST",

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


    container.innerHTML =
        "Loading...";


    try {

        const response =
            await fetch(
                API +
                "/devices",
                {

                    headers: {

                        "Authorization":
                            "Bearer " +
                            token
                    }
                }
            );


        if (!response.ok) {

            if (
                response.status ===
                401
            ) {

                logout();

                return;
            }


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
                            '${device.device_id}',
                            'HOME'
                        )"
                        >
                            HOME
                        </button>


                        <button
                        onclick="sendCommand(
                            '${device.device_id}',
                            'BACK'
                        )"
                        >
                            BACK
                        </button>


                        <button
                        onclick="sendCommand(
                            '${device.device_id}',
                            'VOLUME_UP'
                        )"
                        >
                            VOL +
                        </button>


                        <button
                        onclick="sendCommand(
                            '${device.device_id}',
                            'VOLUME_DOWN'
                        )"
                        >
                            VOL -
                        </button>


                        <button
                        onclick="sendCommand(
                            '${device.device_id}',
                            'TAKE_SCREENSHOT'
                        )"
                        >
                            SCREENSHOT
                        </button>


                        <button
                        onclick="sendCommand(
                            '${device.device_id}',
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

    try {

        const response =
            await fetch(
                API +
                "/command",
                {

                    method:
                        "POST",

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

    localStorage.removeItem(
        "vpc_token"
    );

    token = null;

    showAuth();
}



if (token) {

    showDashboard();

} else {

    showAuth();
}


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
