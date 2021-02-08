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
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsGoogleMaps;

import java.io.IOException;
import java.util.List;

@SuppressLint("ParcelCreator")
public class BluetoothService extends Service implements Parcelable, FireBaseManager.onFireBaseDataNew {

    public static boolean SERVICE_ACTIVE;

    public static final String SERVICE_CHANNEL_NAME = "Foreground Service Channel";
    public static final String SERVICE_STOP_KEY = "BluetoothService-Stop-KEY";
    public static final String SERVICE_KEY = "BluetoothService-KEY";
    public static final String SERVICE_MESSAGE_ID_KEY = "BluetoothService-Message-ID-KEY";
    public static final String SERVICE_MESSAGE_KEY = "BluetoothService-Message-KEY";

    public static final String SERVICE_COMMAND_STOP = "STOP";
    public static final String SERVICE_COMMAND_SEND = "SEND";

    // Service message for Bluetooth
    public static final int SERVICE_MESSAGE_ID_CONNECTION_FAILED = 0;
    public static final int SERVICE_MESSAGE_ID_CONNECTING = 1;
    public static final int SERVICE_MESSAGE_ID_CONNECTED = 2;
    public static final int SERVICE_MESSAGE_ID_DISCONNECTED = 3;
    public static final int SERVICE_MESSAGE_ID_RW_FAILED = 4;
    public static final int SERVICE_MESSAGE_ID_SENT = 5;
    public static final int SERVICE_MESSAGE_ID_RECEIVED = 6;
    public static final int SERVICE_MESSAGE_ID_BT_OFF = 10;

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final String CHANNEL_REPLY_ID = "ForegroundServiceReplyChannel";
    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_REPLY_ID = 2;
    public static final String NOTIFICATION_REPLY_KEY = "KEY_TEXT_REPLY";
    public static final String NOTIFICATION_TITLE = "RTWL Traffic System";

    private Location currentLocation = null;

    private Notification notification;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationCompat.Builder notificationReplyBuilder;
    private RemoteViews notificationLayout;
    private RemoteViews notificationLayoutExpanded;
    private NotificationManager notificationManager;

    @SuppressLint("StaticFieldLeak")
    static BluetoothClientClass clientClass;
    public static BluetoothDevice device;
    private BluetoothSocket socket = null;

