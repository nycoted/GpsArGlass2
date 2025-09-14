package com.google.android.glass.touchpad;

import android.content.Context;
import android.view.MotionEvent;

public class GestureDetector {

    private BaseListener baseListener;
    private FingerListener fingerListener;
    private ScrollListener scrollListener;

    public GestureDetector(Context context) {
        // Stub
    }

    public void setBaseListener(BaseListener listener) {
        this.baseListener = listener;
    }

    public void setFingerListener(FingerListener listener) {
        this.fingerListener = listener;
    }

    public void setScrollListener(ScrollListener listener) {
        this.scrollListener = listener;
    }

    public boolean onMotionEvent(MotionEvent event) {
        // Simule toujours "false" → pas de geste réel capté
        return false;
    }

    // Interfaces
    public interface BaseListener {
        boolean onGesture(Gesture gesture);
    }

    public interface FingerListener {
        void onFingerCountChanged(int previousCount, int currentCount);
    }

    public interface ScrollListener {
        boolean onScroll(float displacement, float delta, float velocity);
    }
}