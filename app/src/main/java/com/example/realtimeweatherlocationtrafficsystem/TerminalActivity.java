package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class TerminalActivity extends AppCompatActivity implements Serializable, FireBaseManager.onFireBaseDataNew {

    private FireBaseManager fireBaseManager;
    private boolean development;
    private BluetoothDevice device;

    private LinearLayout dataBaseLinearLayout;
    private TextView connectedDeviceTextView;
    private TextView statusTextView;
    private TextView receiveBox;
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

    private void initComponents() {
        fireBaseManager = new FireBaseManager(getBaseContext(), getResources(), this);
        development = (boolean) getIntent().getSerializableExtra("DEVELOPMENT_SESSION_ID");
        findViews(); //find views

        if (!development) {
            device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("BT_DEVICE_SESSION_ID");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Handler handler = UtilsBluetooth.getBluetoothHandler(getBaseContext(), receiveBox, statusTextView);
            BluetoothClientClass clientClass = new BluetoothClientClass(device, bluetoothAdapter, handler, send, messageToSend);
            clientClass.start();
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
                if(dataBaseLinearLayout.getVisibility()==View.GONE) {
                    dataBaseLinearLayout.setVisibility(View.VISIBLE);
                    showDataBase.setText(R.string.hide_data_base);
                }
                else{
                    dataBaseLinearLayout.setVisibility(View.GONE);
                    showDataBase.setText(R.string.show_data_base);
                }
            }
        });
    }

    private void findViews() {
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
        dataBase = findViewById(R.id.dataBase);
        clearTerminal = findViewById(R.id.clear_terminal);
        sendTestData = findViewById(R.id.send_test_data);
        messageToSend = findViewById(R.id.message_to_send);
        send = findViewById(R.id.send);
        showDataBase = findViewById(R.id.show_data_base);
    }

    @Override
    public void onBackPressed() {
        if(dataBaseLinearLayout.getVisibility()==View.VISIBLE){
            dataBaseLinearLayout.setVisibility(View.GONE);
            showDataBase.setText(R.string.show_data_base);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public void onDataNewFireBase(List<Region> regions) {
        dataBase.setText(UtilsFireBase.regionListToString(regions));
    }

    private void setStatus(String status, int color) {
        statusTextView.setText(status);
        Utils.blinkTextView(statusTextView, statusTextView.getCurrentTextColor(), Utils.getColorARGB(color), 100, 10);
    }

    public void goToMainActivity(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

}