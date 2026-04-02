package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.grf.adapter.TaskAdapter;
import com.grf.database.TaskDbHelper;
import com.grf.model.Task;
import com.grf.smarttagmanager.R;
import com.grf.utils.SnackbarUtils;
import com.grf.viewmodel.ModuleViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageTaskFragment extends Fragment implements TaskAdapter.Listener {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private final List<Task> tasks = new ArrayList<>();
    private TaskDbHelper taskDbHelper;
    private ExecutorService exec;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = null;
        try {
            root = inflater.inflate(R.layout.fragment_manage_task, container, false);

            rvTasks = root.findViewById(R.id.rvTasks);
            exec = Executors.newSingleThreadExecutor();


            // Option A: let TaskDbHelper create its own SqliteDbHelper (using default DB name/version)
            taskDbHelper = new TaskDbHelper(requireContext());

            View btnNewTask = root.findViewById(R.id.btnNewTask);

            btnNewTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Navigation.findNavController(v)
                                .navigate(R.id.action_dashboard_to_newTaskFragment);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });


            ImageView ivBack = root.findViewById(R.id.ivBack);
            ivBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (getActivity() != null) getActivity().onBackPressed();
                    } catch (Throwable t) {
                    }
                }
            });

            setupRecycler();
            loadTasks();
        } catch (Throwable t) {
            // fallback - return an empty view if something failed
            if (root == null) {
                root = new View(getContext());
            }
        }
        return root;
    }


    private void loadTasks() {
        exec.execute(() -> {
            try {
                // get all tasks (blocking off UI thread)
                List<Task> tasks = taskDbHelper.getTasksList(new TaskDbHelper.TaskFilter()); // empty filter => all
                requireActivity().runOnUiThread(() -> {
                    // update RecyclerView / UI with tasks
                    // e.g. adapter.setItems(tasks);
                    if (!tasks.isEmpty())
                        adapter.replaceAll(tasks);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteTask(long boxId) {
        exec.execute(() -> {
            try {
                boolean ok = taskDbHelper.deleteTask(boxId);

                requireActivity().runOnUiThread(() -> {
                    try {
                        if (ok) {
                            loadTasks();
                            SnackbarUtils.show(requireView(), "Task Deleted");
                        } else
                            SnackbarUtils.show(requireView(), "Failed To Deleted");
                    } catch (Throwable t) {
                        Log.e("TAG", "UI after delete error", t);
                        SnackbarUtils.show(requireView(), "Failed To Deleted");
                    }
                });
            } catch (Exception e) {
                Log.e("TAG", "deleteTask error", e);
                SnackbarUtils.show(requireView(), "Failed To Deleted");
            }
        });
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
        taskDbHelper = null;
    }

    private void setupRecycler() {
        try {
            adapter = new TaskAdapter(tasks, this, 0);
            rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvTasks.setAdapter(adapter);
        } catch (Throwable t) {
            // ignore or log
        }
    }

    private void loadSampleDatas() {
        try {
            tasks.clear();

            tasks.add(new Task(0, "Tote find", 9, "0", 0, 0, "0", "", 0, 0, ModuleViewModel.getDateTime(), new ArrayList<>()));
            tasks.add(new Task(0, "Tray Find", 6, "0", 0, 0, "0", "", 0, 0, ModuleViewModel.getDateTime(),new ArrayList<>()));
            tasks.add(new Task(0, "Task_2025-12-03_15:45:58", 3, "0", 0, 0, "0", "", 0, 0, ModuleViewModel.getDateTime(),new ArrayList<>()));


            adapter.replaceAll(tasks);
        } catch (Throwable t) {
            // fallback: leave list empty
        }
    }

    // Adapter listener callbacks
    @Override
    public void onEdit(Task task, int position, int moduleType) {
        try {
            // demo: append " (edited)" to title

            Task updated = new Task(
                    task.getId(),
                    task.getTitle() + " (edited)",
                    task.getTagCount(),
                    "0",       // tagId (String)
                    0,       // boxId FIXED (String)
                    0.0,       // rssValue FIXED (double)
                    "0",       // zoneId FIXED (String)
                    "",        // moduleId
                    0,
                    0, ModuleViewModel.getDateTime(),new ArrayList<>()
            );
            SnackbarUtils.show(requireView(), "Permission denied");

            // adapter.updateAt(position, updated);
        } catch (Throwable t) {
        }
    }

    @Override
    public void onDelete(Task task, int position, int moduleType) {
        try {
            deleteTask(task.getBoxId());
            // immediate delete for demo
            adapter.removeAt(position);
        } catch (Throwable t) {
        }
    }

    @Override
    public void onItemClick(Task task, int position, int Moduletype) {
        try {
            // handle item click (navigate / show details)
        } catch (Throwable t) {
        }
    }
}