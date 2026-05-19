"""Milvus vector store wrapper.

The connection is lazy — if Milvus is not running the service degrades
gracefully and logs a warning rather than crashing the Flask app.
"""
from __future__ import annotations

import logging
from typing import List

log = logging.getLogger(__name__)

_collection = None
COLLECTION_NAME = "notemind_chunks"


def _get_collection():
    global _collection
    if _collection is not None:
        return _collection
    try:
        from pymilvus import (
            connections,
            Collection,
            CollectionSchema,
            DataType,
            FieldSchema,
            utility,
        )
        from config import EMBEDDING_DIM, MILVUS_HOST, MILVUS_PORT

        connections.connect("default", host=MILVUS_HOST, port=MILVUS_PORT)

        if not utility.has_collection(COLLECTION_NAME):
            fields = [
                FieldSchema("id", DataType.INT64, is_primary=True, auto_id=True),
                FieldSchema("text", DataType.VARCHAR, max_length=4096),
                FieldSchema("source_type", DataType.VARCHAR, max_length=64),
                FieldSchema("note_id", DataType.INT64),
                FieldSchema("course_id", DataType.INT64),
                FieldSchema("lecture_id", DataType.INT64),
                FieldSchema("embedding", DataType.FLOAT_VECTOR, dim=EMBEDDING_DIM),
            ]
            schema = CollectionSchema(fields, description="NoteMind knowledge chunks")
            col = Collection(COLLECTION_NAME, schema)
            col.create_index(
                "embedding",
                {
                    "metric_type": "COSINE",
                    "index_type": "IVF_FLAT",
                    "params": {"nlist": 128},
                },
            )

        _collection = Collection(COLLECTION_NAME)
        _collection.load()
        log.info("Milvus connected. Collection: %s", COLLECTION_NAME)
    except Exception as exc:
        log.warning("Milvus unavailable — vector search disabled: %s", exc)
        _collection = None
    return _collection


def _field_names(col) -> set:
    return {field.name for field in col.schema.fields}


def _scope_id(value: object) -> int:
    try:
        scoped_id = int(value)
    except (TypeError, ValueError):
        return 0
    return scoped_id if scoped_id > 0 else 0


def insert_chunks(
    texts: List[str],
    source_type: str,
    note_id: int,
    embeddings: List[List[float]],
    course_id: object = None,
    lecture_id: object = None,
) -> bool:
    col = _get_collection()
    if col is None:
        return False
    try:
        fields = _field_names(col)
        insert_data = [
            texts,
            [source_type] * len(texts),
            [note_id] * len(texts),
        ]
        if "course_id" in fields and "lecture_id" in fields:
            insert_data.extend([
                [_scope_id(course_id)] * len(texts),
                [_scope_id(lecture_id)] * len(texts),
            ])
        insert_data.append(embeddings)
        col.insert(insert_data)
        col.flush()
        return True
    except Exception as exc:
        log.error("Milvus insert error: %s", exc)
        return False


def search(
    query_embedding: List[float],
    top_k: int = 5,
    course_id: object = None,
    lecture_id: object = None,
) -> List[dict]:
    col = _get_collection()
    if col is None:
        return []
    try:
        fields = _field_names(col)
        scoped_course_id = _scope_id(course_id)
        scoped_lecture_id = _scope_id(lecture_id)
        expr_parts = []
        needs_post_filter = False

        if scoped_course_id:
            if "course_id" in fields:
                expr_parts.append(f"course_id == {scoped_course_id}")
            else:
                needs_post_filter = True
        if scoped_lecture_id:
            if "lecture_id" in fields:
                expr_parts.append(f"lecture_id == {scoped_lecture_id}")
            else:
                needs_post_filter = True

        output_fields = ["text", "source_type", "note_id"]
        if "course_id" in fields:
            output_fields.append("course_id")
        if "lecture_id" in fields:
            output_fields.append("lecture_id")

        search_limit = top_k * 10 if needs_post_filter else top_k
        search_kwargs = {
            "data": [query_embedding],
            "anns_field": "embedding",
            "param": {"metric_type": "COSINE", "params": {"nprobe": 16}},
            "limit": search_limit,
            "output_fields": output_fields,
        }
        if expr_parts:
            search_kwargs["expr"] = " and ".join(expr_parts)

        results = col.search(
            **search_kwargs,
        )
        chunks = []
        for hit in results[0]:
            chunk = {
                "text": hit.entity.get("text"),
                "source_type": hit.entity.get("source_type"),
                "note_id": hit.entity.get("note_id"),
                "course_id": hit.entity.get("course_id") if "course_id" in fields else None,
                "lecture_id": hit.entity.get("lecture_id") if "lecture_id" in fields else None,
                "score": round(float(hit.score), 4),
            }
            if needs_post_filter:
                from database import note_matches_scope

                if not note_matches_scope(chunk["note_id"], scoped_course_id or None, scoped_lecture_id or None):
                    continue
            chunks.append(chunk)
            if len(chunks) >= top_k:
                break
        return chunks
    except Exception as exc:
        log.error("Milvus search error: %s", exc)
        return []
