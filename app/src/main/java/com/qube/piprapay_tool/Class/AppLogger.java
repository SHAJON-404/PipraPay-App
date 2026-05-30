package com.qube.piprapay_tool.Class;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.qube.piprapay_tool.Forwarding_Class.ForwardingConfig;

public class AppLogger {
    private static final String LOG_FILE = "piprapay_logs.txt";
    private static final int MAX_LOG_LINES = 100;

    public static synchronized void log(Context context, String message) {
        logRow(context, "SYS", null, message);
    }

    public static synchronized void logRow(Context context, String type, String url, String status) {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE);
            boolean isNew = !file.exists() || file.length() == 0;

            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            String idStr = "--";
            if (url != null && !url.equals("--")) {
                int id = getWebhookId(context, url);
                if (id != -1) {
                    idStr = "WH" + id;
                }
            }

            String typeCol = padRight(type, 4);
            String idCol = padRight(idStr, 3);

            String row = timestamp + " | " + typeCol + " | " + idCol + " | " + status + "\n";

            FileOutputStream fos = context.openFileOutput(LOG_FILE, Context.MODE_APPEND);
            if (isNew) {
                String header = "TIME     | TYPE | ID  | STATUS / INFO\n" +
                                "-------------------------------------\n";
                fos.write(header.getBytes());
            }
            fos.write(row.getBytes());
            fos.close();

            trimLogFile(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getWebhookId(Context context, String url) {
        try {
            ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).getUrl().equals(url)) {
                    return i + 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static synchronized String getLogs(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            File file = new File(context.getFilesDir(), LOG_FILE);
            if (!file.exists()) {
                return "No logs available.";
            }
            FileInputStream fis = context.openFileInput(LOG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading logs: " + e.getMessage();
        }
        return sb.toString();
    }

    public static synchronized void clearLogs(Context context) {
        try {
            context.deleteFile(LOG_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void trimLogFile(Context context) {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE);
            if (!file.exists()) return;

            ArrayList<String> lines = new ArrayList<>();
            FileInputStream fis = context.openFileInput(LOG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();

            if (lines.size() > MAX_LOG_LINES) {
                FileOutputStream fos = context.openFileOutput(LOG_FILE, Context.MODE_PRIVATE);
                for (int i = lines.size() - MAX_LOG_LINES; i < lines.size(); i++) {
                    fos.write((lines.get(i) + "\n").getBytes());
                }
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean exportLogs(Context context) {
        String logContent = getLogs(context);
        String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String timestampStr = String.valueOf(System.currentTimeMillis() / 1000L);
        String fileName = "piprapay-" + dateStr + "-" + timestampStr + ".txt";

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentResolver resolver = context.getContentResolver();
                android.content.ContentValues contentValues = new android.content.ContentValues();
                contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/PipraPay");

                android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    java.io.OutputStream os = resolver.openOutputStream(uri);
                    if (os != null) {
                        os.write(logContent.getBytes());
                        os.close();
                        return true;
                    }
                }
            } else {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir != null) {
                    File piprapayDir = new File(downloadsDir, "PipraPay");
                    if (!piprapayDir.exists()) {
                        piprapayDir.mkdirs();
                    }
                    File logFile = new File(piprapayDir, fileName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile);
                    fos.write(logContent.getBytes());
                    fos.close();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
