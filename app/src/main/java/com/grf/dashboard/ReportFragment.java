package com.grf.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.grf.adapter.ReportModuleAdapter;
import com.grf.adapter.ReportTagAdapter;
import com.grf.database.TaskDbHelper;
import com.grf.model.ModuleItem;
import com.grf.model.Task;
import com.grf.smarttagmanager.R;
import com.grf.utils.PermissionUtil;
import com.grf.utils.SnackbarUtils;
import com.grf.viewmodel.ModuleViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReportFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReportFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportFragment newInstance(String param1, String param2) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    ModuleViewModel viewModel;
    List<Task> TaskList = new ArrayList<>();
    TextView tvScannedTitle;
    ReportTagAdapter tagAdapter;
    private final int STORAGE_REQ = 101;

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_report, container, false);
        viewModel = new ViewModelProvider(this).get(ModuleViewModel.class);
        viewModel.initDb(requireContext()); // initialize DB with application context
        tvScannedTitle = root.findViewById(R.id.tvScannedTitle);
        Button ExportCsv = root.findViewById(R.id.btnExport);
        ExportCsv.setOnClickListener(v -> {


            PermissionUtil.requestStoragePermission(requireActivity(), STORAGE_REQ);

            List<String> header = Arrays.asList("Task", "Tag ID", "Zone", "User", "Date", "Found");

            List<List<String>> data = new ArrayList<>();

            for (int i = 0; i < TaskList.size(); i++) {
                try {
//                    String zoneId = ModuleViewModel.getModuleLetter(TaskList.get(i).getZoneId());
                    String zoneId = TaskList.get(i).getZoneId();
                    String found = TaskList.get(i).getTagFound() == 1 ? "YES" : "NO";
                    data.add(Arrays.asList(
                            TaskList.get(i).getTitle(),
                            TaskList.get(i).getTagId(),
                            zoneId,
                            "Admin",
                            TaskList.get(i).getDateTime(),
                            found
                    ));

                    // use t
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            boolean res = ModuleViewModel.ExportCsv(requireContext(), header, data);
            if (res)
                SnackbarUtils.show(root, "Export Successfully");
            else SnackbarUtils.show(root, "Export Failed");
        });

        ImageView back = root.findViewById(R.id.ivBack);
        back.setOnClickListener(v -> {
            try {
                requireActivity().onBackPressed();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        // find views
        RecyclerView rvModules = root.findViewById(R.id.rvModules);
        RecyclerView rvScanned = root.findViewById(R.id.rvScannedTags);

// modules layout: grid 3 columns (adjust)
        try {
            int spanCount = 2;
            GridLayoutManager glm = new GridLayoutManager(requireContext(), spanCount);
            rvModules.setLayoutManager(glm);
        } catch (Throwable t) {
        }

// scanned tags vertical
        try {
            rvScanned.setLayoutManager(new LinearLayoutManager(requireContext()));
        } catch (Throwable t) {
        }

// adapters
        final ReportModuleAdapter moduleAdapter = new ReportModuleAdapter(new ArrayList<ModuleItem>(), new ReportModuleAdapter.Listener() {
            @Override
            public void onModuleClick(ModuleItem item, int position) {
                try {


                    TaskDbHelper.TaskFilter filter = new TaskDbHelper.TaskFilter();
                    filter.zoneId = ModuleViewModel.getModuleLetter(String.valueOf(item.actionId));

                    if (item.actionId == 0) {
                        TaskList = viewModel.getDb().getTasksListNoDuplicate(null);
                    } else
                        TaskList = viewModel.getDb().getTasksListNoDuplicate(filter);

//                    for (Task t : TaskList) {
//                        t.setZoneId(String.valueOf(item.actionId));
//                    }

                    sortTaskListByLatestDate(TaskList);
                    ReloadData(TaskList);

                    tvScannedTitle.setText(String.format("Available Tags (%d)", TaskList.size()));

                    // handle module click (filter scanned tags etc)
                } catch (Throwable t) {
                }
            }
        });
        rvModules.setAdapter(moduleAdapter);

        tagAdapter = new ReportTagAdapter(new ArrayList<Task>(), new ReportTagAdapter.Listener() {
            @Override
            public void onTagClick(Task task, int pos) {
                try {
                    // handle tag click (open details)
                } catch (Throwable t) {
                }
            }
        });
        rvScanned.setAdapter(tagAdapter);

// sample data
        try {
            List<ModuleItem> modules = new ArrayList<>();

            //  TaskList = viewModel.getDb().getTasksList(new TaskDbHelper.TaskFilter());


            TaskList = viewModel.getDb().getTasksListNoDuplicate(null);
            sortTaskListByLatestDate(TaskList);

            int TotalData = TaskList.size();

            modules.add(new ModuleItem("All Zones", 0, "", null, TotalData));
            modules.add(new ModuleItem("Zone A", 1, "", null, filterByZoneId(TaskList, "A").size()));
            modules.add(new ModuleItem("Zone B", 2, "", null, filterByZoneId(TaskList, "B").size()));
            modules.add(new ModuleItem("Zone C", 3, "", null, filterByZoneId(TaskList, "C").size()));
            modules.add(new ModuleItem("Zone D", 4, "", null, filterByZoneId(TaskList, "D").size()));
            modules.add(new ModuleItem("Zone E", 5, "", null, filterByZoneId(TaskList, "E").size()));
            modules.add(new ModuleItem("Zone F", 6, "", null, filterByZoneId(TaskList, "F").size()));

            moduleAdapter.replaceAll(modules);
            tvScannedTitle.setText(String.format("Available Tags (%d)", TotalData));

            tagAdapter.replaceAll(TaskList);

        } catch (Throwable t) {
        }


        // Inflate the layout for this fragment
        return root;
    }

    public void sortTaskListByLatestDate(List<Task> taskList) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

            Collections.sort(taskList, (t1, t2) -> {
                try {
                    String d1 = t1.getDateTime();
                    String d2 = t2.getDateTime();

                    if (d1 == null || d1.trim().isEmpty()) return 1;
                    if (d2 == null || d2.trim().isEmpty()) return -1;

                    Date date1 = sdf.parse(d1);
                    Date date2 = sdf.parse(d2);

                    // Newest first
                    return date2.compareTo(date1);

                } catch (Exception e) {
                    return 0; // Cannot parse date → treat equal
                }
            });

        } catch (Exception e) {
            Log.e("TaskSort", "sortTaskListByLatestDate error: " + e.getMessage());
        }
    }


    public int getValidZoneCount(List<Task> list) {
        int count = 0;

        try {
            if (list == null) return 0;

            for (Task t : list) {
                try {
                    if (t != null &&
                            t.getZoneId() != null &&
                            !t.getZoneId().trim().isEmpty() &&
                            !t.getZoneId().trim().equals("0")) {
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }

        return count;
    }

    public List<Task> filterByZoneId(List<Task> input, String zoneId) {
        List<Task> out = new ArrayList<>();

        try {
            if (input == null || zoneId == null) return out;

            for (Task t : input) {
                try {
                    if (t != null && zoneId.equals(t.getZoneId())) {
                        out.add(t);
                    }
                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }

        return out;
    }


    public void ReloadData(List<Task> newList) {
        try {
            // clear adapter first
            tagAdapter.replaceAll(new ArrayList<>());

            // run replace after 200ms
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    tagAdapter.replaceAll(newList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
