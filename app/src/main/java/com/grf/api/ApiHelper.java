package com.grf.api;

import android.content.Context;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONObject;

/**
 * ApiHelper — all POST / PUT / PATCH bodies are enforced as JSON strings.
 *
 * Usage from any Activity or Fragment:
 *
 *   // Build your JSON string however you like:
 *   JSONObject body = new JSONObject();
 *   body.put("username", "john");
 *   body.put("password", "secret");
 *
 *   ApiHelper.post(this, "auth/login", body.toString(), new ApiHelper.ApiCallback() {
 *       public void onSuccess(int statusCode, String response) { ... }
 *       public void onError(int statusCode, String error)      { ... }
 *   });
 */
public class ApiHelper {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // ── Callback interface ────────────────────────────────────────────────────
    public interface ApiCallback {
        void onSuccess(int statusCode, String response);
        void onError(int statusCode, String error);
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    public static void get(Context ctx, String endpoint, ApiCallback cb) {
        enqueue(service(ctx).getRequest(endpoint), cb);
    }

    public static void getWithParams(Context ctx, String endpoint,
                                     Map<String, String> params, ApiCallback cb) {
        enqueue(service(ctx).getRequestWithParams(endpoint, params), cb);
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    /**
     * @param jsonBody  Pass a raw JSON string, e.g. new JSONObject().put("key","val").toString()
     */
    public static void post(Context ctx, String endpoint, String jsonBody, ApiCallback cb) {
        enqueue(service(ctx).postRequest(endpoint, toRequestBody(jsonBody)), cb);
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    public static void put(Context ctx, String endpoint, String jsonBody, ApiCallback cb) {
        enqueue(service(ctx).putRequest(endpoint, toRequestBody(jsonBody)), cb);
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────

    public static void patch(Context ctx, String endpoint, String jsonBody, ApiCallback cb) {
        enqueue(service(ctx).patchRequest(endpoint, toRequestBody(jsonBody)), cb);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public static void delete(Context ctx, String endpoint, ApiCallback cb) {
        enqueue(service(ctx).deleteRequest(endpoint), cb);
    }

    // ── Update base URL at runtime ────────────────────────────────────────────

    public static void updateBaseUrl(Context ctx, String newBaseUrl) {
        ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("base_url", newBaseUrl)
                .apply();
        RetrofitClient.reset(ctx);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Converts a JSON string into an OkHttp RequestBody with Content-Type: application/json */
    private static RequestBody toRequestBody(String jsonBody) {
        return RequestBody.create(JSON, jsonBody != null ? jsonBody : "{}");
    }

    private static ApiService service(Context ctx) {
        return RetrofitClient.getInstance(ctx).create(ApiService.class);
    }

    private static void enqueue(Call<ResponseBody> call, ApiCallback cb) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    int code = response.code();
                    if (response.isSuccessful() && response.body() != null) {
                        cb.onSuccess(code, response.body().string());
                    } else {
                        String errBody = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Unknown error";
                        cb.onError(code, errBody);
                    }
                } catch (Exception e) {
                    cb.onError(-1, "Parse error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                cb.onError(-1, "Network failure: " + t.getMessage());
            }
        });
    }
}