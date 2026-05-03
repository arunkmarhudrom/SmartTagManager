package com.grf.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.cipherlab.rfidapi.BuildConfig;
import com.grf.helper.TokenManager;
import com.grf.smarttagmanager.App;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

/**
 * RetrofitClient — Singleton
 * No GsonConverterFactory needed — all bodies are raw JSON strings (RequestBody).
 * Base URL: read from SharedPreferences, fallback to DEFAULT_BASE_URL.
 */
public class RetrofitClient {

    private static final String DEFAULT_BASE_URL = "https://whrfid.lenskart.com/api/v1/api/";
   // private static final String DEFAULT_BASE_URL = "https://whrfid.lenskart.com/api/v1/api/";

    private static final String PREFS_NAME = "app_settings";
    private static final String PREFS_KEY_URL = "base_url";

    private static RetrofitClient instance;
    private Retrofit retrofit;

    private RetrofitClient(Context context) {
        String baseUrl = resolveBaseUrl(context);
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(buildOkHttpClient())
                // ✅ No GsonConverterFactory — we use raw RequestBody / ResponseBody
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Call after updating base URL in SharedPreferences to rebuild the client.
     */
    public static synchronized void reset(Context context) {
        instance = new RetrofitClient(context.getApplicationContext());
    }

    public <T> T create(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(PREFS_KEY_URL, null);
        return (saved != null && !saved.isEmpty()) ? ensureTrailingSlash(saved) : DEFAULT_BASE_URL;
    }

    private OkHttpClient BypassbuildOkHttpClient() {
        try {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        try {
                            String token = TokenManager.getInstance().getToken();
                            Request original = chain.request();

                            if (token != null && !token.isEmpty()) {
                                Request request = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build();
                                return chain.proceed(request);
                            }

                            return chain.proceed(original);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return chain.proceed(chain.request());
                        }
                    });

            // 🔥 SSL BYPASS ONLY IN DEBUG
            if (BuildConfig.DEBUG) {
                try {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                            }
                    };

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                    builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                    builder.hostnameVerifier((hostname, session) -> true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return builder.build();

        } catch (Exception e) {
            e.printStackTrace();
            return new OkHttpClient();
        }
    }
    private OkHttpClient buildOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    try {
                        String token = TokenManager.getInstance().getToken();
                        Request original = chain.request();

                        if (token != null && !token.isEmpty()) {
                            Request request = original.newBuilder().header("Authorization", "Bearer " + token).build();
                            return chain.proceed(request);
                        }

                        return chain.proceed(original);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return chain.proceed(chain.request());
                    }
                })
                .build();
    }

    private String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}