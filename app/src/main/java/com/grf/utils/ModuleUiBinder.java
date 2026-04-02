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

    public static void bindTasksRecycler(Context context, LifecycleOwner owner, RecyclerView rv, ModuleViewModel vm, String moduleId, TaskAdapter.Listener listener) {
        try {
            rv.setLayoutManager(new LinearLayoutManager(context));
            rv.setHasFixedSize(true);

            TaskAdapter adapter = new TaskAdapter(new ArrayList<>(), listener, vm.getModuleNumber(moduleId));
            rv.setAdapter(adapter);

            // ✅ FIXED: use correct LiveData
            vm.getAllTasks().observe(owner, tasks -> {
                try {
                    List<Task> safeList = (tasks != null) ? tasks : new ArrayList<>();

                    Log.d("BINDER_DEBUG", "Tasks size: " + safeList.size());

                    adapter.replaceAll(safeList);

                } catch (Exception e) {
                    Log.e("ModuleUiBinder", "apply tasks", e);
                }
            });

        } catch (Exception e) {
            Log.e("ModuleUiBinder", "bindTasksRecycler error", e);
        }
    }


}
