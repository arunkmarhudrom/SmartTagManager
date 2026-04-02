package com.grf.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import com.grf.adapter.PopupTagAdapter;

import com.grf.database.TagDbHelper;
import com.grf.database.TaskDbHelper;
import com.grf.model.Tag;
import com.grf.model.TagItem;
import com.grf.model.Task;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.utils.FilePickerUtils;
import com.grf.utils.LogUtils;
import com.grf.utils.PopupUtils;
import com.grf.utils.SnackbarUtils;
import com.grf.viewmodel.ModuleViewModel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class NewTaskFragment extends Fragment {
    private EditText etTaskName;


    private TaskDbHelper taskDbHelper;
    private ExecutorService exec;

    private final List<String> addedTags = new ArrayList<>();

    TagDbHelper tagDb;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_new_task, container, false);

        try {


            exec = Executors.newSingleThreadExecutor();
            taskDbHelper = new TaskDbHelper(requireContext());
            etTaskName = root.findViewById(R.id.etTaskName);

            try {
                etTaskName.setText("Task_" + new java.text.SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

            } catch (Throwable t) {
            }


            ImageView ivBack = root.findViewById(R.id.ivBack);

            ivBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Navigation.findNavController(v).popBackStack();
                    } catch (Throwable t) {
                    }
                }
            });
            //   etTagInput = root.findViewById(R.id.etTagInput);

