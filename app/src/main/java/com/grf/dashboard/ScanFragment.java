package com.grf.dashboard;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.grf.adapter.PendingTagsAdapter;
import com.grf.api.ApiHelper;
import com.grf.database.TagDbHelper;
import com.grf.database.TaskDbHelper;
import com.grf.helper.AppDatabaseHelper;
import com.grf.helper.LoaderUtil;
import com.grf.helper.UpdateTaskCallback;
import com.grf.model.PendingTag;
import com.grf.model.Tag;
import com.grf.model.TagToBeFind;
import com.grf.model.Task;
import com.grf.smarttagmanager.App;
import com.grf.smarttagmanager.LoginActivity;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.uhfmanager.UhfManagerHelper;
import com.grf.uhfmanager.ZebraReader;
import com.grf.utils.JsonToCsvExporter;
import com.grf.utils.LogUtils;
import com.grf.utils.OnKeyPressHandler;
import com.grf.utils.PopupUtils;
import com.grf.utils.ProgressUtil;
import com.grf.utils.SnackbarUtils;
import com.grf.utils.SoundUtils;
import com.grf.viewmodel.ModuleViewModel;
import com.nlscan.android.uhf.UHFReader;
import com.rscja.team.qcom.deviceapi.S;

import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment implements OnKeyPressHandler {

    private static final org.apache.commons.logging.Log log = LogFactory.getLog(ScanFragment.class);

    public ScanFragment() {
        // Required empty public constructor
    }

    //private TaskDbHelper taskDbHelper;
    private ExecutorService exec;
    private String moduleName = "";
    private String taskData = "";
    private String taskName = "";
    private String zoneId = "";
    private int moduleType = -1;
    private long taskId = 0;
    private Task task;
    private boolean isFromDashboard = false;

    TextView tvHeaderCount, tvSectionTitle;

    //List<Tag> AllAvailableTag = new ArrayList<>();
    //TagDbHelper tagDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (getArguments() != null) {
                moduleName = getArguments().getString("moduleName", "");
                moduleType = getArguments().getInt("moduleType", -1);
                taskId = getArguments().getLong("taskId", 0);
                taskData = getArguments().getString("taskData", "");
                zoneId = getArguments().getString("zoneId", "");
                taskName = getArguments().getString("taskName", "");

                isFromDashboard = getArguments().getBoolean("isFromDashboard", false);
                //SoundUtils.play(requireContext(), R.raw.beep);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // handler for optional simulation loop

    private boolean isScanning = false;
    UhfManagerHelper uhfManagerHelper;
    Button btnStartStop;
    int ConfirmTagCount = 0;

    // ---------- onCreateView (simplified) ----------
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        try {
            ModuleViewModel viewModel = new ViewModelProvider(this).get(ModuleViewModel.class);
            tvHeaderCount = view.findViewById(R.id.tvHeaderCount);
            tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
            SoundUtils.init(requireContext());
            viewModel.initDb(requireContext()); // initialize DB with application context


            // taskDbHelper = viewModel.getDb();

            // tagDb = new TagDbHelper(requireContext());

            //AllAvailableTag = tagDb.getAllTags();

            exec = Executors.newSingleThreadExecutor();
            if (taskData != null && !taskData.isEmpty()) {

                task = viewModel.jsonToTask(taskData);
                tvHeaderCount.setText(ConfirmTagCount + "/" + task.getTagCount());
                exec.execute(() -> {
                    //String tags = taskDbHelper.getTagsByBoxId(task.getBoxId());
                    //  LogUtils.e("Tags " + tags);
                    // List<String> filterTagId=  task.tagToBeFindList();
                    // filterTagId = getFilterTagList(tags);

                    try {
                        if (task.tagToBeFindList != null) {
                            for (TagToBeFind item : task.tagToBeFindList) {
                                try {
                                    if (item.rfId != null && !item.rfId.isEmpty()) {
                                        filterTagId.add(item.rfId);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }


            TextView tvModuleTitle = view.findViewById(R.id.tvModuleTitle);
            tvModuleTitle.setText("Verify Tag");

            ImageView ivSettings = view.findViewById(R.id.ivSettings);
            ivSettings.setOnClickListener(v -> {
                Navigation.findNavController(v)
                        .navigate(R.id.action_scan_to_settingFragment);
            });
            ImageView back = view.findViewById(R.id.ivBack);

            back.setOnClickListener(v -> {
                try {
                    if (App.ReaderType == 1) {
                        uhfManagerHelper.stopInventory();
                    } else
                        ZebraReader.getInstance().StopInventory();

                    requireActivity().onBackPressed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // initialize list + adapter
            initAdapter(view);
            MainActivity act = (MainActivity) requireActivity();
            if (App.ReaderType == 1) {
                uhfManagerHelper = act.getUhfManagerHelper();

                if (uhfManagerHelper != null) {
                    // call helper methods safely

                    uhfManagerHelper.setOnTagReadListener((epc, rssi, raw) -> {
                        // handle tag reads (update UI on main thread)
                        // Log.i("MAIN", "EPC: " + epc + " RSSI:" + rssi);
                        boolean found = isTagInList(epc.trim());
                        // update UI list etc.
                        if (found) {
//                        act.rssiToStrength0to100(rssi)
                            handleIncomingTag(epc, rssi);
                        }
                    });
                }
            } else {
                ZebraReader.getInstance().setOnEpcReadListener((epc, rssi) -> {
                    try {
                        // Log.d("RFID", "EPC = " + epc + " RSSI = " + rssi);

                        boolean found = isTagInList(epc.trim());

                        if (found) {

                            handleIncomingTag(epc, rssi);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });


            }
            TextView tvHeaderTaskTitle = view.findViewById(R.id.tvHeaderTaskTitle);
            tvHeaderTaskTitle.setText(taskName);
            TextView tvHeaderSub = view.findViewById(R.id.tvHeaderSub);
            tvHeaderSub.setText("Active in Zone " + moduleName);

            // Start / Stop button
            btnStartStop = view.findViewById(R.id.btnStartStop);
            btnStartStop.setText("▶  Start Scanning Zone");
            if (task.getTaskComplete() == 1) {
                btnStartStop.setEnabled(false);
                SnackbarUtils.show(view, "Task already completed");
            }


            btnStartStop.setOnClickListener(v -> {
                try {
                    if (!isScanning) {
                        // start scanning (startScanning will set isScanning = true)
                        startScanning();

                        // update UI
                        btnStartStop.setText("■  Stop Scanning");
                        btnStartStop.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.red_600)
                                )
                        );
                    } else {
                        // stop scanning (stopScanning will set isScanning = false)
                        stopScanning();

                        // update UI
                        btnStartStop.setText("▶  Start Scanning Zone ");
                        btnStartStop.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.blue_600)
                                )
                        );
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return view;
    }


    // ---------- Fragment fields (place at top of your fragment class) ----------
    private List<PendingTag> pendingList = new ArrayList<>();
    private PendingTagsAdapter adapter;
    private Map<String, Integer> tagIndexMap = new HashMap<>(); // tagId -> index in pendingList
    // add this field to your Fragment (or enclosing class)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, PendingTag> pendingUpdates = new HashMap<>();

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 10; // ms throttle

    private void handleIncomingTag(String epc, int rssi) {
        try {
            int pc = ModuleViewModel.rssiToStrength0to100(rssi);
            addOrUpdateTag(epc, pc, rssi); // ✅ always process
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<String> filterTagId = new ArrayList<>();

    public boolean isTagInList(String tag) {
        try {
            if (tag == null || tag.trim().isEmpty()) {
                Log.w("RFID", "Search tag is NULL or EMPTY");
                return false;
            }

            String searchTag = tag.trim();

            Log.d("RFID", "Searching EPC = " + searchTag);
            Log.d("RFID", "Tag list size = " + filterTagId.size());
            Log.d("RFID", "Tag list contents = " + filterTagId.toString());

            boolean found = filterTagId.contains(searchTag);

            Log.d("RFID", "Search result for EPC [" + searchTag + "] = " + found);

            return found;

        } catch (Exception e) {
            Log.e("RFID", "Error while searching EPC = " + tag, e);
            return false;
        }
    }

    // ---------- InitBindTagList: prepares recycler + adapter ----------
    private void initAdapter(View view) {
        try {
            RecyclerView rv = view.findViewById(R.id.rvPendingTags);
            rv.setItemAnimator(null);
            // keep pendingList as initial empty list or pre-fill
            pendingList.clear();
            tagIndexMap.clear();

            // assign to FIELD (do not create a local variable here)
            adapter = new PendingTagsAdapter(requireContext(), pendingList, new PendingTagsAdapter.Listener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onItemClicked(PendingTag tag) {

                    UpdateTask(tag);
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onItemChecked(PendingTag tag) {
                    UpdateTask(tag);

                }
            });

            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setAdapter(adapter);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    // ---------- startScanning (simulation uses addOrUpdateTag) ----------
    private void startScanning() {
        try {
            // guard: if already scanning, do nothing
            if (isScanning) return;
            // mark scanning started
            isScanning = true;
            if (App.ReaderType == 1)
                uhfManagerHelper.startInventory();
            else
                ZebraReader.getInstance().StartInventory();


        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void stopScanning() {
        try {
            // mark stopped first so runnable won't reschedule itself
            isScanning = false;
            if (App.ReaderType == 1)
                uhfManagerHelper.stopInventory();
            else
                ZebraReader.getInstance().StopInventory();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


// Activity: ensure these are initialized once (e.g. onCreateView)

// adapter created with same list reference: adapter = new PendingTagsAdapter(requireContext(), pendingList, listener);

    private void rebuildIndexMap() {
        try {
            tagIndexMap.clear();
            for (int i = 0; i < pendingList.size(); i++) {
                PendingTag t = pendingList.get(i);
                if (t != null && t.getTagId() != null) {
                    tagIndexMap.put(t.getTagId(), i);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    public String getTrayIdByRfId(String rfId) {
        try {
            if (task != null && task.tagToBeFindList != null && rfId != null) {

                for (TagToBeFind t : task.tagToBeFindList) {
                    try {
                        if (t.rfId != null && t.rfId.equals(rfId)) {
                            return t.trayId; // ✅ match found
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // ❌ not found
    }

    public void addOrUpdateTag(final String tagId, final int strengthPercent, final int rssi) {
        try {
            if (tagId == null) return;
            if (getActivity() == null) return;

            String trayId = getTrayIdByRfId(tagId);

            PendingTag updated = new PendingTag(tagId, 0, strengthPercent, trayId, "", rssi);

            synchronized (pendingUpdates) {
                pendingUpdates.put(tagId, updated); // 🔥 overwrite = no spam
            }

            scheduleUiFlush();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pendingUiOps = new ArrayList<>();
    private boolean isUiScheduled = false;
    private static final long UI_BATCH_DELAY = 50; // 🔥 key improvement


    private void scheduleUiFlush() {
        try {
            if (isUiScheduled) return;
            isUiScheduled = true;

            uiHandler.postDelayed(() -> {
                try {

                    Map<String, PendingTag> copy;
                    synchronized (pendingUpdates) {
                        copy = new HashMap<>(pendingUpdates);
                        pendingUpdates.clear();
                    }

                    for (PendingTag incoming : copy.values()) {
                        SoundUtils.play();
                        String tagId = incoming.getTagId();
                        Integer idxObj = tagIndexMap.get(tagId);

                        if (idxObj == null) {
                            pendingList.add(incoming);
                            int pos = pendingList.size() - 1;
                            tagIndexMap.put(tagId, pos);
                            adapter.notifyItemInserted(pos);

                        } else {
                            int index = idxObj;

                            if (index >= 0 && index < pendingList.size()) {
                                PendingTag existing = pendingList.get(index);

                                if (existing.getSignalPercent() != incoming.getSignalPercent()) {

                                    existing.setSignalPercent(incoming.getSignalPercent());

                                    int newIndex = index;

                                    while (newIndex > 0 &&
                                            pendingList.get(newIndex - 1).getSignalPercent() < existing.getSignalPercent()) {
                                        Collections.swap(pendingList, newIndex, newIndex - 1);
                                        newIndex--;
                                    }

                                    while (newIndex < pendingList.size() - 1 &&
                                            pendingList.get(newIndex + 1).getSignalPercent() > existing.getSignalPercent()) {
                                        Collections.swap(pendingList, newIndex, newIndex + 1);
                                        newIndex++;
                                    }

                                    if (index != newIndex) {
                                        adapter.notifyItemMoved(index, newIndex);
                                    }

                                    adapter.notifyItemChanged(newIndex, "signal");

                                    // 🔥 update index map only for affected range
                                    int start = Math.min(index, newIndex);
                                    int end = Math.max(index, newIndex);
                                    for (int i = start; i <= end; i++) {
                                        tagIndexMap.put(pendingList.get(i).getTagId(), i);
                                    }
                                }
                            }
                        }
                    }

                    tvSectionTitle.setText("Nearby Tag (" + pendingList.size() + ")");

                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    isUiScheduled = false;
                }
            }, 30); // 🔥 smooth UI frame

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    public void addOrUpdateTago(final String tagId, final int strengthPercent, final int rssi) {
        try {
            if (tagId == null) return;
            if (getActivity() == null || getView() == null) return;

            // ensure everything runs on main thread
            mainHandler.post(() -> {
                try {
                    if (pendingList == null) pendingList = new ArrayList<>();
                    if (tagIndexMap == null) {

                    }

                    String trayId = getTrayIdByRfId(tagId);

                    Integer idxObj = tagIndexMap.get(tagId);
                    if (idxObj == null) {
                        // NEW tag -> append then sort & rebuild map
                        PendingTag newTag = new PendingTag(tagId, 0, strengthPercent, trayId, "", rssi);
                        pendingList.add(newTag);

                        // keep sorted by percent desc (so newly added gets right place)
                        pendingList.sort((a, b) -> Integer.compare(b.getSignalPercent(), a.getSignalPercent()));

                        // rebuild index map after reordering
                        rebuildIndexMap();

                        //  SoundUtils.play(requireContext(), R.raw.beep);

                        tvSectionTitle.setText("Nearby Tag (" + pendingList.size() + ")");
                        //  Log.i("MAIN", "new EPC: " + tagId + " RSSI:" + strengthPercent);

                        // notify adapter: list reordered -> simplest: full refresh
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                    } else {
                        // EXISTING -> update if changed
                        int idx = idxObj;
                        if (idx >= 0 && idx < pendingList.size()) {
                            PendingTag existing = pendingList.get(idx);
                            if (existing.getSignalPercent() != strengthPercent) {
                                existing.setSignalPercent(strengthPercent);
                                // SoundUtils.play(requireContext(), R.raw.beep);
                                // Log.i("MAIN", "UPDATE EPC: " + tagId + " RSSI:" + strengthPercent);

                                // after changing percent we must re-sort and rebuild map
                                pendingList.sort((a, b) -> Integer.compare(b.getSignalPercent(), a.getSignalPercent()));
                                rebuildIndexMap();

                                // notify adapter: full refresh (safe under frequent updates)
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        } else {
                            // index mismatch — rebuild map and try again next time
                            rebuildIndexMap();
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Optional: remove pending callbacks when view is destroyed to avoid leaks / stale runs
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            mainHandler.removeCallbacksAndMessages(null);
            SoundUtils.release();
        } catch (Throwable ignored) {
        }
    }


    private boolean isKey243Pressed = false;   // guards repeated keyDown while holding
    private boolean isInventoryActive = false; // your local inventory state (toggle)
    private final int KeyUpDownCode = 243;
    private final int KeyUpDownCodeZebra = 102;

    @Override
    public boolean onKeyDownEvent(int keyCode, KeyEvent event) {
        try {
            LogUtils.e("keyCode" + keyCode);
            if (task.getTaskComplete() == 1) {

                SnackbarUtils.show(requireView(), "Task already completed");

            } else if (keyCode == KeyUpDownCode || keyCode == KeyUpDownCodeZebra) {

                // Ignore repeated keyDown while holding the key
                if (isKey243Pressed) {
                    return true; // already handled while key is held
                }
                isKey243Pressed = true; // mark as currently pressed

                // Toggle inventory on each distinct press
                try {
                    if (!isInventoryActive) {
                        // Start inventory
                        LogUtils.i("FragA keyDown START inventory: " + keyCode);

                        btnStartStop.callOnClick();


                        isInventoryActive = true;
                    } else {
                        // Stop inventory
                        LogUtils.i("FragA keyDown STOP inventory: " + keyCode);

                        // uhfManagerHelper.stopInventory();

                        btnStartStop.callOnClick();
                        isInventoryActive = false;
                    }
                } catch (Exception ex) {
                    LogUtils.e("FragA", "toggle inventory error: " + ex.getMessage());
                    ex.printStackTrace();
                }

                return true; // consumed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // not consumed
    }

    @Override
    public boolean onKeyUpEvent(int keyCode, KeyEvent event) {
        try {

            LogUtils.e("onKeyUpEvent " + keyCode);
            if (keyCode == KeyUpDownCode || keyCode == KeyUpDownCodeZebra) {
                // release the guard so next physical press will be handled
                isKey243Pressed = false;
                LogUtils.i("FragA keyUp: " + keyCode);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // shutdown executor (same behavior you had in Activity.onDestroy)
        try {
            if (exec != null) {
                exec.shutdownNow();
                exec = null;
            }
        } catch (Exception e) {
            Log.e("TAG", "executor shutdown error", e);
        }

        // optional: release DB helper resources if you added a release method
        // (your TaskDbHelper doesn't need explicit close since it opens/closes each operation)
        //  taskDbHelper = null;
    }


    @SuppressLint("SetTextI18n")
    void UpdateTask(PendingTag tag) {
        try {
            ConfirmTagCount++;
            String boxID = String.valueOf(task.getBoxId());
            int tagFound = (task.getTagCount() - ConfirmTagCount);
            tvHeaderCount.setText(ConfirmTagCount + "/" + task.getTagCount());

            TaskDbHelper.TaskUpdate u = new TaskDbHelper.TaskUpdate();
            u.tagFound = 1;
            u.tagCount = tagFound;
            u.zoneId = moduleName;
            u.dateTime = ModuleViewModel.getDateTime();

            // taskDbHelper.updateTask(u, "boxId = ? AND tagId = ?", new String[]{boxID, tag.getTagId()});

            u.dateTime = null;
            u.zoneId = null;
            u.tagFound = null;
            //  taskDbHelper.updateTask(u, "boxId = ?", new String[]{boxID});// for updating total tag count to other item
            //  if (tagFound == 0) { // set task complete
            //   u.taskComplete = 1;
            //  taskDbHelper.updateTask(u, "boxId = ?", new String[]{boxID});
            //  }
            String trayId = getTrayIdByRfId(tag.getTagId());

            LoaderUtil.show(requireContext(), "Updating....");
            updateTaskApi(
                    requireContext(),
                    requireView(),
                    taskId,
                    trayId,
                    Integer.parseInt(zoneId),
                    2,
                    new UpdateTaskCallback() {
                        @Override
                        public void onSuccess(String message) {
                            LoaderUtil.hide();
                            try {
                                Log.d("API", message);
                                SnackbarUtils.show(requireView(), message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            try {
                                LoaderUtil.hide();
                                Log.e("API", error);
                                SnackbarUtils.show(requireView(), error);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );

        } catch (Exception e) {
            LoaderUtil.hide();

            SnackbarUtils.show(requireView(), e.getMessage());
            ConfirmTagCount = ConfirmTagCount - 1;
        }
    }


    public void updateTaskApi(Context context, View view, long taskId, String trayId, int zoneId, int statusId, UpdateTaskCallback callback) {
        try {

            JSONObject body = new JSONObject();
            body.put("task_id", taskId);
            body.put("tray_id", trayId);
            body.put("zone_id", zoneId);
            body.put("status_id", statusId);
            body.put("device_id", Integer.parseInt(App.getDeviceMac()));
            LogUtils.e(body.toString());
            ApiHelper.put(context, "task/update", body.toString(), new ApiHelper.ApiCallback() {

                @Override
                public void onSuccess(int statusCode, String response) {
                    try {

                        if (statusCode == 200) {

                            JSONObject json = new JSONObject(response);

                            boolean success = json.optBoolean("success", false);
                            int apiStatusCode = json.optInt("statusCode", 0);

                            if (success && apiStatusCode == 200) {

                                String message = json.optString("message", "Updated successfully");
                                callback.onSuccess(message); // ✅ return success

                            } else {
                                callback.onError(json.optString("message", "Update failed"));
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
            LoaderUtil.hide();
            e.printStackTrace();
            callback.onError("Request build error");
        }
    }

}

// example inside your RFID callback (may be background thread)
/* String tagId = *//* from reader *//*;
    int percent = *//* computed or mapped from RSSI *//*;
    addOrUpdateTag(tagId, percent);*/
