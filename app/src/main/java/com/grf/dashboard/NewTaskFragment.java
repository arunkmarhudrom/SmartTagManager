package com.grf.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.grf.api.ApiHelper;
import com.grf.smarttagmanager.R;
import com.grf.utils.SnackbarUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewTaskFragment extends Fragment {

    private EditText etTaskName;
    private TextInputEditText etTrayId;
    private MaterialButton btnAddTray;
    private MaterialButton btnViewAddedTags;
    private MaterialButton btnSaveTask;

    private final List<String> trayIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTaskName = view.findViewById(R.id.etTaskName);
        etTrayId = view.findViewById(R.id.etTrayId);
        btnAddTray = view.findViewById(R.id.btnAddTray);
        btnViewAddedTags = view.findViewById(R.id.btnViewAddedTags);
        btnSaveTask = view.findViewById(R.id.btnSaveTask);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).popBackStack();
            } catch (Exception ignored) {
            }
        });

        getParentFragmentManager().setFragmentResultListener("added_tags_result", getViewLifecycleOwner(), (key, bundle) -> {
            ArrayList<String> updated = bundle.getStringArrayList("tray_ids");
            trayIds.clear();
            if (updated != null) {
                trayIds.addAll(updated);
            }
            syncUi();
        });

        setDefaultTaskName();
        syncUi();

        btnAddTray.setOnClickListener(v -> addTrayIdFromInput());
        btnViewAddedTags.setOnClickListener(v -> openAddedTagsScreen(v));
        btnSaveTask.setOnClickListener(v -> submitTask());

        etTrayId.setOnEditorActionListener((v, actionId, event) -> {
            addTrayIdFromInput();
            return true;
        });
    }

    private void setDefaultTaskName() {
        try {
            String defaultName = "Task_" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(new Date());
            etTaskName.setText(defaultName);
        } catch (Exception ignored) {
        }
    }

    private void addTrayIdFromInput() {
        try {
            String raw = etTrayId.getText() == null ? "" : etTrayId.getText().toString();
            String trayId = normalizeTrayId(raw);

            if (trayId.isEmpty()) {
                SnackbarUtils.show(requireView(), "Please enter Tray ID");
                return;
            }

            if (trayIds.contains(trayId)) {
                SnackbarUtils.show(requireView(), "Duplicate Tray ID is not allowed");
                return;
            }

            trayIds.add(trayId);
            etTrayId.setText("");
            syncUi();

        } catch (Exception e) {
            SnackbarUtils.show(requireView(), "Unable to add Tray ID");
        }
    }

    private void openAddedTagsScreen(View view) {
        try {
            Bundle b = new Bundle();
            b.putStringArrayList("tray_ids", new ArrayList<>(trayIds));
            Navigation.findNavController(view).navigate(R.id.action_newTask_to_addedTagsFragment, b);
        } catch (Exception ignored) {
        }
    }

    private void syncUi() {
        try {
            int count = trayIds.size();
            btnViewAddedTags.setText("View Tray IDs (" + count + ")");
            btnViewAddedTags.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        } catch (Exception ignored) {
        }
    }

    private String normalizeTrayId(String input) {
        if (input == null) return "";
        return input.trim().toUpperCase(Locale.ROOT);
    }

    private void submitTask() {
        try {
            String taskName = etTaskName.getText() == null ? "" : etTaskName.getText().toString().trim();

            if (TextUtils.isEmpty(taskName)) {
                SnackbarUtils.show(requireView(), "Task name is required");
                return;
            }

            if (trayIds.isEmpty()) {
                SnackbarUtils.show(requireView(), "Please add at least one Tray ID");
                return;
            }

            JSONObject body = new JSONObject();
            JSONObject task = new JSONObject();
            task.put("name", taskName);
            task.put("active", true);
            body.put("task", task);

            JSONArray taskStatuses = new JSONArray();
            for (String trayId : trayIds) {
                JSONObject row = new JSONObject();
                row.put("tray_id", trayId);
                taskStatuses.put(row);
            }
            body.put("task_statuses", taskStatuses);

            btnSaveTask.setEnabled(false);

            ApiHelper.post(requireContext(), "task/create", body.toString(), new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    try {
                        btnSaveTask.setEnabled(true);
                        if (statusCode == 200 || statusCode == 201) {
                            SnackbarUtils.show(requireView(), "Task created successfully");
                            resetFormAfterSubmit();
                        } else {
                            SnackbarUtils.show(requireView(), "Failed: HTTP " + statusCode);
                        }
                    } catch (Exception e) {
                        btnSaveTask.setEnabled(true);
                        SnackbarUtils.show(requireView(), "Task created, but UI update failed");
                    }
                }

                @Override
                public void onError(int statusCode, String error) {
                    try {
                        btnSaveTask.setEnabled(true);
                        String message = error;
                        try {
                            JSONObject obj = new JSONObject(error);
                            message = obj.optString("message", error);
                        } catch (Exception ignored) {
                        }
                        SnackbarUtils.show(requireView(), statusCode == 401 ? "Unauthorized" : message);
                    } catch (Exception ignored) {
                    }
                }
            });

        } catch (Exception e) {
            btnSaveTask.setEnabled(true);
            SnackbarUtils.show(requireView(), "Unable to submit task");
        }
    }

    private void resetFormAfterSubmit() {
        trayIds.clear();
        syncUi();
        etTrayId.setText("");
        setDefaultTaskName();
    }
}
