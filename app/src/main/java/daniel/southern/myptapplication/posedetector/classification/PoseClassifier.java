package daniel.southern.myptapplication.posedetector.classification;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static daniel.southern.myptapplication.posedetector.classification.Equations.maxAbs;
import static daniel.southern.myptapplication.posedetector.classification.Equations.multiply;
import static daniel.southern.myptapplication.posedetector.classification.Equations.multiplyAll;
import static daniel.southern.myptapplication.posedetector.classification.Equations.subtract;
import static daniel.southern.myptapplication.posedetector.classification.Equations.sumAbs;
import static daniel.southern.myptapplication.posedetector.classification.PoseEmbedding.getPoseEmbedding;

import android.util.Pair;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Classifies {@link Pose} using the data given in {@link PoseSample}.
 * Uses K-nearest neighbour to classify poses
 */
public class PoseClassifier {
    // values for calculating K-nearest neighbour
    private static final int MAX_DISTANCE_TOP_K = 30;
    private static final int MEAN_DISTANCE_TOP_K = 10;
    // weightings for axis. Z given a weight of 0
    private static final PointF3D AXES_WEIGHTS = PointF3D.from(1, 1, 0);

    //list of poses provided in the dataset
    private final List<PoseSample> poseSamples;
    private final int maxDistanceTopK;
    private final int meanDistanceTopK;
    private final PointF3D axesWeights;

    /**
     * {@link PoseClassifier} class constructor
     * @param poseSamples list of poses recognised
     */
    public PoseClassifier(List<PoseSample> poseSamples) {
        this(poseSamples, MAX_DISTANCE_TOP_K, MEAN_DISTANCE_TOP_K, AXES_WEIGHTS);
    }

    /**
     *
     * @param poseSamples list of poses recognised
     * @param maxDistanceTopK (used for pose classification)
     * @param meanDistanceTopK (used for pose classification)
     * @param axesWeights weightings for axis
     */
    public PoseClassifier(List<PoseSample> poseSamples, int maxDistanceTopK,
                          int meanDistanceTopK, PointF3D axesWeights) {
        this.poseSamples = poseSamples;
        this.maxDistanceTopK = maxDistanceTopK;
        this.meanDistanceTopK = meanDistanceTopK;
        this.axesWeights = axesWeights;
    }

    /**
     * Retrieves a list of all pose landmarks from a {@link Pose}
     * @param pose the pose which all landmarks are to be retrieved for
     * @return a list of all landmarks within the detected pose
     */
    private static List<PointF3D> extractPoseLandmarks(Pose pose) {
        List<PointF3D> landmarks = new ArrayList<>();
        for (PoseLandmark poseLandmark : pose.getAllPoseLandmarks()) {
            landmarks.add(poseLandmark.getPosition3D());
        }
        return landmarks;
    }

    public ClassificationResult classify(Pose pose) {
        return classify(extractPoseLandmarks(pose));
    }

    /**
     * Classification based on a list of landmarks of the human body using K nearest neighbour
     * @param landmarks list of landmarks for the body
     * @return {@link ClassificationResult} which includes the most confidently identified exercise
      */
    public ClassificationResult classify(List<PointF3D> landmarks) {
        ClassificationResult result = new ClassificationResult();
        // Return no landmarks detected.
        if (landmarks.isEmpty()) {
            return result;
        }

        // We do flipping on X-axis to ensure is mirror invariant.
        List<PointF3D> flippedLandmarks = new ArrayList<>(landmarks);
        multiplyAll(flippedLandmarks, PointF3D.from(-1, 1, 1));

        List<PointF3D> embedding = getPoseEmbedding(landmarks);
        List<PointF3D> flippedEmbedding = getPoseEmbedding(flippedLandmarks);

        // pick top-K samples by max distance
        // Keeps max distance on top to pop it when top_k size is reached.
        PriorityQueue<Pair<PoseSample, Float>> maxDistances = new PriorityQueue<>(
                maxDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));
        // Retrieve top K poseSamples by least distance to remove outliers.
        for (PoseSample poseSample : poseSamples) {
            List<PointF3D> sampleEmbedding = poseSample.getEmbedding();

            float originalMax = 0;
            float flippedMax = 0;
            for (int i = 0; i < embedding.size(); i++) {
                originalMax =
                        max(
                                originalMax,
                                maxAbs(multiply(subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights)));
                flippedMax =
                        max(
                                flippedMax,
                                maxAbs(
                                        multiply(
                                                subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights)));
            }
            // Set the max distance as min of original and flipped max distance.
            maxDistances.add(new Pair<>(poseSample, min(originalMax, flippedMax)));
            // pop the highest distance to retain top n
            if (maxDistances.size() > maxDistanceTopK) {
                maxDistances.poll();
            }
        }

        // pick top-K samples by mean distance
        // Keeps higher mean distances on top to pop it when top_k size is reached.
        PriorityQueue<Pair<PoseSample, Float>> meanDistances = new PriorityQueue<>(
                meanDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));
        // Retrieve top K poseSamples by least mean distance to remove outliers.
        for (Pair<PoseSample, Float> sampleDistances : maxDistances) {
            PoseSample poseSample = sampleDistances.first;
            List<PointF3D> sampleEmbedding = poseSample.getEmbedding();

            float originalSum = 0;
            float flippedSum = 0;
            for (int i = 0; i < embedding.size(); i++) {
                originalSum += sumAbs(multiply(
                        subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights));
                flippedSum += sumAbs(
                        multiply(subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights));
            }
            // Set the mean distance as min of original and flipped mean distances.
            float meanDistance = min(originalSum, flippedSum) / (embedding.size() * 2);
            meanDistances.add(new Pair<>(poseSample, meanDistance));
            // retain top k to pop the highest mean distance.
            if (meanDistances.size() > meanDistanceTopK) {
                meanDistances.poll();
            }
        }

        for (Pair<PoseSample, Float> sampleDistances : meanDistances) {
            String className = sampleDistances.first.getClassName();
            result.incrementClassConfidence(className);
        }

        return result;
    }

}
