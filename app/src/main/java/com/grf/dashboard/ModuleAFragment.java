package com.grf.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
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

import com.grf.adapter.DashboardAdapter;
import com.grf.adapter.TaskAdapter;
import com.grf.api.ApiHelper;
import com.grf.helper.LoaderUtil;
import com.grf.model.ModuleItem;
import com.grf.model.Task;
import com.grf.smarttagmanager.MainActivity;
import com.grf.smarttagmanager.R;
import com.grf.smarttagmanager.databinding.FragmentDashboardBinding;
import com.grf.smarttagmanager.databinding.FragmentModuleABinding;
import com.grf.uhfmanager.UhfManagerHelper;
import com.grf.utils.ModuleUiBinder;
import com.grf.utils.OnKeyPressHandler;
import com.grf.utils.SnackbarUtils;
import com.grf.viewmodel.ModuleViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModuleAFragment extends Fragment implements OnKeyPressHandler {
    private static final String TAG = "ModuleAFragment";
    private ModuleViewModel viewModel;
    private TextView tvTitle, tvContent;
    FragmentModuleABinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentModuleABinding.inflate(inflater, container, false);
            return binding.getRoot();
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


            ApiHelper.get(requireContext(), "get-pending-task", new ApiHelper.ApiCallback() {

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

                                    LoaderUtil.hide();


                                    vm.BindModuleData(view, vm, requireContext(), getViewLifecycleOwner(), "A",0);

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