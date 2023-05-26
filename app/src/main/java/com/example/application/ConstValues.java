package com.example.application;
// Class to keep constant values
public class ConstValues {
    // All constant values for the signals processing:
    // Total acc:
    public static float max_tacc_x = 2.09318f;
    public static float max_tacc_y = 1.679395f;
    public static float max_tacc_z = 1.279333f;
    public static float min_tacc_x = -0.611752f;
    public static float min_tacc_y = -1.676159f;
    public static float min_tacc_z = -1.807025f;
    // Body acc:
    public static float max_bacc_x = 1.215835f;
    public static float max_bacc_y = 1.014024f;
    public static float max_bacc_z = 1.02971f;
    public static float min_bacc_x = -1.255404f;
    public static float min_bacc_y = -1.457404f;
    public static float min_bacc_z = -1.391085f;
    // Total gyr:
    public static float max_gyr_x = 4.239656f;
    public static float max_gyr_y = 6.876592f;
    public static float max_gyr_z = 3.204549f;
    public static float min_gyr_x = -5.243325f;
    public static float min_gyr_y = -5.414154f;
    public static float min_gyr_z = -2.803811f;
    // Number of signal samples:
    public static int NUM_SAMPLES = 64;
    public static int NUM_INPUT = 6;
    public static int NUM_OUTPUT = 3;
    // Activities:
    public static final String WALKING = "Walking";
    public static final String SITTING = "Sitting";
    public static final String LAYING = "Laying";
    public static final String STANDING = "Standing";
    public static final String UPSTAIRS = "Walking Upstairs";
    public static final String DOWNSTAIRS = "Walking Downstairs";
    // Values for low-pass filters
    // alpha = T/T+dt where dt = 0.02s and T = 0.008s
    static final float ALPHA_1 = 0.3f;
    // alpha = T/T+dt where dt = 0.02s and T = 1/2*pi*fc = 0.53s
    static final float ALPHA_2 = 0.9f;
}
