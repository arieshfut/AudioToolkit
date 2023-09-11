package com.mindlinker.audiotoolkit;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.mindlinker.audiotoolkit.databinding.ActivityMainBinding;

import android.os.Environment;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public final static boolean needAudioTest = BuildConfig.NEED_AUDIO_TEST;

    private static Context context;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static int preMenuOrder = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化context
        context = getApplicationContext();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // request Audio Permissions
        requestAppPermissions();
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
                            .navigate(R.id.action_FirstFragment_to_SecondFragment);
                        preMenuOrder = order;
                    } else if (order == 2) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_FirstFragment_to_ThirdFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_FirstFragment_to_FourthFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 1:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                        preMenuOrder = order;
                    } else if (order == 2) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_SecondFragment_to_ThirdFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_SecondFragment_to_FourthFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 2:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_ThirdFragment_to_FirstFragment);
                        preMenuOrder = order;
                    } else if (order == 1) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_ThirdFragment_to_SecondFragment);
                        preMenuOrder = order;
                    } else if (order == 3) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_ThirdFragment_to_FourthFragment);
                        preMenuOrder = order;
                    } else {
                        showAbout();
                    }
                    break;
                case 3:
                    if (order == 0) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_FourthFragment_to_FirstFragment);
                        preMenuOrder = order;
                    } else if (order == 1) { Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_FourthFragment_to_SecondFragment);
                        preMenuOrder = order;
                    } else if (order == 2) {
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                .navigate(R.id.action_FourthFragment_to_ThirdFragment);
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

    public static String getDumpPath() {
        String filePath = "";
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            filePath = Environment.getExternalStorageDirectory().toString();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            filePath = Objects.requireNonNull(MainActivity.getContext().getExternalFilesDir(null)).getAbsolutePath();
        }

        if (TextUtils.isEmpty(filePath)) {
            filePath = Environment.getExternalStorageDirectory().toString();
        }

        Log.d(TAG, "getDumpPath: " + filePath);
        return filePath;
    }

    private void requestAppPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        // API Level >= 23
        permissions.addAll(Arrays.asList(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE
        ));

        // API Level >= 29
        // Replace 29 with 'Build.VERSION_CODES.Q' after Android SDK upgraded
        if (Build.VERSION.SDK_INT >= 29) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        // API Level >= 31
        // Replace 31 with 'Build.VERSION_CODES.S' after Android SDK upgraded
        if (Build.VERSION.SDK_INT >= 31) {
            permissions.addAll(Arrays.asList(
                    "android.permission.BLUETOOTH_SCAN", 	// Manifest.permission.BLUETOOTH_SCAN
                    "android.permission.BLUETOOTH_CONNECT"	// Manifest.permission.BLUETOOTH_CONNECT
            ));
        }

        String[] permissionsArray = new String[permissions.size()];
        for (int i = 0; i < permissions.size(); ++i) {
            permissionsArray[i] = permissions.get(i);
        }
        ActivityCompat.requestPermissions(this, permissionsArray, 1);
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