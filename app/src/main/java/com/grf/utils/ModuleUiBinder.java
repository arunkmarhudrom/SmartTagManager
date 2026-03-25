package com.grf.utils;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.adapter.TaskAdapter;
import com.grf.model.Task;
import com.grf.viewmodel.ModuleViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModuleUiBinder {

    public static void bindTasksRecycler(Context context,
                                         LifecycleOwner owner,
                                         RecyclerView rv,
                                         ModuleViewModel vm,
                                         String moduleId,
                                         TaskAdapter.Listener listener) {
        try {
            // start with empty list (safe)
            TaskAdapter adapter = new TaskAdapter(new ArrayList<>(), listener, vm.getModuleNumber(moduleId));
            rv.setLayoutManager(new LinearLayoutManager(context));
            rv.setAdapter(adapter);

            // observe VM and update adapter when data arrives
            vm.getTasksForModule("").observe(owner, tasks -> {
                try {
                    adapter.replaceAll(tasks != null ? tasks : new ArrayList<Task>());
                } catch (Exception e) {
                    Log.e("ModuleUiBinder", "apply tasks", e);
                }
            });

        } catch (Exception e) {
            Log.e("ModuleUiBinder", "bindTasksRecycler error", e);
        }
    }

    public static List<Task> loadMultiModuleSampleDataOld() {
        List<Task> tasks = new ArrayList<>();
        try {
            for (int m = 0; m < 6; m++) {

                String module = String.valueOf((char) ('A' + m));

                for (int i = 1; i <= 15; i++) {

                    long id = 0;  // let DB autogenerate the real ID

                    String title = "Task " + i + " " + module;
                    int tagCount = i % 5;
                    String tagId = "TAG-" + i;

                        // FIXED: must be String
                    double rssValue = -70 + i;            // FIXED: must be double
                    String zoneId = String.valueOf(m + 1); // FIXED: must be String

                    tasks.add(new Task(id, title, tagCount, tagId, i, rssValue, zoneId, module,0,0, ModuleViewModel.getDateTime()));
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return tasks;
    }


}
