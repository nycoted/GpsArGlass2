package com.google.android.glass.timeline;

import android.content.Context;

public class TimelineManager {

    private static TimelineManager instance;

    private TimelineManager(Context context) {
        // Stub
    }

    public static TimelineManager from(Context context) {
        if (instance == null) {
            instance = new TimelineManager(context);
        }
        return instance;
    }

    public void insertCard(String text) {
        // Stub → afficherait normalement une carte dans la timeline des Glass
    }
}

