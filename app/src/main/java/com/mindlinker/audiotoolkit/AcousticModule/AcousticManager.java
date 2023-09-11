package com.mindlinker.audiotoolkit.AcousticModule;

public class AcousticManager {
    private final static String TAG = "AcousticManager";

    /** default audio record state */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_START = 2;
    private static final int STATE_STOP = STATE_DEFAULT;

    private static AcousticManager instance;
    private int state;

    static {
        instance = new AcousticManager();
    }

    private AcousticManager() {
        state = STATE_DEFAULT;
    }

    public static AcousticManager getInstance() {
        return instance;
    }
}
