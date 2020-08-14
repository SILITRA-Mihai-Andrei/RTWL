package com.example.realtimeweatherlocationtrafficsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.BluetoothClientClass;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, FireBaseManager.onFireBaseDataNew, GoogleMap.OnMarkerClickListener {

    private final int INDEX_FOR_THE_MOST_COMMON_DATA = 0;
    private final int REQUEST_PERMISSION_CODE = 1;

    private GoogleMap map;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean sendDataInBackground;
    private List<Integer> markerIcons;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private String lastUnfinishedMessage = "";
    private FireBaseManager fireBaseManager;
    private LinearLayout loading;
    private TextView loading_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);
        initDialog();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();
        initMap();
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        goToMainActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        goToMainActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendDataInBackground) {
            unregisterReceiver(receiverState);
            unregisterReceiver(receiverConnection);
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
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        initMarkerIcons();
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            public View getInfoWindow(Marker marker) {
                @SuppressLint("InflateParams") final View window = getLayoutInflater().inflate(R.layout.item_map_windows_info, null);
                Region region = fireBaseManager.getRegion(Integer.parseInt(marker.getId().substring(1))); //Marker id format: "m1, m2, ..., m10"
                TextView coordinates = window.findViewById(R.id.coordinates);
                TextView weather = window.findViewById(R.id.weather);
                TextView temperature = window.findViewById(R.id.temperature);
                TextView humidity = window.findViewById(R.id.humidity);
                TextView air = window.findViewById(R.id.air);
                coordinates.setText(Utils.getCoordinatesWithPoint(region.getName()));
                weather.setText(UtilsGoogleMaps.getWeatherString(
                        UtilsGoogleMaps.getWeatherStringIndex(
                                region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getCode())
                        , getBaseContext()));
                temperature.setText(String.format(getString(R.string.temperature_celsius_placeholder), region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getTemperature()));
                humidity.setText(String.format(getString(R.string.value_percent_placeholder), region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getHumidity()));
                air.setText(String.valueOf(region.getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getAir()));
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
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        if (map != null) {
            map.clear();
            for (int i = 0; i < regions.size(); i++) {
                LatLng location = UtilsGoogleMaps.getCoordinates(regions.get(i).getName());
                if (location == null) return;
                int index = UtilsGoogleMaps.getWeatherStringIndex(regions.get(i).getRecords().get(INDEX_FOR_THE_MOST_COMMON_DATA).getData().getCode());
                map.addMarker(UtilsGoogleMaps.getMarkerOptions(location, "", "", markerIcons.get(index)));
                map.addPolygon(UtilsGoogleMaps.getPolygonOptions(location, UtilsGoogleMaps.REGION_AREA, UtilsGoogleMaps.COLOR_REGION_GREEN));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    sendDataInBackground = true;
                    loading.setVisibility(View.VISIBLE);
                    registerReceiver(receiverState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                    registerReceiver(receiverConnection, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                    initSendDataInBackgroundComponents();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    sendDataInBackground = false;
                    loading.setVisibility(View.GONE);
                    break;
            }
        }
    };

    Handler handler = new android.os.Handler(new android.os.Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UtilsBluetooth.STATE_CONNECTING:
                    loading_message.setText(R.string.connection_to_bluetooth_device);
                    if (loading.getVisibility() == View.GONE) {
                        loading.setVisibility(View.VISIBLE);
                    }
                    break;
                case UtilsBluetooth.STATE_CONNECTED:
                    Toast.makeText(getBaseContext(), String.format(getString(R.string.connected_to_placeholder_device), device.getName()), Toast.LENGTH_SHORT).show();
                    loading.setVisibility(View.GONE);
                    break;
                case UtilsBluetooth.STATE_CONNECTION_FAILED:
                    Toast.makeText(GoogleMapsActivity.this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case UtilsBluetooth.STATE_READING_WRITING_FAILED:
                    Toast.makeText(GoogleMapsActivity.this,
                            String.format(getString(R.string.disconected_from_placeholder_device), device.getName()), Toast.LENGTH_SHORT).show();
                    if (socket.isConnected()) goToMainActivity();
                    else {
                        finish();
                    }
                    break;
                case UtilsBluetooth.STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    if (readBuffer == null) break;
                    String message = new String(readBuffer, 0, msg.arg1);
                    if (message.isEmpty()) break;
                    if (isFinalMessage(message)) {
                        String response = UtilsBluetooth.getReceivedMessage(null, lastUnfinishedMessage + message, getBaseContext());
                        if (!(response == null || response.isEmpty() || response.length() <= 11))
                            Toast.makeText(GoogleMapsActivity.this, response.substring(0, response.length() - 1)
                                    + "\n\n" + response.length() + "\n", Toast.LENGTH_SHORT).show();
                        lastUnfinishedMessage = "";
                    }
                    break;
            }
            return true;
        }
    });

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null){
                    currentLocation = location;
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), UtilsGoogleMaps.DEFAULT_ZOOM));
                }
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull int[] grantResults){
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation();
            }
        }
    }

    private boolean isFinalMessage(String string){
        if(string.contains(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER)){
            return true;
        }
        lastUnfinishedMessage = string;
        return false;
    }


    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GoogleMapsActivity.this);
        builder.setMessage(R.string.need_to_connect_to_send)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener);
        builder.setTitle(R.string.send_data_background);
        builder.setCancelable(false);
        builder.show();
        loading = findViewById(R.id.loading);
        loading_message = findViewById(R.id.loading_message);
    }

    private void initSendDataInBackgroundComponents() {
        device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("BT_DEVICE_SESSION_ID");
        if (device == null || device.getName().equals("")) {
            Toast.makeText(GoogleMapsActivity.this, getString(R.string.please_select_one_device), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(GoogleMapsActivity.this, MainActivity.class));
            return;
        }
        try {
            socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothClientClass clientClass = new BluetoothClientClass(socket, bluetoothAdapter, handler, null, null);
        clientClass.start();
    }

    private final BroadcastReceiver receiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    goToMainActivity();
                }
            }
        }
    };

    private final BroadcastReceiver receiverConnection = new BroadcastReceiver() {
        @SuppressLint("ShowToast")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) || action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Toast toast = new Toast(getBaseContext());
                try {
                    toast.getView().isShown();    // true if visible
                } catch (Exception e) {         // invisible if exception
                    toast = Toast.makeText(context,
                            String.format(getString(R.string.disconected_from_placeholder_device),
                                    device.getName()), Toast.LENGTH_SHORT);
                }
                toast.show();  //finally display it
                goToMainActivity();
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (sendDataInBackground) {
            goToMainActivity();
        } else {
            super.onBackPressed();
        }
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
        try {
            if (socket != null) {
                loading_message.setText(R.string.disconnection_to_bluetooth_device);
                loading.setVisibility(View.VISIBLE);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }
}