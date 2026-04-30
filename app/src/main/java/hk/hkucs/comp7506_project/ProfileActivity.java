package hk.hkucs.comp7506_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import hk.hkucs.comp7506_project.data.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_PROFILE = "notemind_profile";
    private static final String DEFAULT_BACKEND_URL = "http://127.0.0.1:5000";

    private TextInputEditText inputDisplayName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputBackendUrl;
    private TextView displayName;
    private TextView displayEmail;
    private TextView avatarCircle;
    private TextView usageMessages;
    private TextView usageTokens;
    private TextView usageSessions;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_PROFILE, MODE_PRIVATE);
        bindViews();
        loadProfile();
        bindActions();
    }

    private void bindViews() {
        inputDisplayName = findViewById(R.id.inputDisplayName);
        inputEmail = findViewById(R.id.inputEmail);
        inputBackendUrl = findViewById(R.id.inputBackendUrl);
        displayName = findViewById(R.id.displayName);
        displayEmail = findViewById(R.id.displayEmail);
        avatarCircle = findViewById(R.id.avatarCircle);
        usageMessages = findViewById(R.id.usageMessages);
        usageTokens = findViewById(R.id.usageTokens);
        usageSessions = findViewById(R.id.usageSessions);
    }

    private void loadProfile() {
        String name = prefs.getString("display_name", "Student");
        String email = prefs.getString("email", "hku@connect.hku.hk");
        String url = normalizeBackendUrl(prefs.getString("backend_url", DEFAULT_BACKEND_URL));

        inputDisplayName.setText(name);
        inputEmail.setText(email);
        inputBackendUrl.setText(url);
        displayName.setText(name);
        displayEmail.setText(email);
        avatarCircle.setText(initials(name));

        int msgs = prefs.getInt("usage_messages", 0);
        int tokens = prefs.getInt("usage_tokens", 0);
        int sessionCount = new SessionManager(this).loadSessions().size();

        usageMessages.setText(String.valueOf(msgs));
        usageTokens.setText(String.valueOf(tokens));
        usageSessions.setText(String.valueOf(sessionCount));
    }

    private void bindActions() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btnSaveProfile);
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = text(inputDisplayName);
        String email = text(inputEmail);
        String url = text(inputBackendUrl);

        prefs.edit()
                .putString("display_name", name)
                .putString("email", email)
                .putString("backend_url", normalizeBackendUrl(url))
                .apply();

        displayName.setText(name);
        displayEmail.setText(email);
        avatarCircle.setText(initials(name));
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String initials(String name) {
        return name.isEmpty() ? "U" : String.valueOf(name.charAt(0)).toUpperCase();
    }

    private String normalizeBackendUrl(String url) {
        String normalized = url == null || url.trim().isEmpty() ? DEFAULT_BACKEND_URL : url.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if ("http://10.0.2.2:5000".equals(normalized)) {
            normalized = DEFAULT_BACKEND_URL;
        }
        return normalized;
    }
}
