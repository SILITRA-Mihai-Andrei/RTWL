package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView bluetoothStatusTextView;
    private ListView bluetoothDevicesListView;
    private ArrayList<String> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothStatusTextView = findViewById(R.id.bluetooth_status);
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

    private void initBluetoothDevicesListView(){
        bluetoothDevicesListView = findViewById(R.id.ls_bluetooth_devices);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            bluetoothStatusTextView.setText(R.string.bluetooth_not_available);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_orange_light));
        }
        else if(!mBluetoothAdapter.isEnabled()){
            bluetoothStatusTextView.setText(R.string.bluetooth_is_off);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_red_light));
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        else{
            mBluetoothAdapter.startDiscovery();
            bluetoothStatusTextView.setText(R.string.bluetooth_is_on);
            bluetoothStatusTextView.setTextColor(getResources().getColor(R.color.color_green));
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            devices = new ArrayList<>();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device.getName() + "\n" + device.getAddress());
                }
            }
            updateBluetoothDevicesListView();
        }
    }

    private void updateBluetoothDevicesListView() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.item_bluetooth_device, devices);
        bluetoothDevicesListView.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Discovery has found a device. Get the BluetoothDevice
                //object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null ) {
                    devices.add(device.getName() + '\n' + device.getAddress());
                    updateBluetoothDevicesListView();
                }

            }
        }
    };
}