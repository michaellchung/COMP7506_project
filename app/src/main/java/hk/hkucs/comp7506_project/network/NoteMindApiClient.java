package hk.hkucs.comp7506_project.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class NoteMindApiClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:5000";

    private final RequestQueue requestQueue;
    private final String baseUrl;

    public NoteMindApiClient(Context context) {
        this(context, DEFAULT_BASE_URL);
    }

    public NoteMindApiClient(Context context, String baseUrl) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.baseUrl = baseUrl;
    }

    public void summarizeRecording(JSONObject payload, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        post("/api/recording/summarize", payload, listener, errorListener);
    }

    public void extractTextFromPhoto(JSONObject payload, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        post("/api/ocr", payload, listener, errorListener);
    }

    public void askKnowledgeBase(JSONObject payload, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        post("/api/kb/ask", payload, listener, errorListener);
    }

    private void post(String path, JSONObject payload, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                baseUrl + path,
                payload,
                listener,
                errorListener
        );
        requestQueue.add(request);
    }
}
