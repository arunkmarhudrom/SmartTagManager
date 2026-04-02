package com.grf.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.grf.model.Task;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskVH> {

    public interface Listener {
        void onEdit(Task task, int position, int moduleType);

        void onDelete(Task task, int position, int moduleType);

        void onItemClick(Task task, int position, int moduleType);
    }

    public int ModuleType = 0;
    private final List<Task> items = new ArrayList<>();
    private final Listener listener;

    public TaskAdapter(List<Task> initial, Listener listener, int typeOfModule) {
        this.listener = listener; // assign once, always safe

        try {
            if (initial != null) {
                items.addAll(initial);
                this.ModuleType = typeOfModule;
            }
        } catch (Throwable t) {
            // ignore or log
        }
    }

    @NonNull
    @Override
    public TaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_card, parent, false);
            return new TaskVH(v);
        } catch (Throwable t) {
            // return an empty view holder to avoid crash
            View v = new View(parent.getContext());
            return new TaskVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TaskVH holder, int position) {
        try {
            holder.bind(items.get(position), position);
        } catch (Throwable t) {
            try {
                holder.clear();
            } catch (Throwable ignored) {
            }
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

    // PUBLIC MUTATION METHODS (all with try/catch)
    public void replaceAll(List<Task> newList) {
        try {
            this.items.clear();
            this.items.addAll(newList);
            notifyDataSetChanged(); // 🔥 MUST
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(Task task) {
        try {
            items.add(task);
            notifyItemInserted(items.size() - 1);
        } catch (Throwable t) {
        }
    }

    public void removeAt(int position) {
        try {
            if (position >= 0 && position < items.size()) {
                items.remove(position);
                notifyItemRemoved(position);
            }
        } catch (Throwable t) {
        }
    }

    public void updateAt(int position, Task task) {
        try {
            if (position >= 0 && position < items.size()) {
                items.set(position, task);
                notifyItemChanged(position);
            }
        } catch (Throwable t) {
        }
    }

    public List<Task> getItems() {
        try {
            return new ArrayList<>(items);
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    // VIEW HOLDER
    class TaskVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvTagCount;
        ImageView ivEdit;
        ImageView ivDelete;
        LinearLayout layoutChevron, layoutActions, tagDetailLayout;

        TaskVH(@NonNull View itemView) {
            super(itemView);
            try {
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvTagCount = itemView.findViewById(R.id.tvTagCount);
                ivEdit = itemView.findViewById(R.id.ivEdit);
                ivDelete = itemView.findViewById(R.id.ivDelete);

                layoutChevron = itemView.findViewById(R.id.layoutChevron);
                layoutActions = itemView.findViewById(R.id.layoutActions);
                tagDetailLayout = itemView.findViewById(R.id.tagDetailLayout);

            } catch (Throwable t) {
                // create dummy references to avoid NPE later
                tvTitle = new TextView(itemView.getContext());
                tvTagCount = new TextView(itemView.getContext());
                ivEdit = new ImageView(itemView.getContext());
                ivDelete = new ImageView(itemView.getContext());

                layoutChevron = new LinearLayout(itemView.getContext());
                layoutActions = new LinearLayout(itemView.getContext());
            }
        }

        void bind(final Task task, final int position) {
            try {
                tvTitle.setText(task.getTitle());
                tvTagCount.setText(task.getTagCount() + " tags configured");

                if (ModuleType <= 0) {
                    layoutChevron.setVisibility(GONE);
                    layoutActions.setVisibility(VISIBLE);
                } else {
                    layoutActions.setVisibility(GONE);
                    layoutChevron.setVisibility(VISIBLE);

                }

                if (task.getTaskComplete() == 1) {

                    int semiGreen = Color.parseColor("#BE58EF86");

//                    tagDetailLayout.getBackground().setTint(semiGreen);

                }
                itemView.setOnClickListener(v -> {
                    try {
                        int pos = getAdapterPosition();

                        if (pos != RecyclerView.NO_POSITION && listener != null) {
                            Log.d("CLICK_DEBUG", "Clicked: " + items.get(pos).getTitle());

                            // ✅ ALWAYS CALL (remove restriction)
                            listener.onItemClick(items.get(pos), pos, ModuleType);
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                });

                ivEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (listener != null) listener.onEdit(task, position, ModuleType);
                        } catch (Throwable t) {
                        }
                    }
                });

                ivDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (listener != null) listener.onDelete(task, position, ModuleType);
                        } catch (Throwable t) {
                        }
                    }
                });

            } catch (Throwable t) {
                clear();
            }
        }

        void clear() {
            try {
                tvTitle.setText("");
                tvTagCount.setText("");
                itemView.setOnClickListener(null);
                ivEdit.setOnClickListener(null);
                ivDelete.setOnClickListener(null);
            } catch (Throwable t) {
            }
        }
    }
}