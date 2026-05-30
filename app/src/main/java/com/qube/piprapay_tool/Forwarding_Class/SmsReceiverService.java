package com.qube.piprapay_tool.Forwarding_Class;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.qube.piprapay_tool.Class.AppLogger;
import com.qube.piprapay_tool.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SmsReceiverService extends Service {

    BroadcastReceiver receiver;

    private static final String CHANNEL_ID = "SmsDefault";

    private Handler handler;
    private Runnable heartbeatRunnable;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private long lastSuccessPingTimestamp = System.currentTimeMillis();

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

        // Start heartbeat if enabled
        SharedPreferences sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        boolean isEnabled = sharedPref.getBoolean("background_task_enabled", false);
        if (isEnabled) {
            AppLogger.logRow(this, "SYS", null, "SERVICE STARTED");
            startHeartbeat();
        } else {
            stopSelf();
            return;
        }

        // Listen for preference changes to dynamically adjust or stop/start heartbeat pings
        preferenceListener = (sharedPreferences, key) -> {
            if ("ping_interval_minutes".equals(key) || "background_task_enabled".equals(key)) {
                boolean enabled = sharedPreferences.getBoolean("background_task_enabled", false);
                if (enabled) {
                    AppLogger.logRow(this, "SYS", null, "SERVICE ENABLED");
                    restartHeartbeat();
                } else {
                    AppLogger.logRow(this, "SYS", null, "SERVICE DISABLED");
                    stopHeartbeat();
                }
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void startHeartbeat() {
        if (handler == null) {
            handler = new Handler();
        }
        if (heartbeatRunnable != null) {
            handler.removeCallbacks(heartbeatRunnable);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        final int intervalMinutes = sharedPreferences.getInt("ping_interval_minutes", 2);
        final long intervalMillis = intervalMinutes * 60 * 1000L;

        lastSuccessPingTimestamp = System.currentTimeMillis();

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                int autoStopMinutes = sharedPreferences.getInt("auto_stop_minutes", 30);
                long autoStopMillis = autoStopMinutes * 60 * 1000L;

                long elapsed = System.currentTimeMillis() - lastSuccessPingTimestamp;
                if (elapsed >= autoStopMillis) {
                    AppLogger.logRow(SmsReceiverService.this, "SYS", null, "AUTO-STOP: Idle " + autoStopMinutes + "m without success");
                    
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("background_task_enabled", false);
                    editor.apply();
                    
                    stopSelf();
                    return;
                }

                ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(SmsReceiverService.this);
                for (ForwardingConfig config : configs) {
                    if (config.getIsSmsEnabled()) {
                        checkIfActive(config);
                    }
                }
                // Repeat after configured minutes
                handler.postDelayed(this, intervalMillis);
            }
        };
        handler.post(heartbeatRunnable);
    }

    private void stopHeartbeat() {
        if (handler != null && heartbeatRunnable != null) {
            handler.removeCallbacks(heartbeatRunnable);
        }
    }

    private void restartHeartbeat() {
        stopHeartbeat();
        startHeartbeat();
    }

    private void checkIfActive(ForwardingConfig config) {
        String url = config.getUrl();
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST, url,
                response -> {
                    Log.d("PipraPay", "Ping response: " + response);
                    lastSuccessPingTimestamp = System.currentTimeMillis();
                    AppLogger.logRow(SmsReceiverService.this, "PING", url, "SUCCESS");
                },
                error -> {
                    Log.e("PipraPay", "Ping error: " + error.toString());
                    AppLogger.logRow(SmsReceiverService.this, "PING", url, "FAILED");
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("success", true);
                    jsonBody.put("time_stamp", System.currentTimeMillis() / 1000L);
                    return jsonBody.toString().getBytes("utf-8");
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Content-Type", "application/json; charset=utf-8");
                try {
                    JSONObject jsonHeaders = new JSONObject(config.getHeaders());
                    Iterator<String> keys = jsonHeaders.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        headerMap.put(key, jsonHeaders.getString(key));
                    }
                } catch (Exception e) {
                    Log.e("PipraPay", "Failed to parse headers", e);
                }
                return headerMap;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, config.getRetriesNumber(), DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        Volley.newRequestQueue(this).add(stringRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        stopHeartbeat();

        if (preferenceListener != null) {
            SharedPreferences sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
            sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }

        AppLogger.logRow(this, "SYS", null, "SERVICE STOPPED");

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