package hk.hkucs.comp7506_project.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {

    private static final String PREFS_NAME   = "notemind_auth";
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL    = "email";

    private static AuthManager instance;
    private final SharedPreferences prefs;

    private AuthManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager get(Context ctx) {
        if (instance == null) instance = new AuthManager(ctx);
        return instance;
    }

    public void saveAuth(String token, int userId, String username, String email) {
        prefs.edit()
                .putString(KEY_TOKEN,    token)
                .putInt(KEY_USER_ID,     userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL,    email)
                .apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        String t = prefs.getString(KEY_TOKEN, null);
        return t != null && !t.isEmpty();
    }

    public String getToken()    { return prefs.getString(KEY_TOKEN,    ""); }
    public int    getUserId()   { return prefs.getInt(KEY_USER_ID,     -1); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, "Student"); }
    public String getEmail()    { return prefs.getString(KEY_EMAIL,    ""); }
}