package com.aries.audiotoolkit.AudioModule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.MainActivity;

public class AudioVolumeManager {
    private final static String TAG = "AudioVolumeManager";

    private final Context context;
    private AudioManager audioManager;
    private int maxVolumeIndex;
    private int curVolumeIndex;

    private static volatile AudioVolumeManager instance = null;
    public static AudioVolumeManager getInstance() {
        if (instance == null) {
            synchronized (AudioVolumeManager.class) {
                if (instance == null) {
                    instance = new AudioVolumeManager();
                }
            }
        }
        return instance;
    }

    private AudioVolumeManager() {
        context = MainActivity.getContext();
        audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        maxVolumeIndex = getMaxVolume();
    }

    private int getMaxVolume() {
        if (audioManager == null) {
            Log.e(TAG, "setVolume: need init audio manager first");
            return -1;
        }

        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private int getStreamType() {
        int streamType;
        switch (audioManager.getMode()) {
            case AudioManager.MODE_RINGTONE:
                streamType = AudioManager.STREAM_RING;
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                streamType = AudioManager.STREAM_VOICE_CALL;
                break;
            case AudioManager.MODE_NORMAL:
                streamType = AudioManager.STREAM_MUSIC;
                break;
            default:
                Log.w(TAG, "getVolume with unexpected audio mode=" + audioManager.getMode());
                streamType = AudioManager.STREAM_MUSIC;
                break;
        }
        return streamType;
    }

    @SuppressLint("SwitchIntDef")
    public float getVolume() {
        if (audioManager == null) {
            Log.e(TAG, "setVolume: need init audio manager first");
            return -1;
        }

        int currentVolume = audioManager.getStreamVolume(getStreamType());
        return (float)currentVolume/maxVolumeIndex;
    }

    public void setVolumeDirect(boolean up) {
        if (audioManager == null) {
            Log.e(TAG, "setVolume: need init audio manager first");
            return;
        }

        int direct = up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        audioManager.adjustVolume(direct, 0);
    }

    public String getVolumeInfo() {
        if (audioManager == null) {
            return "0/0";
        }
        int streamType = getStreamType();
        maxVolumeIndex = audioManager.getStreamMaxVolume(streamType);
        curVolumeIndex = audioManager.getStreamVolume(streamType);
        return curVolumeIndex + "/" + maxVolumeIndex;
    }
}
