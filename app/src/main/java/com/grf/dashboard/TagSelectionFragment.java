package com.grf.dashboard;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.grf.adapter.TagListAdapter;
import com.grf.helper.TagSelectionCallback;
import com.grf.model.TagItems;
import com.grf.smarttagmanager.R;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class TagSelectionFragment extends Fragment {

    private TagListAdapter adapter;
    private AutoCompleteTextView autoComplete;
    private ArrayAdapter<String> autoAdapter;
    private TagSelectionCallback callback;

    public void setCallback(TagSelectionCallback cb) {
        try {
            this.callback = cb;
        } catch (Exception ignored) {
        }
    }

    /**
     * Optional fragment arguments:
     * - ArrayList<String> "tags"       : list of tag names to bind (id will be index+1)
     * - int "sample_count"            : when "tags" not provided, number of generated sample tags (default 30)
     * - int "max_selection"           : override maximum selection (default 30)
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View root = inflater.inflate(R.layout.fragment_tag_selection, container, false);

            autoComplete = root.findViewById(R.id.autoCompleteTags);
            ListView listView = root.findViewById(R.id.listViewTags);

            // Read args if provided
            Bundle args = getArguments();
            ArrayList<String> providedNames = null;
            int sampleCount = 30;
            int maxSelectionArg = 30;

            if (args != null) {
                try {
                    if (args.containsKey("tags")) {
                        providedNames = args.getStringArrayList("tags");
                    }
                    if (args.containsKey("sample_count")) {
                        sampleCount = Math.max(1, args.getInt("sample_count", 30));
                    }
                    if (args.containsKey("max_selection")) {
                        maxSelectionArg = Math.max(1, args.getInt("max_selection", 30));
                    }
                } catch (Exception ignored) {
                }
            }

            // Build list of TagItems (either provided list or generated sample)
            final List<TagItems> items = new ArrayList<>();
            if (providedNames != null && !providedNames.isEmpty()) {
                long id = 1;
                for (String name : providedNames) {
                    items.add(new TagItems(id++, name == null ? "" : name));
                }
            } else {
                for (int i = 1; i <= sampleCount; i++) {
                    items.add(new TagItems(i, "Tag " + i));
                }
            }

            adapter = new TagListAdapter(requireContext(), items);
            adapter.setMaxSelection(maxSelectionArg);
            listView.setAdapter(adapter);

            // prepare AutoComplete adapter (names)
            List<String> names = new ArrayList<>();
            for (TagItems t : items) names.add(t.name);
            autoAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            autoComplete.setAdapter(autoAdapter);
            autoComplete.setThreshold(1);

            // choosing from autocomplete selects the tag
            autoComplete.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    String pick = autoAdapter.getItem(position);
                    if (pick == null) return;

                    for (TagItems t : items) {
                        if (t.name.equals(pick)) {
                            if (!t.selected && adapter.getSelectedTags().size() >= adapter.getMaxSelection()) {
                                Toast.makeText(requireContext(), "Maximum " + adapter.getMaxSelection() + " allowed", Toast.LENGTH_SHORT).show();
                                break;
                            }
                            t.selected = true;
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateHint();
                    autoComplete.setText("");
                } catch (Exception ignored) {
                }
            });

            // filter list as user types
            autoComplete.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        adapter.getFilter().filter(s);
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            // clicking list items toggles selection, observing max selection
            listView.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    TagItems t = adapter.getItem(position);
                    if (t == null) return;

                    if (!t.selected && adapter.getSelectedTags().size() >= adapter.getMaxSelection()) {
                        Toast.makeText(requireContext(), "Maximum " + adapter.getMaxSelection() + " allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    t.selected = !t.selected;
                    adapter.notifyDataSetChanged();
                    updateHint();
                } catch (Exception ignored) {
                }
            });

            return root;
        } catch (Exception e) {
            TextView tv = new TextView(requireContext());
            tv.setText("Error: " + e.getMessage());
            return tv;
        }
    }

    private void updateHint() {
        try {
            List<TagItems> sel = adapter.getSelectedTags();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sel.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(sel.get(i).name);
            }
            autoComplete.setHint(sb.length() == 0 ? "Search or type..." : sb.toString());
        } catch (Exception ignored) {
        }
    }

    public List<Long> getSelectedIds() {
        List<Long> ids = new ArrayList<>();
        try {
            for (TagItems t : adapter.getSelectedTags()) ids.add(t.id);
        } catch (Exception ignored) {
        }
        return ids;
    }
}