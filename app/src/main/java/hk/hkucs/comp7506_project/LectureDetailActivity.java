package hk.hkucs.comp7506_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hk.hkucs.comp7506_project.data.BackendConfig;
import hk.hkucs.comp7506_project.model.NoteRecord;
import hk.hkucs.comp7506_project.ui.NoteAdapter;

public class LectureDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LECTURE_ID     = "lecture_id";
    public static final String EXTRA_LECTURE_TITLE  = "lecture_title";
    public static final String EXTRA_LECTURE_NUMBER = "lecture_number";
    public static final String EXTRA_LECTURE_DATE   = "lecture_date";
    public static final String EXTRA_COURSE_TITLE   = "course_title";

    private int    lectureId;
    private String lectureTitle  = "Lecture";
    private String lectureDate   = "";
    private int    lectureNumber = 0;
    private String courseTitle   = "Course";

    // UI
    private TextView toolbarTitle, lectureCourseName, lectureTitleLarge, lectureDateView, actionStatus;
    private LinearLayout lectureDateRow, progressSection;
    private MaterialButton btnStartRecording, btnStopRecording;
    private RecyclerView notesRecyclerView;
    private TextView emptyNotesHint;

    private final List<NoteRecord> notes = new ArrayList<>();
    private NoteAdapter noteAdapter;

    // Network
    private RequestQueue requestQueue;
    private String backendUrl;

    // Audio
    private MediaRecorder mediaRecorder;
    private String recordingFilePath;

    // Camera
    private Uri currentPhotoUri;

    // Background work
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Activity-result launchers
    private ActivityResultLauncher<Uri>      cameraLauncher;
    private ActivityResultLauncher<String>   imagePickerLauncher;
    private ActivityResultLauncher<String>   audioPickerLauncher;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private Runnable pendingPermissionAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerLaunchers();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture_detail);

        Intent intent = getIntent();
        lectureId     = intent.getIntExtra(EXTRA_LECTURE_ID, -1);
        lectureTitle  = orDefault(intent.getStringExtra(EXTRA_LECTURE_TITLE), "Lecture");
        lectureNumber = intent.getIntExtra(EXTRA_LECTURE_NUMBER, 0);
        lectureDate   = orDefault(intent.getStringExtra(EXTRA_LECTURE_DATE), "");
        courseTitle   = orDefault(intent.getStringExtra(EXTRA_COURSE_TITLE), "Course");

        executor     = Executors.newSingleThreadExecutor();
        requestQueue = Volley.newRequestQueue(this);
        backendUrl   = BackendConfig.getUrl(this);

        bindViews();
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backendUrl = BackendConfig.getUrl(this);
        loadNotes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
        stopRecorderSilently();
    }

    // ── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        toolbarTitle      = findViewById(R.id.toolbarTitle);
        lectureCourseName = findViewById(R.id.lectureCourseName);
        lectureTitleLarge = findViewById(R.id.lectureTitleLarge);
        lectureDateView   = findViewById(R.id.lectureDate);
        lectureDateRow    = findViewById(R.id.lectureDateRow);

        btnStartRecording = findViewById(R.id.btnStartRecording);
        btnStopRecording  = findViewById(R.id.btnStopRecording);
        progressSection   = findViewById(R.id.progressSection);
        actionStatus      = findViewById(R.id.actionStatus);

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        emptyNotesHint    = findViewById(R.id.emptyNotesHint);

        toolbarTitle.setText(lectureTitle);
        lectureCourseName.setText(courseTitle);
        lectureTitleLarge.setText(lectureTitle);

        if (lectureDate.isEmpty()) {
            lectureDateRow.setVisibility(View.GONE);
        } else {
            lectureDateRow.setVisibility(View.VISIBLE);
            lectureDateView.setText(lectureDate);
        }

        noteAdapter = new NoteAdapter(notes, this::openNoteDetail);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(noteAdapter);
    }

    private void bindActions() {
        ImageButton btnBack         = findViewById(R.id.btnBack);
        ImageButton btnDelete       = findViewById(R.id.btnDeleteLecture);
        MaterialButton btnPickAudio = findViewById(R.id.btnUploadAudio);
        MaterialButton btnPhoto     = findViewById(R.id.btnTakePhoto);
        MaterialButton btnGallery   = findViewById(R.id.btnPickImage);
        MaterialButton btnFile      = findViewById(R.id.btnUploadFile);

        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> confirmDeleteLecture());

        btnStartRecording.setOnClickListener(v -> withPermission(
                Manifest.permission.RECORD_AUDIO, this::startRecording));
        btnStopRecording.setOnClickListener(v  -> stopAndUploadRecording());
        btnPickAudio.setOnClickListener(v      -> audioPickerLauncher.launch("audio/*"));

        btnPhoto.setOnClickListener(v -> withPermission(
                Manifest.permission.CAMERA, this::launchCamera));
        btnGallery.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnFile.setOnClickListener(v -> filePickerLauncher.launch(new String[]{
                "application/pdf",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        }));
    }

    // ── Launcher registration ────────────────────────────────────────────────

    private void registerLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && currentPhotoUri != null) {
                        processImageUri(currentPhotoUri);
                    }
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) processImageUri(uri); });

        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) processAudioUri(uri); });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) processDocumentUri(uri); });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                results -> {
                    boolean allGranted = !results.containsValue(Boolean.FALSE);
                    if (allGranted && pendingPermissionAction != null) {
                        pendingPermissionAction.run();
                    } else if (!allGranted) {
                        Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                    }
                    pendingPermissionAction = null;
                });
    }

    // ── Recording ────────────────────────────────────────────────────────────

    private void startRecording() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        recordingFilePath = getCacheDir().getAbsolutePath() + "/rec_" + ts + ".m4a";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                //noinspection deprecation
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(recordingFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            btnStartRecording.setVisibility(View.GONE);
            btnStopRecording.setVisibility(View.VISIBLE);
            showProgress(getString(R.string.recording_in_progress));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot record: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopRecorderSilently();
        }
    }

    private void stopAndUploadRecording() {
        btnStopRecording.setVisibility(View.GONE);
        btnStartRecording.setVisibility(View.VISIBLE);
        stopRecorderSilently();

        if (recordingFilePath == null) return;
        final String path = recordingFilePath;
        recordingFilePath = null;
        showProgress(getString(R.string.processing));

        executor.execute(() -> {
            try {
                byte[] bytes = readAllBytes(new File(path));
                String b64   = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String title = lectureTitle + " · Recording "
                        + new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                mainHandler.post(() -> uploadForRecording(b64, "m4a", title));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(this, "Read recording failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                new File(path).delete();
            }
        });
    }

    private void stopRecorderSilently() {
        if (mediaRecorder != null) {
            try { mediaRecorder.stop();    } catch (Exception ignored) {}
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    // ── Camera ───────────────────────────────────────────────────────────────

    private void launchCamera() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File photoFile = new File(getCacheDir(), "photo_" + ts + ".jpg");
        currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
        cameraLauncher.launch(currentPhotoUri);
    }

    // ── URI processors ───────────────────────────────────────────────────────

    private void processImageUri(Uri uri) {
        showProgress(getString(R.string.processing));
        executor.execute(() -> {
            try {
                byte[] bytes = readAllBytes(getContentResolver().openInputStream(uri));
                String b64   = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String name  = getDisplayFileName(uri);
                mainHandler.post(() -> uploadForOcr(b64, name));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(this, "Read image failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void processAudioUri(Uri uri) {
        showProgress(getString(R.string.processing));
        executor.execute(() -> {
            try {
                byte[] bytes = readAllBytes(getContentResolver().openInputStream(uri));
                String b64   = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String name  = getDisplayFileName(uri);
                String ext   = extensionFrom(name, "m4a");
                mainHandler.post(() -> uploadForRecording(b64, ext, lectureTitle + " · " + name));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(this, "Read audio failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void processDocumentUri(Uri uri) {
        showProgress(getString(R.string.processing));
        executor.execute(() -> {
            try {
                byte[] bytes = readAllBytes(getContentResolver().openInputStream(uri));
                String b64   = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String name  = getDisplayFileName(uri);
                String ext   = extensionFrom(name, "pdf");
                mainHandler.post(() -> uploadForPpt(b64, name, ext));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(this, "Read file failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Backend uploads ──────────────────────────────────────────────────────

    private void uploadForRecording(String b64, String format, String title) {
        try {
            JSONObject p = new JSONObject()
                    .put("audio_b64",    b64)
                    .put("audio_format", format)
                    .put("title",        title)
                    .put("lecture_id",   lectureId);
            postJson(backendUrl + "/api/recording/summarize", p);
        } catch (JSONException e) {
            hideProgress();
        }
    }

    private void uploadForOcr(String b64, String filename) {
        try {
            JSONObject p = new JSONObject()
                    .put("image_b64",  b64)
                    .put("image_name", filename)
                    .put("title",      lectureTitle + " · " + filename)
                    .put("lecture_id", lectureId);
            postJson(backendUrl + "/api/ocr", p);
        } catch (JSONException e) {
            hideProgress();
        }
    }

    private void uploadForPpt(String b64, String filename, String fileType) {
        try {
            JSONObject p = new JSONObject()
                    .put("file_b64",   b64)
                    .put("file_name",  filename)
                    .put("file_type",  fileType)
                    .put("title",      lectureTitle + " · " + filename)
                    .put("lecture_id", lectureId);
            postJson(backendUrl + "/api/ppt/analyze", p);
        } catch (JSONException e) {
            hideProgress();
        }
    }

    private void postJson(String url, JSONObject payload) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, payload,
                response -> {
                    hideProgress();
                    Toast.makeText(this, "Saved to lecture.", Toast.LENGTH_SHORT).show();
                    loadNotes();
                },
                error -> {
                    hideProgress();
                    String msg = (error.networkResponse != null)
                            ? "Server error " + error.networkResponse.statusCode
                            : "Cannot connect to backend (" + backendUrl + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
        req.setRetryPolicy(new DefaultRetryPolicy(
                120_000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(req);
    }

    // ── Notes list ───────────────────────────────────────────────────────────

    private void loadNotes() {
        if (lectureId <= 0) return;
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                backendUrl + "/api/lectures/" + lectureId + "/notes",
                null,
                this::onNotesLoaded,
                error -> showNotesEmpty(true));
        requestQueue.add(req);
    }

    private void onNotesLoaded(JSONArray array) {
        notes.clear();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject o = array.getJSONObject(i);
                notes.add(new NoteRecord(
                        o.optInt("id", i),
                        o.optString("source_type", "note"),
                        o.optString("title", "Untitled"),
                        o.optString("content", ""),
                        o.optString("created_at", "")));
            } catch (JSONException ignored) {}
        }
        noteAdapter.notifyDataSetChanged();
        showNotesEmpty(notes.isEmpty());
    }

    private void showNotesEmpty(boolean empty) {
        emptyNotesHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        notesRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openNoteDetail(NoteRecord note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra(NoteDetailActivity.EXTRA_ID,          note.getId());
        intent.putExtra(NoteDetailActivity.EXTRA_TITLE,       note.getTitle());
        intent.putExtra(NoteDetailActivity.EXTRA_CONTENT,     note.getContent());
        intent.putExtra(NoteDetailActivity.EXTRA_SOURCE_TYPE, note.getSourceType());
        intent.putExtra(NoteDetailActivity.EXTRA_CREATED_AT,  note.getCreatedAt());
        startActivity(intent);
    }

    // ── Delete lecture ───────────────────────────────────────────────────────

    private void confirmDeleteLecture() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_lecture)
                .setMessage(R.string.confirm_delete_lecture)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> deleteLecture())
                .show();
    }

    private void deleteLecture() {
        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                backendUrl + "/api/lectures/" + lectureId,
                response -> finish(),
                error -> Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show());
        requestQueue.add(req);
    }

    // ── UI helpers ───────────────────────────────────────────────────────────

    private void showProgress(String msg) {
        progressSection.setVisibility(View.VISIBLE);
        actionStatus.setText(msg);
    }

    private void hideProgress() {
        progressSection.setVisibility(View.GONE);
    }

    // ── Permission helper ────────────────────────────────────────────────────

    private void withPermission(String permission, Runnable onGranted) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted.run();
        } else {
            pendingPermissionAction = onGranted;
            permissionLauncher.launch(new String[]{permission});
        }
    }

    // ── File / URI helpers ───────────────────────────────────────────────────

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        is.close();
        return baos.toByteArray();
    }

    private byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readAllBytes(fis);
        }
    }

    private String getDisplayFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = c.getString(idx);
                }
            }
        }
        if (result == null) result = uri.getLastPathSegment();
        return result != null ? result : "file";
    }

    private String extensionFrom(String filename, String fallback) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.US);
        }
        return fallback;
    }

    private static String orDefault(String s, String fallback) {
        return s == null ? fallback : s;
    }
}
