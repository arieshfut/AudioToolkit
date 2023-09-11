package com.mindlinker.audiotoolkit.AudioModule;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

public class AudioEffectThread {
    private final static String TAG = "AudioEffectThread";

    private static AudioEffectThread instance;
    private AcousticEchoCanceler aec;
    private NoiseSuppressor ns;
    private int sessionID = 0;

    private boolean needEnable = false;
    private boolean canEnable = false;
    private boolean isAecOn = false;
    private boolean isNsOn = false;

    static {
        instance = new AudioEffectThread();
    }

    private AudioEffectThread() {}

    public static AudioEffectThread getInstance() {
        return instance;
    }

    public void setSession(int session) {
        Log.d(TAG, "setSession id = " + session);
        if (session > 0) {
            sessionID = session;
            aec = AcousticEchoCanceler.create(session);
            ns = NoiseSuppressor.create(session);
            canEnable = true;
        } else {
            aec = null;
            ns = null;
            sessionID = 0;
            canEnable = false;
            needEnable = false;
        }

        // set effect
        if (needEnable) {
            needEnable = false;
            setAECEnable(isAecOn);
            setNSEnable(isNsOn);
        }
    }

    public boolean isEffectEnable() {
        return isAecEnable() && isNsEnable();
    }

    private boolean isAecEnable() {
        if (aec != null) {
            return aec.getEnabled();
        }
        Log.e(TAG, "aec null return false.");
        return false;
    }

    private boolean isNsEnable() {
        if (ns != null) {
            return ns.getEnabled();
        }
        Log.e(TAG, "ns null return false.");
        return false;
    }

    private boolean setAECEnable(boolean on) {
        if (aec != null) {
            aec.setEnabled(on);
            return aec.getEnabled() == on;
        }

        Log.e(TAG, "aec is null can not setEffect.");
        return false;
    }

    private boolean setNSEnable(boolean on) {
        if (ns != null) {
            ns.setEnabled(on);
            return aec.getEnabled() == on;
        }

        Log.e(TAG, "ns is null can not setEffect.");
        return false;
    }

    public boolean setEffectEnable(boolean on) {
        if (!canEnable) {
            isAecOn = on;
            isNsOn = on;
            needEnable = true;
            return true;
        } else {
            needEnable = false;
            return setAECEnable(on) & setNSEnable(on);
        }
    }
}
