package com.aries.audiotoolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.aries.audiotoolkit.databinding.ActivityMainBinding;

import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public final static boolean needAudioTest = BuildConfig.NEED_AUDIO_TEST;

    private static Context context;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static int preMenuOrder = 0;
    private boolean isNavReady = false;

    private LinearLayout permissionLayout;
    private TextView permissionStatusText;
    private Button requestPermissionButton;
    private LinearLayout permissionListLayout;

    private List<String[]> allPermissionList = null;

    // create dump file path
    public static String dumpPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化context
        context = getApplicationContext();

        requestLogPath();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        permissionLayout = findViewById(R.id.permissionLayout);
        permissionStatusText = findViewById(R.id.permissionStatusText);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        permissionListLayout = findViewById(R.id.permissionListLayout);

        initAllPermission();
        updatePermissionStatusUI();

        requestPermissionButton.setOnClickListener(v -> requestAppPermissions());

        if (allPermissionsGranted()) {
            setupNavigation();
        } else {
            findViewById(R.id.nav_host_fragment_content_main).setVisibility(View.GONE);
        }
    }

    private void setupNavigation() {
        Log.d(TAG, "setupNavigation start");
        isNavReady = true;
        permissionLayout.setVisibility(View.GONE);
        findViewById(R.id.nav_host_fragment_content_main).setVisibility(View.VISIBLE);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    private void initAllPermission() {
        if (allPermissionList != null) {
            return;
        }

        allPermissionList = new ArrayList<>();

        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            allPermissionList.add(new String[]{"读媒体图片", Manifest.permission.READ_MEDIA_IMAGES});
            allPermissionList.add(new String[]{"读媒体视频", Manifest.permission.READ_MEDIA_VIDEO});
            allPermissionList.add(new String[]{"读媒体音频", Manifest.permission.READ_MEDIA_AUDIO});
        } else {
            allPermissionList.add(new String[]{"读存储", Manifest.permission.READ_EXTERNAL_STORAGE});
            allPermissionList.add(new String[]{"写存储", Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }*/

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            allPermissionList.add(new String[]{"写存储", Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }

        // API Level >= 23
        allPermissionList.add(new String[]{"相机", Manifest.permission.CAMERA});
        allPermissionList.add(new String[]{"录音", Manifest.permission.RECORD_AUDIO});
        allPermissionList.add(new String[]{"电话状态", Manifest.permission.READ_PHONE_STATE});

        // API Level >= 29
        if (Build.VERSION.SDK_INT >= 29) {
            allPermissionList.add(new String[]{"后台服务", Manifest.permission.FOREGROUND_SERVICE});
        }
        // API Level >= 31
        if (Build.VERSION.SDK_INT >= 31) {
            allPermissionList.add(new String[]{"蓝牙扫描", "android.permission.BLUETOOTH_SCAN"});
            allPermissionList.add(new String[]{"蓝牙连接", "android.permission.BLUETOOTH_CONNECT"});
        }
    }

    @SuppressLint("SetTextI18n")
    private void updatePermissionStatusUI() {
        boolean allGranted = allPermissionsGranted();
        permissionStatusText.setText("全部权限已同意: " + (allGranted ? "是" : "否"));
        permissionStatusText.setTextColor(allGranted ? 0xFF2E7D32 : 0xFFE65100);

        permissionListLayout.removeAllViews();
        permissionListLayout.setVisibility(allGranted ? View.GONE : View.VISIBLE);

        if (!allGranted) {
            for (String[] entry : allPermissionList) {
                String name = entry[0];
                String perm = entry[1];
                boolean granted = ContextCompat.checkSelfPermission(this, perm)
                        == PackageManager.PERMISSION_GRANTED;

                TextView tv = new TextView(this);
                tv.setText(name + ": " + (granted ? "已同意" : "未同意"));
                tv.setTextColor(granted ? 0xFF2E7D32 : 0xFFD32F2F);
                tv.setTextSize(12);
                tv.setPadding(0, 2, 0, 2);
                permissionListLayout.addView(tv);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int order = item.getOrder();

        //noinspection SimplifiableIfStatement
        if (preMenuOrder != order) {
            switch (preMenuOrder) {
                case 0:
                    if (order == 1) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AudioBasicFragment_to_PreResearchFragment);
                        preMenuOrder = order;
                    } else if (order == 2) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AudioBasicFragment_to_AcousticFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_AudioBasicFragment_to_AudioInfoFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 1:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_PreResearchFragment_to_AudioBasicFragment);
                        preMenuOrder = order;
                    } else if (order == 2) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_PreResearchFragment_to_AcousticFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_PreResearchFragment_to_AudioInfoFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 2:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AcousticFragment_to_AudioBasicFragment);
                        preMenuOrder = order;
                    } else if (order == 1) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AcousticFragment_to_PreResearchFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_AcousticFragment_to_AudioInfoFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 3:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AudioInfoFragment_to_AudioBasicFragment);
                        preMenuOrder = order;
                    } else if (order == 1) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_AudioInfoFragment_to_PreResearchFragment);
                        preMenuOrder = order;
                    } else if (order == 2) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_AudioInfoFragment_to_AcousticFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                default:
                    showAbout();
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void requestLogPath() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            String filePath = Environment.getExternalStorageDirectory().toString() + "/audiotoolkit";
            dumpPath = filePath;
            boolean flags = true;
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    flags = file.mkdirs();
                }
            } catch (Exception e) {
                Log.e(TAG, "create Dump dir=" + filePath + " with exception " + e);
                flags = false;
            }
            Log.i(TAG, "Create dir=" + flags + ", filePath=" + filePath);
        } else {
            dumpPath = Objects.requireNonNull(MainActivity.getContext().getExternalFilesDir(null)).getAbsolutePath();
        }
    }

    public static String getDumpPath() {
        Log.d(TAG, "getDumpPath: " + dumpPath);
        return dumpPath;
    }

    private void requestAppPermissions() {
        String[] allPermission = new String[allPermissionList.size()];
        for (int i = 0; i < allPermissionList.size(); ++i) {
            String[] entry = allPermissionList.get(i);
            allPermission[i] = entry[1];
        }
        ActivityCompat.requestPermissions(this, allPermission, 1);
    }

    private boolean allPermissionsGranted() {
        for (String[] entry : allPermissionList) {
            if (ContextCompat.checkSelfPermission(this, entry[1])
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.i(TAG, permissions[i] + (granted ? " granted" : " denied"));
            }

            updatePermissionStatusUI();

            if (!isNavReady && allPermissionsGranted()) {
                setupNavigation();
            }
        }
    }

    /**
     * 获取全局上下文
     */
    public static Context getContext() {
        return context;
    }

    public static void showToast(String text) {
        Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showLongToast(String text) {
        Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void showSnackBar(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    public static void showAbout() {
        String info = "音频工具箱\n";
        info += R.string.toolkit_version;
        showToast(info);
    }
}