package com.grf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grf.model.TagItem;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class TagSearchRecyclerAdapter extends RecyclerView.Adapter<TagSearchRecyclerAdapter.VH> implements Filterable {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<TagItem> fullList = new ArrayList<>();
    private final List<TagItem> filteredList = new ArrayList<>();
    private final Set<String> selectedTotIds = new HashSet<>();

    public TagSearchRecyclerAdapter(Context ctx, List<TagItem> items) {
        Context safeCtx = ctx != null ? ctx : null;
        LayoutInflater safeInflater = LayoutInflater.from(ctx);

        this.context = safeCtx;
        this.inflater = safeInflater;

        try {
            if (items != null) {
                this.fullList.addAll(items);
                this.filteredList.addAll(items);
            }
        } catch (Exception ignored) {}
    }


    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View v = inflater.inflate(R.layout.item_row_tag, parent, false);
            return new VH(v);
        } catch (Exception e) {
            // fallback empty view holder to avoid crash
            View v = new View(parent.getContext());
            return new VH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            final TagItem item = filteredList.get(position);
            holder.tvTagId.setText(item.getTagId() == null ? "" : item.getTagId());

            // avoid triggering listener while setting checked state
            holder.cbSelect.setOnCheckedChangeListener(null);
            boolean checked = selectedTotIds.contains(item.getTotId());
            holder.cbSelect.setChecked(checked);

            // row click toggles selection
            holder.itemView.setOnClickListener(v -> {
                try {
                    toggleSelection(item);
                    notifyItemChanged(position);
                } catch (Exception ignored) {}
            });

            // checkbox click toggles selection
            holder.cbSelect.setOnClickListener(v -> {
                try {
                    toggleSelection(item);
                    notifyItemChanged(position);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    public int getItemCount() {
        try {
            return filteredList.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // ViewHolder
    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTagId;
        final CheckBox cbSelect;

        VH(@NonNull View v) {
            super(v);
            TextView t = null;
            CheckBox c = null;
            try {
                t = v.findViewById(R.id.tvTagId);
                c = v.findViewById(R.id.cbSelect);
            } catch (Exception ignored) {}
            tvTagId = t;
            cbSelect = c;
        }
    }

    // Selection helpers
    public void toggleSelection(TagItem item) {
        try {
            if (item == null) return;
            String totId = item.getTotId();
            if (totId == null) return;
            if (selectedTotIds.contains(totId)) selectedTotIds.remove(totId);
            else selectedTotIds.add(totId);
        } catch (Exception ignored) {}
    }

    public List<TagItem> getSelectedItems() {
        List<TagItem> out = new ArrayList<>();
        try {
            for (TagItem t : fullList) {
                try {
                    if (t != null && selectedTotIds.contains(t.getTotId())) out.add(t);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return out;
    }

    public void setSelectedByIds(Collection<String> totIds) {
        try {
            selectedTotIds.clear();
            if (totIds != null) {
                for (String id : totIds) {
                    try {
                        if (id != null) selectedTotIds.add(id);
                    } catch (Exception ignored) {}
                }
            }
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    public void replaceAll(List<TagItem> items) {
        try {
            fullList.clear();
            if (items != null) fullList.addAll(items);
            filteredList.clear();
            filteredList.addAll(fullList);
            selectedTotIds.clear();
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    // Filterable implementation (searches tagId and totId)
    @Override
    public Filter getFilter() {
        return tagFilter;
    }

    private final Filter tagFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            try {
                List<TagItem> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(fullList);
                } else {
                    String q = constraint.toString().toLowerCase().trim();
                    for (TagItem t : fullList) {
                        try {
                            if (t == null) continue;
                            String tagId = t.getTagId() == null ? "" : t.getTagId().toLowerCase();
                            String totId = t.getTotId() == null ? "" : t.getTotId().toLowerCase();
                            if (tagId.contains(q) || totId.contains(q)) filtered.add(t);
                        } catch (Exception ignored) {}
                    }
                }
                results.values = filtered;
                results.count = filtered.size();
            } catch (Exception e) {
                results.values = new ArrayList<TagItem>();
                results.count = 0;
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            try {
                filteredList.clear();
                if (results != null && results.values instanceof List) {
                    filteredList.addAll((List<TagItem>) results.values);
                }
                notifyDataSetChanged();
            } catch (Exception ignored) {}
        }
    };
}
