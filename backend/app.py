from flask import Flask, jsonify, request

from database import (
    init_db,
    save_note,
    create_or_update_session,
    list_sessions,
    delete_session,
    save_chat_message,
    get_total_usage,
    list_notes_full,
    list_notes_by_lecture,
    get_note_by_id,
    create_course,
    list_courses,
    get_course,
    delete_course,
    create_lecture,
    list_lectures,
    get_lecture,
    delete_lecture,
    create_user,
    get_user_by_email,
    email_exists,
    create_auth_token,
    get_user_by_token,
    delete_auth_token,
    verify_password,
)
from services.ai_service import generate_answer, summarize_recording
from services.ocr_service import extract_text
from services.embedding_service import embed_and_store
from services.ppt_service import extract_and_summarize

app = Flask(__name__)


@app.before_request
def ensure_database():
    init_db()


def _lecture_id_from(payload: dict):
    raw = payload.get("lecture_id")
    if raw in (None, "", "null"):
        return None
    try:
        return int(raw)
    except (TypeError, ValueError):
        return None


# ── Auth ──────────────────────────────────────────────────────────────────────

@app.post("/api/register")
def auth_register():
    payload  = request.get_json(silent=True) or {}
    username = (payload.get("username") or "").strip()
    email    = (payload.get("email")    or "").strip().lower()
    password = (payload.get("password") or "").strip()

    if not username:
        return jsonify({"error": "username required"}), 400
    if not email or "@" not in email:
        return jsonify({"error": "valid email required"}), 400
    if len(password) < 6:
        return jsonify({"error": "password must be at least 6 characters"}), 400
    if email_exists(email):
        return jsonify({"error": "email already registered"}), 409

    user_id = create_user(username, email, password)
    token   = create_auth_token(user_id)
    return jsonify({
        "token":    token,
        "user_id":  user_id,
        "username": username,
        "email":    email,
    }), 201


@app.post("/api/login")
def auth_login():
    payload  = request.get_json(silent=True) or {}
    email    = (payload.get("email")    or "").strip().lower()
    password = (payload.get("password") or "").strip()

    if not email or not password:
        return jsonify({"error": "email and password required"}), 400

    user = get_user_by_email(email)
    if user is None or not verify_password(password, user["password"]):
        return jsonify({"error": "invalid email or password"}), 401

    token = create_auth_token(user["id"])
    return jsonify({
        "token":    token,
        "user_id":  user["id"],
        "username": user["username"],
        "email":    user["email"],
    })



@app.post("/api/logout")
def auth_logout():
    token = request.headers.get("Authorization", "").replace("Bearer ", "").strip()
    if token:
        delete_auth_token(token)
    return jsonify({"status": "ok"})

# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return jsonify({"status": "ok", "service": "NoteMind backend"})


# ── Courses ───────────────────────────────────────────────────────────────────

@app.get("/api/courses")
def courses_list():
    return jsonify(list_courses())


@app.post("/api/courses")
def courses_create():
    payload = request.get_json(silent=True) or {}
    title = (payload.get("title") or "").strip()
    if not title:
        return jsonify({"error": "title required"}), 400
    course_id = create_course(
        title,
        (payload.get("description") or "").strip(),
        (payload.get("schedule") or "").strip(),
    )
    return jsonify(get_course(course_id)), 201


@app.get("/api/courses/<int:course_id>")
def courses_detail(course_id: int):
    course = get_course(course_id)
    if course is None:
        return jsonify({"error": "Course not found"}), 404
    course["lectures"] = list_lectures(course_id)
    return jsonify(course)


@app.delete("/api/courses/<int:course_id>")
def courses_delete(course_id: int):
    delete_course(course_id)
    return jsonify({"status": "deleted"})


# ── Lectures ──────────────────────────────────────────────────────────────────

@app.get("/api/courses/<int:course_id>/lectures")
def lectures_list(course_id: int):
    return jsonify(list_lectures(course_id))


@app.post("/api/courses/<int:course_id>/lectures")
def lectures_create(course_id: int):
    payload = request.get_json(silent=True) or {}
    if get_course(course_id) is None:
        return jsonify({"error": "Course not found"}), 404
    lecture = create_lecture(
        course_id,
        (payload.get("title") or "").strip(),
        (payload.get("lecture_date") or "").strip(),
    )
    return jsonify(lecture), 201


@app.get("/api/lectures/<int:lecture_id>")
def lectures_detail(lecture_id: int):
    lecture = get_lecture(lecture_id)
    if lecture is None:
        return jsonify({"error": "Lecture not found"}), 404
    lecture["notes"] = list_notes_by_lecture(lecture_id)
    return jsonify(lecture)


@app.delete("/api/lectures/<int:lecture_id>")
def lectures_delete(lecture_id: int):
    delete_lecture(lecture_id)
    return jsonify({"status": "deleted"})


@app.get("/api/lectures/<int:lecture_id>/notes")
def lectures_notes(lecture_id: int):
    return jsonify(list_notes_by_lecture(lecture_id))


# ── Recording ─────────────────────────────────────────────────────────────────

@app.post("/api/recording/summarize")
def recording_summary():
    payload = request.get_json(silent=True) or {}
    lecture_id = _lecture_id_from(payload)
    result = summarize_recording(payload)
    note_id = save_note("recording", result["title"], result["summary"], lecture_id)
    if result.get("transcript"):
        embed_and_store(result["transcript"], "recording", note_id)
    result["note_id"] = note_id
    result["lecture_id"] = lecture_id
    return jsonify(result)


# ── OCR ───────────────────────────────────────────────────────────────────────

@app.post("/api/ocr")
def photo_ocr():
    payload = request.get_json(silent=True) or {}
    lecture_id = _lecture_id_from(payload)
    result = extract_text(payload)
    note_id = save_note("ocr", result["title"], result["text"], lecture_id)
    if result.get("text"):
        embed_and_store(result["text"], "ocr", note_id)
    result["note_id"] = note_id
    result["lecture_id"] = lecture_id
    return jsonify(result)


# ── PPT / PDF Analysis ────────────────────────────────────────────────────────

@app.post("/api/ppt/analyze")
def ppt_analyze():
    payload = request.get_json(silent=True) or {}
    lecture_id = _lecture_id_from(payload)
    result = extract_and_summarize(payload)
    note_id = save_note("ppt", result["title"], result["summary"] or result["text"], lecture_id)
    if result.get("text"):
        embed_and_store(result["text"], "ppt", note_id)
    result["note_id"] = note_id
    result["lecture_id"] = lecture_id
    return jsonify(result)


# ── Knowledge Base Q&A ────────────────────────────────────────────────────────

@app.post("/api/kb/ask")
def knowledge_base_answer():
    payload = request.get_json(silent=True) or {}
    question = payload.get("question", "")
    session_id = payload.get("session_id", "default")

    if question:
        save_chat_message(session_id, "user", question)

    result = generate_answer(question, session_id)

    if result.get("answer"):
        save_chat_message(session_id, "assistant", result["answer"])

    return jsonify(result)


# ── Notes ─────────────────────────────────────────────────────────────────────

@app.get("/api/notes")
def get_notes():
    limit = int(request.args.get("limit", 30))
    return jsonify(list_notes_full(limit))


@app.get("/api/notes/<int:note_id>")
def get_note(note_id: int):
    note = get_note_by_id(note_id)
    if note is None:
        return jsonify({"error": "Note not found"}), 404
    return jsonify(note)


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
