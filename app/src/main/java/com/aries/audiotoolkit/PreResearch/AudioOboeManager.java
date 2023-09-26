package com.aries.audiotoolkit.PreResearch;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.aries.audiotoolkit.AudioModule.AudioDeviceManager;
import com.aries.audiotoolkit.MainActivity;


public class AudioOboeManager {
    private final static String TAG = "AudioOboeManager";
    private static AudioOboeManager instance;

    private int deviceId = 0;
    private int sampleRate = 16000;
    private int channelCount = 1;
    private int bit = 16;

    private final AudioManager audioManager;
    private final static String fileDir = "/sdcard/audiotoolkit/";
    private OboeThread oboeThread;
    private boolean btEnable = false;
    private boolean needRecorder = false;
    private boolean needPlayer = false;


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
        AudioDeviceLog();
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
        AudioDeviceInfo[] devs = audioManager.getDevices(flag);
        for (AudioDeviceInfo dev : devs) {
            if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                return dev.getId();
            }
        }
        return 0;
    }

    public void setOboeRecordParam(int devId, int sample, int channel, int b) {
        if (!needRecorder) {
            return;
        }
        if (btEnable) {
            deviceId = getBluetoothDevId(AudioManager.GET_DEVICES_INPUTS);
        } else {
            deviceId = devId;
        }
        sampleRate = sample;
        channelCount = channel;
        bit = b;

        NativeOboeInitRecorder(deviceId, sampleRate, channelCount, bit);
    }

    public void setOboePlayParam(int deviceId) {
        if (!needPlayer) {
            return;
        }
        String playDir = fileDir + "test.wav";
        if (btEnable) {
            NativeOboeInitPlayer(playDir, getBluetoothDevId(AudioManager.GET_DEVICES_OUTPUTS));
        } else {
            NativeOboeInitPlayer(playDir, deviceId);
        }
    }

    public void start() {
        Log.d(TAG, "Start Oboe Thread now.");
        oboeThread = new OboeThread("OboeJavaThread");
        oboeThread.start();
    }

    public void stop() {
        Log.d(TAG, "Stop Oboe Thread now.");
        if (oboeThread != null) {
            oboeThread.stopThread();
            oboeThread = null;
        }
    }

    public void updateBluetoothId(boolean isEnable) {
        if (isEnable) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            NativeUpdateDeviceId(getBluetoothDevId(AudioManager.GET_DEVICES_INPUTS), getBluetoothDevId(AudioManager.GET_DEVICES_OUTPUTS));
        } else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
            /*try {
                sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            NativeUpdateDeviceId(0, 0);
        }
    }

    /**
     * parse AudioDeviceInfo list to String
     */
    private String parseDeviceInfo(AudioDeviceInfo dev) {
        String result = "";
        result += "ID:" + dev.getId() + ", ";
        result += "Type:" + AudioDeviceManager.DeviceNameAlias[dev.getType()] + ", ";
        result += "ProductName:" + dev.getProductName() + ", ";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            result += "Address:" + dev.getAddress() + ", ";
        }
        result += "toString:" + dev + ", ";
        return result;
    }

    private void AudioDeviceLog() {
        int count = 0;
        AudioDeviceInfo[] inputDevs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo dev : inputDevs) {
            count++;
            Log.i(TAG, "Input Dev[" + count + "] " + parseDeviceInfo(dev));
        }

        count = 0;
        AudioDeviceInfo[] outputDevs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo dev : outputDevs) {
            count++;
            Log.i(TAG, "Output Dev[" + count + "] " + parseDeviceInfo(dev));
        }
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

}
