package com.aries.audiotoolkit.AudioModule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.MainActivity;

public class AudioVolumeManager {
    private final static String TAG = "AudioVolumeManager";

    private final Context context;
    private AudioManager audioManager;
    private final int maxVolumeIndex;

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

    @SuppressLint("SwitchIntDef")
    public float getVolume() {
        if (audioManager == null) {
            Log.e(TAG, "setVolume: need init audio manager first");
            return -1;
        }

        int currentVolume;
        switch (audioManager.getMode()) {
            case AudioManager.MODE_RINGTONE:
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                break;
            case AudioManager.MODE_NORMAL:
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                break;
            default:
                Log.w(TAG, "getVolume with unexpected audio mode=" + audioManager.getMode());
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                break;
        }

        return (float)currentVolume/maxVolumeIndex;
    }
}
