package com.example.realtimeweatherlocationtrafficsystem.models;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.realtimeweatherlocationtrafficsystem.R;

import java.util.UUID;

public class UtilsBluetooth {

    public final static int STATE_LISTENING = 1;
    public final static int STATE_CONNECTING = 2;
    public final static int STATE_CONNECTED = 3;
    public final static int STATE_CONNECTION_FAILED = 4;
    public final static int STATE_READING_FAILED = 5;
    public final static int STATE_MESSAGE_RECEIVED = 6;
    public final static int STATE_MESSAGE_SEND = 7;

    public final static int BLUETOOTH_BUFFER_SIZE = 1024;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static Handler getBluetoothHandler(final Context context, final TextView receiveBox, final TextView status){
        return new android.os.Handler(new android.os.Handler.Callback() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch(msg.what){
                    case UtilsBluetooth.STATE_LISTENING:
                        Toast.makeText(context, "Listening", Toast.LENGTH_SHORT).show();
                        break;
                    case UtilsBluetooth.STATE_CONNECTING:
                        Toast.makeText(context, "Connecting", Toast.LENGTH_SHORT).show();
                        break;
                    case UtilsBluetooth.STATE_CONNECTED:
                        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                        break;
                    case UtilsBluetooth.STATE_CONNECTION_FAILED:
                        Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show();
                        break;
                    case UtilsBluetooth.STATE_READING_FAILED:
                        Toast.makeText(context, "Reading incoming message failed", Toast.LENGTH_SHORT).show();
                        break;
                    case UtilsBluetooth.STATE_MESSAGE_RECEIVED:
                        byte[] readBuffer = (byte[]) msg.obj;
                        String message = new String(readBuffer, 0, msg.arg1);
                        receiveBox.setText(receiveBox.getText().toString()+message);
                        break;
                    case UtilsBluetooth.STATE_MESSAGE_SEND:
                        status.setText(context.getResources().getString(R.string.message_send));
                        break;
                }
                return true;
            }
        });
    }

}
