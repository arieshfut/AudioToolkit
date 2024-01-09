package com.aries.multicompile;

public class NativeLib {

    // Used to load the 'multicompile' library on application startup.
    static {
        System.loadLibrary("multicompile");
    }

    /**
     * A native method that is implemented by the 'multicompile' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}