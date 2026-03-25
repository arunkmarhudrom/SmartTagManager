package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.grf.adapter.TaskAdapter;
import com.grf.model.Task;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.uhfmanager.UhfManagerHelper;
import com.grf.utils.ModuleUiBinder;
import com.grf.utils.OnKeyPressHandler;
import com.grf.viewmodel.ModuleViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModuleAFragment extends Fragment implements OnKeyPressHandler {
    private static final String TAG = "ModuleAFragment";
    private ModuleViewModel viewModel;
    private TextView tvTitle, tvContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_module_a, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView error", e);
            return new View(requireContext());
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            tvTitle = view.findViewById(R.id.tvModuleTitleA);
            tvContent = view.findViewById(R.id.tvModuleContentA);

            // use activity-scoped VM so other fragments/binders use same instance
            ModuleViewModel vm = new ViewModelProvider(requireActivity()).get(ModuleViewModel.class);

            vm.initDb(requireContext()); // initialize DB with application context

            vm.BindModuleData(view, vm, requireContext(), getViewLifecycleOwner(), "A");

            ImageView back = view.findViewById(R.id.ivBack);
            back.setOnClickListener(v -> {
                try {
                    requireActivity().onBackPressed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error", e);
        }
    }

    @Override
    public boolean onKeyDownEvent(int keyCode, KeyEvent event) {
        try {

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // handle down press

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // not consumed
    }

    @Override
    public boolean onKeyUpEvent(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}