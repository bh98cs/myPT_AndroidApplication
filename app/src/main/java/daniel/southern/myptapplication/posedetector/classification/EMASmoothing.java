package daniel.southern.myptapplication.posedetector.classification;

import android.os.SystemClock;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Class to run EMA smoothing
 */
public class EMASmoothing {
    private static final int DEFAULT_WINDOW_SIZE = 10;
    private static final float DEFAULT_ALPHA = 0.2f;

    private static final long RESET_THRESHOLD_MS = 100;

    private final int windowSize;
    private final float alpha;
    private final Deque<ClassificationResult> window;

    private long lastInputMs;

    public EMASmoothing() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_ALPHA);
    }

    public EMASmoothing(int windowSize, float alpha) {
        this.windowSize = windowSize;
        this.alpha = alpha;
        this.window = new LinkedBlockingDeque<>(windowSize);
    }

    public ClassificationResult getSmoothedResult(ClassificationResult classificationResult) {
        // Resets memory if the duration between inputs too long
        long nowMs = SystemClock.elapsedRealtime();
        if (nowMs - lastInputMs > RESET_THRESHOLD_MS) {
            window.clear();
        }
        lastInputMs = nowMs;

        // If at window size, remove the last result.
        if (window.size() == windowSize) {
            window.pollLast();
        }
        // Insert at the beginning of the window.
        window.addFirst(classificationResult);

        Set<String> allClasses = new HashSet<>();
        for (ClassificationResult result : window) {
            allClasses.addAll(result.getAllClasses());
        }

        ClassificationResult smoothedResult = new ClassificationResult();

        for (String className : allClasses) {
            float factor = 1;
            float topSum = 0;
            float bottomSum = 0;
            for (ClassificationResult result : window) {
                float value = result.getClassConfidence(className);

                topSum += factor * value;
                bottomSum += factor;

                factor = (float) (factor * (1.0 - alpha));
            }
            smoothedResult.putClassConfidence(className, topSum / bottomSum);
        }

        return smoothedResult;
    }

}
