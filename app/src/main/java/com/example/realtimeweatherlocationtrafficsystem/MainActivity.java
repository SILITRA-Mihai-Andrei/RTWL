package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;
import com.example.realtimeweatherlocationtrafficsystem.services.FireBaseService;
import com.example.realtimeweatherlocationtrafficsystem.services.LocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This is the first screen of the app.
 * Here, the app will ask for required permissions and will make sure that all necessary services will be started.
 * Also, on the screen it will be displayed all available Bluetooth paired devices.
 * The activity will auto select the common Bluetooth device available in settings.
 * The activity will receive broadcast messages from services.
 */
public class MainActivity extends AppCompatActivity {

    // Variables for passing objects to intends
    public static final String BT_DEVICE_SESSION_ID = "BT_DEVICE_SESSION_ID";
    public static final String DEVELOPMENT_SESSION_ID = "DEVELOPMENT_SESSION_ID";

    // Variables that stores the services keys
    public static final String SERVICE_KEY = "FireBase-activities-KEY";
    public static final String SERVICE_MESSAGE_ID_KEY = "FireBase-activities-Message-ID-KEY";

    // Variables that stores the id for messages received from broadcasts
    public static final int SERVICE_MESSAGE_ID_LOCATION = 200;
    public static final int SERVICE_MESSAGE_ID_REGIONS = 201;

    // Variables for Bluetooth connection
    public static boolean ENABLE_BLUETOOTH_IN_PROGRESS = false;
    public static boolean DISCOVERY_BLUETOOTH_IN_PROGRESS = false;

    // Variables for Bluetooth connectivity
    private final static int BLUETOOTH_IS_OFF = 1;
    private final static int BLUETOOTH_IS_NOT_AVAILABLE = 2;
    private final static int BLUETOOTH_IS_ON = 3;
    private final static boolean PAIRED_DEVICES = true;
    private final static boolean NO_PAIRED_DEVICES = false;

    // Background services intents
    private Intent fireBaseServiceIntent;
    private Intent locationServiceIntent;

    // Bluetooth objects
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices;     // all paired devices
    private BluetoothDevice selected_device;        // the selected device

    // UI components for Bluetooth connection
    private LinearLayout loading;
    private TextView loading_message;
    private TextView bluetoothStatusTextView;
    private TextView noPairedDevices;
    private TextView selectedBluetoothDevice;
    private Button goToBluetoothSettings;
    private TextView connectedDeviceLabel;
    private Button sendBackground;
    private ListView bluetoothDevicesListView;

    // Toolbar menu
    private LinearLayout infoLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize UI components
        initComponents();
        // Open background services required for app (Firebase, Location)
        openBackgroundService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the Bluetooth UI components
        initBluetoothDevicesListView();
        // This registers messageReceiver to receive messages from broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(BluetoothService.SERVICE_KEY));
        // Check if the location is enabled
        if (!Utils.isLocationEnabled(getContentResolver(), this)) {
            // Ask for permission is if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the background services if not necessary
        closeBackgroundService();
    }

