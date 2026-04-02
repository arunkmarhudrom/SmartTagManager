package com.grf.smarttagmanager;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.grf.api.ApiHelper;
import com.grf.helper.LoaderUtil;
import com.grf.helper.SharedPreferencesHelper;
import com.grf.helper.TokenManager;
import com.grf.smarttagmanager.databinding.ActivityLoginBinding;
import com.grf.utils.DeviceUtils;
import com.grf.utils.PermissionUtil;
import com.grf.utils.SnackbarUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private static final int REQ_STORAGE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        SharedPreferencesHelper pref = new SharedPreferencesHelper(this);
        pref.putString("base_url", "https://whrfid.lenskart.com/api/v1/api/");
        // pref.putString("base_url", "https://rfidapi.bhishmcube.com/v1/api/");

        String deviceId = DeviceUtils.getOrCreateDeviceId(LoginActivity.this);

        boolean isFirstLogin = pref.getBoolean("first_login");

        if (isFirstLogin) {
            binding.deviceID.setVisibility(VISIBLE);
            binding.deviceID.setText(deviceId);
        }
        setContentView(binding.getRoot());
        PermissionUtil.requestStoragePermission(
                LoginActivity.this,
                REQ_STORAGE
        );
        initListeners();
    }

    private void initListeners() {
        try {

            binding.btnSignIn.setOnClickListener(v -> {
                try {
                    String username = binding.etUsername.getText() != null
                            ? binding.etUsername.getText().toString().trim()
                            : "";

                    String password = binding.etPassword.getText() != null
                            ? binding.etPassword.getText().toString().trim()
                            : "";

                    if (TextUtils.isEmpty(username) || username.length() < 4) {
                        SnackbarUtils.show(binding.getRoot(),
                                "Enter valid username (min 4 chars)");
                        return;
                    }

                    if (TextUtils.isEmpty(password) || password.length() < 4) {
                        SnackbarUtils.show(binding.getRoot(),
                                "Enter valid password (min 4 chars)");
                        return;
                    }

                    LoaderUtil.show(this, "Please wait...");
                    try {

                        JSONObject body = new JSONObject();
                        body.put("username", username);
                        body.put("password", password);
                        body.put("mac", App.getDeviceMac());

                        ApiHelper.post(this, "device-login", body.toString(), new ApiHelper.ApiCallback() {

                            @Override
                            public void onSuccess(int statusCode, String response) {
                                try {
                                    if (statusCode == 200) {

                                        SharedPreferencesHelper sr = new SharedPreferencesHelper(LoginActivity.this);
                                        sr.putString("login_user", username);

                                        if (sr.getBoolean("first_login")) {
                                            sr.putBoolean("first_login", false);
                                        }


                                        JSONObject json = new JSONObject(response);

                                        boolean status = json.optBoolean("status", false);
                                        boolean success = json.optBoolean("success", false);
                                        int apiStatusCode = json.optInt("statusCode", 0);

                                        if (status && success && apiStatusCode == 200) {

                                            JSONObject data = json.optJSONObject("data");
                                            if (data != null) {
                                                String token = data.optString("token", null);
                                                String deviceId = data.optString("device_id", null);
                                                App.setDeviceMac(deviceId);
                                                if (token != null && !token.isEmpty()) {
                                                    TokenManager.getInstance().setToken(token);

                                                    LoaderUtil.hide();
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                } else {
                                                    LoaderUtil.hide();
                                                    SnackbarUtils.show(binding.getRoot(), "Token missing");
                                                }
                                            } else {
                                                LoaderUtil.hide();
                                                SnackbarUtils.show(binding.getRoot(), "Invalid response data");
                                            }

                                        } else {
                                            LoaderUtil.hide();
                                            String message = json.optString("message", "Login failed");
                                            SnackbarUtils.show(binding.getRoot(), message);
                                        }

                                    } else {
                                        LoaderUtil.hide();

                                        String msg = response;
                                        try {
                                            JSONObject json = new JSONObject(response);
                                            msg = json.optString("message", "Failed to login");
                                        } catch (Exception e) {

                                        }
                                        SnackbarUtils.show(binding.getRoot(), statusCode == 401 ? msg : response);

                                    }

                                } catch (Exception e) {
                                    LoaderUtil.hide();
                                    e.printStackTrace();
                                    SnackbarUtils.show(binding.getRoot(), "Parsing error");
                                }
                            }

                            @Override
                            public void onError(int statusCode, String error) {
                                LoaderUtil.hide();
                                String msg = error;
                                try {
                                    JSONObject json = new JSONObject(error);
                                    msg = json.optString("message", "Failed to login");
                                } catch (Exception e) {

                                }
                                SnackbarUtils.show(binding.getRoot(), statusCode == 401 ? msg : error);
                            }
                        });

                    } catch (JSONException e) {
                        LoaderUtil.hide();
                        e.printStackTrace();
                    }


                } catch (Exception ex) {
                    ex.printStackTrace();
                    SnackbarUtils.show(binding.getRoot(),
                            "Login Failed: " + ex.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionUtil.handleResult(
                requestCode,
                REQ_STORAGE,
                permissions,
                grantResults,
                new PermissionUtil.PermissionCallback() {
                    @Override
                    public void onGranted() {
                        Toast.makeText(LoginActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                        // ==> Now you can access external storage (Android < 11)
                    }

                    @Override
                    public void onDenied() {
                        Toast.makeText(LoginActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


}