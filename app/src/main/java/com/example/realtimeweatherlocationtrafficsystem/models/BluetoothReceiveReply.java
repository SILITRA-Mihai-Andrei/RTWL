package com.example.realtimeweatherlocationtrafficsystem.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimeweatherlocationtrafficsystem.services.BluetoothService;

/**
 * Handle messages received from broadcasts.
 * Send back broadcasts message or call static functions.
 */
public class BluetoothReceiveReply extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (intent.getAction() == null) return;

        switch (intent.getAction()) {
            // Broadcast message sent by Bluetooth foreground service from its notification
            // Message sent when the notification button close/stop is clicked
            case BluetoothService.SERVICE_COMMAND_STOP:
                // The string SERVICE_STOP_KEY will be used to filer the intent
                Intent newIntent = new Intent(BluetoothService.SERVICE_STOP_KEY);
                // Send a broadcast message to activities that needs this event
                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
                break;
            // Broadcast message sent by Bluetooth foreground service from its notification
            // Message sent when the notification button send command is click
            case BluetoothService.SERVICE_COMMAND_SEND:
                // Get the message written in notification using a remote input
                Bundle resultInputBundle = RemoteInput.getResultsFromIntent(intent);
                // Check if the bundle containing the message exists
                if (resultInputBundle != null) {
                    // Get the message from intent using the key declared in the service class
                    CharSequence reply = resultInputBundle.getCharSequence(BluetoothService.NOTIFICATION_REPLY_KEY);
                    // Check if the reply exists and is not empty
                    if (reply != null && reply.length() != 0) {
                        // Send the message to Bluetooth device
                        // TODO: this method must be modified and used in other class; MEMORY LEAK!
                        BluetoothService.getReply(reply + "");
                    }
                    break;
                }
        }
    }

}
