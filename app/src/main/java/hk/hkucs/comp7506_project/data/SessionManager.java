package hk.hkucs.comp7506_project.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import hk.hkucs.comp7506_project.model.ChatMessage;
import hk.hkucs.comp7506_project.model.ChatSession;

public class SessionManager {
    private static final String PREFS_NAME = "notemind_sessions";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_CURRENT_ID = "current_session_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<ChatSession> loadSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        String json = prefs.getString(KEY_SESSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ChatSession session = new ChatSession(
                        obj.getString("id"),
                        obj.getString("title"),
                        obj.getLong("createdAt")
                );
                JSONArray msgs = obj.getJSONArray("messages");
                for (int j = 0; j < msgs.length(); j++) {
                    JSONObject m = msgs.getJSONObject(j);
                    session.addMessage(new ChatMessage(m.getString("content"), m.getInt("type")));
                }
                sessions.add(session);
            }
        } catch (JSONException ignored) {
        }
        return sessions;
    }

    public void saveSessions(List<ChatSession> sessions) {
        try {
            JSONArray arr = new JSONArray();
            for (ChatSession s : sessions) {
                JSONObject obj = new JSONObject();
                obj.put("id", s.getId());
                obj.put("title", s.getTitle());
                obj.put("createdAt", s.getCreatedAt());
                JSONArray msgs = new JSONArray();
                for (ChatMessage m : s.getMessages()) {
                    JSONObject mo = new JSONObject();
                    mo.put("content", m.getContent());
                    mo.put("type", m.getType());
                    msgs.put(mo);
                }
                obj.put("messages", msgs);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public String getCurrentSessionId() {
        return prefs.getString(KEY_CURRENT_ID, null);
    }

    public void setCurrentSessionId(String id) {
        prefs.edit().putString(KEY_CURRENT_ID, id).apply();
    }
}
