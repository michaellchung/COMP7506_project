package hk.hkucs.comp7506_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import hk.hkucs.comp7506_project.data.SessionManager;
import hk.hkucs.comp7506_project.model.ChatMessage;
import hk.hkucs.comp7506_project.model.ChatSession;
import hk.hkucs.comp7506_project.ui.MessageAdapter;
import hk.hkucs.comp7506_project.ui.SessionAdapter;

public class ChatActivity extends AppCompatActivity {
    private static final String DEFAULT_BACKEND_URL = "http://127.0.0.1:5000";

    private DrawerLayout drawerLayout;
    private TextView sessionTitleText;
    private RecyclerView messagesRecyclerView;
    private RecyclerView sessionsRecyclerView;
    private EditText chatInput;

    private SessionManager sessionManager;
    private List<ChatSession> sessions;
    private ChatSession currentSession;

    private MessageAdapter messageAdapter;
    private SessionAdapter sessionAdapter;
    private LinearLayoutManager msgLayoutManager;

    private RequestQueue requestQueue;
    private String backendUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("notemind_profile", MODE_PRIVATE);
        backendUrl = normalizeBackendUrl(prefs.getString("backend_url", DEFAULT_BACKEND_URL));
        requestQueue = Volley.newRequestQueue(this);

        sessionManager = new SessionManager(this);
        sessions = sessionManager.loadSessions();

        bindViews();
        initSessions();
        bindActions();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        sessionTitleText = findViewById(R.id.sessionTitleText);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        chatInput = findViewById(R.id.chatInput);
    }

    private void initSessions() {
        if (sessions.isEmpty()) {
            createNewSession();
        } else {
            String savedId = sessionManager.getCurrentSessionId();
            currentSession = sessions.stream()
                    .filter(s -> s.getId().equals(savedId))
                    .findFirst()
                    .orElse(sessions.get(0));
        }

        msgLayoutManager = new LinearLayoutManager(this);
        msgLayoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(msgLayoutManager);
        messageAdapter = new MessageAdapter(currentSession.getMessages());
        messagesRecyclerView.setAdapter(messageAdapter);

        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionAdapter = new SessionAdapter(sessions, session -> {
            switchToSession(session);
            drawerLayout.closeDrawers();
        });
        sessionAdapter.setActiveSessionId(currentSession.getId());
        sessionsRecyclerView.setAdapter(sessionAdapter);

        updateTitleBar();
    }

    private void bindActions() {
        ImageButton btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);
        ImageButton btnSend = findViewById(R.id.btnSend);
        MaterialButton btnNewChatDrawer = findViewById(R.id.btnNewChatDrawer);

        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnNewChat.setOnClickListener(v -> {
            createNewSession();
            drawerLayout.closeDrawers();
        });

        btnNewChatDrawer.setOnClickListener(v -> {
            createNewSession();
            drawerLayout.closeDrawers();
        });

        btnSend.setOnClickListener(v -> sendMessage());

        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void createNewSession() {
        ChatSession session = new ChatSession("New Chat");
        sessions.add(0, session);
        sessionManager.saveSessions(sessions);
        switchToSession(session);
        if (sessionAdapter != null) sessionAdapter.notifyDataSetChanged();
    }

    private void switchToSession(ChatSession session) {
        currentSession = session;
        sessionManager.setCurrentSessionId(session.getId());
        messageAdapter = new MessageAdapter(currentSession.getMessages());
        messagesRecyclerView.setAdapter(messageAdapter);
        if (sessionAdapter != null) sessionAdapter.setActiveSessionId(session.getId());
        updateTitleBar();
        scrollToBottom();
    }

    private void sendMessage() {
        String text = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        chatInput.setText("");

        ChatMessage userMsg = new ChatMessage(text, ChatMessage.TYPE_USER);
        currentSession.addMessage(userMsg);

        if (currentSession.getMessages().size() == 1) {
            currentSession.setTitle(text.length() > 32 ? text.substring(0, 32) + "…" : text);
            updateTitleBar();
        }
        messageAdapter.notifyItemInserted(currentSession.getMessages().size() - 1);
        scrollToBottom();

        ChatMessage aiMsg = new ChatMessage("Thinking…", ChatMessage.TYPE_AI);
        currentSession.addMessage(aiMsg);
        int aiIndex = currentSession.getMessages().size() - 1;
        messageAdapter.notifyItemInserted(aiIndex);
        scrollToBottom();

        callBackend(text, aiMsg, aiIndex);
    }

    private void callBackend(String question, ChatMessage aiMsg, int aiIndex) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("question", question);
            payload.put("session_id", currentSession.getId());

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    backendUrl + "/api/kb/ask",
                    payload,
                    response -> {
                        String answer = response.optString("answer", "No response from server.");
                        aiMsg.setContent(answer);
                        messageAdapter.notifyItemChanged(aiIndex);
                        sessionManager.saveSessions(sessions);
                        scrollToBottom();
                    },
                    error -> {
                        aiMsg.setContent(buildConnectionErrorMessage(error));
                        messageAdapter.notifyItemChanged(aiIndex);
                        sessionManager.saveSessions(sessions);
                    }
            );
            requestQueue.add(req);
        } catch (JSONException e) {
            aiMsg.setContent("Error preparing request.");
            messageAdapter.notifyItemChanged(aiIndex);
        }
    }

    private void updateTitleBar() {
        sessionTitleText.setText(currentSession.getTitle());
        if (sessionAdapter != null) sessionAdapter.notifyDataSetChanged();
    }

    private String normalizeBackendUrl(String url) {
        String normalized = TextUtils.isEmpty(url) ? DEFAULT_BACKEND_URL : url.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // 10.0.2.2 only works on Android Emulator. USB real-device debugging uses adb reverse.
        if ("http://10.0.2.2:5000".equals(normalized)) {
            normalized = DEFAULT_BACKEND_URL;
        }
        return normalized;
    }

    private String buildConnectionErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            return "Server returned HTTP " + error.networkResponse.statusCode
                    + ". Backend URL: " + backendUrl;
        }
        return "Could not connect to server. Backend URL: " + backendUrl
                + "\nFor USB debugging, keep adb reverse enabled and use http://127.0.0.1:5000.";
    }

    private void scrollToBottom() {
        if (messageAdapter == null || messagesRecyclerView.getLayoutManager() == null) return;
        int count = messageAdapter.getItemCount();
        if (count > 0) messagesRecyclerView.scrollToPosition(count - 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sessionManager.saveSessions(sessions);
    }
}
