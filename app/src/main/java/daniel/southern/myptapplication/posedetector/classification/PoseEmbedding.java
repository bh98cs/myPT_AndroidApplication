package daniel.southern.myptapplication.posedetector.classification;

import static daniel.southern.myptapplication.posedetector.classification.Equations.average;
import static daniel.southern.myptapplication.posedetector.classification.Equations.l2Norm2D;
import static daniel.southern.myptapplication.posedetector.classification.Equations.multiplyAll;
import static daniel.southern.myptapplication.posedetector.classification.Equations.subtract;
import static daniel.southern.myptapplication.posedetector.classification.Equations.subtractAll;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for embedding a list of pose landmarks
 */
public class PoseEmbedding {
    // Multiplier to apply to the torso to get minimal body size
    private static final float TORSO_MULTIPLIER = 2.5f;

    /**
     *
     * @param landmarks list of pose landmarks
     * @return list of landmarks after normalization
     */
    public static List<PointF3D> getPoseEmbedding(List<PointF3D> landmarks) {
        List<PointF3D> normalizedLandmarks = normalize(landmarks);
        return getEmbedding(normalizedLandmarks);
    }

    /**
     * Normalizes a list of pose landmarks
     * @param landmarks list of pose landmarks
     * @return list of normalized pose landmarks
     */
    private static List<PointF3D> normalize(List<PointF3D> landmarks) {
        List<PointF3D> normalizedLandmarks = new ArrayList<>(landmarks);
        // Normalize translation.
        PointF3D center = average(
                landmarks.get(PoseLandmark.LEFT_HIP), landmarks.get(PoseLandmark.RIGHT_HIP));
        subtractAll(center, normalizedLandmarks);

        // Normalize scale.
        multiplyAll(normalizedLandmarks, 1 / getPoseSize(normalizedLandmarks));
        // Multiplication by 100 is not required, but makes it easier to debug.
        multiplyAll(normalizedLandmarks, 100);
        return normalizedLandmarks;
    }

    /**
     * Uses 2D landmarks to calculate pose size
     * @param landmarks pose landmarks
     * @return pose size
     */
    private static float getPoseSize(List<PointF3D> landmarks) {
        PointF3D hipsCenter = average(
                landmarks.get(PoseLandmark.LEFT_HIP), landmarks.get(PoseLandmark.RIGHT_HIP));

        PointF3D shouldersCenter = average(
                landmarks.get(PoseLandmark.LEFT_SHOULDER),
                landmarks.get(PoseLandmark.RIGHT_SHOULDER));

        float torsoSize = l2Norm2D(subtract(hipsCenter, shouldersCenter));

        float maxDistance = torsoSize * TORSO_MULTIPLIER;
        for (PointF3D landmark : landmarks) {
            float distance = l2Norm2D(subtract(hipsCenter, landmark));
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        return maxDistance;
    }

    private static List<PointF3D> getEmbedding(List<PointF3D> lm) {
        List<PointF3D> embedding = new ArrayList<>();

        // group  distances by number of joints between the pairs
        embedding.add(subtract(
                average(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.RIGHT_HIP)),
                average(lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.RIGHT_SHOULDER))
        ));

        embedding.add(subtract(
                lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_ELBOW)));
        embedding.add(subtract(
                lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_ELBOW)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_ELBOW), lm.get(PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_ELBOW), lm.get(PoseLandmark.RIGHT_WRIST)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_KNEE)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_KNEE)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_KNEE), lm.get(PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_KNEE), lm.get(PoseLandmark.RIGHT_ANKLE)));

        // Two joints.
        embedding.add(subtract(
                lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(
                lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_WRIST)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_ANKLE)));

        // Four joints.
        embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_WRIST)));

        // Five joints.
        embedding.add(subtract(
                lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(
                lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_ANKLE)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_WRIST)));

        // Cross body.
        embedding.add(subtract(lm.get(PoseLandmark.LEFT_ELBOW), lm.get(PoseLandmark.RIGHT_ELBOW)));
        embedding.add(subtract(lm.get(PoseLandmark.LEFT_KNEE), lm.get(PoseLandmark.RIGHT_KNEE)));

        embedding.add(subtract(lm.get(PoseLandmark.LEFT_WRIST), lm.get(PoseLandmark.RIGHT_WRIST)));
        embedding.add(subtract(lm.get(PoseLandmark.LEFT_ANKLE), lm.get(PoseLandmark.RIGHT_ANKLE)));

        return embedding;
    }

}
