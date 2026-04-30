# NoteMind Backend

Minimal Flask backend for the Android client framework.

## Run

```bash
cd backend

# 创建虚拟环境 (可选但推荐)
python -m venv .venv

# 激活虚拟环境
# Windows PowerShell:
.\.venv\Scripts\activate
# macOS / Linux:
source .venv/bin/activate

pip install -r requirements.txt
python app.py
```

Android emulator can call this server through `http://10.0.2.2:5000`.
For physical devices via USB debugging, use `http://127.0.0.1:5000` with `adb reverse tcp:5000 tcp:5000`.

## Endpoints

- `GET /health`
- `POST /api/recording/summarize`
- `POST /api/ocr`
- `POST /api/kb/ask`

The current services return placeholders. Replace the service functions with Whisper/OCR/DeepSeek calls when each module is implemented.
