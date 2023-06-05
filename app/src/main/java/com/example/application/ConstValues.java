package com.example.application;
// Class to keep constant values
public class ConstValues {
    // All constant values for the signals processing:
    // Number of signal samples:
    public static int NUM_SAMPLES = 64;
    public static int NUM_INPUT = 6;
    public static int NUM_OUTPUT = 3;
    // Values for low-pass filters
    // alpha = T/T+dt where dt = 0.02s and T = 0.008s
    static final float ALPHA_1 = 0.3f;
    // alpha = T/T+dt where dt = 0.02s and T = 1/2*pi*fc = 0.53s
    static final float ALPHA_2 = 0.9f;
}
