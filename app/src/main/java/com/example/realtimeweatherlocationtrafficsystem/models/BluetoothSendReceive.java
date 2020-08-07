package com.example.realtimeweatherlocationtrafficsystem.models;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class BluetoothSendReceive extends Thread {

    private Handler handler;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public BluetoothSendReceive(BluetoothSocket socket, Handler handler, final Button send, final EditText message) {
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
                    try {
                        write(message.getText().toString().getBytes());
                        sendHandlerMessage(UtilsBluetooth.STATE_MESSAGE_SEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void run() {
        if (inputStream == null) return;

        byte[] buffer = new byte[UtilsBluetooth.BLUETOOTH_BUFFER_SIZE];
        int bytes;

        while (true) try {
            bytes = inputStream.read(buffer);
            handler.obtainMessage(UtilsBluetooth.STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
        } catch (IOException e) {
            e.printStackTrace();
            sendHandlerMessage(UtilsBluetooth.STATE_READING_FAILED);
        }
    }

    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    private void sendHandlerMessage(int msg) {
        Message message = Message.obtain();
        message.what = msg;
        handler.sendMessage(message);
    }
}
