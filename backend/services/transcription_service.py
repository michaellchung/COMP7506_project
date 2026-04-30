"""Audio transcription via OpenRouter (Whisper-compatible endpoint)."""
from __future__ import annotations

import logging

log = logging.getLogger(__name__)


def transcribe_audio(audio_path: str) -> str:
    """Transcribe a local audio file to text.

    audio_path: path to an audio file (mp3, m4a, wav, webm …)
    Returns the transcribed text string.
    """
    try:
        from openai import OpenAI
        from config import OPENROUTER_API_KEY, OPENROUTER_BASE_URL, TRANSCRIPTION_MODEL

        client = OpenAI(api_key=OPENROUTER_API_KEY, base_url=OPENROUTER_BASE_URL)
        with open(audio_path, "rb") as f:
            resp = client.audio.transcriptions.create(
                model=TRANSCRIPTION_MODEL,
                file=f,
                response_format="text",
            )
        return resp if isinstance(resp, str) else str(resp)
    except Exception as exc:
        log.error("Transcription failed: %s", exc)
        return f"Transcription unavailable. Error: {exc}"
