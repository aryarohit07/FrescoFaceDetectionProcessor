package com.rohitarya.fresco.facedetection.processor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.rohitarya.fresco.facedetection.processor.core.FrescoFaceDetector;

/**
 * Created by Rohit Arya (http://rohitarya.com) on 2/8/16.
 */
public class FaceCenterCrop extends BasePostprocessor {

    protected int width, height;

    public static final int PIXEL = 0;
    public static final int DP = 1;

    public FaceCenterCrop(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public FaceCenterCrop(int width, int height, int unit) {
        if (unit == PIXEL) {
            this.width = width;
            this.height = height;
        } else if (unit == DP) {
            Resources resources = FrescoFaceDetector.getContext().getResources();
            this.width = resources.getDimensionPixelSize(width);
            this.height = resources.getDimensionPixelSize(height);
        } else {
            throw new IllegalArgumentException("unit should either be FaceCenterCrop.PIXEL, FaceCenterCrop.DP");
        }
    }

    @Override
    public CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {

        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("width or height should not be zero!");
        }

        float scaleX = (float) width / sourceBitmap.getWidth();
        float scaleY = (float) height / sourceBitmap.getHeight();

        if (scaleX != scaleY) {

            Bitmap.Config config =
                    sourceBitmap.getConfig() != null ? sourceBitmap.getConfig() : Bitmap.Config.ARGB_8888;
            CloseableReference<Bitmap> bitmapRef = bitmapFactory.createBitmap(width, height, config);

            try {
                Bitmap destBitmap = bitmapRef.get();

                float scale = Math.max(scaleX, scaleY);

                float left = 0f;
                float top = 0f;

                float scaledWidth = width, scaledHeight = height;

                PointF focusPoint = new PointF();

                detectFace(sourceBitmap, focusPoint);

                if (scaleX < scaleY) {

                    scaledWidth = scale * sourceBitmap.getWidth();

                    float faceCenterX = scale * focusPoint.x;
                    left = getLeftPoint(width, scaledWidth, faceCenterX);

                } else {

                    scaledHeight = scale * sourceBitmap.getHeight();

                    float faceCenterY = scale * focusPoint.y;
                    top = getTopPoint(height, scaledHeight, faceCenterY);
                }

                RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
                Canvas canvas = new Canvas(destBitmap);
                canvas.drawBitmap(sourceBitmap, null, targetRect, null);

                return CloseableReference.cloneOrNull(bitmapRef);
            } catch (Exception e) {
                e.printStackTrace();
                return super.process(sourceBitmap, bitmapFactory);
            } finally {
                CloseableReference.closeSafely(bitmapRef);
            }
        } else {
            return super.process(sourceBitmap, bitmapFactory);
        }
    }

    /**
     * Calculates a point (focus point) in the bitmap, around which cropping needs to be performed.
     *
     * @param bitmap           Bitmap in which faces are to be detected.
     * @param centerOfAllFaces To store the center point.
     */
    private void detectFace(Bitmap bitmap, PointF centerOfAllFaces) {
        FaceDetector faceDetector = FrescoFaceDetector.getFaceDetector();
        if (!faceDetector.isOperational()) {
            centerOfAllFaces.set(bitmap.getWidth() / 2, bitmap.getHeight() / 2); // center crop
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        final int totalFaces = faces.size();
        if (totalFaces > 0) {
            float sumX = 0f;
            float sumY = 0f;
            for (int i = 0; i < totalFaces; i++) {
                PointF faceCenter = new PointF();
                getFaceCenter(faces.get(faces.keyAt(i)), faceCenter);
                sumX = sumX + faceCenter.x;
                sumY = sumY + faceCenter.y;
            }
            centerOfAllFaces.set(sumX / totalFaces, sumY / totalFaces);
            return;
        }
        centerOfAllFaces.set(bitmap.getWidth() / 2, bitmap.getHeight() / 2); // center crop
    }

    /**
     * Calculates center of a given face
     *
     * @param face   Face
     * @param center Center of the face
     */
    private void getFaceCenter(Face face, PointF center) {
        float x = face.getPosition().x;
        float y = face.getPosition().y;
        float width = face.getWidth();
        float height = face.getHeight();
        center.set(x + (width / 2), y + (height / 2)); // face center in original bitmap
    }

    private float getTopPoint(int height, float scaledHeight, float faceCenterY) {
        if (faceCenterY <= height / 2) { // Face is near the top edge
            return 0f;
        } else if ((scaledHeight - faceCenterY) <= height / 2) { // face is near bottom edge
            return height - scaledHeight;
        } else {
            return (height / 2) - faceCenterY;
        }
    }

    private float getLeftPoint(int width, float scaledWidth, float faceCenterX) {
        if (faceCenterX <= width / 2) { // face is near the left edge.
            return 0f;
        } else if ((scaledWidth - faceCenterX) <= width / 2) {  // face is near right edge
            return (width - scaledWidth);
        } else {
            return (width / 2) - faceCenterX;
        }
    }
}
