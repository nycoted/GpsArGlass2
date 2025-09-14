package com.google.gpsarglass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView gpsInfo;
    private ImageView arrow;
    private ImageView logo;
    private GestureDetector gestureDetector;
    private EyeGestureManager eyeGestureManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Récupération des vues
        webView = (WebView) findViewById(R.id.webview);
        gpsInfo = (TextView) findViewById(R.id.gpsInfo);
        arrow = (ImageView) findViewById(R.id.arrow);
        logo = (ImageView) findViewById(R.id.logo);

        // 🔹 Configurer WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.loadUrl("https://www.google.com/maps/@48.8588443,2.2943506,3a,75y,90t");

        // 🔹 Détecteur de gestes
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 🔹 Geste œil (clin d’œil → ouvre la caméra)
        eyeGestureManager = EyeGestureManager.from(this);
        if (eyeGestureManager != null) {
            eyeGestureManager.register(EyeGesture.WINK, new EyeGestureManager.Listener() {
                @Override
                public void onDetected(EyeGesture eyeGesture) {
                    Toast.makeText(MainActivity.this,
                            "Clin d’œil détecté : ouverture caméra",
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivity(intent);
                }
            });
        }
    }

    // 🔹 Gère les événements tactiles
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    // 🔹 Gestion des Intents entrants
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if ("com.example.ACTION_BUS_TIME".equals(action)) {
            gpsInfo.setText("Bus 72 dans 5 minutes");

        } else if ("com.example.ACTION_LOCATION".equals(action)) {
            webView.loadUrl("https://maps.google.com/maps?q=ma_position&layer=sv");

        } else if ("com.example.ACTION_ZOOM_IN".equals(action)) {
            webView.zoomIn();

        } else if ("com.example.ACTION_ZOOM_OUT".equals(action)) {
            webView.zoomOut();

        } else if ("com.example.ACTION_GO_TO".equals(action)) {
            String destination = intent.getStringExtra("destination");
            if (destination != null) {
                gpsInfo.setText("Navigation vers : " + destination);
                // 🔹 URL correcte pour lancer Google Maps Directions
                webView.loadUrl("https://www.google.com/maps/dir/?api=1&destination=" + destination);
            }
        }
    }

    // 🔹 Gestion des gestes tactiles
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Toast.makeText(MainActivity.this, "Tap détecté", Toast.LENGTH_SHORT).show();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Toast.makeText(MainActivity.this,
                    "Double-tap : recentrage GPS", Toast.LENGTH_SHORT).show();
            webView.loadUrl("https://maps.google.com/maps?q=ma_position&layer=sv");
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {

            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // 🔹 Swipe horizontal → rotation flèche
                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        arrow.setRotation(90);
                        Toast.makeText(MainActivity.this, "Swipe droite", Toast.LENGTH_SHORT).show();
                    } else {
                        arrow.setRotation(270);
                        Toast.makeText(MainActivity.this, "Swipe gauche", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

            } else {
                // 🔹 Swipe vertical
                if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffY > 0) {
                        Toast.makeText(MainActivity.this,
                                "Swipe bas : quitter l'application", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        webView.zoomIn();
                        Toast.makeText(MainActivity.this,
                                "Swipe haut : zoom in", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}