package com.grf.smarttagmanager;

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

import com.grf.smarttagmanager.databinding.ActivityLoginBinding;
import com.grf.utils.PermissionUtil;
import com.grf.utils.SnackbarUtils;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private static final int REQ_STORAGE = 2001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
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

                    SnackbarUtils.show(binding.getRoot(), "Login Success");

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    // Optional: close the current activity (e.g., LoginActivity) so user can't go back
                    finish();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SnackbarUtils.show(binding.getRoot(),
                            "Login Failed: Error occurred");
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