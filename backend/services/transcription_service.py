"""Audio transcription via OpenRouter chat-completions with audio input.

OpenRouter does not expose Whisper's `/v1/audio/transcriptions` endpoint, but it
does proxy multimodal models that accept audio inside chat messages
(e.g. ``openai/gpt-4o-mini-audio-preview``). Those models only accept ``wav`` or
``mp3`` payloads, so any other format (m4a/aac/ogg/...) is transcoded to mp3 via
the bundled ``imageio-ffmpeg`` binary first.
"""
from __future__ import annotations

import base64
import logging
import os
import subprocess
import tempfile

log = logging.getLogger(__name__)

_ACCEPTED_FORMATS = {"mp3", "wav"}


def transcribe_audio(audio_path: str) -> str:
    """Transcribe a local audio file to text using OpenRouter."""
    try:
        from openai import OpenAI
        from config import (
            OPENROUTER_API_KEY,
            OPENROUTER_BASE_URL,
            TRANSCRIPTION_MODEL,
        )

        if not OPENROUTER_API_KEY:
            return "Transcription unavailable: OPENROUTER_API_KEY is empty in backend/.env."

        mp3_path, fmt, cleanup = _ensure_mp3_or_wav(audio_path)

        try:
            with open(mp3_path, "rb") as f:
                audio_b64 = base64.b64encode(f.read()).decode("utf-8")

            client = OpenAI(
                api_key=OPENROUTER_API_KEY,
                base_url=OPENROUTER_BASE_URL,
                default_headers={
                    "HTTP-Referer": "https://notemind.app",
                    "X-Title": "NoteMind",
                },
            )

            resp = client.chat.completions.create(
                model=TRANSCRIPTION_MODEL,
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": (
                                    "Transcribe this audio verbatim. "
                                    "Output ONLY the transcript text without any "
                                    "commentary, headers, or speaker labels."
                                ),
                            },
                            {
                                "type": "input_audio",
                                "input_audio": {"data": audio_b64, "format": fmt},
                            },
                        ],
                    }
                ],
                max_tokens=4000,
            )

            text = (resp.choices[0].message.content or "").strip()

            try:
                from database import save_api_usage

                if resp.usage:
                    save_api_usage(
                        "transcription",
                        TRANSCRIPTION_MODEL,
                        resp.usage.prompt_tokens or 0,
                        resp.usage.completion_tokens or 0,
                    )
            except Exception as usage_exc:  # noqa: BLE001
                log.warning("usage tracking failed: %s", usage_exc)

            return text
        finally:
            if cleanup and mp3_path and os.path.exists(mp3_path):
                try:
                    os.unlink(mp3_path)
                except OSError:
                    pass

    except Exception as exc:  # noqa: BLE001
        log.error("Transcription failed: %s", exc)
        return f"Transcription failed: {exc}"


# ── Internal helpers ────────────────────────────────────────────────────────


def _ensure_mp3_or_wav(audio_path: str) -> tuple[str, str, bool]:
    """Return (path, format_label, cleanup) — converting to mp3 if necessary."""
    ext = audio_path.rsplit(".", 1)[-1].lower() if "." in audio_path else ""
    if ext in _ACCEPTED_FORMATS:
        return audio_path, ext, False

    ffmpeg_path = _resolve_ffmpeg()
    if ffmpeg_path is None:
        raise RuntimeError(
            "ffmpeg not available. Install it system-wide or `pip install imageio-ffmpeg`."
        )

    out_fd, out_path = tempfile.mkstemp(suffix=".mp3")
    os.close(out_fd)

    try:
        subprocess.run(
            [
                ffmpeg_path,
                "-y",
                "-loglevel",
                "error",
                "-i",
                audio_path,
                "-ac",
                "1",          # mono
                "-ar",
                "16000",      # 16 kHz — fine for speech recognition
                "-codec:a",
                "libmp3lame",
                "-b:a",
                "64k",
                out_path,
            ],
            check=True,
        )
    except subprocess.CalledProcessError as exc:
        if os.path.exists(out_path):
            os.unlink(out_path)
        raise RuntimeError(f"ffmpeg conversion failed: {exc}") from exc

    return out_path, "mp3", True


def _resolve_ffmpeg() -> str | None:
    try:
        import imageio_ffmpeg

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:  # noqa: BLE001
        pass

    from shutil import which

    return which("ffmpeg")
