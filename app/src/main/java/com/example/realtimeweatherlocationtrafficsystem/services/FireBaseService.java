package com.example.realtimeweatherlocationtrafficsystem.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Prediction;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This background service control the database communication.
 * It will read and write from/to database.
 * Reading from database is made by using listeners to events.
 */
public class FireBaseService extends Service implements FireBaseManager.onFireBaseDataNew, Serializable {

    // True if the service is active, false otherwise
    public static boolean SERVICE_ACTIVE;

    // Service keys
    public static final String SERVICE_KEY = "FireBase-FirebaseService-KEY";                            // used for identifying the messages sent by this service
    public static final String SERVICE_MESSAGE_ID_KEY = "FireBase-Message-ID-KEY";                      // identify the message id
    public static final String SERVICE_MESSAGE_COORDINATES_KEY = "FireBase-Message-Coordinates-KEY";    // specifies that the message contains coordinates
    public static final String SERVICE_MESSAGE_TIME_KEY = "FireBase-Message-Time-KEY";                  // specifies that the message contains the time of the message
    public static final String SERVICE_MESSAGE_DATA_KEY = "FireBase-Message-Data-KEY";                  // specifies that the message contains the weather data
    public static final int SERVICE_MESSAGE_ID_SET_VALUE = 100;                                         // the received message specifies that the Intent data must be sent to database

    // Define the FireBase manager that will control the communication with the database
    public static FireBaseManager fireBaseManager;
    // Define the region list received from database
    public static List<Region> regions = new ArrayList<>();
    public static List<HashMap<String, HashMap<String, Prediction>>> predictions = new ArrayList<>();


    @Override
    public void onCreate() {
        super.onCreate();
        // Register the broadcast receiver that will receive messages from broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(SERVICE_KEY));
        // Start the thread that will periodically check if the service is still necessary
        checkStatus.start();
        SERVICE_ACTIVE = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialize the Firebase manager that will control the database communication
        fireBaseManager = new FireBaseManager(this);
        // Register the broadcast receiver that will receive messages from broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(SERVICE_KEY));
        SERVICE_ACTIVE = true;
        return Service.START_NOT_STICKY;
    }

    // Define the thread that will periodically check if the service is still necessary
    private final Thread checkStatus = new Thread() {
        public void run() {
            // Check if the service is still necessary
            checkAppStatus();
            try {
                // Wait 5 seconds before next check
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            // Get the message id, indicating what the message contains
            int messageID = intent.getIntExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, -1);
            // Check if the message id is valid
            if (messageID == -1) return;
            // Check if the message id specifies that the Intent data must be sent to database
            if (messageID == FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE) {
                // Set the received data to database
                fireBaseManager.setValue(
                        intent.getStringExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY),
                        intent.getStringExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY),
                        (Data) intent.getSerializableExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY));
            }
        }
    };

    /**
     * Get a region from regions list using the region name.
     *
     * @param name specifies the region name that is searched.
     * @return the region with the same name from regions list.
     */
    public static Region getRegion(String name) {
        // Check if the region name exists
        if (name == null) return null;
        // Loop trough all regions list
        for (int i = 0; i < regions.size(); i++) {
            // Check if the current region in loop have the same name as the searching one
            if (regions.get(i).getName().equals(name))
                // Return the found region
                return regions.get(i);
        }
        return null;
    }

    /**
     * Send a broadcast message specifying that there is new GPS location available.
     */
    private void sendNewData() {
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(MainActivity.SERVICE_KEY);
        // Adding some data
        intent.putExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, MainActivity.SERVICE_MESSAGE_ID_REGIONS);
        // Send the message as broadcast message to all active activities
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Stop this background service if its not necessary.
     * If the app is not running and the Bluetooth service is not active, this service is useless.
     */
    private void checkAppStatus() {
        // Check if the app is running and the Bluetooth service is active
        if (!Utils.APP_ACTIVE && Utils.isBackgroundRunning(this) && !BluetoothService.SERVICE_ACTIVE) {
            // The service is not necessary anymore
            onDestroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        // Update the received regions list
        FireBaseService.regions = regions;
        // Send a broadcast message to all active activities indicating that there is new GPS location available
        sendNewData();
    }

    @Override
    public void onDataNewFireBasePredictions(List<HashMap<String, HashMap<String, Prediction>>> predictions) {
        // Update the received predictions list
        FireBaseService.predictions = predictions;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SERVICE_ACTIVE = false;
        try {
            // Unregister since the activity is not visible
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        // Remove the Firebase manager
        fireBaseManager = null;
        // Remove the regions list
        regions = null;
        // Stop the thread that periodically check if the service is necessary
        checkStatus.interrupt();
        // Stop the service
        stopSelf();
    }
}
