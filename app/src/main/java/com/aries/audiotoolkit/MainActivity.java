package com.aries.audiotoolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.aries.audiotoolkit.databinding.ActivityMainBinding;

import android.os.Environment;
import android.util.Log;
import android.view.View;
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
    private ActivityMainBinding binding;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private static List<String[]> sAllPermissionList = null;

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

        setupToolbarIcon();

        initAllPermission();

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("关于");
                    break;
                case 1:
                    tab.setText("基本功能");
                    break;
                case 2:
                    tab.setText("预研");
                    break;
                case 3:
                    tab.setText("声学");
                    break;
            }
        }).attach();

        // 默认展示tab
        if (allPermissionsGranted()) {
            viewPager.setCurrentItem(1, false); // 默认展示tab2: 基本功能
        } else {
            viewPager.setCurrentItem(0, false); // 默认展示tab1: 关于
        }

        // 拦截所有页面切换（滑动+Tab点击），在滚动完全停止后检查，避免平滑滚动中断导致的重复回调
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    if (!allPermissionsGranted() && viewPager.getCurrentItem() != 0) {
                        showToast("请您优先申请全部权限");
                        viewPager.setCurrentItem(0, false);
                    }
                }
            }
        });
    }

    private void setupToolbarIcon() {
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbarTitle.post(() -> {
            int size = binding.toolbarTitle.getHeight();
            if (size <= 0) return;

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, true);
            Bitmap circle = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(circle);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            float radius = size / 2f;
            canvas.drawCircle(radius, radius, radius, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(scaled, 0, 0, paint);

            binding.toolbarIcon.setImageBitmap(circle);
        });
    }

    private void initAllPermission() {
        if (sAllPermissionList != null) {
            return;
        }

        sAllPermissionList = new ArrayList<>();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            sAllPermissionList.add(new String[]{"写存储", Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }

        // API Level >= 23
        sAllPermissionList.add(new String[]{"相机", Manifest.permission.CAMERA});
        sAllPermissionList.add(new String[]{"录音", Manifest.permission.RECORD_AUDIO});
        sAllPermissionList.add(new String[]{"电话状态", Manifest.permission.READ_PHONE_STATE});

        // API Level >= 29
        if (Build.VERSION.SDK_INT >= 29) {
            sAllPermissionList.add(new String[]{"后台服务", Manifest.permission.FOREGROUND_SERVICE});
        }
        // API Level >= 31
        if (Build.VERSION.SDK_INT >= 31) {
            sAllPermissionList.add(new String[]{"蓝牙扫描", "android.permission.BLUETOOTH_SCAN"});
            sAllPermissionList.add(new String[]{"蓝牙连接", "android.permission.BLUETOOTH_CONNECT"});
        }
    }

    public static List<String[]> getAllPermissionList() {
        return sAllPermissionList;
    }

    public static boolean hasAllPermissions(Context ctx) {
        if (sAllPermissionList == null) return true;
        for (String[] entry : sAllPermissionList) {
            if (ContextCompat.checkSelfPermission(ctx, entry[1])
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean allPermissionsGranted() {
        return hasAllPermissions(this);
    }

    public void requestAppPermissions() {
        String[] allPermission = new String[sAllPermissionList.size()];
        for (int i = 0; i < sAllPermissionList.size(); ++i) {
            String[] entry = sAllPermissionList.get(i);
            allPermission[i] = entry[1];
        }
        ActivityCompat.requestPermissions(this, allPermission, 1);
    }

    public void navigateToTab(int position) {
        if (viewPager != null) {
            viewPager.setCurrentItem(position);
        }
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

            // 通知AudioInfoFragment更新权限UI
            notifyAudioInfoFragment();

            // 如果所有权限都已同意，切换到tab2
            if (allPermissionsGranted()) {
                viewPager.setCurrentItem(1);
            }
        }
    }

    private void notifyAudioInfoFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f instanceof AudioInfoFragment) {
                ((AudioInfoFragment) f).updatePermissionUI();
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

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new AudioInfoFragment();
                case 1:
                    return new AudioBasicFragment();
                case 2:
                    return new PreResearchFragment();
                case 3:
                    return new AcousticFragment();
                default:
                    return new AudioBasicFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
