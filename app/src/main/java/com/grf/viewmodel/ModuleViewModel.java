package com.grf.viewmodel;


import static android.content.ContentValues.TAG;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.navigation.Navigation;

import com.grf.adapter.TaskAdapter;
import com.grf.api.ApiHelper;
import com.grf.database.TaskDbHelper;
import com.grf.helper.LoaderUtil;
import com.grf.helper.TaskCallback;
import com.grf.model.TagToBeFind;
import com.grf.model.Task;
import com.grf.smarttagmanager.App;
import com.grf.smarttagmanager.R;
import com.grf.utils.CsvExportUtil;
import com.grf.utils.LogUtils;
import com.grf.utils.ModuleUiBinder;
import com.grf.utils.PreferenceUtils;
import com.grf.utils.SnackbarUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ModuleViewModel extends ViewModel {
    private TaskDbHelper taskDbHelper;
    // master list (source of truth)
    private final MutableLiveData<List<Task>> allTasks = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<String> text = new MutableLiveData<>("Default module text");

    public ModuleViewModel() {
        try {
            // keep empty or seed in fragment via setAllTasks(...)
            allTasks.setValue(new ArrayList<>());
        } catch (Exception e) {
            allTasks.postValue(new ArrayList<>());
        }
    }

    // call this from Fragment once with safe app context
    public void initDb(Context ctx) {
        try {

            minDbm = getMinDbm(ctx);
            maxDbm = getMaxDbm(ctx);
            greenTh = getGreenTh(ctx);
            yellowTh = getYellowTh(ctx);

            if (taskDbHelper == null && ctx != null) {
                taskDbHelper = new TaskDbHelper(ctx.getApplicationContext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TaskDbHelper getDb() {
        return taskDbHelper;
    }

    public LiveData<String> getText() {
        return text;
    }

    public void setText(String t) {
        try {
            text.setValue(t);
        } catch (Exception e) {
            text.postValue(t);
        }
    }

    // inside your ViewModel / repository class

    /**
     * Return a LiveData that contains tasks for the given moduleId.
     * If moduleId is null or empty -> return allTasks (no filtering).
     */
    public LiveData<List<Task>> getTasksForModule(final String moduleId) {
        // if moduleId is empty or null, just return the unfiltered source
        if (moduleId == null || moduleId.trim().isEmpty()) {
            // return a defensive mapped copy so callers don't accidentally mutate the internal list,
            // but returning allTasks directly is also fine if you prefer.
            return Transformations.map(allTasks, list -> {
                if (list == null) return new ArrayList<>();
                return new ArrayList<>(list);
            });
        }

        // otherwise return a MediatorLiveData that keeps itself updated from allTasks
        final MediatorLiveData<List<Task>> out = new MediatorLiveData<>();
        try {
            // initial snapshot
            try {
                List<Task> snapshot = allTasks.getValue();
                out.setValue(filterByModule(snapshot, moduleId));
            } catch (Exception ignored) {
                out.setValue(new ArrayList<>());
            }

            // subscribe for future updates
            out.addSource(allTasks, tasks -> {
                try {
                    out.setValue(filterByModule(tasks, moduleId));
                } catch (Exception e) {
                    Log.e("ModuleViewModel", "filter error", e);
                    out.setValue(new ArrayList<>());
                }
            });

            return out;
        } catch (Exception e) {
            Log.e("ModuleViewModel", "getTasksForModule error", e);
            MutableLiveData<List<Task>> empty = new MutableLiveData<>();
            empty.setValue(new ArrayList<>());
            return empty;
        }
    }

    /**
     * Helper: filter list by moduleId (null-safe).
     */
    private List<Task> filterByModule(List<Task> src, String moduleId) {
        if (src == null || moduleId == null) return new ArrayList<>();
        List<Task> out = new ArrayList<>();
        for (Task t : src) {
            if (t == null) continue;
            String m = t.getModuleId();
            if (m == null) continue;
            if (m.equals(moduleId)) out.add(t);
        }
        return out;
    }

    /**
     * Replace all tasks (use this to seed sample data). Uses setValue on main thread,
     * postValue otherwise, so it's safe to call from anywhere.
     */
    public void setAllTasks(List<Task> tasks) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                allTasks.setValue(tasks != null ? tasks : new ArrayList<>());
            } else {
                allTasks.postValue(tasks != null ? tasks : new ArrayList<>());
            }
        } catch (Exception e) {
            allTasks.postValue(tasks != null ? tasks : new ArrayList<>());
        }
    }

    /**
     * Add a list of tasks safely (never crashes, never null)
     */
    public void addTask(Task task) {
        try {
            if (task == null) return;

            List<Task> cur = allTasks.getValue();
            if (cur == null) cur = new ArrayList<>();

            cur.add(task);
            setAllTasks(cur);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove all tasks completely
     */
    public void clearAllTasks() {
        try {
            allTasks.setValue(new ArrayList<>());
        } catch (Exception e) {
            // fallback
            allTasks.postValue(new ArrayList<>());
        }
    }

    public void BindModuleData(View view, ModuleViewModel vm, Context ctx, LifecycleOwner owner, String moduleId, int zoneId) {
        try {
            // List<Task> tasks = taskDbHelper.getTasksList(new TaskDbHelper.TaskFilter());

            LoaderUtil.show(App.getContext(), "Fetching task..");
            fetchTasks(App.getContext(), view, new TaskCallback() {

                @Override
                public void onSuccess(List<Task> tasks) {
                    try {
                        LoaderUtil.hide();
                        tasks.sort((t1, t2) -> Long.compare(t1.getId(), t2.getId()));


                        if (tasks.isEmpty()) {
                            SnackbarUtils.show(view, "No Task Found");
                        }
                        tasks.forEach(task -> {
                            try {
                                task.setZoneId(String.valueOf(zoneId));
                            } catch (Exception e) {
                                Log.e(TAG, "setZoneId error", e);
                            }
                        });
                        vm.setAllTasks(tasks);
                    } catch (Exception e) {
                        LoaderUtil.hide();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(String error) {
                    LoaderUtil.hide();
                    SnackbarUtils.show(view, error);
                }
            });


            // listener for adapter actions
            TaskAdapter.Listener listener = new TaskAdapter.Listener() {
                @Override
                public void onEdit(Task task, int position, int moduleType) { /* ... */ }

                @Override
                public void onDelete(Task task, int position, int moduleType) { /* ... */ }

                @Override
                public void onItemClick(Task task, int position, int moduleType) {
                    try {
                        Log.d("CLICK_DEBUG", "CLICK RECEIVED");

                        Bundle bundle = new Bundle();
                        bundle.putString("taskData", taskToJson(task));
                        bundle.putString("moduleName", moduleId);
                        bundle.putInt("moduleType", moduleType);
                        bundle.putLong("taskId", task.getId());
                        bundle.putString("zoneId", task.getZoneId());
                        bundle.putString("taskName", task.getTitle());
                        bundle.putBoolean("isFromDashboard", true);

                        Navigation.findNavController(view)
                                .navigate(R.id.action_common_to_scanFragment, bundle);

                    } catch (Exception e) {
                        Log.e(TAG, "Navigation error", e);
                    }
                }
            };
            // then bind RecyclerView (observer will update adapter)
            ModuleUiBinder.bindTasksRecycler(ctx, owner,
                    view.findViewById(R.id.rvTasks), vm, moduleId, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public void fetchTasks(Context context, View view, TaskCallback callback) {
        try {

            ApiHelper.get(context, "get-pending-task", new ApiHelper.ApiCallback() {

                @Override
                public void onSuccess(int statusCode, String response) {
                    try {

                        if (statusCode == 200) {

                            JSONObject json = new JSONObject(response);
                            boolean success = json.optBoolean("success", false);
                            int apiStatusCode = json.optInt("statusCode", 0);

                            if (success && apiStatusCode == 200) {

                                JSONArray dataArray = json.optJSONArray("data");

                                if (dataArray != null && dataArray.length() > 0) {

                                    List<Task> tasks = new ArrayList<>();

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        try {

                                            JSONObject obj = dataArray.getJSONObject(i);

                                            long id = obj.optLong("id", 0);
                                            String title = obj.optString("name", "");
                                            String dateTime = obj.optString("created_at", "");

                                            JSONArray trays = obj.optJSONArray("trays");
                                            List<TagToBeFind> trayList = new ArrayList<>();

                                            if (trays != null) {
                                                for (int j = 0; j < trays.length(); j++) {
                                                    try {

                                                        JSONObject trayObj = trays.getJSONObject(j);

                                                        String trayId = trayObj.optString("tray_id", "");
                                                        String rfId = trayObj.optString("rf_id", "");
                                                        String status = trayObj.optString("status_name", "");
                                                        String findingTime = trayObj.optString("finding_time", "");
                                                        String zone = trayObj.optString("zone_name", "");

                                                        trayList.add(new TagToBeFind(trayId, rfId, status, findingTime, zone));

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }

                                            int tagCount = trayList.size();

                                            Task task = new Task(id, title, tagCount, "", 0, 0.0, "", "", 0, 0, dateTime, trayList);

                                            tasks.add(task);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    callback.onSuccess(tasks); // ✅ return data

                                } else {
                                    callback.onError("No data found");
                                }

                            } else {
                                callback.onError(json.optString("message", "Failed"));
                            }

                        } else {
                            callback.onError("HTTP Error: " + statusCode);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Parsing error");
                    }
                }

                @Override
                public void onError(int statusCode, String error) {
                    callback.onError(statusCode == 401 ? "Unauthorized" : error);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Unexpected error");
        }
    }

    public int getModuleNumber(String module) {
        try {
            if (module == null) return 0;

            switch (module.toUpperCase()) {
                case "A":
                    return 1;
                case "B":
                    return 2;
                case "C":
                    return 3;
                case "D":
                    return 4;
                case "E":
                    return 5;
                case "F":
                    return 6;
                default:
                    return 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getModuleLetter(String number) {
        try {
            switch (number) {
                case "1":
                    return "A";
                case "2":
                    return "B";
                case "3":
                    return "C";
                case "4":
                    return "D";
                case "5":
                    return "E";
                case "6":
                    return "F";
                default:
                    return "ALL";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public String taskToJson(Task task) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.toJson(task);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public Task jsonToTask(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json, Task.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected void onCleared() {
        try {

            if (taskDbHelper != null) {
                taskDbHelper.CloseDb();
            }
        } catch (Exception ignored) {
        }
    }

    public static String getDateTime() {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date());
        } catch (Exception e) {
            return "";
        }
    }


    public static boolean ExportCsv(Context context, List<String> header, List<List<String>> data) {
        try {
            String name = "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String time = sdf.format(new Date());
                name = "my_report_" + time + ".csv";
            } catch (Exception e) {
                name = "my_report.csv"; // fallback
            }

            Uri savedFile = CsvExportUtil.saveCsvToDownloads(
                    context,
                    name,
                    header,
                    data
            );

            if (savedFile != null) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {

        }
        return false;
    }

    public static int minDbm = -80;
    public static int maxDbm = -40;
    public static int greenTh = 65;
    public static int yellowTh = 40;

    public static int rssiToStrength0to100(int rssiDbm) {
        try {

            LogUtils.d("min : " + minDbm + " Max : " + maxDbm + " greenTh : " + greenTh + " yellowTh : " + yellowTh);
            int rssi = Math.max(minDbm, Math.min(maxDbm, rssiDbm));

            double percent = ((double) (rssi - minDbm) / (maxDbm - minDbm)) * 100.0;

            int strength = (int) Math.round(percent);

            if (strength < 1) strength = 1;
            if (strength > 100) strength = 100;

            return strength;

        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }


    // ========= PUBLIC GETTERS (use anywhere in the app) =========

    public static int getMinDbm(Context ctx) {
        try {
            return Integer.parseInt(PreferenceUtils.getString(ctx, "MIN_DBM", "-80"));
        } catch (Exception e) {
            return -80;
        }
    }

    public static int getMaxDbm(Context ctx) {
        try {
            return Integer.parseInt(PreferenceUtils.getString(ctx, "MAX_DBM", "-40"));
        } catch (Exception e) {
            return -40;
        }
    }

    public static int getGreenTh(Context ctx) {
        try {
            return Integer.parseInt(PreferenceUtils.getString(ctx, "GREEN_TH", "65"));
        } catch (Exception e) {
            return 65;
        }
    }

    public static int getYellowTh(Context ctx) {
        try {
            return Integer.parseInt(PreferenceUtils.getString(ctx, "YELLOW_TH", "40"));
        } catch (Exception e) {
            return 40;
        }
    }

}



