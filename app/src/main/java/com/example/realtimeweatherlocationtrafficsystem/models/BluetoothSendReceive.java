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


public class BluetoothSendReceive extends Thread {

    private Handler handler;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private BluetoothSocket socket;

    public BluetoothSendReceive(BluetoothSocket socket, Handler handler, final Button send, final EditText message) {
        this.socket = socket;
        this.handler = handler;
        InputStream tmpInputStream = null;
        OutputStream tmpOutputStream = null;

        try {
            tmpInputStream = socket.getInputStream();
            tmpOutputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
        }
        inputStream = tmpInputStream;
        outputStream = tmpOutputStream;
        if (send != null && message != null) {
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    write(message.getText().toString().getBytes());
                    message.setText("");
                    sendHandlerMessage(UtilsBluetooth.STATE_MESSAGE_SEND);
                }
            });
        } else if (send != null) {
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    write(send.getText().toString().getBytes());
                    sendHandlerMessage(UtilsBluetooth.STATE_MESSAGE_SEND);
                }
            });
        }
    }

    public void run() {
        if (inputStream == null) {
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
            return;
        }

        byte[] buffer = new byte[UtilsBluetooth.BLUETOOTH_BUFFER_SIZE];
        int bytes;

        if (socket.isConnected()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Arrays.fill(buffer, (byte) 0);
        while (true) try {
            if (!socket.isConnected()) {
                sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
                return;
            }

            int stream_size = inputStream.available();
            if (stream_size > 0) {
                Arrays.fill(buffer, (byte) 0);
                bytes = inputStream.read(buffer);
                if (bytes == -1) {
                    sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
                    return;
                }
                if (Utils.containsByte(buffer, (byte) UtilsBluetooth.BLUETOOTH_RECEIVE_DELIMITER.charAt(0))) {
                    if (stream_size <= UtilsBluetooth.BLUETOOTH_ONE_RECORD_SIZE) {
                        handler.obtainMessage(UtilsBluetooth.STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                    }
                }
            } else {
                SystemClock.sleep(1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] bytes) {
        if (bytes.length == 0) return;
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            sendHandlerMessage(UtilsBluetooth.STATE_READING_WRITING_FAILED);
        }
    }

    private void sendHandlerMessage(int msg) {
        Message message = Message.obtain();
        message.what = msg;
        handler.sendMessage(message);
    }
}
