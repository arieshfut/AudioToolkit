package com.aries.audiotoolkit.AudioModule;


import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioPlayerThread {
    private static final String TAG = "AudioPlayerThread";


    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSE = 3;
    private static final int STATE_STOP = STATE_DEFAULT;

    private String mWavFilePath;
    // private AssetFileDescriptor mInternalFile;
    private FileInputStream playerStream;
    private MediaPlayer mMediaPlayer;
    private boolean loopState;

    private int state;

    AudioPlayerThread() {
        mMediaPlayer = new MediaPlayer();
        mWavFilePath = null;
        playerStream = null;
        loopState = true;
        state = STATE_DEFAULT;
    }

    public void setPlayerParam(String filePath, boolean isAssetFile) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        mWavFilePath = filePath;
        if (isAssetFile) {
            setPlayerFile(mWavFilePath);
        } else {
            setPlayFilePath(mWavFilePath);
        }
    }

    public void setPlayerFile(String filePath) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        mWavFilePath = filePath;
        try {
            mMediaPlayer.reset();
            AssetFileDescriptor mInternalFile = MainActivity.getContext().getResources().getAssets().openFd(mWavFilePath);
            mMediaPlayer.setDataSource(mInternalFile.getFileDescriptor(), mInternalFile.getStartOffset(), mInternalFile.getLength());
            mMediaPlayer.prepare();
            state = STATE_INIT;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPlayFilePath(String filePath) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        mWavFilePath = filePath;
        try {
            playerStream = new FileInputStream(mWavFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(playerStream.getFD());// 设置声音源
            mMediaPlayer.prepare();
            state = STATE_INIT;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLoopState(boolean loop) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        if (loopState == loop) {
            return;
        }

        loopState = loop;
        mMediaPlayer.setLooping(loop);
    }

    public int start(int mode) {
        if (state != STATE_INIT) {
            Log.e(TAG, "please init player first.");
            return -1;
        }
        mMediaPlayer.setLooping(loopState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes temp;
            int contentType = AudioAttributes.CONTENT_TYPE_MUSIC;
            int usage = AudioAttributes.USAGE_MEDIA;
            int stream = AudioManager.STREAM_MUSIC;
            if (mode == AudioManager.MODE_IN_COMMUNICATION) {
                contentType = AudioAttributes.CONTENT_TYPE_SPEECH;
                usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
                stream = AudioManager.STREAM_VOICE_CALL;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                temp = new AudioAttributes.Builder()
                        .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                        .setContentType(contentType)
                        .setUsage(usage)
                        .setLegacyStreamType(stream)
                        .build();
            } else {
                temp = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setLegacyStreamType(stream)
                        .build();
            }
            mMediaPlayer.setAudioAttributes(temp);
        } else{
            Log.w(TAG, "can not init AudioAttributes.");
        }
        mMediaPlayer.start();
        state = mMediaPlayer.isPlaying() ? STATE_PLAYING : STATE_STOP;
        if (state == STATE_PLAYING) {
            Log.i(TAG, "start MediaPlayer success with wav=" + mWavFilePath
                    + ", loop=" + (loopState ? "true" : "false") + ", state=playing"
                    + ", sessionId=" + mMediaPlayer.getAudioSessionId());
        } else {
            Log.e(TAG, "start MediaPlayer failed.");
        }
        return 0;
    }

    public void pause() {
        if (state != STATE_PLAYING) {
            Log.e(TAG, "please init player first.");
            return;
        }
        mMediaPlayer.pause();
        state = STATE_PAUSE;
    }

    public void stop() {
        if (state != STATE_PLAYING) {
            return;
        }

        if (mMediaPlayer == null) {
            return;
        }

        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        state = STATE_STOP;
    }
}
