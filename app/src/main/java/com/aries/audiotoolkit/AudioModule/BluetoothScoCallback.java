package com.aries.audiotoolkit.AudioModule;

public interface BluetoothScoCallback {
    void onA2dpAdded();
    void onScoAdded();
    void onScoRemoved();
    void onA2dpRemoved();
}
