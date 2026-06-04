package com.grf.dashboard;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.KeyEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.adapter.CycleTagAdapter;
import com.grf.api.ApiHelper;
import com.grf.helper.LoaderUtil;
import com.grf.smarttagmanager.App;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.uhfmanager.UhfManagerHelper;
import com.grf.uhfmanager.ZebraReader;
import com.grf.utils.OnKeyPressHandler;
import com.grf.utils.SnackbarUtils;
import com.grf.utils.SoundUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CycleFragment extends Fragment implements OnKeyPressHandler {
    private static final String TAG = "CycleFragment";

    private UhfManagerHelper uhfManagerHelper;
    private final Set<String> uniqueTags = new HashSet<>();
    private boolean isScanning = false;
    private Button btnStartStop;
    private Button btnSubmitTags;
    private CycleTagAdapter cycleTagAdapter;
    private TextView tvCount;
    private RecyclerView rvCycleTags;
    private LinearLayoutManager cycleTagsLayoutManager;
    private boolean isKeyPressed = false;
    private final int KEY_TRIGGER = 243;
    private final int KEY_TRIGGER_ZEBRA = 102;

    public CycleFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCount = view.findViewById(R.id.tvUniqueCount);
        btnStartStop = view.findViewById(R.id.btnCycleStartStop);
        btnSubmitTags = view.findViewById(R.id.btnSubmitTags);
        SoundUtils.init(requireContext());
        ImageView ivBack = view.findViewById(R.id.ivBack);
        rvCycleTags = view.findViewById(R.id.rvCycleTags);

        cycleTagAdapter = new CycleTagAdapter();
        cycleTagsLayoutManager = new LinearLayoutManager(requireContext());
        cycleTagsLayoutManager.setReverseLayout(false);
        cycleTagsLayoutManager.setStackFromEnd(false);
        rvCycleTags.setLayoutManager(cycleTagsLayoutManager);
        rvCycleTags.setItemAnimator(null);
        rvCycleTags.setAdapter(cycleTagAdapter);
        ivBack.setOnClickListener(v -> navigateBackAfterStopping());
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        stopScanningBeforeLeaving();
                        setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                }
        );

        if (App.ReaderType == 1 && requireActivity() instanceof MainActivity) {
            uhfManagerHelper = ((MainActivity) requireActivity()).getUhfManagerHelper();
            if (uhfManagerHelper != null) {
                uhfManagerHelper.setOnTagReadListener((epc, rssi, raw) -> handleIncomingTag(epc, rssi));
            }
        } else {
            ZebraReader.getInstance().setOnEpcReadListener((epc, rssi) -> handleIncomingTag(epc, rssi));
        }

        btnStartStop.setOnClickListener(v -> {
            if (!isScanning) {
                startScanning();
                btnStartStop.setText("Stop Scan");
                btnStartStop.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red_600))
                );
                btnSubmitTags.setVisibility(View.GONE);
            } else {
                stopScanning();
                btnStartStop.setText("Start Scan");
                btnStartStop.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_600))
                );
                btnSubmitTags.setVisibility(uniqueTags.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        btnSubmitTags.setOnClickListener(v -> showSubmitDialog());
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    public void onDestroyView() {
        stopScanning();
        SoundUtils.release();
        super.onDestroyView();
    }

    private void startScanning() {
        try {
            if (isScanning) return;
            if (App.ReaderType == 1) {
                if (uhfManagerHelper == null) {
                    Log.w(TAG, "UhfManagerHelper is null.");
                    return;
                }
                uhfManagerHelper.startInventory();
            } else {
                ZebraReader.getInstance().StartInventory();
            }
            isScanning = true;
        } catch (Exception e) {
            Log.e(TAG, "startScanning error", e);
        }
    }

    private void stopScanning() {
        try {
            if (!isScanning) return;
            if (App.ReaderType == 1) {
                if (uhfManagerHelper != null) {
                    uhfManagerHelper.stopInventory();
                }
            } else {
                ZebraReader.getInstance().StopInventory();
            }
            isScanning = false;
        } catch (Exception e) {
            Log.e(TAG, "stopScanning error", e);
        }
    }

    private void stopScanningBeforeLeaving() {
        stopScanning();
        isKeyPressed = false;
    }

    private void navigateBackAfterStopping() {
        try {
            stopScanningBeforeLeaving();
            requireActivity().onBackPressed();
        } catch (Exception e) {
            Log.e(TAG, "navigateBackAfterStopping error", e);
        }
    }

    private void showSubmitDialog() {
        try {
            if (!isAdded()) return;
            if (uniqueTags.isEmpty()) {
                SnackbarUtils.show(requireView(), "No tags found to submit");
                return;
            }

            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_submit_tags, null, false);
            EditText etRemarks = dialogView.findViewById(R.id.etRemarks);
            Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
            Button btnSend = dialogView.findViewById(R.id.btnDialogSend);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnSend.setOnClickListener(v -> {
                String remarks = etRemarks.getText() != null ? etRemarks.getText().toString().trim() : "";
                dialog.dismiss();
                submitTagsToApi(remarks);
            });

            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setGravity(Gravity.CENTER);
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
                dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Exception e) {
            Log.e(TAG, "showSubmitDialog error", e);
        }
    }

    private void submitTagsToApi(String remarks) {
        try {
            if (!isAdded()) return;

            List<String> tagsToSend = new ArrayList<>(uniqueTags);
            if (tagsToSend.isEmpty()) {
                SnackbarUtils.show(requireView(), "No tags found to submit");
                return;
            }

            JSONObject body = new JSONObject();
            body.put("remarks", remarks.isEmpty() ? "Cycle count submission" : remarks);
            body.put("tray_found", tagsToSend.size());

            JSONArray tagsArray = new JSONArray();
            for (String tag : tagsToSend) {
                JSONObject tagObj = new JSONObject();
                tagObj.put("rfid", tag);
                tagsArray.put(tagObj);
            }
            body.put("tags", tagsArray);

            LoaderUtil.show(requireContext(), "Submitting tags...");
            ApiHelper.post(requireContext(), "cycle-count/create", body.toString(), new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    try {
                        LoaderUtil.hide();
                        if (statusCode == 200 || statusCode == 201) {
                            JSONObject json = new JSONObject(response);
                            boolean success = json.optBoolean("success", false);
                            int apiStatusCode = json.optInt("statusCode", 0);
                            String message = json.optString("message", "Updated successfully");

                            if (success && (apiStatusCode == 200 || apiStatusCode == 201)) {
                                SnackbarUtils.show(requireView(), message);
                                resetCycleSession();
                            } else {
                                SnackbarUtils.show(requireView(), message);
                            }
                        } else {
                            SnackbarUtils.show(requireView(), "HTTP Error: " + statusCode);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "submit onSuccess ui error", e);
                    }
                }

                @Override
                public void onError(int statusCode, String error) {
                    try {
                        LoaderUtil.hide();
                        String message = error;
                        try {
                            JSONObject json = new JSONObject(error);
                            message = json.optString("message", error);
                        } catch (Exception ignored) {
                        }
                        SnackbarUtils.show(requireView(), statusCode == 401 ? "Unauthorized" : message);
                    } catch (Exception e) {
                        Log.e(TAG, "submit onError ui error", e);
                    }
                }
            });
        } catch (Exception e) {
            LoaderUtil.hide();
            Log.e(TAG, "submitTagsToApi error", e);
            if (isAdded()) {
                SnackbarUtils.show(requireView(), "Failed to build request");
            }
        }
    }

    private void handleIncomingTag(String epc, int rssi) {
        try {
            if (epc == null) return;
            String tag = epc.trim();
            if (tag.isEmpty()) return;

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    SoundUtils.play();
                    cycleTagAdapter.upsertTag(tag, rssi);
                    keepStrongestItemsVisible();
                });
            }

            if (uniqueTags.add(tag)) {
                Log.d(TAG, "Unique Tag: " + tag);
                if (isAdded()) {
                    requireActivity().runOnUiThread(
                            () -> tvCount.setText("Unique Tags: " + uniqueTags.size())
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Tag read error", e);
        }
    }

    private void resetCycleSession() {
        try {
            stopScanning();
            uniqueTags.clear();
            if (cycleTagAdapter != null) {
                cycleTagAdapter.clearAll();
            }
            if (tvCount != null) {
                tvCount.setText("Unique Tags: 0");
            }
            if (btnStartStop != null) {
                btnStartStop.setText("Start Scan");
                btnStartStop.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_600))
                );
            }
            if (btnSubmitTags != null) {
                btnSubmitTags.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "resetCycleSession error", e);
        }
    }

    private void keepStrongestItemsVisible() {
        if (rvCycleTags == null) return;
        rvCycleTags.post(() -> {
            try {
                if (cycleTagsLayoutManager != null) {
                    cycleTagsLayoutManager.scrollToPositionWithOffset(0, 0);
                } else {
                    rvCycleTags.scrollToPosition(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "keepStrongestItemsVisible error", e);
            }
        });
    }

    @Override
    public boolean onKeyDownEvent(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KEY_TRIGGER || keyCode == KEY_TRIGGER_ZEBRA) {
                if (isKeyPressed) return true;
                isKeyPressed = true;
                btnStartStop.callOnClick();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "onKeyDownEvent error", e);
        }
        return false;
    }

    @Override
    public boolean onKeyUpEvent(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KEY_TRIGGER || keyCode == KEY_TRIGGER_ZEBRA) {
                isKeyPressed = false;
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "onKeyUpEvent error", e);
        }
        return false;
    }
}
