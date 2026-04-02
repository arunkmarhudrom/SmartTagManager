package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.grf.smarttagmanager.R;
import com.grf.viewmodel.ModuleViewModel;

public class CommonFragment extends Fragment {

    private static final String TAG = "CommonFragment";
    private static final String ARG_MODULE = "module";
    private static final String ZONE_ID = "zoneId";

    private ModuleViewModel viewModel;
    private TextView tvTitle, tvContent;

    public static CommonFragment newInstance(String module) {
        CommonFragment fragment = new CommonFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODULE, module);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_common, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView error", e);
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            tvTitle = view.findViewById(R.id.tvModuleTitle);
            tvContent = view.findViewById(R.id.tvModuleContent);

            String module = getArguments() != null ? getArguments().getString(ARG_MODULE, "A") : "A";
            int zoneId = getArguments() != null ? getArguments().getInt(ZONE_ID, 0) : 0;

            tvTitle.setText("Module " + module);

            viewModel = new ViewModelProvider(this).get(ModuleViewModel.class);
            viewModel.initDb(requireContext());

            viewModel.BindModuleData(view, viewModel, requireContext(), getViewLifecycleOwner(), module,zoneId);

            viewModel.getText().observe(getViewLifecycleOwner(), s -> {
                try {
                    tvContent.setText(s);
                } catch (Exception ex) {
                    Log.e(TAG, "UI update error", ex);
                }
            });

            ImageView back = view.findViewById(R.id.ivBack);
            back.setOnClickListener(v -> {
                try {
                    requireActivity().onBackPressed();
                } catch (Exception e) {
                    Log.e(TAG, "Back error", e);
                }
            });

            viewModel.setText("Welcome to Module " + module);

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error", e);
        }
    }
}