from flask import Flask, jsonify, request

from database import (
    init_db,
    save_note,
    create_or_update_session,
    list_sessions,
    delete_session,
    save_chat_message,
    get_total_usage,
)
from services.ai_service import generate_answer, summarize_recording
from services.ocr_service import extract_text
from services.embedding_service import embed_and_store

app = Flask(__name__)


@app.before_request
def ensure_database():
    init_db()


# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return jsonify({"status": "ok", "service": "NoteMind backend"})


# ── Recording ─────────────────────────────────────────────────────────────────

@app.post("/api/recording/summarize")
def recording_summary():
    payload = request.get_json(silent=True) or {}
    result = summarize_recording(payload)
    note_id = save_note("recording", result["title"], result["summary"])
    if result.get("transcript"):
        embed_and_store(result["transcript"], "recording", note_id)
    return jsonify(result)


# ── OCR ───────────────────────────────────────────────────────────────────────

@app.post("/api/ocr")
def photo_ocr():
    payload = request.get_json(silent=True) or {}
    result = extract_text(payload)
    note_id = save_note("ocr", result["title"], result["text"])
    if result.get("text"):
        embed_and_store(result["text"], "ocr", note_id)
    return jsonify(result)


# ── Knowledge Base Q&A ────────────────────────────────────────────────────────

@app.post("/api/kb/ask")
def knowledge_base_answer():
    payload = request.get_json(silent=True) or {}
    question = payload.get("question", "")
    session_id = payload.get("session_id", "default")

    # Persist messages to DB
    if question:
        save_chat_message(session_id, "user", question)

    result = generate_answer(question, session_id)

    if result.get("answer"):
        save_chat_message(session_id, "assistant", result["answer"])

    return jsonify(result)


# ── Chat Sessions ─────────────────────────────────────────────────────────────

@app.get("/api/chat/sessions")
def get_sessions():
    return jsonify(list_sessions())


@app.post("/api/chat/sessions")
def create_session():
    payload = request.get_json(silent=True) or {}
    session_id = payload.get("session_id", "")
    title = payload.get("title", "New Chat")
    if not session_id:
        return jsonify({"error": "session_id required"}), 400
    create_or_update_session(session_id, title)
    return jsonify({"status": "ok", "session_id": session_id})


@app.delete("/api/chat/sessions/<session_id>")
def remove_session(session_id: str):
    delete_session(session_id)
    return jsonify({"status": "deleted"})


# ── Profile / Usage ───────────────────────────────────────────────────────────

@app.get("/api/profile/usage")
def profile_usage():
    return jsonify(get_total_usage())


if __name__ == "__main__":
    init_db()
    app.run(host="0.0.0.0", port=5000, debug=True)
