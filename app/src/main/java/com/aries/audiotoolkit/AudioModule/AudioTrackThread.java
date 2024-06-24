package com.aries.audiotoolkit.AudioModule;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.aries.audiotoolkit.common.WaveFile;


public class AudioTrackThread {
    private static final String TAG = "AudioTrackThread";

    /** default audio player state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_STOP = STATE_DEFAULT;

    // Default audio data format is PCM 16 bit per sample.
    // Guaranteed to be supported by all devices.
    private static final int BITS_PER_SAMPLE = 16;

    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;

    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    // The TrackThread is allowed to wait for successful call to join()
    // but the wait times out after this amount of time.
    private static final long AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS = 2000;

    private AudioTrack mAudioTrack = null;
    private TrackThread mAudioThread = null;
    private WaveFile mWavFile = null;

    private int mMode;
    private int mSampleRate;
    private int mChannel;
    private int mChannelConfig;
    private int mBit;
    private int mFormat;
    private int mUsage;
    private int mContentType;
    private int mStreamType;
    private int mBufferSize;
    private byte[] mPcmData;

    private int state;

    /**
     * Audio thread which keeps calling AudioTrack.write() to stream audio.
     * Data is periodically acquired from the native WebRTC layer using the
     * nativeGetPlayoutData callback function.
     * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
     */
    private class TrackThread extends Thread {
        private volatile boolean keepAlive = true;

        public TrackThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.d(TAG, "TrackThread" + getThreadInfo());

            try {
                // In MODE_STREAM mode we can optionally prime the output buffer by
                // writing up to bufferSizeInBytes (from constructor) before starting.
                // This priming will avoid an immediate underrun, but is not required.
                // TODO(henrika): initial tests have shown that priming is not required.
                mAudioTrack.play();
            } catch (IllegalStateException e) {
                Log.d(TAG, "AudioTrack play failed, IllegalStateException: " + e.getMessage());
                releaseAudioResources();
                return;
            }
            // We have seen reports that AudioTrack.play() can sometimes start in a
            // paued mode (e.g. when application is in background mode).
            // TODO(henrika): consider calling reportWebRtcAudioTrackStartError()
            // and release audio resources here as well. For now, let the thread start
            // and hope that the audio session can be restored later.
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                Log.w(TAG, "AudioTrack failed to enter playing state.");
            }

            int sizeFromWav;
            while (keepAlive) {
                sizeFromWav = mWavFile.readBuffer(mPcmData);
                if (sizeFromWav == mBufferSize) {
                    int bytesWritten = mAudioTrack.write(mPcmData, 0, mBufferSize);
                    if (bytesWritten != sizeFromWav) {
                        Log.e(TAG, "AudioTrack.write played invalid number of bytes: " + bytesWritten);
                        // If a write() returns a negative value, an error has occurred.
                        // Stop playing and report an error in this case.
                        if (bytesWritten < 0) {
                            keepAlive = false;
                            Log.d(TAG, "AudioTrack.write failed: " + bytesWritten);
                        }
                    }
                } else if (sizeFromWav == -1) {
                    Log.w(TAG, "Wav file read error or EndOfFile, reopen wav file and continue play.");
                    mWavFile.close();
                    mWavFile.open();
                } else if (sizeFromWav == -2) {
                    Log.e(TAG, "Wav file open error, stop play.");
                    break;
                } else if (sizeFromWav == 0) {
                    Log.e(TAG, "Wav file read error, stop play.");
                    break;
                }
            }

