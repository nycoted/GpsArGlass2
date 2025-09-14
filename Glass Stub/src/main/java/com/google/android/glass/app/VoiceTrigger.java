package com.google.android.glass.app;

public class VoiceTrigger {
    private String command;

    public VoiceTrigger(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}