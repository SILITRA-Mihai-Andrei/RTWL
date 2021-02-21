package com.example.realtimeweatherlocationtrafficsystem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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

/**
 * This activity will be used in two modes:
 * -- Development mode: where the user can write custom values and send to database.
 * -- Terminal/User mode: where the user can see what data was received from Bluetooth service
 * connected to the Bluetooth device (selected in the MainActivity), send commands to Bluetooth
 * device, see all commands available to send.
 * In the both modes can be seen the database content.
 * <p>
 * The activity receives the mode through the intent starter.
 * Also, from intent starter will be extracted the selected Bluetooth device (for using its name in texts).
 */
public class TerminalActivity extends AppCompatActivity implements Serializable {

    // Define the mode
    private boolean development;        // true means development mode and false means terminal/user mode
    private BluetoothDevice device;     // the selected device received from MainActivity through intent

    // UI components for terminal/user mode
    private LinearLayout dataBaseLinearLayout;  // container where database content will be shown
    private LinearLayout messageContainer;      // container where the send components will be shown
    private TextView connectedDeviceTextView;   // top text indicating the Bluetooth connection (ex: "Connected to xxx")
    private TextView statusTextView;            // text view where last event is shown (ex: "Message received")
    private TextView receiveBox;                // here is where the received messages from Bluetooth devices are displayed
    private TextView commands;                  // shows all available commands for Bluetooth device
    private TextView dataBase;                  // all database content will be written here
    private EditText messageToSend;             // here the user will write the commands
    private Button clearTerminal;               // the button that will erase the receiveBox
    private Button expandTerminal;              // the button that will expand the receiveBox and hide the components below (except disconnect button)
    private Button send;                        // the button that will send the command to Bluetooth module through Bluetooth service
    private Button showDataBase;                // the button that will change dataBaseLinearLayout visibility