            // Stops playing the audio data. Since the instance was created in
            // MODE_STREAM mode, audio will stop playing after the last buffer that
            // was written has been played.
            if (mAudioTrack != null) {
                Log.d(TAG, "Stopping the audio track...");
                try {
                    mAudioTrack.stop();
                    Log.d(TAG, "The audio track has now been stopped.");
                } catch (Exception e) {
                    Log.e(TAG, "AudioTrack.stop failed: " + e.getMessage());
                }
            }
        }

        // Stops the inner thread loop and also calls AudioTrack.pause() and flush().
        // Does not block the calling thread.
        public void stopThread() {
            Log.d(TAG, "stopThread");
            keepAlive = false;
        }
    }

    public AudioTrackThread() {
        Log.d(TAG, "constructor" + getThreadInfo());
        state = STATE_DEFAULT;
    }

    public void setTrackParam(String wavFilePath, boolean isAssetWav) {
        mWavFile = new WaveFile(wavFilePath, isAssetWav);
    }

    private boolean setAudioParam() {
        mSampleRate = mWavFile.sampleRate;
        mChannel = mWavFile.numChannels;
        mChannelConfig = (mChannel == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
        mBit = mWavFile.bitsPerSample;
        switch (mBit) {
            case 8:
                mFormat = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 16:
                mFormat = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case 32:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mFormat = AudioFormat.ENCODING_PCM_FLOAT;
                } else {
                    Log.e(TAG, "wav bit " + mBit + "not support on version < LOLLIPOP.");
                    return false;
                }
                break;
            default:
                Log.e(TAG, "wav bit " + mBit + "not support.");
                return false;
        }
        mBufferSize = mChannel * (mBit / 8) * (mSampleRate / BUFFERS_PER_SECOND);
        mPcmData = new byte[mBufferSize];

        switch (mMode) {
            case AudioManager.MODE_RINGTONE:
                mUsage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
                mContentType = AudioAttributes.CONTENT_TYPE_SONIFICATION;
                mStreamType = AudioManager.STREAM_RING;
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                mUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
                mContentType = AudioAttributes.CONTENT_TYPE_SPEECH;
                mStreamType = AudioManager.STREAM_VOICE_CALL;
                break;
            default:
                mUsage = AudioAttributes.USAGE_MEDIA;
                mContentType = AudioAttributes.CONTENT_TYPE_MUSIC;
                mStreamType = AudioManager.STREAM_MUSIC;
                break;
        }
        Log.d(TAG, "set param " + getThreadInfo());
        return true;
    }

    public boolean init(int audioMode) {
        if (!mWavFile.isNormalWave) {
            Log.e(TAG, "wav file format not normal, please check or choose other file.");
            return false;
        }

        mMode = audioMode;
        if (!setAudioParam()) {
            Log.e(TAG, "wav audio format error.");
            return false;
        }

        // Get the minimum buffer size required for the successful creation of an
        // AudioTrack object to be created in the MODE_STREAM mode.
        // Note that this size doesn't guarantee a smooth playback under load.
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mFormat);
        // For the streaming mode, data must be written to the audio sink in
        // chunks of size (given by byteBuffer.capacity()) less than or equal
        // to the total buffer size |minBufferSizeInBytes|. But, we have seen
        // reports of "getMinBufferSize(): error querying hardware". Hence, it
        // can happen that |minBufferSizeInBytes| contains an invalid value.
        if (minBufferSize < mBufferSize) {
            Log.e(TAG, "AudioTrack.getMinBufferSize returns an invalid value.");
            return false;
        }

        // Ensure that prevision audio session was stopped correctly before trying
        // to create a new AudioTrack.
        if (mAudioTrack != null) {
            Log.d(TAG, "Conflict with existing AudioTrack.");
            return false;
        }
        try {
            mAudioTrack = createAudioTrack();
            if (mAudioTrack == null) {
                Log.d(TAG, "Initialization of AudioTrack is NULL.");
                releaseAudioResources();
                return false;
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "IllegalArgumentException: " + e.getMessage());
            releaseAudioResources();
            return false;
        }

        // It can happen that an AudioTrack is created but it was not successfully
        // initialized upon creation. Seems to be the case e.g. when the maximum
        // number of globally available audio tracks is exceeded.
        if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "Initialization of audio track failed.");
            releaseAudioResources();
            return false;
        }

        state = STATE_INIT;
        Log.i(TAG, "init AudioTrack success with sample=" + mSampleRate
                + ", channel=" + mChannel + ", bit=" + mBit + ", bufferSize=" + mBufferSize
                + ", usage=" + mUsage + ", contentType=" + mContentType
                + ", streamType=" + mStreamType + ", state=" + mAudioTrack.getState()
                + ", sessionId=" + mAudioTrack.getAudioSessionId());
        return true;
    }

    public boolean start() {
        Log.d(TAG, "start play out");
        if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "AudioTrack instance is not successfully initialized.");
            return false;
        }

        if (mWavFile.canReadBuffer()) {
            mWavFile.open();
            mAudioThread = new TrackThread("TrackThread");
            mAudioThread.start();
            state = STATE_START;
        }
        return true;
    }

    public boolean stop() {
        Log.d(TAG, "stop play out");
        if (state == STATE_START) {
            if(mAudioThread != null) {
                mAudioThread.stopThread();
            }
        }

        final Thread aThread = mAudioThread;
        mAudioThread = null;
        if (aThread != null) {
            Log.d(TAG, "Stopping the TrackThread...");
            aThread.interrupt();
            if (!joinUninterruptibly(aThread, AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS)) {
                Log.e(TAG, "Join of TrackThread timed out.");
            }
            Log.d(TAG, "TrackThread has now been stopped.");
        }

        releaseAudioResources();
        mWavFile.close();
        state = STATE_STOP;
        return true;
    }

    // Creates and AudioTrack instance using AudioAttributes and AudioFormat as input.
    // It allows certain platforms or routing policies to use this information for more
    // refined volume or routing decisions.
    private AudioTrack createAudioTrack() {
        int nativeSampleRate = AudioTrack.getNativeOutputSampleRate(mStreamType);
        Log.d(TAG, "nativeOutputSampleRate: " + nativeSampleRate);
        if (mSampleRate != nativeSampleRate) {
            Log.w(TAG, "Unable to use fast mode since requested sample rate is not native");
        }
        // Create an audio track where the audio usage is for VoIP and the content type is speech.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new AudioTrack(
                        new AudioAttributes.Builder()
                                .setUsage(mUsage)
                                .setContentType(mContentType)
                                .build(),
                        new AudioFormat.Builder()
                                .setEncoding(mFormat)
                                .setSampleRate(mSampleRate)
                                .setChannelMask(mChannelConfig)
                                .build(),
                        mBufferSize,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE);
            } else {
                return new AudioTrack(mStreamType, mSampleRate, mChannel, mFormat, mBufferSize,
                        AudioTrack.MODE_STREAM, 0);
            }
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "UnsupportedOperationException error: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException error: " + e.getMessage());
            return null;
        }
    }

    // Releases the native AudioTrack resources.
    private void releaseAudioResources() {
        Log.d(TAG, "releaseAudioResources");
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    // Helper method for building a string of thread information.
    public static String getThreadInfo() {
        return "@[name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId()
                + "]";
    }

    public static boolean joinUninterruptibly(final Thread thread, long timeoutMs) {
        final long startTimeMs = SystemClock.elapsedRealtime();
        long timeRemainingMs = timeoutMs;
        boolean wasInterrupted = false;
        while (timeRemainingMs > 0) {
            try {
                thread.join(timeRemainingMs);
                break;
            } catch (InterruptedException e) {
                // Someone is asking us to return early at our convenience. We can't cancel this operation,
                // but we should preserve the information and pass it along.
                wasInterrupted = true;
                final long elapsedTimeMs = SystemClock.elapsedRealtime() - startTimeMs;
                timeRemainingMs = timeoutMs - elapsedTimeMs;
            }
        }
        // Pass interruption information along.
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return !thread.isAlive();
    }
}
