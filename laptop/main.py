import os
import base64
import subprocess
import sys
import time

def start_live_viewer():
    viewer_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "live_viewer.py"
    )

    if not os.path.exists(viewer_path):
        print("Live viewer file not found:", viewer_path)
        return False

    try:
        subprocess.Popen(
            [sys.executable, viewer_path],
            cwd=os.path.dirname(viewer_path)
        )

        print("Live viewer window opened.")
        return True

    except Exception as e:
        print("Live viewer start failed:", e)
        return False
from network.client import (
    send_command,
    get_screenshot,
    get_phone_status
)


# =========================================
# COMMANDS
# =========================================

COMMANDS = {
    "1": ("HOME", {}),
    "2": ("BACK", {}),
    "3": ("VOLUME_UP", {}),
    "4": ("VOLUME_DOWN", {}),
    "5": ("TAKE_SCREENSHOT", {}),
    "6": ("PHONE_STATUS", {}),
    "7": ("OPEN_APP", {}),
    "8": ("LIVE_SCREEN", {}),
}


# =========================================
# SAVE SCREENSHOT
# =========================================

def save_screenshot(image_data, filename):

    os.makedirs(
        "screenshots",
        exist_ok=True
    )

    # Remove data URI prefix if present
    if "," in image_data:
        image_data = image_data.split(
            ",",
            1
        )[1]

    image_bytes = base64.b64decode(
        image_data
    )

    filepath = os.path.join(
        "screenshots",
        filename
    )

    with open(
        filepath,
        "wb"
    ) as file:
        file.write(image_bytes)

    return filepath


# =========================================
# WAIT FOR PHONE STATUS
# =========================================

def wait_for_phone_status(
    attempts=15,
    delay=1
):

    for _ in range(attempts):

        status = get_phone_status()

        if status is not None:
            return status

        time.sleep(delay)

    return None


# =========================================
# MAIN LOOP
# =========================================

while True:

    print()
    print("==============================")
    print("      VOICE PHONE CONTROL")
    print("==============================")

    print("1. HOME")
    print("2. BACK")
    print("3. VOLUME UP")
    print("4. VOLUME DOWN")
    print("5. SCREENSHOT")
    print("6. PHONE STATUS")
    print("7. OPEN APP")
    print("8. LIVE SCREEN")
    print("q. EXIT")

    print("==============================")

    choice = input(
        "Command type karo: "
    ).strip().lower()


    # =====================================
    # EXIT
    # =====================================

    if choice == "q":

        print("Exiting...")
        break


    # =====================================
    # OPEN APP
    # =====================================

    if choice == "7":

        app_name = input(
            "App name "
            "(WhatsApp/YouTube/Chrome/Settings/Camera): "
        ).strip()

        if not app_name:

            print(
                "App name required."
            )

            continue

        try:

            result = send_command(
                "OPEN_APP",
                {
                    "app": app_name
                }
            )

            print(
                "Server:",
                result
            )

            print(
                f"Opening {app_name}..."
            )

        except Exception as e:

            print(
                "Error:",
                e
            )

        continue


    # =====================================
    # COMMAND VALIDATION
    # =====================================

    if choice not in COMMANDS:

        print(
            "Invalid option."
        )

        continue


    command, payload = COMMANDS[
        choice
    ]


    try:

        # =================================
        # SEND COMMAND
        # =================================

        result = send_command(
            command,
            payload
        )

        print(
            "Server:",
            result
        )


        # =================================
        # LIVE SCREEN
        # =================================

      if command == "LIVE_SCREEN":

    print()
    print("Starting live phone screen...")
    print("Waiting for Android MediaProjection permission...")

    start_live_viewer()

    print("Live viewer is running.")
    print("Close the Live Screen window when finished.")

    continue


        # =================================
        # SCREENSHOT
        # =================================

        if command == "TAKE_SCREENSHOT":

            print(
                "Waiting for screenshot..."
            )

            screenshot = None

            for _ in range(15):

                screenshot = get_screenshot()

                if screenshot:
                    break

                time.sleep(1)


            if screenshot:

                filepath = save_screenshot(
                    screenshot["image"],
                    screenshot["filename"]
                )

                print(
                    f"Screenshot saved: {filepath}"
                )

            else:

                print(
                    "Screenshot receive nahi hua."
                )


        # =================================
        # PHONE STATUS
        # =================================

        elif command == "PHONE_STATUS":

            print(
                "\nWaiting for phone status..."
            )

            status = wait_for_phone_status()


            if status:

                print()

                print(
                    "📱 PHONE STATUS"
                )

                print(
                    "=============================="
                )

                print(
                    f"Battery   : "
                    f"{status['battery']}%"
                )

                print(
                    f"Charging  : "
                    f"{'Yes' if status['charging'] else 'No'}"
                )

                print(
                    f"Network   : "
                    f"{'Connected' if status['network'] else 'Disconnected'}"
                )

                print(
                    f"Android   : "
                    f"{status['android']}"
                )

                print(
                    "=============================="
                )


            else:

                print(
                    "Phone status receive nahi hua."
                )


    except Exception as e:

        print(
            "Error:",
            e
        )