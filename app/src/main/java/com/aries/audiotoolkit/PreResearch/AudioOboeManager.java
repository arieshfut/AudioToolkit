package com.aries.audiotoolkit.PreResearch;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.AudioModule.AudioDeviceManager;
import com.aries.audiotoolkit.MainActivity;


public class AudioOboeManager {
    private final static String TAG = "AudioOboeManager";
    private static AudioOboeManager instance;

    private int recordDeviceId = 0;
    private int playDeviceId = 0;
    private String playFile = "";
    private int sampleRate = 16000;
    private int channelCount = 1;
    private int bit = 16;

    private final AudioManager audioManager;
    private final static String fileDir = "/sdcard/audiotoolkit/";
    private OboeThread oboeThread;
    private boolean btEnable = false;
    private boolean needRecorder = false;
    private boolean needPlayer = false;
    private boolean isRunning = false;


    // Used to load the 'native_oboe_manager' library on application startup.
    static {
        System.loadLibrary("native_oboe_manager");
    }

    static {
        instance = new AudioOboeManager();
    }

    public static AudioOboeManager getInstance() {
        return instance;
    }
    private AudioOboeManager() {
        audioManager = ((AudioManager) MainActivity.getContext().getSystemService(Context.AUDIO_SERVICE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceLog();
        }
        NativeOboeCreate();
    }


    public void setOboeProp(int audioApi, boolean needRecord, boolean needPlay, boolean bluetoothEnable) {
        String path = MainActivity.getDumpPath();
        btEnable = bluetoothEnable;
        needRecorder = needRecord;
        needPlayer = needPlay;
        NativeSetProp(path, audioApi, needRecord, needPlay);
    }

    private int getBluetoothDevId(int flag) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devs = audioManager.getDevices(flag);
            for (AudioDeviceInfo dev : devs) {
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return dev.getId();
                }
            }
        }
        return 0;
    }

    private int getA2dpDevId() {
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

    public void setOboeParam(int devId, int sample, int channel, int b, int outputDeviceId) {
        // set Oboe Record Param(devId, sample, channel, bit);
        recordDeviceId = devId;
        sampleRate = sample;
        channelCount = channel;
        bit = b;

        // set Oboe Play Param(outputDeviceId);
        playFile = fileDir + "test.wav";
        playDeviceId = outputDeviceId;

        if (btEnable) {
            enableBluetoothSco(true);
            recordDeviceId = getBluetoothDevId(AudioManager.GET_DEVICES_INPUTS);
            playDeviceId = getBluetoothDevId(AudioManager.GET_DEVICES_OUTPUTS);
        }

        if (needRecorder) {
            NativeOboeInitRecorder(recordDeviceId, sampleRate, channelCount, bit);
        }

        if (needPlayer) {
            NativeOboeInitPlayer(playFile, playDeviceId);
        }
    }

    public void start() {
        Log.d(TAG, "Start Oboe Thread now.");
        oboeThread = new OboeThread("OboeJavaThread");
        oboeThread.start();
        isRunning = true;
    }

    public void stop() {
        Log.d(TAG, "Stop Oboe Thread now.");
        if (oboeThread != null) {
            oboeThread.stopThread();
            oboeThread = null;
        }

        enableBluetoothSco(false);
        btEnable = false;
        isRunning = false;
    }

    public void setBluetoothScoProp(boolean isEnable) {
        btEnable = isEnable;
        if (isRunning) {
            enableBluetoothSco(btEnable);
            if (btEnable) {
                NativeUpdateDeviceId(getBluetoothDevId(AudioManager.GET_DEVICES_INPUTS), getBluetoothDevId(AudioManager.GET_DEVICES_OUTPUTS));
            } else {
                NativeUpdateDeviceId(0, getA2dpDevId());
            }
        }
    }

    private void enableBluetoothSco(boolean enable) {
        if (enable) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
        Log.i(TAG, "Set bluetoothSco=" + (enable ? "on" : "off")
                + "， and now BluetoothSco=" + (audioManager.isBluetoothScoOn() ? "on" : "off"));
        Log.i(TAG, "Bluetooth a2dp=" + (audioManager.isBluetoothA2dpOn() ? "on" : "off"));
    }

    /**
     * parse AudioDeviceInfo list to String
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private String parseDeviceInfo(AudioDeviceInfo dev) {
        String result = "";
        result += "ID:" + dev.getId() + ", ";
        result += "Type:" + AudioDeviceManager.DeviceNameAlias[dev.getType()] + ", ";
        result += "ProductName:" + dev.getProductName() + ", ";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            result += "Address:" + dev.getAddress() + ", ";
        }
        result += "toString:" + dev;
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void AudioDeviceLog() {
        int count = 0;
        AudioDeviceInfo[] inputDevs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        StringBuilder message = new StringBuilder();
        for (AudioDeviceInfo dev : inputDevs) {
            count++;
            message.append("[input ").append(count).append("--").append(parseDeviceInfo(dev)).append("]\n");
        }
        Log.i(TAG, message.toString());

        count = 0;
        message = new StringBuilder();
        AudioDeviceInfo[] outputDevs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo dev : outputDevs) {
            count++;
            message.append("[output ").append(count).append("--").append(parseDeviceInfo(dev)).append("]\n");
        }
        Log.i(TAG, message.toString());
    }

    public boolean isLatencyDetectionSupported() {
        return NativeOboeLatencySupport();
    }

    public double getCurrentOutputLatencyMillis() {
        return NativeOboeLatencyMillis();
    }

    private class OboeThread extends Thread {
        private final static String TAG_THREAD = "OboeThread";
        public OboeThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Log.d(TAG_THREAD, "OboeThread start.");
            int result = NativeOboeStart();
            if (result < 0) {
                Log.d(TAG_THREAD, "NativeOboeStart error:" + result);
            }
        }

        // Stops the inner thread loop and also calls AudioRecord.stop().
        // Does not block the calling thread.
        public void stopThread() {
            NativeOboeStop();
            Log.i(TAG_THREAD, "stopThread");
        }
    }

    /**
     * A native method that is implemented by the 'native_alsa' native library,
     * which is packaged with this application.
     */
    public native void NativeOboeCreate();
    public native void NativeSetProp(String path, int audioApi, boolean needRecord, boolean needPlay);
    public native void NativeUpdateDeviceId(int inputId, int outputId);
    public native void NativeOboeInitRecorder(int devId, int sample, int channel, int bit);
    public native void NativeOboeInitPlayer(String path, int devId);
    public native int NativeOboeStart();
    public native void NativeOboeStop();
    public native boolean NativeOboeLatencySupport();
    public native double NativeOboeLatencyMillis();

}
