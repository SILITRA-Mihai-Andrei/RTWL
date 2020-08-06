package com.example.realtimeweatherlocationtrafficsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsFireBase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class TerminalActivity extends AppCompatActivity implements Serializable, FireBaseManager.onFireBaseDataNew {

    private FireBaseManager fireBaseManager;
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
    private TextView dataBase;

    private boolean development;
    private BluetoothDevice device;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private int counter;
    volatile boolean stopWorker;
    private android.os.Handler bluetoothHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        initComponents();
    }

    private void initComponents() {
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);
        development = (boolean) getIntent().getSerializableExtra("DEVELOPMENT_SESSION_ID");
        if (!development) {
            device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("BT_DEVICE_SESSION_ID");
            bluetoothHandler = new android.os.Handler();
            bluetoothHandler.postDelayed(updateTimerThread, 0);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        findViews(); //find views
        setDevelopmentViews();
        setListeners();
        connectToBluetooth();
    }

    private void connectBluetooth() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mSocket = device.createRfcommSocketToServiceRecord(uuid);
        mSocket.connect();
        mOutputStream = mSocket.getOutputStream();
        mInputStream = mSocket.getInputStream();

        beginListenForData();
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
            String connectedTo = String.format(getResources().getString(R.string.connected_to_placeholder_device), device.getName());
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
                    if (!Utils.areFieldsCompleted(coordinates, code, temperature, humidity, air)) {
                        setStatus(getResources().getString(R.string.complete_all_fields), Utils.COLOR_BLUE);
                        return;
                    }
                    int result = Utils.isDataValid(coordinates.getText().toString(),
                            code.getText().toString(), temperature.getText().toString(),
                            humidity.getText().toString(), air.getText().toString());
                    if (result == Utils.VALID) {
                        setStatus(Utils.getInvalidMessage(result, getBaseContext()), Utils.COLOR_GREEN);
                        fireBaseManager.setValue(Utils.getCoordinatesForDataBase(coordinates.getText().toString()), Utils.getCurrentDateAndTime(),
                                new Data(Utils.getInt(code.getText().toString()),
                                        Utils.getInt(temperature.getText().toString()),
                                        Utils.getInt(humidity.getText().toString()),
                                        Utils.getInt(air.getText().toString())));
                    } else
                        setStatus(Utils.getInvalidMessage(result, getBaseContext()), Utils.COLOR_RED);
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
        dataBase = findViewById(R.id.dataBase);
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

    private void connectToBluetooth() {
        if (!development) {
            try {
                connectBluetooth();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, StandardCharsets.UTF_8);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            receiveBox.setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        dataBase.setText(UtilsFireBase.regionListToString(regions));
    }

    void sendData() throws IOException {
        mOutputStream.write((messageToSend.getText().toString() + "\n").getBytes());
    }

    private void setStatus(String status, int color) {
        statusTextView.setText(status);
        Utils.blinkTextView(statusTextView, statusTextView.getCurrentTextColor(), Utils.getColorARGB(color), 100, 10);
    }

    public void goToMainActivity(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

}