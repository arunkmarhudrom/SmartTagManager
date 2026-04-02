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
import com.grf.api.ApiHelper;
import com.grf.helper.LoaderUtil;
import com.grf.helper.SharedPreferencesHelper;
import com.grf.helper.TokenManager;
import com.grf.model.ModuleItem;
import com.grf.smarttagmanager.LoginActivity;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.smarttagmanager.databinding.FragmentDashboardBinding;
import com.grf.utils.PopupUtils;
import com.grf.utils.SnackbarUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    FragmentDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            return binding.getRoot();
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
                SharedPreferencesHelper pre = new SharedPreferencesHelper(requireContext());
                String user = pre.getString("login_user");
                tvLoggedIn.setText("Logged in as\n" + (user != null ? user : "Guest"));
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
            LoaderUtil.show(requireContext(), "Fetching data..");
            ApiHelper.get(requireContext(), "zone/get-active", new ApiHelper.ApiCallback() {

                @Override
                public void onSuccess(int statusCode, String response) {
                    try {
                        if (statusCode == 200) {

                            JSONObject json = new JSONObject(response);

                            boolean success = json.optBoolean("success", false);
                            int apiStatusCode = json.optInt("statusCode", 0);

                            if (success && apiStatusCode == 200) {

                                JSONArray dataArray = json.optJSONArray("data");

                                if (dataArray != null) {

                                    items.clear();

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        try {
                                            JSONObject obj = dataArray.getJSONObject(i);

                                            int id = obj.optInt("id");
                                            String name = obj.optString("name");

                                            // 🔥 Map navigation dynamically
                                            // int actionId = getActionIdByName(name);

                                            // 🎨 optional color mapping
                                            int[] colors = getColorByIndex(i);

                                            items.add(new ModuleItem(
                                                    name,
                                                    0,
                                                    "Tap to view tasks",
                                                    colors,
                                                    id
                                            ));

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    LoaderUtil.hide();

                                    DashboardAdapter adapter = new DashboardAdapter(items, (position) -> {
                                        try {
                                            ModuleItem item = items.get(position);

                                            Bundle bundle = new Bundle();
                                            bundle.putString("module", item.title); // or item.module
                                            bundle.putInt("zoneId", item.count);

                                            Navigation.findNavController(view)
                                                    .navigate(R.id.action_dashboard_to_commonFragment, bundle);

                                        } catch (Exception e) {
                                            Log.e(TAG, "Navigation error", e);
                                        }
                                    });

                                    rv.setAdapter(adapter);

                                } else {
                                    LoaderUtil.hide();
                                    SnackbarUtils.show(binding.getRoot(), "No data found");
                                }

                            } else {
                                LoaderUtil.hide();
                                String message = json.optString("message", "Failed");
                                SnackbarUtils.show(binding.getRoot(), message);
                            }

                        } else {
                            LoaderUtil.hide();
                            SnackbarUtils.show(binding.getRoot(), "HTTP Error: " + statusCode);
                        }

                    } catch (Exception e) {
                        LoaderUtil.hide();
                        e.printStackTrace();
                        SnackbarUtils.show(binding.getRoot(), "Parsing error");
                    }
                }

                @Override
                public void onError(int statusCode, String error) {
                    LoaderUtil.hide();
                    SnackbarUtils.show(binding.getRoot(), statusCode == 401 ? "Unauthorized" : error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error", e);
        }
    }

    private int getActionIdByName(String name) {
        try {
            switch (name) {
                case "Zone A":
                    return R.id.action_dashboard_to_moduleA;
                case "Zone B":
                    return R.id.action_dashboard_to_moduleB;
                case "Zone C":
                    return R.id.action_dashboard_to_moduleC;
                case "Zone D":
                    return R.id.action_dashboard_to_moduleD;
                case "Zone E":
                    return R.id.action_dashboard_to_moduleE;
                case "Zone F":
                    return R.id.action_dashboard_to_moduleF;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int[] getColorByIndex(int i) {
        try {
            int[][] colors = {
                    {0xFFFF6B6B, 0xFFFF8E53},
                    {0xFFFFCA28, 0xFFFFA726},
                    {0xFF66BB6A, 0xFF43A047},
                    {0xFF29B6F6, 0xFF0288D1},
                    {0xFFAB47BC, 0xFF8E24AA},
                    {0xFFFF7043, 0xFFF4511E}
            };
            return colors[i % colors.length];
        } catch (Exception e) {
            e.printStackTrace();
            return new int[]{0xFFCCCCCC, 0xFF999999};
        }
    }
}