    // UI components for developing mode
    private Button sendTestData;                // the button that will send the custom data written by user to database
    private EditText coordinates;               // the text area where the coordinates of the region will be written
    private EditText code;                      // the weather code
    private EditText temperature;               // the weather temperature
    private EditText humidity;                  // the weather humidity
    private EditText air;                       // the air quality / pollution

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        // Initialize all UI components
        initComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = true;
        // This registers messageReceiver to receive messages from broadcasts
        // Create a filter that will filtrate the messages from broadcasts
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.SERVICE_KEY);
        intentFilter.addAction(MainActivity.SERVICE_KEY);
        // Register the receiver that will receive the broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        // Check the mode
        if (!development) {
            // Check if the Bluetooth service is active
            if (BluetoothService.SERVICE_ACTIVE) {
                // Create the String that will display the Bluetooth device connected
                String string = String.format(getString(R.string.connected_to_placeholder_device), device.getName());
                connectedDeviceTextView.setText(string);
                statusTextView.setText(string);
            } else {
                // In terminal/user mode the Bluetooth service must be active
                goToMainActivity();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the receiver since the activity is inactive
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Try to unregister the broadcasts receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        // Go to main activity
        // When the user comes back to app, the MainActivity will check all changes made
        goToMainActivity();
    }

    @Override
    protected void onDestroy() {
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = false;
        super.onDestroy();
    }

    // Handling the received Intents from BluetoothService Service
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the message id from broadcast
            int messageID = intent.getIntExtra(MainActivity.SERVICE_MESSAGE_ID_KEY, -1);
            // Check if the message id is valid
            if (messageID != -1) {
                // Check if id indicates that Firebase service has new list of regions from database
                if (messageID == MainActivity.SERVICE_MESSAGE_ID_REGIONS) {
                    // Update the database text view with the new regions list
                    dataBase.setText(UtilsFireBase.regionListToString(FireBaseService.regions));
                    return;
                }
            }

            // Extract data included in the Intent coming from Bluetooth service
            messageID = intent.getIntExtra(BluetoothService.SERVICE_MESSAGE_ID_KEY, -1);
            String message = intent.getStringExtra(BluetoothService.SERVICE_MESSAGE_KEY);

            // Check if the message id is valid
            if (messageID == -1) return;
            switch (messageID) {
                // The Bluetooth was turned OFF
                case BluetoothService.SERVICE_MESSAGE_ID_BT_OFF:
                    Toast.makeText(context, getString(R.string.bluetooth_is_off), Toast.LENGTH_SHORT).show();
                    // Go back to MainActivity
                    goToMainActivity();
                    break;
                // The Bluetooth device lost connection or disconnected
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTION_FAILED:
                case BluetoothService.SERVICE_MESSAGE_ID_DISCONNECTED:
                case BluetoothService.SERVICE_MESSAGE_ID_RW_FAILED:
                    // Go back to MainActivity
                    goToMainActivity();
                    break;
                // Bluetooth service is trying to connect to Bluetooth device
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTING:
                    connectedDeviceTextView.setText(message);
                    break;
                // Bluetooth service successfully connected to Bluetooth device
                case BluetoothService.SERVICE_MESSAGE_ID_CONNECTED:
                    connectedDeviceTextView.setText(message);
                    statusTextView.setText(message);
                    break;
                // Bluetooth service received a message from Bluetooth device and sent it here through broadcast
                case BluetoothService.SERVICE_MESSAGE_ID_RECEIVED:
                    // Check if the message exists and is not empty
                    if (message == null) break;
                    if (message.isEmpty()) break;

                    // Check if the message contains the time
                    // All Bluetooth service messages received must contain the current time when the message was received
                    if (message.split(UtilsBluetooth.MESSAGE_TIME_END)[1].length() <= 3)
                        break;
                    // Split the received message by message content separator
                    // Message that contains weather data are separated in two
                    // The first part contains the translated sensors values (ex: sun, rain, wind)
                    // The second part contains the message received from Bluetooth device (unmodified)
                    String[] splited = message.split("@");
                    // Check if the receive box text length plus the new message received length are higher than the limit
                    if (receiveBox.getText().length() + splited[0].length() > Utils.MAX_RECEIVE_BOX_LENGTH) {
                        // Clear all receive box text
                        receiveBox.setText("");
                    }
                    // Set the first part of received message to the end of current receive box text
                    receiveBox.append(splited[0] + "\n");
                    // Update the status
                    setStatus(getString(R.string.received_message));
                    break;
            }
        }
    };

    /**
     * Get the objects from intent starter.
     * Find all UI components.
     * Set the UI components according to the received mode.
     * Set necessary listeners.
     */
    private void initComponents() {
        // Get the mode from intent starter
        development = (boolean) getIntent().getSerializableExtra(MainActivity.DEVELOPMENT_SESSION_ID);
        // Find UI components
        findViews();

        // Check if is terminal/user mode
        if (!development) {
            // Try to get the Bluetooth device from intent starter
            device = Objects.requireNonNull(getIntent().getExtras()).getParcelable(MainActivity.BT_DEVICE_SESSION_ID);
        }
        // Set the UI components according to the received mode
        setDevelopmentViews();
        // Set necessary listeners
        setListeners();
    }

    /**
     * Set the UI components according to the received mode.
     * Make the activity ready to communicate with Bluetooth device (terminal/user mode) or
     * ready to send custom data to database.
     */
    private void setDevelopmentViews() {
        // Find the main containers for both modes
        LinearLayout testContainer = findViewById(R.id.test_container);             // development mode
        LinearLayout terminalContainer = findViewById(R.id.terminal_container);     // terminal/user mode
        // Check if is development mode
        if (development) {
            // Set views necessary to send custom data to database
            connectedDeviceTextView.setText(R.string.development_mode);
            testContainer.setVisibility(View.VISIBLE);
            terminalContainer.setVisibility(View.GONE);
            statusTextView.setText(R.string.first_status_in_development_mode);
        } else {
            // Set views necessary to communicate with Bluetooth device.
            String connectedTo = String.format(getResources().getString(R.string.connecting_to_placeholder_device), device.getName());
            connectedDeviceTextView.setText(connectedTo);
            testContainer.setVisibility(View.GONE);
            terminalContainer.setVisibility(View.VISIBLE);
            statusTextView.setText(connectedTo);
        }
    }

    /**
     * Set listener for view.
     */
    private void setListeners() {
        // Check if is developments mode
        if (development) {
            // Set listener for click on button that send the custom data to database
            sendTestData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check if all mandatory fields are filled
                    if (!Utils.areFieldsCompleted(coordinates, code, temperature, humidity, air)) {
                        statusTextView.setText(getResources().getString(R.string.complete_all_fields));
                        return;
                    }
                    // Check if all fields have valid values
                    int result = Utils.isDataValid(
                            coordinates.getText().toString(),
                            code.getText().toString(),
                            temperature.getText().toString(),
                            humidity.getText().toString(),
                            air.getText().toString());
                    // Check validity of the result
                    if (result == Utils.VALID) {
                        // Get the message corresponding to the result
                        statusTextView.setText(Utils.getValidityMessage(result, getBaseContext()));
                        // Create a new Data object with the values written by user
                        Data data = new Data(
                                Utils.getInt(code.getText().toString()),
                                Utils.getInt(temperature.getText().toString()),
                                Utils.getInt(humidity.getText().toString()),
                                Utils.getInt(air.getText().toString()));
                        // Create a new intent that will send a message to Firebase service with the data that must be sent
                        Intent intent = new Intent(FireBaseService.SERVICE_KEY);
                        // Adding some data to intent using the key and value
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_ID_KEY, FireBaseService.SERVICE_MESSAGE_ID_SET_VALUE);
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_COORDINATES_KEY, Utils.getCoordinatesWithDecimals(coordinates.getText().toString(), 2));
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_TIME_KEY, Utils.getCurrentDateAndTime());
                        intent.putExtra(FireBaseService.SERVICE_MESSAGE_DATA_KEY, data);
                        // Send the intent message as broadcast
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    } else {
                        // Some fields are not valid
                        statusTextView.setText(Utils.getValidityMessage(result, getBaseContext()));
                    }
                }
            });
            // For terminal/user mode
        } else {
            // Set listener for click on the button that will clear the receive box
            clearTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Clear the text from receive box
                    receiveBox.setText("");
                }
            });
            // Set listener for click on the button that will expand the receive box
            expandTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int visibility = View.GONE;
                    // Check if the receive box is already expanded
                    // When the container is hidden, the receive box has more space to expand
                    if (messageContainer.getVisibility() == View.GONE) {
                        // The receive box is expanded
                        visibility = View.VISIBLE;
                    }
                    // Set the visibility to views
                    messageContainer.setVisibility(visibility);
                    messageToSend.setVisibility(visibility);
                    send.setVisibility(visibility);
                    showDataBase.setVisibility(visibility);
                }
            });
        }
        // Set listener for click on the button that will show the database content
        showDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the database text container is already shown
                if (dataBaseLinearLayout.getVisibility() == View.GONE) {
                    // Get the regions list from Firebase service and convert them to String
                    dataBase.setText(UtilsFireBase.regionListToString(FireBaseService.regions));
                    dataBaseLinearLayout.setVisibility(View.VISIBLE);
                    showDataBase.setText(R.string.hide_data_base);
                } else {
                    dataBaseLinearLayout.setVisibility(View.GONE);
                    showDataBase.setText(R.string.show_data_base);
                }
            }
        });

        // Set listener for click on the button that will display all available commands for Bluetooth device
        commands.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new alert dialog in this activity
                AlertDialog.Builder builder = new AlertDialog.Builder(TerminalActivity.this);
                // Write the available commands
                builder.setMessage(UtilsBluetooth.BLUETOOTH_COMMANDS_LIST).setCancelable(true);
                // Set the alert dialog title
                builder.setTitle(R.string.bluetooth_commands_title);
                // Show the alert dialog
                builder.show();
            }
        });

        // Set listener for click on the button that will send the command to Bluetooth device
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If there is no message to send, don't send, right?
                if (messageToSend == null) return;
                // Convert the message to String
                String command = messageToSend.getText().toString();
                // Write the command to Bluetooth device using the Bluetooth socket
                // TODO: There must be removed the memory leak created by this static method
                BluetoothService.getReply(command);
                // Clear the message box
                messageToSend.setText("");
                // Update the status
                statusTextView.setText(R.string.message_sent);
            }
        });
    }

    /**
     * Find all UI components.
     */
    private void findViews() {
        dataBaseLinearLayout = findViewById(R.id.data_base_linear_layout);
        messageContainer = findViewById(R.id.message_to_send_label);
        connectedDeviceTextView = findViewById(R.id.connected_device);
        statusTextView = findViewById(R.id.status);
        coordinates = findViewById(R.id.coordinates);
        // Write a random GPS coordinate (usually the user wants to send test data)
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

    /**
     * Update the status text view with the message.
     *
     * @param message the message written in the status text view.
     */
    private void setStatus(String message) {
        statusTextView.setText(String.format(getString(R.string.message_with_time_placeholder), Utils.getTime(), message));
    }

    /**
     * Go to main activity safety.
     * Unregister the broadcasts receivers.
     *
     * @param view is used for calling the method from layout.
     */
    public void goToMainActivity(View view) {
        // Unregister the broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        // End the activity
        finish();
    }

    /**
     * Go to main activity. Call the extended method.
     */
    public void goToMainActivity() {
        goToMainActivity(new View(this));
    }

    @Override
    public void onBackPressed() {
        if (development) {
            super.onBackPressed();
        }
        // If the mode is terminal/user, check if the database is shown
        // This is usually used when the user is looking in the database content and wants to hide it
        // At the first back pressed will hide the database container
        // The database container will also be hidden using the OK button
        else if (dataBaseLinearLayout.getVisibility() == View.VISIBLE) {
            dataBaseLinearLayout.setVisibility(View.GONE);
            showDataBase.setText(R.string.show_data_base);
            return;
        }
        // Go to MainActivity safety
        goToMainActivity();
    }
}