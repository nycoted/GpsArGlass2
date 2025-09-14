package com.google.android.glass.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SwipeDismissFrameLayout extends FrameLayout {
    public SwipeDismissFrameLayout(Context context) {
        super(context);
    }

    public SwipeDismissFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeDismissFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnDismissedListener {
        void onDismissed(SwipeDismissFrameLayout layout);
    }

    public void setOnDismissedListener(OnDismissedListener listener) {
        // Stub: ne fait rien
    }
}