    // Handling the received messages from Bluetooth Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract the message id from the intent received from broadcast
            int messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);
            // Check if the message is valid
            if (messageID == -1) return;
            switch (messageID) {
                // Trying to connect to Bluetooth device
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTING:
                    // Show connecting view over main screen - waiting to connect
                    loading_message.setText(R.string.connection_to_bluetooth_device);
                    loading.setVisibility(View.VISIBLE);
                    break;
                // Any of the cases below means that Bluetooth device lost connection with the phone
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                    // Update the Bluetooth views
                    connectedDeviceLabel.setTextColor(Color.RED);
                    connectedDeviceLabel.setText(R.string.not_connected);
                    sendBackground.setTextColor(Color.GREEN);
                    sendBackground.setText(R.string.connect);
                    loading.setVisibility(View.GONE);
                    break;
                // Detected that user disabled Bluetooth
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                    setBluetoothViews(BLUETOOTH_IS_OFF);
                    loading.setVisibility(View.GONE);
                    break;
                // Bluetooth device successfully connected to the phone
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTED:
                    // Update the Bluetooth views for connected Bluetooth device
                    connectedDeviceLabel.setTextColor(Color.GREEN);
                    connectedDeviceLabel.setText(String.format(getString(R.string.connected_to_placeholder_device), selected_device.getName()));
                    sendBackground.setTextColor(Color.RED);
                    sendBackground.setText(R.string.disconnect);
                    loading.setVisibility(View.GONE);
                    break;
            }
        }
    };

    /**
     * Initialize the UI components and add listeners.
     */
    private void initComponents() {
        // Initialize the toolbar menu
        setToolbar();
        // Find the UI components
        bluetoothStatusTextView = findViewById(R.id.bluetooth_status);
        noPairedDevices = findViewById(R.id.no_paired_devices);
        selectedBluetoothDevice = findViewById(R.id.selected_bluetooth_devices);
        goToBluetoothSettings = findViewById(R.id.btn_go_to_bluetooth_settings);
        connectedDeviceLabel = findViewById(R.id.connected_device);
        sendBackground = findViewById(R.id.btn_send_background);
        bluetoothDevicesListView = findViewById(R.id.ls_bluetooth_devices);
        loading = findViewById(R.id.loading);
        loading_message = findViewById(R.id.loading_message);

        // Set listener for click on the button
        // This button will be visible on the screen when the Bluetooth is OFF
        // The user will be redirected to Bluetooth settings to enable it
        goToBluetoothSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Create the redirection intent to Bluetooth settings
                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                // Set the component to intent to show where to redirect (Bluetooth settings)
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings");
                // Configure the intent for suitable redirecting to Bluetooth settings from this activity
                intent.addCategory(Intent.CATEGORY_LAUNCHER);   // add the type of category for intent
                intent.setComponent(cn);                        // show where to redirect
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // create a new task for this intent
                startActivity(intent);                          // start the intent to force user to enable Bluetooth
            }
        });
        // Check if the Bluetooth service is active and working
        // Configure the UI components corresponding to Bluetooth service state (ON/OFF)
        if (BluetoothService.SERVICE_ACTIVE) {
            connectedDeviceLabel.setTextColor(Color.GREEN);
            connectedDeviceLabel.setText(getString(R.string.connected_to_placeholder_device, BluetoothService.device.getName()));
            sendBackground.setTextColor(Color.RED);
            sendBackground.setText(R.string.disconnect);
        } else {
            connectedDeviceLabel.setTextColor(Color.RED);
            connectedDeviceLabel.setText(R.string.not_connected);
            sendBackground.setTextColor(Color.GREEN);
            sendBackground.setText(R.string.connect);
        }
    }

    /**
     * Initialize and configure the toolbar menu in MainActivity
     */
    private void setToolbar() {
        // Find the UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Configure the toolbar object
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.menu_main);
        // Set listener for click on the toolbar menu
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Check which item from menu was clicked/selected/taped
                switch (item.getItemId()) {
                    case R.id.settings:
                        // Start SettingsActivity
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;
                    case R.id.development:
                        // Check if the mandatory services are active
                        if (!FireBaseService.SERVICE_ACTIVE || !LocationService.SERVICE_ACTIVE) {
                            // Open the mandatory services that are inactive
                            openBackgroundService();
                        }
                        // Start the TerminalActivity in development mode
                        // In this mode can be sent data to database written by user and not received from Bluetooth device
                        startActivityAndSend(TerminalActivity.class, true);
                        break;
                    case R.id.info:
                        // Show the info view
                        infoLinearLayout.setVisibility(View.VISIBLE);
                        break;
                }
                return false;
            }
        });
        // Set the UI components for toolbar menu
        setToolbarMenuViews();
    }

    /**
     * Set the UI components for toolbar menu.
     * Set listeners for buttons.
     * Hide all views that must be visible after the user click on a item menu.
     */
    private void setToolbarMenuViews() {
        // Find UI components from toolbar menu layout
        infoLinearLayout = findViewById(R.id.infoLinearLayout);
        Button ok = findViewById(R.id.ok);
        // Hide the views
        infoLinearLayout.setVisibility(View.GONE);
        // Set listener for click on the button that will hide the info view
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide the view
                infoLinearLayout.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Initialize and add listeners for Bluetooth components on the screen.
     * Assign the default Bluetooth adapter and start searching for Bluetooth paired devices.
     * Check if the Bluetooth is enabled or disabled.
     */
    private void initBluetoothDevicesListView() {
        // Get the default Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Search for paired Bluetooth devices and populate the list view
        setBluetoothDevices();
        // Check if the Bluetooth is enabled or disabled
        checkBluetoothStatus();
        // Set listener for click on each item from the list view (Bluetooth paired devices)
        bluetoothDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // The user clicked on a paired Bluetooth device
                // Get the item and pass to the function that will extract the Bluetooth device
                getBluetoothDevice((String) parent.getItemAtPosition(position), false);
            }
        });
    }

    /**
     * Update the Bluetooth device list view using the Bluetooth device list received from Bluetooth adapter.
     * Auto select the common Bluetooth device if exists in the received list. (ex: HC-05, HC-06)
     */
    private void updateBluetoothDevicesListView() {
        // Create a new list where the Bluetooth paired devices will be stored
        List<String> deviceList = new ArrayList<>();
        // Loop through all Bluetooth paired devices received from adapter
        for (int i = 0; i < devices.size(); i++) {
            // Add it to the list
            deviceList.add(devices.get(i).getName() + "\n" + devices.get(i).getAddress());
        }
        // Create an ArrayAdapter object to control the list view where the list of devices will be shown
        // The adapter will get the list view layout that will be populated with Bluetooth paired devices
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.item_bluetooth_device, deviceList);
        // Set to list view the created adapter that will populate the view
        bluetoothDevicesListView.setAdapter(arrayAdapter);
        // Update the UI to show the changes made by adapter
        arrayAdapter.notifyDataSetChanged();
        // Auto select the most common Bluetooth device that is used for Bluetooth communication (ex: HC-05, HC-06)
        getBluetoothDevice(UtilsBluetooth.MAIN_BLUETOOTH_DEVICE, true);
        // If the common Bluetooth was not found in the list, search for the next common one
        if (selected_device == null) {
            getBluetoothDevice(UtilsBluetooth.MAIN_AUXILIARY_BLUETOOTH_DEVICE, true);
        }
    }

    /**
     * Check the Bluetooth status using the default Bluetooth adapter.
     * Set the Bluetooth UI components accordingly.
     */
    private void checkBluetoothStatus() {
        // Check if Bluetooth adapter exists
        if (mBluetoothAdapter == null) {
            // If not, Bluetooth is not available on this phone
            // Set Bluetooth UI components accordingly
            setBluetoothViews(BLUETOOTH_IS_NOT_AVAILABLE);
            // Bluetooth adapter exists and is checking the status
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is NOT enabled, set Bluetooth UI components for Bluetooth OFF
            setBluetoothViews(BLUETOOTH_IS_OFF);
        } else {
            // Bluetooth is enabled, set the Bluetooth paired devices list received from adapter
            setBluetoothDevices();
            // Set the Bluetooth UI components for Bluetooth ON
            setBluetoothViews(BLUETOOTH_IS_ON);
        }
    }

    /**
     * Get the Bluetooth paired devices received from the Bluetooth adapter.
     * Start discovering Bluetooth devices.
     * Set the UI components according to the Bluetooth paired devices list received.
     */
    private void setBluetoothDevices() {
        // Check if the adapter is available
        if (mBluetoothAdapter == null) return;
        // Start discovering Bluetooth devices
        mBluetoothAdapter.startDiscovery();
        // Get the Bluetooth paired devices list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // Create a new list for the Bluetooth paired devices received from adapter
        devices = new ArrayList<>();
        // Check if there are Bluetooth paired devices in received list
        if (pairedDevices.size() > 0) {
            // There is at least one Bluetooth paired device
            // Set the UI components for that
            setPairedDevicesView(PAIRED_DEVICES);
            // Add to list the received list from Bluetooth adapter
            devices.addAll(pairedDevices);
        } else {
            // Set the UI components for NO Bluetooth paired devices
            setPairedDevicesView(NO_PAIRED_DEVICES);
        }
        // Update the UI components for list view
        updateBluetoothDevicesListView();
    }

    /**
     * Set Bluetooth UI components according to Bluetooth status.
     * Bluetooth status can be ON, OFF or NOT AVAILABLE.
     *
     * @param mode is the mode that indicates how the UI components must be shown.
     */
    private void setBluetoothViews(int mode) {
        if (mode == BLUETOOTH_IS_NOT_AVAILABLE) {
            bluetoothStatusTextView.setText(R.string.bluetooth_not_available);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_orange_light));
            // Hide the views that are active only with Bluetooth enabled
            noPairedDevices.setVisibility(View.GONE);
            findViewById(R.id.bluetooth_devices_txt).setVisibility(View.GONE);
        } else if (mode == BLUETOOTH_IS_OFF) {
            bluetoothStatusTextView.setText(R.string.bluetooth_is_off);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_red_light));
            // Hide the views that are active only if there are paired devices
            setPairedDevicesView(NO_PAIRED_DEVICES);

            // Check if there is an intent that is enabling Bluetooth
            if (!ENABLE_BLUETOOTH_IN_PROGRESS) {
                // Create a new intent that will try to enable Bluetooth
                ENABLE_BLUETOOTH_IN_PROGRESS = true;
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }
        } else if (mode == BLUETOOTH_IS_ON) {
            ENABLE_BLUETOOTH_IN_PROGRESS = false;
            bluetoothStatusTextView.setText(R.string.bluetooth_is_on);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_green_light));
            // After Bluetooth is ON or re-enabled, get the most common Bluetooth paired device (HC-05)
            getBluetoothDevice(UtilsBluetooth.MAIN_BLUETOOTH_DEVICE, true);
            // Set the UI components according to Bluetooth ON/re-enabled
            setPairedDevicesView(PAIRED_DEVICES);
            // Check if the list of Bluetooth paired devices has items and if the Bluetooth discovering is still in progress
            if (devices != null && devices.size() == 0 && !DISCOVERY_BLUETOOTH_IN_PROGRESS) {
                // There is not Bluetooth paired device, so start discovering again
                DISCOVERY_BLUETOOTH_IN_PROGRESS = true;
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                startActivity(discoverableIntent);
            }
        } else assert true;
    }

    /**
     * Set the UI components according to the received mode.
     *
     * @param mode is the mode that indicates how the UI components must be shown.
     */
    private void setPairedDevicesView(boolean mode) {
        // Check the mode received
        if (mode == PAIRED_DEVICES) {
            // Set the UI components for the case when there is at least one Bluetooth paired device
            noPairedDevices.setVisibility(View.GONE);
            goToBluetoothSettings.setVisibility(View.GONE);
            selectedBluetoothDevice.setVisibility(View.VISIBLE);
            bluetoothDevicesListView.setVisibility(View.VISIBLE);
        } else {
            // Set the UI components for the case when there is NO Bluetooth paired device
            noPairedDevices.setVisibility(View.VISIBLE);
            goToBluetoothSettings.setVisibility(View.VISIBLE);
            selectedBluetoothDevice.setVisibility(View.GONE);
            bluetoothDevicesListView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Get the BluetoothDevice object from the String passed as parameter.
     *
     * @param device     is the device name and (eventually) address.
     * @param autoSelect is the mode that allow the method to auto select the first Bluetooth device from list using only the name.
     *                   autoSelect=false - will select the Bluetooth device with the same name and address;
     *                   autoSelect=true - will select the first Bluetooth device that have the same name.
     */
    private void getBluetoothDevice(String device, boolean autoSelect) {
        // Split the String received from the item selected by user from list view
        String[] deviceSplited = device.split("\n");
        // Check if the device list exists
        if (devices == null) return;
        // Check if the String contains two Strings separates by \n
        if (deviceSplited.length != 2 && !autoSelect) {
            // The String received is not valid for autoSelect=false
            Toast.makeText(this, R.string.something_wrong_with_bluetooth_name, Toast.LENGTH_SHORT).show();
            return;
        }
        // Loop through all Bluetooth paired devices to get the one selected by user and passed as parameter
        for (int i = 0; i < devices.size(); i++) {
            // Check if the current Bluetooth device from list has the same name as the one selected from the list view
            if (devices.get(i).getName().equals(deviceSplited[0])) {
                // Check if the autoSelect mode is active
                if (autoSelect) {
                    // Select the first Bluetooth device with the same name as @param device
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.auto_selected));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                } else if (devices.get(i).getAddress().equals(deviceSplited[1])) {
                    // Select the Bluetooth device with the same name and address as the String received as parameter
                    // The name and the address is separated with a character (ex: '\n')
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.chosen_device));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Get a Spanned object formatted from a string with HTML.
     *
     * @param string is the resource string.
     * @return a Spanned object containing the received String as parameter and the selected Bluetooth device formatted with green color and bold.
     */
    private Spanned getHtmlStringFormat(int string) {
        // Check if there is a selected Bluetooth device
        if (selected_device == null) return null;
        // Create and return a Spanned object created in Html class, using the parameter received and the format
        return Html.fromHtml(
                getResources().getString(string) + " <font color=" + getResources().getColor(R.color.color_green_light) +
                        "><b>" + selected_device.getName() + "</b></font>.");
    }

    /**
     * Start Bluetooth foreground service.
     * If the service is already active, the method will stop it.
     */
    public void openBluetoothService() {
        // Create the intent that will activate the service
        Intent bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        // Set the flag that will tell the intent to start in a new task
        // This helps in keeping the smooth in UI
        bluetoothServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Check if the service is already active
        if (BluetoothService.SERVICE_ACTIVE) {
            // Stop the service if active
            stopService(bluetoothServiceIntent);
        } else {
            // Check if there is a selected Bluetooth device to connect to
            if (selected_device == null) {
                // Can't start the Bluetooth service without a selected Bluetooth device
                Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
                return;
            }
            // Put the selected device in intent to transfer it to the service
            bluetoothServiceIntent.putExtra(MainActivity.BT_DEVICE_SESSION_ID, selected_device);
            // Start the foreground Bluetooth service
            ContextCompat.startForegroundService(this, bluetoothServiceIntent);
        }
    }

    /**
     * Method called from button layout. It will call the extended method.
     * If the Bluetooth service is already active, it will stop it.
     * If the Bluetooth service is not active, it will start it.
     */
    public void openBluetoothService(View view) {
        // Call the extended method
        openBluetoothService();
    }

    /**
     * Open the background services if not active.
     * The background services will be started in a new task.
     */
    public void openBackgroundService() {
        // Check if the FireBase service is already active
        if (!FireBaseService.SERVICE_ACTIVE) {
            // Create the intent that will open the Firebase background service
            fireBaseServiceIntent = new Intent(this, FireBaseService.class);
            fireBaseServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // create a new task for this intent
            // Start the service
            startService(fireBaseServiceIntent);
        }
        if (!LocationService.SERVICE_ACTIVE) {
            // Create the intent that will open the Location background service
            locationServiceIntent = new Intent(this, LocationService.class);
            locationServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // create a new task for this intent
            startService(locationServiceIntent);
        }
    }

    /**
     * Close the background services if are active.
     */
    public void closeBackgroundService() {
        // Check if the Bluetooth service is active
        if (FireBaseService.SERVICE_ACTIVE) {
            // Check if the Bluetooth service is active
            if (BluetoothService.SERVICE_ACTIVE) {
                // If Bluetooth service is active, the Firebase service is needed
                Toast.makeText(this, getString(R.string.stop_bluetooth_service_first), Toast.LENGTH_LONG).show();
            } else {
                // Stop the service
                stopService(fireBaseServiceIntent);
            }
        }
        // Check if the location service is active
        if (LocationService.SERVICE_ACTIVE) {
            // Check if Bluetooth service received at least one valid GPS coordinates from GPS module
            if (BluetoothService.GPS_MODULE_WORKING) {
                // GPS module can supply GPS coordinates instead of this service
                stopService(locationServiceIntent);
            } else {
                // GPS module didn't receive any valid GPS coordinates
                // The service can't be stopped, because there will be no location available to use
                Toast.makeText(this, getString(R.string.no_gps_data_without_service), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This method will start the GoogleMapsActivity.
     * To start the new activity, Bluetooth service and Location service must be active.
     */
    public void goToGoogleMaps(View view) {
        // Check if mandatory services and features are active
        if (!BluetoothService.SERVICE_ACTIVE && (!LocationService.SERVICE_ACTIVE || !BluetoothService.GPS_MODULE_WORKING)) {
            Toast.makeText(this, getString(R.string.no_gps_data), Toast.LENGTH_LONG).show();
            return;
        }
        // Start the new activity
        startActivityAndSend(GoogleMapsActivity.class, false);
    }

    /**
     * This method will start the TerminalActivity.
     * To start the new activity, there must be a selected Bluetooth device in MainActivity.
     * Also, the Bluetooth service must be active.
     */
    public void goToTerminal(View view) {
        // Check if there is a selected Bluetooth device
        if (selected_device == null) {
            // The activity can't start without a Bluetooth device
            Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
            return;
            // Check if the Bluetooth service is active
        } else if (!BluetoothService.SERVICE_ACTIVE) {
            // The activity can't start without Bluetooth service
            Toast.makeText(this, getString(R.string.need_to_connect), Toast.LENGTH_SHORT).show();
            return;
        }
        // Start the TerminalActivity
        startActivityAndSend(TerminalActivity.class, false);
    }

    /**
     * Unregister all broadcasts receivers.
     */
    private void end() {
        try {
            // Unregister the broadcast receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            // The receiver is already unregistered or wasn't registered
            e.printStackTrace();
        }
    }

    /**
     * Start new activity and pass to intent the selected Bluetooth device and the mode (development or not).
     * The method will safely stop the current activity, unregistering all broadcasts receivers.
     *
     * @param destination is the activity you want to start.
     * @param development is the mode; true means development mode and false means user mode.
     */
    public void startActivityAndSend(Class<?> destination, boolean development) {
        // Unregister all registered broadcasts receivers
        end();
        // Create the intent that will call the activity
        Intent intent = new Intent(this, destination);
        // Insert the objects to intent to pass them to new activity
        intent.putExtra(BT_DEVICE_SESSION_ID, selected_device);     // the selected Bluetooth device
        intent.putExtra(DEVELOPMENT_SESSION_ID, development);       // the mode
        // Start the new activity
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        end(); // unregister all broadcast receivers
        super.onBackPressed();
        this.finishAffinity();
    }
}