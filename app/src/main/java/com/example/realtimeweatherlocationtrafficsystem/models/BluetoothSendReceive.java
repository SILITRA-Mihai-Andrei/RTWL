package com.example.realtimeweatherlocationtrafficsystem.models;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Thread that will take care of messages received from Bluetooth device and messages that must be sent to it.
 * It will send state messages and received messages from Bluetooth device through the handler.
 */
public class BluetoothSendReceive extends Thread {

    // Defines how many wrong messages can be received from Bluetooth device before notify the problem
    public static final int MAX_FAILED_RECEIVED_MESSAGE = 15;

    // The handler will send messages to other activities
    private Handler handler;
    // Stream that will be used to receive the Bluetooth device message
    private final InputStream inputStream;
    // Stream that will be used to send messages to Bluetooth device
    private final OutputStream outputStream;
    // The socket that will control the Bluetooth connection and send/receive messages
    private BluetoothSocket socket;

    // Count how many times was received an invalid message from Bluetooth device
    private int failed_received_message = 0;

    public BluetoothSendReceive(BluetoothSocket socket, Handler handler, final Button send, final EditText message) {
        this.socket = socket;
        this.handler = handler;
        InputStream tmpInputStream = null;
        OutputStream tmpOutputStream = null;

        // Try to get the Bluetooth streams, where the messages will be received or written to be sent
        try {
            tmpInputStream = socket.getInputStream();
            tmpOutputStream = socket.getOutputStream();
        } catch (IOException e) {
            // Could not get the Bluetooth streams
            e.printStackTrace();
            // Send state message through handler
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
        }
        inputStream = tmpInputStream;
        outputStream = tmpOutputStream;
        // Set listeners for button
        setListeners(send, message);
    }

    /**
     * Handle the received messages from Bluetooth device.
     * Control the Bluetooth connection.
     */
    public void run() {
        // Check if the input stream exists
        if (inputStream == null) {
            // Send state message through handler
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
            // Stop the thread, can't continue without input stream
            interrupt();
            return;
        }

        // Create the buffer where the message will be written
        byte[] buffer = new byte[UtilsBluetooth.BLUETOOTH_BUFFER_SIZE];
        // Count the number of received bytes
        int bytes;

        // Check if the Bluetooth is connected to a Bluetooth device
        if (socket.isConnected()) {
            try {
                // Clear the Bluetooth buffer
                // Start clean reading received messages from Bluetooth device
                //noinspection ResultOfMethodCallIgnored
                inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Fill the buffer with 0 values
        // Avoid random values to be Bluetooth messages
        Arrays.fill(buffer, (byte) 0);
        // Start receiving Bluetooth messages from Bluetooth device
        while (true) try {
            // Check if the Bluetooth is connected to Bluetooth device
            if (!socket.isConnected()) {
                // Send state message through handler
                sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
                // Stop the thread, can't continue without Bluetooth device connected
                interrupt();
                return;
            }

            // Check the number of received bytes in stream
            int stream_size = inputStream.available();
            // Check if there is at least one byte received in stream from Bluetooth device
            if (stream_size > 0) {
                // Fill the buffer with 0 values - clear it for new reading
                Arrays.fill(buffer, (byte) 0);
                // Read the received message and store the number of bytes received
                bytes = inputStream.read(buffer);
                // Check the number of bytes received
                if (bytes == -1) {
                    // The reading failed
                    // Send state message through handler
                    sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
                    return;
                }
                // Check if the received message contains the Bluetooth message delimiter
                if (Utils.containsByte(buffer, (byte) UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER.charAt(0))) {
                    // Check if the message received length is valid
                    if (stream_size <= UtilsBluetooth.BLUETOOTH_ONE_RECORD_SIZE) {
                        // Send the received message trough the handler
                        handler.obtainMessage(UtilsBluetooth.STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                        failed_received_message = 0;
                    }
                } else {
                    // Wrong/invalid message received
                    // Increment the failure and check if the number if failures touched the limit
                    if (++failed_received_message >= MAX_FAILED_RECEIVED_MESSAGE) {
                        // Send a message trough handler notifying the problem with receiving invalid messages
                        handler.obtainMessage(UtilsBluetooth.STATE_FAILED_RECEIVING_MESSAGE_LIMIT, bytes, -1, buffer).sendToTarget();
                    }
                }
            } else {
                // Wait some time before the next reading
                // Weather data doesn't change too fast
                SystemClock.sleep(1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a message to Bluetooth device.
     *
     * @param bytes is the message to send casted to bytes.
     */
    public void write(byte[] bytes) {
        // Check if there is at least one byte to send
        if (bytes.length == 0) return;
        // Try to write the message to the stream
        // The message will be send after that to the Bluetooth device
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            // The message could not be written to the stream
            e.printStackTrace();
            // Send state message trough handler
            sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
        }
    }

    /**
     * Send message through the handler.
     *
     * @param msg is the message that will be send through the handler.
     */
    private void sendHandlerMessage(int msg) {
        // Get the message object
        Message message = Message.obtain();
        message.what = msg;
        // Send the message trough the handler
        handler.sendMessage(message);
    }

    /**
     * Set listener for the button that will notify when to send a message to Bluetooth device.
     * If the text area doesn't exists, the button name will be sent instead.
     *
     * @param send    is the button that will notify the listener when is clicked.
     * @param message is the text area where the message to send is written.
     */
    private void setListeners(final Button send, final EditText message) {
        // Check if the button exists
        if (send == null) return;
        // Create listener for click on the button that will notify when to send a message
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the message that will be received
                String msg;
                // Check if the text area exists
                if (message == null) {
                    // Use the send button name instead
                    // This is a trick that should not be here...
                    msg = send.getText().toString();
                } else {
                    // Get the message from the text area
                    msg = message.getText().toString();
                    // Clear the message area
                    message.setText("");
                }
                // Send the message to Bluetooth device
                write(msg.getBytes());
                // Send state message trough handler
                sendHandlerMessage(UtilsBluetooth.STATE_MESSAGE_SEND);
            }
        });
    }
}
