package com.halo.decomp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spartan CE - Dynamic MOTD & Version HUD Message Manager.
 * Handles rotating tactical messages, offline defaults, and optional remote JSON sync from GitHub.
 */
public class MotdManager {

    private static final String PREFS_NAME = "spartan-motd-prefs";
    private static final String KEY_INDEX = "motd_current_index";
    private static final String KEY_SYNC_PERMISSION = "motd_sync_permission"; // -1: not asked, 0: denied, 1: allowed
    private static final String REMOTE_FILE = "spartan_remote_motd.json";
    private static final String GITHUB_MOTD_URL = "https://raw.githubusercontent.com/devlwte/spartance/main/motd.json";

    // Default internal fallback array (100% legal-safe & trademark-free)
    private static final String[] DEFAULT_MESSAGES = {
        "Spartan CE • v1.0.0",
        "Engine Built by @kyrnforge",
        "Tactical Mobile Engine Active",
        "HUD Controller & Aim System Ready",
        "Follow @kyrnforge for Updates"
    };

    private static String cachedMessage = null;

    /**
     * Gets the active message for the current session.
     * Rotates to the next message on each launch.
     */
    public static synchronized String getRotatingMessage(Context context) {
        if (cachedMessage != null) {
            return cachedMessage;
        }

        List<String> list = loadMessages(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int index = prefs.getInt(KEY_INDEX, 0);
        if (index >= list.size()) {
            index = 0;
        }

        cachedMessage = list.get(index);

        // Advance index for next app launch
        int nextIndex = (index + 1) % list.size();
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply();

        return cachedMessage;
    }

    /**
     * Returns currently cached message without advancing the index.
     */
    public static String getCurrentMessage(Context context) {
        if (cachedMessage == null) {
            return getRotatingMessage(context);
        }
        return cachedMessage;
    }

    /**
     * Loads messages from local cached JSON if available, otherwise returns default list.
     */
    public static List<String> loadMessages(Context context) {
        List<String> result = new ArrayList<>();
        File file = new File(context.getFilesDir(), REMOTE_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                if (json.has("messages")) {
                    JSONArray array = json.getJSONArray("messages");
                    for (int i = 0; i < array.length(); i++) {
                        String msg = array.getString(i).trim();
                        if (!msg.isEmpty()) {
                            result.add(msg);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fall back to defaults on corrupt JSON
            }
        }

        if (result.isEmpty()) {
            result.addAll(Arrays.asList(DEFAULT_MESSAGES));
        }
        return result;
    }

    /**
     * Checks if remote sync is allowed and fetches the latest JSON in the background.
     * If permission hasn't been requested yet, shows a polite consent dialog.
     */
    public static void checkAndSyncRemote(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int permission = prefs.getInt(KEY_SYNC_PERMISSION, -1);

        if (permission == -1) {
            // First time: Ask polite permission
            activity.runOnUiThread(() -> {
                new AlertDialog.Builder(activity)
                    .setTitle("Sincronización Spartan CE")
                    .setMessage("¿Deseas activar la sincronización periódica de avisos comunitarios y notas de versión?\n\n(Descarga ligera de ~1 KB desde GitHub de forma segura).")
                    .setPositiveButton("Permitir", (dialog, which) -> {
                        prefs.edit().putInt(KEY_SYNC_PERMISSION, 1).apply();
                        fetchRemoteJson(activity.getApplicationContext());
                    })
                    .setNegativeButton("Solo Offline", (dialog, which) -> {
                        prefs.edit().putInt(KEY_SYNC_PERMISSION, 0).apply();
                    })
                    .show();
            });
        } else if (permission == 1) {
            // Permission already granted: fetch in background
            fetchRemoteJson(activity.getApplicationContext());
        }
    }

    private static void fetchRemoteJson(Context context) {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_MOTD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }

                    // Validate that it's valid JSON with "messages"
                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("messages") && json.getJSONArray("messages").length() > 0) {
                        File target = new File(context.getFilesDir(), REMOTE_FILE);
                        try (FileOutputStream fos = new FileOutputStream(target)) {
                            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {
                // Silent fail: keep using internal array without disrupting the player
            }
        }).start();
    }
}
