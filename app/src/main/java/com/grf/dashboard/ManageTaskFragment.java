package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.grf.adapter.TaskAdapter;
import com.grf.api.ApiHelper;
import com.grf.model.TagToBeFind;
import com.grf.model.Task;
import com.grf.smarttagmanager.R;
import com.grf.utils.SnackbarUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageTaskFragment extends Fragment implements TaskAdapter.Listener {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private final List<Task> tasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_manage_task, container, false);

        rvTasks = root.findViewById(R.id.rvTasks);

        View btnNewTask = root.findViewById(R.id.btnNewTask);
        btnNewTask.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v)
                        .navigate(R.id.action_dashboard_to_newTaskFragment);
            } catch (Throwable ignored) {
            }
        });

        ImageView ivBack = root.findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            try {
                if (getActivity() != null) getActivity().onBackPressed();
            } catch (Throwable ignored) {
            }
        });

        setupRecycler();
        loadTasksFromApi();

        return root;
    }

    private void loadTasksFromApi() {
        try {
            ApiHelper.get(requireContext(), "get-pending-task", new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    try {
                        if (statusCode != 200) {
                            SnackbarUtils.show(requireView(), "HTTP Error: " + statusCode);
                            return;
                        }

                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        int apiStatusCode = json.optInt("statusCode", 0);
                        if (!success || apiStatusCode != 200) {
                            SnackbarUtils.show(requireView(), json.optString("message", "Failed to load tasks"));
                            return;
                        }

                        JSONArray dataArray = json.optJSONArray("data");
                        List<Task> fresh = new ArrayList<>();

                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.optJSONObject(i);
                                if (obj == null) continue;

                                long id = obj.optLong("id", 0);
                                String title = obj.optString("name", "");
                                String dateTime = obj.optString("created_at", "");

                                JSONArray trays = obj.optJSONArray("trays");
                                List<TagToBeFind> trayList = new ArrayList<>();
                                if (trays != null) {
                                    for (int j = 0; j < trays.length(); j++) {
                                        JSONObject trayObj = trays.optJSONObject(j);
                                        if (trayObj == null) continue;

                                        String trayId = trayObj.optString("tray_id", "");
                                        String rfId = trayObj.optString("rf_id", "");
                                        String status = trayObj.optString("status_name", "");
                                        String findingTime = trayObj.optString("finding_time", "");
                                        String zone = trayObj.optString("zone_name", "");

                                        trayList.add(new TagToBeFind(trayId, rfId, status, findingTime, zone));
                                    }
                                }

                                int tagCount = trayList.size();
                                fresh.add(new Task(id, title, tagCount, "", 0, 0.0, "", "", 0, 0, dateTime, trayList));
                            }
                        }

                        Collections.sort(fresh, (t1, t2) -> {
                            String d1 = t1 != null ? t1.getDateTime() : "";
                            String d2 = t2 != null ? t2.getDateTime() : "";
                            int byDate = d2.compareToIgnoreCase(d1); // latest created_at first
                            if (byDate != 0) return byDate;
                            long id1 = t1 != null ? t1.getId() : 0L;
                            long id2 = t2 != null ? t2.getId() : 0L;
                            return Long.compare(id2, id1); // fallback: highest id first
                        });

                        tasks.clear();
                        tasks.addAll(fresh);
                        adapter.replaceAll(tasks);

                    } catch (Exception e) {
                        SnackbarUtils.show(requireView(), "Failed to parse tasks");
                    }
                }

                @Override
                public void onError(int statusCode, String error) {
                    try {
                        SnackbarUtils.show(requireView(), statusCode == 401 ? "Unauthorized" : error);
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception e) {
            SnackbarUtils.show(requireView(), "Failed to load task list");
        }
    }

    private void setupRecycler() {
        adapter = new TaskAdapter(tasks, this, 1);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
    }

    @Override
    public void onEdit(Task task, int position, int moduleType) {
        SnackbarUtils.show(requireView(), "Edit is not enabled here");
    }

    @Override
    public void onDelete(Task task, int position, int moduleType) {
        SnackbarUtils.show(requireView(), "Delete is not enabled here");
    }

    @Override
    public void onItemClick(Task task, int position, int moduleType) {
        try {
            Bundle b = new Bundle();
            b.putString("taskData", new Gson().toJson(task));
            Navigation.findNavController(requireView()).navigate(R.id.action_manageTask_to_viewTaskDetailsFragment, b);
        } catch (Exception e) {
            SnackbarUtils.show(requireView(), "Unable to open task details");
        }
    }
}
