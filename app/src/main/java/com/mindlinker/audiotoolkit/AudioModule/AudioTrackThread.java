package com.mindlinker.audiotoolkit.AudioModule;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.mindlinker.audiotoolkit.common.WaveFile;


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
    // but the wait times out afther this amount of time.
    private static final long AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS = 2000;

    private AudioTrack audioTrack = null;
    private TrackThread audioThread = null;
    private WaveFile wavFile = null;

    private int mode;
    private int sampleRate;
    private int channel;
    private int channelConfig;
    private int bit;
    private int format;
    private int bytesPerFrame;
    private int usage;
    private int contentType;
    private int streamType;
    private int bufferSize;
    private byte[] pcmData;

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
                audioTrack.play();
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
            if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                Log.w(TAG, "AudioTrack failed to enter playing state.");
            }

            int sizeFromWav;
            while (keepAlive) {
                sizeFromWav = wavFile.readBuffer(pcmData);
                if (sizeFromWav == bufferSize) {
                    int bytesWritten = audioTrack.write(pcmData, 0, bufferSize);
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
                    wavFile.close();
                    wavFile.open();
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
            if (audioTrack != null) {
                Log.d(TAG, "Stopping the audio track...");
                try {
                    audioTrack.stop();
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

    public boolean setTrackParam(String wavFilePath, boolean isAssetWav) {
        wavFile = new WaveFile(wavFilePath, isAssetWav);
        return true;
    }

    private boolean setAudioParam() {
        sampleRate = wavFile.sampleRate;
        channel = wavFile.numChannels;
        channelConfig = (channel == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
        bit = wavFile.bitsPerSample;
        switch (bit) {
            case 8:
                format = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 16:
                format = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case 32:
                format = AudioFormat.ENCODING_PCM_FLOAT;
                break;
            default:
                Log.e(TAG, "wav bit " + bit + "not support.");
                return false;
        }
        bytesPerFrame = channel * (bit / 8);
        bufferSize = bytesPerFrame * (sampleRate / BUFFERS_PER_SECOND);
        pcmData = new byte[bufferSize];

        switch (mode) {
            case AudioManager.MODE_RINGTONE:
                usage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
                contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION;
                streamType = AudioManager.STREAM_RING;
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
                contentType = AudioAttributes.CONTENT_TYPE_SPEECH;
                streamType = AudioManager.STREAM_VOICE_CALL;
                break;
            default:
                usage = AudioAttributes.USAGE_MEDIA;
                contentType = AudioAttributes.CONTENT_TYPE_MUSIC;
                streamType = AudioManager.STREAM_MUSIC;
                break;
        }
        return true;
    }

    public boolean init(int audioMode) {
        if (!wavFile.isNormalWave) {
            Log.e(TAG, "wav file format not normal, please check or choose other file.");
            return false;
        }

        mode = audioMode;
        if (!setAudioParam()) {
            Log.e(TAG, "wav audio format error.");
            return false;
        }

        // Get the minimum buffer size required for the successful creation of an
        // AudioTrack object to be created in the MODE_STREAM mode.
        // Note that this size doesn't guarantee a smooth playback under load.
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, format);
        // For the streaming mode, data must be written to the audio sink in
        // chunks of size (given by byteBuffer.capacity()) less than or equal
        // to the total buffer size |minBufferSizeInBytes|. But, we have seen
        // reports of "getMinBufferSize(): error querying hardware". Hence, it
        // can happen that |minBufferSizeInBytes| contains an invalid value.
        if (minBufferSize < bufferSize) {
            Log.e(TAG, "AudioTrack.getMinBufferSize returns an invalid value.");
            return false;
        }

        // Ensure that prevision audio session was stopped correctly before trying
        // to create a new AudioTrack.
        if (audioTrack != null) {
            Log.d(TAG, "Conflict with existing AudioTrack.");
            return false;
        }
        try {
            audioTrack = createAudioTrack();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "IllegalArgumentException: " + e.getMessage());
            releaseAudioResources();
            return false;
        }

        // It can happen that an AudioTrack is created but it was not successfully
        // initialized upon creation. Seems to be the case e.g. when the maximum
        // number of globally available audio tracks is exceeded.
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "Initialization of audio track failed.");
            releaseAudioResources();
            return false;
        }

        state = STATE_INIT;
        Log.d(TAG, "init AudioTrack success with sample=" + sampleRate
                + ", channel=" + channel + ", bit=" + bit + ", bufferSize=" + bufferSize
                + ", usage=" + usage + ", contentType=" + contentType + ", streamType=" + streamType
                + ", state=" + audioTrack.getState()
                + ", sessionId=" + audioTrack.getAudioSessionId());
        return true;
    }

    public boolean start() {
        Log.d(TAG, "start play out");
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "AudioTrack instance is not successfully initialized.");
            return false;
        }

        if (wavFile.canReadBuffer()) {
            wavFile.open();
            audioThread = new TrackThread("TrackThread");
            audioThread.start();
            state = STATE_START;
        }

        return true;
    }

    public boolean stop() {
        Log.d(TAG, "stop play out");
        if (state == STATE_START) {
            wavFile.close();
            if(audioThread != null) {
                audioThread.stopThread();
            }
        }

        final Thread aThread = audioThread;
        audioThread = null;
        if (aThread != null) {
            Log.d(TAG, "Stopping the TrackThread...");
            aThread.interrupt();
            if (!joinUninterruptibly(aThread, AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS)) {
                Log.e(TAG, "Join of TrackThread timed out.");
            }
            Log.d(TAG, "TrackThread has now been stopped.");
        }

        releaseAudioResources();
        state = STATE_STOP;
        return true;
    }

    // Creates and AudioTrack instance using AudioAttributes and AudioFormat as input.
    // It allows certain platforms or routing policies to use this information for more
    // refined volume or routing decisions.
    private AudioTrack createAudioTrack() {
        int nativeSampleRate = AudioTrack.getNativeOutputSampleRate(streamType);
        Log.d(TAG, "nativeOutputSampleRate: " + nativeSampleRate);
        if (sampleRate != nativeSampleRate) {
            Log.w(TAG, "Unable to use fast mode since requested sample rate is not native");
        }
        // Create an audio track where the audio usage is for VoIP and the content type is speech.
        return new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(format)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channel)
                        .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    // Helper method which throws an exception  when an assertion has failed.
    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    // Releases the native AudioTrack resources.
    private void releaseAudioResources() {
        Log.d(TAG, "releaseAudioResources");
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
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