    public BluetoothService() {
        SERVICE_ACTIVE = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceiver(receiverState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(BluetoothService.SERVICE_STOP_KEY));

        createNotificationChannel(CHANNEL_ID);
        createNotificationChannel(CHANNEL_REPLY_ID);
        setNotification();

        startForeground(NOTIFICATION_ID, notification);

        SERVICE_ACTIVE = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (socket == null || !socket.isConnected()) {
            device = intent.getParcelableExtra(MainActivity.BT_DEVICE_SESSION_ID);
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

    private void createNotificationChannel(String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channel,
                    SERVICE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public Handler handler = new android.os.Handler(new android.os.Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UtilsBluetooth.STATE_CONNECTING:
                    String strConnecting = String.format(getString(R.string.connecting_to_placeholder_device), device.getName());
                    notificationLayout.setTextViewText(R.id.title, strConnecting);
                    notificationLayoutExpanded.setTextViewText(R.id.title, strConnecting);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTING,
                            String.format(getString(R.string.connecting_to_placeholder_device), device.getName()));
                    break;
                case UtilsBluetooth.STATE_CONNECTED:
                    String stringConnected = String.format(getString(R.string.connected_to_placeholder_device), device.getName());
                    notificationLayout.setTextViewText(R.id.title, stringConnected);
                    notificationLayoutExpanded.setTextViewText(R.id.title, stringConnected);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    Toast.makeText(BluetoothService.this, stringConnected, Toast.LENGTH_SHORT).show();
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTED, stringConnected);
                    break;
                case UtilsBluetooth.STATE_READING_WRITING_FAILED:
                    String stringRW = String.format(getString(R.string.rw_failed), device.getName());
                    // Reading or writing was NOT successful
                    sendMessage(SERVICE_MESSAGE_ID_RW_FAILED, stringRW);
                    onDestroy();
                    break;
                case UtilsBluetooth.STATE_CONNECTION_FAILED:
                    String stringFailed = getString(R.string.connection_failed);
                    Toast.makeText(BluetoothService.this, stringFailed, Toast.LENGTH_SHORT).show();
                    sendMessage(SERVICE_MESSAGE_ID_CONNECTION_FAILED, stringFailed);
                    onDestroy();
                    break;
                case UtilsBluetooth.STATE_MESSAGE_SEND:
                    sendMessage(SERVICE_MESSAGE_ID_SENT, getString(R.string.message_sent));
                    break;
                case UtilsBluetooth.STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    if (readBuffer == null) break;
                    String message = new String(readBuffer, 0, msg.arg1);
                    if (message.isEmpty()) break;
                    String response = UtilsBluetooth.getReceivedMessage(message, getBaseContext());

                    if (response != null) {
                        if (response.isEmpty()) break;
                        if (LocationService.SERVICE_ACTIVE && LocationService.currentLocation != null) {
                            currentLocation = LocationService.currentLocation;
                        }
                        if (response.split(UtilsBluetooth.MESSAGE_TIME_END)[1].length() > 3) {
                            String[] splited = response.split("@");
                            splited[0] = checkLocation(splited[0]);
                            if (splited.length == 2) {
                                splited[1] = checkLocation(splited[1]);
                                sendMessage(SERVICE_MESSAGE_ID_RECEIVED, splited[0] + "@" + splited[1]);
                                String[] d = splited[1].split(" ");
                                if (currentLocation == null && d[0].equals("0.0") && d[1].equals("0.0")) {
                                    updateNotification(splited[0]);
                                    break;
                                }
                                updateNotification(splited[0]);
                                sendToDataBase(d);
                            } else {
                                sendMessage(SERVICE_MESSAGE_ID_RECEIVED, splited[0]);
                                updateNotification(response.split(
                                        UtilsBluetooth.MESSAGE_TIME_END)[1].replace("\n", ""));
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // onClick will close the notification
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    private String checkLocation(String location) {
        if (location.contains(UtilsBluetooth.MUST_GET_LOCATION)) {
            if (currentLocation == null) {
                location = location.replace(
                        UtilsBluetooth.MUST_GET_LOCATION,
                        UtilsBluetooth.MUST_GET_LOCATION_STRING);
            } else {
                location = location.replace(
                        UtilsBluetooth.MUST_GET_LOCATION,
                        getString(R.string.region_dots) + " " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
            }
        } else if (location.contains(UtilsBluetooth.INVALID_GPS_COORDINATES) || location.contains(UtilsBluetooth.INVALID_GPS_COORDINATES1)) {
            if (currentLocation != null) {
                String replace = currentLocation.getLatitude() + " " + currentLocation.getLongitude();
                location = location.replace(UtilsBluetooth.INVALID_GPS_COORDINATES, replace)
                        .replace(UtilsBluetooth.INVALID_GPS_COORDINATES1, replace);
            }
        }
        return location;
    }

    private void updateNotification(String data) {
        data = data.replace("\t", "");
        String[] d = data.split("\n-");
        if (d.length >= 7) {
            String region;
            try {
                region = UtilsBluetooth.MSG_REGION_START + " " + Utils.getCoordinatesFormat(
                        d[0].split("]")[1].replace("\n", "")
                                .split(UtilsBluetooth.MSG_REGION_START + " ")[1], 2, ".");
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return;
            }
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
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        } else {
            notificationReplyBuilder.setContentTitle(getString(R.string.reply_from) + " " + device.getName());
            notificationReplyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(data));
            notificationReplyBuilder.setTimeoutAfter(5000);
            notificationManager.notify(NOTIFICATION_REPLY_ID, notificationReplyBuilder.build());
        }
    }

    private void sendToDataBase(String[] d) {
        String lat = null, lon = null;
        if (currentLocation != null) {
            lat = d[0].equals("0.0") || d[0].equals(".0") ? currentLocation.getLatitude() + "" : d[0];
            lon = d[1].equals("0.0") ? currentLocation.getLongitude() + "" : d[1];
        }
        int validity = Utils.isDataValid(lat + " " + lon, d[2], d[3], d[4], d[5].replace(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER, ""));
        if (validity == Utils.VALID) {
            Data data = new Data(Utils.getInt(d[2]),
                    Utils.getInt(d[3]),
                    Utils.getInt(d[4]),
                    Utils.getInt(d[5].replace(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER, "")));
            sendDatabase(Utils.getCoordinatesForDataBase(lat + " " + lon, 2), Utils.getCurrentDateAndTime(), data);
        }
    }

    private void connectBluetooth() {
        if (device == null) {
            onDestroy();
        } else if (socket != null) {
            if (socket.isConnected()) {
                return;
            }
        }
        try {
            assert device != null;
            socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        clientClass = new BluetoothClientClass(socket, bluetoothAdapter, handler, null, null);
        clientClass.start();
    }

    private final BroadcastReceiver receiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Toast.makeText(context, action, Toast.LENGTH_SHORT).show();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    sendMessage(SERVICE_MESSAGE_ID_BT_OFF, null);
                    onDestroy();
                }
            }
        }
    };

    /**
     * Send message to an activity using the public SERVICE_KEY key.
     */
    private void sendMessage(int messageID, String message) {
        if (messageID == -1) return;
        // The string SERVICE_KEY will be used to filer the intent
        Intent intent = new Intent(SERVICE_KEY);
        // Adding some data
        intent.putExtra(SERVICE_MESSAGE_ID_KEY, messageID);
        intent.putExtra(SERVICE_MESSAGE_KEY, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Send message to an activity using the public SERVICE_FIREBASE_KEY key.
     */
    private void sendDatabase(String coordinates, String time, Data data) {
        // The string SERVICE_FIREBASE_KEY will be used to filer the intent
        Intent intent = new Intent(FireBaseService.SERVICE_KEY);
        // Adding some data
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY, coordinates);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY, time);
        intent.putExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY, data);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        try {
            unregisterReceiver(receiverState);
            socket.close();
        } catch (IllegalArgumentException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
        clientClass = null;
        notificationManager.cancelAll();
        sendMessage(SERVICE_MESSAGE_ID_DISCONNECTED, String.format(getString(R.string.disconnected_from_placeholder_device), device.getName()));
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

    @Override
    public void onDataNewFireBase(List<Region> regions) {

    }
}
