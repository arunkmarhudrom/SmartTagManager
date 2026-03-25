package com.grf.dashboard;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
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
import com.grf.database.TagDbHelper;
import com.grf.database.TaskDbHelper;
import com.grf.helper.AppDatabaseHelper;
import com.grf.model.PendingTag;
import com.grf.model.Tag;
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
import com.grf.utils.SnackbarUtils;
import com.grf.utils.SoundUtils;
import com.grf.viewmodel.ModuleViewModel;
import com.nlscan.android.uhf.UHFReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment implements OnKeyPressHandler {

    public ScanFragment() {
        // Required empty public constructor
    }

    private TaskDbHelper taskDbHelper;
    private ExecutorService exec;
    private String moduleName = "";
    private String taskData = "";
    private int moduleType = -1;
    private String taskId = "";
    private Task task;
    private boolean isFromDashboard = false;
    List<String> filterTagId = new ArrayList<>();
    TextView tvHeaderCount, tvSectionTitle;

    List<Tag> AllAvailableTag = new ArrayList<>();
    TagDbHelper tagDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (getArguments() != null) {
                moduleName = getArguments().getString("moduleName", "");
                moduleType = getArguments().getInt("moduleType", -1);
                taskId = getArguments().getString("taskId", "");
                taskData = getArguments().getString("taskData", "");

                isFromDashboard = getArguments().getBoolean("isFromDashboard", false);
                //SoundUtils.play(requireContext(), R.raw.beep);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // ---------- Fragment fields (place at top of your fragment class) ----------
    private List<PendingTag> pendingList = new ArrayList<>();
    private PendingTagsAdapter adapter;
    private final Map<String, Integer> tagIndexMap = new HashMap<>(); // tagId -> index in pendingList

    // handler for optional simulation loop

    private boolean isScanning = false;
    UhfManagerHelper uhfManagerHelper;
    Button btnStartStop;
    int ConfirmTagCount = 0;

    // ---------- onCreateView (simplified) ----------
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        try {
            ModuleViewModel viewModel = new ViewModelProvider(this).get(ModuleViewModel.class);
            tvHeaderCount = view.findViewById(R.id.tvHeaderCount);
            tvSectionTitle = view.findViewById(R.id.tvSectionTitle);

            viewModel.initDb(requireContext()); // initialize DB with application context


            taskDbHelper = viewModel.getDb();

            tagDb = new TagDbHelper(requireContext());

            AllAvailableTag = tagDb.getAllTags();

            exec = Executors.newSingleThreadExecutor();
            if (taskData != null && !taskData.isEmpty()) {

                task = viewModel.jsonToTask(taskData);
                tvHeaderCount.setText(ConfirmTagCount + "/" + task.getTagCount());
                exec.execute(() -> {
                    String tags = taskDbHelper.getTagsByBoxId(task.getBoxId());
                    LogUtils.e("Tags " + tags);
                    filterTagId = getFilterTagList(tags);
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
            tvHeaderTaskTitle.setText(taskId);
            TextView tvHeaderSub = view.findViewById(R.id.tvHeaderSub);
            tvHeaderSub.setText("Active in Zone " + moduleName);

            // Start / Stop button
            btnStartStop = view.findViewById(R.id.btnStartStop);
            btnStartStop.setText("▶  Start Scanning Zone " + moduleName);
            if (task.getTaskComplete() == 1) {
                btnStartStop.setEnabled(false);
                SnackbarUtils.show(view, "Task already completed");
            }

//            List<String> my30 = Arrays.asList(
//                    " BA01CA103000000000000009",
//                    " BA01CA101100000000000006",
//                    " BA01CA103000000000000018",
//                    " BA01CA102200000000000005",
//                    " BA01CA102200000000000011",
//                    " BA01CA101300000000000027",
//                    " BA00002CA320000000470600",
//                    " BA01CA100900000000000009",
//                    " BA01CA102200000000000006",
//                    " 2A5555555555555555555555"
//            );
//    UHFReader.READER_STATE st;
//            st = uhfManagerHelper.applyExactEpcFilters(my30);
//
//            if (st == UHFReader.READER_STATE.OK_ERR) {
//                Log.e("UHF", " Apply filter: " + st);
//            } else {
//                Log.e("UHF", "Failed to apply filter: " + st);
//            }

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
                        btnStartStop.setText("▶  Start Scanning Zone " + moduleName);
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

//
//            // test: add two items quickly to verify UI updates
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                addOrUpdateTag("BA01CA103000000000000009", 45);
//            }, 500);
//
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                addOrUpdateTag("BA01CA103000000000000001", 78);
//            }, 1000);
//
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                adapter.updateSignalByTagId("BA01CA103000000000000001", 50);
//            }, 1000 * 2);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                adapter.updateSignalByTagId("BA01CA103000000000000009", 80);
//            }, 1000 * 3);


        } catch (Throwable t) {
            t.printStackTrace();
        }

        return view;
    }

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 10; // ms throttle

    private void handleIncomingTag(String epc, int rssi) {
        long now = System.currentTimeMillis();

        if (now - lastUpdateTime >= UPDATE_INTERVAL) {
            lastUpdateTime = now;
            int pc = ModuleViewModel.rssiToStrength0to100(rssi);
            addOrUpdateTag(epc, pc, rssi);

        } else {
            // Too fast - ignore this call
            return;
        }
    }

    public List<String> getFilterTagList(String tags) {
        List<String> filterTagId = new ArrayList<>();

        try {
            if (tags != null && !tags.trim().isEmpty()) {

                String[] items = tags.split(",");

                for (String t : items) {
                    filterTagId.add(t.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filterTagId;
    }

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


    RecyclerView rv;

    // ---------- InitBindTagList: prepares recycler + adapter ----------
    private void initAdapter(View view) {
        try {
            RecyclerView rv = view.findViewById(R.id.rvPendingTags);

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

            taskDbHelper.updateTask(u, "boxId = ? AND tagId = ?", new String[]{boxID, tag.getTagId()});

            u.dateTime = null;
            u.zoneId = null;
            u.tagFound = null;
            taskDbHelper.updateTask(u, "boxId = ?", new String[]{boxID});// for updating total tag count to other item
            if (tagFound == 0) { // set task complete
                u.taskComplete = 1;
                taskDbHelper.updateTask(u, "boxId = ?", new String[]{boxID});
            }
        } catch (Exception e) {
            ConfirmTagCount = ConfirmTagCount - 1;
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


    // add this field to your Fragment (or enclosing class)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
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

    public void addOrUpdateTag(final String tagId, final int strengthPercent, final int rssi) {
        try {
            if (tagId == null) return;
            if (getActivity() == null || getView() == null) return;

            // ensure everything runs on main thread
            mainHandler.post(() -> {
                try {
                    if (pendingList == null) pendingList = new ArrayList<>();
                    if (tagIndexMap == null) {

                    }
                    Tag t = getTagByCode(tagId);
                    String toteId = (t != null && t.getToteBarcode() != null)
                            ? t.getToteBarcode()
                            : "Not Found";

                    Integer idxObj = tagIndexMap.get(tagId);
                    if (idxObj == null) {
                        // NEW tag -> append then sort & rebuild map
                        PendingTag newTag = new PendingTag(tagId, 0, strengthPercent, toteId, "", rssi);
                        pendingList.add(newTag);

                        // keep sorted by percent desc (so newly added gets right place)
                        pendingList.sort((a, b) -> Integer.compare(b.getSignalPercent(), a.getSignalPercent()));

                        // rebuild index map after reordering
                        rebuildIndexMap();

                        SoundUtils.play(requireContext(), R.raw.beep);

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
                                SoundUtils.play(requireContext(), R.raw.beep);
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

    public Tag getTagByCode(String code) {
        try {
            if (AllAvailableTag == null || code == null) return null;

            for (Tag t : AllAvailableTag) {
                if (t != null && code.equals(t.getTagCode())) {
                    return t; // return first match
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // not found
    }


    // Optional: remove pending callbacks when view is destroyed to avoid leaks / stale runs
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            mainHandler.removeCallbacksAndMessages(null);
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


    //    @Override
//    public boolean onKeyDownEvent(int keyCode, KeyEvent event) {
//        try {
//            if (keyCode == KeyUpDownCode) {
//
//                // Ignore repeated keyDown while holding
//                if (isKey243Pressed) {
//                    return true;  // already handled
//                }
//
//                isKey243Pressed = true;  // mark as pressed
//
//                LogUtils.i("FragA keyDown: " + keyCode);
//                Toast.makeText(requireContext(), "Frag A: DOWN", Toast.LENGTH_SHORT).show();
//                uhfManagerHelper.startInventory();
//
//
//
//                MainActivity act = (MainActivity) requireActivity();
//                 act.checkInventoryHealth(); // <--- ADD THIS
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onKeyUpEvent(int keyCode, KeyEvent event) {
//        try {
//            if (keyCode == KeyUpDownCode) {
//
//                isKey243Pressed = false; // allow next keyDown
//
//                LogUtils.i("FragA keyUp: " + keyCode);
//                Toast.makeText(requireContext(), "Frag A: UP", Toast.LENGTH_SHORT).show();
//                uhfManagerHelper.stopInventory();
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
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
        taskDbHelper = null;
    }

    void Usage() {


        // 1) Build or parse JSONArray (example)
        JSONArray arr = new JSONArray();
        try {
            JSONObject a = new JSONObject();
            a.put("id", 1);
            a.put("first_name", "Arun");
            a.put("last_name", "Drom");
            a.put("email", "arun@example.com");
            a.put("address", new JSONObject().put("city", "Mumbai").put("zip", "400001"));
            arr.put(a);

            JSONObject b = new JSONObject();
            b.put("id", 2);
            b.put("first_name", "Sita");
            b.put("last_name", "Kumar");
            b.put("email", "sita@example.com");
            arr.put(b);
        } catch (JSONException ignored) {
        }

// 2) Choose columns order (optional). Use dot notation for nested fields:
        List<String> columns = new ArrayList<>();
        columns.add("id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("email");
        columns.add("address.city"); // nested column — exporter will try to access address.city

// 3) Export
        Uri saved = JsonToCsvExporter.exportJsonArrayToCsv(
                requireContext(),
                arr,
                "contacts_export.csv",
                columns,
                "Contacts Export — MyApp", // title row (optional)
                true // include BOM so Excel opens correctly
        );

        if (saved != null) {
            // Optionally share
            JsonToCsvExporter.shareCsv(requireContext(), saved, "Share CSV");
        }

    }


}

// example inside your RFID callback (may be background thread)
/* String tagId = *//* from reader *//*;
    int percent = *//* computed or mapped from RSSI *//*;
    addOrUpdateTag(tagId, percent);*/