//            try {
//                // Disable autocorrect / suggestions
//                etTagInput.setInputType(
//                        InputType.TYPE_CLASS_TEXT |
//                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
//                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
//                );
//
//                // Force uppercase + block space
//                etTagInput.setFilters(new InputFilter[]{
//                        (source, start, end, dest, dstart, dend) -> {
//                            try {
//                                StringBuilder out = new StringBuilder();
//                                for (int i = start; i < end; i++) {
//                                    char c = source.charAt(i);
//
//                                    // Ignore space
//                                    if (c == ' ') continue;
//
//                                    // Always uppercase
//                                    out.append(Character.toUpperCase(c));
//                                }
//                                return out.toString();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                return "";
//                            }
//                        }
//                });
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


            MaterialButton btnSaveTask = root.findViewById(R.id.btnSaveTask);
            //  chipGroupTags = root.findViewById(R.id.chipGroupTags);
            //   tvAddedTagsTitle = root.findViewById(R.id.tvAddedTagsTitle);


            btnSaveTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String taskName = etTaskName.getText().toString();
                    insertTestData(taskName);
                }
            });

            getParentFragmentManager().setFragmentResultListener("tag_result", this, (requestKey, bundle) -> {
                try {
                    String selectedText = bundle.getString("selected_tags", "");
//                    android.widget.Toast.makeText(requireContext(), "Selected: " + selectedText, android.widget.Toast.LENGTH_LONG).show();

                    autoCompleteTags.setText(selectedText);

                } catch (Exception ignored) {
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
        BindDropDownUi(root);
        return root;
    } // Call this from a button click or wherever you need to open picker

    private FilePickerUtils filePicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tagDb = new TagDbHelper(requireContext());


        filePicker = new FilePickerUtils(new FilePickerUtils.Callback() {
            @Override
            public void onFilesPicked(@NonNull List<String> fileData) {
                // handle on background thread or post to UI
                parseAndInsertTags(fileData);
            }

            @Override
            public void onError(@NonNull String message) {
                // show error on UI thread
                Log.e("ImportFragment", "FilePicker error: " + message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        filePicker.register(this); // IMPORTANT
    }

    EditText autoCompleteTags;
    String listTagIds = "";

    private void parseAndInsertTags(List<String> fileData) {
        try {
            listTagIds = "";

            List<Tag> tags = new ArrayList<>();

            boolean skipHeader = true;

            for (String line : fileData) {
                try {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Skip header row (Sl. No | Tote Barcode | Tag Code)
                    if (skipHeader) {
                        skipHeader = false;
                        continue;
                    }

                    // CSV or TAB-safe split
                    String[] cols;
                    if (line.contains("\t")) {
                        cols = line.split("\t");
                    } else {
                        cols = line.split(",");
                    }

                    int slNo = 0;
                    String tote = "";
                    String tagCode = "";

                    if (cols == null || cols.length == 0) {
                        // nothing to do
                    } else if (cols.length == 1) {
                        // only tote
                        tote = cols[0].trim();

                    } else if (cols.length == 2) {
                        // slNo , tote
                        try {
                            slNo = Integer.parseInt(cols[0].trim());
                        } catch (Exception ignored) { /* leave slNo = 0 if parse fails */ }
                        tote = cols[1].trim();

                    } else { // cols.length >= 3
                        // slNo , tote , tagCode (ignore extra columns)
                        try {
                            slNo = Integer.parseInt(cols[0].trim());
                        } catch (Exception ignored) {}
                        tote = cols[1].trim();
                        tagCode = cols[2].trim();
                    }

                    if (!tote.isEmpty()) {
                        if (!listTagIds.isEmpty()) {
                            listTagIds += ",";
                        }
                        listTagIds += tote;
                    }

                    // Build Tag object
                    Tag t = new Tag();
                    t.setSlNo(slNo);
                    t.setToteBarcode(tote);
                    t.setTagCode(tagCode);

                    // Hard-coded values
                    t.setActive(0);
                    t.setZone(0);
                    t.setModule("All");

                    // Current date-time
                    String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(new java.util.Date());
                    t.setDateTime(now);

                    tags.add(t);

                } catch (Exception e) {
                    Log.e("IMPORT", "Failed to parse line: " + line, e);
                }
            }
            PopupUtils.showCustomYesNoDialog(
                    requireContext(),
                    "Task Create ?",
                    "Yes → Create Task\n" +
                            "No → Import Data",
                    new PopupUtils.PopupCallback() {
                        @Override
                        public void onYes() {
                            try {

                                getAllTagIdAsync(listTagIds)
                                        .thenAccept(TagIds -> {
                                            if (TagIds.isEmpty()) {
                                                SnackbarUtils.show(requireView(), "No Dat Found");
                                                return;
                                            }
                                            autoCompleteTags.setText(TagIds);
                                            String taskName = etTaskName.getText().toString();
                                            insertTestData(taskName);

                                        });


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onNo() {
                            // Insert into DB
                            if (!tags.isEmpty()) {
                                int inserted = tagDb.insertTags(tags);
                                SnackbarUtils.show(requireView(), "Imported " + inserted + " Data");
                            } else {
                                SnackbarUtils.show(requireView(), "No valid rows found");
                            }
                        }

                        @Override
                        public void onCLose() {
                            // no-op
                        }
                    }
            );


        } catch (Exception e) {
            Log.e("IMPORT", "parseAndInsertTags ERROR", e);
        }
    }

    MaterialButton btnAddTag;

    public CompletableFuture<String> getAllTagIdAsync(String TagCodes) {
        return CompletableFuture.supplyAsync(() -> tagDb.getAllTagIdAsync(TagCodes));
    }


    // inside your fragment (where BindDropDownUi is)
    void BindDropDownUi(View view) {
        try {
            autoCompleteTags = view.findViewById(R.id.autoCompleteTags);
            btnAddTag = view.findViewById(R.id.btnAddTag);

            // Listen for result from TagSearchFragment
            try {
                getParentFragmentManager().setFragmentResultListener(
                        "tag_selection", // requestKey
                        getViewLifecycleOwner(),
                        (requestKey, bundle) -> {
                            try {
                                if (bundle == null) return;
                                ArrayList<String> selectedTagIds = bundle.getStringArrayList("selected_tag_ids");
                                if (selectedTagIds == null) selectedTagIds = new ArrayList<>();

                                // join with commas (no spaces) e.g. TAG-A, TAG-B -> "TAG-A,TAG-B"
                                String joined = android.text.TextUtils.join(",", selectedTagIds);

                                try {
                                    autoCompleteTags.setText(joined);
                                } catch (Exception ignored) {
                                }

                            } catch (Exception ignored) {
                            }
                        }
                );
            } catch (Exception ignored) {
            }

            // Open TagSearchFragment when edittext or button clicked
            View.OnClickListener openSearch = v -> {
                try {
                    // optionally pass initial selected ids; here not passing any
                    android.os.Bundle args = new android.os.Bundle();
                    // args.putStringArrayList("initial_selected_ids", someList); // optional
                    Navigation.findNavController(v).navigate(R.id.action_newTask_to_searchFragment, args);
                } catch (Exception ignored) {
                }
            };

            try {
                autoCompleteTags.setOnClickListener(openSearch);
            } catch (Exception ignored) {
            }
            try {
                btnAddTag.setOnClickListener(v -> {
                    filePicker.launchPicker();
                });
            } catch (Exception ignored) {
            }

        } catch (Exception ignored) {
        }
    }


    void BindDropDownUid(View view) {
        try {


            final MaterialAutoCompleteTextView autoCompleteTags = view.findViewById(R.id.autoCompleteTags);
            final ImageButton btnAddTag = view.findViewById(R.id.btnAddTag);
            final com.google.android.material.textfield.TextInputLayout til =
                    view.findViewById(R.id.textInputLayoutTags);

            // sample data - replace with real data
            List<TagItem> allTags = new ArrayList<>();
            try {
                allTags.add(new TagItem("TOT1001", "TAG-A1B2C3"));
                allTags.add(new TagItem("TOT1002", "TAG-D4E5F6"));
                allTags.add(new TagItem("TOT1003", "TAG-G7H8I9"));
                allTags.add(new TagItem("TOT1004", "TAG-123456"));
                allTags.add(new TagItem("TOT1005", "TAG-TEST01"));
                allTags.add(new TagItem("TOT1006", "TAG-SAMPLE"));
                allTags.add(new TagItem("TOT1007", "TAG-XYZ001"));
                allTags.add(new TagItem("TOT1008", "TAG-ALPHA"));
                allTags.add(new TagItem("TOT1009", "TAG-BETA"));
                allTags.add(new TagItem("TOT1010", "TAG-GAMMA"));
            } catch (Exception ignored) {
            }

            final PopupTagAdapter popupAdapter = new PopupTagAdapter(requireContext(), allTags);
            // flag to suppress programmatic updates re-triggering popup
            final boolean[] suppressPopup = {false};

            // register selection listener so UI binds immediately on check/uncheck
            try {
                popupAdapter.setSelectionListener(selectedList -> {
                    try {
                        StringBuilder sb = new StringBuilder();
                        try {
                            if (selectedList != null) {
                                for (TagItem ti : selectedList) {
                                    try {
                                        sb.append(ti.getTagId()).append(", ");
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        String built = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : sb.toString();

                        suppressPopup[0] = true;  // ← FIXED

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

            // ListPopupWindow setup
            final androidx.appcompat.widget.ListPopupWindow popup = new androidx.appcompat.widget.ListPopupWindow(requireContext());
            try {
                popup.setAdapter(popupAdapter);
                popup.setAnchorView(autoCompleteTags);
                popup.setModal(false); // allow multi-select interactions
                autoCompleteTags.post(() -> {
                    try {
                        popup.setWidth(autoCompleteTags.getWidth());
                    } catch (Exception ignored) {
                    }
                });
                popup.setHeight(dpToPx(240));
            } catch (Exception ignored) {
            }


            // utility lambdas (as final objects) to work with comma-separated tokens
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
                    String prefix = idx == -1 ? "" : t.substring(0, idx + 1); // include comma if exists
                    prefix = prefix.replaceAll("\\s+$", "");
                    if (!prefix.isEmpty()) prefix = prefix + " ";
                    final String newText = prefix + tag.getTagId() + ", ";

                    // suppress filtering & popup while we programmatically update text
                    suppressPopup[0] = true;
                    try {
                        // IMPORTANT: pass false to avoid triggering AutoCompleteView filter
                        autoCompleteTags.setText(newText, false);
                        autoCompleteTags.setSelection(newText.length());
                    } catch (Exception ignored) {
                    }

                    // longer delay to ensure TextWatcher and popup logic have steadied
                    autoCompleteTags.postDelayed(() -> {
                        try {
                            suppressPopup[0] = false;
                        } catch (Exception ignored) {
                        }
                    }, 150); // 150ms instead of 50ms
                } catch (Exception ignored) {
                }
            };

            final java.util.function.Function<Void, String> buildTextFromSelected = (v) -> {
                try {
                    List<TagItem> sel = popupAdapter.getSelectedItems();
                    if (sel == null || sel.isEmpty()) return "";
                    StringBuilder sb = new StringBuilder();
                    for (TagItem ti : sel) {
                        sb.append(ti.getTagId()).append(", ");
                    }
                    if (sb.length() > 2) sb.setLength(sb.length() - 2); // remove trailing ", "
                    return sb.toString();
                } catch (Exception ignored) {
                    return "";
                }
            };

            // set up TextInputLayout clear icon and its behavior
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
                // initial visibility from adapter
                try {
                    til.setEndIconVisible(!popupAdapter.getSelectedItems().isEmpty());
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }

            // show popup when clicked or focused (respect suppress flag)
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

            // filter popup as user types (only filter by last token)
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

                        // filter adapter by last token
                        popupAdapter.getFilter().filter(last);

                        // show popup if last token non-empty or even if empty to show all suggestions
                        if (!popup.isShowing()) popup.show();

                        // update end icon visibility
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

            // when a user clicks an item in popup -> toggle selection and bind text immediately
            popup.setOnItemClickListener((parent, v, position, id) -> {
                try {
                    Object obj = popupAdapter.getItem(position);
                    if (!(obj instanceof TagItem)) return;
                    TagItem chosen = (TagItem) obj;

                    // always toggle (select OR unselect) so clicking toggles state
                    popupAdapter.toggleSelection(chosen);

                    // refresh adapter visuals (checkboxes)
                    popupAdapter.notifyDataSetChanged();

                    // keep popup visible to allow more selections (optional)
                    if (!popup.isShowing()) popup.show();
                } catch (Exception ignored) {
                }
            });

            // Add button: get entire AutoComplete text (comma separated tokens)
            btnAddTag.setOnClickListener(v -> {
                try {
                    String current = autoCompleteTags.getText() == null ? "" : autoCompleteTags.getText().toString();
                    android.widget.Toast.makeText(requireContext(), "AutoComplete text: " + current, android.widget.Toast.LENGTH_SHORT).show();
                    if (popup.isShowing()) popup.dismiss();

                    filePicker.launchPicker();

                } catch (Exception ignored) {
                }
            });

            // onDismiss: synchronize the text with adapter selections but keep selections intact
            popup.setOnDismissListener(() -> {
                try {
                    // if user selected via popup, adapter already updated; but ensure the text reflects adapter selections
                    String built = buildTextFromSelected.apply(null);
                    if (!built.isEmpty()) {
                        // suppress while updating
                        suppressPopup[0] = true;
                        try {
                            String toSet = built + (built.endsWith(",") ? " " : ", ");
                            autoCompleteTags.setText(toSet, false); // avoid triggering filter
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
                        // if nothing selected, leave whatever user typed (so they can continue editing)
                        try {
                            til.setEndIconVisible(!android.text.TextUtils.isEmpty(autoCompleteTags.getText()));
                        } catch (Exception ignored) {
                        }
                    }

                    // refresh adapter UI so next open shows the right check states
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

    public List<String> getFilterTagList(String tags) {
        List<String> filterTagId = new ArrayList<>();

        try {
            if (tags != null && !tags.trim().isEmpty()) {

                String[] items = tags.split(",");

                for (String t : items) {
                    filterTagId.add(t.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filterTagId;
    }


    private void insertTestData(String taskName) {
        exec.execute(() -> {
            try {


                String dateTime = ModuleViewModel.getDateTime();

                List<Task> list = new ArrayList<>();

                String input = autoCompleteTags.getText().toString().trim();

                if (input.isEmpty()) {
                    SnackbarUtils.show(requireView(), "No Tag Selected!");
                    return;
                }

                List<String> tagList = Arrays.stream(input.split(","))
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .collect(Collectors.toList());


                int uid = generateUid(tagList);
                // loop through each tag
                for (String tagId : tagList) {

                    if (tagId == null) continue;

                    tagId = tagId.trim();  // remove spaces

                    if (tagId.isEmpty()) continue; // skip empty entries

                    list.add(new Task(
                            0,           // id
                            taskName,       // title
                            tagList.size(), // tagCount
                            tagId,          // tagId
                            uid,            // boxId? or user id?
                            0,              // tagFound
                            "",             // zoneId
                            "",             // moduleId
                            0,              // rssValue
                            0,              // taskComplete
                            dateTime ,new ArrayList<>()       // dateTime
                    ));
                }

                int inserted = taskDbHelper.insertListTasks(list);

                requireActivity().runOnUiThread(() -> {
                    if (inserted > 0) {
                        SnackbarUtils.show(requireView(), "Task Created Successfully");
                        ResetUi();
                    } else
                        SnackbarUtils.show(requireView(), "Failed to Create Task");
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    SnackbarUtils.show(requireView(), "Failed to insert");
                });
            }
        });
    }

    public String getJoinedTags(List<String> addedTags) {
        try {
            return android.text.TextUtils.join(",", addedTags);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public int generateUid(List<?> addedTags) {
        try {
            int count = (addedTags == null) ? 0 : addedTags.size();

            long time = System.currentTimeMillis(); // always positive

            String raw = count + String.valueOf(time);  // example: "3" + "1736542342345"

            long uid = Long.parseLong(raw);

            return (int) (uid % Integer.MAX_VALUE); // always positive
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    void ResetUi() {
        try {
            etTaskName.setText("Task_" + new java.text.SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

            autoCompleteTags.setText("");

        } catch (Exception e) {

        }
    }

    private void saveNewTask() {
        exec.execute(() -> {
            try {
                Task t = new Task(
                        0,
                        "My New Task",
                        5,
                        "TAG123",
                        0,
                        -65.0,
                        "Z1",
                        "A",
                        0,
                        0, ModuleViewModel.getDateTime(),new ArrayList<>()
                );

                long id = taskDbHelper.insertTask(t);

                requireActivity().runOnUiThread(() -> {
                    if (id > 0) {
                        SnackbarUtils.show(requireView(), "Inserted ID: " + id);
                        // reload list if needed
                    } else {
                        SnackbarUtils.show(requireView(), "Insert failed!");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private int dpToPx(int dp) {
        try {
            float density = requireContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        } catch (Throwable t) {
            return dp;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // shutdown executor (same behavior you had in Activity.onDestroy)
        try {
            if (exec != null) {
                exec.shutdownNow();
                exec = null;
            }
        } catch (Exception e) {
            Log.e("TAG", "executor shutdown error", e);
        }

        // optional: release DB helper resources if you added a release method
        // (your TaskDbHelper doesn't need explicit close since it opens/closes each operation)
        taskDbHelper = null;
    }
}
