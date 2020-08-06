package com.example.realtimeweatherlocationtrafficsystem;

import androidx.fragment.app.FragmentActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, FireBaseManager.onFireBaseDataNew, GoogleMap.OnMarkerClickListener {

    private final int INDEX_FOR_THE_MOST_COMMON_DATA = 0;

    private GoogleMap map;
    private String device;
    private List<String> weatherString;
    private List<Integer> markerIcons;
    private FireBaseManager fireBaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);
        initMap();
        initWeatherString();
        initMarkerIcons();
        device = (String) getIntent().getSerializableExtra("BT_DEVICE_SESSION_ID");
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);
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
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            public View getInfoWindow(Marker marker) {
                View window = getLayoutInflater().inflate(R.layout.item_map_windows_info, null);
                Region region = fireBaseManager.getRegion(Integer.parseInt(marker.getId().substring(1))); //Marker id format: "m1, m2, ..., m10"
                TextView coordinates = window.findViewById(R.id.coordinates);
                TextView weather = window.findViewById(R.id.weather);
                TextView temperature = window.findViewById(R.id.temperature);
                TextView humidity = window.findViewById(R.id.humidity);
                TextView air = window.findViewById(R.id.air);
                coordinates.setText(Utils.getCoordinatesWithPoint(region.getName()));
                weather.setText(weatherString.get(UtilsGoogleMaps.getWeatherStringIndex(region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getCode())));
                temperature.setText(String.format(getString(R.string.temperature_celsius_placeholder), region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getTemperature()));
                humidity.setText(String.format(getString(R.string.value_percent_placeholder), region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getHumidity()));
                air.setText(String.valueOf(region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getAir()));
                return window;
            }
            public View getInfoContents(Marker arg0) {return null;}
        });
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        if (map != null) {
            map.clear();
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(UtilsGoogleMaps.getCoordinates(regions.get(0).getName()), 14f));
            for (int i = 0; i < regions.size(); i++) {
                LatLng location = UtilsGoogleMaps.getCoordinates(regions.get(i).getName());
                if (location == null) return;
                int index = UtilsGoogleMaps.getWeatherStringIndex(regions.get(i).getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getCode());
                map.addMarker(UtilsGoogleMaps.getMarkerOptions(location, "","", markerIcons.get(index)));
                /*map.addMarker(UtilsGoogleMaps.getMarkerOptions(
                        location,
                        UtilsGoogleMaps.getMarkerTitle(
                                location.latitude + " " + location.longitude,
                                getString(R.string.marker_title_placeholder),
                                weatherString.get(index)),
                        UtilsGoogleMaps.getMarkerDescription(regions.get(i).getRecords().get(0).getData(), getResources()),
                        markerIcons.get(index)));*/
                map.addPolygon(UtilsGoogleMaps.getPolygonOptions(location, UtilsGoogleMaps.REGION_AREA, UtilsGoogleMaps.COLOR_REGION_GREEN));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void initWeatherString(){
        weatherString = new ArrayList<>();
        weatherString.add(getString(R.string.weather_sunny));
        weatherString.add(getString(R.string.weather_sun));
        weatherString.add(getString(R.string.weather_heat));
        weatherString.add(getString(R.string.weather_soft_rain));
        weatherString.add(getString(R.string.weather_moderate_rain));
        weatherString.add(getString(R.string.weather_torrential_rain));
        weatherString.add(getString(R.string.weather_soft_wind));
        weatherString.add(getString(R.string.weather_moderate_wind));
        weatherString.add(getString(R.string.weather_torrential_wind));
        weatherString.add(getString(R.string.weather_soft_snow_fall));
        weatherString.add(getString(R.string.weather_moderate_snow_fall));
        weatherString.add(getString(R.string.weather_massive_snow_fall));
    }

    private void initMarkerIcons(){
        markerIcons = new ArrayList<>();
        markerIcons.add(R.drawable.sunny);
        markerIcons.add(R.drawable.sun);
        markerIcons.add(R.drawable.heat);
        markerIcons.add(R.drawable.soft_rain);
        markerIcons.add(R.drawable.moderate_rain);
        markerIcons.add(R.drawable.torrential_rain);
        markerIcons.add(R.drawable.soft_wind);
        markerIcons.add(R.drawable.moderate_wind);
        markerIcons.add(R.drawable.torrential_wind);
        markerIcons.add(R.drawable.soft_snow_fall);
        markerIcons.add(R.drawable.moderate_snow_fall);
        markerIcons.add(R.drawable.massive_snow_fall);
    }
}