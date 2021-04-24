package com.example.realtimeweatherlocationtrafficsystem;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Prediction;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * In this activity will be displayed a Google map with a few control buttons
 * (change map type, enable/disable track location). Over the map will be shown markers and regions (polygons)
 * read from database (FireBase).
 */
public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraIdleListener {

    private SharedPreferences googleMapsPreferences;    // takes values from settings
    private GoogleMap map;                              // the Google map object
    private Location currentLocation;                   // stores the last location
    private boolean locationTracked = false;            // indicates if the location is tracked or not
    private List<Region> regions = new ArrayList<>();   // the list of regions that will be inserted on map
    private List<Integer> markerIcons;                  // stores the markers icons for each weather type
    private TextView received;                          // text box where Bluetooth data is displayed
    private ImageView regionWeatherIcon;                // weather icon in the region where it is zoomed
    private ImageView locationTrackIcon;                // the location tracking icon

    // Colors for receive box background
    private final int receivedBoxOFF = Color.argb(32, 0, 0, 0);  // low opacity color
    private final int receivedBoxON = Color.argb(196, 0, 0, 0);  // high opacity color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if the Firebase, Bluetooth and Location services are active
        // If the Firebase service is not active, there is not data that can be displayed on map
        // If the Bluetooth service and Location service are not active at the same time,
        //  there is no GPS data that can locate the current user position
        if (!FireBaseService.SERVICE_ACTIVE || (!BluetoothService.SERVICE_ACTIVE && !LocationService.SERVICE_ACTIVE)) {
            // Go back to MainActivity to activate the mandatory services
            goToMainActivity();
            return;
        }
        setContentView(R.layout.activity_google_maps);
        initComponents();  // init the UI components
        // Get the values from settings for this activity
        googleMapsPreferences = getSharedPreferences(getString(R.string.preference_google_maps_key), MODE_PRIVATE);
        // Initialize the Google map
        initMap();
    }

    @Override
    protected void onPause() {
        // Unregister the message receiver since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = true;
        // This registers messageReceiver to receive messages from broadcast
        // Create the filters of receiver - which type of messages to take
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.SERVICE_KEY);  // messages from Bluetooth service
        intentFilter.addAction(MainActivity.SERVICE_KEY);  // general messages from broadcast
        // Locally register the message receiver from broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        // Check if there are regions in database
        // This is called in onResume to update the regions
        if (FireBaseService.regions != null) {
            // There are regions in database, so display them on the map
            onFirebaseDataNew(FireBaseService.regions);
        }
        // Check if the Bluetooth service is active and if Location service has a location
        if (!BluetoothService.SERVICE_ACTIVE && LocationService.currentLocation == null) {
            // There is no GPS data
            // Location service is active, so this activity will continue to wait for a location
            received.setText(getString(R.string.no_gps_data));
        }
    }

    /**
     * Initialize the Google map and the listener (onMapReadyCallback). When the map will be ready
     * there will be a notification from listener.
     */
    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // Check if the fragment was not found or the object wasn't initialized
        if (mapFragment == null) {
            // The map can't be used - the activity can't continue
            goToMainActivity();
            return;
        }
        // Set the listener that will notify when the map is ready
        mapFragment.getMapAsync(this);
    }

    // Handling the received Intents from BluetoothService Service and general broadcast from activities
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the message id from broadcast containing messages for all activities
            int messageID = intent.getIntExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, -1);
            // Check if the message was a general one (for all activities)
            if (messageID != -1) {
                // Check if the message contains the regions from Firebase service
                if (messageID == MainActivity.SERVICE_MESSAGE_ID_REGIONS) {
                    // Update the regions in activity
                    onFirebaseDataNew(FireBaseService.regions);
                }
                // Check if the message contains the location from Location service
                else if (messageID == MainActivity.SERVICE_MESSAGE_ID_LOCATION) {
                    // Update the current location object
                    currentLocation = LocationService.currentLocation;
                    // Check if location tracking is enabled and the new location exists
                    if (locationTracked && currentLocation != null) {
                        moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                    }
                }
                return;
            }

            // Extract data included in the Intent - coming from Bluetooth service
            messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);
            String message = intent.getStringExtra(BluetoothService.SERVICE_MESSAGE_KEY);

            // Invalid message id
            if (messageID == -1) return;
            switch (messageID) {
                // One of the cases below means that Bluetooth device is disconnected
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                    Toast.makeText(context, getString(R.string.bluetooth_device_offline_no_send), Toast.LENGTH_SHORT).show();
                    received.setText(getString(R.string.bluetooth_device_offline_no_send));
                    end();  // removes all broadcast receivers
                    break;
                // Message id indicates that was received a message from Bluetooth device
                case BluetoothService.SERVICE_MESSAGE_ID_RECEIVED:
                    if (message == null) break;       // the message should not be null
                    if (message.isEmpty()) break;     // the message should not be empty
                    // All Bluetooth messages must have a time [hh:mm:ss]
                    // If this time is not valid - the message is not valid
                    String[] s_message = message.split(UtilsBluetooth.MESSAGE_TIME_END);
                    if (s_message.length == 2 && s_message[1].length() <= 3)
                        break;

                    // Bluetooth messages that contains GPS coordinates and weather data are duplicated
                    // The first part uses the sensors values to translate in strings (ex: Sun, Rain, Wind)
                    // The second part will contain the message received from Bluetooth device
                    // There two parts are splited by a symbol ("@" in this case)
                    String[] splited = message.split("@");

                    // Check if was received a message with only GPS coordinates
                    if (splited.length == 1 && splited[0].contains(UtilsBluetooth.MESSAGE_GPS_COORDINATES) && currentLocation != null && locationTracked) {
                        moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                    } else if (splited.length == 2) {
                        // Display the translated sensors values into the receive box
                        received.setText(splited[0]);
                        /* Check again if the coordinates are valid */
                        if ((splited[1].contains(UtilsBluetooth.INVALID_GPS_COORDINATES) || splited[1].contains(UtilsBluetooth.INVALID_GPS_COORDINATES1))) {
                            // The coordinates are invalid
                            // Check if location is enabled
                            String locationService = Utils.isLocationEnabled(getContentResolver(), getBaseContext()) ? "" : "\n" + getString(R.string.active_location);
                            // No GPS data available (neither from Bluetooth device, nor from location service)
                            // This means that the data received can't be sent to database
                            received.setText(getString(R.string.no_gps_data_no_send) + locationService);
                            // Anyway, if the location tracking is activated and the last location exists
                            // Move the map to the last valid location
                            // If the location service is activated, there should be an update when the device is moving
                            if (locationTracked && currentLocation != null) {
                                moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                                break;
                            }
                        }
                        // If the location tracking is activated and the GPS coordinated received from Bluetooth device are valid
                        // Use there GPS coordinates to move the map (GPS module has priority versus GPS phone)
                        else if (locationTracked) {
                            // Take the second part of the message (unmodified message received from Bluetooth device)
                            //  and split it to get the latitude and longitude
                            String[] data = splited[1].split(" ");
                            // Using try/catch in the case of invalid coordinates (not numbers)
                            try {
                                // Convert the coordinates to float values and move the map
                                moveMapCamera(Float.parseFloat(data[0]), Float.parseFloat(data[1]), map.getCameraPosition().zoom);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
        }
    };

    /**
     * This method will update the regions displayed on map.
     *
     * @param regionsReceived is the list of regions that must be displayed.
     */
    public void onFirebaseDataNew(List<Region> regionsReceived) {
        // The list must not be null
        if (regionsReceived == null) return;
        // The map must be initialized and active
        if (map != null) {
            // Clear the map of markers, polygons and other components
            map.clear();
            // Clear the regions list of activity
            regions.clear();
            // Loop through all regions received
            for (int i = 0; i < regionsReceived.size(); i++) {
                // Get and store the current region in loop
                Region region = regionsReceived.get(i);
                // Add the current region to list
                regions.add(region);
                // The region name contains its coordinates (ex: Region 47.24 26.23)
                // Get the latitude and longitude from the current region name
                LatLng location = UtilsGoogleMaps.getCoordinates(region.getName(), 2);
                // If the coordinates are invalid, continue with the next loop
                if (location == null) continue;
                // Add a marker on map for the current region using its location coordinates and weather data
                map.addMarker(UtilsGoogleMaps.getMarkerOptions(location, region.getName(), null,
                        markerIcons.get(UtilsGoogleMaps.getWeatherStringIndex(
                                region.getWeather().getWeather(), getBaseContext()))));
                // Initialize the region area background color with green, corresponding to low
                //  weather intensity (ex: sunny, soft rain, soft wind)
                int color = UtilsGoogleMaps.COLOR_REGION_GREEN;
                // Get the index of the weather string (0 is sunny and 11 is massive snow fall)
                int index = UtilsGoogleMaps.getWeatherStringIndex(region.getWeather().getWeather(), this);
                // Check if the weather intensity is medium (ex: sun, moderate rain, moderate wind)
                if (index == 1 || index == 4 || index == 7)
                    // Update the region area background color for this weather intensity case
                    color = UtilsGoogleMaps.COLOR_REGION_ORANGE;
                    // Check if the weather intensity is high (ex: heat, torrential rain, massive snow fall)
                else if (index == 2 || index == 5 || index == 8)
                    // Update the region area background color for this weather intensity case
                    color = UtilsGoogleMaps.COLOR_REGION_RED;
                // Add the region area on map
                // The region area is a polygon with a fixed distance
                map.addPolygon(UtilsGoogleMaps.getPolygonOptions(location, UtilsGoogleMaps.REGION_AREA, color));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // On marker click will close the InfoWindow of the marker
        return false;
    }

    /**
     * This listener will be called when the user stopped from interacting with the map.
     * This listener is called to check the zoom. If the zoom if higher than a specific value,
     * the weather icon of the current region that is zoomed will be displayed as a static icon over map.
     * This helps the user to see the region weather icon when the zoom is too high.
     * If the user zoom in, this function will be called after the zoom is finished.
     */
    @Override
    public void onCameraIdle() {
        // Check if all components are initialized
        if (map == null || regions == null || markerIcons == null || regionWeatherIcon == null)
            return;
        // Check if the current map zoom is higher than the value from settings (ex: 16f)
        if (map.getCameraPosition().zoom >= Float.parseFloat(
                googleMapsPreferences.getString(getString(R.string.maps_max_zoom_region_key), SettingsActivity.DEFAULT_MAX_ZOOM_REGION))) {
            // Get the coordinates from the middle of the map view
            LatLng point = map.getCameraPosition().target;
            // Get the coordinates as string with 9 decimals (for calculation precision)
            String pointCoordinates = Utils.getCoordinatesWithDecimals(point.latitude + " " + point.longitude, 9);
            // Loop through all regions active on map
            for (int i = 0; i < regions.size(); i++) {
                // Check if the middle of the map view is inside of the current region
                if (UtilsGoogleMaps.isPointInRegion(regions.get(i).getName(), pointCoordinates, UtilsGoogleMaps.REGION_AREA)) {
                    // The middle of the map view is inside of an active region
                    // Get the region weather icon and set it as static icon over map
                    regionWeatherIcon.setImageResource(markerIcons.get(
                            UtilsGoogleMaps.getWeatherStringIndex(regions.get(i).getWeather().getWeather(), getBaseContext())));
                    return;
                }
            }
        }
        // If the zoom is not higher enough, the static icon will be hidden
        regionWeatherIcon.setImageDrawable(null);
    }

    /**
     * Enable or disable the location tracking. Set the UI components accordingly.
     */
    private void setLocationTrack(boolean tracked) {
        // Set the variable
        locationTracked = tracked;
        // Check if the tracking is enabled
        if (tracked) {
            // Set the corresponding icon
            locationTrackIcon.setImageResource(R.drawable.location_track);
            // If the last location exists and is valid
            if (currentLocation != null) {
                // Move the map to current location or last known location with the current map zoom
                moveMapCamera(currentLocation.getLatitude(), currentLocation.getLongitude(), map.getCameraPosition().zoom);
                // Enable traffic lines on map
                map.setTrafficEnabled(true);
            }
        } else {
            // Location tracking is disabled
            locationTrackIcon.setImageResource(R.drawable.location_untracked);
            map.setTrafficEnabled(false);
        }
    }

    /**
     * This method will be called after the Google map is ready.
     *
     * @param googleMap is the instance of GoogleMap class.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Check if the Firebase service is active
        if (!FireBaseService.SERVICE_ACTIVE) {
            // Without this service, the activity can't continue
            goToMainActivity();
        }

        // Assign the ready map
        map = googleMap;
        // Check if the map is valid
        if (map == null)
            goToMainActivity();     // can't continue activity without this map object

        // Initialize the map components
        initMapComponents();
        // Set the InfoWindow adapter for markers
        setInfoWindowAdapter();
        // Update the regions in activity
        onFirebaseDataNew(FireBaseService.regions);
        // Set the location tracking by default
        setLocationTrack(true);
    }

    /**
     * Move map camera using the coordinates and the zoom. If the zoom is -1f, the value from the settings will be used.
     *
     * @param lat  is the latitude of the point
     * @param lon  is the longitude of the point
     * @param zoom is the zoom value
     */
    private void moveMapCamera(double lat, double lon, float zoom) {
        // Invalid zoom value
        if (zoom == -1f) {
            // Get the zoom value from the settings
            zoom = Float.parseFloat(googleMapsPreferences.getString(getString(
                    R.string.maps_default_zoom_key), SettingsActivity.DEFAULT_START_ZOOM));
        }
        // Move the map view using the values
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lon), zoom));
    }

    @Override
    public void onBackPressed() {
        // Go back to MainActivity when back button is pressed
        goToMainActivity();
        super.onBackPressed();
    }

    /**
     * Unregister all broadcast receivers.
     */
    private void end() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    /**
     * Initialize the UI components of GoogleMapsActivity. Add listeners for buttons and components.
     */
    private void initComponents() {
        // Find the UI components
        locationTrackIcon = findViewById(R.id.location);
        received = findViewById(R.id.received);
        regionWeatherIcon = findViewById(R.id.regionWeatherIcon);

        // Receive box will start with high opacity background color
        received.setBackgroundColor(receivedBoxON);
        // Set listener for click on receive box
        // This will change the opacity background color
        received.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the background color has the OFF background color
                if (((ColorDrawable) received.getBackground()).getColor() == receivedBoxOFF) {
                    // Change it to ON background color
                    received.setBackgroundColor(receivedBoxON);
                } else {
                    // Change it to OFF background color
                    received.setBackgroundColor(receivedBoxOFF);
                }
            }
        });
        received.setText(getString(R.string.no_message));
    }

    /**
     * Initialize all map components and set listeners.
     * Check permission is done in Utils.java file.
     */
    @SuppressLint("MissingPermission")
    private void initMapComponents() {
        // Create the listener called when the user stop interacting with the map
        map.setOnCameraIdleListener(this);
        // Update the map zoom to the value from settings (starting zoom value)
        moveMapCamera(
                map.getCameraPosition().target.latitude,    // get the current latitude from map view
                map.getCameraPosition().target.longitude,   // get the current longitude from map view
                // Get the zoom value from settings
                Float.parseFloat(googleMapsPreferences.getString(getString(
                        R.string.maps_default_zoom_key), SettingsActivity.DEFAULT_START_ZOOM)));
        // Find the map type button
        ImageView mapType = findViewById(R.id.mapType);
        // Set listener for click on map type button
        // This will change the map type (ex: normal, hybrid, satellite)
        mapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Map type indexes:
                 MAP_TYPE_NONE = 0;
                 MAP_TYPE_NORMAL = 1;
                 MAP_TYPE_SATELLITE = 2;
                 MAP_TYPE_TERRAIN = 3;
                 MAP_TYPE_HYBRID = 4; */
                // Check if the map type has the last index
                if (map.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                    // Set the first index
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else {
                    // Increment the map type index
                    map.setMapType(map.getMapType() + 1);
                }
            }
        });
        // Set listener for click to location tracking button
        locationTrackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the location tracking is activated
                if (locationTracked) setLocationTrack(false);   // stop the location tracking
                else setLocationTrack(true);                    // start the location tracking
            }
        });
        // Check if the location is enabled
        if (Utils.isLocationEnabled(getContentResolver(), this)) {
            /* Enables or disables the my-location layer.
             * While enabled and the location is available, the my-location layer continuously
             *   draws an indication of a user's current location and bearing,
             *   and displays UI controls that allow a user to interact with their location
             *  (for example, to enable or disable camera tracking of their location and bearing). */
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                // The permission for location is not granted
                Toast.makeText(this, getString(R.string.grant_location_permission), Toast.LENGTH_LONG).show();
                // The user must grant location permission to use the Google map
                goToMainActivity();
            }
        }
        // Set Google maps components
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        // Initialize the list of markers icons used for custom marker icon
        initMarkerIcons();
    }

    /**
     * Initialize components and set the InfoWindow adapter. This method will customize the InfoWindow for all markers.
     */
    private void setInfoWindowAdapter() {
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            public View getInfoWindow(Marker marker) {
                @SuppressLint("InflateParams")
                // Get the InfoWindow layout
                final View window = getLayoutInflater().inflate(R.layout.item_map_windows_info, null);
                // Declare the region that will by represented by this marker
                Region region;
                try {
                    // Try to find the region with the same name as the marker title
                    // The marker title is the region name
                    // The region name contains the region coordinates (ex: Region 46.23 26.20)
                    region = FireBaseService.getRegion(marker.getTitle());
                    // If the region wasn't found or is invalid, can't create the marker InfoWindow
                    if (region == null)
                        return null;
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }

                // Find all UI components
                TextView coordinates = window.findViewById(R.id.coordinates);
                TextView weather = window.findViewById(R.id.weather);
                TextView temperature = window.findViewById(R.id.temperature);
                TextView humidity = window.findViewById(R.id.humidity);
                TextView air = window.findViewById(R.id.air);
                ProgressBar airBar = window.findViewById(R.id.airBar);

                // Set the components of InfoWindow
                // Set the coordinates for the current region
                coordinates.setText(Utils.getCoordinatesWithPoint(region.getName()));
                // Get the weather of the region
                Weather weatherObj = region.getWeather();
                // Set the weather title
                weather.setText(weatherObj.getWeather());
                // Get the weather string index (0 is "sunny" and 11 is "massive snow fall" weather)
                int index = UtilsGoogleMaps.getWeatherStringIndex(weatherObj.getWeather(), getBaseContext());
                if ((index + 2) % 3 == 0) {  // check weather index; 1, 4, 7, 10 are moderate
                    // Set the weather title color for moderate weather (orange)
                    weather.setTextColor(getResources().getColor(R.color.color_orange_light));
                } else if ((index + 1) % 3 == 0) {  // check weather index; 2, 5, 8, 11 are dangerous
                    // Set the weather title color for dangerous weather (red)
                    weather.setTextColor(getResources().getColor(R.color.color_red_dark));
                }
                // Set the temperature
                temperature.setText(String.format(getString(R.string.float_temperature_celsius_placeholder), weatherObj.getTemperature()));
                // Set the humidity
                humidity.setText(String.format(getString(R.string.float_percent_placeholder), weatherObj.getHumidity()));
                // Set the air quality / pollution
                air.setText(String.format(getString(R.string.float_percent_placeholder), weatherObj.getAir()));
                // Set the air quality / pollution bar
                if (weatherObj.getAir() >= 0 && weatherObj.getAir() <= 100)
                    // Set air quality / pollution
                    airBar.setProgress((int) weatherObj.getAir());

                // Set the predictions for the current region
                setPredictions(window, region);

                return window;
            }

            // Unused method, but necessary
            public View getInfoContents(Marker arg0) {
                return null;
            }
        });
        // Set listener for click on InfoWindow
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // When the InfoWindow is active/opened - hide it
                marker.hideInfoWindow();
            }
        });
    }

    /**
     * Set predictions for the region's marker.
     * Inflate in the InfoWindow 3 new views containing prediction data.
     * <p>
     * Prediction data contains:
     * -- weather code predicted with its probability;
     * -- temperature predicted with its probability;
     * -- humidity predicted with its probability.
     *
     * @param window is the InfoWindow of the region marker where the predictions will be inserted.
     * @param region specifies the region.
     */
    private void setPredictions(View window, Region region) {
        // Get the predictions from the region
        HashMap<String, Prediction> prediction = UtilsGoogleMaps.getPredictions(region.getName());
        // Check if there are predictions
        if (prediction != null) {
            // Get the system inflater necessary to inflate view in the InfoWindow
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) return;
            // Get the LinearLayout where the views will be inserted
            LinearLayout weather_data_ll = window.findViewById(R.id.weather_data_ll);
            // Create new LinearLayout for a single prediction
            // In this will be inserted the views with custom prediction data
            LinearLayout predictions_ll = new LinearLayout(window.getContext());
            // In this list will be inserted all the prediction views created with custom data
            // From this list, the views will be inserted in the main layout (containing all predictions)
            ArrayList<View> views = new ArrayList<>();

            // Configure the prediction layout
            predictions_ll.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            predictions_ll.setOrientation(LinearLayout.HORIZONTAL);
            predictions_ll.setHorizontalGravity(View.TEXT_ALIGNMENT_CENTER);

            // Count the prediction views
            int counter = 0;
            for (String key : prediction.keySet()) {
                // Get the prediction layout and inflate it in a view
                @SuppressLint("InflateParams")
                View v = inflater.inflate(R.layout.item_prediction, null);
                // Get the prediction data from the HashMap
                Prediction predict = prediction.get(key);
                // Check if the prediction exists
                if (predict == null) continue;

                // Find all view from prediction layout
                ConstraintLayout prediction_body = v.findViewById(R.id.prediction_body);
                TextView prediction_datetime = v.findViewById(R.id.prediction_datetime);
                TextView prediction_weather_probability = v.findViewById(R.id.prediction_weather_probability);
                ImageView prediction_weather_image = v.findViewById(R.id.prediction_image);
                TextView prediction_weather = v.findViewById(R.id.prediction_weather);
                TextView prediction_temperature = v.findViewById(R.id.prediction_temperature);
                TextView prediction_temperature_accuracy = v.findViewById(R.id.prediction_temperature_accuracy);
                TextView prediction_humidity = v.findViewById(R.id.prediction_humidity);
                TextView prediction_humidity_accuracy = v.findViewById(R.id.prediction_humidity_accuracy);

                // Set the views from prediction layout with custom prediction data
                prediction_weather.setText(
                        UtilsGoogleMaps.getWeatherString(
                                UtilsGoogleMaps.getWeatherStringIndex(
                                        predict.getCode()), getBaseContext()));
                String[] datetime = key.split(":");
                prediction_datetime.setText(String.format("%s:%s", datetime[3], datetime[4]));
                prediction_weather_probability.setText(String.format(getString(R.string.int_percent_placeholder), predict.getCode_p()));
                prediction_weather_image.setImageDrawable(ContextCompat.getDrawable(window.getContext(), R.drawable.sun));
                prediction_temperature.setText(String.format(getString(R.string.int_temperature_celsius_placeholder), predict.getTemperature()));
                prediction_temperature_accuracy.setText(String.format(getString(R.string.int_percent_placeholder), predict.getTemperature_p()));
                prediction_humidity.setText(String.format(getString(R.string.int_percent_placeholder), predict.getHumidity()));
                prediction_humidity_accuracy.setText(String.format(getString(R.string.int_percent_placeholder), predict.getHumidity_p()));
                // The prediction has data, so show the view
                prediction_body.setVisibility(View.VISIBLE);
                // Add the configured view to the list - it will be inserted in the main layout after
                views.add(v);
                // Count another valid prediction
                counter++;
            }
            // Check if there are 3 predictions
            if (counter != 3) {
                // Add 'No available' view to complete the invalid predictions
                for (int i = counter; i < 3; i++) {
                    // Create a new view
                    @SuppressLint("InflateParams")
                    View v = inflater.inflate(R.layout.item_prediction, null);
                    // Show only the views for invalid predictions
                    TextView no_available_prediction = v.findViewById(R.id.no_available);
                    no_available_prediction.setVisibility(View.VISIBLE);
                    // Add the view to the list
                    views.add(v);
                }
            }
            // Reverse the list
            // The HashMap data is reversed, so now reverse again XD
            Collections.reverse(views);
            // Loop through all the views in the list
            for (View view : views) {
                // Add the views to the predictions layout (horizontal linear layout)
                predictions_ll.addView(view);
            }
            // Add the predictions layout to the InfoWindow view (under air quality)
            weather_data_ll.addView(predictions_ll);
        }
    }


    /**
     * Initialize the list of markers icons corresponding to each weather condition.
     */
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

    /**
     * Go to MainActivity safe. Unregister all broadcast receivers.
     */
    public void goToMainActivity() {
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = false;
        end();
        finish();
    }
}