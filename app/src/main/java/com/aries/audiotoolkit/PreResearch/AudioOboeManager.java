package com.aries.audiotoolkit.PreResearch;


import static java.lang.Thread.sleep;


import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aries.audiotoolkit.AudioModule.AudioDeviceManager;
import com.aries.audiotoolkit.AudioModule.BluetoothScoCallback;
import com.aries.audiotoolkit.MainActivity;


public class AudioOboeManager implements BluetoothScoCallback {
    private final static String TAG = "AudioOboeManager";

    public static final int DEVICES_INPUTS    = 0x0001;
    public static final int DEVICES_OUTPUTS   = 0x0002;

    private int recordDeviceId = 0;
    private int playDeviceId = 0;
    private String playFile = "";
    private int sampleRate = 16000;
    private int channelCount = 1;
    private int bit = 16;

    private Context mContext;
    private final Handler handler;
    private AudioManager audioManager;
    private AudioDeviceManager mAudioDevManager;
    private final static String fileDir = "/sdcard/audiotoolkit/";
    private OboeThread oboeThread;
    private boolean btEnable = false;
    private boolean needRecorder = false;
    private boolean needPlayer = false;
    private boolean isRunning = false;

    private static final int DEV_UPDATE_SCO_MS = 1500;
    private final Runnable BtScoAddedRunner = this::btScoAdded;
    private final Runnable BtScoRemovedRunner = this::btScoRemoved;

    // Used to load the 'native_oboe_manager' library on application startup.
    static {
        System.loadLibrary("native_oboe_manager");
    }

    private static volatile AudioOboeManager instance = null;
    public static AudioOboeManager getInstance() {
        if (instance == null) {
            synchronized (AudioOboeManager.class) {
                if (instance == null) {
                    instance = new AudioOboeManager();
                }
            }
        }
        return instance;
    }

    private AudioOboeManager() {
        handler = new Handler();
        mContext = MainActivity.getContext();
        audioManager = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
        mAudioDevManager = AudioDeviceManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceLog();
        }

        mAudioDevManager.registerBluetoothCallback(this);
        NativeOboeCreate();
    }

    public void setOboeProp(int audioApi, boolean needRecord, boolean needPlay, boolean bluetoothEnable) {
        String path = MainActivity.getDumpPath();
        btEnable = bluetoothEnable;
        needRecorder = needRecord;
        needPlayer = needPlay;
        NativeSetProp(path, audioApi, needRecord, needPlay);
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
            mAudioDevManager.enableBluetoothSco(true);
            recordDeviceId = mAudioDevManager.getScoDeviceId(DEVICES_INPUTS);
            playDeviceId = mAudioDevManager.getScoDeviceId(DEVICES_OUTPUTS);
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

        mAudioDevManager.enableBluetoothSco(false);
        btEnable = false;
        isRunning = false;
    }

    public void btScoAdded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new Thread(new DeviceAddedRunnable()).start();
        }
    }

    public void btScoRemoved() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new Thread(new DeviceRemovedRunnable()).start();
        }
    }

    public boolean setBluetoothScoProp(boolean isEnable) {
        if (!mAudioDevManager.hasBluetoothDev()) {
            return false;
        }

        btEnable = isEnable;
        if (isRunning) {
            mAudioDevManager.enableBluetoothSco(btEnable);
            if (btEnable) {
                NativeUpdateDeviceId(mAudioDevManager.getScoDeviceId(DEVICES_INPUTS), mAudioDevManager.getScoDeviceId(DEVICES_OUTPUTS));
            } else {
                NativeUpdateDeviceId(0, mAudioDevManager.getA2dpDevId());
            }
        }

        return btEnable;
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

    @Override
    public void onA2dpAdded() {
    }

    @Override
    public void onScoAdded() {
        startBtScoAddedTimer();
    }

    @Override
    public void onScoRemoved() {
        startBtScoRemovedTimer();
    }

    @Override
    public void onA2dpRemoved() {
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


    protected void finalize() {
        audioManager = null;
        mContext = null;
        mAudioDevManager.unregisterBluetoothCallback();
        mAudioDevManager = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private class DeviceAddedRunnable implements Runnable {
        @Override
        public void run() {
            if (audioManager == null) {
                return;
            }
            for (int time = 0; time < 10; time++) {
                if (audioManager.isBluetoothScoOn()) {
                    Log.i(TAG, "DeviceAddedRunnable update Device Id");
                    int inputDevId = mAudioDevManager.getScoDeviceId(AudioManager.GET_DEVICES_INPUTS);
                    int outputDevId = mAudioDevManager.getScoDeviceId(AudioManager.GET_DEVICES_OUTPUTS);
                    NativeUpdateDeviceId(inputDevId, outputDevId);
                    return;
                }

                try {
                    sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException" + e.getMessage());
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private class DeviceRemovedRunnable implements Runnable {
        @Override
        public void run() {
            if (audioManager == null) {
                return;
            }
            for (int time = 0; time < 10; time++) {
                if (!audioManager.isBluetoothScoOn()) {
                    Log.i(TAG, "DeviceRemovedRunnable update Device Id");
                    NativeUpdateDeviceId(0, 0);
                    return;
                }

                try {
                    sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException" + e.getMessage());
                }
            }
        }
    }

    private void startBtScoAddedTimer() {
        if (handler == null) {
            return;
        }
        clearBtScoAddedTimer();
        handler.postDelayed(BtScoAddedRunner, DEV_UPDATE_SCO_MS);
    }

    private void startBtScoRemovedTimer() {
        if (handler == null) {
            return;
        }
        clearBtScoRemovedTimer();
        handler.postDelayed(BtScoRemovedRunner, DEV_UPDATE_SCO_MS);
    }

    private void clearBtScoAddedTimer() {
        if (handler == null) {
            return;
        }
        handler.removeCallbacks(BtScoAddedRunner);
    }

    private void clearBtScoRemovedTimer() {
        if (handler == null) {
            return;
        }
        handler.removeCallbacks(BtScoRemovedRunner);
    }
}
