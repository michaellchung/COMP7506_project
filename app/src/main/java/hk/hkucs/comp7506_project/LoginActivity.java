package hk.hkucs.comp7506_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import hk.hkucs.comp7506_project.data.AuthManager;
import hk.hkucs.comp7506_project.data.BackendConfig;

public class LoginActivity extends AppCompatActivity {

    private boolean isRegisterMode = false;

    private TextView tvFormTitle;
    private TextView tvToggleLabel;
    private TextView tvToggle;
    private TextInputLayout layoutUsername;
    private TextInputEditText inputUsername;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private MaterialButton btnAction;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        requestQueue = Volley.newRequestQueue(this);
        bindViews();
    }

    private void bindViews() {
        tvFormTitle    = findViewById(R.id.tvFormTitle);
        tvToggleLabel  = findViewById(R.id.tvToggleLabel);
        tvToggle       = findViewById(R.id.tvToggle);
        layoutUsername = findViewById(R.id.layoutUsername);
        inputUsername  = findViewById(R.id.inputUsername);
        inputEmail     = findViewById(R.id.inputEmail);
        inputPassword  = findViewById(R.id.inputPassword);
        btnAction      = findViewById(R.id.btnAction);

        tvToggle.setOnClickListener(v -> switchMode());
        btnAction.setOnClickListener(v -> submit());
    }

    private void switchMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            tvFormTitle.setText("Create Account");
            btnAction.setText("Sign Up");
            tvToggleLabel.setText("Already have an account? ");
            tvToggle.setText("Sign In");
            layoutUsername.setVisibility(View.VISIBLE);
        } else {
            tvFormTitle.setText("Sign In");
            btnAction.setText("Sign In");
            tvToggleLabel.setText("Don't have an account? ");
            tvToggle.setText("Sign Up");
            layoutUsername.setVisibility(View.GONE);
        }
    }

    private void submit() {
        String email    = text(inputEmail);
        String password = text(inputPassword);

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isRegisterMode) {
            String username = text(inputUsername);
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }
            doRegister(username, email, password);
        } else {
            doLogin(email, password);
        }
    }

    private void doLogin(String email, String password) {
        String url = BackendConfig.getUrl(this) + "/api/login";
        JSONObject body = new JSONObject();
        try {
            body.put("email",    email);
            body.put("password", password);
        } catch (JSONException e) { setLoading(false); return; }

        requestQueue.add(new JsonObjectRequest(Request.Method.POST, url, body,
                this::handleAuthResponse, this::handleError));
    }

    private void doRegister(String username, String email, String password) {
        String url = BackendConfig.getUrl(this) + "/api/register";
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("email",    email);
            body.put("password", password);
        } catch (JSONException e) { setLoading(false); return; }

        requestQueue.add(new JsonObjectRequest(Request.Method.POST, url, body,
                this::handleAuthResponse, this::handleError));
    }

    private void handleAuthResponse(JSONObject response) {
        String token    = response.optString("token");
        int    userId   = response.optInt("user_id", -1);
        String username = response.optString("username", "Student");
        String email    = response.optString("email", "");
        AuthManager.get(this).saveAuth(token, userId, username, email);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleError(com.android.volley.VolleyError error) {
        setLoading(false);
        String msg = isRegisterMode ? "Registration failed" : "Login failed";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                JSONObject err = new JSONObject(new String(error.networkResponse.data));
                msg = err.optString("error", msg);
            } catch (JSONException ignored) {}
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean loading) {
        btnAction.setEnabled(!loading);
        btnAction.setText(loading ? "Please wait…" : (isRegisterMode ? "Sign Up" : "Sign In"));
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}