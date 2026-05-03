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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.grf.smarttagmanager.LoginActivity;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.utils.PopupUtils;
import com.grf.utils.PreferenceUtils;
import com.grf.utils.SnackbarUtils;

public class SettingFragment extends Fragment {

    private Slider sliderMinDbm, sliderMaxDbm, sliderGreenThreshold, sliderYellowThreshold;
    private TextView tvMinDbmValue, tvMaxDbmValue, tvGreenThresholdValue, tvYellowThresholdValue;
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
            sliderMinDbm = v.findViewById(R.id.sliderMinDbm);
            sliderMaxDbm = v.findViewById(R.id.sliderMaxDbm);
            sliderGreenThreshold = v.findViewById(R.id.sliderGreenThreshold);
            sliderYellowThreshold = v.findViewById(R.id.sliderYellowThreshold);

            tvMinDbmValue = v.findViewById(R.id.tvMinDbmValue);
            tvMaxDbmValue = v.findViewById(R.id.tvMaxDbmValue);
            tvGreenThresholdValue = v.findViewById(R.id.tvGreenThresholdValue);
            tvYellowThresholdValue = v.findViewById(R.id.tvYellowThresholdValue);

            btnSave = v.findViewById(R.id.btnSaveRssiSettings);

            setupSliderListeners();
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

            int minDbm = parseOrDefault(PreferenceUtils.getString(ctx, "MIN_DBM", "-80"), -80);
            int maxDbm = parseOrDefault(PreferenceUtils.getString(ctx, "MAX_DBM", "-40"), -40);
            int greenTh = parseOrDefault(PreferenceUtils.getString(ctx, "GREEN_TH", "65"), 65);
            int yellowTh = parseOrDefault(PreferenceUtils.getString(ctx, "YELLOW_TH", "40"), 40);

            sliderMinDbm.setValue(minDbm);
            sliderMaxDbm.setValue(maxDbm);
            sliderGreenThreshold.setValue(greenTh);
            sliderYellowThreshold.setValue(yellowTh);

            updateValueLabels();

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

            int minDbm = Math.round(sliderMinDbm.getValue());
            int maxDbm = Math.round(sliderMaxDbm.getValue());
            int greenTh = Math.round(sliderGreenThreshold.getValue());
            int yellowTh = Math.round(sliderYellowThreshold.getValue());

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
            PreferenceUtils.setString(ctx, "MIN_DBM", String.valueOf(minDbm));
            PreferenceUtils.setString(ctx, "MAX_DBM", String.valueOf(maxDbm));
            PreferenceUtils.setString(ctx, "GREEN_TH", String.valueOf(greenTh));
            PreferenceUtils.setString(ctx, "YELLOW_TH", String.valueOf(yellowTh));

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

    private void setupSliderListeners() {
        Slider.OnChangeListener onChangeListener = (slider, value, fromUser) -> updateValueLabels();
        sliderMinDbm.addOnChangeListener(onChangeListener);
        sliderMaxDbm.addOnChangeListener(onChangeListener);
        sliderGreenThreshold.addOnChangeListener(onChangeListener);
        sliderYellowThreshold.addOnChangeListener(onChangeListener);
    }

    private void updateValueLabels() {
        try {
            tvMinDbmValue.setText(Math.round(sliderMinDbm.getValue()) + " dBm");
            tvMaxDbmValue.setText(Math.round(sliderMaxDbm.getValue()) + " dBm");
            tvGreenThresholdValue.setText(Math.round(sliderGreenThreshold.getValue()) + "%");
            tvYellowThresholdValue.setText(Math.round(sliderYellowThreshold.getValue()) + "%");
        } catch (Exception ignored) {
        }
    }

    private int parseOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }


}
