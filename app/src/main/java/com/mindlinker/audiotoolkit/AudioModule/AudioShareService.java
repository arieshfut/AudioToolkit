package com.mindlinker.audiotoolkit.AudioModule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.mindlinker.audiotoolkit.MainActivity;
import com.mindlinker.audiotoolkit.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioShareService extends Service {

    private static String TAG = "AudioShareService";

    private Context mContext = null;

    private int shareSampleRate;
    private int shareChannel;
    private int shareUsage;
    private Intent shareData = null;
    private MediaProjection mediaProjection = null;
    private int mBufferSizeInBytes = 0;
    private AudioRecord audioRecord = null;

    private ExecutorService mExecutorService = null;
    private String filePath = null;

    private boolean isRecord = false;
    private boolean needPrintFirstFrame = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = MainActivity.getContext();
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling startService(Intent),
     * providing the arguments it supplied and a unique integer token representing the start request.
     * Do not call this method directly.
     *
     * @param intent resultCode.resultData.
     * @param flags about flags
     * @param startId result about start state
     * @return START_NOT_STICKY
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        try {
            shareSampleRate = intent.getIntExtra("shareSampleRate", 16000);
            shareChannel = intent.getIntExtra("shareChannel", 1);
            shareUsage = intent.getIntExtra("shareUsage", AudioAttributes.USAGE_MEDIA);
            shareData = intent.getParcelableExtra("shareData");

            mediaProjection = createMediaProjection();
            audioRecord = createAudioRecord();

            if (audioRecord != null) {
                audioRecord.startRecording();
                isRecord = true;
                needPrintFirstFrame = true;

                mExecutorService = Executors.newCachedThreadPool();
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        writePcmToFile();
                    }
                });
            } else {
                Log.e(TAG, "createAudioRecord is null.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**
         * START_NOT_STICKY:
         * Constant to return from onStartCommand(Intent, int, int): if this service's process is
         * killed while it is started (after returning from onStartCommand(Intent, int, int)),
         * and there are no new start intents to deliver to it, then take the service out of the
         * started state and don't recreate until a future explicit call to Context.startService(Intent).
         * The service will not receive a onStartCommand(Intent, int, int) call with a null Intent
         * because it will not be re-started if there are no pending Intents to deliver.
         */
        return Service.START_NOT_STICKY;
    }

    public static int checkShareParameter(int sample, int channel, int usage, Intent data) {
        if (sample != 8000 && sample != 16000 && sample != 32000 && sample != 44100
                && sample != 48000 && sample != 96000) {
            Log.e(TAG, "unexpected audio sample rate = " + sample + " error.");
            return -1;
        }

        if (channel != 1 && channel != 2) {
            Log.e(TAG, "unexpected audio channel = " + channel + " error.");
            return -1;
        }

        if (MainActivity.getContext().checkPermission(Manifest.permission.CAPTURE_AUDIO_OUTPUT, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            if (usage == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
                Log.e(TAG, "unexpected share usage = USAGE_VOICE_COMMUNICATION without permission.");
                return -1;
            }
        }

        if (data == null) {
            Log.e(TAG, "unexpected share intent data is null.");
            return -1;
        }

        return 0;
    }

    //createMediaProjection
    public MediaProjection createMediaProjection() {
        /**
         * Use with getSystemService(Class) to retrieve a MediaProjectionManager instance for
         * managing media projection sessions.
         */
        return ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                .getMediaProjection(Activity.RESULT_OK, shareData);
        /**
         * Retrieve the MediaProjection obtained from a succesful screen capture request.
         * Will be null if the result from the startActivityForResult() is anything other than RESULT_OK.
         */
    }

    private boolean createFile(String path) {
        if (TextUtils.isEmpty(path)) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd_HH-mm-ss");
            path = Environment.getExternalStorageDirectory() + "/audio/loop_" + simpleDateFormat.format(new Date()) + ".pcm";
        }

        boolean flags = false;
        try {
            File file = new File(path);
            if (file.exists()) {
                return true;
            } else {
                // 如果路径不存在，先创建路径
                File mFile = file.getParentFile();
                if (mFile != null && !mFile.exists()) {
                    flags = mFile.mkdirs();
                }
                flags = flags & file.createNewFile();
            }
        } catch (Exception e) {
            return false;
        }
        return flags;
    }

    private void writePcmToFile() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            filePath = Environment.getExternalStorageDirectory() + "/share_" + simpleDateFormat.format(new Date()) + ".pcm";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            filePath = Objects.requireNonNull(mContext.getExternalFilesDir(null)).getAbsolutePath() + "/share_" + simpleDateFormat.format(new Date()) + ".pcm";
        }

        boolean result = createFile(filePath);
        Log.i(TAG, "Create record file path=" + result + ", filePathName " + filePath);
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;
        byte[] data = new byte[mBufferSizeInBytes];

        if (null != os) {
            while (isRecord && null != audioRecord) {
                read = audioRecord.read(data, 0, mBufferSizeInBytes);
                if (needPrintFirstFrame) {
                    Log.e(TAG, "AudioRecord loop first buffer start.");
                    needPrintFirstFrame = false;
                }

                // 如果读取音频数据没有出现错误，就将数据写入到文件
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private AudioRecord createAudioRecord() {
        // Used to record audio and video. The recording control is based on a simple state machine.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission denied.");
            return null;
        }

        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null, please check parameter.");
            return null;
        }

        int sampleRate = shareSampleRate;
        int format = (shareChannel == 1) ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        int encode = AudioFormat.ENCODING_PCM_16BIT;
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, format, encode);

        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(shareUsage)
                .build();

        return new AudioRecord.Builder()
                // .setAudioSource(source)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(encode)
                        .setSampleRate(sampleRate)
                        .setChannelMask(format)
                        .build())
                .setBufferSizeInBytes(2 * mBufferSizeInBytes)
                .setAudioPlaybackCaptureConfig(config)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (audioRecord != null) {
            Log.i(TAG, "onDestroy isRecord " + isRecord);
            if (isRecord) {
                isRecord = false;
                audioRecord.stop();
                audioRecord.release();
            }
            audioRecord = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdownNow();
            mExecutorService = null;
        }

        isRecord = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 123, nfIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 123, nfIntent, PendingIntent.FLAG_ONE_SHOT);
        }

        builder.setContentIntent(pendingIntent) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }
}
