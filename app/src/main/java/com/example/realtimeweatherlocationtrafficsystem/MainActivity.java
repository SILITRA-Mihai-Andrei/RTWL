package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static int BLUETOOTH_IS_OFF = 1;
    private final static int BLUETOOTH_IS_NOT_AVAILABLE = 2;
    private final static int BLUETOOTH_IS_ON = 3;
    private final static boolean PAIRED_DEVICES = true;
    private final static boolean NO_PAIRED_DEVICES = false;
    public static boolean ENABLE_BLUETOOTH_IN_PROGRESS = false;
    public static boolean DISCOVERY_BLUETOOTH_IN_PROGRESS = false;

    //for bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices;
    private BluetoothDevice selected_device;
    private TextView bluetoothStatusTextView;
    private TextView noPairedDevices;
    private TextView selectedBluetoothDevice;
    private Button goToBluetoothSettings;
    private ListView bluetoothDevicesListView;
    //for toolbar menu
    private LinearLayout infoLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
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
    }

    private void initComponents() {
        setToolbar();
        bluetoothStatusTextView = findViewById(R.id.bluetooth_status);
        noPairedDevices = findViewById(R.id.no_paired_devices);
        selectedBluetoothDevice = findViewById(R.id.selected_bluetooth_devices);
        goToBluetoothSettings = findViewById(R.id.btn_go_to_bluetooth_settings);
        bluetoothDevicesListView = findViewById(R.id.ls_bluetooth_devices);
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

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.settings) {
                    Toast.makeText(MainActivity.this, "Settings (MUST IMPLEMENT)", Toast.LENGTH_SHORT).show();
                } else if (item.getItemId() == R.id.development) {
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
        if(mBluetoothAdapter == null) return;
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
        if(devices==null) return;
        if (deviceSplited.length != 2 && !autoSelect) {
            Toast.makeText(this, R.string.something_wrong_with_bluetooth_name, Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getName().equals(deviceSplited[0])) {
                if(autoSelect){
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.auto_selected));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                }
                else if (devices.get(i).getAddress().equals(deviceSplited[1])) {
                    selected_device = devices.get(i);
                    selectedBluetoothDevice.setText(getHtmlStringFormat(R.string.chosen_device));
                    selectedBluetoothDevice.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private Spanned getHtmlStringFormat(int string) {
        if(selected_device==null) return null;
        return Html.fromHtml(
                getResources().getString(string) + " <font color=" + getResources().getColor(R.color.color_green_light) + "><b>"
                        + selected_device.getName() + "</b></font>.");
    }

    @Override
    public void onBackPressed() {
        if(infoLinearLayout.getVisibility()==View.VISIBLE){
            infoLinearLayout.setVisibility(View.GONE);
        }
        else {
            super.onBackPressed();
            this.finishAffinity();
        }
    }

    public void goToGoogleMaps(View view) {
        startActivityAndSend(GoogleMapsActivity.class, false);
    }

    public void goToTerminal(View view) {
        /*Intent intent1 = new Intent(this, TesteActivity.class);
        intent1.putExtra("BT_DEVICE_SESSION_ID", selected_device);
        startActivity(intent1);*/
        if (selected_device == null) {
            Toast.makeText(this, R.string.please_select_one_device, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityAndSend(TerminalActivity.class, false);
    }

    public void startActivityAndSend(Class<?> destination, boolean development) {
        Intent intent = new Intent(this, destination);
        intent.putExtra("BT_DEVICE_SESSION_ID", selected_device);
        intent.putExtra("DEVELOPMENT_SESSION_ID", development);
        startActivity(intent);
    }
}