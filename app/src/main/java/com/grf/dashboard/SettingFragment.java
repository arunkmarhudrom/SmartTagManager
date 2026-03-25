package com.grf.dashboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.grf.smarttagmanager.LoginActivity;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.utils.PopupUtils;
import com.grf.utils.PreferenceUtils;
import com.grf.utils.SnackbarUtils;

public class SettingFragment extends Fragment {

    private EditText etMinDbm, etMaxDbm, etGreenThreshold, etYellowThreshold;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        try {
            initViews(view);
            loadValues();       // load saved values
            setupListeners(view);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    private void initViews(View v) {
        try {
            etMinDbm = v.findViewById(R.id.etMinDbm);
            etMaxDbm = v.findViewById(R.id.etMaxDbm);
            etGreenThreshold = v.findViewById(R.id.etGreenThreshold);
            etYellowThreshold = v.findViewById(R.id.etYellowThreshold);
            btnSave = v.findViewById(R.id.btnSaveRssiSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners(View view) {
        try {
            ImageView back = view.findViewById(R.id.ivBack);
            back.setOnClickListener(v -> requireActivity().onBackPressed());

            btnSave.setOnClickListener(v->{

                PopupUtils.showCustomYesNoDialog(
                        requireContext(),
                        "Logout?",
                        "Are you sure you want to cahnge?",
                        new PopupUtils.PopupCallback() {
                            @Override
                            public void onYes() {
                                try {
                                    saveValues();

                                } catch (Exception e) {
                                    Log.e("TAG", "Logout yes error", e);
                                }
                            }

                            @Override
                            public void onNo() {
                                Log.d("TAG", "Logout cancelled");
                            }
                            @Override
                            public void onCLose() {
                                // no-op
                            }
                        }
                );


            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================
    // LOAD VALUES USING PreferenceUtils
    // ======================
    private void loadValues() {
        try {
            Context ctx = requireContext();

            etMinDbm.setText(PreferenceUtils.getString(ctx, "MIN_DBM", "-80"));
            etMaxDbm.setText(PreferenceUtils.getString(ctx, "MAX_DBM", "-40"));
            etGreenThreshold.setText(PreferenceUtils.getString(ctx, "GREEN_TH", "65"));
            etYellowThreshold.setText(PreferenceUtils.getString(ctx, "YELLOW_TH", "40"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================
    // SAVE VALUES USING PreferenceUtils
    // ======================
    private void saveValues() {
        try {
            Context ctx = requireContext();

            String minDbmStr = etMinDbm.getText().toString().trim();
            String maxDbmStr = etMaxDbm.getText().toString().trim();
            String greenThStr = etGreenThreshold.getText().toString().trim();
            String yellowThStr = etYellowThreshold.getText().toString().trim();

            // -----------------------------
            // EMPTY CHECK
            // -----------------------------
            if (minDbmStr.isEmpty() || maxDbmStr.isEmpty() ||
                    greenThStr.isEmpty() || yellowThStr.isEmpty()) {
                SnackbarUtils.show(requireView(), "Please fill all fields");
                return;
            }

            // -----------------------------
            // MUST START WITH "-"
            // -----------------------------
            if (!minDbmStr.startsWith("-") || !maxDbmStr.startsWith("-")) {
                SnackbarUtils.show(requireView(), "RSSI values must start with '-' (e.g., -40)");
                return;
            }

            int minDbm = Integer.parseInt(minDbmStr);
            int maxDbm = Integer.parseInt(maxDbmStr);

            int greenTh = Integer.parseInt(greenThStr);
            int yellowTh = Integer.parseInt(yellowThStr);

            final int TOP_LIMIT = -20;     // strongest allowed
            final int BOTTOM_LIMIT = -150; // weakest allowed

            // -----------------------------
            // RANGE VALIDATION
            // -----------------------------
            if (minDbm > TOP_LIMIT || minDbm < BOTTOM_LIMIT) {
                SnackbarUtils.show(requireView(), "Min dBm must be between -20 and -150");
                return;
            }

            if (maxDbm > TOP_LIMIT || maxDbm < BOTTOM_LIMIT) {
                SnackbarUtils.show(requireView(), "Max dBm must be between -20 and -150");
                return;
            }

            // -----------------------------
            // LOGIC: MIN MUST NOT BE STRONGER THAN MAX
            // -----------------------------
            if (minDbm > maxDbm) {
                SnackbarUtils.show(requireView(), "Min dBm cannot be greater (stronger) than Max dBm");
                return;
            }

            // -----------------------------
            // VALIDATE THRESHOLDS (0–100)
            // -----------------------------
            if (greenTh < 1 || greenTh > 100) {
                SnackbarUtils.show(requireView(), "Green threshold must be 1–100");
                return;
            }

            if (yellowTh < 1 || yellowTh > 100) {
                SnackbarUtils.show(requireView(), "Yellow threshold must be 1–100");
                return;
            }

            if (greenTh <= yellowTh) {
                SnackbarUtils.show(requireView(), "Green must be greater than Yellow");
                return;
            }

            // -----------------------------
            // SAVE VALUES
            // -----------------------------
            PreferenceUtils.setString(ctx, "MIN_DBM", minDbmStr);
            PreferenceUtils.setString(ctx, "MAX_DBM", maxDbmStr);
            PreferenceUtils.setString(ctx, "GREEN_TH", greenThStr);
            PreferenceUtils.setString(ctx, "YELLOW_TH", yellowThStr);

            SnackbarUtils.show(requireView(), "Settings Saved ✔");

            // -----------------------------
            // DELAYED BACK PRESS (200 ms)
            // -----------------------------
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    requireActivity().onBackPressed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 200);

        } catch (Exception e) {
            e.printStackTrace();
            SnackbarUtils.show(requireView(), "Error saving");
        }
    }


}
