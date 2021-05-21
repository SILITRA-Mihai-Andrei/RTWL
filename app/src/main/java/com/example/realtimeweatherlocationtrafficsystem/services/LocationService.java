package com.example.realtimeweatherlocationtrafficsystem.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.example.realtimeweatherlocationtrafficsystem.R;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * This background service will try to get the current GPS location or the last known location.
 */
public class LocationService extends Service implements LocationListener {

    // True if the service is active, false otherwise
    public static boolean SERVICE_ACTIVE;

    // Define the notification channel for dangerous notifications
    private static final CharSequence NOTIFICATION_TITLE = "Dangers";                                           // the notification title
    public static final String CHANNEL_ID = "LocationServiceNotificationChannel";                               // the notification channel id
    public static final String SERVICE_CHANNEL_NAME = "Location Service Notification Channel";                  // notification channel
    public static final int NOTIFICATION_ID = 1;                                                                // Bluetooth device reply notification
    public static final int NOTIFICATION_TIME_UPDATE = 30000;                                                   // update the same message


    public static final String SERVICE_KEY = "LocationService-KEY";                                             // used to send messages to activities
    public static final String SERVICE_MESSAGE_ID_KEY = "LocationService-Message-ID-KEY";                       // identify the message id
    public static final int SERVICE_MESSAGE_DANGERS_KEY = 300;                                                  // specifies that the message contains dangers list
    public static final String SERVICE_MESSAGE_DANGERS_DATA_KEY = "LocationService-Message-Dangers-Data-KEY";   // specifies the dangers list

    public final static String DANGER_CURRENT_REGION_SEP = "@";                                                 // indicates that the dangers message contains current region danger
    public final static String DANGER_REGION_SEP = "#";                                                         // separate the dangers in the dangers message

    // The last valid GPS location
    public static Location currentLocation = null;

    private NotificationCompat.Builder notificationBuilder;          // builder for the main notification, used to configure it
    private NotificationManager notificationManager;                 // manage all notifications, update, configure and remove
    private RemoteViews notificationLayout;                          // simple, one line notification layout
    private RemoteViews notificationLayoutExpanded;                  // expanded notification layout, for more lines
    private String lastNotification = "";                            // last notification content (used to avoid duplicate notification)
    private Date lastCount = null;                                   // stores how many identical messages was received for reply notification

