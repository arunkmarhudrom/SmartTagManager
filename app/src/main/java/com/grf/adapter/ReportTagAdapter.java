package com.grf.adapter;


import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.model.Task;
import com.grf.smarttagmanager.R;
import com.grf.viewmodel.ModuleViewModel;

import java.util.ArrayList;
import java.util.List;

public class ReportTagAdapter extends RecyclerView.Adapter<ReportTagAdapter.TagVH> {

    public interface Listener {
        void onTagClick(Task task, int pos);
    }

    private final List<Task> items = new ArrayList<>();
    private final Listener listener;

    public ReportTagAdapter(List<Task> initial, Listener listener) {
        this.listener = listener;
        try {
            if (initial != null) items.addAll(initial);
        } catch (Throwable t) {
        }
    }

    @NonNull
    @Override
    public TagVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scanned_tag, parent, false);
            return new TagVH(v);
        } catch (Throwable t) {
            View v = new View(parent.getContext());
            return new TagVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TagVH holder, int position) {
        try {
            holder.bind(items.get(position), position);
        } catch (Throwable t) {
        }
    }

    @Override
    public int getItemCount() {
        try {
            return items.size();
        } catch (Throwable t) {
            return 0;
        }
    }

    public void replaceAll(List<Task> newItems) {
        try {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        } catch (Throwable t) {
        }
    }

    public class TagVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvZone, tvTaskChip, tvEmail, tvDate;

        public TagVH(@NonNull View itemView) {
            super(itemView);
            try {
                tvTitle = itemView.findViewById(R.id.tvTagTitle);
                tvZone = itemView.findViewById(R.id.tvZoneSec);

                tvDate = itemView.findViewById(R.id.tvDate);
                tvTaskChip = itemView.findViewById(R.id.tvTaskChip);
                tvEmail = itemView.findViewById(R.id.tvEmail);

            } catch (Throwable t) {
                tvTitle = new TextView(itemView.getContext());
                tvZone = new TextView(itemView.getContext());
            }
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Task task, final int pos) {
            try {
                // Title: use title if available, otherwise fall back to id
                String title = task.getTitle() != null && !task.getTitle().isEmpty()
                        ? task.getTitle()
                        : String.valueOf(task.getId());
                tvTitle.setText(task.getTagId());

                tvZone.setText("Zone - " + task.getZoneId());
                tvTaskChip.setText(title);
                tvEmail.setText("Admin");
                tvDate.setText(task.getDateTime());


                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (listener != null) listener.onTagClick(task, pos);
                        } catch (Throwable t) {
                            Log.e("", "onClick listener error", t);
                        }
                    }
                });
            } catch (Throwable t) {
                Log.e("TAG", "bind error", t);
            }
        }

    }
}
