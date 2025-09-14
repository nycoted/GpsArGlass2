package com.google.gpsarglass;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xwalk.core.JavascriptInterface;
import org.xwalk.core.XWalkInitializer;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class MapsActivity extends Activity implements XWalkInitializer.XWalkInitListener {

    private XWalkView xwalkMapView;
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
        setContentView(R.layout.activity_maps);

        xwalkMapView = (XWalkView) findViewById(R.id.xwalkMapView);

        // ⚡ Debug + JS
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        xwalkMapView.getSettings().setJavaScriptEnabled(true);
        xwalkMapView.getSettings().setDomStorageEnabled(true);   // nécessaire
        xwalkMapView.getSettings().setDatabaseEnabled(true);
        xwalkMapView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        xwalkMapView.getSettings().setAllowFileAccessFromFileURLs(true);

        // ⚡ Forcer un User-Agent moderne
        xwalkMapView.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 9; Glass) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36"
        );

        // ✅ Logger les erreurs et états de chargement
        xwalkMapView.setResourceClient(new XWalkResourceClient(xwalkMapView) {
            @Override
            public void onReceivedLoadError(XWalkView view, int errorCode,
                                            String description, String failingUrl) {
                Log.e("Crosswalk", "Maps error: " + description + " @ " + failingUrl);
                Toast.makeText(MapsActivity.this,
                        "Erreur Crosswalk: " + description,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onLoadStarted(XWalkView view, String url) {
                Log.i("Crosswalk", "▶️ Start load: " + url);
            }

            @Override
            public void onLoadFinished(XWalkView view, String url) {
                Log.i("Crosswalk", "✅ Finish load: " + url);
            }
        });

        // ✅ Bridge JS <-> Java
        xwalkMapView.addJavascriptInterface(new MapJSBridge(), "Android");

        // ✅ Charger map.html avec clé API
        try {
            InputStream is = getAssets().open("map.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            String apiKey = getString(R.string.google_maps_key);
            String html = sb.toString().replace("%API_KEY%", apiKey);

            // ⚠️ IMPORTANT : utiliser HTTPS comme baseURL
            xwalkMapView.loadDataWithBaseURL(
                    "https://local.gpsarglass/",
                    html,
                    "text/html",
                    "utf-8",
                    null
            );

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Erreur chargement map.html", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- JS Bridge ----------------
    private class MapJSBridge {
        @JavascriptInterface
        public void openStreetView(double lat, double lng) {
            Intent i = new Intent(MapsActivity.this, StreetViewActivity.class);
            i.putExtra("lat", lat);
            i.putExtra("lng", lng);
            startActivity(i);
        }
    }

    // ---------------- Transit ----------------
    private void fetchTransitInfo(String origin, String destination, String mode) {
        try {
            String apiKey = getString(R.string.google_maps_key);
            String url = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin
                    + "&destination=" + destination
                    + "&mode=transit&transit_mode=" + mode
                    + "&key=" + apiKey;
            new FetchTransitTask().execute(url);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur préparation requête transit", Toast.LENGTH_SHORT).show();
        }
    }

    private class FetchTransitTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String json) {
            try {
                JSONObject obj = new JSONObject(json);
                JSONArray routes = obj.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject leg = routes.getJSONObject(0)
                            .getJSONArray("legs").getJSONObject(0);

                    String departure = leg.has("departure_time")
                            ? leg.getJSONObject("departure_time").getString("text")
                            : "Non disponible";
                    String arrival = leg.has("arrival_time")
                            ? leg.getJSONObject("arrival_time").getString("text")
                            : "Non disponible";

                    Toast.makeText(MapsActivity.this,
                            "🚍 Départ : " + departure + "\n🏁 Arrivée : " + arrival,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MapsActivity.this, "Aucun trajet trouvé", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MapsActivity.this, "Erreur parsing horaires", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------- Decode Polyline ----------------
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    // ---------------- Nettoyage ----------------
    @Override
    protected void onDestroy() {
        if (xwalkMapView != null) {
            xwalkMapView.removeJavascriptInterface("Android");
            xwalkMapView.onDestroy();
        }
        super.onDestroy();
    }
}

