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


def insert_chunks(
    texts: List[str],
    source_type: str,
    note_id: int,
    embeddings: List[List[float]],
) -> bool:
    col = _get_collection()
    if col is None:
        return False
    try:
        col.insert([texts, [source_type] * len(texts), [note_id] * len(texts), embeddings])
        col.flush()
        return True
    except Exception as exc:
        log.error("Milvus insert error: %s", exc)
        return False


def search(query_embedding: List[float], top_k: int = 5) -> List[dict]:
    col = _get_collection()
    if col is None:
        return []
    try:
        results = col.search(
            data=[query_embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 16}},
            limit=top_k,
            output_fields=["text", "source_type"],
        )
        return [
            {
                "text": hit.entity.get("text"),
                "source_type": hit.entity.get("source_type"),
                "score": round(float(hit.score), 4),
            }
            for hit in results[0]
        ]
    except Exception as exc:
        log.error("Milvus search error: %s", exc)
        return []
