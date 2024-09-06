package com.aries.audiotoolkit.AudioModule;


import static androidx.core.content.ContextCompat.startForegroundService;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


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
    private AudioDeviceManager audioDevice;
    private AudioVolumeManager audioVolume;
    private AudioManager audioManager;

    private int state;
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
    private int shareUsage = 1;
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
        audioDevice = AudioDeviceManager.getInstance();
        audioVolume = AudioVolumeManager.getInstance();

        /*new Timer().schedule(new TimerTask() {
            public void run() {
                if (audioManager != null) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("audioMode=").append(audioManager.getMode());
                    msg.append(" maxVol=").append(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    msg.append(" currentVol=").append(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        msg.append(" isVolumeFixed=").append(audioManager.isVolumeFixed());
                    }
                    msg.append(" isBtScoOn=").append(audioManager.isBluetoothScoOn());
                    msg.append(" isMicMute=").append(audioManager.isMicrophoneMute());
                    msg.append(" isSpkOn=").append(audioManager.isSpeakerphoneOn());
                    Log.i(TAG, String.valueOf(msg));
                }
            }
        }, 0, 10000);*/

        state = STATE_DEFAULT;
    }

    public static AudioModuleManager getInstance() {
        return instance;
    }

    public void finalize() {
        Log.d(TAG, "destructor");
    }

    public void setFlagAndMode(boolean needRecord, boolean needShare, boolean needPlay, int mode) {
        mNeedRecord = needRecord;
        mNeedShare = needShare;
        mNeedPlay = needPlay;
        mAudioMode = mode;
    }

    public void setRecordParameter(int audioSource, int sample, int channel) {
        if (audioRecord.checkRecordParameter(audioSource, sample, channel) < 0) {
            return;
        }
        recordSource = audioSource;
        recordSampleRate = sample;
        recordChannel = channel;

    }

    public void setPlayerParameter(String assetFle, boolean isAssetFile, boolean needAudioTrack) {
        playerFle = assetFle;
        isAssetWav = isAssetFile;
        useAudioTrack = needAudioTrack;
    }

    public void setShareParameters(int sample, int channel, int usage, Intent data) {
        shareSampleRate = sample;
        shareChannel = channel;
        shareUsage = usage;
        shareData = data;
    }

    public boolean isEffectEnable() {
        return audioEffect.isEffectEnable();
    }
    public boolean setEffectEnable(boolean on) {
        return audioEffect.setEffectEnable(on);
    }

    public boolean isBtScoOn() { return audioDevice.isBtScoOn(); }
    public void switchBtScoState() { audioDevice.switchBtScoState(); }

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
                    isPlaying = audioTrack.start();
                }
            } else {
                if (audioPlayer != null) {
                    audioPlayer.setPlayerParam(playerFle, isAssetWav);
                    isPlaying = (audioPlayer.start(getAudioMode()) == 0);
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
        if (state == STATE_DEFAULT) {
            return 0;
        }

        if (mNeedPlay) {
            if (useAudioTrack) {
                if (audioTrack != null) {
                    audioTrack.stop();
                    isPlaying = false;
                }
            } else {
                if (audioPlayer != null) {
                    audioPlayer.stop();
                    isPlaying = false;
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
        return audioVolume.getVolumeInfo();
    }

    public String getMuteInfo() {
        if (audioManager == null) {
            return " ";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (audioManager.isStreamMute(AudioManager.STREAM_MUSIC) ? "true" : "false");
        } else {
            return " ";
        }
    }

    public String getPhoneState() {
        String phoneState = " ";
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

    public void setVolume(boolean b) {
        audioVolume.setVolumeDirect(b);
    }
}
