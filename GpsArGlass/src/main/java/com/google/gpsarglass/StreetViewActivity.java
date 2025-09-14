package com.google.gpsarglass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.xwalk.core.JavascriptInterface;
import org.xwalk.core.XWalkInitializer;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class StreetViewActivity extends Activity implements XWalkInitializer.XWalkInitListener {

    private XWalkView xwalkStreetView;
    private TextView txtInfo;
    private double lat;
    private double lng;
    private XWalkInitializer xWalkInitializer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⚡ Forcer TLS 1.2 (important pour Android 4.4 / Glass)
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Init Crosswalk
        xWalkInitializer = new XWalkInitializer(this, this);
        xWalkInitializer.initAsync();
    }

    // ---------------- Crosswalk lifecycle ----------------
    @Override
    public void onXWalkInitStarted() {}

    @Override
    public void onXWalkInitCancelled() {}

    @Override
    public void onXWalkInitFailed() {
        Toast.makeText(this, "❌ Erreur init Crosswalk", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onXWalkInitCompleted() {
        setContentView(R.layout.activity_streetview);

        // Initialisation des vues
        xwalkStreetView = (XWalkView) findViewById(R.id.xwalkStreetView);
        txtInfo = (TextView) findViewById(R.id.txtInfo);

        // ⚡ Activer JS + Debug
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        xwalkStreetView.getSettings().setJavaScriptEnabled(true);
        xwalkStreetView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        xwalkStreetView.getSettings().setAllowFileAccessFromFileURLs(true);

        // Logger les erreurs de chargement
        xwalkStreetView.setResourceClient(new XWalkResourceClient(xwalkStreetView) {
            @Override
            public void onReceivedLoadError(XWalkView view, int errorCode,
                                            String description, String failingUrl) {
                Log.e("Crosswalk", "StreetView error: " + description + " @ " + failingUrl);
                Toast.makeText(StreetViewActivity.this,
                        "Erreur Crosswalk: " + description,
                        Toast.LENGTH_LONG).show();
            }
        });

        // ⚡ JS <-> Java Bridge
        xwalkStreetView.addJavascriptInterface(new JSBridge(), "Android");

        // ⚡ Récup coords envoyées depuis MapsActivity
        lat = getIntent().getDoubleExtra("lat", 48.8584); // Tour Eiffel par défaut
        lng = getIntent().getDoubleExtra("lng", 2.2945);

        // Charger streetview.html avec clé API + coords
        try {
            InputStream is = getAssets().open("streetview.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            String apiKey = getString(R.string.google_maps_key);
            String html = sb.toString()
                    .replace("%API_KEY%", apiKey)
                    .replace("%LAT%", String.valueOf(lat))
                    .replace("%LNG%", String.valueOf(lng));

            xwalkStreetView.loadDataWithBaseURL("file:///android_asset/",
                    html, "text/html", "utf-8", null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Erreur chargement streetview.html", Toast.LENGTH_LONG).show();
        }

        // ✅ Confirmation utilisateur
        Toast toast = Toast.makeText(this, "✅ Street View chargé", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // ---------------- JS Bridge ----------------
    private class JSBridge {
        @JavascriptInterface
        public void onPositionChanged(String latLng) {
            try {
                if (latLng == null) return;
                String[] parts = latLng.split(",");
                if (parts.length < 2) return;
                final double newLat = Double.parseDouble(parts[0]);
                final double newLng = Double.parseDouble(parts[1]);

                // Broadcast vers MapsActivity
                Intent intent = new Intent("com.google.gpsarglass.UPDATE_MAP");
                intent.putExtra("lat", newLat);
                intent.putExtra("lng", newLng);
                sendBroadcast(intent);

                runOnUiThread(() -> {
                    if (txtInfo != null) {
                        txtInfo.setText(String.format(Locale.getDefault(),
                                "📍 Position: %.6f, %.6f", newLat, newLng));
                    }
                    Toast.makeText(StreetViewActivity.this,
                            "📌 Position mise à jour",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void onPOVChanged(String headingPitch) {
            try {
                if (headingPitch == null) return;
                String[] p = headingPitch.split(",");
                if (p.length < 2) return;
                final float heading = Float.parseFloat(p[0]);
                final float pitch = Float.parseFloat(p[1]);

                // Broadcast vers MapsActivity
                Intent intent = new Intent("com.google.gpsarglass.UPDATE_MAP");
                intent.putExtra("heading", heading);
                intent.putExtra("pitch", pitch);
                sendBroadcast(intent);

                runOnUiThread(() -> {
                    if (txtInfo != null) {
                        txtInfo.setText(String.format(Locale.getDefault(),
                                "🎥 Caméra: heading=%.1f°, pitch=%.1f°",
                                heading, pitch));
                    }
                    Toast.makeText(StreetViewActivity.this,
                            "🎥 POV mis à jour",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------- Nettoyage ----------------
    @Override
    protected void onDestroy() {
        if (xwalkStreetView != null) {
            xwalkStreetView.removeJavascriptInterface("Android");
            xwalkStreetView.onDestroy();
        }
        super.onDestroy();
    }
}