    public static final int DANGER_TIME_UPDATE = 15000;             // update the same reply message
    private static String newDangers = "";                          // stores the new dangers received and waiting to be shown
    private static String lastDangers = "";                         // stores the last dangers
    private static Date lastCountDanger = null;                     // last time when a dangers message was sent

    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_ACTIVE = true;
        // Start location updates
        startLocation();
        // Create and set the notification channels for the service
        createNotificationChannel();
        setNotification();
        // Start a thread that will check every second if there are new dangers to show
        Thread timer = new Thread() {
            public void run() {
                while (SERVICE_ACTIVE) {
                    try {
                        // Check if there are dangers in the message
                        if (newDangers != null && !newDangers.equals("")) {
                            // Update and show the notification with the dangers
                            updateNotification(newDangers);
                            // Reset the message to wait another one
                            newDangers = null;
                        }
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        timer.start();
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
    private static void sendNewLocation() {
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
        LocalBroadcastManager.getInstance(null).sendBroadcast(intent);
        // Check if there are dangers in and near the current GPS location (current region)
        checkDangers();
    }

    /**
     * Update the current location with the received GPS coordinates.
     *
     * @param coordinates: the GPS coordinates.
     */
    public static void updateLocation(String coordinates) {
        if (coordinates == null) return;    // the received GPS coordinates are not valid
        Location lastLocation = currentLocation;
        if (currentLocation == null) {       // check if the current location is valid
            currentLocation = new Location(LocationManager.GPS_PROVIDER);   // create new one
        }
        // Split the latitude and longitude
        String[] c = coordinates.split(" ");
        // Check if there are 2 components after split
        if (c.length == 2) {
            try {
                // Try to convert the received GPS coordinates to double
                currentLocation.setLatitude(Double.parseDouble(c[0]));
                currentLocation.setLongitude(Double.parseDouble(c[1]));
            } catch (NumberFormatException e) {
                // Restore the last location
                currentLocation = lastLocation;
            }
        }
        sendNewLocation();
    }

    /**
     * Check for nearby danger regions using the last valid GPS coordinates.
     */
    private static void checkDangers() {
        if (currentLocation == null || FireBaseService.regions == null || FireBaseService.dangers == null)
            return;
        StringBuilder result = new StringBuilder();
        // Build the coordinates if the current GPS location
        String coordinates = currentLocation.getLatitude() + " " + currentLocation.getLongitude();
        // Loop through all regions active on map
        for (int i = 0; i < FireBaseService.regions.size(); i++) {
            String region = FireBaseService.regions.get(i).getName();   // get the name of the current region in iteration
            // Check if the current GPS location is inside or nearby of the current region
            if (UtilsGoogleMaps.isPointInRegion(region, coordinates, UtilsGoogleMaps.REGION_AREA * 1.25)) {
                // The current GPS location is inside of an active region
                // Loop through all dangers to check if there are dangerous region near the current GPS location
                for (HashMap<String, String> danger : FireBaseService.dangers) {
                    // Get the region name of the danger
                    String d_region = (String) danger.keySet().toArray()[0];
                    // Check if the current region in the loop is the same with the danger's region
                    // Is checking if the current region in loop have a dangerous weather (from dangers list)
                    if (region.equals(d_region)) {
                        // Initialize the separator of the current region danger
                        // This will indicates if the dangers message contains the current region of the GPS location
                        String current_region_danger_separator = "";
                        // Check if the current region in loop is the same with the current region of the GPS location
                        if (region.equals(Utils.getCoordinatesFormat(coordinates, 2, " "))) {
                            current_region_danger_separator = DANGER_CURRENT_REGION_SEP;    // update the separator
                        }
                        // Build the dangers message with the separator that indicates the current region danger
                        // the current danger in loop and the separator between dangers
                        result.append(current_region_danger_separator).append(danger.get(region)).append(DANGER_REGION_SEP);
                        break;
                    }
                }
            }
        }
        String s_result = result.toString();
        if (s_result.length() > 1 && !s_result.equals(lastDangers) || (lastCountDanger != null && Utils.getTimeDifference(lastCountDanger) > DANGER_TIME_UPDATE)) {
            // Show notification with dangers
            newDangers = s_result;
            // The string SERVICE_FIREBASE_KEY will be used to filer the intent
            Intent intent = new Intent(SERVICE_KEY);
            // Adding some data
            intent.putExtra(SERVICE_MESSAGE_ID_KEY, SERVICE_MESSAGE_DANGERS_KEY);
            intent.putExtra(SERVICE_MESSAGE_DANGERS_DATA_KEY, s_result);
            // Send the message as broadcast message to all active activities
            LocalBroadcastManager.getInstance(null).sendBroadcast(intent);
            // Get the current date and time
            lastCountDanger = new java.util.Date();
            // Update the last dangers
            lastDangers = s_result;
        }
    }

    /**
     * Create the notification channel.
     */
    private void createNotificationChannel() {
        // Check if the SDK version can handle the notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel configuration
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,                                // identification channel string
                    SERVICE_CHANNEL_NAME,                   // the foreground service channel name
                    NotificationManager.IMPORTANCE_HIGH     // set the notification importance
            );
            // Get the notification manager
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            // Create the notification channel
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Create the app notification.
     * Add the actions (reply and closing the service).
     * Add custom layout for notification.
     */
    private void setNotification() {
        // Initialize the notification manager - this will notify the changes for all notifications
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Get the layouts to use in the custom notification
        // simple, one line notification layout
        // Get the layouts to use in the custom notification
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification);
        notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification_danger_expanded);

        // Create the Bluetooth messages notification
        // This notification will be active when is received a custom message through Bluetooth
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(NOTIFICATION_TITLE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.alert)
                .setCustomContentView(notificationLayout) // the small layout
                .setCustomBigContentView(notificationLayoutExpanded) // the large layout
                .setSmallIcon(R.mipmap.ic_launcher) // the notification icon
                .setAutoCancel(true) // onClick will close the notification
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Create the notification
        // notification object (used to update)
        Notification notification = notificationBuilder.build();
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Update the notification with new data.
     *
     * @param data specifies the new data.
     */
    private void updateNotification(String data) {
        // Split the data to get the dangers messages
        String[] d = data.split(DANGER_REGION_SEP);
        // Check if the message is valid, the current message is different from the last or there passed the minimum timeout for new update
        if (d.length > 0 && !lastNotification.equals(data) || lastCount != null && Utils.getTimeDifference(lastCount) > NOTIFICATION_TIME_UPDATE) {
            // Update the last message
            lastNotification = data;
            // Get the current date and time
            lastCount = new java.util.Date();
            // This variable will be set true if the message contains the current region of GPS location
            // the user's current GPS location is inside a dangerous region
            boolean current_region_danger = false;
            // Check if the message contains the separator that indicates the danger for current region of GPS location
            if (d[0].contains(DANGER_CURRENT_REGION_SEP)) {
                d[0] = d[0].replace(DANGER_CURRENT_REGION_SEP, "");     // remove the separator
                notificationLayout.setTextViewText(R.id.title, d[0]);               // set the title of the simple notification
                notificationBuilder.setContentTitle(d[0]);                          // set the title for the content of notification
                // Set the layout components of the expanded notification using the dangers message
                notificationLayoutExpanded.setTextViewText(R.id.title, getString(R.string.danger_notification_region));
                notificationLayoutExpanded.setTextViewText(R.id.region, d[0]);
                current_region_danger = true;   // the user's current GPS location is inside a dangerous region
            } else {
                // The user's current GPS location is outside of a dangerous region, but nearby one
                notificationLayout.setTextViewText(R.id.title, getString(R.string.notification_dangers_title));
                notificationBuilder.setContentTitle(getString(R.string.notification_dangers_title));
                notificationLayoutExpanded.setTextViewText(R.id.region, getString(R.string.notification_dangers_title));
            }
            // Build the message with the dangers message for each danger region
            StringBuilder body = new StringBuilder();
            body.append(getString(R.string.danger_near_regions)).append("\n");
            // Loop through all dangers of the message (splited by the separator)
            for (int i = 0; i < d.length; i++) {
                // Check if the current region of the GPS location is dangerous
                if (current_region_danger && i == 0) {
                    // The current region of the GPS location is not displayed in this component
                    // It was displayed in the component above (in the notification expanded layout)
                    continue;
                }
                // Append the next dangers message of a nearby region
                body.append("- ").append(d[i]).append("\n");
            }
            notificationLayoutExpanded.setTextViewText(R.id.dangers, body.toString());
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(data));
        }
        // Update the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
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
