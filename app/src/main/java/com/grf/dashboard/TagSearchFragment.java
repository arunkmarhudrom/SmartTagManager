package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.grf.adapter.PopupTagAdapter;
import com.grf.database.TagDbHelper;
import com.grf.model.Tag;
import com.grf.model.TagItem;
import com.grf.smarttagmanager.R;


import android.app.Dialog;
import android.content.Context;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class TagSearchFragment extends DialogFragment {

    public TagSearchFragment() {
        // empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_tag_search, container, false);
        } catch (Exception e) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            super.onViewCreated(view, savedInstanceState);

            // back button wiring (dismiss fragment)
            try {
                ImageView back = view.findViewById(R.id.ivBack);
                back.setOnClickListener(v -> {
                    try {
                        requireActivity().onBackPressed();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception ignored) {
            }

            // Optional: set module title if you want to change it dynamically
            try {
                android.widget.TextView tv = view.findViewById(R.id.tvModuleTitleB);
                if (tv != null) {
                    // tv.setText("Select Tags"); // uncomment to change text programmatically
                }
            } catch (Exception ignored) {
            }

            // Bind your dropdown UI
            BindDropDownUi(view);

        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStart() {
        try {
            super.onStart();
            Dialog d = getDialog();
            if (d != null && d.getWindow() != null) {
                d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Exception ignored) {
        }
    }

    // ----------------------------
    // BindDropDownUi implementation (edited to use TotId instead of TagId where appropriate)
    // ----------------------------
    MaterialAutoCompleteTextView autoCompleteTags;
    TagDbHelper tagDb;
    androidx.appcompat.widget.ListPopupWindow popup;

    void BindDropDownUi(View view) {
        try {

            autoCompleteTags = view.findViewById(R.id.autoCompleteTags);
            final android.widget.ImageButton btnAddTag = view.findViewById(R.id.btnAddTag);
            final com.google.android.material.textfield.TextInputLayout til =
                    view.findViewById(R.id.textInputLayoutTags);

            tagDb = new TagDbHelper(requireContext());

            List<Tag> tagList = tagDb.getAllTags();
            List<TagItem> allTags = new ArrayList<>();

            for (Tag t : tagList) {
                try {
                    // TagItem(totId, tagId)
                    TagItem item = new TagItem(
                            t.getToteBarcode(),
                            t.getTagCode()
                    );
                    allTags.add(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final PopupTagAdapter popupAdapter = new PopupTagAdapter(requireContext(), allTags);
            final boolean[] suppressPopup = {false};

            try {
                popupAdapter.setSelectionListener(selectedList -> {
                    try {
                        StringBuilder sb = new StringBuilder();
                        try {
                            if (selectedList != null) {
                                for (TagItem ti : selectedList) {
                                    try {
                                        // USE TotId instead of TagId
                                        sb.append(ti.getTotId()).append(", ");
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        String built = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : sb.toString();

                        suppressPopup[0] = true;

                        try {
                            if (built == null || built.isEmpty()) {
                                autoCompleteTags.setText("", false);
                                til.setEndIconVisible(false);
                            } else {
                                String toSet = built + ", ";
                                autoCompleteTags.setText(toSet, false);
                                autoCompleteTags.setSelection(autoCompleteTags.length());
                                til.setEndIconVisible(true);
                            }
                        } catch (Exception ignored) {
                        }

                        autoCompleteTags.postDelayed(() -> {
                            try {
                                suppressPopup[0] = false;
                            } catch (Exception ignored) {
                            }
                        }, 150);

                    } catch (Exception ignored) {
                    }
                });

            } catch (Exception ignored) {
            }

            popup = new androidx.appcompat.widget.ListPopupWindow(requireContext());
            try {
                popup.setAdapter(popupAdapter);
                popup.setAnchorView(autoCompleteTags);
                popup.setModal(false);
                autoCompleteTags.post(() -> {
                    try {
                        popup.setWidth(autoCompleteTags.getWidth());
                    } catch (Exception ignored) {
                    }
                });
                popup.setHeight(dpToPx(240));
            } catch (Exception ignored) {
            }

            final java.util.function.Function<String, String> getLastToken = (full) -> {
                try {
                    if (full == null) return "";
                    int idx = full.lastIndexOf(',');
                    if (idx == -1) return full.trim();
                    return full.substring(idx + 1).trim();
                } catch (Exception e) {
                    return "";
                }
            };

            final java.util.function.BiConsumer<String, TagItem> replaceLastTokenWithSelection = (currentFull, tag) -> {
                try {
                    String t = currentFull == null ? "" : currentFull;
                    int idx = t.lastIndexOf(',');
                    String prefix = idx == -1 ? "" : t.substring(0, idx + 1);
                    prefix = prefix.replaceAll("\\s+$", "");
                    if (!prefix.isEmpty()) prefix = prefix + " ";
                    // REPLACE: use TotId
                    final String newText = prefix + tag.getTotId() + ", ";

                    suppressPopup[0] = true;
                    try {
                        autoCompleteTags.setText(newText, false);
                        autoCompleteTags.setSelection(newText.length());
                    } catch (Exception ignored) {
                    }

                    autoCompleteTags.postDelayed(() -> {
                        try {
                            suppressPopup[0] = false;
                        } catch (Exception ignored) {
                        }
                    }, 150);
                } catch (Exception ignored) {
                }
            };

            final java.util.function.Function<Void, String> buildTextFromSelected = (v) -> {
                try {
                    java.util.List<TagItem> sel = popupAdapter.getSelectedItems();
                    if (sel == null || sel.isEmpty()) return "";
                    StringBuilder sb = new StringBuilder();
                    for (TagItem ti : sel) {
                        // USE TotId instead of TagId
                        sb.append(ti.getTotId()).append(", ");
                    }
                    if (sb.length() > 2) sb.setLength(sb.length() - 2);
                    return sb.toString();
                } catch (Exception ignored) {
                    return "";
                }
            };

            try {
                til.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT);
                til.setEndIconOnClickListener(v -> {
                    try {
                        autoCompleteTags.setText("");
                        autoCompleteTags.clearFocus();
                        popupAdapter.clearSelection();
                        popupAdapter.notifyDataSetChanged();
                        til.setEndIconVisible(false);
                        if (popup.isShowing()) popup.dismiss();
                    } catch (Exception ignored) {
                    }
                });
                try {
                    til.setEndIconVisible(!popupAdapter.getSelectedItems().isEmpty());
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }

            autoCompleteTags.setOnClickListener(v -> {
                try {
                    if (suppressPopup[0]) return;
                    if (popup.isShowing()) popup.dismiss();
                    else {
                        String last = getLastToken.apply(autoCompleteTags.getText() == null ? "" : autoCompleteTags.getText().toString());
                        popupAdapter.getFilter().filter(last);
                        popup.show();
                    }
                } catch (Exception ignored) {
                }
            });

            autoCompleteTags.setOnFocusChangeListener((v, hasFocus) -> {
                try {
                    if (suppressPopup[0]) return;
                    if (hasFocus) {
                        String last = getLastToken.apply(autoCompleteTags.getText() == null ? "" : autoCompleteTags.getText().toString());
                        popupAdapter.getFilter().filter(last);
                        popup.show();
                    }
                } catch (Exception ignored) {
                }
            });

            autoCompleteTags.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        if (suppressPopup[0]) return;

                        String full = s == null ? "" : s.toString();
                        String last = getLastToken.apply(full);

                        popupAdapter.getFilter().filter(last);

                        if (!popup.isShowing()) popup.show();

                        try {
                            til.setEndIconVisible(s != null && s.length() > 0);
                        } catch (Exception ignored) {
                        }

                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            });

            popup.setOnItemClickListener((parent, v, position, id) -> {
                try {
                    Object obj = popupAdapter.getItem(position);
                    if (!(obj instanceof TagItem)) return;
                    TagItem chosen = (TagItem) obj;

                    // toggle selection - selection listener will update the AutoComplete text
                    popupAdapter.toggleSelection(chosen);
                    popupAdapter.notifyDataSetChanged();

                    if (!popup.isShowing()) popup.show();
                } catch (Exception ignored) {
                }
            });

            btnAddTag.setOnClickListener(v -> {
                try {
                    String selectedText = autoCompleteTags.getText() == null ? "" : autoCompleteTags.getText().toString();


                    List<String> totIdList = new ArrayList<>();

                    for (String s : selectedText.split(",")) {
                        if (s != null && !s.trim().isEmpty()) {
                            totIdList.add(s.trim());
                        }
                    }

                    List<String> tagIds = popupAdapter.getTagidByTotid(totIdList);

                    String tagIdList = String.join(",", tagIds);

                    // SEND RESULT BACK
                    Bundle result = new Bundle();
                    result.putString("selected_tags", tagIdList);
                    getParentFragmentManager().setFragmentResult("tag_result", result);

                    requireActivity().onBackPressed();
                } catch (Exception ignored) {
                }
            });

            popup.setOnDismissListener(() -> {
                try {
                    String built = buildTextFromSelected.apply(null);
                    if (!built.isEmpty()) {
                        suppressPopup[0] = true;
                        try {
                            String toSet = built + (built.endsWith(",") ? " " : ", ");
                            autoCompleteTags.setText(toSet, false);
                            autoCompleteTags.setSelection(autoCompleteTags.length());
                        } catch (Exception ignored) {
                        }
                        autoCompleteTags.postDelayed(() -> {
                            try {
                                suppressPopup[0] = false;
                            } catch (Exception ignored) {
                            }
                        }, 150);

                    } else {
                        try {
                            til.setEndIconVisible(!android.text.TextUtils.isEmpty(autoCompleteTags.getText()));
                        } catch (Exception ignored) {
                        }
                    }

                    try {
                        popupAdapter.notifyDataSetChanged();
                    } catch (Exception ignored) {
                    }

                } catch (Exception ignored) {
                }
            });

        } catch (Exception ignored) {
        }
    }

    // helper: convert dp to px
    private int dpToPx(int dp) {
        try {
            Context ctx = requireContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        } catch (Exception e) {
            return dp;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (popup.isShowing()) popup.dismiss();
        } catch (Exception e) {

        }
    }
}
