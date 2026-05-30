package com.qube.piprapay_tool.Activity;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.qube.piprapay_tool.BuildConfig;
import com.qube.piprapay_tool.Class.BaseActivity;
import com.qube.piprapay_tool.Forwarding_Class.ForwardingConfig;
import com.qube.piprapay_tool.Forwarding_Class.ForwardingConfigDialog;
import com.qube.piprapay_tool.Forwarding_Class.ListAdapter;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Forwarding_Class.SmsReceiverService;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Iterator;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_SMS_PERMISSION = 101;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATION = 102;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 103;

    private CheckBox checkboxSms, checkboxBattery, checkbox_background_task;
    private Context context;
    private ListAdapter listAdapter;
    private RelativeLayout relativeHome, relativeSetting, relativeLog;
    private TextView greetingTextView, date_time, user_name, batteryPercentage, title_battary, syslogs_text;
    private View layout_home, layout_settings, layout_logs, btn_clear_logs;
    private ImageView homeImg, settingsImg, save_name, logsImg, btnAdd;
    private EditText editText_name;
    private Spinner spinner_ping_interval, spinner_auto_stop_delay;
    private CircularProgressIndicator batteryProgressBar;
    final Toast[] currentToast = new Toast[1];
    private String User_name;
    private ConstraintLayout clear_data_btn, about_us_btn;
    private Handler handler;
    private Runnable urlCheckRunnable;
    private SharedPreferences.OnSharedPreferenceChangeListener mainPreferenceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        User_name = sharedPreferences.getString("User_name", null);

        relativeHome = findViewById(R.id.relativeHome);
        relativeSetting = findViewById(R.id.relativeSetting);
        relativeLog = findViewById(R.id.relativeLog);
        layout_home = findViewById(R.id.layout_home);
        layout_logs = findViewById(R.id.layout_logs);
        layout_settings = findViewById(R.id.layout_settings);
        homeImg = findViewById(R.id.homeImg);
        settingsImg = findViewById(R.id.settingsImg);
        logsImg = findViewById(R.id.logsImg);
        syslogs_text = findViewById(R.id.syslogs_text);
        btn_clear_logs = findViewById(R.id.btn_clear_logs);
        greetingTextView = findViewById(R.id.greetingTextView);
        title_battary = findViewById(R.id.title_battary);
        date_time = findViewById(R.id.date_time);
        user_name = findViewById(R.id.user_name);
        editText_name = findViewById(R.id.editText_name);
        save_name = findViewById(R.id.save_name);
        spinner_ping_interval = findViewById(R.id.spinner_ping_interval);
        batteryProgressBar = findViewById(R.id.battary_progressBar);
        batteryPercentage = findViewById(R.id.battary_percentag);
        checkboxSms = findViewById(R.id.checkbox_sms);
        checkboxBattery = findViewById(R.id.checkbox_battery);
        checkbox_background_task = findViewById(R.id.checkbox_background_task);
        spinner_auto_stop_delay = findViewById(R.id.spinner_auto_stop_delay);
        clear_data_btn = findViewById(R.id.clear_data_btn);
        about_us_btn = findViewById(R.id.about_us_btn);

        showList();
        checkPermissions();
        battary_status();
        String greeting = getGreetingMessage();
        greetingTextView.setText(greeting);


        if (User_name != null && !User_name.trim().isEmpty()) {
            user_name.setText(User_name);
        }
        else {
            user_name.setText("Welcome!");
        }
        editText_name.setText(User_name);

        editText_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                save_name.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.main_color), PorterDuff.Mode.SRC_IN);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up Spinner for Ping Interval (1 to 10 minutes)
        String[] items = new String[]{"1 min", "2 min", "3 min", "4 min", "5 min", "6 min", "7 min", "8 min", "9 min", "10 min"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_ping_interval.setAdapter(adapter);

        int savedInterval = sharedPreferences.getInt("ping_interval_minutes", 2);
        int defaultPosition = savedInterval - 1;
        if (defaultPosition < 0 || defaultPosition >= 10) {
            defaultPosition = 1; // Default index to correspond to 2
        }
        spinner_ping_interval.setSelection(defaultPosition);

        spinner_ping_interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedInterval = position + 1; // 0-based index maps to 1-10 mins
                int currentSaved = sharedPreferences.getInt("ping_interval_minutes", 2);
                if (selectedInterval != currentSaved) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("ping_interval_minutes", selectedInterval);
                    editor.apply();
                    showTost("Ping interval updated to " + selectedInterval + " minutes!");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        relativeHome.setOnClickListener(v ->{

            layout_home.setVisibility(View.VISIBLE);
            layout_logs.setVisibility(View.GONE);
            layout_settings.setVisibility(View.GONE);
            if (btnAdd != null) {
                btnAdd.setVisibility(View.VISIBLE);
            }

            homeImg.setImageResource(R.drawable.ic_home_select_task);
            logsImg.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.save_defolt), PorterDuff.Mode.SRC_IN);
            settingsImg.setImageResource(R.drawable.ic_event_unselect_task);

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(70);
            }
        });
        relativeLog.setOnClickListener(v -> {
            layout_home.setVisibility(View.GONE);
            layout_logs.setVisibility(View.VISIBLE);
            layout_settings.setVisibility(View.GONE);
            if (btnAdd != null) {
                btnAdd.setVisibility(View.GONE);
            }

            homeImg.setImageResource(R.drawable.ic_home_unselect_task);
            logsImg.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.main_color), PorterDuff.Mode.SRC_IN);
            settingsImg.setImageResource(R.drawable.ic_event_unselect_task);

            syslogs_text.setText(com.qube.piprapay_tool.Class.AppLogger.getLogs(MainActivity.this));

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(70);
            }
        });
        relativeSetting.setOnClickListener(v ->{
            layout_home.setVisibility(View.GONE);
            layout_logs.setVisibility(View.GONE);
            layout_settings.setVisibility(View.VISIBLE);
            if (btnAdd != null) {
                btnAdd.setVisibility(View.GONE);
            }

            homeImg.setImageResource(R.drawable.ic_home_unselect_task);
            logsImg.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.save_defolt), PorterDuff.Mode.SRC_IN);
            settingsImg.setImageResource(R.drawable.ic_event_select_task);

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(70);
            }
        });
        btn_clear_logs.setOnClickListener(v -> {
            com.qube.piprapay_tool.Class.AppLogger.clearLogs(MainActivity.this);
            syslogs_text.setText("No logs available.");
            showTost("Logs cleared!");
        });
        View btn_export_logs = findViewById(R.id.btn_export_logs);
        btn_export_logs.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
                    return;
                }
            }
            performLogExport();
        });
        save_name.setOnClickListener(v -> {
            String name = editText_name.getText().toString().trim();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("User_name", name); // save name
            editor.apply();
            showTost("Name saved successfully!");
            save_name.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.save_defolt), PorterDuff.Mode.SRC_IN);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (name != null && !name.trim().isEmpty()) {
                user_name.setText(name);
            } else {
                user_name.setText("Welcome!");
            }
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        });

        checkboxSms.setOnClickListener(v -> {
            if (checkboxSms.isChecked()) {
                // Ask for permission if not granted
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
                }
            } else {
                // Open App Settings to allow user to revoke permission manually
                openAppSettings("To remove SMS permission manually.");
            }
        });
        checkboxBattery.setOnClickListener(v -> {
            if (checkboxBattery.isChecked()) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATION);
                }
            } else {
                openAppSettings("To disable battery optimization manually.");
            }
        });

        boolean isChecked = sharedPreferences.getBoolean("background_task_enabled", false);
        checkbox_background_task.setChecked(isChecked);
        checkbox_background_task.setOnCheckedChangeListener((buttonView, isChecked1) -> {
            // Save the checkbox state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("background_task_enabled", isChecked1);
            editor.apply();

            if (isChecked1) {
                startService();
            } else {
                stopService();
            }
        });

        // Set up Spinner for Auto Service Stop Delay (30 min to 3 hr)
        String[] autoStopItems = new String[]{"30 min", "1 hr", "1.5 hr", "2 hr", "2.5 hr", "3 hr"};
        ArrayAdapter<String> autoStopAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, autoStopItems);
        autoStopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_auto_stop_delay.setAdapter(autoStopAdapter);

        int savedAutoStopMin = sharedPreferences.getInt("auto_stop_minutes", 30);
        int autoStopPos = 0;
        if (savedAutoStopMin == 30) autoStopPos = 0;
        else if (savedAutoStopMin == 60) autoStopPos = 1;
        else if (savedAutoStopMin == 90) autoStopPos = 2;
        else if (savedAutoStopMin == 120) autoStopPos = 3;
        else if (savedAutoStopMin == 150) autoStopPos = 4;
        else if (savedAutoStopMin == 180) autoStopPos = 5;
        spinner_auto_stop_delay.setSelection(autoStopPos);

        spinner_auto_stop_delay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int minutes = 30;
                switch (position) {
                    case 0: minutes = 30; break;
                    case 1: minutes = 60; break;
                    case 2: minutes = 90; break;
                    case 3: minutes = 120; break;
                    case 4: minutes = 150; break;
                    case 5: minutes = 180; break;
                }
                int currentSaved = sharedPreferences.getInt("auto_stop_minutes", 30);
                if (minutes != currentSaved) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("auto_stop_minutes", minutes);
                    editor.apply();
                    showTost("Auto shutdown timer updated to " + autoStopItems[position] + "!");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mainPreferenceListener = (sharedPrefs, key) -> {
            if ("background_task_enabled".equals(key)) {
                boolean enabled = sharedPrefs.getBoolean("background_task_enabled", false);
                runOnUiThread(() -> {
                    checkbox_background_task.setOnCheckedChangeListener(null);
                    checkbox_background_task.setChecked(enabled);
                    checkbox_background_task.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("background_task_enabled", isChecked1);
                        editor.apply();

                        if (isChecked1) {
                            startService();
                        } else {
                            stopService();
                        }
                    });
                });
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(mainPreferenceListener);

        if (isChecked) {
            startService();
        }
        new android.os.Handler().postDelayed(new Runnable() {
                    public void run() {
                        boolean stillChecked = sharedPreferences.getBoolean("background_task_enabled", false);
                        if (stillChecked) {
                            startService();
                        }
                    }
                }, 5 * 60 * 1000 );
        clear_data_btn.setOnClickListener(v -> {
            openAppSettings("Clear data manually.");
        });
        about_us_btn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });




    }

    private void showList() {
        context = this;
        RecyclerView recyclerView = findViewById(R.id.RecyclerView_main_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);

        listAdapter = new ListAdapter(configs, context);
        recyclerView.setAdapter(listAdapter);

        btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            animateButtonClick(btnAdd);
            new ForwardingConfigDialog(context, getLayoutInflater(), listAdapter).showNew();
        });

