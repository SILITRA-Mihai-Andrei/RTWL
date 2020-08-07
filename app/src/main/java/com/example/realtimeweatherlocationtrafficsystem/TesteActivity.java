package com.example.realtimeweatherlocationtrafficsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.realtimeweatherlocationtrafficsystem.models.BluetoothClientClass;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import java.util.Objects;

public class TesteActivity extends AppCompatActivity {

    private TextView tv1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teste);
        tv1 = findViewById(R.id.tv1);
        Button btn1 = findViewById(R.id.btn1);

        /*device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("BT_DEVICE_SESSION_ID");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = UtilsBluetooth.getBluetoothHandler(getBaseContext(), tv1);
                BluetoothClientClass clientClass = new BluetoothClientClass(device, bluetoothAdapter, handler, null, null);
                clientClass.start();
                tv1.setText("Connecting2");*/
                /*try {
                    bluetoothSendReceive.write("test".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }*//*
            }
        });*/
    }
}