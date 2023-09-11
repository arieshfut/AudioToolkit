package com.mindlinker.audiotoolkit;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mindlinker.audiotoolkit.AcousticModule.AcousticManager;
import com.mindlinker.audiotoolkit.databinding.FragmentThirdBinding;

public class ThirdFragment extends Fragment {
    private static final String TAG = "ThirdFragment";

    private Context context;
    private AcousticManager acousticManager;
    private FragmentThirdBinding binding;
    private String openFilePath;
    private ActivityResultLauncher<String[]> chooseFileLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MainActivity.getContext();
        openFilePath = context.getString(R.string.audioEffect_User_Corpus_default);
        chooseFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            openFilePath = UriUtil.getPath(context, result);
                            Log.i(TAG, "pathString=" + openFilePath + ", uri=" + result);
                        } else {
                            Log.i(TAG, "pathString=" + openFilePath);
                        }
                        binding.UserCorpusTitleText.setText(openFilePath);
                    }
                });
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        MainActivity.preMenuOrder = 2;
        binding = FragmentThirdBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = MainActivity.getContext();
        acousticManager = AcousticManager.getInstance();

        binding.acousticCorpusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.buttonUserCorpus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] fileType = {"*/*"};
                chooseFileLauncher.launch(fileType);
            }
        });

        binding.buttonAcousticMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.showToast("该功能建设中");
            }
        });

        binding.buttonAecTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.showToast("该功能建设中");
            }
        });
    }

    public void releaseSource() {
        // must release source
        acousticManager = null;
        context = null;
    }

    @Override
    public void onDestroyView() {
        releaseSource();

        super.onDestroyView();
        binding = null;
    }

}