package com.example.realtimeweatherlocationtrafficsystem.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.MainActivity;
import com.example.realtimeweatherlocationtrafficsystem.R;
import com.example.realtimeweatherlocationtrafficsystem.models.BluetoothClientClass;
import com.example.realtimeweatherlocationtrafficsystem.models.BluetoothReceiveReply;
import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;

import java.io.IOException;
import java.util.Date;

/**
 * This foreground service will connect to a Bluetooth device and handle the connection and communication with it.
 * The service states will be sent through broadcasts messages.
 */
@SuppressLint("ParcelCreator")
public class BluetoothService extends Service implements Parcelable {

    // True if the service is active, false otherwise
    public static boolean SERVICE_ACTIVE;

    // True if the GPS module sends valid GPS coordinates
    public static boolean GPS_MODULE_WORKING = false;

    // Service keys
    public static final String SERVICE_CHANNEL_NAME = "Foreground Service Channel";         // notification channel
    public static final String SERVICE_STOP_KEY = "BluetoothService-Stop-KEY";              // receive stop message
    public static final String SERVICE_KEY = "BluetoothService-KEY";                        // used to send messages to activities
    public static final String SERVICE_MESSAGE_ID_KEY = "BluetoothService-Message-ID-KEY";  // indicates what type of message the service is sending
    public static final String SERVICE_MESSAGE_KEY = "BluetoothService-Message-KEY";        // indicates the message the service is sending

    // Notification button commands
    public static final String SERVICE_COMMAND_STOP = "STOP";   // stop/close the foreground service
    public static final String SERVICE_COMMAND_SEND = "SEND";   // send the direct message from notification

    // Service messages for Bluetooth communication
    public static final int SERVICE_MESSAGE_ID_CONNECTION_FAILED = 0;   // couldn't connect to Bluetooth device
    public static final int SERVICE_MESSAGE_ID_CONNECTING = 1;          // connecting to Bluetooth device
    public static final int SERVICE_MESSAGE_ID_CONNECTED = 2;           // connected to Bluetooth device
    public static final int SERVICE_MESSAGE_ID_DISCONNECTED = 3;        // disconnected from Bluetooth device
    public static final int SERVICE_MESSAGE_ID_RW_FAILED = 4;           // couldn't read/write (usually lost connection)
    public static final int SERVICE_MESSAGE_ID_SENT = 5;                // message sent to Bluetooth device
    public static final int SERVICE_MESSAGE_ID_RECEIVED = 6;            // message received from Bluetooth device
    public static final int SERVICE_MESSAGE_ID_BT_OFF = 10;             // Bluetooth disabled

    // Define the main notification channel for this foreground service
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    // Define the reply notification channel for the state messages received from Bluetooth device
    public static final String CHANNEL_REPLY_ID = "ForegroundServiceReplyChannel";
    // Define notification variables
    public static final int NOTIFICATION_ID = 1;                      // foreground notification id
    public static final int NOTIFICATION_REPLY_ID = 2;                // Bluetooth device reply notification
    public static final int NOTIFICATION_TIME_REPLY_UPDATE = 30000;   // update the same reply message
    public static final int NOTIFICATION_TIME_REPLY_TIMEOUT =         // Bluetooth device reply notification timeout
            NOTIFICATION_TIME_REPLY_UPDATE / 2;
    public static final String NOTIFICATION_REPLY_KEY = "KEY_TEXT_REPLY";   // the reply message wrote in notification (direct message)
    public static final String NOTIFICATION_TITLE = "RTWL Traffic System";  // foreground service notification title

    // Store the current or the last known GPS location
    private Location currentLocation = null;

    private Notification notification;                              // notification object (used to update)
    private NotificationCompat.Builder notificationBuilder;         // builder for the main notification, used to configure it
    private NotificationCompat.Builder notificationReplyBuilder;    // builder for the reply notification, used to configure it
    private RemoteViews notificationLayout;                         // simple, one line notification layout
    private RemoteViews notificationLayoutExpanded;                 // expanded notification layout, for the main notification
    private NotificationManager notificationManager;                // manage all notifications, update, configure and remove
    private String lastReply = "";                                  // last reply received from Bluetooth device (stored to update it after timeout)
    private Date lastReplyCount = null;                             // stores how many identical messages was received for reply notification

