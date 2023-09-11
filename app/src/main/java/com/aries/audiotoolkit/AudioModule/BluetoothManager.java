package com.aries.audiotoolkit.AudioModule;

import android.media.AudioManager;

public class BluetoothManager {
    private final static String TAG = "BluetoothManager";

    private AudioManager audioManager;

    public BluetoothManager(AudioManager manager) {
        audioManager = manager;
    }

    public boolean isBtScoOn() {
        return audioManager.isBluetoothScoOn();
    }

    public void switchBtScoState() {
        if (isBtScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        } else {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
    }
}