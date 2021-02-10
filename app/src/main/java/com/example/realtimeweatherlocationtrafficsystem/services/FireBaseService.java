package com.example.realtimeweatherlocationtrafficsystem.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FireBaseService extends Service implements FireBaseManager.onFireBaseDataNew, Serializable {

    public static final String SERVICE_KEY = "FireBase-FirebaseService-KEY";
    public static final String SERVICE_MESSAGE_ID_KEY = "FireBase-Message-ID-KEY";
    public static final String SERVICE_MESSAGE_COORDINATES_KEY = "FireBase-Message-Coordinates-KEY";
    public static final String SERVICE_MESSAGE_TIME_KEY = "FireBase-Message-Time-KEY";
    public static final String SERVICE_MESSAGE_DATA_KEY = "FireBase-Message-Data-KEY";
    public static final int SERVICE_MESSAGE_ID_SET_VALUE = 100;
    public static boolean SERVICE_ACTIVE;

    public static FireBaseManager fireBaseManager;
    public static List<Region> regions = new ArrayList<>();


    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(SERVICE_KEY));
        SERVICE_ACTIVE = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fireBaseManager = new FireBaseManager(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(SERVICE_KEY));
        SERVICE_ACTIVE = true;
        return Service.START_NOT_STICKY;
    }

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            int messageID = intent.getIntExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, -1);
            if (messageID == -1) return;
            if (messageID == FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE) {
                fireBaseManager.setValue(
                        intent.getStringExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY),
                        intent.getStringExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY),
                        (Data) intent.getSerializableExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY));
            }
        }
    };

    public static Region getRegion(String name) {
        if (name == null) return null;
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getName().equals(name))
                return regions.get(i);
        }
        return null;
    }

    private void sendNewData() {
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(MainActivity.SERVICE_KEY);
        // Adding some data
        intent.putExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, MainActivity.SERVICE_MESSAGE_ID_REGIONS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        FireBaseService.regions = regions;
        sendNewData();
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
        fireBaseManager = null;
        regions = null;
        stopSelf();
    }
}