    // TODO: must implement a method to send messages to Bluetooth device without this static object
    // TODO: must remove this static object - it's a MEMORY LEAK !!!
    @SuppressLint("StaticFieldLeak")
    static BluetoothClientClass clientClass;    // used to send message to Bluetooth device
    public static BluetoothDevice device;       // the Bluetooth device selected in MainActivity
    private BluetoothSocket socket = null;      // the Bluetooth socket that control the Bluetooth connection

    /**
     * DEFAULT Constructor
     * This is called when the service is started.
     */
    public BluetoothService() {
        SERVICE_ACTIVE = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the broadcasts receivers
        registerReceiver(receiverState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(BluetoothService.SERVICE_STOP_KEY));

        // Create and set the notification channels for the service
        createNotificationChannel(CHANNEL_ID);
        createNotificationChannel(CHANNEL_REPLY_ID);
        setNotification();

        // Start the foreground notification for this service
        startForeground(NOTIFICATION_ID, notification);

        // The foreground service is considered active from now
        SERVICE_ACTIVE = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (socket == null || !socket.isConnected()) {
            // Get the selected Bluetooth device from the MainActivity from the Intent that started the service
            device = intent.getParcelableExtra(MainActivity.BT_DEVICE_SESSION_ID);
            // Connect the selected Bluetooth device
            connectBluetooth();
        }
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    // Handling the received Intents from BluetoothReceiveReply Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(SERVICE_STOP_KEY)) {
                Toast.makeText(context, getString(R.string.closing_bt_communication), Toast.LENGTH_SHORT).show();
                onDestroy();
            }
        }
    };

    /**
     * Create the notification channel.
     *
     * @param channel is the identification channel string.
     */
    private void createNotificationChannel(String channel) {
        // Check if the SDK version can handle the notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel configuration
            NotificationChannel serviceChannel = new NotificationChannel(
                    channel,                                // identification channel string
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

    // Create the handler that will receive the Bluetooth connection messages
    private Handler handler = new android.os.Handler(new android.os.Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                // Connecting to Bluetooth device
                case UtilsBluetooth.STATE_CONNECTING:
                    String strConnecting = String.format(getString(R.string.connecting_to_placeholder_device), device.getName());
                    notificationLayout.setTextViewText(R.id.title, strConnecting);
                    notificationLayoutExpanded.setTextViewText(R.id.title, strConnecting);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    // Send broadcast message to all active activities
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTING,
                            String.format(getString(R.string.connecting_to_placeholder_device), device.getName()));
                    break;
                // Connected to Bluetooth device
                case UtilsBluetooth.STATE_CONNECTED:
                    String stringConnected = String.format(getString(R.string.connected_to_placeholder_device), device.getName());
                    notificationLayout.setTextViewText(R.id.title, stringConnected);
                    notificationLayoutExpanded.setTextViewText(R.id.title, stringConnected);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    Toast.makeText(BluetoothService.this, stringConnected, Toast.LENGTH_SHORT).show();
                    // Send broadcast message to all active activities
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTED, stringConnected);
                    break;
                // Something was wrong when reading/writing from/to Bluetooth device (usually lost connection)
                case UtilsBluetooth.STATE_READING_WRITING_FAILED:
                    String stringRW = String.format(getString(R.string.rw_failed), device.getName());
                    // Reading or writing was NOT successful
                    // Send broadcast message to all active activities
                    sendMessage(SERVICE_MESSAGE_ID_RW_FAILED, stringRW);
                    // Stop the service, the Bluetooth device may be disconnected
                    onDestroy();
                    break;
                // Connection to Bluetooth device was lost
                case UtilsBluetooth.STATE_CONNECTION_FAILED:
                    String stringFailed = getString(R.string.connection_failed);
                    Toast.makeText(BluetoothService.this, stringFailed, Toast.LENGTH_SHORT).show();
                    // Send broadcast message to all active activities
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTION_FAILED, stringFailed);
                    // Stop the service
                    onDestroy();
                    break;
                // The message was sent to Bluetooth device
                case UtilsBluetooth.STATE_MESSAGE_SEND:
                    sendMessage(SERVICE_MESSAGE_ID_SENT, getString(R.string.message_sent));
                    break;
                // Message received from Bluetooth device
                case UtilsBluetooth.STATE_MESSAGE_RECEIVED:
                    // Get the message bytes received
                    byte[] readBuffer = (byte[]) msg.obj;
                    // Check if the message bytes exists
                    if (readBuffer == null) break;
                    // Convert the message bytes to a message string
                    String message = new String(readBuffer, 0, msg.arg1);
                    // Check if the message exists
                    if (message.isEmpty()) break;
                    // Translate the received message
                    // The result is two strings separated by "@"
                    // The first part is the translated string
                    // The second part is the received string (unmodified)
                    String[] messages = message.split(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER);
                    for (String m : messages){
                        String response = UtilsBluetooth.getReceivedMessage(m, getBaseContext());

                        // Check if the translated message exists
                        if (response != null) {
                            // Check if the message is not empty
                            if (response.isEmpty()) break;
                            // Check if the location service and the last location exists
                            if (LocationService.SERVICE_ACTIVE && LocationService.currentLocation != null) {
                                // Update the current location object from the LocationService
                                currentLocation = LocationService.currentLocation;
                            }
                            String[] s_response = response.split(UtilsBluetooth.MESSAGE_TIME_END);
                            // Check if the message contains the current time
                            if (s_response[1].length() > 3) {
                                // Split the message to receive the two parts
                                String[] splited = response.split("@");
                                // Check if the translated message contains valid GPS coordinates
                                splited[0] = checkLocation(splited[0]);
                                // Check if the message contains both messages
                                if (splited.length == 2) {
                                    // Check the received message if contains valid GPS coordinates
                                    splited[1] = checkLocation(splited[1]);
                                    // Send broadcast message to all active activities with th translated and received messages
                                    sendMessage(SERVICE_MESSAGE_ID_RECEIVED, splited[0] + "@" + splited[1]);
                                    // Update the notification with the received message
                                    updateNotification(splited[0]);
                                    // Split the message received to get all components
                                    String[] d = splited[1].split(" ");
                                    // Send the data to database
                                    sendToDataBase(d);
                                } else {
                                    if (s_response[1].contains(UtilsBluetooth.MESSAGE_GPS_COORDINATES)){
                                        if (GPS_MODULE_WORKING && LocationService.currentLocation != null){
                                            currentLocation = LocationService.currentLocation;
                                        }
                                        // Send broadcast message to all active activities with th translated and received messages
                                        sendMessage(SERVICE_MESSAGE_ID_RECEIVED, s_response[1]);
                                    }
                                    else {
                                        // The received message is a reply message
                                        // Send the reply message as broadcast message to all active activities
                                        sendMessage(SERVICE_MESSAGE_ID_RECEIVED, splited[0]);
                                        // Update the reply notification
                                        updateNotification(s_response[1].replace("\n", ""));
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    });

    /**
     * Create the app notification.
     * Add the actions (reply and closing the service).
     * Add custom layout for notification.
     */
    private void setNotification() {
        // Create the intent showing where the close button will send a message
        Intent notificationStopIntent = new Intent(this, BluetoothReceiveReply.class);
        // Add to intent the STOP message tag
        notificationStopIntent.setAction(SERVICE_COMMAND_STOP);
        // Create the intent showing where the send button will send the message wrote by user
        Intent notificationSendIntent = new Intent(this, BluetoothReceiveReply.class);
        // Add to intent the SEND message tag
        notificationSendIntent.setAction(SERVICE_COMMAND_SEND);

        // Create the intents that are waiting for a specific action
        PendingIntent pendingReplyIntent = PendingIntent.getBroadcast(this, 0, notificationSendIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0, notificationStopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Initialize the notification manager - this will notify the changes for all notifications
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Get the layouts to use in the custom notification
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification);
        notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification_expanded);

        // Initialise RemoteInput for receiving the user message wrote in notification
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY_KEY)
                .setLabel(getString(R.string.write_command))
                .build();
        // Create the reply action
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                android.R.drawable.sym_action_chat, getString(R.string.send_command), pendingReplyIntent)
                .addRemoteInput(remoteInput) // this will send the wrote message to intent
                .build();

        // Create the close action
        NotificationCompat.Action closeAction = new NotificationCompat.Action.Builder(
                android.R.drawable.sym_action_chat, getString(R.string.close), pendingCloseIntent)
                .setAllowGeneratedReplies(true) // useful for small devices
                .build();

        // Create the notification builder - add all features created above to notification
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(NOTIFICATION_TITLE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle()) // for custom layout
                .setCustomContentView(notificationLayout) // the small layout
                .setCustomBigContentView(notificationLayoutExpanded) // the large layout
                .setSmallIcon(R.mipmap.ic_launcher) // the notification icon
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // create a sound when notify
                .setOnlyAlertOnce(true)  // alert only one time
                .addAction(replyAction) // add the reply action
                .addAction(closeAction); // add the close action

        // Create the notification
        notification = notificationBuilder.build();
        // Start the notification
        startForeground(NOTIFICATION_ID, notification);
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification);

        // Create the Bluetooth messages notification
        // This notification will be active when is received a custom message through Bluetooth
        notificationReplyBuilder = new NotificationCompat.Builder(this, CHANNEL_REPLY_ID)
                .setSmallIcon(R.drawable.arduino)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true) // onClick will close the notification
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    /**
     * Check if the location is valid.
     * If the location is not valid, it will try to get GPS coordinates from LocationService.
     *
     * @param message specifies the message that contains the location.
     * @return the same message, but with valid GPS coordinates (if exists).
     */
    private String checkLocation(String message) {
        // Check if the message contains the character that indicates that the GPS coordinates must be replaced
        if (message.contains(UtilsBluetooth.MUST_GET_LOCATION)) {
            // Check if the current location exists
            if (currentLocation == null) {
                // Replace the character with custom message
                // The GPS location can't be replaced, there is no GPS data to use
                message = message.replace(
                        UtilsBluetooth.MUST_GET_LOCATION, getString(R.string.no_gps_data));
            } else {
                // Replace the character with valid GPS location
                message = message.replace(
                        UtilsBluetooth.MUST_GET_LOCATION,
                        getString(R.string.region_dots) + " " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
            }
        }
        // Check if the message contains invalid GPS coordinates
        else if (message.contains(UtilsBluetooth.INVALID_GPS_COORDINATES) || message.contains(UtilsBluetooth.INVALID_GPS_COORDINATES1)) {
            // Check if the current location object exists
            if (currentLocation != null) {
                // Get the GPS coordinates from the LocationService
                String replace = currentLocation.getLatitude() + " " + currentLocation.getLongitude();
                message = message.replace(UtilsBluetooth.INVALID_GPS_COORDINATES, replace)
                        .replace(UtilsBluetooth.INVALID_GPS_COORDINATES1, replace);
            }
        }
        return message;
    }

    /**
     * Update the notification with new data.
     * Check if the message contains weather data or a reply from Bluetooth device.
     *
     * @param data specifies the new data.
     */
    private void updateNotification(String data) {
        // Remove all tabs characters from the message
        data = data.replace("\t", "");
        // Split the data to get the weather values
        String[] d = data.split("\n-");
        // Check if the message contains weather data
        if (d.length >= 7) {
            // Create a new region
            String region;
            // Check if the message contains the start message for region
            if (!d[0].contains(UtilsBluetooth.MSG_REGION_START)) {
                region = getString(R.string.no_gps_data);
            } else {
                // The message don't contain region
                // Use the data from message to create one
                region = UtilsBluetooth.MSG_REGION_START + " " + Utils.getCoordinatesFormat(
                        d[0].split("]")[1].replace("\n", "")
                                .split(UtilsBluetooth.MSG_REGION_START + " ")[1], 2, ".");
            }
            // Configure the notification layout with the new data
            notificationLayoutExpanded.setImageViewResource(R.id.logo,
                    UtilsGoogleMaps.getWeatherIcon(UtilsGoogleMaps.getWeatherStringIndex(d[1], this)));
            notificationLayout.setTextViewText(R.id.title, region + " - " + d[1]);
            notificationLayoutExpanded.setTextViewText(R.id.region, region);
            notificationLayoutExpanded.setTextColor(R.id.weather, UtilsGoogleMaps.getWeatherTextColor(UtilsGoogleMaps.getWeatherGrade(d[1], this)));
            notificationLayoutExpanded.setTextViewText(R.id.weather, d[1]);
            notificationLayoutExpanded.setTextViewText(R.id.temperature, d[2].split(": ")[1] + "℃");
            notificationLayoutExpanded.setTextViewText(R.id.humidity, d[3].split(": ")[1] + "%");
            notificationLayoutExpanded.setTextViewText(R.id.speed, d[5].split(": ")[1] + " km/h");
            notificationLayoutExpanded.setTextViewText(R.id.air, d[4].split(": ")[1] + "%");
            // Get the direction from data
            // If the direction is not valid, don't show it on notification
            String direction = d[6].split(": ")[1];
            direction = direction.equals(UtilsBluetooth.DIRECTION_UNKNOWN + "\n") ? null : direction;
            if (direction == null) {
                notificationLayoutExpanded.setViewVisibility(R.id.directionImage, View.INVISIBLE);
                notificationLayoutExpanded.setViewVisibility(R.id.directionLabel, View.INVISIBLE);
                notificationLayoutExpanded.setViewVisibility(R.id.direction, View.INVISIBLE);
            } else {
                notificationLayoutExpanded.setTextViewText(R.id.direction, direction);
                notificationLayoutExpanded.setViewVisibility(R.id.directionImage, View.VISIBLE);
                notificationLayoutExpanded.setViewVisibility(R.id.directionLabel, View.VISIBLE);
                notificationLayoutExpanded.setViewVisibility(R.id.direction, View.VISIBLE);
            }
        }
        // The message contains a reply from Bluetooth device
        else {
            // Check if the current reply is the same as the last one
            // Check if the timeout was touch to update the notification again
            // If the Bluetooth device send the same message, it will be displayed only after timeout
            if (!lastReply.equals(data) || (lastReplyCount != null
                    && Utils.getTimeDifference(lastReplyCount) > NOTIFICATION_TIME_REPLY_UPDATE)) {
                // Update the last reply
                lastReply = data;
                // Get the current date and time
                lastReplyCount = new java.util.Date();
                // Configure the notification and update it
                notificationReplyBuilder.setContentTitle(getString(R.string.reply_from) + " " + device.getName());
                notificationReplyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(data));
                notificationReplyBuilder.setTimeoutAfter(NOTIFICATION_TIME_REPLY_TIMEOUT);
                notificationManager.notify(NOTIFICATION_REPLY_ID, notificationReplyBuilder.build());
            }
        }
        // The main notification (this foreground service) is always updated (to delete the reply)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Send the data received to database.
     *
     * @param d specifies the data components.
     *          The data components must be:
     *          - latitude (from -90 to 90);
     *          - longitude (from -180 to 180);
     *          - weather code (from 100 to 499);
     *          - temperature (from -50 to 50);
     *          - humidity (from 0 to 100);
     *          - air quality or pollution (from 0 to 100);
     *          - speed (from 0 kmph);
     *          - direction (from 0 to 360 degrees or from N (North) to NW (North-West))
     */
    private void sendToDataBase(String[] d) {
        // Define new latitude and longitude
        String lat = null, lon = null;
        // Check if the current location of the last known one exists
        if (currentLocation != null) {
            // Assign the variables with valid GPS coordinates
            lat = d[0].equals("0.0") || d[0].equals(".0") ? currentLocation.getLatitude() + "" : d[0];
            lon = d[1].equals("0.0") ? currentLocation.getLongitude() + "" : d[1];
        }
        // Check the data validity
        int validity = Utils.isDataValid(lat + " " + lon, d[2], d[3], d[4], d[5].replace(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER, ""));
        // Check if the data is valid
        if (validity == Utils.VALID) {
            // Create new data object with the components
            Data data = new Data(Utils.getInt(d[2]),
                    Utils.getInt(d[3]),
                    Utils.getInt(d[4]),
                    Utils.getInt(d[5].replace(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER, "")));
            // Send the data to database
            sendDatabase(Utils.getCoordinatesWithDecimals(lat + " " + lon, 2), Utils.getCurrentDateAndTime(), data);
        }
    }

    /**
     * Connect the Bluetooth device.
     */
    private void connectBluetooth() {
        // Check if there is a selected Bluetooth device
        if (device == null) {
            // Stop the service, nothing to do without Bluetooth device
            onDestroy();
        }
        // Check if the Bluetooth socket exists
        else if (socket != null) {
            // Check if the Bluetooth device is already connected
            if (socket.isConnected()) {
                return;
            }
        }
        try {
            assert device != null;
            // Get the Bluetooth device using the default UUID
            socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Get the default Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Create the Bluetooth client that will control the Bluetooth connection
        clientClass = new BluetoothClientClass(socket, bluetoothAdapter, handler, null, null);
        // Start the thread
        clientClass.start();
    }

    // Create the receiver that will receive broadcast messages
    private final BroadcastReceiver receiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // Get the action from Intent
            String action = intent.getAction();
            // Check if the action exists
            if (action == null) return;
            // Check if the message says that the Bluetooth state changed
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                // Get the state of the intent
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                // Check the state
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    // Sent broadcast message to all active activities
                    sendMessage(SERVICE_MESSAGE_ID_BT_OFF, null);
                    // Stop this service
                    onDestroy();
                }
            }
        }
    };

    /**
     * Send message to an activity using the public SERVICE_KEY key.
     *
     * @param messageID specifies the message id of the message, used to identify it when received.
     * @param message   specifies the message that will be received.
     */
    private void sendMessage(int messageID, String message) {
        // Check if the message id is valid
        if (messageID == -1) return;
        // The string SERVICE_KEY will be used to filer the intent
        Intent intent = new Intent(SERVICE_KEY);
        // Adding some data
        intent.putExtra(SERVICE_MESSAGE_ID_KEY, messageID);
        intent.putExtra(SERVICE_MESSAGE_KEY, message);
        // Send the message as broadcast message
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Send message to an activity using the public SERVICE_FIREBASE_KEY key.
     *
     * @param coordinates specifies the coordinates of the region.
     * @param time        specifies the message time.
     * @param data        specifies the data that will be send to database.
     */
    private void sendDatabase(String coordinates, String time, Data data) {
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(FireBaseService.SERVICE_KEY);
        // Adding some data
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY, coordinates);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY, time);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY, data);
        // Send the message as broadcast message
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * This static function will receive the text inserted by user in the direct reply notification.
     * It also send the command to Bluetooth module.
     *
     * @param reply is the text inserted.
     */
    public static void getReply(String reply) {
        if (clientClass == null) return;
        if (clientClass.getSocket() == null) return;
        clientClass.getBluetoothSendReceive().write(reply.getBytes());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        SERVICE_ACTIVE = false;
        try {
            // Unregister all broadcast receivers
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            unregisterReceiver(receiverState);
            // Close the Bluetooth connection
            socket.close();
        } catch (IllegalArgumentException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
        clientClass = null;
        // Remove all notifications
        notificationManager.cancelAll();
        // Send message to all active activities that the Bluetooth device was disconnected
        sendMessage(SERVICE_MESSAGE_ID_DISCONNECTED, String.format(getString(R.string.disconnected_from_placeholder_device), device.getName()));
        // Stop the service
        stopSelf();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(currentLocation, flags);
        dest.writeParcelable(device, flags);
    }

}
