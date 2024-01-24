package com.aries.audiotoolkit.AudioModule;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class AudioDeviceManager {
    private static final String TAG = "AudioDeviceManager";

    private AudioManager audioManager;
    private AudioDeviceMonitor deviceMonitor = null;
    private boolean isVirtualDeviceAdded = false;
    private boolean isVirtualDeviceRemoved = false;

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

    public AudioDeviceManager(AudioManager manager) {
        audioManager = manager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceMonitor = new AudioDeviceMonitor();
        }
    }

    public int start() {
        if (audioManager != null) {
            isVirtualDeviceAdded = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.registerAudioDeviceCallback(deviceMonitor, null);
            }
        }

        return 1;
    }

    public void stop() {
        if (audioManager != null) {
            isVirtualDeviceRemoved = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.unregisterAudioDeviceCallback(deviceMonitor);
            }
        }
        audioManager = null;
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

    public void showToast(String title, String message) {
        MainActivity.showToast(title + message);
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
                return;
            }

            if (addedDevices != null && addedDevices.length > 0) {
                int size = addedDevices.length;
                StringBuilder message = new StringBuilder("[");
                for (int i = 0; i < size; i++) {
                    message.append(i).append(" ").append(parseDeviceInfo(addedDevices[i])).append(";\n");
                }
                message.append("]");
                Log.i(TAG, message.toString());
            } else {
                Log.v(TAG, "invalid devices add.");
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
                return;
            }

            if (removedDevices != null && removedDevices.length > 0) {
                int size = removedDevices.length;
                StringBuilder message = new StringBuilder("[");
                for (int i = 0; i < size; i++) {
                    message.append(i).append(" ").append(parseDeviceInfo(removedDevices[i])).append("; ");
                }
                message.append("]");
                Log.i(TAG, message.toString());
                showToast("DeviceRemoved:", message.toString());
            } else {
                Log.v(TAG, "invalid devices remove.");
            }
        }
    }
}