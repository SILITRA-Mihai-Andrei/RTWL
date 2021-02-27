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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;

/**
 * This background service will try to get the current GPS location or the last known location.
 */
public class LocationService extends Service implements LocationListener {

    // True if the service is active, false otherwise
    public static boolean SERVICE_ACTIVE;

    // The last valid GPS location
    public static Location currentLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_ACTIVE = true;
        // Start location updates
        startLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SERVICE_ACTIVE = true;
        // Start location updates
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
        // Stop the service
        stopSelf();
    }

    /**
     * Start the GPS location updates.
     */
    @SuppressLint("MissingPermission")
    private void startLocation() {
        // Get the location manager that will control the location events
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Check if the location manager exists
        if (locationManager != null) {
            // Request updates every period of time
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
        }
        // Get the last known GPS location
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Set listener that will notify when the last known GPS location is ready
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Check if this service still necessary
                checkAppStatus();
                // Check if the location exists
                if (location == null) return;
                // Update the location with the new one
                currentLocation = location;
                // Notify new location available
                sendNewLocation();
            }
        });
    }

    /**
     * Send a broadcast message that specifies that new GPS location is available.
     */
    private void sendNewLocation() {
        // Check if the current location updated exists
        if (currentLocation != null) {
            // Set fixed decimals for GPS location coordinates
            currentLocation.setLatitude(Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", currentLocation.getLatitude())));
            currentLocation.setLongitude(Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", currentLocation.getLongitude())));
        }
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(MainActivity.SERVICE_KEY);
        // Adding some data
        intent.putExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, MainActivity.SERVICE_MESSAGE_ID_LOCATION);
        // Send the message as broadcast message to all active activities
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Stop this background service if its not necessary.
     * If the app is not running and the Bluetooth service is not active, this service is useless.
     */
    private void checkAppStatus() {
        // Check if the app is still running and the Bluetooth service is active
        if (!Utils.APP_ACTIVE && Utils.isBackgroundRunning(this) && !BluetoothService.SERVICE_ACTIVE) {
            // This service is not necessary anymore
            onDestroy();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Check if this service is still necessary
        checkAppStatus();
        // Check if the location exists
        if (location == null) return;
        // Update the current location with the new one
        currentLocation = location;
        // Send a broadcast message that specifies that there is new GPS location available
        sendNewLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
