package com.powerino.prototype;

/**
 * Created by me on 17/02/2018.
 */

public class Constants {
    public static final long CALIBRATION_POINTS = 30;

    public static enum CalibrationStatus {
        FORWARDS_IN_PROGRESS, BACKWARDS_IN_PROGRESS, WEIGHT_IN_PROGRESS,
        FORWARDS_DONE, BACKWARDS_DONE, WEIGHT_DONE,
        NONE,
        UNINITIALIZED
    }

    // Set this to false when running in an emulator (they don't support Bluetooth)
    public static final boolean ENABLE_BLUETOOTH = false;

}
