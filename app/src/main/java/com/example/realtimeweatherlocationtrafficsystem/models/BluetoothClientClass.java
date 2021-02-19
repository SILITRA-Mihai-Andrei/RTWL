package com.example.realtimeweatherlocationtrafficsystem.models;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

/**
 * The class will take care of Bluetooth connection.
 * It will use a Bluetooth socket to connect to a Bluetooth device.
 * Will create a BluetoothSendReceive object that will control the read/write communication.
 * The messages received/sent will be transmitted using a handler.
 */
public class BluetoothClientClass extends Thread {

    // Define the thread that will take care of read/write communication
    BluetoothSendReceive sendReceive;
    // The Bluetooth socket that will realize and control the Bluetooth connection
    private BluetoothSocket socket;
    // The Bluetooth adapter that will receive the the Bluetooth devices and control the discovery service
    private BluetoothAdapter adapter;
    // The handler which will be used to send/receive messages from thread
    private Handler handler;
    // The button that will notify when the thread must send a message
    private Button send;
    // The UI component where the message to send will be written
    private EditText message;

    /**
     * Constructor
     * Initialize the thread.
     * <p>
     * This constructor is specially created for TerminalActivity.
     * Considering @param send and @param message null, it can be used for other purposes.
     *
     * @param socket  is the Bluetooth socket that will take care of Bluetooth connection.
     * @param adapter is the Bluetooth adapter that will receive all paired Bluetooth devices and will control the Bluetooth device discovery.
     * @param handler is the Handler object that will be used to send and receive messages between threads and activity.
     * @param send    is the button that will notify the thread when to send a message (onClickListener).
     * @param message is the text area where the message is written.
     */
    public BluetoothClientClass(BluetoothSocket socket, BluetoothAdapter adapter, Handler handler, Button send, EditText message) {
        this.adapter = adapter;
        this.handler = handler;
        this.send = send;
        this.message = message;
        this.socket = socket;
    }

    /**
     * The starting function of this thread.
     * It will connect the Bluetooth to a Bluetooth device.
     * Send Bluetooth connection states through handler.
     */
    public void run() {
        try {
            // Cancel the Bluetooth device discovery
            // There should be already a Bluetooth device selected
            adapter.cancelDiscovery();
            // Start connecting to Bluetooth device
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTING);
            // Check if the Bluetooth socket exists
            if (socket == null) return;
            // Check if the socket is already connected to a Bluetooth device
            if (!socket.isConnected()) {
                // Connect the Bluetooth socket
                socket.connect();
            }
            // Check again if the Bluetooth socket is connected
            if (socket.isConnected()) {
                // Send the successful connection message to handler
                sendHandlerMessage(UtilsBluetooth.STATE_CONNECTED);
            } else {
                // Send the failed connection message to handler
                sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
                return;
            }
            // Create the thread that will handle the send/receive messages to/from Bluetooth device
            sendReceive = new BluetoothSendReceive(socket, handler, send, message);
            // Start the thread
            sendReceive.start();
        } catch (IOException e) {
            // Connection to Bluetooth device was unsuccessful
            e.printStackTrace();
            // Send the failed connection message to handler
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
        }
    }

    /**
     * Send a message to the handler received from constructor.
     *
     * @param msg is the message id that will be sent.
     */
    private void sendHandlerMessage(int msg) {
        // Create the message object
        Message message = Message.obtain();
        message.what = msg;
        // Send the message through the handler
        handler.sendMessage(message);
    }

    /**
     * GETTER
     * Get the thread that handle receive/send messages from Bluetooth device.
     */
    public BluetoothSendReceive getBluetoothSendReceive() {
        return sendReceive;
    }

    /**
     * GETTER
     * Get the Bluetooth socket that handle the Bluetooth connection with the Bluetooth device.
     */
    public BluetoothSocket getSocket() {
        return socket;
    }
}
