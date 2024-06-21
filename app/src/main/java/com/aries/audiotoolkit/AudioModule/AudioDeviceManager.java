package com.aries.audiotoolkit.AudioModule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AudioDeviceManager {
    private static final String TAG = "AudioDeviceManager";

    private Context mContext;
    private AudioManager audioManager;
    private AudioDeviceMonitor deviceMonitor = null;
    private boolean isVirtualDeviceAdded = false;
    private boolean isVirtualDeviceRemoved = false;
    private BluetoothScoCallback mBtCallback = null;

    private BluetoothHeadsetReceiver btHeadset = null;
    private BluetoothProfile headsetProfile;
    private boolean hasBluetoothDev;


    public static final String[] DeviceNameAlias = {
            "Unknown",  // UNKNOWN
            "Earpiece",  // BUILTIN_EARPIECE
            "Speaker",  // BUILTIN_SPEAKER
            "WIRED_HEADSET",  // WIRED_HEADSET
            "WIRED_HEADPHONES",  // WIRED_HEADPHONES
            "LINE_ANALOG",    // LINE_ANALOG
            "LINE_DIGITAL",    // LINE_DIGITAL
            "BLUETOOTH_SCO",    // BLUETOOTH_SCO
            "BLUETOOTH_A2DP",    // BLUETOOTH_A2DP
            "HDMI",    // HDMI
            "HDMI_ARC",    // HDMI_ARC
            "USB_DEVICE",    // USB_DEVICE
            "USB_ACCESSORY",    // USB_ACCESSORY
            "DOCK",    // DOCK
            "FM",    // FM
            "Mic",    // BUILTIN_MIC
            "FM_TUNER",    // FM_TUNER
            "TV_TUNER",    // TV_TUNER
            "TELEPHONY",    // TELEPHONY
            "AUX_LINE",    // AUX_LINE
            "IP",    // IP
            "BUS",    // BUS
            "USB_HEADSET",    // USB_HEADSET
            "HEARING_AID",    // HEARING_AID
            "SPEAKER_SAFE",    // TYPE_BUILTIN_SPEAKER_SAFE
            "REMOTE_SUBMIX",    // REMOTE_SUBMIX
            "BLE_HEADSET",    // BLE_HEADSET
            "BLE_SPEAKER",    // BLE_SPEAKER
            "ECHO_REFERENCE",    // ECHO_REFERENCE
            "HDMI_EARC",    // HDMI_EARC
            "BLE_BROADCAST"    // BLE_BROADCAST
    };

    private static volatile AudioDeviceManager instance = null;
    public static AudioDeviceManager getInstance() {
        if (instance == null) {
            synchronized (AudioDeviceManager.class) {
                if (instance == null) {
                    instance = new AudioDeviceManager();
                }
            }
        }
        return instance;
    }

    private AudioDeviceManager() {
        mContext = MainActivity.getContext();
        audioManager = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceMonitor = new AudioDeviceMonitor();
        }

        if (audioManager != null) {
            isVirtualDeviceAdded = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.registerAudioDeviceCallback(deviceMonitor, null);
            } else {
                initBluetoothState();
                Log.d(TAG, "use bluetooth profile.");
            }
        }
    }

    /**
     * Build.VERSION.SDK_INT < Build.VERSION_CODES.M
     */
    private void initBluetoothState() {
        hasBluetoothDev = false;
        BluetoothHeadsetListener headsetListener = new BluetoothHeadsetListener();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean result = adapter.getProfileProxy(mContext, headsetListener, BluetoothProfile.HEADSET);
        if (!result) {
            Log.e(TAG, "getProfileProxy for HEADSET failed.");
        }

        btHeadset = new BluetoothHeadsetReceiver();
        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        btFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        btFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(btHeadset, btFilter);

        // check bluetooth device is already connect or not
        hasBluetoothDev = hasBluetoothDevLowerLollipop();
        Log.w(TAG, "initBluetoothState with hasBluetoothDevice=" + (hasBluetoothDev ? "true" : "false"));
        if (hasBluetoothDev && mBtCallback != null) {
            mBtCallback.onScoAdded();
        }
    }

    /**
     * Build.VERSION.SDK_INT < Build.VERSION_CODES.M
     */
    @SuppressLint("MissingPermission")
    private boolean hasBluetoothDevLowerLollipop() {
        int count = 0;

        if (headsetProfile == null) {
            return false;
        }

        List<BluetoothDevice> devices = headsetProfile.getConnectedDevices();
        for (BluetoothDevice dev : devices) {
            count++;
            Log.d(TAG, "Dev[" + count + "] name=" + dev.getName()
                    + ", type=" + dev.getType() + ", address=" + dev.getAddress());
        }

        return count > 0;
    }

    /**
     * AudioManager.getDevices() will return devices list info, we must check the audio devices is a
     * virtual or valid devices. For example TYPE_DOCK is a virtual audio device that can not used to
     * record or play.
     * @param deviceInfo AudioDeviceInfo returned from AudioManager lists.
     * @return 1 if device is a valid audio device.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isInvalidDevice(AudioDeviceInfo deviceInfo) {
        if (deviceInfo != null) {
            switch (deviceInfo.getType()) {
                case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_HDMI:
                case AudioDeviceInfo.TYPE_USB_DEVICE:
                case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE:
                case AudioDeviceInfo.TYPE_BLE_HEADSET:
                case AudioDeviceInfo.TYPE_BLE_SPEAKER:
                    return false;
                default:
                    return true;
            }
        } else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String getDevsInfo(int flags) {
        StringBuilder listInfo = new StringBuilder();
        boolean needSplit = false;

        if (audioManager != null) {
            AudioDeviceInfo[] devs = audioManager.getDevices(flags);
            int i = 0;
            StringBuilder message = new StringBuilder("");
            for (; i < devs.length; i++) {
                message.append("[").append(i).append("--").append(parseDeviceInfo(devs[i])).append("]\n");
                if (isInvalidDevice(devs[i])) {
                    continue;
                }

                if (!needSplit) {
                    needSplit = true;
                } else {
                    listInfo.append(", ");
                }
                if (flags == AudioManager.GET_DEVICES_INPUTS) {
                    if (devs[i].getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        listInfo.append(devs[i].getAddress());
                    }
                }
                listInfo.append(DeviceNameAlias[devs[i].getType()]);
            }
            Log.i(TAG, message.toString());
        }
        return listInfo.toString();
    }

    public String getInputDevsInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getDevsInfo(AudioManager.GET_DEVICES_INPUTS);
        } else {
            return "";
        }
    }

    public String getOutputDevsInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getDevsInfo(AudioManager.GET_DEVICES_OUTPUTS);
        } else {
            return "";
        }
    }

    public List<String> listSpeaker() {

        List<String> speakers = new ArrayList<>();
        speakers.add("跟随系统");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return speakers;
        }

        if (audioManager != null) {
            AudioDeviceInfo[] devs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            int i = 0;
            for (; i < devs.length; i++) {
                if (isInvalidDevice(devs[i])) {
                    continue;
                }
                speakers.add(DeviceNameAlias[devs[i].getType()]);
            }
        }
        return speakers;
    }

    public boolean selectSpeaker(int deviceId) {
        return false;
    }

    public int getCurrentSpeaker() {
        return 0;
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

    public int getScoDeviceId(int flag) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] inputDevs = audioManager.getDevices(flag);
            for (AudioDeviceInfo dev : inputDevs) {
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return dev.getId();
                }
            }
        }
        return 0;
    }

    public int getA2dpDevId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo dev : devs) {
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    return dev.getId();
                }
            }
        }
        return 0;
    }

    public void enableBluetoothSco(boolean enable) {
        if (audioManager == null) {
            return;
        }

        if (enable && !audioManager.isBluetoothScoOn()) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else if (!enable && audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            int mode = enable ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL;
            if (audioManager.getMode() != mode) {
                audioManager.setMode(mode);
            }
        }

        Log.i(TAG, "Set bluetoothSco=" + (enable ? "on" : "off")
                + "， and now BluetoothSco=" + (audioManager.isBluetoothScoOn() ? "on" : "off"));
        Log.i(TAG, "Bluetooth a2dp=" + (audioManager.isBluetoothA2dpOn() ? "on" : "off"));
    }

    public void registerBluetoothCallback(BluetoothScoCallback cb) {
        mBtCallback = cb;
    }

    public void unregisterBluetoothCallback() {
        mBtCallback = null;
    }

    /**
     * parse AudioDeviceInfo list to String
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private String parseDeviceInfo(AudioDeviceInfo dev) {
        String result = "";
        result += "ID:" + dev.getId() + ", ";
        result += "Type:" + DeviceNameAlias[dev.getType()] + ", ";
        result += "ProductName:" + dev.getProductName() + ", ";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            result += "Address:" + dev.getAddress() + ", ";
        }
        result += "toString:" + dev;
        return result;
    }

    /**
     * AudioDeviceCallback defines the mechanism by which applications can receive notifications
     * of audio device connection and disconnection events.
     * @see AudioManager#registerAudioDeviceCallback(AudioDeviceCallback, android.os.Handler handler).
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private class AudioDeviceMonitor extends AudioDeviceCallback {

        /**
         * Called by the {@link AudioManager} to indicate that one or more audio devices have been
         * connected.
         * @param addedDevices  An array of {@link AudioDeviceInfo} objects corresponding to any
         * newly added audio devices.
         */
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            if (isVirtualDeviceAdded) {
                isVirtualDeviceAdded = false;
            }

            int count = 0;
            boolean bluetoothScoAdded = false;
            boolean bluetoothA2dpAdded = false;
            for (AudioDeviceInfo dev : addedDevices) {
                count++;
                Log.i(TAG, "Added Dev[" + count + "] " + parseDeviceInfo(dev));
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    bluetoothScoAdded = true;
                }
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    bluetoothA2dpAdded = true;
                }
            }

            if (bluetoothScoAdded && mBtCallback != null) {
                mBtCallback.onScoAdded();
            }

            if (bluetoothA2dpAdded && mBtCallback != null) {
                mBtCallback.onA2dpAdded();
            }
        }

        /**
         * Called by the {@link AudioManager} to indicate that one or more audio devices have been
         * disconnected.
         * @param removedDevices  An array of {@link AudioDeviceInfo} objects corresponding to any
         * newly removed audio devices.
         */
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            if (isVirtualDeviceRemoved) {
                isVirtualDeviceRemoved = false;
            }

            int count = 0;
            boolean bluetoothScoRemoved = false;
            boolean bluetoothA2dpRemoved = false;
            for (AudioDeviceInfo dev : removedDevices) {
                count++;
                Log.i(TAG, "Remove Dev[" + count + "] " + parseDeviceInfo(dev));
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    bluetoothScoRemoved = true;
                }
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    bluetoothA2dpRemoved = true;
                }
            }

            if (bluetoothScoRemoved && mBtCallback != null) {
                mBtCallback.onScoRemoved();
            }

            if (bluetoothA2dpRemoved && mBtCallback != null) {
                mBtCallback.onA2dpRemoved();
            }
        }
    }

    private class BluetoothHeadsetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Receiver Broadcast with action=" + action);
            if (Objects.equals(action, AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                Log.d(TAG, "AudioManager SCO audio state update, AudioManager.EXTRA_SCO_AUDIO_STATE=" + state);
                switch (state) {
                    case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoAdded();
                        }
                        break;
                    case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoRemoved();
                        }
                        break;
                    default:
                        break;
                }
            } else if (Objects.equals(action, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Log.d(TAG, "BluetoothHeadset audio state changed, BluetoothProfile.EXTRA_STATE=" + state);
                switch (state) {
                    case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoAdded();
                        }
                        break;
                    case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoRemoved();
                        }
                        break;
                    default:
                        break;
                }
            } else if (Objects.equals(action, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                Log.d(TAG, "BluetoothHeadset connection state changed, BluetoothProfile.EXTRA_STATE=" + state);
                switch (state) {
                    case BluetoothHeadset.STATE_CONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoAdded();
                        }
                        break;
                    case BluetoothHeadset.STATE_DISCONNECTED:
                        if (mBtCallback != null) {
                            mBtCallback.onScoRemoved();
                        }
                        break;
                    default:
                        break;
                }
            } else {
                Log.w(TAG, "BluetoothHeadsetReceiver with unknown action.");
            }
        }
    }

    private class BluetoothHeadsetListener implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG, "ServiceConnected with HEADSET profile.");
                headsetProfile = proxy;
            } else {
                Log.w(TAG, "profile:" + profile + " has connected.");
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.w(TAG, "ServiceDisconnected with profile=" + profile);
        }
    }

    protected void finalize() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (audioManager != null && deviceMonitor != null) {
                audioManager.unregisterAudioDeviceCallback(deviceMonitor);
            }
        }
        if (mContext != null && btHeadset != null) {
            mContext.unregisterReceiver(btHeadset);
        }
        mContext = null;
        audioManager = null;
    }
}