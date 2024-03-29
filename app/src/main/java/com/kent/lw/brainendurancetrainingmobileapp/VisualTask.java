package com.kent.lw.brainendurancetrainingmobileapp;

import java.util.Random;

public class VisualTask {

    // VISUAL
    private final String TASK_VISUAL = "Visual";

    private final int INTERVAL_MIN = 1;
    private final int INTERVAL_MAX = 5;
    private final int INTERVAL_INCREMENT = 2*1000;

    private final int STIMULUS_DISAPPEAR_TIME = 30*1000;

    public static final int TASK_VISUAL_DURATION = 1*60*1000; //1mins

    private final int TASK_VISUAL_VALID_RES_THRESHOLD = 500;
    private final int TASK_VISUAL_FALSE_START_THRESHOLD = 100;

    private Random random = new Random();

    public int getStimulusDisappearTime () {
        return STIMULUS_DISAPPEAR_TIME;
    }


    public int getRandomInterval () {
        return (random.nextInt(INTERVAL_MAX)+1) * INTERVAL_INCREMENT;
    }

    public int getValidResThreshold() {
        return TASK_VISUAL_VALID_RES_THRESHOLD;
    }

    public int getFalseStartThreshold() { return TASK_VISUAL_FALSE_START_THRESHOLD; }

}
