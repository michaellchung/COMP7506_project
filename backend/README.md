# NoteMind Backend

Minimal Flask backend for the Android client framework.

## Run

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

Android emulator can call this server through `http://10.0.2.2:5000`.

## Endpoints

- `GET /health`
- `POST /api/recording/summarize`
- `POST /api/ocr`
- `POST /api/kb/ask`

The current services return placeholders. Replace the service functions with Whisper/OCR/DeepSeek calls when each module is implemented.
