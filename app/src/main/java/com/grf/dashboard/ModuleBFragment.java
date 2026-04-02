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

public class ModuleBFragment extends Fragment {
    private static final String TAG = "ModuleBFragment";
    private ModuleViewModel viewModel;
    private TextView tvTitle, tvContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_module_b, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView error", e);
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            tvTitle = view.findViewById(R.id.tvModuleTitleB);
            tvContent = view.findViewById(R.id.tvModuleContentB);

            viewModel = new ViewModelProvider(this).get(ModuleViewModel.class);
            viewModel.initDb(requireContext()); // initialize DB with application context
            viewModel.BindModuleData(view, viewModel, requireContext(), getViewLifecycleOwner(),"B",0);
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
                    e.printStackTrace();
                }
            });

            viewModel.setText("Welcome to Module B");
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error", e);
        }
    }
}