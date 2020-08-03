package com.example.realtimeweatherlocationtrafficsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class TerminalActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private DatabaseReference dataReference;
    private TextView connectedDeviceTextView;
    private TextView statusTextView;
    private EditText coordinates;
    private EditText code;
    private EditText temperature;
    private EditText humidity;
    private EditText air;
    private Button sendTestData;
    private TextView receiveBox;
    private EditText messageToSend;
    private Button send;

    private boolean development;
    private String device;
    private BluetoothAdapter mBluetoothAdapter;
    private android.os.Handler bluetoothHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        initComponents();
    }

    private void initComponents() {
        mDatabase = FirebaseDatabase.getInstance();
        dataReference = mDatabase.getReference().child("data");
        device = (String) getIntent().getSerializableExtra("BT_DEVICE_SESSION_ID");
        development = (boolean) getIntent().getSerializableExtra("DEVELOPMENT_SESSION_ID");
        bluetoothHandler = new android.os.Handler();
        bluetoothHandler.postDelayed(updateTimerThread, 0);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        findViews(); //find views
        setDevelopmentViews();
        setListeners();
    }

    private void setDevelopmentViews() {
        LinearLayout testContainer = findViewById(R.id.test_container); //when bluetooth device is NOT available
        LinearLayout terminalContainer = findViewById(R.id.terminal_container); //when bluetooth device is available
        if (development) {
            connectedDeviceTextView.setText(R.string.development_mode);
            testContainer.setVisibility(View.VISIBLE);
            terminalContainer.setVisibility(View.GONE);
            statusTextView.setText(R.string.first_status_in_development_mode);
        } else {
            String connectedTo = String.format(getResources().getString(R.string.connected_to_placeholder_device), device.split("\n")[0]);
            connectedDeviceTextView.setText(connectedTo);
            testContainer.setVisibility(View.GONE);
            terminalContainer.setVisibility(View.VISIBLE);
            statusTextView.setText(connectedTo);
        }
    }

    private void setListeners() {
        if (development) {
            sendTestData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dataReference.child("47 64 26 24").child(Utils.getCurrentDateAndTime()).setValue(new Data(100, 23, 50, 70));
                    dataReference.child("47 63 26 24").child(Utils.getCurrentDateAndTime()).setValue(new Data(200, 33, 50, 70));
                    if (coordinates == null || code == null || temperature == null || humidity == null || air == null) {
                        setStatus(getResources().getString(R.string.complete_all_fields));
                        return;
                    }
                    int result = Utils.isDataValid(coordinates.getText().toString(),
                            code.getText().toString(), temperature.getText().toString(),
                            humidity.getText().toString(), air.getText().toString());
                    setStatus(Utils.getInvalidMessage(result, getBaseContext()));
                }
            });
        } else {
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //something to send to bluetooth device
                }
            });
        }
        dataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Data data = new Data(Objects.requireNonNull(ds.getValue(Data.class)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TerminalActivity.this,
                        String.format(getResources().getString(R.string.data_base_error), error.toString()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findViews() {
        connectedDeviceTextView = findViewById(R.id.connected_device);
        statusTextView = findViewById(R.id.status);
        coordinates = findViewById(R.id.coordinates);
        coordinates.setText(R.string.coordinates_hint);
        code = findViewById(R.id.code);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        air = findViewById(R.id.air);
        sendTestData = findViewById(R.id.send_test_data);
        receiveBox = findViewById(R.id.receive_box);
        messageToSend = findViewById(R.id.message_to_send);
        send = findViewById(R.id.send);
    }

    private void checkConnection() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.check_bluetooth_connection, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            bluetoothHandler.postDelayed(this, 2000);
            checkConnection();
        }
    };

    private void setStatus(String status) {
        statusTextView.setText(status);
        Utils.blinkTextView(statusTextView, statusTextView.getCurrentTextColor(), 100, 10);
    }

    public void goToMainActivity(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }
}