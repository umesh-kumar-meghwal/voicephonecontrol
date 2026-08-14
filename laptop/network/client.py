import requests


SERVER_URL = "https://phonecontrol-black.vercel.app"

API_TOKEN = "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"


def get_headers():

    return {
        "Authorization": f"Bearer {API_TOKEN}",
        "Content-Type": "application/json"
    }


def send_command(
    command,
    payload=None
):

    if payload is None:
        payload = {}

    url = f"{SERVER_URL}/api/command"

    data = {
        "command": command,
        "payload": payload
    }

    response = requests.post(
        url,
        json=data,
        headers=get_headers(),
        timeout=10
    )

    response.raise_for_status()

    return response.json()


def get_screenshot():

    url = f"{SERVER_URL}/api/screenshot"

    response = requests.get(
        url,
        headers=get_headers(),
        timeout=30
    )

    response.raise_for_status()

    data = response.json()

    if not data.get("ok"):
        return None

    return data


def get_phone_status():

    url = f"{SERVER_URL}/api/status"

    response = requests.get(
        url,
        headers=get_headers(),
        timeout=10
    )

    response.raise_for_status()

    data = response.json()

    if not data.get("ok"):
        return None

    return data.get("status")
