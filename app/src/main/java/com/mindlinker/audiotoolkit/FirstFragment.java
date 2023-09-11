package com.mindlinker.audiotoolkit;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mindlinker.audiotoolkit.AudioModule.AudioModuleManager;
import com.mindlinker.audiotoolkit.common.WaveFile;
import com.mindlinker.audiotoolkit.databinding.FragmentFirstBinding;

import java.util.List;
import java.util.Objects;

public class FirstFragment extends Fragment {
    private final static String TAG = "FirstFragment";

    private Context context;
    private FragmentFirstBinding binding;
    private AudioBroadcastReceiver audioBroadcastReceiver;
    private ActivityResultLauncher<String[]> chooseWavLauncher;

    private Spinner mAudioModeSpinner = null;
    private Spinner mRecordSourceSpinner = null;
    private Spinner mRecordSampleSpinner = null;
    private Spinner mRecordChannelSpinner = null;
    private Spinner mShareSampleSpinner = null;
    private Spinner mShareChannelSpinner = null;
    private Spinner mShareUsageSpinner = null;
    private Spinner mBuildIn3ASpinner = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode") private Switch mRecordSwitch = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode") private Switch mShareSwitch = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode") private Switch mPlaySwitch = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode") private Switch mMicMuteSwitch = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode") private Switch mScoStateSwitch = null;

    private AudioModuleManager mAudioModule = null;
    private boolean needRecord = false;
    private boolean needShare = false;
    private boolean needPlay = false;
    private boolean isRecordSetReady = false;
    private boolean isPlaySetReady = false;
    private boolean isShareSetReady = false;
    private boolean isScreenRequestEnd = false;
    private boolean isStarting = false;
    private boolean playAssetFile = true;
    private String externalWavPath;

    private ActivityResultLauncher<Intent> startScreenLaunch;

    private final Handler handler = new Handler();
    private final Runnable shareDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScreenRequestEnd) {
                if (isShareSetReady) {
                    runAudioTest();
                }
                isShareSetReady = false;
                isScreenRequestEnd = false;
            } else {
                handler.postDelayed(this, 1000);
                // check screen capture permission every 1 second
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化context
        context = MainActivity.getContext();

        externalWavPath = context.getString(R.string.user_player_Corpus_default);
        chooseWavLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            String wavPath = UriUtil.getPath(context, result);
                            if (WaveFile.isWavFileExist(wavPath)) {
                                externalWavPath = wavPath;
                                playAssetFile = false;
                            } else {
                                MainActivity.showToast("选择了无效的wav文件");
                            }
                            binding.UserCorpusTitleText.setText(externalWavPath);
                        } else {
                            Log.i(TAG, "Uri is null, pathString=" + externalWavPath);
                        }
                    }
                });

        startScreenLaunch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Log.i(TAG, "ActivityResult=" + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    //获得录屏权限，启动Service进行录制
                    setShareParameter(result.getData());
                    isShareSetReady = true;
                } else {
                    isShareSetReady = false;
                    MainActivity.showToast("权限申请失败，无法共享音频。");
                }
                isScreenRequestEnd = true;
            }
        });
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        MainActivity.preMenuOrder = 0;
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化组件
        mAudioModeSpinner = getView().findViewById(R.id.audioModeSpinner);
        mAudioModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mRecordSourceSpinner = getView().findViewById(R.id.audioSourceSpinner);
        mRecordSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mRecordSampleSpinner = getView().findViewById(R.id.sampleRateSpinner);
        mRecordSampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mRecordChannelSpinner = getView().findViewById(R.id.channelCountSpinner);
        mRecordChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mShareSampleSpinner = getView().findViewById(R.id.shareSampleSpinner);
        mShareSampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mShareChannelSpinner = getView().findViewById(R.id.shareChannelSpinner);
        mShareChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mShareUsageSpinner = getView().findViewById(R.id.shareUsageSpinner);
        mShareUsageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mBuildIn3ASpinner = getView().findViewById(R.id.buildIn3ASpinner);
        mBuildIn3ASpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mAudioModule.getRecordState()) {
                    switch (position) {
                        case 1:
                            mAudioModule.setEffectEnable(true);
                            break;
                        case 2:
                            mAudioModule.setEffectEnable(false);
                            break;
                        default:
                            break;
                    }

                    update3AState(mAudioModule.isEffectEnable());
                } else {
                    Log.d(TAG, "recording state error.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.playerFileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                playAssetFile = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.UserCorpusTitleText.setText(context.getString(R.string.user_player_Corpus_default));
        binding.UserCorpusTitleText.setOnClickListener(view2 -> {
            String[] fileType = {"*/*"};
            chooseWavLauncher.launch(fileType);
        });

        mRecordSwitch = getView().findViewById(R.id.recordSwitch);
        mShareSwitch = getView().findViewById(R.id.shareSwitch);
        mPlaySwitch = getView().findViewById(R.id.playerSwitch);
        mMicMuteSwitch = getView().findViewById(R.id.micMuteSwitch);
        mScoStateSwitch = getView().findViewById(R.id.sco_switch);

        binding.buttonAudioTest.setText("开始测试");
        binding.buttonAudioTest.setOnClickListener(view1 -> {
            if (isStarting) {
                stopAudioTest();
            } else {
                startAudioTest();
            }
        });

        // 初始化UI显示
        binding.audioRecordPathText.setText("录音文件：" + MainActivity.getDumpPath());
        mAudioModeSpinner.setSelection(0);     // MODE_NORMAL
        mRecordSourceSpinner.setSelection(1);  // MIC
        mRecordSampleSpinner.setSelection(1);  // 16000
        mRecordChannelSpinner.setSelection(0); // MONO
        mShareSampleSpinner.setSelection(1);   // 16000
        mShareChannelSpinner.setSelection(0);  // 1
        mShareUsageSpinner.setSelection(1);    // MEDIA
        mBuildIn3ASpinner.setSelection(0);    // default
        binding.audioTrackSwitch.setChecked(false); // not use media player
        mRecordSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> needRecord = isChecked);
        needRecord = mRecordSwitch.isChecked();

        mShareSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> needShare = isChecked);
        needShare = mShareSwitch.isChecked();

        mPlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> needPlay = isChecked);
        needPlay = mPlaySwitch.isChecked();

        mAudioModule = AudioModuleManager.getInstance();

        mMicMuteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAudioModule.setMicMute(isChecked);
            mMicMuteSwitch.setChecked(mAudioModule.isMicMute());
        });

        mScoStateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "switch SCO from " + !isChecked + " to " + isChecked);
            mAudioModule.switchBtScoState();
            mScoStateSwitch.setChecked(mAudioModule.isBtScoOn());
        });
        mScoStateSwitch.setChecked(mAudioModule.isBtScoOn());

        updateVolume();
        updatePhoneState();
        updateSpeakerList();
        updateMicMuteState();

        binding.audioMicListText.setText("麦克风：" + mAudioModule.micInfo());
        binding.audioMicListText.setOnClickListener(view2 -> {
            binding.audioMicListText.setText("麦克风：" + mAudioModule.micInfo());
        });
        binding.audioSpeakerListText.setText("扬声器：" + mAudioModule.spkInfo());
        binding.audioSpeakerListText.setOnClickListener(view3 -> {
            binding.audioSpeakerListText.setText("扬声器：" + mAudioModule.spkInfo());
        });

        audioBroadcastReceiver = new AudioBroadcastReceiver();
        IntentFilter volumeFilter = new IntentFilter();
        // Register receiver for change about VOLUME_CHANGED_ACTION.
        volumeFilter.addAction(AudioBroadcastReceiver.VOLUME_CHANGED_ACTION);
        Objects.requireNonNull(getActivity()).registerReceiver(audioBroadcastReceiver, volumeFilter);

        // 设置测试状态
        isStarting = false;
    }

    private void setParameterForTest() {
        mAudioModule.setFlagAndMode(needRecord, needShare, needPlay, mAudioModeSpinner.getSelectedItemPosition());
        setRecordParameter();
        setPlayParameter();

        // Share parameters need set by ScreenCapture permission
        if (needShare) {
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            //Returns an Intent that must passed to startActivityForResult() in order to start screen capture.
            Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
            startScreenLaunch.launch(permissionIntent);
        }
    }

    private void startAudioTest() {
        if (isStarting) {
            MainActivity.showToast("启动新的测试，请停止当前进行中的测试");
            return;
        }

        if (!(needRecord | needShare | needPlay)) {
            MainActivity.showToast("请选择测试项：录音、共享音频或播放");
            return;
        }

        setParameterForTest();

        if (!needShare) {
            runAudioTest();
        } else {
            shareDataRunnable.run();
        }
    }

    private void runAudioTest() {
        // 执行测试内容
        int startState = mAudioModule.startAll();
        switch (startState) {
            case -3:
                MainActivity.showToast("启动测试失败，请检测共享音频参数设置");
                break;
            case -2:
                MainActivity.showToast("启动测试失败，请检测录音参数设置");
                break;
            case -1:
                MainActivity.showToast("启动测试失败，请检测设备状态");
                break;
            case 0:
                MainActivity.showToast("测试进行中...");
                break;
            default:
                MainActivity.showToast("测试运行中");
                break;
        }

        if (startState >= 0) {
            // 更新UI
            isStarting = true;
            binding.buttonAudioTest.setText("停止测试");
        } else {
            isStarting = false;
            binding.buttonAudioTest.setText("开始测试");
        }
        update3AState(mAudioModule.isEffectEnable());
        updateVolume();
        updatePhoneState();
        updateSpeakerList();
    }

    private void stopAudioTest() {
        if (!isStarting) {
            MainActivity.showToast("当前无测试运行");
            return;
        }

        // 停止测试内容
        int stopState = mAudioModule.stopAll();
        if (stopState < 0) {
            MainActivity.showToast("停止测试时发生异常");
        }

        // 更新UI
        isRecordSetReady = false;
        isPlaySetReady = false;
        isStarting = false;
        update3AState(mAudioModule.isEffectEnable());
        MainActivity.showToast("测试结束");
        binding.buttonAudioTest.setText("开始测试");
    }

    private void setRecordParameter() {
        int source = mRecordSourceSpinner.getSelectedItemPosition();
        int sample = mRecordSampleSpinner.getSelectedItemPosition();
        int channel = mRecordChannelSpinner.getSelectedItemPosition() + 1;
        int effect = mBuildIn3ASpinner.getSelectedItemPosition();

        switch (effect) {
            case 1:
                mAudioModule.setEffectEnable(true);
                break;
            case 2:
                mAudioModule.setEffectEnable(false);
                break;
            default:
                break;
        }

        switch (sample) {
            case 0:
                sample = 8000;
                break;
            case 2:
                sample = 32000;
                break;
            case 3:
                sample = 44100;
                break;
            case 4:
                sample = 48000;
                break;
            case 5:
                sample = 96000;
                break;
            default:
                sample = 16000;
                break;
        }
        mAudioModule.setRecordParameter(source, sample, channel);
        isRecordSetReady = true;
    }

    private void setPlayParameter() {
        String fileName;
        if (playAssetFile) {
            int wavFileIndex = binding.playerFileSpinner.getSelectedItemPosition();
            switch (wavFileIndex) {
                case 1:
                // case 2:
                default:
                    fileName = "Eagles.Hotel_California.wav";
                    break;
            }
        } else {
            fileName = externalWavPath;
        }
        boolean useAudioTrack = !binding.audioTrackSwitch.isChecked();
        mAudioModule.setPlayerParameter(fileName, playAssetFile, useAudioTrack);
        isPlaySetReady = true;
    }

    private void setShareParameter(Intent data) {
        if (needShare) {
            if (data == null) {
                Log.e(TAG, "can not get result data, please check.");
                return;
            }

            int sampleRate = mShareSampleSpinner.getSelectedItemPosition();
            switch (sampleRate) {
                case 0:
                    sampleRate = 8000;
                    break;
                case 2:
                    sampleRate = 32000;
                    break;
                case 3:
                    sampleRate = 44100;
                    break;
                case 4:
                    sampleRate = 48000;
                    break;
                case 5:
                    sampleRate = 96000;
                    break;
                default:
                    sampleRate = 16000;
                    break;
            }
            int channelCount = mShareChannelSpinner.getSelectedItemPosition() + 1;
            int usage = mShareUsageSpinner.getSelectedItemPosition();
            switch (usage) {
                case 0:
                    usage = AudioAttributes.USAGE_GAME;
                    break;
                case 2:
                    // AudioAttributes.USAGE_VOICE_COMMUNICATION = 2;
                    break;
                default:
                    usage = AudioAttributes.USAGE_MEDIA;
                    break;
            }
            mAudioModule.setShareParameters(sampleRate, channelCount, usage, data);
        }
    }

    @SuppressLint("SetTextI18n")
    public void update3AState(boolean on) {
        binding.audio3AText.setText("内置3A状态：" + (on ? "on" : "off"));
    }

    @SuppressLint("SetTextI18n")
    public void updateVolume() {
        binding.audioVolumeText.setText("Volume:" + mAudioModule.getVolumeInfo());
    }

    @SuppressLint("SetTextI18n")
    public void updatePhoneState() {
        binding.audioCallText.setText("通话状态:" + mAudioModule.getPhoneState());
    }

    @SuppressLint("SetTextI18n")
    public void updateMicMuteState() {
        mMicMuteSwitch.setChecked(mAudioModule.isMicMute());
    }

    public void updateSpeakerList() {
        List<String> speakerArray = mAudioModule.listSpeaker();
        ArrayAdapter<String>  arrAdapterSource = new ArrayAdapter<>(MainActivity.getContext(), android.R.layout.simple_spinner_item, speakerArray);
        arrAdapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.playerDeviceSpinner.setAdapter(arrAdapterSource);
        // TODO: need update current speaker
        // mAudioModule.getCurrentSpeaker();
        // binding.playerDeviceSpinner.setSelection();
    }

    @Override
    public void onDestroyView() {
        Objects.requireNonNull(getActivity()).unregisterReceiver(audioBroadcastReceiver);
        super.onDestroyView();
        binding = null;
    }

    // Intent broadcast receiver which handles changes about system stream value.
    private class AudioBroadcastReceiver extends BroadcastReceiver {
        private static final String VolumeTAG = "VolumeBroadcastReceiver";
        private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
        private static final String MICROPHONE_MUTE_CHANGED = "android.media.action.MICROPHONE_MUTE_CHANGED";

        AudioBroadcastReceiver() {}

        @SuppressLint("SwitchIntDef")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                Log.e(VolumeTAG, "action is null while BroadcastReceiver from Volume.");
                return;
            }

            if (VOLUME_CHANGED_ACTION.equals(action)) {
                updateVolume();
            } else if (MICROPHONE_MUTE_CHANGED.equals(action)) {
                updateMicMuteState();
            }
        }
    }
}