package daniel.southern.myptapplication.posedetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

import daniel.southern.myptapplication.GraphicOverlay;

public class PoseGraphic extends GraphicOverlay.Graphic {

    private static final float STROKE_WIDTH = 10.0f;
    //private static final float POSE_CLASSIFICATION_TEXT_SIZE = 60.0f;
    //private final List<String> poseClassification;
    private final Pose pose;
    //private final Paint classificationTextPaint;
    private final Paint whitePaint;

    PoseGraphic(
            GraphicOverlay overlay,
            Pose pose) {
        super(overlay);
        this.pose = pose;

        whitePaint = new Paint();
        whitePaint.setStrokeWidth(STROKE_WIDTH);
        whitePaint.setColor(Color.WHITE);
    }

    @Override
    public void draw(Canvas canvas) {
        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
        if (landmarks.isEmpty()) {
            return;
        }

        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
        PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
        PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
        PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        PoseLandmark leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
        PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
        PoseLandmark leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
        PoseLandmark rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
        PoseLandmark leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
        PoseLandmark rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);

        drawLine(canvas, leftShoulder, rightShoulder, whitePaint);
        drawLine(canvas, leftHip, rightHip, whitePaint);

        // Left body
        drawLine(canvas, leftShoulder, leftElbow, whitePaint);
        drawLine(canvas, leftElbow, leftWrist, whitePaint);
        drawLine(canvas, leftShoulder, leftHip, whitePaint);
        drawLine(canvas, leftHip, leftKnee, whitePaint);
        drawLine(canvas, leftKnee, leftAnkle, whitePaint);
        drawLine(canvas, leftWrist, leftThumb, whitePaint);
        drawLine(canvas, leftWrist, leftPinky, whitePaint);
        drawLine(canvas, leftWrist, leftIndex, whitePaint);
        drawLine(canvas, leftIndex, leftPinky, whitePaint);
        drawLine(canvas, leftAnkle, leftHeel, whitePaint);
        drawLine(canvas, leftHeel, leftFootIndex, whitePaint);

        // Right body
        drawLine(canvas, rightShoulder, rightElbow, whitePaint);
        drawLine(canvas, rightElbow, rightWrist, whitePaint);
        drawLine(canvas, rightShoulder, rightHip, whitePaint);
        drawLine(canvas, rightHip, rightKnee, whitePaint);
        drawLine(canvas, rightKnee, rightAnkle, whitePaint);
        drawLine(canvas, rightWrist, rightThumb, whitePaint);
        drawLine(canvas, rightWrist, rightPinky, whitePaint);
        drawLine(canvas, rightWrist, rightIndex, whitePaint);
        drawLine(canvas, rightIndex, rightPinky, whitePaint);
        drawLine(canvas, rightAnkle, rightHeel, whitePaint);
        drawLine(canvas, rightHeel, rightFootIndex, whitePaint);

    }

    void drawLine(Canvas canvas, PoseLandmark startLandmark, PoseLandmark endLandmark, Paint paint) {
        PointF3D start = startLandmark.getPosition3D();
        PointF3D end = endLandmark.getPosition3D();

        canvas.drawLine(
                translateX(start.getX()),
                translateY(start.getY()),
                translateX(end.getX()),
                translateY(end.getY()),
                paint);
    }
}
