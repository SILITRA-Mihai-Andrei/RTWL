package com.example.realtimeweatherlocationtrafficsystem.models;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

public class BluetoothClientClass extends Thread{

    BluetoothSendReceive sendReceive;
    private BluetoothSocket socket;
    private BluetoothAdapter adapter;
    private Handler handler;
    private Button send;
    private EditText message;

    public BluetoothClientClass(BluetoothSocket socket, BluetoothAdapter adapter, Handler handler, Button send, EditText message) {
        this.adapter = adapter;
        this.handler = handler;
        this.send = send;
        this.message = message;
        this.socket = socket;
    }

    public void run() {
        try {
            adapter.cancelDiscovery();
            sendHandlerMessage(UtilsBluetooth.STATE_CONNECTING);
            if(socket==null) return;
            if(!socket.isConnected()){
                socket.connect();
            }
            if(socket.isConnected())
                sendHandlerMessage(UtilsBluetooth.STATE_CONNECTED);
            else{
                sendHandlerMessage(UtilsBluetooth.STATE_CONNECTION_FAILED);
                return;
            }
            sendReceive = new BluetoothSendReceive(socket, handler, send, message);
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

    public BluetoothSendReceive getBluetoothSendReceive(){
        return sendReceive;
    }
    public BluetoothSocket getSocket(){return socket;}
}
