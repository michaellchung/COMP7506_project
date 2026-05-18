package hk.hkucs.comp7506_project;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import hk.hkucs.comp7506_project.data.BackendConfig;
import hk.hkucs.comp7506_project.data.SessionManager;
import hk.hkucs.comp7506_project.data.AuthManager;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_PROFILE = BackendConfig.PREFS;

    /** Monthly token budget for the progress bar visualisation. */
    private static final long TOKEN_BUDGET = 1_000_000L;

    private TextInputEditText inputDisplayName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputBackendUrl;
    private TextView displayName;
    private TextView displayEmail;
    private TextView avatarCircle;
    private TextView usageMessages;
    private TextView usageTokens;
    private TextView usageSessions;
    private TextView usageBudgetLabel;
    private ProgressBar usageProgressBar;

    private SharedPreferences prefs;
    private RequestQueue requestQueue;

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
        requestQueue = Volley.newRequestQueue(this);

        bindViews();
        loadProfile();
        bindActions();
        fetchUsage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUsage();
    }

    private void bindViews() {
        inputDisplayName  = findViewById(R.id.inputDisplayName);
        inputEmail        = findViewById(R.id.inputEmail);
        inputBackendUrl   = findViewById(R.id.inputBackendUrl);
        displayName       = findViewById(R.id.displayName);
        displayEmail      = findViewById(R.id.displayEmail);
        avatarCircle      = findViewById(R.id.avatarCircle);
        usageMessages     = findViewById(R.id.usageMessages);
        usageTokens       = findViewById(R.id.usageTokens);
        usageSessions     = findViewById(R.id.usageSessions);
        usageBudgetLabel  = findViewById(R.id.usageBudgetLabel);
        usageProgressBar  = findViewById(R.id.usageProgressBar);
    }

    private void loadProfile() {
        String name  = prefs.getString("display_name", AuthManager.get(this).getUsername());
        String email = prefs.getString("email", AuthManager.get(this).getEmail());
        String url   = BackendConfig.normalize(prefs.getString("backend_url", BackendConfig.DEFAULT_BACKEND));

        inputDisplayName.setText(name);
        inputEmail.setText(email);
        inputBackendUrl.setText(url);
        displayName.setText(name);
        displayEmail.setText(email);
        avatarCircle.setText(initials(name));

        // Initialize with cached values; backend fetch will override.
        usageMessages.setText(String.valueOf(prefs.getInt("usage_messages", 0)));
        usageTokens.setText(formatNumber(prefs.getInt("usage_tokens", 0)));
        usageSessions.setText(String.valueOf(new SessionManager(this).loadSessions().size()));
        applyTokenProgress(prefs.getInt("usage_tokens", 0));
    }

    private void fetchUsage() {
        String url = BackendConfig.getUrl(this) + "/api/profile/usage";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    int requests = response.optInt("total_requests", 0);
                    long tokens  = response.optLong("total_tokens", 0L);
                    int sessions = response.optInt("total_sessions", 0);

                    usageMessages.setText(String.valueOf(requests));
                    usageTokens.setText(formatNumber(tokens));
                    if (sessions > 0) usageSessions.setText(String.valueOf(sessions));
                    applyTokenProgress(tokens);

                    prefs.edit()
                            .putInt("usage_messages", requests)
                            .putInt("usage_tokens", (int) Math.min(tokens, Integer.MAX_VALUE))
                            .apply();
                },
                error -> { /* Keep cached values silently */ });
        requestQueue.add(req);
    }

    private void applyTokenProgress(long tokens) {
        int pct = (int) Math.min(100, Math.round(tokens * 100.0 / TOKEN_BUDGET));
        usageProgressBar.setProgress(pct);
        usageBudgetLabel.setText(formatNumber(tokens) + " / 1M");
    }

    private static String formatNumber(long n) {
        if (n >= 1_000_000) return String.format(Locale.US, "%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format(Locale.US, "%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private void bindActions() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btnSaveProfile);
        btnSave.setOnClickListener(v -> saveProfile());

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            AuthManager.get(this).logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveProfile() {
        String name  = text(inputDisplayName);
        String email = text(inputEmail);
        String url   = text(inputBackendUrl);

        prefs.edit()
                .putString("display_name", name)
                .putString("email", email)
                .putString("backend_url", BackendConfig.normalize(url))
                .apply();

        displayName.setText(name);
        displayEmail.setText(email);
        avatarCircle.setText(initials(name));
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        fetchUsage();
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String initials(String name) {
        return name.isEmpty() ? "U" : String.valueOf(name.charAt(0)).toUpperCase();
    }
}
