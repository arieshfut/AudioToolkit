package com.aries.audiotoolkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;


public class AudioInfoFragment extends Fragment {
    private static final String TAG = "AudioInfoFragment";

    private Context context;

    private View permissionLayout;
    private TextView permissionStatusText;
    private Button requestPermissionButton;
    private LinearLayout permissionListLayout;
    private TextView versionText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MainActivity.getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.toolkit_about, container, false);

        permissionLayout = rootView.findViewById(R.id.permissionLayout);
        permissionStatusText = rootView.findViewById(R.id.permissionStatusText);
        requestPermissionButton = rootView.findViewById(R.id.requestPermissionButton);
        permissionListLayout = rootView.findViewById(R.id.permissionListLayout);
        versionText = rootView.findViewById(R.id.version_text);

        return rootView;
    }

    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        versionText.setText("版本：" + BuildConfig.VERSION_NAME);
        updatePermissionUI();

        requestPermissionButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                if (MainActivity.hasAllPermissions(context)) {
                    activity.navigateToTab(1);
                } else {
                    activity.requestAppPermissions();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void updatePermissionUI() {
        if (permissionStatusText == null) {
            return;
        }

        List<String[]> allPermissionList = MainActivity.getAllPermissionList();
        if (allPermissionList == null) {
            return;
        }

        Context ctx = getContext();
        if (ctx == null) {
            return;
        }

        boolean allGranted = MainActivity.hasAllPermissions(ctx);
        permissionStatusText.setText("全部权限已同意: " + (allGranted ? "是" : "否"));
        permissionStatusText.setTextColor(allGranted ? 0xFF2E7D32 : 0xFFE65100);

        requestPermissionButton.setText(allGranted ? "开始使用" : "申请权限");

        permissionListLayout.removeAllViews();
        permissionListLayout.setVisibility(View.VISIBLE);

        for (String[] entry : allPermissionList) {
            String name = entry[0];
            String perm = entry[1];
            boolean granted = ContextCompat.checkSelfPermission(ctx, perm)
                    == PackageManager.PERMISSION_GRANTED;

            TextView tv = new TextView(ctx);
            tv.setText(name + ": " + (granted ? "已同意" : "未同意"));
            tv.setTextColor(granted ? 0xFF2E7D32 : 0xFFD32F2F);
            tv.setTextSize(12);
            tv.setPadding(0, 2, 0, 2);
            permissionListLayout.addView(tv);
        }
    }

    public void releaseSource() {
        context = null;
    }

    @Override
    public void onDestroyView() {
        releaseSource();

        super.onDestroyView();
    }
}
