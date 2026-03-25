package com.grf.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grf.model.TagItems;
import com.grf.smarttagmanager.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TagListAdapter extends ArrayAdapter<TagItems> implements Filterable {

    private final LayoutInflater inflater;
    private final List<TagItems> original;
    private List<TagItems> filtered;
    private final Object lock = new Object();
    private final Set<Long> selectedIds = new HashSet<>();
    private int maxSelection = 30; // default to 30

    public TagListAdapter(@NonNull Context context, @NonNull List<TagItems> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
        original = new ArrayList<>(items);
        filtered = new ArrayList<>(items);
        for (TagItems t : items) if (t.selected) selectedIds.add(t.id);
    }

    /** Allow caller to change maximum selection at runtime */
    public void setMaxSelection(int max) {
        try { this.maxSelection = Math.max(1, max); } catch (Exception ignored) {}
    }

    /** Allow caller to read current maximum selection */
    public int getMaxSelection() {
        try { return maxSelection; } catch (Exception ignored) { return 30; }
    }

    @Override
    public int getCount() {
        try { return filtered.size(); } catch (Exception e) { return 0; }
    }

    @Nullable
    @Override
    public TagItems getItem(int position) {
        try { return filtered.get(position); } catch (Exception e) { return null; }
    }

    @Override
    public long getItemId(int position) {
        try {
            TagItems t = filtered.get(position);
            return t == null ? position : t.id;
        } catch (Exception e) { return position; }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            ViewHolder vh;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_tag, parent, false);
                vh = new ViewHolder();
                vh.checkBox = convertView.findViewById(R.id.checkBoxTag);
                vh.name = convertView.findViewById(R.id.textViewTagName);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            TagItems item = filtered.get(position);
            vh.name.setText(item.name);

            vh.checkBox.setOnCheckedChangeListener(null);
            vh.checkBox.setChecked(item.selected);

            final int pos = position;
            vh.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    TagItems tag = filtered.get(pos);
                    if (tag == null) return;

                    if (isChecked) {
                        if (selectedIds.size() >= maxSelection && !selectedIds.contains(tag.id)) {
                            // revert the visual state
                            vh.checkBox.setChecked(false);
                            return;
                        }
                        tag.selected = true;
                        selectedIds.add(tag.id);
                    } else {
                        tag.selected = false;
                        selectedIds.remove(tag.id);
                    }

                    // reflect to original list
                    for (TagItems o : original) if (o.id == tag.id) { o.selected = tag.selected; break; }

                    notifyDataSetChanged();
                } catch (Exception ignored) {}
            });

            return convertView;
        } catch (Exception e) {
            TextView tv = new TextView(getContext());
            tv.setText("Error");
            return tv;
        }
    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView name;
    }

    public List<TagItems> getSelectedTags() {
        try {
            List<TagItems> res = new ArrayList<>();
            for (TagItems t : original) if (t.selected) res.add(t);
            return res;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                try {
                    List<TagItems> list;
                    if (constraint == null || constraint.length() == 0) {
                        synchronized (lock) { list = new ArrayList<>(original); }
                    } else {
                        String s = constraint.toString().toLowerCase().trim();
                        List<TagItems> m = new ArrayList<>();
                        for (TagItems t : original) if (t.name.toLowerCase().contains(s)) m.add(t);
                        list = m;
                    }
                    results.values = list;
                    results.count = list.size();
                } catch (Exception e) {
                    results.values = Collections.emptyList();
                    results.count = 0;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                try {
                    //noinspection unchecked
                    filtered = (List<TagItems>) results.values;
                    if (filtered == null) filtered = new ArrayList<>();
                    notifyDataSetChanged();
                } catch (Exception ignored) {}
            }
        };
    }
}