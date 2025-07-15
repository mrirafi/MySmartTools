package com.meghpy.mysmarttools;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class DraggableTextView extends androidx.appcompat.widget.AppCompatTextView {

    private float dX, dY;
    private boolean isSelected = false;

    private OnSelectedListener onSelectedListener;

    public DraggableTextView(Context context) {
        super(context);
        init();
    }

    public DraggableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setTextColor(Color.BLACK);
        setTextSize(18);
        setBackgroundColor(Color.TRANSPARENT);
        setPadding(10, 10, 10, 10);
        setInputType(InputType.TYPE_CLASS_TEXT);

        // Make clickable and focusable for touch handling
        setClickable(true);
        setFocusable(true);

        // Handle drag and click
        setOnTouchListener(new OnTouchListener() {
            private long lastDownTime = 0;
            private static final int CLICK_DELAY = 200;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastDownTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - lastDownTime;
                        if (clickDuration < CLICK_DELAY) {
                            // It's a click, open edit dialog and notify selected
                            showEditDialog();
                            notifySelected();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void notifySelected() {
        if (onSelectedListener != null) {
            onSelectedListener.onSelected(this);
        }
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        this.onSelectedListener = listener;
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Text");

        final EditText input = new EditText(getContext());
        input.setText(getText());
        input.setSelection(getText().length());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            setText(input.getText().toString());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public void increaseTextSize() {
        float currentSizeSp = getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        float newSize = currentSizeSp + 2f;
        setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize);
    }

    public void decreaseTextSize() {
        float currentSizeSp = getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        float newSize = currentSizeSp - 2f;
        if (newSize < 6f) newSize = 6f;  // minimum size in sp
        setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize);
    }


    public interface OnSelectedListener {
        void onSelected(DraggableTextView textView);
    }
}
