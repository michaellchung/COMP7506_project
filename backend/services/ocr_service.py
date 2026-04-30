"""OCR service — extracts and structures text from images using a vision LLM."""
from __future__ import annotations

import logging

log = logging.getLogger(__name__)


def extract_text(payload: dict) -> dict:
    from config import OCR_MODEL, OPENROUTER_API_KEY, OPENROUTER_BASE_URL
    from openai import OpenAI

    image_name = payload.get("image_name") or "Uploaded Image"
    image_b64 = payload.get("image_b64")  # base64-encoded JPEG/PNG

    if not image_b64:
        return {
            "title": f"{image_name} OCR Note",
            "text": "No image data provided. Send image_b64 (base64 string) in the request body.",
            "structured_note": {"definition": "", "examples": [], "review_points": []},
        }

    prompt = (
        "Extract ALL text visible in this image precisely. "
        "Then organize the content into structured study notes with the following sections:\n"
        "1. Key Definitions\n"
        "2. Examples\n"
        "3. Review Points / Exam Hotspots\n\n"
        "Use clear headings."
    )

    try:
        client = OpenAI(
            api_key=OPENROUTER_API_KEY,
            base_url=OPENROUTER_BASE_URL,
            default_headers={"HTTP-Referer": "https://notemind.app", "X-Title": "NoteMind"},
        )
        resp = client.chat.completions.create(
            model=OCR_MODEL,
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {"url": f"data:image/jpeg;base64,{image_b64}"},
                        },
                    ],
                }
            ],
            max_tokens=1500,
        )
        extracted = resp.choices[0].message.content
        from database import save_api_usage

        save_api_usage("ocr", OCR_MODEL, resp.usage.prompt_tokens, resp.usage.completion_tokens)
    except Exception as exc:
        log.error("OCR LLM call failed: %s", exc)
        extracted = f"OCR unavailable. Error: {exc}"

    return {
        "title": f"{image_name} OCR Note",
        "text": extracted,
        "structured_note": {"definition": extracted, "examples": [], "review_points": []},
    }
