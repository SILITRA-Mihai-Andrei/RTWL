package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static int BLUETOOTH_IS_OFF = 1;
    private final static int BLUETOOTH_IS_NOT_AVAILABLE = 2;
    private final static int BLUETOOTH_IS_ON = 3;
    private final static boolean PAIRED_DEVICES = true;
    private final static boolean NO_PAIRED_DEVICES = false;
    public static boolean ENABLE_BLUETOOTH_IN_PROGRESS = false;
    public static boolean DISCOVERY_BLUETOOTH_IN_PROGRESS = false;

    private BluetoothAdapter mBluetoothAdapter;
    private android.os.Handler bluetoothHandler;
    private TextView bluetoothStatusTextView;
    private TextView noPairedDevices;
    private TextView selectedBluetoothDevice;
    private Button goToBluetoothSettings;
    private ListView bluetoothDevicesListView;
    private Switch development;
    private ArrayList<String> devices;
    private String selected_device = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothHandler = new android.os.Handler();
        bluetoothHandler.postDelayed(updateTimerThread, 0);
        initComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBluetoothDevicesListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
        bluetoothHandler.removeCallbacks(updateTimerThread);
    }

    private void initComponents() {
        bluetoothStatusTextView = findViewById(R.id.bluetooth_status);
        noPairedDevices = findViewById(R.id.no_paired_devices);
        selectedBluetoothDevice = findViewById(R.id.selected_bluetooth_devices);
        goToBluetoothSettings = findViewById(R.id.btn_go_to_bluetooth_settings);
        development = findViewById(R.id.development);
        bluetoothDevicesListView = findViewById(R.id.ls_bluetooth_devices);
        bluetoothDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selected_device = (String) parent.getItemAtPosition(position);
                selectedBluetoothDevice.setText(Html.fromHtml(
                        getResources().getString(R.string.connected_to_device) + " <font color=" + getResources().getColor(R.color.color_green) + "><b>"
                                        + selected_device.split("\n")[0] + "</b></font>."));
                selectedBluetoothDevice.setVisibility(View.VISIBLE);
            }
        });
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
    }

    private void initBluetoothDevicesListView() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothStatus();
    }

    private void updateBluetoothDevicesListView() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.item_bluetooth_device, devices);
        bluetoothDevicesListView.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
    }

    private Runnable updateTimerThread = new Runnable()
    {
        public void run()
        {
            bluetoothHandler.postDelayed(this, 2000);
            checkBluetoothStatus();
            if(selected_device.isEmpty()){
                selectedBluetoothDevice.setVisibility(View.GONE);
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Discovery has found a device. Get the BluetoothDevice
                //object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED){
                        devices.add(device.getName() + '\n' + device.getAddress());
                        updateBluetoothDevicesListView();
                        Toast.makeText(context, "found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void checkBluetoothStatus(){
        if (mBluetoothAdapter == null) {
            setBluetoothViews(BLUETOOTH_IS_NOT_AVAILABLE);
        } else if (!mBluetoothAdapter.isEnabled()) {
            setBluetoothViews(BLUETOOTH_IS_OFF);
        } else {
            setBluetoothViews(BLUETOOTH_IS_ON);
            setBluetoothDevices(mBluetoothAdapter);
        }
    }

    private void setBluetoothDevices(BluetoothAdapter adapter) {
        adapter.startDiscovery();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        devices = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            setPairedDevicesView(PAIRED_DEVICES);
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else {
            setPairedDevicesView(NO_PAIRED_DEVICES);
        }
        updateBluetoothDevicesListView();
    }

    private void setBluetoothViews(int mode) {
        if (mode == BLUETOOTH_IS_NOT_AVAILABLE) {
            bluetoothStatusTextView.setText(R.string.bluetooth_not_available);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_orange_light));
            setPairedDevicesView(NO_PAIRED_DEVICES);
        } else if (mode == BLUETOOTH_IS_OFF) {
            bluetoothStatusTextView.setText(R.string.bluetooth_is_off);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_red_light));
            setPairedDevicesView(NO_PAIRED_DEVICES);
            if(!ENABLE_BLUETOOTH_IN_PROGRESS){
                ENABLE_BLUETOOTH_IN_PROGRESS = true;
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }
        } else if (mode == BLUETOOTH_IS_ON) {
            ENABLE_BLUETOOTH_IN_PROGRESS = false;
            bluetoothStatusTextView.setText(R.string.bluetooth_is_on);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_green));
            setPairedDevicesView(PAIRED_DEVICES);
            if(devices != null && devices.size() == 0 && !DISCOVERY_BLUETOOTH_IN_PROGRESS){
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
        } else {
            noPairedDevices.setVisibility(View.VISIBLE);
            goToBluetoothSettings.setVisibility(View.VISIBLE);
        }
    }

    public void goToGoogleMaps(View view){
        if(selected_device.isEmpty()){
            Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
            //return;
        }
        Intent intent = new Intent(this, GoogleMapsActivity.class);
        intent.putExtra("BT_DEVICE_SESSION_ID", selected_device);
        startActivity(intent);
    }

    public void goToTerminal(View view){
        if(selected_device.isEmpty()){
            Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra("BT_DEVICE_SESSION_ID", selected_device);
        intent.putExtra("DEVELOPMENT_SESSION_ID", development.isChecked());
        startActivity(intent);
    }
}