package com.grf.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.grf.model.TagToBeFind;
import com.grf.model.Task;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.List;

public class ViewTaskDetailsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        TextView tvTaskName = view.findViewById(R.id.tvTaskName);
        TextView tvTaskMeta = view.findViewById(R.id.tvTaskMeta);
        RecyclerView rvTaskDetails = view.findViewById(R.id.rvTaskDetails);

        ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        String taskData = getArguments() != null ? getArguments().getString("taskData", "") : "";
        Task task = null;
        try {
            task = new Gson().fromJson(taskData, Task.class);
        } catch (Exception ignored) {
        }

        if (task == null) {
            tvTaskName.setText("Task details not available");
            tvTaskMeta.setText("Total Trays: 0");
            rvTaskDetails.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvTaskDetails.setAdapter(new TrayDetailsAdapter(new ArrayList<>()));
            return;
        }

        tvTaskName.setText(task.getTitle() == null ? "Task" : task.getTitle());
        int total = task.tagToBeFindList == null ? 0 : task.tagToBeFindList.size();
        tvTaskMeta.setText("Total Trays: " + total);

        rvTaskDetails.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTaskDetails.setItemAnimator(null);
        rvTaskDetails.setAdapter(new TrayDetailsAdapter(task.tagToBeFindList == null ? new ArrayList<>() : task.tagToBeFindList));
    }

    private static class TrayDetailsAdapter extends RecyclerView.Adapter<TrayDetailsAdapter.VH> {

        private final List<TagToBeFind> items;

        TrayDetailsAdapter(List<TagToBeFind> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_added_tray, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TagToBeFind item = items.get(position);
            holder.tvTrayId.setText(item.trayId == null ? "-" : item.trayId);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTrayId;
            MaterialButton btnEdit;
            MaterialButton btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTrayId = itemView.findViewById(R.id.tvTrayId);
                btnEdit = itemView.findViewById(R.id.btnEditTray);
                btnDelete = itemView.findViewById(R.id.btnDeleteTray);
            }
        }
    }
}
