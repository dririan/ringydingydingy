package org.vorti.RingyDingyDingy;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String message = "";
        if(bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];

            // Get the activation code
            PreferenceManager preferencemanager = new PreferenceManager(context.getSharedPreferences(PreferenceManager.PREFERENCE_NAME, Context.MODE_PRIVATE));
            String code = preferencemanager.getCode();

            for(int i=0; i< msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                message = msgs[i].getMessageBody().toString();
                ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(message.trim().split("\\s+")));
                String source = msgs[i].getOriginatingAddress();

                if(tokens.get(0).compareToIgnoreCase("RingyDingyDingy") == 0 && tokens.get(1).compareTo(code) == 0) {
                    // Drop the SMS message so it doesn't go to the user's inbox
                    this.abortBroadcast();

                    if(tokens.size() < 3)
                        tokens.add("ring");

                    if(tokens.get(2).compareToIgnoreCase("lock") == 0) {
                        if(Integer.parseInt(Build.VERSION.SDK) >= 8) {
                            LockingSupport lockingSupport = LockingSupport.getInstance(context);
                            if(lockingSupport.isActive()) {
                                lockingSupport.lock();
                                sendSMS(source, Resources.getString(R.string.lock_success, context));
                            }
                            else
                                sendSMS(source, Resources.getString(R.string.lock_error_needs_permission, context));
                        }
                        else
                            sendSMS(source, Resources.getString(R.string.lock_error_needs_froyo, context));
                    }
                    else if(tokens.get(2).compareToIgnoreCase("ring") == 0) {
                        Intent remoteRingIntent = new Intent();
                        remoteRingIntent.setClass(context, RemoteRingActivity.class)
                                        .setData(Uri.fromParts("remotering", source, null))
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(remoteRingIntent);
                    }
                    else
                        sendSMS(source, Resources.getString(R.string.unknown_command, context));
                }
            }
        }
    }

    private void sendSMS(String destination, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(destination, null, message, null, null);
    }

}

