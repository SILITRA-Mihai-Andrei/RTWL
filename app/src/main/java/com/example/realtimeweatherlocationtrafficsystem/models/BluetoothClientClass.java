package com.example.realtimeweatherlocationtrafficsystem.models;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

public class BluetoothClientClass extends Thread{

    private BluetoothSocket socket;
    private BluetoothAdapter adapter;
    private Handler handler;
    private Button send;
    private EditText message;

    public BluetoothClientClass(BluetoothDevice device, BluetoothAdapter adapter, Handler handler, Button send, EditText message) {
        this.adapter = adapter;
        this.handler = handler;
        this.send = send;
        this.message = message;
        try {
            socket = device.createRfcommSocketToServiceRecord(UtilsBluetooth.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
        }
    }

    public void run() {
        try {
            adapter.cancelDiscovery();
            socket.connect();
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTED);
            BluetoothSendReceive sendReceive = new BluetoothSendReceive(socket, handler, send, message);
            sendReceive.start();
        } catch (IOException e) {
            e.printStackTrace();
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
        }
    }

    private void sendHandlerMessage(int msg) {
        Message message = Message.obtain();
        message.what = msg;
        handler.sendMessage(message);
    }
}
