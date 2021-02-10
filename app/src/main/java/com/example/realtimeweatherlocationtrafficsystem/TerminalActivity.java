package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realtimeweatherlocationtrafficsystem.models.Data;
import com.example.realtimeweatherlocationtrafficsystem.models.Utils;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsBluetooth;
import com.example.realtimeweatherlocationtrafficsystem.models.UtilsFireBase;
import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;
import com.example.realtimeweatherlocationtrafficsystem.services.FireBaseService;

import java.io.Serializable;
import java.util.Objects;

public class TerminalActivity extends AppCompatActivity implements Serializable {

    private boolean development;
    private BluetoothDevice device;

    private LinearLayout dataBaseLinearLayout;
    private LinearLayout messageToSendLabel;
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
    private Button expandTerminal;
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
    protected void onResume() {
        super.onResume();
        // This registers messageReceiver to receive messages.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.SERVICE_KEY);
        intentFilter.addAction(MainActivity.SERVICE_KEY);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        if (!development) {
            if (BluetoothService.SERVICE_ACTIVE) {
                String string = String.format(getString(R.string.connected_to_placeholder_device), device.getName());
                connectedDeviceTextView.setText(string);
                statusTextView.setText(string);
            } else {
                goToMainActivity();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        goToMainActivity();
    }

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            int messageID = intent.getIntExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, -1);
            if (messageID != -1) {
                if (messageID == MainActivity.SERVICE_MESSAGE_ID_REGIONS) {
                    dataBase.setText(UtilsFireBase.regionListToString(FireBaseService.regions));
                }
            }

            // Extract data included in the Intent
            messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);
            String message = intent.getStringExtra(BluetoothService.SERVICE_MESSAGE_KEY);

            if (messageID == -1) return;
            switch (messageID) {
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                    Toast.makeText(context, getString(R.string.bluetooth_is_off), Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                    goToMainActivity();
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTING:
                    connectedDeviceTextView.setText(message);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTED:
                    connectedDeviceTextView.setText(message);
                    statusTextView.setText(message);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_SENT:
                    setStatus(message);
                    break;
                case BluetoothService.SERVICE_MESSAGE_ID_RECEIVED:
                    if (message == null) break;
                    if (message.isEmpty()) break;

                    if (message.split(UtilsBluetooth.MESSAGE_TIME_END)[1].length() <= 3)
                        break;
                    String[] splited = message.split("@");
                    if (receiveBox.getText().length() + splited[0].length() > Utils.MAX_RECEIVE_BOX_LENGTH) {
                        receiveBox.setText("");
                    }
                    receiveBox.setText(receiveBox.getText() + splited[0] + "\n");
                    setStatus(getString(R.string.received_message));
                    break;
            }
        }
    };

    private void initComponents() {
        development = (boolean) getIntent().getSerializableExtra(MainActivity.DEVELOPMENT_SESSION_ID);
        findViews(); //find views

        if (!development) {
            device = Objects.requireNonNull(getIntent().getExtras()).getParcelable(MainActivity.BT_DEVICE_SESSION_ID);
            //BluetoothServiceIntent = Objects.requireNonNull(getIntent().getExtras()).getParcelable(MainActivity.BT_SERVICE_SESSION_ID);
        }
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
                        statusTextView.setText(getResources().getString(R.string.complete_all_fields));
                        return;
                    }
                    int result = Utils.isDataValid(coordinates.getText().toString(),
                            code.getText().toString(), temperature.getText().toString(),
                            humidity.getText().toString(), air.getText().toString());
                    if (result == Utils.VALID) {
                        statusTextView.setText(Utils.getInvalidMessage(result, getBaseContext()));
                        Data data = new Data(Utils.getInt(code.getText().toString()),
                                Utils.getInt(temperature.getText().toString()),
                                Utils.getInt(humidity.getText().toString()),
                                Utils.getInt(air.getText().toString()));
                        Intent intent = new Intent(FireBaseService.SERVICE_KEY);
                        // Adding some data
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE);
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY, Utils.getCoordinatesForDataBase(coordinates.getText().toString(), 2));
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY, Utils.getCurrentDateAndTime());
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY, data);
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    } else
                        statusTextView.setText(Utils.getInvalidMessage(result, getBaseContext()));
                }
            });
        } else {
            clearTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    receiveBox.setText("");
                }
            });
            expandTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int visibility = View.GONE;
                    if (messageToSendLabel.getVisibility() == View.GONE) {
                        visibility = View.VISIBLE;
                    }
                    messageToSendLabel.setVisibility(visibility);
                    messageToSend.setVisibility(visibility);
                    send.setVisibility(visibility);
                    showDataBase.setVisibility(visibility);
                }
            });
        }
        showDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataBaseLinearLayout.getVisibility() == View.GONE) {
                    dataBase.setText(UtilsFireBase.regionListToString(FireBaseService.regions));
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

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageToSend == null) return;
                String command = messageToSend.getText().toString();
                BluetoothService.getReply(command);
                messageToSend.setText("");
                statusTextView.setText(R.string.message_sent);
            }
        });
    }

    private void findViews() {
        dataBaseLinearLayout = findViewById(R.id.data_base_linear_layout);
        messageToSendLabel = findViewById(R.id.message_to_send_label);
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
        expandTerminal = findViewById(R.id.expand_terminal);
        sendTestData = findViewById(R.id.send_test_data);
        messageToSend = findViewById(R.id.message_to_send);
        send = findViewById(R.id.send);
        showDataBase = findViewById(R.id.show_data_base);
    }

    private void setStatus(String message) {
        statusTextView.setText(String.format(getString(R.string.message_with_time_placeholder), Utils.getTime(), message));
    }

    @Override
    public void onBackPressed() {
        if (development) {
            super.onBackPressed();
        } else if (dataBaseLinearLayout.getVisibility() == View.VISIBLE) {
            dataBaseLinearLayout.setVisibility(View.GONE);
            showDataBase.setText(R.string.show_data_base);
            return;
        }
        goToMainActivity();
    }

    public void goToMainActivity(View view) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        finish();
    }

    public void goToMainActivity() {
        goToMainActivity(new View(this));
    }

}