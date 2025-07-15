package com.meghpy.mysmarttools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingOverlayView extends View {

    public enum Mode {
        DRAW,
        ERASE,
        NONE
    }

    private Bitmap drawingBitmap;
    private Canvas drawingCanvas;

    private Path currentPath;
    private Paint drawPaint;
    private Paint erasePaint;

    private Mode currentMode = Mode.NONE;

    public DrawingOverlayView(Context context) {
        super(context);
        init();
    }

    public DrawingOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setColor(0xFFFF0000);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(6);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeWidth(50); // Eraser size
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawingCanvas = new Canvas(drawingBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(drawingBitmap, 0, 0, null);
        if (currentPath != null && currentMode == Mode.DRAW) {
            canvas.drawPath(currentPath, drawPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (currentMode == Mode.DRAW) {
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                } else if (currentMode == Mode.ERASE) {
                    drawingCanvas.drawCircle(x, y, erasePaint.getStrokeWidth() / 2, erasePaint);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentMode == Mode.DRAW && currentPath != null) {
                    currentPath.lineTo(x, y);
                } else if (currentMode == Mode.ERASE) {
                    drawingCanvas.drawCircle(x, y, erasePaint.getStrokeWidth() / 2, erasePaint);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentMode == Mode.DRAW && currentPath != null) {
                    drawingCanvas.drawPath(currentPath, drawPaint);
                    currentPath = null;
                }
                break;
        }

        invalidate();
        return true;
    }

    public Bitmap getDrawingBitmap() {
        return drawingBitmap;
    }

    public void clear() {
        drawingBitmap.eraseColor(0x00000000);
        invalidate();
    }
}