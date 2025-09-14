package com.google.android.glass.eye;

/**
 * Stub de EyeGestureManager.
 * Dans Glass XE, il servait à détecter les gestes de l’œil.
 * Ici, c’est uniquement pour que le projet compile.
 */
public class EyeGestureManager {

    /** Interface pour recevoir les événements de l'œil. */
    public interface Listener {
        void onDetected(EyeGesture eyeGesture);
    }

    private EyeGestureManager() {
        // Stub : pas d’implémentation réelle
    }

    /** Retourne une instance fictive du manager. */
    public static EyeGestureManager from(Object context) {
        return new EyeGestureManager();
    }

    /** Enregistre un listener pour un geste donné. */
    public void register(EyeGesture gesture, Listener listener) {
        // Stub : aucune détection réelle
    }

    /** Désenregistre un listener. */
    public void unregister(EyeGesture gesture) {
        // Stub
    }
}