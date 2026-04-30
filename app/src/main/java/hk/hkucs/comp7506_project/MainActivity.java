package hk.hkucs.comp7506_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import hk.hkucs.comp7506_project.data.BackendConfig;
import hk.hkucs.comp7506_project.model.Course;
import hk.hkucs.comp7506_project.ui.CourseAdapter;

public class MainActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private String backendUrl;

    private final List<Course> courses = new ArrayList<>();
    private CourseAdapter courseAdapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private TextView avatarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        requestQueue = Volley.newRequestQueue(this);

        recyclerView = findViewById(R.id.coursesRecyclerView);
        emptyView    = findViewById(R.id.emptyCoursesView);
        avatarButton = findViewById(R.id.avatarButton);
        FloatingActionButton fabChat = findViewById(R.id.fabChat);

        courseAdapter = new CourseAdapter(courses, new CourseAdapter.OnCourseClickListener() {
            @Override public void onCourseClick(Course c)   { openCourseDetail(c); }
            @Override public void onAddCourseClick()        { showCreateCourseDialog(); }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(courseAdapter);

        fabChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        avatarButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        backendUrl = BackendConfig.getUrl(this);
        refreshAvatar();
        loadCourses();
    }

    // ── Network ──────────────────────────────────────────────────────────────

    private void loadCourses() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, backendUrl + "/api/courses", null,
                this::onCoursesLoaded,
                error -> showCoursesEmpty(true));
        requestQueue.add(req);
    }

    private void onCoursesLoaded(JSONArray array) {
        courses.clear();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject o = array.getJSONObject(i);
                courses.add(new Course(
                        o.optInt("id", 0),
                        o.optString("title", "Course"),
                        o.optString("description", ""),
                        o.optString("schedule", ""),
                        o.optInt("lecture_count", 0)));
            } catch (JSONException ignored) {}
        }
        courseAdapter.notifyDataSetChanged();
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showCoursesEmpty(boolean empty) {
        // Add-course card is always visible; no separate empty state needed.
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    // ── Create course dialog ─────────────────────────────────────────────────

    private void showCreateCourseDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_course, null);
        TextInputEditText titleEt = view.findViewById(R.id.inputCourseTitle);
        TextInputEditText descEt  = view.findViewById(R.id.inputCourseDescription);
        TextInputEditText schEt   = view.findViewById(R.id.inputCourseSchedule);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_course)
                .setView(view)
                .setPositiveButton(R.string.action_create, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    String title = textOf(titleEt);
                    if (TextUtils.isEmpty(title)) {
                        titleEt.setError("Required");
                        return;
                    }
                    createCourse(title, textOf(descEt), textOf(schEt), dialog);
                }));
        dialog.show();
    }

    private void createCourse(String title, String description, String schedule, AlertDialog dialog) {
        try {
            JSONObject payload = new JSONObject()
                    .put("title", title)
                    .put("description", description)
                    .put("schedule", schedule);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, backendUrl + "/api/courses", payload,
                    response -> {
                        dialog.dismiss();
                        loadCourses();
                    },
                    error -> Toast.makeText(this,
                            "Failed to create course (" + backendUrl + ")",
                            Toast.LENGTH_LONG).show());
            requestQueue.add(req);
        } catch (JSONException e) {
            Toast.makeText(this, "Invalid input.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void openCourseDetail(Course c) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID,    c.getId());
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_TITLE, c.getTitle());
        startActivity(intent);
    }

    // ── Avatar refresh ───────────────────────────────────────────────────────

    private void refreshAvatar() {
        SharedPreferences prefs = getSharedPreferences(BackendConfig.PREFS, MODE_PRIVATE);
        String name = prefs.getString("display_name", "");
        avatarButton.setText(name.isEmpty()
                ? "U"
                : String.valueOf(name.charAt(0)).toUpperCase());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String textOf(TextInputEditText et) {
        CharSequence s = et != null ? et.getText() : null;
        return s == null ? "" : s.toString().trim();
    }
}
