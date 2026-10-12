package com.halo.decomp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Spartan CE - Dynamic Commands Manager.
 * Syncs commands.json from GitHub repository, caches locally,
 * and provides offline fallbacks from assets or hardcoded list.
 */
public final class CommandManager {

    private static final String REMOTE_URL = "https://raw.githubusercontent.com/devlwte/spartance/main/commands.json";
    private static final String CACHE_FILE = "commands_cache.json";
    private static final String ASSET_FILE = "commands.json";

    public static class CommandItem {
        public final String slug;
        public final String title;
        public final String titleEn;
        public final String command;

        public CommandItem(String slug, String title, String titleEn, String command) {
            this.slug = slug != null ? slug : "";
            this.title = title != null ? title : "";
            this.titleEn = titleEn != null ? titleEn : title;
            this.command = command != null ? command : "";
        }

        public String getDisplayTitle() {
            String lang = Locale.getDefault().getLanguage();
            if ("en".equalsIgnoreCase(lang) && titleEn != null && !titleEn.isEmpty()) {
                return titleEn;
            }
            return title.isEmpty() ? command : title;
        }
    }

    private static List<CommandItem> cachedList = null;

    /**
     * Returns the active list of commands (from memory cache, file cache, assets, or defaults).
     */
    public static synchronized List<CommandItem> getCommands(Context context) {
        if (cachedList != null && !cachedList.isEmpty()) {
            return cachedList;
        }

        // 1. Try local cache file from previous sync
        List<CommandItem> list = loadFromFile(context);
        if (list != null && !list.isEmpty()) {
            cachedList = list;
            return cachedList;
        }

        // 2. Try packaged asset file
        list = loadFromAsset(context);
        if (list != null && !list.isEmpty()) {
            cachedList = list;
            return cachedList;
        }

        // 3. Built-in hardcoded fallback
        cachedList = getHardcodedDefaults();
        return cachedList;
    }

    /**
     * Asynchronously downloads commands.json from GitHub, updates cache, and runs callback.
     */
    public static void syncRemote(Context context, Runnable onComplete) {
        new Thread(() -> {
            boolean updated = false;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(REMOTE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SpartanCE-Mobile/1.0");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream in = conn.getInputStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        String jsonString = sb.toString();
                        List<CommandItem> parsed = parseJson(jsonString);
                        if (parsed != null && !parsed.isEmpty()) {
                            // Save to local cache file
                            File file = new File(context.getFilesDir(), CACHE_FILE);
                            try (FileOutputStream out = new FileOutputStream(file)) {
                                out.write(jsonString.getBytes(StandardCharsets.UTF_8));
                            }
                            synchronized (CommandManager.class) {
                                cachedList = parsed;
                            }
                            updated = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }

            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }).start();
    }

    private static List<CommandItem> loadFromFile(Context context) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return null;
        try (InputStream in = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return parseJson(sb.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<CommandItem> loadFromAsset(Context context) {
        try (InputStream in = context.getAssets().open(ASSET_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return parseJson(sb.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<CommandItem> parseJson(String jsonString) {
        try {
            JSONObject obj = new JSONObject(jsonString);
            JSONArray array = obj.optJSONArray("command");
            if (array == null) array = obj.optJSONArray("commands");
            if (array == null) return null;

            List<CommandItem> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String slug = item.optString("slug", "");
                String title = item.optString("title", "");
                String titleEn = item.has("title_EN") ? item.optString("title_EN", title) : item.optString("title_en", title);
                String command = item.optString("command", "");
                if (!command.isEmpty()) {
                    list.add(new CommandItem(slug, title, titleEn, command));
                }
            }
            return list;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<CommandItem> getHardcodedDefaults() {
        List<CommandItem> list = new ArrayList<>();
        list.add(new CommandItem("jump_on", "Activar Súper Salto", "Enable Super Jump", "cheat_super_jump 1"));
        list.add(new CommandItem("jump_off", "Desactivar Súper Salto", "Disable Super Jump", "cheat_super_jump 0"));
        list.add(new CommandItem("god_on", "Activar Modo Dios", "Enable God Mode", "cheat_deathless_player 1"));
        list.add(new CommandItem("god_off", "Desactivar Modo Dios", "Disable God Mode", "cheat_deathless_player 0"));
        list.add(new CommandItem("ammo_on", "Activar Munición Infinita", "Enable Infinite Ammo", "cheat_infinite_ammo 1"));
        list.add(new CommandItem("ammo_off", "Desactivar Munición Infinita", "Disable Infinite Ammo", "cheat_infinite_ammo 0"));
        list.add(new CommandItem("clip_on", "Activar Cargador sin Fondo", "Enable Bottomless Clip", "cheat_bottomless_clip 1"));
        list.add(new CommandItem("clip_off", "Desactivar Cargador sin Fondo", "Disable Bottomless Clip", "cheat_bottomless_clip 0"));
        list.add(new CommandItem("spawn_weapons", "Generar Todas las Armas", "Spawn All Weapons", "cheat_all_weapons"));
        list.add(new CommandItem("spawn_vehicles", "Generar Todos los Vehículos", "Spawn All Vehicles", "cheat_all_vehicles"));
        return list;
    }
}
