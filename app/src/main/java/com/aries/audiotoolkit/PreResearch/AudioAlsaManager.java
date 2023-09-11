package com.aries.audiotoolkit.PreResearch;

import android.annotation.SuppressLint;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;


public class AudioAlsaManager {
    private final static String TAG = "AudioAlsaManager";
    private static AudioAlsaManager instance;
    private int alsaCard;
    private int alsaDevice;
    private int alsaSample;
    private int alsaChannel;
    private int alsaBit;
    private AlsaThread alsaThread;

    // Used to load the 'native_alsa' library on application startup.
    static {
        System.loadLibrary("native_alsa");
    }

    static {
        instance = new AudioAlsaManager();
    }

    public static AudioAlsaManager getInstance() {
        return instance;
    }

    public void start() {
        Log.d(TAG, "Start Alsa Capture now.");
        alsaThread = new AlsaThread("AlsaJavaThread");
        alsaThread.start();
    }

    public void stop() {
        Log.d(TAG, "Stop Alsa Capture now.");
        NativeAlsaCaptureStop();
        if (alsaThread != null) {
            alsaThread.stopThread();
            alsaThread = null;
        }
    }

    public void setParameter(int card, int device, int sample, int channel, int bit) {
        alsaCard = card;
        alsaDevice = device;
        alsaSample = sample;
        alsaChannel = channel;
        alsaBit = bit;
    }

    private class AlsaThread extends Thread {
        private final static String TAG_THREAD = "AlsaThread";
        public AlsaThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Log.d(TAG_THREAD, "AlsaThread start.");
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String path = MainActivity.getDumpPath() + "/alsa_" + simpleDateFormat.format(new Date()) + ".wav";
            int result = NativeAlsaCaptureStart(path, alsaCard, alsaDevice, alsaSample, alsaChannel, alsaBit);
            if (result < 0) {
                Log.d(TAG_THREAD, "Alsa capture error:" + result);
            }
        }

        // Stops the inner thread loop and also calls AudioRecord.stop().
        // Does not block the calling thread.
        public void stopThread() {
            Log.i(TAG_THREAD, "stopThread");
        }
    }

    /**
     * A native method that is implemented by the 'native_alsa' native library,
     * which is packaged with this application.
     */
    public native int NativeAlsaCaptureStart(String path, int card, int device, int sampleRate, int channel, int bit);

    public native void NativeAlsaCaptureStop();
}
