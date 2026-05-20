import subprocess
import tempfile
import os


def extract_transcript(url: str) -> str:
    try:
        temp_dir = tempfile.mkdtemp()

        command = [
            "yt-dlp",
            "--write-auto-sub",
            "--skip-download",
            "--sub-lang", "en",
            "--output", os.path.join(temp_dir, "%(title)s.%(ext)s"),
            url
        ]

        subprocess.run(command, check=True)

        # ------------------find .vtt file-------------------
        
        
        for file in os.listdir(temp_dir):
            if file.endswith(".vtt"):
                with open(os.path.join(temp_dir, file), "r", encoding="utf-8") as f:
                    return f.read()

        raise Exception("No subtitle file found")

    except Exception as e:
        print("TRANSCRIPT ERROR:", str(e))

        
        
        #  ----------------fallback--------------------------
        return "Transcript not available due to YouTube restrictions."