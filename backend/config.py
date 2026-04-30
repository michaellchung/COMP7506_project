import os
from dotenv import load_dotenv

load_dotenv()

OPENROUTER_API_KEY: str = os.getenv("OPENROUTER_API_KEY", "")
OPENROUTER_BASE_URL: str = os.getenv("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1")

CHAT_MODEL: str = os.getenv("CHAT_MODEL", "openai/gpt-4o-mini")
OCR_MODEL: str = os.getenv("OCR_MODEL", "openai/gpt-4o")
EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "openai/text-embedding-3-small")

# Transcription is done through OpenRouter chat-completions with an
# audio-input-capable model. The audio is sent as base64 mp3.
TRANSCRIPTION_MODEL: str = os.getenv("TRANSCRIPTION_MODEL", "openai/gpt-4o-mini-audio-preview")

MILVUS_HOST: str = os.getenv("MILVUS_HOST", "localhost")
MILVUS_PORT: int = int(os.getenv("MILVUS_PORT", "19530"))
EMBEDDING_DIM: int = int(os.getenv("EMBEDDING_DIM", "1536"))

CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "500"))
CHUNK_OVERLAP: int = int(os.getenv("CHUNK_OVERLAP", "50"))
