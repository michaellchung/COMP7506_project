import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).with_name("notemind.db")


def get_connection():
    return sqlite3.connect(DB_PATH)


def init_db():
    with get_connection() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS notes (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                source_type TEXT NOT NULL,
                title       TEXT NOT NULL,
                content     TEXT NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
            """
        )
        conn.commit()


# ── Notes ────────────────────────────────────────────────────────────────────

def save_note(source_type, title, content):
    with get_connection() as conn:
        cursor = conn.execute(
            "INSERT INTO notes (source_type, title, content) VALUES (?, ?, ?)",
            (source_type, title, content),
        )
        conn.commit()
        return cursor.lastrowid


def list_recent_notes(limit=5):
    with get_connection() as conn:
        rows = conn.execute(
            "SELECT source_type, title, content FROM notes ORDER BY created_at DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [{"source_type": r[0], "title": r[1], "content": r[2]} for r in rows]


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
