package daniel.southern.myptapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.common.internal.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Graphic overlay for UI
 */
public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    // Matrix for transforming from image coordinates to overlay coordinates.
    private final Matrix transformationMatrix = new Matrix();

    private int imageWidth;
    private int imageHeight;

    //used to scale image to fit the overlay view
    private float scaleFactor = 1.0f;

    // The number of horizontal pixels to be cropped to fit the image within the overlay View
    private float postScaleWidthOffset;

    // The number of vertical pixels to be cropped to fit the image within the overlay View
    private float postScaleHeightOffset;
    private boolean needUpdateTransformation = true;

    /**
     * Base class for a graphic object to be created within an overlay
     */
    public abstract static class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * Draws the graphic on the overlay
         * @param canvas the canvas which graphics can be drawn on
         */
        public abstract void draw(Canvas canvas);

        /** Adjusts the supplied value from the image scale to the view scale. */
        public float scale(float imagePixel) {
            return imagePixel * overlay.scaleFactor;
        }

        /**
         * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
         */
        public float translateX(float x) {
            return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
        }

        /**
         * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
         */
        public float translateY(float y) {
            return scale(y) - overlay.postScaleHeightOffset;
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        needUpdateTransformation = true);
    }

    /** Removes all graphics from the overlay. */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /** Adds a graphic to the overlay. */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    /**
     * Sets the source information of the image being processed to transform image coordinates
     *
     * @param imageWidth the width of the image sent to pose detector
     * @param imageHeight the height of the image sent to pose detector
     */
    public void setImageSourceInfo(int imageWidth, int imageHeight) {
        Preconditions.checkState(imageWidth > 0, "image width must be positive");
        Preconditions.checkState(imageHeight > 0, "image height must be positive");
        synchronized (lock) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            needUpdateTransformation = true;
        }
        postInvalidate();
    }

    /**
     * Transforms the image (cropping, scaling and translating) if needed
     */
    private void updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return;
        }
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) imageWidth / imageHeight;
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) getWidth() / imageWidth;
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) getHeight() / imageHeight;
            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
        }

        transformationMatrix.reset();
        transformationMatrix.setScale(scaleFactor, scaleFactor);
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);

        transformationMatrix.postScale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);


        needUpdateTransformation = false;
    }

    /** Draws the overlay with its graphic objects. */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            updateTransformationIfNeeded();

            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }
}
