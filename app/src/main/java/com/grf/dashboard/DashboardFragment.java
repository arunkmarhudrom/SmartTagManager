package com.grf.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.grf.adapter.DashboardAdapter;
import com.grf.model.ModuleItem;
import com.grf.smarttagmanager.LoginActivity;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.utils.PopupUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_dashboard, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView error", e);
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            // Header views
//            ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
            TextView tvLoggedIn = view.findViewById(R.id.tvLoggedIn);
            Button btnManage = view.findViewById(R.id.btnManageTasks);
            Button btnReports = view.findViewById(R.id.btnReports);
            Button btnLogout = view.findViewById(R.id.btnLogout);

            // Set email dynamically if needed; hardcoded here for demo
            try {
                tvLoggedIn.setText("Logged in as\n" + "prasunbasu16@gmail.com");
            } catch (Exception ex) {
                Log.e(TAG, "set email error", ex);
            }

            // header button listeners
            btnManage.setOnClickListener(v -> {
                try {

                    Navigation.findNavController(v).navigate(R.id.action_dashboard_to_manageTaskFragment);

                } catch (Exception e) {
                    Log.e(TAG, "ManageTasks click error", e);
                }
            });

            btnReports.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(v).navigate(R.id.action_dashboard_to_reportFragment);

                } catch (Exception e) {
                    Log.e(TAG, "Reports click error", e);
                }
            });

            btnLogout.setOnClickListener(v -> {
                try {

                    PopupUtils.showCustomYesNoDialog(
                            requireContext(),
                            "Logout?",
                            "Are you sure you want to logout?",
                            new PopupUtils.PopupCallback() {
                                @Override
                                public void onYes() {
                                    //  requireActivity().finish();
                                    try {
                                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                        startActivity(intent);
                                        requireActivity().finish(); // closes MainActivity
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void onNo() {
                                    // your no action
                                }
                                @Override
                                public void onCLose() {
                                    // no-op
                                }
                            }
                    );


                } catch (Exception e) {
                    Log.e(TAG, "Logout click error", e);
                }
            });

            // RecyclerView grid
            RecyclerView rv = view.findViewById(R.id.rvModules);
            GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
            rv.setLayoutManager(glm);


            final List<ModuleItem> items = new ArrayList<>();

            items.add(new ModuleItem("Zone A", R.id.action_dashboard_to_moduleA,
                    "Tap to view tasks",
                    new int[]{0xFFFF6B6B, 0xFFFF8E53}, 0));

            items.add(new ModuleItem("Zone B", R.id.action_dashboard_to_moduleB,
                    "Tap to view tasks",
                    new int[]{0xFFFFCA28, 0xFFFFA726}, 0));

            items.add(new ModuleItem("Zone C", R.id.action_dashboard_to_moduleC,
                    "Tap to view tasks",
                    new int[]{0xFF66BB6A, 0xFF43A047}, 0));

            items.add(new ModuleItem("Zone D", R.id.action_dashboard_to_moduleD,
                    "Tap to view tasks",
                    new int[]{0xFF29B6F6, 0xFF0288D1}, 0));

            items.add(new ModuleItem("Zone E", R.id.action_dashboard_to_moduleE,
                    "Tap to view tasks",
                    new int[]{0xFFAB47BC, 0xFF8E24AA}, 0));

            items.add(new ModuleItem("Zone F", R.id.action_dashboard_to_moduleF,
                    "Tap to view tasks",
                    new int[]{0xFFFF7043, 0xFFF4511E}, 0));

            DashboardAdapter adapter = new DashboardAdapter(items, actionId -> {
                try {
                    Navigation.findNavController(view).navigate(actionId);
                } catch (Exception e) {
                    Log.e(TAG, "Navigation error", e);
                }
            });

            rv.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error", e);
        }
    }
}