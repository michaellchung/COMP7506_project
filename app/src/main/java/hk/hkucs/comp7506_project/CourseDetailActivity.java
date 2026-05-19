package hk.hkucs.comp7506_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hk.hkucs.comp7506_project.data.BackendConfig;
import hk.hkucs.comp7506_project.model.Lecture;
import hk.hkucs.comp7506_project.ui.LectureAdapter;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID    = "course_id";
    public static final String EXTRA_COURSE_TITLE = "course_title";

    private RequestQueue requestQueue;
    private String backendUrl;
    private int courseId;
    private String courseTitle = "Course";

    private TextView toolbarTitle, courseTitleLarge, courseDescription, courseSchedule;
    private LinearLayout scheduleRow;
    private RecyclerView lecturesRecycler;
    private TextView emptyHint;

    private final List<Lecture> lectures = new ArrayList<>();
    private LectureAdapter lectureAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        courseId    = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        courseTitle = getIntent().getStringExtra(EXTRA_COURSE_TITLE);
        if (courseTitle == null) courseTitle = "Course";

        requestQueue = Volley.newRequestQueue(this);
        backendUrl   = BackendConfig.getUrl(this);

        bindViews();
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backendUrl = BackendConfig.getUrl(this);
        loadCourseDetail();
    }

    private void bindViews() {
        toolbarTitle      = findViewById(R.id.toolbarTitle);
        courseTitleLarge  = findViewById(R.id.courseTitleLarge);
        courseDescription = findViewById(R.id.courseDescription);
        courseSchedule    = findViewById(R.id.courseSchedule);
        scheduleRow       = findViewById(R.id.courseScheduleRow);
        lecturesRecycler  = findViewById(R.id.lecturesRecyclerView);
        emptyHint         = findViewById(R.id.emptyLecturesHint);

        toolbarTitle.setText(courseTitle);
        courseTitleLarge.setText(courseTitle);

        lectureAdapter = new LectureAdapter(lectures, this::openLectureDetail);
        lecturesRecycler.setLayoutManager(new LinearLayoutManager(this));
        lecturesRecycler.setAdapter(lectureAdapter);
    }

    private void bindActions() {
        ImageButton btnBack          = findViewById(R.id.btnBack);
        ImageButton btnAskCourse     = findViewById(R.id.btnAskCourse);
        ImageButton btnDelete        = findViewById(R.id.btnDeleteCourse);
        MaterialButton btnNewLecture = findViewById(R.id.btnNewLecture);

        btnBack.setOnClickListener(v -> finish());
        btnAskCourse.setOnClickListener(v -> openCourseChat());
        btnDelete.setOnClickListener(v -> confirmDeleteCourse());
        btnNewLecture.setOnClickListener(v -> showCreateLectureDialog());
    }

    // ── Network ──────────────────────────────────────────────────────────────

    private void loadCourseDetail() {
        if (courseId <= 0) return;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, backendUrl + "/api/courses/" + courseId, null,
                this::onCourseLoaded,
                error -> Toast.makeText(this,
                        "Failed to load course (" + backendUrl + ")",
                        Toast.LENGTH_LONG).show());
        requestQueue.add(req);
    }

    private void onCourseLoaded(JSONObject obj) {
        courseTitle = obj.optString("title", courseTitle);
        toolbarTitle.setText(courseTitle);
        courseTitleLarge.setText(courseTitle);

        String desc = obj.optString("description", "");
        if (desc.isEmpty()) {
            courseDescription.setVisibility(View.GONE);
        } else {
            courseDescription.setVisibility(View.VISIBLE);
            courseDescription.setText(desc);
        }

        String sch = obj.optString("schedule", "");
        if (sch.isEmpty()) {
            scheduleRow.setVisibility(View.GONE);
        } else {
            scheduleRow.setVisibility(View.VISIBLE);
            courseSchedule.setText(sch);
        }

        lectures.clear();
        JSONArray arr = obj.optJSONArray("lectures");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject l = arr.optJSONObject(i);
                if (l == null) continue;
                lectures.add(new Lecture(
                        l.optInt("id", 0),
                        l.optInt("course_id", courseId),
                        l.optInt("lecture_number", i + 1),
                        l.optString("title", "Lecture " + (i + 1)),
                        l.optString("lecture_date", ""),
                        l.optInt("note_count", 0)));
            }
        }
        lectureAdapter.notifyDataSetChanged();

        boolean empty = lectures.isEmpty();
        emptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        lecturesRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Create lecture ───────────────────────────────────────────────────────

    private void showCreateLectureDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_lecture, null);
        TextInputEditText titleEt = view.findViewById(R.id.inputLectureTitle);
        TextInputEditText dateEt  = view.findViewById(R.id.inputLectureDate);

        // Pre-fill today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        dateEt.setText(today);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_lecture)
                .setView(view)
                .setPositiveButton(R.string.action_create, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> createLecture(textOf(titleEt), textOf(dateEt), dialog)));
        dialog.show();
    }

    private void createLecture(String title, String date, AlertDialog dialog) {
        try {
            JSONObject payload = new JSONObject()
                    .put("title", title)
                    .put("lecture_date", date);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    backendUrl + "/api/courses/" + courseId + "/lectures",
                    payload,
                    response -> {
                        dialog.dismiss();
                        Lecture lec = new Lecture(
                                response.optInt("id", 0),
                                response.optInt("course_id", courseId),
                                response.optInt("lecture_number", 0),
                                response.optString("title", "Lecture"),
                                response.optString("lecture_date", ""),
                                0);
                        loadCourseDetail();
                        openLectureDetail(lec);
                    },
                    error -> Toast.makeText(this,
                            "Failed to create lecture.", Toast.LENGTH_SHORT).show());
            requestQueue.add(req);
        } catch (JSONException e) {
            Toast.makeText(this, "Invalid input.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Delete course ────────────────────────────────────────────────────────

    private void confirmDeleteCourse() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_course)
                .setMessage(R.string.confirm_delete_course)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> deleteCourse())
                .show();
    }

    private void deleteCourse() {
        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                backendUrl + "/api/courses/" + courseId,
                response -> finish(),
                error -> Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show());
        requestQueue.add(req);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void openLectureDetail(Lecture lec) {
        Intent intent = new Intent(this, LectureDetailActivity.class);
        intent.putExtra(LectureDetailActivity.EXTRA_LECTURE_ID,     lec.getId());
        intent.putExtra(LectureDetailActivity.EXTRA_LECTURE_TITLE,  lec.getTitle());
        intent.putExtra(LectureDetailActivity.EXTRA_LECTURE_NUMBER, lec.getLectureNumber());
        intent.putExtra(LectureDetailActivity.EXTRA_COURSE_TITLE,   courseTitle);
        intent.putExtra(LectureDetailActivity.EXTRA_LECTURE_DATE,   lec.getLectureDate());
        startActivity(intent);
    }

    private void openCourseChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_COURSE_ID, courseId);
        intent.putExtra(ChatActivity.EXTRA_COURSE_TITLE, courseTitle);
        startActivity(intent);
    }

    private static String textOf(TextInputEditText et) {
        CharSequence s = et != null ? et.getText() : null;
        return s == null ? "" : s.toString().trim();
    }
}
