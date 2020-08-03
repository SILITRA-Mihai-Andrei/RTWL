package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private String device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);
        initMap();
        device = (String) getIntent().getSerializableExtra("BT_DEVICE_SESSION_ID");
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync((OnMapReadyCallback) this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng my_location = new LatLng(47.60, 26.21);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(my_location, 15f));
        map.addMarker(new MarkerOptions().position(my_location).title("Marker in my location"));
        double distance = 0.005;
        googleMap.addPolygon(new PolygonOptions()
                .add(new LatLng(my_location.latitude-distance, my_location.longitude-distance),
                        new LatLng(my_location.latitude+distance, my_location.longitude-distance),
                        new LatLng(my_location.latitude+distance, my_location.longitude+distance),
                        new LatLng(my_location.latitude-distance, my_location.longitude+distance))
                .strokeColor(Color.argb(40, 0, 255, 0))
                .fillColor(Color.argb(32, 0, 255, 0))
                .strokeWidth(5f));

        LatLng pct1 = new LatLng(47.60, 26.22);
        map.addMarker(new MarkerOptions().position(pct1).title("Marker in my location"));
        googleMap.addPolygon(new PolygonOptions()
                .add(new LatLng(pct1.latitude-distance, pct1.longitude-distance),
                        new LatLng(pct1.latitude+distance, pct1.longitude-distance),
                        new LatLng(pct1.latitude+distance, pct1.longitude+distance),
                        new LatLng(pct1.latitude-distance, pct1.longitude+distance))
                .strokeColor(Color.argb(40, 0, 255, 0))
                .fillColor(Color.argb(32, 0, 255, 0))
                .strokeWidth(5f));
    }
}