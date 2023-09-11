package com.aries.audiotoolkit.AudioModule;


import static androidx.core.content.ContextCompat.startForegroundService;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;

import java.util.List;


public class AudioModuleManager {
    private final static String TAG = "AudioModuleManager";

    private static AudioModuleManager instance;

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_STOP = STATE_DEFAULT;

    private AudioRecordThread audioRecord;
    private AudioPlayerThread audioPlayer;
    private AudioTrackThread audioTrack;
    private AudioEffectThread audioEffect;
    private BluetoothManager bluetoothManager;
    private AudioDeviceManager audioDevice;
    private AudioVolumeManager audioVolume;
    private AudioManager audioManager;

    private int state;
    private String information;
    /** flag indicate audio record/player/share need runing */
    private boolean mNeedRecord = false;
    private boolean mNeedShare = false;
    private boolean mNeedPlay = false;
    private int mAudioMode = AudioManager.MODE_NORMAL;

    // default recrod parameters
    private int recordSource = MediaRecorder.AudioSource.DEFAULT;
    private int recordSampleRate = 16000;
    private int recordChannel = 1;
    private boolean isRecording = false;

    // default share parameters
    private int shareSampleRate = 16000;
    private int shareChannel = 1;
    private int shareUsage = AudioAttributes.USAGE_MEDIA;
    private Intent shareData = null;
    private boolean isSharing = false;

    // default player parameters
    private String playerFle = null;
    private boolean isAssetWav = false;
    private boolean useAudioTrack = true;
    private boolean isPlaying = false;

    static {
        instance = new AudioModuleManager();
    }

    private AudioModuleManager() {
        audioManager = ((AudioManager) MainActivity.getContext().getSystemService(Context.AUDIO_SERVICE));
        audioRecord = new AudioRecordThread();
        audioPlayer = new AudioPlayerThread();
        audioTrack = new AudioTrackThread();
        audioEffect = AudioEffectThread.getInstance();
        bluetoothManager = new BluetoothManager(audioManager);
        audioDevice = new AudioDeviceManager(audioManager);
        audioVolume = new AudioVolumeManager(audioManager);
        information = "";

        audioDevice.start();
        audioVolume.start();
        state = STATE_DEFAULT;
    }

    public static AudioModuleManager getInstance() {
        return instance;
    }

    public void finalize() {
        audioDevice.stop();
        audioVolume.stop();
        Log.d(TAG, "destructor");
    }

    public void setFlagAndMode(boolean needRecord, boolean needShare, boolean needPlay, int mode) {
        mNeedRecord = needRecord;
        mNeedShare = needShare;
        mNeedPlay = needPlay;
        mAudioMode = mode;
    }

    public int setRecordParameter(int audioSource, int sample, int channel) {
        if (audioRecord.checkRecordParameter(audioSource, sample, channel) < 0) {
            return -1;
        }
        recordSource = audioSource;
        recordSampleRate = sample;
        recordChannel = channel;

        return 0;
    }

    public int setPlayerParameter(String assetFle, boolean isAssetFile, boolean needAudioTrack) {
        playerFle = assetFle;
        isAssetWav = isAssetFile;
        useAudioTrack = needAudioTrack;
        return 0;
    }

    public int setShareParameters(int sample, int channel, int usage, Intent data) {
        shareSampleRate = sample;
        shareChannel = channel;
        shareUsage = usage;
        shareData = data;
        return 0;
    }

    public boolean isEffectEnable() {
        return audioEffect.isEffectEnable();
    }
    public boolean setEffectEnable(boolean on) {
        return audioEffect.setEffectEnable(on);
    }

    public boolean isBtScoOn() { return bluetoothManager.isBtScoOn(); }
    public void switchBtScoState() { bluetoothManager.switchBtScoState(); }

    public void setMicMute(boolean mute) { audioManager.setMicrophoneMute(mute); }
    public boolean isMicMute() { return audioManager.isMicrophoneMute(); }

    public boolean getShareState() {
        return isSharing;
    }
    public boolean getPlayState() {
        return isPlaying;
    }
    public boolean getRecordState() {
        return isRecording;
    }

