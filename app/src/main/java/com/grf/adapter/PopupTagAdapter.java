package com.grf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.grf.model.TagItem;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PopupTagAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<TagItem> fullList;
    private final List<TagItem> filteredList;
    private final Set<String> selectedTotIds;

    public PopupTagAdapter(Context ctx, List<TagItem> items) {
        this.context = ctx;
        this.inflater = LayoutInflater.from(ctx);
        this.fullList = new ArrayList<>();
        if (items != null) this.fullList.addAll(items);
        this.filteredList = new ArrayList<>();
        this.filteredList.addAll(this.fullList);
        this.selectedTotIds = new HashSet<>();
    }

    @Override
    public int getCount() {
        try { return filteredList.size(); } catch (Exception e) { return 0; }
    }

    @Override
    public Object getItem(int position) {
        try { return filteredList.get(position); } catch (Exception e) { return null; }
    }

    @Override
    public long getItemId(int position) { return position; }

    static class VH {
        TextView tvTagId;
        CheckBox cbSelect;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            VH vh;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_row_tag, parent, false);
                vh = new VH();
                vh.tvTagId = convertView.findViewById(R.id.tvTagId);
                vh.cbSelect = convertView.findViewById(R.id.cbSelect);
                convertView.setTag(vh);
            } else {
                vh = (VH) convertView.getTag();
            }

            TagItem item = filteredList.get(position);

            // <-- BIND TotId (not TagId)
            try {
                String display = item != null && item.getTotId() != null ? item.getTotId() :
                        (item != null && item.getTagId() != null ? item.getTagId() : "");
                vh.tvTagId.setText(display);
                vh.tvTagId.setContentDescription(display);
            } catch (Exception ignored) {
                vh.tvTagId.setText("");
            }

            vh.cbSelect.setOnCheckedChangeListener(null);
            boolean checked = item != null && selectedTotIds.contains(item.getTotId());
            vh.cbSelect.setChecked(checked);

            // row click toggles selection
            convertView.setOnClickListener(v -> {
                try {
                    toggleSelection(item);
                    notifyDataSetChanged();
                } catch (Exception ignored) {}
            });

            // checkbox click toggles selection
            vh.cbSelect.setOnClickListener(v -> {
                try {
                    toggleSelection(item);
                    notifyDataSetChanged();
                } catch (Exception ignored) {}
            });

            View divider = convertView.findViewById(R.id.divider);
            if (position == filteredList.size() - 1)
                divider.setVisibility(View.GONE);
            else
                divider.setVisibility(View.VISIBLE);

            return convertView;
        } catch (Exception e) {
            // fallback empty view
            return new View(context);
        }
    }

    // Public toggle
    public void toggleSelection(TagItem item) {
        try {
            if (item == null) return;
            String id = item.getTotId();
            if (id == null) return;
            if (selectedTotIds.contains(id)) {
                selectedTotIds.remove(id);
            } else {
                selectedTotIds.add(id);
            }
            // notify listener immediately after change
            try {
                if (selectionListener != null) selectionListener.onSelectionChanged(getSelectedItems());
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }


    public List<TagItem> getSelectedItems() {
        List<TagItem> out = new ArrayList<>();
        try {
            for (TagItem t : fullList) {
                try {
                    if (t != null && t.getTotId() != null && selectedTotIds.contains(t.getTotId())) out.add(t);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return out;
    }

    public Set<String> getSelectedTotIds() {
        return selectedTotIds;
    }

    public void clearSelection() {
        try { selectedTotIds.clear(); notifyDataSetChanged(); } catch (Exception ignored) {}
    }

    public void replaceAll(List<TagItem> newList) {
        try {
            fullList.clear();
            if (newList != null) fullList.addAll(newList);
            filteredList.clear();
            filteredList.addAll(fullList);
            selectedTotIds.clear();
            notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    /**
     * Returns true if the given TagItem is currently selected.
     */
    public boolean isSelected(TagItem item) {
        try {
            if (item == null) return false;
            return selectedTotIds.contains(item.getTotId());
        } catch (Exception ignored) { return false; }
    }

    /**
     * Convenience: check by totId directly.
     */
    public boolean isSelectedById(String totId) {
        try {
            if (totId == null) return false;
            return selectedTotIds.contains(totId);
        } catch (Exception ignored) { return false; }
    }


    /** listener to notify when selection changes */
    public interface SelectionListener {
        void onSelectionChanged(List<TagItem> selected);
    }

    private SelectionListener selectionListener;

    public void setSelectionListener(SelectionListener l) {
        try { this.selectionListener = l; } catch (Exception ignored) {}
    }

    /**
     * Replace selection set from a collection of totIds (safe, idempotent).
     */
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
            // notify listener once with current selection snapshot
            try {
                if (selectionListener != null) selectionListener.onSelectionChanged(getSelectedItems());
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    /**
     * Accept a list of TagItem and mark those totIds selected (convenience).
     */
    public void setSelectedFromTagItems(List<TagItem> items) {
        try {
            selectedTotIds.clear();
            if (items != null) {
                for (TagItem t : items) {
                    try {
                        if (t != null && t.getTotId() != null) selectedTotIds.add(t.getTotId());
                    } catch (Exception ignored) {}
                }
            }
            notifyDataSetChanged();
            try {
                if (selectionListener != null) selectionListener.onSelectionChanged(getSelectedItems());
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }



    // Filter implementation (search by tagId or totId)
    @Override
    public Filter getFilter() { return tagFilter; }

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
                        if (t.getTagId() != null && t.getTagId().toLowerCase().contains(q)) {
                            filtered.add(t);
                        } else if (t.getTotId() != null && t.getTotId().toLowerCase().contains(q)) {
                            filtered.add(t);
                        }
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

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            try {
                filteredList.clear();
                //noinspection unchecked
                filteredList.addAll((List<TagItem>) results.values);
                notifyDataSetChanged();
            } catch (Exception ignored) {}
        }
    };


    public List<String> getTagidByTotid(List<String> totIds) {
        List<String> result = new ArrayList<>();

        try {
            if (totIds == null || totIds.isEmpty()) return result;

            // Faster lookup
            Set<String> lookup = new HashSet<>();
            for (String id : totIds) {
                if (id != null) lookup.add(id.trim());
            }

            // Match totId → return tagId
            for (TagItem item : fullList) {
                try {
                    if (item != null && item.getTotId() != null) {
                        if (lookup.contains(item.getTotId())) {
                            if (item.getTagId() != null) {
                                result.add(item.getTagId());
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

        } catch (Exception ignored) {}

        return result;
    }


}
