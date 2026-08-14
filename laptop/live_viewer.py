import base64
import io
import tkinter as tk

import requests
from PIL import Image, ImageTk


SERVER_URL = "https://phonecontrol-black.vercel.app"

API_TOKEN = "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

HEADERS = {
    "Authorization": f"Bearer {API_TOKEN}"
}


class LiveViewer:

    def __init__(self):

        self.running = True

        self.root = tk.Tk()

        self.root.title(
            "Voice Phone Control - Live Screen"
        )

        self.root.geometry(
            "420x850"
        )

        self.root.protocol(
            "WM_DELETE_WINDOW",
            self.close
        )

        self.label = tk.Label(
            self.root,
            text="Waiting for phone...",
            anchor="center"
        )

        self.label.pack(
            fill="both",
            expand=True
        )

        self.update_frame()

        self.root.mainloop()

    def get_frame(self):

        try:

            response = requests.get(
                f"{SERVER_URL}/api/screenshot",
                headers=HEADERS,
                timeout=10
            )

            if response.status_code != 200:

                print(
                    "Server:",
                    response.status_code,
                    response.text
                )

                return None

            data = response.json()

            if not data.get("ok"):

                return None

            image_data = data.get("image")

            if not image_data:

                return None

            # Remove data:image/... prefix
            if "," in image_data:

                image_data = image_data.split(
                    ",",
                    1
                )[1]

            image_bytes = base64.b64decode(
                image_data
            )

            image = Image.open(
                io.BytesIO(image_bytes)
            )

            image.load()

            return image

        except Exception as e:

            print(
                "Frame error:",
                e
            )

            return None

    def update_frame(self):

        if not self.running:

            return

        image = self.get_frame()

        if image is not None:

            max_width = 400
            max_height = 780

            image.thumbnail(
                (
                    max_width,
                    max_height
                ),
                Image.Resampling.LANCZOS
            )

            photo = ImageTk.PhotoImage(
                image
            )

            self.label.configure(
                image=photo,
                text=""
            )

            self.label.image = photo

        self.root.after(
            350,
            self.update_frame
        )

    def close(self):

        self.running = False

        self.root.destroy()


if __name__ == "__main__":

    print(
        "================================="
    )

    print(
        "      PHONE LIVE SCREEN FOR UMESH DEVELOPER"
    )

    print(
        "================================="
    )

    print(
        "Connecting through Internet..."
    )

    LiveViewer()
