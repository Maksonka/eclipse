import sys
import os
from faster_whisper import WhisperModel

MODEL_ID = "Systran/faster-whisper-small"
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-ct2")


def main():
    if len(sys.argv) < 2:
        sys.stderr.write("usage: whisper_local.py <wav_path>\n")
        sys.exit(2)
    wav = sys.argv[1]
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass
    model = WhisperModel(MODEL_ID, device="cpu", compute_type="int8", download_root=CACHE)
    segments, _info = model.transcribe(wav, language="ru", beam_size=5, vad_filter=True)
    text = " ".join(seg.text.strip() for seg in segments if seg.text and seg.text.strip()).strip()
    sys.stdout.write(text + "\n")
    sys.stdout.flush()


if __name__ == "__main__":
    main()