    public int startAll() {
        int result = 0;

        setAudioMode(mAudioMode);

        if (mNeedPlay) {
            if (useAudioTrack) {
                if (audioTrack != null) {
                    audioTrack.setTrackParam(playerFle, isAssetWav);
                    if (!audioTrack.init(mAudioMode)) {
                        stopAll();
                        return -1;
                    }
                    audioTrack.start();
                }
            } else {
                if (audioPlayer != null) {
                    audioPlayer.setPlayerParam(playerFle, isAssetWav);
                    audioPlayer.start(getAudioMode());
                }
            }
        }

        if (mNeedRecord) {
            if (audioRecord != null) {
                result = audioRecord.init(recordSource, recordSampleRate, recordChannel, false);
                if (result < 0) {
                    stopAll();
                    return result;
                }
                
                audioRecord.start();
                isRecording = true;
            } else {
                Log.e(TAG, "Should init audioRecord before .");
            }
        }

        if (mNeedShare) {
            if (AudioShareService.checkShareParameter(shareSampleRate, shareChannel, shareUsage, shareData) >= 0) {
                //获得录屏权限，启动Service进行录制
                Intent intent = new Intent(MainActivity.getContext(), AudioShareService.class);
                intent.putExtra("shareSampleRate", shareSampleRate);
                intent.putExtra("shareChannel", shareChannel);
                intent.putExtra("shareUsage", shareUsage);
                intent.putExtra("shareData", shareData);
                startForegroundService(MainActivity.getContext(), intent);
                isSharing = true;
            } else {
                Log.e(TAG, "Should check audio share parameters.");
                return -3;
            }
        }

        state = STATE_START;
        return 0;
    }

    public int stopAll() {
        int result = -1;

        if (state == STATE_DEFAULT) {
            return 0;
        }

        if (mNeedPlay) {
            if (useAudioTrack) {
                if (audioTrack != null) {
                    audioTrack.stop();
                }
            } else {
                if (audioPlayer != null) {
                    audioPlayer.stop();
                }
            }
        }

        if (mNeedRecord) {
            if (audioRecord != null) {
                audioRecord.stop();
                isRecording = false;
            }
        }

        if (mNeedShare) {
            Intent service = new Intent(MainActivity.getContext(), AudioShareService.class);
            MainActivity.getContext().stopService(service);
            isSharing = false;
        }

        state = STATE_STOP;
        return 0;
    }

    void setAudioMode(int mode) {
        if (audioManager != null) {
            audioManager.setMode(mode);
            Log.d(TAG, "setAudioMode " + mode + " to " + audioManager.getMode());
        } else {
            Log.e(TAG, "setAudioMode must init audioManager first.");
        }
    }

    private int getAudioMode() {
        if (audioManager != null) {
            return audioManager.getMode();
        } else {
            return AudioManager.MODE_NORMAL;
        }
    }

    public String micInfo() {
        return audioDevice.getInputDevsInfo();
    }

    public String spkInfo() {
        return audioDevice.getOutputDevsInfo();
    }

    public List<String> listSpeaker() {
        return audioDevice.listSpeaker();
    }

    public String getVolumeInfo() {
        if (audioManager == null) {
            return "0/0";
        }

        int streamType;
        switch (audioManager.getMode()) {
            case AudioManager.MODE_IN_COMMUNICATION:
                streamType = AudioManager.STREAM_VOICE_CALL;
                break;
            case AudioManager.MODE_RINGTONE:
                streamType = AudioManager.STREAM_RING;
                break;
            case AudioManager.MODE_IN_CALL:
                streamType = AudioManager.USE_DEFAULT_STREAM_TYPE;
                break;
            default:
                streamType = AudioManager.STREAM_MUSIC;
                break;
        }

        return audioManager.getStreamVolume(streamType) + "/" + audioManager.getStreamMaxVolume(streamType);
    }

    public String getPhoneState() {
        String phoneState = "None";
        if (audioManager != null) {
            TelephonyManager telephone = (TelephonyManager) MainActivity.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (telephone != null) {
                switch (telephone.getCallState()) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        phoneState = "OnCall";
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        phoneState = "Ringing";
                        break;
                    default:
                        phoneState = "None";
                        break;
                }
            }
        }

        return phoneState;
    }

}
