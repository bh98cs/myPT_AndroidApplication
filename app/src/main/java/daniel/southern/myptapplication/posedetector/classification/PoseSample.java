package daniel.southern.myptapplication.posedetector.classification;

import android.util.Log;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.base.Splitter;
import com.google.mlkit.vision.common.PointF3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to read a CSV file containing pose samples
 */
public class PoseSample {
    private static final String TAG = "PoseSample";
    //number of landmarks in the pose
    private static final int NUM_LANDMARKS = 33;
    private static final int NUM_DIMS = 3;

    private final String name;
    private final String className;
    private final List<PointF3D> embedding;

    /**
     * {@link PoseSample} class constructor
     * @param name name
     * @param className class name
     * @param landmarks pose landmarks
     */
    public PoseSample(String name, String className, List<PointF3D> landmarks) {
        this.name = name;
        this.className = className;
        this.embedding = PoseEmbedding.getPoseEmbedding(landmarks);
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public List<PointF3D> getEmbedding() {
        return embedding;
    }

    /**
     * Reads a string from the CSV data file to retrieve pose samples
     * @param csvLine the line read from the CSV file
     * @param separator the separator between values (in this case a comma)
     * @return {@link PoseSample} from the values given in the CSV line
     */
    public static PoseSample getPoseSample(String csvLine, String separator) {
        List<String> values = Splitter.onPattern(separator).splitToList(csvLine);
        // Format is expected to be Name,Class,X1,Y1,Z1,X2,Y2,Z2...
        // + 2 is for Name & Class.
        if (values.size() != (NUM_LANDMARKS * NUM_DIMS) + 2) {
            Log.e(TAG, "Invalid number of tokens for PoseSample");
            return null;
        }
        String name = values.get(0);
        String className = values.get(1);
        List<PointF3D> landmarks = new ArrayList<>();
        // Read from the third attribute, first 2 tokens are name and class.
        for (int i = 2; i < values.size(); i += NUM_DIMS) {
            try {
                landmarks.add(
                        PointF3D.from(
                                Float.parseFloat(values.get(i)),
                                Float.parseFloat(values.get(i + 1)),
                                Float.parseFloat(values.get(i + 2))));
            } catch (NullPointerException | NumberFormatException e) {
                Log.e(TAG, "Invalid value " + values.get(i) + " for landmark position.");
                return null;
            }
        }
        return new PoseSample(name, className, landmarks);
    }

}
