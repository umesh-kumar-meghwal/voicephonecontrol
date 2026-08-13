from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
import os

app = FastAPI(title="Voice Phone Control Server")

API_TOKEN = os.getenv("API_TOKEN", "change-me")


class Command(BaseModel):
    command: str
    payload: dict = {}


ALLOWED = {
    "OPEN_APP",
    "BACK",
    "HOME",
    "VOLUME_UP",
    "VOLUME_DOWN",
    "TAKE_SCREENSHOT",
}


pending_command = None


@app.get("/health")
def health():
    return {"ok": True}


# Laptop -> Server
@app.post("/command")
def send_command(
    body: Command,
    authorization: str | None = Header(default=None)
):
    global pending_command

    if authorization != f"Bearer {API_TOKEN}":
        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )

    if body.command not in ALLOWED:
        raise HTTPException(
            status_code=400,
            detail="Command not allowed"
        )

    pending_command = {
        "command": body.command,
        "payload": body.payload
    }

    return {
        "ok": True,
        "message": "Command queued",
        "command": body.command
    }


# Android -> Server
@app.get("/command")
def get_command(
    authorization: str | None = Header(default=None)
):
    global pending_command

    if authorization != f"Bearer {API_TOKEN}":
        raise HTTPException(
            status_code=401,
            detail="Unauthorized"
        )

    command = pending_command
    pending_command = None

    if command is None:
        return {
            "ok": True,
            "command": None
        }

    return {
        "ok": True,
        **command
    }