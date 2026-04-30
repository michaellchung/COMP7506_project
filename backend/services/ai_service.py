"""LLM service using OpenRouter with RAG from Milvus."""
from __future__ import annotations

import logging
from typing import List

log = logging.getLogger(__name__)

_HEADERS = {"HTTP-Referer": "https://notemind.app", "X-Title": "NoteMind"}


def _client():
    from openai import OpenAI
    from config import OPENROUTER_API_KEY, OPENROUTER_BASE_URL

    return OpenAI(
        api_key=OPENROUTER_API_KEY,
        base_url=OPENROUTER_BASE_URL,
        default_headers=_HEADERS,
    )


def summarize_recording(payload: dict) -> dict:
    from config import CHAT_MODEL
    from services.transcription_service import transcribe_audio

    lecture_title = payload.get("title") or "Uploaded Lecture"
    audio_path = payload.get("audio_path", "")
    transcript = payload.get("transcript", "")

    # If a file path is provided but no transcript yet, transcribe first
    if audio_path and not transcript:
        transcript = transcribe_audio(audio_path)

    if not transcript:
        return {
            "title": f"{lecture_title} Summary",
            "transcript": "",
            "summary": "No transcript available. Provide an audio file or transcript text.",
            "terms": [],
        }

    prompt = (
        f"You are an academic study assistant. Below is a transcript of an English lecture titled '{lecture_title}'.\n\n"
        f"TRANSCRIPT:\n{transcript[:6000]}\n\n"
        "Please provide a structured response with:\n"
        "1. Key Topics Outline\n"
        "2. Important Academic Terms & Definitions\n"
        "3. Likely Exam Hotspots\n\n"
        "Format with clear headers."
    )

    try:
        resp = _client().chat.completions.create(
            model=CHAT_MODEL,
            messages=[{"role": "user", "content": prompt}],
            max_tokens=1500,
        )
        summary = resp.choices[0].message.content
        _track_usage(CHAT_MODEL, resp.usage.prompt_tokens, resp.usage.completion_tokens, "recording_summary")
    except Exception as exc:
        log.error("summarize_recording failed: %s", exc)
        summary = f"AI summary unavailable. Check OPENROUTER_API_KEY in .env. Error: {exc}"

    return {
        "title": f"{lecture_title} Summary",
        "transcript": transcript,
        "summary": summary,
        "terms": [],
    }


def generate_answer(question: str, session_id: str = "default") -> dict:
    from config import CHAT_MODEL
    from database import list_recent_notes
    from services.embedding_service import get_embedding
    from services import milvus_service

    if not question:
        return {"answer": "Please ask a question.", "sources": []}

    # 1. Embed query and retrieve from Milvus (RAG)
    context_chunks: List[dict] = []
    try:
        query_embedding = get_embedding(question)
        context_chunks = milvus_service.search(query_embedding, top_k=5)
    except Exception as exc:
        log.warning("RAG retrieval failed, falling back to SQLite: %s", exc)

    # 2. Fallback to recent SQLite notes if Milvus is unavailable
    if context_chunks:
        context_text = "\n\n".join(
            f"[{c['source_type']}] {c['text']}" for c in context_chunks
        )
        sources = [
            {"text": c["text"][:120], "source_type": c["source_type"], "score": c.get("score", 0)}
            for c in context_chunks
        ]
    else:
        notes = list_recent_notes(limit=5)
        context_text = "\n\n".join(
            f"[{n['source_type']}] {n['title']}:\n{n['content']}" for n in notes
        )
        sources = [{"title": n["title"], "source_type": n["source_type"]} for n in notes]

    system_prompt = (
        "You are NoteMind, a personal AI study assistant. "
        "Answer ONLY based on the student's own notes in the context below. "
        "If the answer is not in the context, say so clearly. "
        "Be concise and academically accurate."
    )
    user_prompt = f"Context from my notes:\n{context_text}\n\nQuestion: {question}"

    try:
        resp = _client().chat.completions.create(
            model=CHAT_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            max_tokens=800,
        )
        answer = resp.choices[0].message.content
        _track_usage(CHAT_MODEL, resp.usage.prompt_tokens, resp.usage.completion_tokens, "kb_ask")
    except Exception as exc:
        log.error("generate_answer LLM call failed: %s", exc)
        answer = (
            f"Could not reach AI service. Please check your OPENROUTER_API_KEY in backend/.env.\n\nError: {exc}"
        )

    return {"answer": answer, "question": question, "sources": sources}


def _track_usage(model: str, input_tokens: int, output_tokens: int, endpoint: str) -> None:
    try:
        from database import save_api_usage

        save_api_usage(endpoint, model, input_tokens, output_tokens)
    except Exception as exc:
        log.warning("Failed to track API usage: %s", exc)
