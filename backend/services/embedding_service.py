"""Text chunking, embedding via OpenRouter, and Milvus storage."""
from __future__ import annotations

import logging
from typing import List

log = logging.getLogger(__name__)


def chunk_text(text: str, chunk_size: int = 0, overlap: int = 0) -> List[str]:
    from config import CHUNK_SIZE, CHUNK_OVERLAP

    chunk_size = chunk_size or CHUNK_SIZE
    overlap = overlap or CHUNK_OVERLAP

    chunks: List[str] = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        # Try to end on a sentence boundary
        if end < len(text):
            boundary = text.rfind(".", start, end)
            if boundary > start + chunk_size // 2:
                end = boundary + 1
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)
        start = end - overlap
    return chunks


def get_embedding(text: str) -> List[float]:
    from openai import OpenAI
    from config import OPENROUTER_API_KEY, OPENROUTER_BASE_URL, EMBEDDING_MODEL

    client = OpenAI(api_key=OPENROUTER_API_KEY, base_url=OPENROUTER_BASE_URL)
    response = client.embeddings.create(model=EMBEDDING_MODEL, input=text)
    return response.data[0].embedding


def embed_and_store(
    text: str,
    source_type: str,
    note_id: int,
    course_id: object = None,
    lecture_id: object = None,
) -> bool:
    """Chunk text, embed each chunk, and store in Milvus. Returns True on success."""
    try:
        from services import milvus_service

        if course_id is None or lecture_id is None:
            try:
                from database import get_note_scope

                scope = get_note_scope(note_id)
                if scope:
                    course_id = course_id if course_id is not None else scope["course_id"]
                    lecture_id = lecture_id if lecture_id is not None else scope["lecture_id"]
            except Exception as exc:
                log.warning("Could not resolve note scope for embedding: %s", exc)

        chunks = chunk_text(text)
        if not chunks:
            return False
        embeddings = [get_embedding(c) for c in chunks]
        return milvus_service.insert_chunks(
            chunks,
            source_type,
            note_id,
            embeddings,
            course_id=course_id,
            lecture_id=lecture_id,
        )
    except Exception as exc:
        log.warning("embed_and_store failed: %s", exc)
        return False
