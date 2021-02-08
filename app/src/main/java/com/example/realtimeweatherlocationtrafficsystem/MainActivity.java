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
import android.content.pm.PackageManager;
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

import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;
import com.example.realtimeweatherlocationtrafficsystem.services.FireBaseService;
import com.example.realtimeweatherlocationtrafficsystem.services.LocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String BT_DEVICE_SESSION_ID = "BT_DEVICE_SESSION_ID";
    public static final String DEVELOPMENT_SESSION_ID = "DEVELOPMENT_SESSION_ID";

    public static final String SERVICE_KEY = "FireBase-activities-KEY";
    public static final String SERVICE_MESSAGE_ID_KEY = "FireBase-activities-Message-ID-KEY";
    public static final int SERVICE_MESSAGE_ID_LOCATION = 200;
    public static final int SERVICE_MESSAGE_ID_REGIONS = 201;

    private final static int BLUETOOTH_IS_OFF = 1;
    private final static int BLUETOOTH_IS_NOT_AVAILABLE = 2;
    private final static int BLUETOOTH_IS_ON = 3;
    private final static boolean PAIRED_DEVICES = true;
    private final static boolean NO_PAIRED_DEVICES = false;
    public static boolean ENABLE_BLUETOOTH_IN_PROGRESS = false;
    public static boolean DISCOVERY_BLUETOOTH_IN_PROGRESS = false;

    private Intent fireBaseServiceIntent;
    private Intent locationServiceIntent;
    private LinearLayout loading;
    private TextView loading_message;

    //for bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices;
    private BluetoothDevice selected_device;
    private TextView bluetoothStatusTextView;
    private TextView noPairedDevices;
    private TextView selectedBluetoothDevice;
    private Button goToBluetoothSettings;
    private TextView connectedDeviceLabel;
    private Button sendBackground;
    private ListView bluetoothDevicesListView;
    //for toolbar menu
    private LinearLayout infoLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        openBackgroundService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBluetoothDevicesListView();
        // This registers messageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(BluetoothService.SERVICE_KEY));
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    protected void onRestart() {
        super.onRestart();
        openBackgroundService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        closeBackgroundService();
    }

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            int messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);

            if (messageID == -1) return;
            switch (messageID) {
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTING:
                    loading_message.setText(R.string.connection_to_bluetooth_device);
                    loading.setVisibility(View.VISIBLE);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                    connectedDeviceLabel.setTextColor(Color.RED);
                    connectedDeviceLabel.setText(R.string.not_connected);
                    sendBackground.setTextColor(Color.GREEN);
                    sendBackground.setText(R.string.connect);
                    loading.setVisibility(View.GONE);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                    setBluetoothViews(BLUETOOTH_IS_OFF);
                    loading.setVisibility(View.GONE);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTED:
                    connectedDeviceLabel.setTextColor(Color.GREEN);
                    connectedDeviceLabel.setText(String.format(getString(R.string.connected_to_placeholder_device), selected_device.getName()));
                    sendBackground.setTextColor(Color.RED);
                    sendBackground.setText(R.string.disconnect);
                    loading.setVisibility(View.GONE);
                    break;
            }
        }
    };

    private void initComponents() {
        setToolbar();
        bluetoothStatusTextView = findViewById(R.id.bluetooth_status);
        noPairedDevices = findViewById(R.id.no_paired_devices);
        selectedBluetoothDevice = findViewById(R.id.selected_bluetooth_devices);
        goToBluetoothSettings = findViewById(R.id.btn_go_to_bluetooth_settings);
        connectedDeviceLabel = findViewById(R.id.connected_device);
        sendBackground = findViewById(R.id.btn_send_background);
        bluetoothDevicesListView = findViewById(R.id.ls_bluetooth_devices);
        loading = findViewById(R.id.loading);
        loading_message = findViewById(R.id.loading_message);

        goToBluetoothSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings");
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        connectedDeviceLabel.setTextColor(Color.RED);
        connectedDeviceLabel.setText(R.string.not_connected);
        sendBackground.setTextColor(Color.GREEN);
        sendBackground.setText(R.string.connect);
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                } else if (item.getItemId() == R.id.development) {
                    if (!FireBaseService.SERVICE_ACTIVE || !LocationService.SERVICE_ACTIVE) {
                        openBackgroundService();
                    }
                    startActivityAndSend(TerminalActivity.class, true);
                } else if (item.getItemId() == R.id.info) {
                    infoLinearLayout.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
        setToolbarMenuViews();
    }

    private void setToolbarMenuViews() {
        infoLinearLayout = findViewById(R.id.infoLinearLayout);
        infoLinearLayout.setVisibility(View.GONE);
        Button ok = findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoLinearLayout.setVisibility(View.GONE);
            }
        });
    }

    private void initBluetoothDevicesListView() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setBluetoothDevices();
        bluetoothDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getBluetoothDevice((String) parent.getItemAtPosition(position), false);
            }
        });
        checkBluetoothStatus();
    }

    private void updateBluetoothDevicesListView() {
        List<String> deviceList = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            deviceList.add(devices.get(i).getName() + "\n" + devices.get(i).getAddress());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.item_bluetooth_device, deviceList);
        bluetoothDevicesListView.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
        getBluetoothDevice(UtilsBluetooth.MAIN_BLUETOOTH_DEVICE, true);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            checkBluetoothStatus();
        }
    };

    private void checkBluetoothStatus() {
        if (mBluetoothAdapter == null) {
            setBluetoothViews(BLUETOOTH_IS_NOT_AVAILABLE);
        } else if (!mBluetoothAdapter.isEnabled()) {
            setBluetoothViews(BLUETOOTH_IS_OFF);
        } else {
            setBluetoothDevices();
            setBluetoothViews(BLUETOOTH_IS_ON);
        }
    }

    private void setBluetoothDevices() {
        if (mBluetoothAdapter == null) return;
        mBluetoothAdapter.startDiscovery();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        devices = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            setPairedDevicesView(PAIRED_DEVICES);
            devices.addAll(pairedDevices);
        } else {
            setPairedDevicesView(NO_PAIRED_DEVICES);
        }
        updateBluetoothDevicesListView();
    }

    private void setBluetoothViews(int mode) {
        if (mode == BLUETOOTH_IS_NOT_AVAILABLE) {
            bluetoothStatusTextView.setText(R.string.bluetooth_not_available);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_orange_light));
            noPairedDevices.setVisibility(View.GONE);
            findViewById(R.id.bluetooth_devices_txt).setVisibility(View.GONE);
        } else if (mode == BLUETOOTH_IS_OFF) {
            bluetoothStatusTextView.setText(R.string.bluetooth_is_off);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_red_light));
            setPairedDevicesView(NO_PAIRED_DEVICES);
            if (!ENABLE_BLUETOOTH_IN_PROGRESS) {
                ENABLE_BLUETOOTH_IN_PROGRESS = true;
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }
        } else if (mode == BLUETOOTH_IS_ON) {
            ENABLE_BLUETOOTH_IN_PROGRESS = false;
            bluetoothStatusTextView.setText(R.string.bluetooth_is_on);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_green_light));
            getBluetoothDevice(UtilsBluetooth.MAIN_BLUETOOTH_DEVICE, true);
            setPairedDevicesView(PAIRED_DEVICES);
            if (devices != null && devices.size() == 0 && !DISCOVERY_BLUETOOTH_IN_PROGRESS) {
                DISCOVERY_BLUETOOTH_IN_PROGRESS = true;
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                startActivity(discoverableIntent);
            }
        } else assert true;
    }

    private void setPairedDevicesView(boolean mode) {
        if (mode == PAIRED_DEVICES) {
            noPairedDevices.setVisibility(View.GONE);
            goToBluetoothSettings.setVisibility(View.GONE);
            selectedBluetoothDevice.setVisibility(View.VISIBLE);
            bluetoothDevicesListView.setVisibility(View.VISIBLE);
        } else {
            noPairedDevices.setVisibility(View.VISIBLE);
            goToBluetoothSettings.setVisibility(View.VISIBLE);
            selectedBluetoothDevice.setVisibility(View.GONE);
            bluetoothDevicesListView.setVisibility(View.INVISIBLE);
        }
    }

    private void getBluetoothDevice(String device, boolean autoSelect) {
        String[] deviceSplited = device.split("\n");
        if (devices == null) return;
        if (deviceSplited.length != 2 && !autoSelect) {
            Toast.makeText(this, R.string.something_wrong_with_bluetooth_name, Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getName().equals(deviceSplited[0])) {
                if (autoSelect) {
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.auto_selected));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                } else if (devices.get(i).getAddress().equals(deviceSplited[1])) {
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.chosen_device));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private Spanned getHtmlStringFormat(int string) {
        if (selected_device == null) return null;
        return Html.fromHtml(
                getResources().getString(string) + " <font color=" + getResources().getColor(R.color.color_green_light) + "><b>"
                        + selected_device.getName() + "</b></font>.");
    }

    @Override
    public void onBackPressed() {
        if (infoLinearLayout.getVisibility() == View.VISIBLE) {
            infoLinearLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
            this.finishAffinity();
        }
    }

    public void openBluetoothService() {
        Intent bluetoothServiceIntent = new Intent(this, BluetoothService.class);

        if (BluetoothService.SERVICE_ACTIVE) {
            stopService(bluetoothServiceIntent);
        } else {
            if (selected_device == null) {
                Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothServiceIntent.putExtra(MainActivity.BT_DEVICE_SESSION_ID, selected_device);
            ContextCompat.startForegroundService(this, bluetoothServiceIntent);
        }
    }

    public void openBluetoothService(View view) {
        openBluetoothService();
    }

    public void openBackgroundService(){
        fireBaseServiceIntent = new Intent(this, FireBaseService.class);
        locationServiceIntent = new Intent(this, LocationService.class);
        this.startService(fireBaseServiceIntent);
        this.startService(locationServiceIntent);
    }

    public void closeBackgroundService(){
        if (FireBaseService.SERVICE_ACTIVE) {
            stopService(fireBaseServiceIntent);
        }
        if (LocationService.SERVICE_ACTIVE) {
            stopService(locationServiceIntent);
        }
    }

    public void goToGoogleMaps(View view) {
        if (!BluetoothService.SERVICE_ACTIVE && !LocationService.SERVICE_ACTIVE) {
            Toast.makeText(this, getString(R.string.gps_module_gps_phone_not_working), Toast.LENGTH_LONG).show();
            return;
        }
        startActivityAndSend(GoogleMapsActivity.class, false);
    }

    public void goToTerminal(View view) {
        if (selected_device == null) {
            Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
            return;
        } else if (!BluetoothService.SERVICE_ACTIVE) {
            Toast.makeText(this, getString(R.string.need_to_connect), Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityAndSend(TerminalActivity.class, false);
    }

    private void end() {
        try {
            //Unregister the ACTION_FOUND receiver.
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            // Unregister since the activity is not visible
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void startActivityAndSend(Class<?> destination, boolean development) {
        end();
        Intent intent = new Intent(this, destination);
        intent.putExtra(BT_DEVICE_SESSION_ID, selected_device);
        intent.putExtra(DEVELOPMENT_SESSION_ID, development);
        startActivity(intent);
    }
}