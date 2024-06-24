package com.aries.audiotoolkit.PreResearch;


public class PreResearchManager {
    private final static String TAG = "PreResearchManager";

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_STOP = STATE_DEFAULT;

    private static PreResearchManager instance;
    private AudioAlsaManager audioAlsa;
    private AudioOboeManager audioOboe;
    private int state;
    private int oboeState;

    static {
        instance = new PreResearchManager();
    }

    private PreResearchManager() {
        audioAlsa = AudioAlsaManager.getInstance();
        audioOboe = AudioOboeManager.getInstance();
        state = STATE_DEFAULT;
    }

    public static PreResearchManager getInstance() {
        return instance;
    }

    public void aslaCapture(boolean enable) {
        if (enable) {
            audioAlsa.start();
        } else {
            audioAlsa.stop();
        }
    }

    public void setAlsaParameter(int card, int device, int sample, int channel, int bit) {
        audioAlsa.setParameter(card, device, sample, channel, bit);
        state = STATE_INIT;
    }

    public void oboeEnable(boolean enable) {
        if (enable) {
            audioOboe.start();
        } else {
            audioOboe.stop();
        }
    }

    public void setOboeParameter(int api, boolean needRecord, boolean needPlay, int inputDevId, int sample, int channel, int bit, int outputDev, boolean btEnable) {
        audioOboe.setOboeProp(api, needRecord, needPlay, btEnable);
        audioOboe.setOboeParam(inputDevId, sample, channel, bit, outputDev);
    }

    public boolean setBluetoothScoProp(boolean btEnable) {
        return audioOboe.setBluetoothScoProp(btEnable);
    }

    public boolean isLatencyDetectionSupported() {
        if (audioOboe != null) {
            return audioOboe.isLatencyDetectionSupported();
        }
        return false;
    }

    public double getCurrentOutputLatencyMillis() {
        return audioOboe.getCurrentOutputLatencyMillis();
    }
}
