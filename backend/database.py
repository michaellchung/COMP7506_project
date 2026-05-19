import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).with_name("notemind.db")


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db():
    with get_connection() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS courses (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                title       TEXT NOT NULL,
                description TEXT,
                schedule    TEXT,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS lectures (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id       INTEGER NOT NULL,
                lecture_number  INTEGER NOT NULL,
                title           TEXT,
                lecture_date    TEXT,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS notes (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                source_type TEXT NOT NULL,
                title       TEXT NOT NULL,
                content     TEXT NOT NULL,
                lecture_id  INTEGER,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (lecture_id) REFERENCES lectures(id) ON DELETE SET NULL
            );

            CREATE TABLE IF NOT EXISTS chat_sessions (
                session_id  TEXT PRIMARY KEY,
                title       TEXT NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS chat_messages (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id  TEXT NOT NULL,
                role        TEXT NOT NULL,
                content     TEXT NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id)
            );

            CREATE TABLE IF NOT EXISTS api_usage (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                endpoint      TEXT NOT NULL,
                model         TEXT NOT NULL,
                input_tokens  INTEGER DEFAULT 0,
                output_tokens INTEGER DEFAULT 0,
                created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
	    CREATE TABLE IF NOT EXISTS users (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                username    TEXT NOT NULL,
                email       TEXT NOT NULL UNIQUE,
                password    TEXT NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS auth_tokens (
                token       TEXT PRIMARY KEY,
                user_id     INTEGER NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """
        )
        # Best-effort migration: add lecture_id to existing notes table
        try:
            conn.execute("ALTER TABLE notes ADD COLUMN lecture_id INTEGER")
        except sqlite3.OperationalError:
            pass
        conn.commit()


# ── Courses ──────────────────────────────────────────────────────────────────

def create_course(title: str, description: str = "", schedule: str = "") -> int:
    with get_connection() as conn:
        cur = conn.execute(
            "INSERT INTO courses (title, description, schedule) VALUES (?, ?, ?)",
            (title, description, schedule),
        )
        conn.commit()
        return cur.lastrowid


def list_courses() -> list:
    with get_connection() as conn:
        rows = conn.execute(
            """
            SELECT c.id, c.title, c.description, c.schedule, c.created_at,
                   (SELECT COUNT(*) FROM lectures l WHERE l.course_id = c.id) AS lecture_count
            FROM courses c
            ORDER BY c.created_at DESC
            """
        ).fetchall()
    return [
        {
            "id": r[0],
            "title": r[1],
            "description": r[2] or "",
            "schedule": r[3] or "",
            "created_at": str(r[4]),
            "lecture_count": r[5] or 0,
        }
        for r in rows
    ]


def get_course(course_id: int):
    with get_connection() as conn:
        row = conn.execute(
            "SELECT id, title, description, schedule, created_at FROM courses WHERE id=?",
            (course_id,),
        ).fetchone()
    if not row:
        return None
    return {
        "id": row[0],
        "title": row[1],
        "description": row[2] or "",
        "schedule": row[3] or "",
        "created_at": str(row[4]),
    }


def delete_course(course_id: int):
    with get_connection() as conn:
        conn.execute("DELETE FROM courses WHERE id=?", (course_id,))
        conn.commit()


# ── Lectures ─────────────────────────────────────────────────────────────────

def create_lecture(course_id: int, title: str = "", lecture_date: str = "") -> dict:
    with get_connection() as conn:
        next_num = conn.execute(
            "SELECT COALESCE(MAX(lecture_number), 0) + 1 FROM lectures WHERE course_id=?",
            (course_id,),
        ).fetchone()[0]
        cur = conn.execute(
            "INSERT INTO lectures (course_id, lecture_number, title, lecture_date) VALUES (?, ?, ?, ?)",
            (course_id, next_num, title or f"Lecture {next_num}", lecture_date),
        )
        conn.commit()
        new_id = cur.lastrowid
    return {
        "id": new_id,
        "course_id": course_id,
        "lecture_number": next_num,
        "title": title or f"Lecture {next_num}",
        "lecture_date": lecture_date,
    }


def list_lectures(course_id: int) -> list:
    with get_connection() as conn:
        rows = conn.execute(
            """
            SELECT l.id, l.course_id, l.lecture_number, l.title, l.lecture_date, l.created_at,
                   (SELECT COUNT(*) FROM notes n WHERE n.lecture_id = l.id) AS note_count
            FROM lectures l
            WHERE l.course_id = ?
            ORDER BY l.lecture_number DESC
            """,
            (course_id,),
        ).fetchall()
    return [
        {
            "id": r[0],
            "course_id": r[1],
            "lecture_number": r[2],
            "title": r[3] or f"Lecture {r[2]}",
            "lecture_date": r[4] or "",
            "created_at": str(r[5]),
            "note_count": r[6] or 0,
        }
        for r in rows
    ]


def get_lecture(lecture_id: int):
    with get_connection() as conn:
        row = conn.execute(
            """
            SELECT l.id, l.course_id, l.lecture_number, l.title, l.lecture_date, l.created_at,
                   c.title AS course_title
            FROM lectures l JOIN courses c ON c.id = l.course_id
            WHERE l.id = ?
            """,
            (lecture_id,),
        ).fetchone()
    if not row:
        return None
    return {
        "id": row[0],
        "course_id": row[1],
        "lecture_number": row[2],
        "title": row[3] or f"Lecture {row[2]}",
        "lecture_date": row[4] or "",
        "created_at": str(row[5]),
        "course_title": row[6],
    }


def delete_lecture(lecture_id: int):
    with get_connection() as conn:
        conn.execute("DELETE FROM lectures WHERE id=?", (lecture_id,))
        conn.commit()


# ── Notes ────────────────────────────────────────────────────────────────────

def save_note(source_type, title, content, lecture_id: object = None):
    with get_connection() as conn:
        cursor = conn.execute(
            "INSERT INTO notes (source_type, title, content, lecture_id) VALUES (?, ?, ?, ?)",
            (source_type, title, content, lecture_id),
        )
        conn.commit()
        return cursor.lastrowid


def list_notes_full(limit: int = 30) -> list:
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT id, source_type, title, content, lecture_id, created_at FROM notes ORDER BY created_at DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [
        {
            "id": r[0],
            "source_type": r[1],
            "title": r[2],
            "content": r[3],
            "lecture_id": r[4],
            "created_at": str(r[5]),
        }
        for r in rows
    ]


def list_notes_by_lecture(lecture_id: int) -> list:
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT id, source_type, title, content, created_at FROM notes WHERE lecture_id=? ORDER BY created_at DESC",
            (lecture_id,),
        ).fetchall()
    return [
        {
            "id": r[0],
            "source_type": r[1],
            "title": r[2],
            "content": r[3],
            "lecture_id": lecture_id,
            "created_at": str(r[4]),
        }
        for r in rows
    ]


def get_note_by_id(note_id: int):
    with get_connection() as conn:
        row = conn.execute(
            "SELECT id, source_type, title, content, lecture_id, created_at FROM notes WHERE id=?",
            (note_id,),
        ).fetchone()
    if row:
        return {
            "id": row[0],
            "source_type": row[1],
            "title": row[2],
            "content": row[3],
            "lecture_id": row[4],
            "created_at": str(row[5]),
        }
    return None


def get_note_scope(note_id: int):
    with get_connection() as conn:
        row = conn.execute(
            """
            SELECT n.id, n.lecture_id, l.course_id
            FROM notes n
            LEFT JOIN lectures l ON l.id = n.lecture_id
            WHERE n.id = ?
            """,
            (note_id,),
        ).fetchone()
    if not row:
        return None
    return {
        "note_id": row[0],
        "lecture_id": row[1],
        "course_id": row[2],
    }


def note_matches_scope(note_id: int, course_id: object = None, lecture_id: object = None) -> bool:
    scope = get_note_scope(note_id)
    if scope is None:
        return False
    if lecture_id is not None and scope["lecture_id"] != lecture_id:
        return False
    if course_id is not None and scope["course_id"] != course_id:
        return False
    return True


def list_recent_notes(limit=5, course_id: object = None, lecture_id: object = None):
    filters = []
    params = []
    if lecture_id is not None:
        filters.append("n.lecture_id = ?")
        params.append(lecture_id)
    if course_id is not None:
        filters.append("l.course_id = ?")
        params.append(course_id)
    where = f"WHERE {' AND '.join(filters)}" if filters else ""
    params.append(limit)

    with get_connection() as conn:
        rows = conn.execute(
            f"""
            SELECT n.id, n.source_type, n.title, n.content, n.lecture_id, l.course_id
            FROM notes n
            LEFT JOIN lectures l ON l.id = n.lecture_id
            {where}
            ORDER BY n.created_at DESC
            LIMIT ?
            """,
            params,
        ).fetchall()
    return [
        {
            "id": r[0],
            "source_type": r[1],
            "title": r[2],
            "content": r[3],
            "lecture_id": r[4],
            "course_id": r[5],
        }
        for r in rows
    ]


# ── Chat sessions ─────────────────────────────────────────────────────────────

def create_or_update_session(session_id: str, title: str):
    with get_connection() as conn:
        conn.execute(
            """
            INSERT INTO chat_sessions (session_id, title)
            VALUES (?, ?)
            ON CONFLICT(session_id) DO UPDATE SET title=excluded.title,
                updated_at=CURRENT_TIMESTAMP
            """,
            (session_id, title),
        )
        conn.commit()


def list_sessions():
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT session_id, title, created_at, updated_at FROM chat_sessions ORDER BY updated_at DESC"
        ).fetchall()
    return [{"session_id": r[0], "title": r[1], "created_at": r[2], "updated_at": r[3]} for r in rows]


def delete_session(session_id: str):
    with get_connection() as conn:
        conn.execute("DELETE FROM chat_messages WHERE session_id=?", (session_id,))
        conn.execute("DELETE FROM chat_sessions WHERE session_id=?", (session_id,))
        conn.commit()


def save_chat_message(session_id: str, role: str, content: str):
    with get_connection() as conn:
        # Ensure session row exists first (FK requirement); the Android client
        # generates its own UUIDs without explicitly registering them.
        conn.execute(
            "INSERT OR IGNORE INTO chat_sessions (session_id, title) VALUES (?, ?)",
            (session_id, "New Chat"),
        )
        conn.execute(
            "INSERT INTO chat_messages (session_id, role, content) VALUES (?, ?, ?)",
            (session_id, role, content),
        )
        conn.execute(
            "UPDATE chat_sessions SET updated_at=CURRENT_TIMESTAMP WHERE session_id=?",
            (session_id,),
        )
        conn.commit()


def get_chat_messages(session_id: str, limit: int = 20):
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT role, content, created_at FROM chat_messages WHERE session_id=? ORDER BY created_at DESC LIMIT ?",
            (session_id, limit),
        ).fetchall()
    return [{"role": r[0], "content": r[1], "created_at": r[2]} for r in reversed(rows)]


# ── API usage ─────────────────────────────────────────────────────────────────

def save_api_usage(endpoint: str, model: str, input_tokens: int, output_tokens: int):
    with get_connection() as conn:
        conn.execute(
            "INSERT INTO api_usage (endpoint, model, input_tokens, output_tokens) VALUES (?, ?, ?, ?)",
            (endpoint, model, input_tokens, output_tokens),
        )
        conn.commit()


def get_total_usage():
    with get_connection() as conn:
        row = conn.execute(
            "SELECT COUNT(*), SUM(input_tokens + output_tokens) FROM api_usage"
        ).fetchone()
        sessions = conn.execute("SELECT COUNT(*) FROM chat_sessions").fetchone()[0]
    return {
        "total_requests": row[0] or 0,
        "total_tokens": int(row[1] or 0),
        "total_sessions": sessions,
    }
import uuid
import hashlib


def _hash_password(password: str) -> str:
    """Simple SHA-256 hash.  For production use bcrypt/argon2."""
    return hashlib.sha256(password.encode("utf-8")).hexdigest()


# ── Users ─────────────────────────────────────────────────────────────────────

def create_user(username: str, email: str, password: str) -> int:
    hashed = _hash_password(password)
    with get_connection() as conn:
        cursor = conn.execute(
            "INSERT INTO users (username, email, password) VALUES (?, ?, ?)",
            (username, email, hashed),
        )
        conn.commit()
        return cursor.lastrowid


def get_user_by_email(email: str) -> object:
    with get_connection() as conn:
        row = conn.execute(
            "SELECT id, username, email, password FROM users WHERE email = ?",
            (email,),
        ).fetchone()
    if row is None:
        return None
    return {"id": row[0], "username": row[1], "email": row[2], "password": row[3]}


def get_user_by_id(user_id: int) -> object:
    with get_connection() as conn:
        row = conn.execute(
            "SELECT id, username, email FROM users WHERE id = ?",
            (user_id,),
        ).fetchone()
    if row is None:
        return None
    return {"id": row[0], "username": row[1], "email": row[2]}


def email_exists(email: str) -> bool:
    with get_connection() as conn:
        row = conn.execute(
            "SELECT 1 FROM users WHERE email = ?", (email,)
        ).fetchone()
    return row is not None


# ── Auth tokens ───────────────────────────────────────────────────────────────

def create_auth_token(user_id: int) -> str:
    token = str(uuid.uuid4())
    with get_connection() as conn:
        conn.execute(
            "INSERT INTO auth_tokens (token, user_id) VALUES (?, ?)",
            (token, user_id),
        )
        conn.commit()
    return token


def get_user_by_token(token: str) -> object:
    with get_connection() as conn:
        row = conn.execute(
            """SELECT u.id, u.username, u.email
               FROM auth_tokens t
               JOIN users u ON u.id = t.user_id
               WHERE t.token = ?""",
            (token,),
        ).fetchone()
    if row is None:
        return None
    return {"id": row[0], "username": row[1], "email": row[2]}


def delete_auth_token(token: str) -> None:
    with get_connection() as conn:
        conn.execute("DELETE FROM auth_tokens WHERE token = ?", (token,))
        conn.commit()


def verify_password(plain: str, hashed: str) -> bool:
    return _hash_password(plain) == hashed
