package com.grf.dashboard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.grf.smarttagmanager.R;
import com.grf.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddedTagsFragment extends Fragment {

    private final List<String> trayIds = new ArrayList<>();
    private TrayAdapter adapter;
    private TextView tvTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_added_tags, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotal = view.findViewById(R.id.tvTotal);
        ImageView ivBack = view.findViewById(R.id.ivBack);
        RecyclerView rvTrayList = view.findViewById(R.id.rvTrayList);

        ArrayList<String> incoming = getArguments() != null
                ? getArguments().getStringArrayList("tray_ids")
                : null;
        if (incoming != null) {
            trayIds.addAll(incoming);
        }

        adapter = new TrayAdapter(new ArrayList<>(trayIds), new TrayAdapter.Listener() {
            @Override
            public void onEdit(int position) {
                showEditDialog(position);
            }

            @Override
            public void onDelete(int position) {
                if (position >= 0 && position < trayIds.size()) {
                    trayIds.remove(position);
                    refresh();
                }
            }
        });

        rvTrayList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTrayList.setItemAnimator(null);
        rvTrayList.setAdapter(adapter);

        ivBack.setOnClickListener(v -> closeWithResult(v));
        refresh();
    }

    private void showEditDialog(int position) {
        try {
            if (position < 0 || position >= trayIds.size()) return;

            String oldValue = trayIds.get(position);
            final EditText input = new EditText(requireContext());
            input.setText(oldValue);
            input.setSelection(oldValue.length());

            new AlertDialog.Builder(requireContext())
                    .setTitle("Edit Tray ID")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String updated = normalize(input.getText() == null ? "" : input.getText().toString());
                        if (updated.isEmpty()) {
                            SnackbarUtils.show(requireView(), "Tray ID cannot be empty");
                            return;
                        }
                        if (!updated.equals(oldValue) && trayIds.contains(updated)) {
                            SnackbarUtils.show(requireView(), "Duplicate Tray ID is not allowed");
                            return;
                        }
                        trayIds.set(position, updated);
                        refresh();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception ignored) {
        }
    }

    private void refresh() {
        adapter.submit(new ArrayList<>(trayIds));
        tvTotal.setText("Total: " + trayIds.size());
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void closeWithResult(View view) {
        Bundle result = new Bundle();
        result.putStringArrayList("tray_ids", new ArrayList<>(trayIds));
        getParentFragmentManager().setFragmentResult("added_tags_result", result);
        Navigation.findNavController(view).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            Bundle result = new Bundle();
            result.putStringArrayList("tray_ids", new ArrayList<>(trayIds));
            getParentFragmentManager().setFragmentResult("added_tags_result", result);
        } catch (Exception ignored) {
        }
    }

    private static class TrayAdapter extends RecyclerView.Adapter<TrayAdapter.VH> {

        interface Listener {
            void onEdit(int position);
            void onDelete(int position);
        }

        private final List<String> items;
        private final Listener listener;

        TrayAdapter(List<String> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        void submit(List<String> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_added_tray, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tvTrayId.setText(items.get(position));
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(holder.getBindingAdapterPosition());
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(holder.getBindingAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
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
