package com.mindlinker.audiotoolkit.PreResearch;


public class PreResearchManager {
    private final static String TAG = "PreResearchManager";

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_STOP = STATE_DEFAULT;

    private static PreResearchManager instance;
    private AudioAlsaManager audioAlsa;
    private int state;

    static {
        instance = new PreResearchManager();
    }

    private PreResearchManager() {
        audioAlsa = AudioAlsaManager.getInstance();
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
}
