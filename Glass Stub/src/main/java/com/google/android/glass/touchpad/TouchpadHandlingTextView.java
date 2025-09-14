package com.google.android.glass.touchpad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.Gesture;
// Import the correct GestureDetector from the Google Glass GDK
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.Gesture;

public class TouchpadHandlingTextView extends TextView {

    private final GestureDetector mTouchDetector;

    public TouchpadHandlingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchDetector = createGestureDetector(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public TouchpadHandlingTextView(Context context) {
        this(context, null);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (isFocused()) {
            return mTouchDetector.onMotionEvent(event);
        }
        return super.onGenericMotionEvent(event);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    performClick();
                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }
}