def parse_command(text: str):
    t = text.lower().strip()

    # YouTube
    if (
        ("youtube" in t or "यूट्यूब" in t)
        and ("khol" in t or "open" in t or "खोल" in t)
    ):
        return "OPEN_APP", {
            "package": "com.google.android.youtube"
        }

    # Chrome
    if (
        ("chrome" in t or "क्रोम" in t)
        and ("khol" in t or "open" in t or "खोल" in t)
    ):
        return "OPEN_APP", {
            "package": "com.android.chrome"
        }

    # Back
    if "back" in t or "पीछे" in t or "वापस" in t:
        return "BACK", {}

    # Home
    if "home" in t or "होम" in t:
        return "HOME", {}

    # Volume Up
    if (
        "volume" in t
        or "वॉल्यूम" in t
        or "आवाज़" in t
        or "आवाज" in t
    ):
        if (
            "badha" in t
            or "बढ़ा" in t
            or "बढ़ाओ" in t
            or "up" in t
            or "ज्यादा" in t
        ):
            return "VOLUME_UP", {}

        if (
            "kam" in t
            or "कम" in t
            or "घटाओ" in t
            or "down" in t
        ):
            return "VOLUME_DOWN", {}

    # Screenshot
    if (
        "screenshot" in t
        or "स्क्रीनशॉट" in t
        or "स्क्रीन शॉट" in t
    ):
        return "TAKE_SCREENSHOT", {}

    return None, {}