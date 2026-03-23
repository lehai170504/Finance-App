package com.example.homiefinanceapp.views;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private ScaleGestureDetector scaleDetector;

    private float minScale = 1f;
    // Giới hạn zoom vừa đủ để xem rõ trên màn hình điện thoại, tránh phóng quá đà.
    private float maxScale = 2f;
    private float currentScale = 1f;
    private float lastX;
    private float lastY;
    private boolean isDragging;

    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(matrix);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitImageToView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    matrix.postTranslate(dx, dy);
                    fixTranslation();
                    setImageMatrix(matrix);
                    lastX = event.getX();
                    lastY = event.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float nextScale = currentScale * scaleFactor;

            if (nextScale > maxScale) {
                scaleFactor = maxScale / currentScale;
                nextScale = maxScale;
            } else if (nextScale < minScale) {
                scaleFactor = minScale / currentScale;
                nextScale = minScale;
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            currentScale = nextScale;
            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }
    }

    private void fitImageToView() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float drawableWidth = getDrawable().getIntrinsicWidth();
        float drawableHeight = getDrawable().getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) return;

        matrix.reset();
        float scale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * scale) * 0.5f;
        float dy = (viewHeight - drawableHeight * scale) * 0.5f;
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        currentScale = 1f;
        minScale = 1f;
        setImageMatrix(matrix);
    }

    private void fixTranslation() {
        if (getDrawable() == null) return;
        matrix.getValues(values);
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        float contentWidth = getDrawable().getIntrinsicWidth() * scaleX;
        float contentHeight = getDrawable().getIntrinsicHeight() * scaleY;
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float minX = Math.min(0f, viewWidth - contentWidth);
        float maxX = Math.max(0f, viewWidth - contentWidth);
        float minY = Math.min(0f, viewHeight - contentHeight);
        float maxY = Math.max(0f, viewHeight - contentHeight);

        float clampedX = Math.max(minX, Math.min(transX, maxX));
        float clampedY = Math.max(minY, Math.min(transY, maxY));
        matrix.postTranslate(clampedX - transX, clampedY - transY);
    }
}

