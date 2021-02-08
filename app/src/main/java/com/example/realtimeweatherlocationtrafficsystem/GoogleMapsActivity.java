package com.example.realtimeweatherlocationtrafficsystem;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Bundle;

import android.view.View;

import android.widget.ImageView;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;

import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;
import com.example.realtimeweatherlocationtrafficsystem.models.Weather;
import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;
import com.example.realtimeweatherlocationtrafficsystem.services.FireBaseService;

import com.example.realtimeweatherlocationtrafficsystem.services.LocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraIdleListener {

    private SharedPreferences googleMapsPreferences;
    private GoogleMap map;
    private Location currentLocation;
    private boolean locationTracked = false;
    private List<Region> regions = new ArrayList<>();
    private List<Integer> markerIcons;
    private TextView received;
    private ImageView regionWeatherIcon;
    private ImageView locationTrackIcon;
    private int counterFailureGPS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);
        initComponents();
        googleMapsPreferences = getSharedPreferences(getString(R.string.preference_google_maps_key), MODE_PRIVATE);
        initMap();
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This registers messageReceiver to receive messages.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.SERVICE_KEY);
        intentFilter.addAction(MainActivity.SERVICE_KEY);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        if (FireBaseService.regions != null) {
            onFirebaseDataNew(FireBaseService.regions);
        }
        currentLocation = LocationService.currentLocation;
        if (BluetoothService.SERVICE_ACTIVE) {
            received.setVisibility(View.VISIBLE);
        }
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (!FireBaseService.SERVICE_ACTIVE) {
            goToMainActivity();
        }
        map = googleMap;
        if (map == null) goToMainActivity();
        moveMapCamera(map.getCameraPosition().target.latitude, map.getCameraPosition().target.longitude, 15f);
        ImageView mapType = findViewById(R.id.mapType);
        map.setOnCameraIdleListener(this);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (map.getMapType() == GoogleMap.MAP_TYPE_HYBRID)
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                else map.setMapType(map.getMapType() + 1);
            }
        });
        locationTrackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationTracked) setLocationTrack(false);
                else setLocationTrack(true);
            }
        });
        map.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        initMarkerIcons();
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            public View getInfoWindow(Marker marker) {
                @SuppressLint("InflateParams") final View window = getLayoutInflater().inflate(R.layout.item_map_windows_info, null);
                Region region;
                try {
                    region = FireBaseService.getRegion(marker.getTitle());
                    if (region == null)
                        return null;
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
                TextView coordinates = window.findViewById(R.id.coordinates);
                TextView weather = window.findViewById(R.id.weather);
                TextView temperature = window.findViewById(R.id.temperature);
                TextView humidity = window.findViewById(R.id.humidity);
                TextView air = window.findViewById(R.id.air);
                ProgressBar airBar = window.findViewById(R.id.airBar);
                coordinates.setText(Utils.getCoordinatesWithPoint(region.getName()));
                Weather weatherObj = region.getWeather();
                weather.setText(weatherObj.getWeather());
                int index = UtilsGoogleMaps.getWeatherStringIndex(weatherObj.getWeather(), getBaseContext());
                if ((index + 2) % 3 == 0) { //get weather index; 1, 4, 7, 10 are moderate
                    weather.setTextColor(getResources().getColor(R.color.color_orange_light));
                } else if ((index + 1) % 3 == 0) { //get weather index; 2, 5, 8, 11 are dangerous
                    weather.setTextColor(getResources().getColor(R.color.color_red_dark));
                }
                temperature.setText(String.format(getString(R.string.temperature_celsius_placeholder), weatherObj.getTemperature()));
                humidity.setText(String.format(getString(R.string.value_percent_placeholder), weatherObj.getHumidity()));
                air.setText(String.format(getString(R.string.value_percent_placeholder), weatherObj.getAir()));
                if (weatherObj.getAir() >= 3 && weatherObj.getAir() <= 100)
                    airBar.setProgress((int) weatherObj.getAir());
                return window;
            }

            public View getInfoContents(Marker arg0) {
                return null;
            }
        });
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
            }
        });
        setLocationTrack(true);
    }

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            int messageID = intent.getIntExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, -1);
            if (messageID != -1) {
                if (messageID == MainActivity.SERVICE_MESSAGE_ID_REGIONS) {
                    onFirebaseDataNew(FireBaseService.regions);
                } else if (messageID == MainActivity.SERVICE_MESSAGE_ID_LOCATION) {
                    currentLocation = LocationService.currentLocation;
                }
            }

            // Extract data included in the Intent
            messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);
            String message = intent.getStringExtra(BluetoothService.SERVICE_MESSAGE_KEY);

            if (messageID == -1) return;
            switch (messageID) {
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                    Toast.makeText(context, getString(R.string.bluetooth_device_offline_no_send), Toast.LENGTH_SHORT).show();
                    received.setVisibility(View.GONE);
                    end();
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_RECEIVED:
                    if (message == null) break;
                    if (message.isEmpty()) break;
                    if (message.split(UtilsBluetooth.MESSAGE_TIME_END)[1].length() <= 3)
                        break;

                    String[] splited = message.split("@");

                    /* Check again if the coordinates are valid */
                    if ((splited[1].contains(UtilsBluetooth.INVALID_GPS_COORDINATES) || splited[1].contains(UtilsBluetooth.INVALID_GPS_COORDINATES1))
                            && currentLocation == null) {
                        /* The coordinates are still invalid - the location of this phone is not working */
                        received.setText(R.string.gps_module_gps_phone_not_working);
                        if (counterFailureGPS++ >= 2 && !Utils.isLocationEnabled(getContentResolver())) {
                            currentLocation = null;
                            counterFailureGPS = 0;
                        }
                        break;
                    }
                    if (locationTracked) {
                        if (currentLocation == null) break;
                        moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                    }
                    break;
            }
        }
    };

    public void onFirebaseDataNew(List<Region> regionsReceived) {
        if (regionsReceived == null) return;
        if (map != null) {
            map.clear();
            regions.clear();
            for (int i = 0; i < regionsReceived.size(); i++) {
                Region region = regionsReceived.get(i);
                regions.add(region);
                LatLng location = UtilsGoogleMaps.getCoordinates(region.getName(), 2);
                if (location == null) return;
                map.addMarker(UtilsGoogleMaps.getMarkerOptions(location, region.getName(), "",
                        markerIcons.get(UtilsGoogleMaps.getWeatherStringIndex(
                                region.getWeather().getWeather(), getBaseContext()))));
                int color = UtilsGoogleMaps.COLOR_REGION_GREEN;
                int index = UtilsGoogleMaps.getWeatherStringIndex(region.getWeather().getWeather(), this);
                if (index == 1 || index == 4 || index == 7)
                    color = UtilsGoogleMaps.COLOR_REGION_ORANGE;
                else if (index == 2 || index == 5 || index == 8)
                    color = UtilsGoogleMaps.COLOR_REGION_RED;
                map.addPolygon(UtilsGoogleMaps.getPolygonOptions(location, UtilsGoogleMaps.REGION_AREA, color));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onCameraIdle() {
        if (map == null || regions == null || markerIcons == null || regionWeatherIcon == null)
            return;
        if (map.getCameraPosition().zoom >= Float.parseFloat(googleMapsPreferences.getString(getString(R.string.maps_max_zoom_region_key), "16"))) {
            LatLng point = map.getCameraPosition().target;
            String pointCoordinates = Utils.getCoordinatesForDataBase(point.latitude + " " + point.longitude, 9);
            for (int i = 0; i < regions.size(); i++) {
                if (UtilsGoogleMaps.isPointInRegion(regions.get(i).getName(), pointCoordinates, UtilsGoogleMaps.REGION_AREA)) {
                    regionWeatherIcon.setImageResource(markerIcons.get(
                            UtilsGoogleMaps.getWeatherStringIndex(regions.get(i).getWeather().getWeather(), getBaseContext())));
                    return;
                }
            }
        }
        regionWeatherIcon.setImageDrawable(null);
    }

    private void setLocationTrack(boolean tracked) {
        locationTracked = tracked;
        if (tracked) {
            locationTrackIcon.setImageResource(R.drawable.location_track);
            if (currentLocation != null) {
                moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                map.setTrafficEnabled(true);
            }
        } else {
            locationTrackIcon.setImageResource(R.drawable.location_untracked);
            map.setTrafficEnabled(false);
        }
    }

    private void moveMapCamera(double lat, double lon, float zoom) {
        if (zoom == -1f)
            zoom = Float.parseFloat(googleMapsPreferences.getString(getString(
                    R.string.maps_default_zoom_key), "15f"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lon), zoom));
    }

    @Override
    public void onBackPressed() {
        goToMainActivity();
        super.onBackPressed();
    }

    private void end() {
        // Unregister
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    private void initComponents() {
        locationTrackIcon = findViewById(R.id.location);
        received = findViewById(R.id.received);
        regionWeatherIcon = findViewById(R.id.regionWeatherIcon);
    }

    private void initMarkerIcons() {
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

    public void goToMainActivity() {
        end();
        finish();
    }
}