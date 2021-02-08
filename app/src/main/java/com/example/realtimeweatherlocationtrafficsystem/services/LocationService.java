package com.example.realtimeweatherlocationtrafficsystem.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationService extends Service implements LocationListener {

    public static boolean SERVICE_ACTIVE;
    public static Location currentLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_ACTIVE = true;
        startLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SERVICE_ACTIVE = true;
        startLocation();
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SERVICE_ACTIVE = false;
        stopSelf();
    }

    @SuppressLint("MissingPermission")
    private void startLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
        }
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location == null) return;
                currentLocation = location;
                sendNewLocation();
            }
        });
    }

    private void sendNewLocation() {
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(MainActivity.SERVICE_KEY);
        // Adding some data
        intent.putExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, MainActivity.SERVICE_MESSAGE_ID_LOCATION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location == null) return;
        currentLocation = location;
        sendNewLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
