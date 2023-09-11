package com.aries.audiotoolkit.PreResearch;

import android.os.Build;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;


public class AudioOboeManager {
    private final static String TAG = "AudioOboeManager";
    private static AudioOboeManager instance;

    private int deviceId = 0;
    private int sampleRate = 16000;
    private int channelCount = 1;
    private int bit = 16;

    private final static String fileDir = "/sdcard/audiotoolkit/";
    private OboeThread oboeThread;


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
    private AudioOboeManager() { NativeOboeCreate(); }

    public void setOboeProp(int audioApi, boolean needRecord, boolean needPlay) {
        String path = MainActivity.getDumpPath();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // path += "/audiotoolkit";
        }
        NativeSetProp(path, audioApi, needRecord, needPlay);
    }

    public void setOboeRecordParam(int devId, int sample, int channel, int b) {
        deviceId = devId;
        sampleRate = sample;
        channelCount = channel;
        bit = b;
        NativeOboeInitRecorder(deviceId, sampleRate, channelCount, bit);
    }

    public void setOboePlayParam(int deviceId) {
        String playDir = fileDir + "test.wav";
        NativeOboeInitPlayer(playDir, deviceId);
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
