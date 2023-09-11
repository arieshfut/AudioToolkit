package com.aries.audiotoolkit.AudioModule;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.aries.audiotoolkit.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecordThread {
    private static final String TAG = "AudioRecordThread";

    private static final boolean DEBUG = false;
    private static final boolean DUMP = true;

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_RECORDING = 3;
    private static final int STATE_STOP = STATE_DEFAULT;

    // Requested size of each recorded buffer provided to the client.
    private static final int MAX_RECORD_ERROR_COUNT = 50;

    private int mAudioSource;
    private int mSampleRate;
    private int mAudioFormat;
    private int mEncodeBit;
    private int mBufferSizeInBytes;
    private AudioEffectThread audioEffect;
    private int state;

    private byte[] byteData;
    private RecordThread recordThread;
    private AudioRecord audioRecorder = null;

    private String filePath;
    private FileOutputStream os;


    public AudioRecordThread() {
        filePath = null;
        os = null;
        recordThread = null;
        audioEffect = null;
        state = STATE_DEFAULT;
    }

    public int init(int source, int sample, int channel, boolean flag) {
        // Used to record audio and video. The recording control is based on a simple state machine.
        if (ActivityCompat.checkSelfPermission(MainActivity.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "has no permission for RECORD_AUDIO");
            MainActivity.showToast(TAG + "has no permission for RECORD_AUDIO");
            audioRecorder = null;
            return -1;
        }

        int checkResult = checkRecordParameter(source, sample, channel);
        if (checkResult < 0) {
            audioRecorder = null;
            return -1;
        }

        mAudioSource = source;
        mSampleRate = sample;
        mAudioFormat = (channel == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
        mEncodeBit = AudioFormat.ENCODING_PCM_16BIT;
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRate, mAudioFormat, mEncodeBit);
        byteData = new byte[mBufferSizeInBytes];

        try {
            audioRecorder = new AudioRecord.Builder()
                    .setAudioSource(mAudioSource)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(mEncodeBit)
                            .setSampleRate(mSampleRate)
                            .setChannelMask(mAudioFormat)
                            .build())
                    .setBufferSizeInBytes(2 * mBufferSizeInBytes)
                    .build();
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "UnsupportedOperationException error: " + e.getMessage());
            audioRecorder = null;
            return -1;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException error: " + e.getMessage());
            audioRecorder = null;
            return -1;
        }

        if (audioRecorder == null || audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Failed to create a new AudioRecord instance");
            audioRecorder = null;
            return -1;
        }

        if (DUMP) {
            createDumpFile();
        }

        audioEffect = AudioEffectThread.getInstance();
        audioEffect.setSession(audioRecorder.getAudioSessionId());

        state = STATE_INIT;
        return 0;
    }

    public int checkRecordParameter(int source, int sample, int channel) {
        if (source < MediaRecorder.AudioSource.DEFAULT || source > MediaRecorder.AudioSource.UNPROCESSED) {
            Log.e(TAG, "unexpected audio source = " + source + " error.");
            return -1;
        }

        if (MainActivity.getContext().checkPermission(Manifest.permission.CAPTURE_AUDIO_OUTPUT, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            switch (source) {
                case MediaRecorder.AudioSource.VOICE_UPLINK:
                case MediaRecorder.AudioSource.VOICE_DOWNLINK:
                case MediaRecorder.AudioSource.VOICE_CALL:
                case MediaRecorder.AudioSource.REMOTE_SUBMIX:
                    Log.e(TAG, "unexpected audio source = " + source + " without permission.");
                    return -1;
                default:
                    break;
            }
        }

        if (sample != 8000 && sample != 16000 && sample != 32000 && sample != 44100
                && sample != 48000 && sample != 96000) {
            Log.e(TAG, "unexpected audio sample rate = " + sample + " error.");
            return -1;
        }

        if (channel != 1 && channel != 2) {
            Log.e(TAG, "unexpected audio channel = " + channel + " error.");
            return -1;
        }

        return 0;
    }

    public void start() {
        Log.d(TAG, "start Recording now.");

        if (state == STATE_INIT) {
            // Starts recording from the AudioRecord instance.
            try {
                audioRecorder.startRecording();
                Log.i(TAG, "startRecording end.");
                state = STATE_START;
                if (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    state = STATE_RECORDING;
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "AudioRecord.startRecording failed: " + e.getMessage());
            }
        }

        if (state != STATE_RECORDING || audioRecorder == null) {
            Log.e(TAG, "AudioRecorder is null or state error");
        }

        recordThread = new RecordThread("AudioRecordJavaThread");
        recordThread.start();
    }

    public void stop() {
        audioEffect.setSession(0);

        Log.d(TAG, "stop Recording now.");
        if (recordThread != null) {
            recordThread.stopThread();
            recordThread = null;
        }
    }

    /**
     * Audio thread which keeps calling ByteBuffer.read() waiting for audio
     * to be recorded. Feeds recorded data to the native counterpart as a
     * periodic sequence of callbacks using DataIsRecorded().
     * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
     */
    private class RecordThread extends Thread {
        private volatile boolean keepAlive = true;
        private int errorCount = 0;
        private int preBytesRead = 0;
        private boolean needPrintFirstFrame;

        public RecordThread(String name) {
            super(name);
            needPrintFirstFrame = true;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.d(TAG, "AudioRecordThread start.");
            errorCount = 0;
            preBytesRead = 0;

            while (keepAlive) {
                int bytesRead = audioRecorder.read(byteData, 0, mBufferSizeInBytes);
                if (needPrintFirstFrame) {
                    Log.d(TAG, "AudioRecord voip first buffer start.");
                    needPrintFirstFrame = false;
                }
                if (bytesRead == mBufferSizeInBytes) {
                    dumpPcmToFile(byteData);
                } else {
                    String errorMessage = "AudioRecord.read failed: " + bytesRead;
                    Log.e(TAG, errorMessage);
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord read error: ERROR_INVALID_OPERATION");
                        keepAlive = false;
                    }

                    if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        if (preBytesRead == AudioRecord.ERROR_BAD_VALUE || errorCount == 0) {
                            errorCount++;
                        }
                    } else {
                        errorCount = 0;
                    }

                    if (errorCount > MAX_RECORD_ERROR_COUNT) {
                        Log.e(TAG, "AudioRecord read error: ERROR_BAD_VALUE");
                        keepAlive = false;
                    }
                }

                preBytesRead = bytesRead;
            }

            try {
                if (audioRecorder != null) {
                    audioRecorder.stop();
                    state = STATE_STOP;
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "AudioRecord.stop failed: " + e.getMessage());
            }
        }

        // Stops the inner thread loop and also calls AudioRecord.stop().
        // Does not block the calling thread.
        public void stopThread() {
            Log.i(TAG, "stopThread");
            keepAlive = false;
            errorCount = 0;
            preBytesRead = 0;
            closeDumpFile();
        }
    }

    private void createDumpFile() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        filePath = MainActivity.getDumpPath() + "/voip_" + simpleDateFormat.format(new Date()) + ".pcm";

        boolean flags = true;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                // 如果路径不存在，先创建路径
                File mFile = file.getParentFile();
                if (mFile != null && !mFile.exists()) {
                    mFile.mkdirs();
                }
                flags = file.createNewFile();
            }
        } catch (Exception e) {
            Log.e(TAG, "createDumpFile=" + filePath + " with exception " + e.toString());
            flags = false;
        }

        Log.i(TAG, "createDumpFile=" + flags + ", filePathName " + filePath);
        if (!flags) {
            return;
        }

        try {
            os = null;
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void dumpPcmToFile(byte[] data) {
        if (null != os) {
            try {
                os.write(data);
            } catch (IOException ignored) {
            }
        }
    }

    private void closeDumpFile() {
        if (null != os) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "closeDumpFile with os null");
        }
    }

}
