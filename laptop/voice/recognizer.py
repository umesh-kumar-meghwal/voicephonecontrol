import speech_recognition as sr


def listen():
    r = sr.Recognizer()

    try:
        print("Opening microphone...")

        with sr.Microphone(device_index=1) as source:
            print("Listening...")

            r.adjust_for_ambient_noise(source, duration=0.5)

            audio = r.listen(
                source,
                timeout=5,
                phrase_time_limit=5
            )

        print("Recognizing...")

        text = r.recognize_google(
            audio,
            language="hi-IN"
        )

        return text

    except Exception as e:
        print("MIC ERROR:", repr(e))
        raise