package com.aries.audiotoolkit.common;

import android.content.res.AssetManager;
import android.util.Log;

import com.aries.audiotoolkit.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class WaveFile {
    private static final String TAG = "WaveFile";

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_PARSE = 2;

    private static final int ID_RIFF = 0x46464952;
    private static final int ID_WAVE = 0x45564157;
    private static final int ID_FMT = 0x20746d66;
    private static final int ID_DATA = 0x61746164;
    private static final int ChunkFormatBytes = 16;

    // riff wave header
    private int riff_id;
    private int riff_sz;
    private int wave_id;
    // chunk header
    private int id;
    private int sz;
    // chunk format -- wave file format
    public short audioFormat;
    public short numChannels;
    public int sampleRate;
    public int byteRate;
    public short blockAlign;
    public short bitsPerSample;

    private String filePath;
    private int wavDataIndex;
    private boolean isAssetWav;

    public boolean isNormalWave;
    private AssetManager asset;
    private InputStream pcmData;

    private int state;

    public WaveFile(String wavFilePath, boolean isAssetWavFile) {
        isAssetWav = isAssetWavFile;
        filePath = wavFilePath;
        wavDataIndex = 0;
        isNormalWave = false;
        state = STATE_INIT;
        if (isAssetWav) {
            parseAssetFile();
        } else {
            parseExternalFile();
        }
    }

    private void parseAssetFile() {
        try {
            asset = MainActivity.getContext().getAssets();
            pcmData = asset.open(filePath);
            if (parseWavHead(pcmData) >= 0) {
                isNormalWave = true;
                state = STATE_PARSE;
            }
        } catch (IOException e) {
            Log.e(TAG, "parseAssetFile IOException error: " + e.getMessage());
        }
    }

    private void parseExternalFile() {
        if (!isWavFileExist(filePath)) {
            Log.e(TAG, filePath + " not exist.");
            return;
        }

        try {
            FileInputStream rdf = new FileInputStream(filePath);
            if (parseWavHead(rdf) >= 0) {
                isNormalWave = true;
                state = STATE_PARSE;
            }
            rdf.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "parseExternalFile FileNotFoundException error: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "parseExternalFile IOException error: " + e.getMessage());
        }
    }

    public int parseWavHead(InputStream rdf) {
        if (rdf == null) {
            return -1;
        }

        try {
            int position = 0;
            riff_id = readInt(rdf); position += 4;
            riff_sz = readInt(rdf); position += 4;
            wave_id = readInt(rdf); position += 4;
            if ((riff_id != ID_RIFF) || wave_id != ID_WAVE) {
                Log.e(TAG, (isAssetWav ? "AssetFile" : filePath) + " is not a riff/wave file.");
            } else {
                int more_chunks = 1;
                do {
                    id = readInt(rdf); position += 4;
                    sz = readInt(rdf); position += 4;
                    switch (id) {
                        case ID_FMT:
                            audioFormat = readShort(rdf); position += 2;
                            numChannels = readShort(rdf); position += 2;
                            sampleRate = (int) readUnInt32(rdf); position += 4;
                            byteRate = (int) readUnInt32(rdf); position += 4;
                            blockAlign = readShort(rdf); position += 2;
                            bitsPerSample = readShort(rdf); position += 2;

                            /* If the format header is larger, skip the rest */
                            if (sz > ChunkFormatBytes) {
                                read(rdf, 0, sz - ChunkFormatBytes);
                                position += sz - ChunkFormatBytes;
                            }

                            break;
                        case ID_DATA:
                            /* Stop looking for chunks */
                            wavDataIndex = position;
                            more_chunks = 0;
                            break;
                        default:
                            /* Unknown chunk, skip bytes */
                            read(rdf, 0, 2); position += 2;
                    }
                } while (more_chunks > 0);
            }
            return 0;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "parseWavHead FileNotFoundException error: " + e.getMessage());
            return -1;
        } catch (IOException e) {
            Log.e(TAG, "parseWavHead IOException error: " + e.getMessage());
            return -1;
        }
    }

    public boolean canReadBuffer() {
        return isNormalWave && state == STATE_PARSE && wavDataIndex > 0;
    }

    public void open() {
        if (isAssetWav) {
            try {
                // asset.open(filePath);
                if (pcmData == null) {
                    pcmData = asset.open(filePath);
                    byte[] wavHeader = new byte[wavDataIndex];
                    readBuffer(wavHeader);
                    Log.w(TAG, "open wav and filter out wav header.");
                }
            } catch (IOException e) {
                Log.e(TAG, "open IOException error: " + e.getMessage());
            }
        } else {
            try {
                pcmData = new FileInputStream(filePath);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "open IOException error: " + e.getMessage());
            }
        }
    }

    public int readBuffer(byte[] byteBuffer) {
        int size = 0;
        if (pcmData == null) {
            Log.e(TAG, "FileInputStream is null");
            return -2;
        }

        try {
            size = pcmData.read(byteBuffer, 0, byteBuffer.length);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "readBuffer FileNotFoundException error: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "readBuffer IOException error: " + e.getMessage());
        }

        return size;
    }

    public void close() {
        if (isAssetWav) {
            try {
                if (pcmData != null) {
                    pcmData.close();
                    pcmData = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "close Asset IOException error: " + e.getMessage());
            }
        } else {
            try {
                if (pcmData != null) {
                    pcmData.close();
                    pcmData = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "close External IOException error: " + e.getMessage());
            }
        }
    }

    private void read(InputStream rdf, int pos, int length) throws IOException {
        byte[] result = new byte[length];
        int size = rdf.read(result, pos, length);
        if (size < 0) {
            Log.e(TAG, "read endOfFile.");
        }
    }

    private long readUnInt32(InputStream rdf) throws IOException {
        long res = 0;
        long[] l = new long[4];
        for (int i = 0; i < 4; ++i) {
            l[i] = rdf.read();
        }
        res = l[0] | (l[1] << 8) | (l[2] << 16) | (l[3] << 24);
        return res;
    }

    private int readInt(InputStream rdf) throws IOException {
        byte[] b = new byte[4];
        int size = rdf.read(b, 0, 4);
        if (size < 0) {
            return 0;
        } else {
            return ((b[3] << 24) + (b[2] << 16) + (b[1] << 8) + (b[0]));
        }
    }

    private short readShort(InputStream rdf) throws IOException {
        short temp = 0;
        byte[] b = new byte[2];
        int size = rdf.read(b, 0, 2);
        if (size > 0) {
            temp = (short)((b[1] << 8) + (b[0]));
        }
        return temp;
    }

    public static boolean isWavFileExist(String filePath) {
        if (!isFileExist(filePath) || !filePath.endsWith(".wav")) {
            Log.e(TAG, filePath + " not exist.");
            return false;
        }

        return true;
    }

    private static boolean isFileExist(String path) {
        try {
            File file = new File(path);
            return file.exists();
        } catch (Exception e) {
            Log.e(TAG, "File=" + path + " can not check, error=" + e.getMessage());
            return false;
        }
    }

    protected void finalize () {
        if (pcmData != null) {
            try {
                pcmData.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            pcmData = null;
        }

        if (asset != null) {
            asset.close();
            asset = null;
        }
    }

}
