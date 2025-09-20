package com.qube.piprapay_tool.Forwarding_Class;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.qube.piprapay_tool.R;

public class SmsReceiverService extends Service {

    BroadcastReceiver receiver;

    private static final String CHANNEL_ID = "SmsDefault";

    public SmsReceiverService() {
        receiver = new SmsBroadcastReceiver();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        } else {
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        }

        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);

            // Load custom layout
            RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.custom_notification);
            notificationLayout.setTextViewText(R.id.text_title, "PipraPay is doing his work...");
            notificationLayout.setTextViewText(R.id.text_content, "Connected");
            notificationLayout.setImageViewResource(R.id.image_icon, R.drawable.ant_round);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ant_round)
                    .setCustomContentView(notificationLayout)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setColor(getColor(R.color.main_color))
                    .build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}