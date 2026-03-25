package com.grf.smarttagmanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.grf.uhfmanager.ReconnectManager;
import com.grf.uhfmanager.UhfManagerHelper;
import com.grf.uhfmanager.ZebraReader;
import com.grf.utils.LogUtils;
import com.grf.utils.OnKeyPressHandler;
import com.grf.utils.PopupUtils;
import com.grf.utils.ProgressUtil;
import com.grf.utils.SnackbarUtils;
import com.grf.viewmodel.ModuleViewModel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NavController navController;


    private ReconnectManager reconnectManager;
    private ExecutorService scheduler;


    private UhfManagerHelper uhfHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            KeyPress();
            try {
                View rootView = getWindow().getDecorView().getRootView();
                NavHostFragment navHostFragment = (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                if (navHostFragment != null) {
                    navController = navHostFragment.getNavController();
                }


                if (App.ReaderType == 1) {
                    scheduler = Executors.newSingleThreadExecutor();
                    uhfHelper = new UhfManagerHelper(this);
                    uhfHelper.init();
                    uhfHelper.setPromptSoundEnable(false);

                    LogUtils.d("All Setting : " + uhfHelper.mapToJson(uhfHelper.GetAllParams()));


                    // power on (async)uhfHelper.powerOnAsync();
                    // ------------------------------------
                    //   START CONNECT + RETRY PROCESS
                    // ------------------------------------
                    // connectWithRetries();     // ← ADD THIS LINE
                    // startHealthMonitoring();  // ← ADD THIS LINE
                    // uhfHelper.powerOnAsync();

                    reconnectManager = new ReconnectManager(this, scheduler, uhfHelper);
                    reconnectManager.start();   // registers screen ON/OFF receiver

                    // ⭐ AUTO-FIRST CONNECT (no manual button)
                    reconnectManager.ensureConnected();
                } else {
                    ZebraReader.init(MainActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ProgressUtil.showLoading(MainActivity.this, "Initialize Reader....");
                        }
                    });

                    ZebraReader.getInstance().InitReader(isConnected -> {
                        Log.e(TAG, "InitReader Callback Received");
                        if (isConnected) {
                            SnackbarUtils.show(rootView, "Reader initialized successfully");
                            ProgressUtil.dismiss();

                        } else {
                            SnackbarUtils.show(rootView, "Something went wrong. Try again.");
                            ProgressUtil.dismiss();
                        }

                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "NavHostFragment init error", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate error", e);
        }
    }

    public UhfManagerHelper getUhfManagerHelper() {
        return uhfHelper;
    }

    void KeyPress() {
        try {

            // Register a lifecycle-aware back callback that always runs first
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    try {
                        Log.d(TAG, "OnBackPressedCallback triggered");

                        if (navController == null) {
                            Log.d(TAG, "navController null → calling default finish()");
                            if (isEnabled()) {
                                // disable this callback and call default behavior
                                setEnabled(false);
                                MainActivity.this.onBackPressed();
                                setEnabled(true);
                            } else {
                                MainActivity.this.onBackPressed();
                            }
                            return;
                        }

                        Integer currentId = null;
                        if (navController.getCurrentDestination() != null) {
                            currentId = navController.getCurrentDestination().getId();
                        }
                        Log.d(TAG, "Current destination ID = " + currentId);

                        // Show logout popup only when on dashboardFragment (root)
                        if (currentId != null && currentId == R.id.dashboardFragment) {
                            Log.d(TAG, "At dashboard → showing logout popup");
                            PopupUtils.showCustomYesNoDialog(
                                    MainActivity.this,
                                    "Logout?",
                                    "Are you sure you want to logout?",
                                    new PopupUtils.PopupCallback() {
                                        @Override
                                        public void onYes() {
                                            try {
                                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                                startActivity(intent);
                                                finish(); // close MainActivity
                                            } catch (Exception e) {
                                                Log.e(TAG, "Logout onYes error", e);
                                            }
                                        }

                                        @Override
                                        public void onNo() {
                                            Log.d(TAG, "Logout cancelled");
                                        }

                                        @Override
                                        public void onCLose() {
                                            // no-op
                                        }
                                    }
                            );
                            // do not call super; we handled it
                            return;
                        }

                        // Otherwise try navController navigateUp
                        boolean navigatedUp = false;
                        try {
                            navigatedUp = navController.navigateUp();
                        } catch (Exception e) {
                            Log.w(TAG, "navigateUp threw", e);
                        }
                        Log.d(TAG, "navigateUp returned = " + navigatedUp);

                        if (!navigatedUp) {
                            Log.d(TAG, "navigateUp false → finishing / default back");
                            // no nav up available -> default behavior
                            // temporarily disable this callback and call activity's onBackPressed to avoid loop
                            setEnabled(false);
                            MainActivity.super.onBackPressed();
                            setEnabled(true);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "OnBackPressedCallback error", e);
                        // fallback
                        setEnabled(false);
                        MainActivity.super.onBackPressed();
                        setEnabled(true);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }


    // --- key handling: override dispatchKeyEvent so both down & up are captured centrally ---
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event == null) {
            return super.dispatchKeyEvent(event);
        }

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        // Only handle key down/up for DPAD navigation (optional filter)
        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }

        try {
            // 1. Let the current fragment handle the key if it wants to
            Fragment currentFragment = getCurrentNavPrimaryFragment();
            if (currentFragment instanceof OnKeyPressHandler) {
                OnKeyPressHandler handler = (OnKeyPressHandler) currentFragment;
                boolean consumed = (action == KeyEvent.ACTION_DOWN)
                        ? handler.onKeyDownEvent(keyCode, event)
                        : handler.onKeyUpEvent(keyCode, event);

                if (consumed) {
                    return true; // Fragment consumed the event
                }
            }

            // 2. Fallback: Activity-level handling based on current NavDestination ID
            if (navController != null && navController.getCurrentDestination() != null) {
                int currentId = navController.getCurrentDestination().getId();

                // Use if-else instead of switch to avoid "constant expression required" in Java
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (currentId == R.id.dashboardFragment) {
                        Log.d(TAG, "DOWN on dashboard");
                        return true;
                    } else if (currentId == R.id.moduleAFragment) {
                        Log.d(TAG, "DOWN on Module A");
                        return true;
                    }
                    // Add more destinations as needed
                    // } else if (currentId == R.id.someOtherFragment) { ... }

                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (currentId == R.id.dashboardFragment) {
                        Log.d(TAG, "UP on dashboard");
                        return true;
                    } else if (currentId == R.id.moduleAFragment) {
                        Log.d(TAG, "UP on Module A");
                        return true;
                    }
                    // Add more as needed
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in dispatchKeyEvent", e);
        }

        // If no one consumed it, let the system handle it (e.g. navigation, back button, etc.)
        return super.dispatchKeyEvent(event);
    }

    /**
     * Helper: get the current primary fragment inside the NavHostFragment
     */
    private Fragment getCurrentNavPrimaryFragment() {
        try {
            Fragment navHost = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHost == null) return null;
            Fragment primary = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
            return primary;
        } catch (Exception e) {
            Log.w(TAG, "getCurrentNavPrimaryFragment failed", e);
            return null;
        }
    }

    // keep your onSupportNavigateUp and onBackPressed unchanged (or as previously fixed)
    @Override
    public boolean onSupportNavigateUp() {
        try {
            if (navController != null) {
                return navController.navigateUp() || super.onSupportNavigateUp();
            }
        } catch (Exception e) {
            Log.e(TAG, "navigateUp error", e);
        }
        return super.onSupportNavigateUp();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() called");

        try {
            if (navController == null) {
                Log.d(TAG, "navController is null → default back");
                super.onBackPressed();
                return;
            }

            int currentId = navController.getCurrentDestination().getId();
            Log.d(TAG, "Current destination = " + currentId);

            // 👉 SHOW POPUP ONLY ON MAIN SCREEN (dashboardFragment)
            if (currentId == R.id.dashboardFragment) {

                Log.d(TAG, "Dashboard screen → showing logout dialog");

                PopupUtils.showCustomYesNoDialog(
                        this,
                        "Logout?",
                        "Are you sure you want to logout?",
                        new PopupUtils.PopupCallback() {
                            @Override
                            public void onYes() {
                                try {
                                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish(); // closes MainActivity
                                } catch (Exception e) {
                                    Log.e(TAG, "Logout yes error", e);
                                }
                            }

                            @Override
                            public void onNo() {
                                Log.d(TAG, "Logout cancelled");
                            }

                            @Override
                            public void onCLose() {
                                // no-op
                            }
                        }
                );

                // ❗ VERY IMPORTANT → STOP back press here
                return;
            }

            // 👉 Other fragments → normal navigation back
            boolean navigatedUp = navController.navigateUp();
            Log.d(TAG, "navigateUp = " + navigatedUp);

            if (!navigatedUp) {
                super.onBackPressed();
            }

        } catch (Exception e) {
            Log.e(TAG, "onBackPressed error", e);
            super.onBackPressed();
        }
    }


    @Override
    protected void onDestroy() {
        try {

            if (App.ReaderType == 2) {
                ZebraReader.getInstance().disconnectReader();
            } else {
                uhfHelper.destroy();

                try {
                    reconnectManager.stop();
                    try {
                        scheduler.shutdownNow();
                    } catch (Exception ignored) {
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            Log.e("MAIN", "destroy error", e);
        } finally {
            super.onDestroy();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (App.ReaderType == 1)
                reconnectManager.onVisible();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (App.ReaderType == 1)
                reconnectManager.onInvisible();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//PreferenceUtils.setString(this, "NAME", "Arun");
//    String name = PreferenceUtils.getString(this, "NAME", "");
//
//PreferenceUtils.setInt(this, "AGE", 25);
//    int age = PreferenceUtils.getInt(this, "AGE", 0);
//
//PreferenceUtils.setBoolean(this, "ACTIVE", true);
//    boolean active = PreferenceUtils.getBoolean(this, "ACTIVE", false);

    private static final int FILE_PICKER_REQUEST = 1001;

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 2001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        try {
//            if (requestCode == 2001 && resultCode == RESULT_OK) {
//                Uri treeUri = data.getData();
//
//                getContentResolver().takePersistableUriPermission(
//                        treeUri,
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
//                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                );
//
//                Toast.makeText(this, "Folder Granted: " + treeUri, Toast.LENGTH_LONG).show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}