package com.grf.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.grf.model.ModuleItem;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.List;

public class ReportModuleAdapter extends RecyclerView.Adapter<ReportModuleAdapter.ModuleVH> {

    public interface Listener {
        void onModuleClick(ModuleItem item, int position);
    }

    private final List<ModuleItem> items = new ArrayList<>();
    private final Listener listener;

    public ReportModuleAdapter(List<ModuleItem> initial, Listener listener) {
        this.listener = listener;
        try {
            if (initial != null) items.addAll(initial);
        } catch (Throwable t) {
            // ignore
        }
    }

    @NonNull
    @Override
    public ModuleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_module, parent, false);
            return new ModuleVH(v);
        } catch (Throwable t) {
            View v = new View(parent.getContext());
            return new ModuleVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleVH holder, int position) {
        try {
            holder.bind(items.get(position), position);
        } catch (Throwable t) {
            // safe
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

    public void replaceAll(List<ModuleItem> newItems) {
        try {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        } catch (Throwable t) {}
    }

    public void updateAt(int pos, ModuleItem item) {
        try {
            if (pos >= 0 && pos < items.size()) {
                items.set(pos, item);
                notifyItemChanged(pos);
            }
        } catch (Throwable t) {}
    }

    public class ModuleVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCount, tvSubtitle;
        public ModuleVH(@NonNull View itemView) {
            super(itemView);
            try {
                tvTitle = itemView.findViewById(R.id.tvModuleTitle);
                tvCount = itemView.findViewById(R.id.tvModuleCount);
                tvSubtitle = itemView.findViewById(R.id.tvModuleSubtitle);
            } catch (Throwable t) {
                tvTitle = new TextView(itemView.getContext());
                tvCount = new TextView(itemView.getContext());
                tvSubtitle = new TextView(itemView.getContext());
            }
        }

        public void bind(final ModuleItem item, final int pos) {
            try {
                tvTitle.setText(item.title != null ? item.title : "");
                tvCount.setText(String.valueOf(item.count));
                tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (listener != null) listener.onModuleClick(item, pos);
                        } catch (Throwable t) {}
                    }
                });
            } catch (Throwable t) {}
        }
    }
}
