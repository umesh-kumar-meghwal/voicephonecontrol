from typing import Optional
import hashlib
import base64
import uuid

from fastapi import Header, HTTPException
from pydantic import BaseModel

from server.app import app, get_current_user, supabase


# =========================================================
# SCREENSHOT REQUEST MODEL
# =========================================================

class ScreenshotUpload(BaseModel):
    filename: str
    image: str


# =========================================================
# DEVICE TOKEN AUTH
# =========================================================

def authenticate_android_device(authorization: Optional[str]):
    if not authorization:
        raise HTTPException(
            status_code=401,
            detail="Authorization header missing"
        )

    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=401,
            detail="Invalid authorization format"
        )

    token = authorization.replace("Bearer ", "", 1).strip()

    if not token:
        raise HTTPException(
            status_code=401,
            detail="Device token missing"
        )

    token_hash = hashlib.sha256(
        token.encode("utf-8")
    ).hexdigest()

    result = (
        supabase
        .table("devices")
        .select("device_id,user_id")
        .eq("device_token_hash", token_hash)
        .limit(1)
        .execute()
    )

    if not result.data:
        raise HTTPException(
            status_code=401,
            detail="Invalid device token"
        )

    return result.data[0]


# =========================================================
# SCREENSHOT UPLOAD
# =========================================================

@app.post("/api/screenshot")
def upload_screenshot(
    data: ScreenshotUpload,
    authorization: Optional[str] = Header(None)
):
    device = authenticate_android_device(authorization)

    image = data.image.strip()

    if not image.startswith("data:image/"):
        raise HTTPException(
            status_code=400,
            detail="Invalid image data"
        )

    if len(image) > 15_000_000:
        raise HTTPException(
            status_code=413,
            detail="Screenshot too large"
        )

    try:
        # -------------------------------------------------
        # Separate base64 data from data URL
        # -------------------------------------------------

        if "," not in image:
            raise HTTPException(
                status_code=400,
                detail="Invalid image format"
            )

        header, encoded_image = image.split(",", 1)

        # -------------------------------------------------
        # Decode base64
        # -------------------------------------------------

        image_bytes = base64.b64decode(encoded_image)

        if not image_bytes:
            raise HTTPException(
                status_code=400,
                detail="Empty screenshot"
            )

        # -------------------------------------------------
        # Generate unique Storage path
        # -------------------------------------------------

        extension = "jpg"

        if "png" in header.lower():
            extension = "png"

        storage_filename = (
            f"{device['device_id']}/"
            f"{uuid.uuid4().hex}.{extension}"
        )

        # -------------------------------------------------
        # Upload to Supabase Storage
        # -------------------------------------------------

        content_type = (
            "image/png"
            if extension == "png"
            else "image/jpeg"
        )

        storage_result = (
            supabase
            .storage
            .from_("screenshots")
            .upload(
                storage_filename,
                image_bytes,
                {
                    "content-type": content_type,
                    "upsert": "false"
                }
            )
        )

        print(
            "[SCREENSHOT STORAGE UPLOAD]",
            storage_filename
        )

        # -------------------------------------------------
        # Get public URL
        # -------------------------------------------------

        public_url = (
            supabase
            .storage
            .from_("screenshots")
            .get_public_url(storage_filename)
        )

        # -------------------------------------------------
        # Save URL in database
        # -------------------------------------------------

        result = (
            supabase
            .table("screenshots")
            .insert({
                "filename": data.filename,
                "image": public_url,
                "user_id": device["user_id"],
                "device_id": device["device_id"],
            })
            .execute()
        )

        # -------------------------------------------------
        # Keep latest 10 screenshots for this device
        # -------------------------------------------------

        old_rows = (
            supabase
            .table("screenshots")
            .select("id")
            .eq("device_id", device["device_id"])
            .order("created_at", desc=True)
            .execute()
        )

        rows = old_rows.data or []

        if len(rows) > 10:

            delete_ids = [
                row["id"]
                for row in rows[10:]
                if row.get("id") is not None
            ]

            for screenshot_id in delete_ids:
                (
                    supabase
                    .table("screenshots")
                    .delete()
                    .eq("id", screenshot_id)
                    .execute()
                )

        return {
            "ok": True,
            "message": "Screenshot uploaded to Storage",
            "filename": data.filename,
            "storage_path": storage_filename,
            "image_url": public_url,
            "saved": bool(result.data),
        }

    except HTTPException:
        raise

    except Exception as e:
        print(
            "[SCREENSHOT UPLOAD ERROR]",
            repr(e)
        )

        raise HTTPException(
            status_code=500,
            detail="Failed to save screenshot"
        )


# =========================================================
# LATEST SCREENSHOT
# =========================================================

@app.get("/api/screenshot/latest")
def latest_screenshot(
    current_user=__import__(
        "server.app",
        fromlist=["get_current_user"]
    ).get_current_user
):
    try:
        user = current_user

        rows = (
            supabase
            .table("screenshots")
            .select(
                "id,filename,image,device_id,created_at"
            )
            .eq("user_id", user["id"])
            .order("created_at", desc=True)
            .limit(1)
            .execute()
        )

        if not rows.data:
            return {
                "ok": True,
                "screenshot": None
            }

        return {
            "ok": True,
            "screenshot": rows.data[0]
        }

    except Exception as e:
        print(
            "[LATEST SCREENSHOT ERROR]",
            repr(e)
        )

        raise HTTPException(
            status_code=500,
            detail="Failed to fetch screenshot"
        )


# =========================================================
# LATEST PHONE STATUS
# =========================================================

@app.get("/api/phone-status/latest")
def latest_phone_status(
    current_user=__import__(
        "server.app",
        fromlist=["get_current_user"]
    ).get_current_user
):
    try:
        user = current_user

        rows = (
            supabase
            .table("phone_status")
            .select("*")
            .eq("user_id", user["id"])
            .order("created_at", desc=True)
            .limit(1)
            .execute()
        )

        if not rows.data:
            return {
                "ok": True,
                "status": None
            }

        return {
            "ok": True,
            "status": rows.data[0]
        }

    except Exception as e:
        print(
            "[PHONE STATUS ERROR]",
            repr(e)
        )

        raise HTTPException(
            status_code=500,
            detail="Failed to fetch phone status"
        )