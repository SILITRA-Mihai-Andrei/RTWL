package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;

public class BluetoothReceiveReply extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case BluetoothService.SERVICE_COMMAND_STOP:
                        // The string SERVICE_KEY will be used to filer the intent
                        Intent newIntent = new Intent(BluetoothService.SERVICE_STOP_KEY);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
                        break;
                    case BluetoothService.SERVICE_COMMAND_SEND:
                        Bundle resultInputBundle = RemoteInput.getResultsFromIntent(intent);
                        if (resultInputBundle != null) {
                            CharSequence reply = resultInputBundle.getCharSequence(BluetoothService.NOTIFICATION_REPLY_KEY);
                            if (reply != null && reply.length() != 0) {
                                BluetoothService.getReply(reply + "");
                            }
                            break;
                        }
                }
            }
        }
    }

}
