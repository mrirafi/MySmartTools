package com.meghpy.mysmarttools;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class DraggableImageView extends AppCompatImageView {

    private float dX, dY;
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;

    public DraggableImageView(Context context) {
        super(context);
        init(context);
    }

    public DraggableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setScaleType(ScaleType.FIT_CENTER); // ✅ Shows full original image aspect ratio
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dX = getX() - event.getRawX();
                dY = getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    setX(event.getRawX() + dX);
                    setY(event.getRawY() + dY);
                }
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.2f, Math.min(scaleFactor, 5.0f));
            setScaleX(scaleFactor);
            setScaleY(scaleFactor);
            return true;
        }
    }

    public float getScaleFactor() {
        return scaleFactor;
    }
}
