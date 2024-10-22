package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.utils;

import com.intellij.remoterobot.stepsProcessing.StepLogger;
import com.intellij.remoterobot.stepsProcessing.StepWorker;

public class StepsLogger {
    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        StepWorker.registerProcessor(new StepLogger());
        initialized = true;
    }
}
