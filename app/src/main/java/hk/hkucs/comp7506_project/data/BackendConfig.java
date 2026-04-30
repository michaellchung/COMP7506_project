package hk.hkucs.comp7506_project.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class BackendConfig {
    public static final String PREFS           = "notemind_profile";
    public static final String DEFAULT_BACKEND = "http://127.0.0.1:5000";
    private static final String KEY_BACKEND_URL = "backend_url";

    private BackendConfig() {}

    public static String getUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalize(prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND));
    }

    public static String normalize(String url) {
        if (TextUtils.isEmpty(url)) return DEFAULT_BACKEND;
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if ("http://10.0.2.2:5000".equals(u)) u = DEFAULT_BACKEND;
        return u;
    }
}
