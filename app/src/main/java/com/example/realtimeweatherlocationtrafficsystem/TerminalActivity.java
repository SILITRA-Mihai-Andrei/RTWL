package com.example.realtimeweatherlocationtrafficsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.BluetoothClientClass;
import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.FireBaseManager;
import com.example.realtimeweatherlocationtrafficsystem.models.Region;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsFireBase;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class TerminalActivity extends AppCompatActivity implements Serializable, FireBaseManager.onFireBaseDataNew {

    private FireBaseManager fireBaseManager;
    private boolean development;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private String lastUnfinishedMessage = "";

    private LinearLayout loading;
    private TextView loading_message;
    private LinearLayout dataBaseLinearLayout;
    private TextView connectedDeviceTextView;
    private TextView statusTextView;
    private TextView receiveBox;
    private TextView commands;
    private TextView dataBase;
    private EditText coordinates;
    private EditText code;
    private EditText temperature;
    private EditText humidity;
    private EditText air;
    private EditText messageToSend;
    private Button clearTerminal;
    private Button sendTestData;
    private Button send;
    private Button showDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        initComponents();
    }

    @Override
    protected void onPause() {
        super.onPause();
        goToMainActivity(new View(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        goToMainActivity(new View(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!development) {
            unregisterReceiver(receiverState);
            unregisterReceiver(receiverConnection);
        }
    }

    private void initComponents() {
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);
        development = (boolean) getIntent().getSerializableExtra("DEVELOPMENT_SESSION_ID");
        findViews(); //find views

        if (!development) {
            loading_message.setText(R.string.connection_to_bluetooth_device);
            registerReceiver(receiverState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            registerReceiver(receiverConnection, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
            device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("BT_DEVICE_SESSION_ID");
            try {
                assert device != null;
                socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
                }catch (IOException e1){e1.printStackTrace();}
            }
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothClientClass clientClass = new BluetoothClientClass(socket, bluetoothAdapter, handler, send, messageToSend);
            clientClass.start();
        }
        setDevelopmentViews();
        setListeners();
    }

    private void setDevelopmentViews() {
        LinearLayout testContainer = findViewById(R.id.test_container); //when bluetooth device is NOT available
        LinearLayout terminalContainer = findViewById(R.id.terminal_container); //when bluetooth device is available
        if (development) {
            loading.setVisibility(View.GONE);
            connectedDeviceTextView.setText(R.string.development_mode);
            testContainer.setVisibility(View.VISIBLE);
            terminalContainer.setVisibility(View.GONE);
            statusTextView.setText(R.string.first_status_in_development_mode);
        } else {
            String connectedTo = String.format(getResources().getString(R.string.connecting_to_placeholder_device), device.getName());
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
            clearTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    receiveBox.setText("");
                }
            });
        }
        showDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataBaseLinearLayout.getVisibility() == View.GONE) {
                    dataBaseLinearLayout.setVisibility(View.VISIBLE);
                    showDataBase.setText(R.string.hide_data_base);
                } else {
                    dataBaseLinearLayout.setVisibility(View.GONE);
                    showDataBase.setText(R.string.show_data_base);
                }
            }
        });

        commands.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TerminalActivity.this);
                builder.setMessage(UtilsBluetooth.BLUETOOTH_COMMANDS_LIST).setCancelable(true);
                builder.setTitle(R.string.bluetooth_commands_title);
                builder.show();
            }
        });
    }

    private void findViews() {
        loading = findViewById(R.id.loading);
        loading_message = findViewById(R.id.loading_message);
        dataBaseLinearLayout = findViewById(R.id.data_base_linear_layout);
        connectedDeviceTextView = findViewById(R.id.connected_device);
        statusTextView = findViewById(R.id.status);
        coordinates = findViewById(R.id.coordinates);
        coordinates.setText(R.string.coordinates_hint);
        code = findViewById(R.id.code);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        air = findViewById(R.id.air);
        receiveBox = findViewById(R.id.receive_box);
        commands = findViewById(R.id.commands);
        dataBase = findViewById(R.id.dataBase);
        clearTerminal = findViewById(R.id.clear_terminal);
        sendTestData = findViewById(R.id.send_test_data);
        messageToSend = findViewById(R.id.message_to_send);
        send = findViewById(R.id.send);
        showDataBase = findViewById(R.id.show_data_base);
    }

    Handler handler = new android.os.Handler(new android.os.Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UtilsBluetooth.STATE_CONNECTING:
                    loading_message.setText(R.string.connection_to_bluetooth_device);
                    if (loading.getVisibility() == View.GONE) {
                        loading.setVisibility(View.VISIBLE);
                    }
                    connectedDeviceTextView.setText(String.format(getString(R.string.connecting_to_placeholder_device), device.getName()));
                    break;
                case UtilsBluetooth.STATE_CONNECTED:
                    connectedDeviceTextView.setText(String.format(getString(R.string.connected_to_placeholder_device), device.getName()));
                    setStatus(String.format(getString(R.string.connected_to_placeholder_device), device.getName()), R.color.color_green);
                    loading.setVisibility(View.GONE);
                    break;
                case UtilsBluetooth.STATE_CONNECTION_FAILED:
                    Toast.makeText(TerminalActivity.this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
                    goToMainActivity(new View(getBaseContext()));
                    break;
                case UtilsBluetooth.STATE_READING_WRITING_FAILED:
                    Toast.makeText(TerminalActivity.this,
                            String.format(getString(R.string.disconected_from_placeholder_device), device.getName()), Toast.LENGTH_SHORT).show();
                    if(socket.isConnected()) goToMainActivity(new View(getBaseContext()));
                    else {
                        finish();
                        startActivity(new Intent(TerminalActivity.this, MainActivity.class));
                    }
                    break;
                case UtilsBluetooth.STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    if(readBuffer==null) break;
                    String message = new String(readBuffer, 0, msg.arg1);
                    if(message.isEmpty()) break;
                    if(isFinalMessage(message)){
                        String[] response = UtilsBluetooth.getReceivedMessage(receiveBox.getText().toString(), lastUnfinishedMessage+message, getBaseContext());
                        Toast.makeText(TerminalActivity.this, response[0], Toast.LENGTH_SHORT).show();
                        if(!(response[0]==null || response[0].isEmpty())) receiveBox.setText(response[0]);
                        lastUnfinishedMessage = "";
                    }
                    setStatus(getString(R.string.received_message), R.color.color_green);
                    break;
                case UtilsBluetooth.STATE_MESSAGE_SEND:
                    setStatus(TerminalActivity.this.getResources().getString(R.string.message_send), R.color.blue);
                    break;
            }
            return true;
        }
    });

    private final BroadcastReceiver receiverState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    goToMainActivity(new View(getBaseContext()));
                }
            }
        }
    };

    private final BroadcastReceiver receiverConnection = new BroadcastReceiver() {
        @SuppressLint("ShowToast")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) || action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Toast toast = new Toast(getBaseContext());
                try{
                    toast.getView().isShown();    // true if visible
                } catch (Exception e) {         // invisible if exception
                    toast = Toast.makeText(context,
                            String.format(getString(R.string.disconected_from_placeholder_device),
                                    device.getName()), Toast.LENGTH_SHORT);
                }
                toast.show();  //finally display it
                goToMainActivity(new View(getBaseContext()));
            }
        }
    };

    private boolean isFinalMessage(String string){
        if(string.contains(UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER)){
            return true;
        }
        lastUnfinishedMessage = string;
        return false;
    }

    @Override
    public void onBackPressed() {
        if(development){
            super.onBackPressed();
        }
        else if (dataBaseLinearLayout.getVisibility() == View.VISIBLE) {
            dataBaseLinearLayout.setVisibility(View.GONE);
            showDataBase.setText(R.string.show_data_base);
        } else {
            goToMainActivity(new View(this));
        }
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        dataBase.setText(UtilsFireBase.regionListToString(regions));
    }

    private void setStatus(String status, int color) {
        statusTextView.setText(status);
        //Utils.blinkTextView(statusTextView, statusTextView.getCurrentTextColor(), Utils.getColorARGB(color), 150, 8);
    }

    public void goToMainActivity(View view) {
        try {
            if (socket!=null){
                socket.close();
                loading_message.setText(R.string.disconnection_to_bluetooth_device);
                loading.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}