//        if (!this.isServiceRunning()) {
//            this.startService();
//        }
    }
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SmsReceiverService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void startService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }
    private void stopService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);
        appContext.stopService(intent);
    }
    private void checkPermissions() {
        // SMS Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            checkboxSms.setChecked(true);
        } else {
            checkboxSms.setChecked(false);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
        }

        // Ignore Battery Optimization
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                checkboxBattery.setChecked(true);
            } else {
                checkboxBattery.setChecked(false);
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATION);
            }
        } else {
            checkboxBattery.setChecked(true); // Older versions don't need this
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkboxSms.setChecked(true);
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                checkboxSms.setChecked(false);
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performLogExport();
            } else {
                Toast.makeText(this, "Storage permission is required to export logs.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performLogExport() {
        boolean success = com.qube.piprapay_tool.Class.AppLogger.exportLogs(MainActivity.this);
        if (success) {
            showTost("Logs successfully exported to Downloads!");
        } else {
            showTost("Failed to export logs.");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATION) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    checkboxBattery.setChecked(true);
                } else {
                    checkboxBattery.setChecked(false);
                    Toast.makeText(this, "Battery optimization not ignored", Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void openAppSettings(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    private void animateButtonClick(View textView) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 1f, 0.9f, 1f);
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200); // Duration of the animation
        animatorSet.start();
    }
    private void showTost(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                .setAction("Hide", undo -> {
                    if (currentToast[0] != null) currentToast[0].cancel();
                });

        snackbar.setBackgroundTint(context.getResources().getColor(R.color.main_color));
        snackbar.setActionTextColor(context.getResources().getColor(R.color.white)); // yellow
        snackbar.show();
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.tost_sound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
    }
    private String getGreetingMessage() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }
    private void battary_status() {

        registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int percentage = (int) ((level / (float) scale) * 100);

                batteryProgressBar.setMax(100);
                batteryProgressBar.setProgress(percentage);
                batteryPercentage.setText(percentage + "%");

                // Show status based on battery level
                String status;
                if (percentage <= 20) {
                    status = "⚠️ Charge Immediately!";
                } else if (percentage <= 40) {
                    status = "🔋 Battery Low";
                } else if (percentage <= 60) {
                    status = "🙂 Medium";
                } else if (percentage <= 80) {
                    status = "👍 Good";
                } else if (percentage < 100) {
                    status = "💪 Very Good";
                } else {
                    status = "🔌 Fully Charged";
                }

                title_battary.setText(status);
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        date_time.setText(currentDate);
    }

    private final Runnable logRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (layout_logs != null && layout_logs.getVisibility() == View.VISIBLE && syslogs_text != null) {
                syslogs_text.setText(com.qube.piprapay_tool.Class.AppLogger.getLogs(MainActivity.this));
            }
            if (handler != null) {
                handler.postDelayed(this, 2000);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (handler == null) {
            handler = new Handler();
        }
        handler.post(logRefreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacks(logRefreshRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainPreferenceListener != null) {
            SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(mainPreferenceListener);
        }
    }
}
