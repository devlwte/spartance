package com.halo.decomp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
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
        public final String offCommand;

        public CommandItem(String slug, String title, String titleEn, String command, String offCommand) {
            this.slug = slug != null ? slug : "";
            this.title = title != null ? title : "";
            this.titleEn = titleEn != null ? titleEn : title;
            this.command = command != null ? command : "";
            this.offCommand = offCommand != null ? offCommand : "";
        }

        public CommandItem(String slug, String title, String titleEn, String command) {
            this(slug, title, titleEn, command, "");
        }

        public boolean isToggle() {
            return offCommand != null && !offCommand.trim().isEmpty();
        }

        public String getDisplayTitle() {
            String lang = Locale.getDefault().getLanguage();
            if ("en".equalsIgnoreCase(lang) && titleEn != null && !titleEn.isEmpty()) {
                return titleEn;
            }
            return title.isEmpty() ? command : title;
        }

        public String getCleanTitle() {
            String t = getDisplayTitle();
            if (isToggle()) {
                if (t.startsWith("Activar ")) t = t.substring(8).trim();
                else if (t.startsWith("activar ")) t = t.substring(8).trim();
                else if (t.startsWith("Desactivar ")) t = t.substring(11).trim();
                else if (t.startsWith("desactivar ")) t = t.substring(11).trim();
                else if (t.startsWith("Enable ")) t = t.substring(7).trim();
                else if (t.startsWith("enable ")) t = t.substring(7).trim();
                else if (t.startsWith("Disable ")) t = t.substring(8).trim();
                else if (t.startsWith("disable ")) t = t.substring(8).trim();
            }
            return t;
        }
    }

    private static final String PREFS_NAME = "tactical_cheat_states";

    public static boolean isCheatActive(Context context, String slug) {
        if (slug == null || slug.isEmpty()) return false;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("active_" + slug, false);
    }

    public static void setCheatActive(Context context, String slug, boolean active) {
        if (slug == null || slug.isEmpty()) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean("active_" + slug, active).apply();
    }

    /**
     * Reapplies all cheats currently marked as ACTIVE in preferences.
     * Connects in the background to the internal Halo console socket (127.0.0.1:2323)
     * with retry logic to wait for the engine to initialize.
     */
    public static void reapplyActiveCheats(final Context context) {
        if (context == null) return;
        final Context appCtx = context.getApplicationContext();

        new Thread(() -> {
            Socket socket = null;
            // Wait up to 30 seconds for the engine telnet server to start listening
            for (int i = 0; i < 30; i++) {
                try {
                    socket = new Socket("127.0.0.1", 2323);
                    if (socket.isConnected()) {
                        break;
                    }
                } catch (Exception e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            }

            if (socket == null) return;

            try (Socket s = socket;
                 PrintWriter w = new PrintWriter(s.getOutputStream(), true)) {

                List<CommandItem> items = getCommands(appCtx);
                if (items == null || items.isEmpty()) return;

                int appliedCount = 0;
                for (CommandItem item : items) {
                    if (item.isToggle() && isCheatActive(appCtx, item.slug)) {
                        String cmd = item.command.trim();
                        if (!cmd.isEmpty()) {
                            w.println(cmd);
                            w.flush();
                            if (cmd.startsWith("cheat_") && cmd.endsWith(" 1")) {
                                w.println("(set " + cmd + ")");
                                w.println("set " + cmd);
                                w.flush();
                            }
                            appliedCount++;
                            try {
                                Thread.sleep(60);
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }

                if (appliedCount > 0) {
                    final int count = appliedCount;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(appCtx, "⚡ " + count + " truco" + (count > 1 ? "s activos restaurados" : " activo restaurado"), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception ignored) {}
        }).start();
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
     * The remote repository JSON always replaces the local cache and commands.
     */
    public static void syncRemote(Context context, Runnable onComplete) {
        new Thread(() -> {
            boolean updated = false;
            HttpURLConnection conn = null;
            try {
                // Add timestamp query parameter so Fastly CDN always returns the absolute latest commit
                String cacheBustingUrl = REMOTE_URL + "?nocache=" + System.currentTimeMillis();
                URL url = new URL(cacheBustingUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setUseCaches(false);
                conn.setDefaultUseCaches(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SpartanCE-Mobile/1.0");
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                conn.setRequestProperty("Pragma", "no-cache");
                conn.setRequestProperty("Expires", "0");

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
                            // Remote data ALWAYS overrides local cache file and memory cache
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

            // Parse not_working array (commands to hide from user interface)
            java.util.Set<String> notWorkingSet = new java.util.HashSet<>();
            JSONArray notWorkingArray = obj.optJSONArray("not_working");
            if (notWorkingArray == null) notWorkingArray = obj.optJSONArray("notWorking");
            if (notWorkingArray != null) {
                for (int i = 0; i < notWorkingArray.length(); i++) {
                    String slug = notWorkingArray.optString(i, "").trim().toLowerCase(Locale.ROOT);
                    if (!slug.isEmpty()) {
                        notWorkingSet.add(slug);
                    }
                }
            }

            List<CommandItem> rawList = new ArrayList<>();
            boolean hasExplicitOffCommand = false;

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String slug = item.optString("slug", "").trim();
                String title = item.optString("title", "").trim();
                String titleEn = item.has("title_EN") ? item.optString("title_EN", title) : item.optString("title_en", title);
                String command = item.optString("command", "").trim();
                String offCommand = item.optString("off_command", "").trim();

                if (command.isEmpty()) continue;

                // Check if this command is marked as not working
                String lowerSlug = slug.toLowerCase(Locale.ROOT);
                String baseSlug = lowerSlug.endsWith("_on") ? lowerSlug.substring(0, lowerSlug.length() - 3)
                        : lowerSlug.endsWith("_off") ? lowerSlug.substring(0, lowerSlug.length() - 4)
                        : lowerSlug;

                if (notWorkingSet.contains(lowerSlug) || notWorkingSet.contains(baseSlug)) {
                    // Hidden: do not include non-working command in the console
                    continue;
                }

                // Also check if command prefix is in notWorkingSet (e.g. "cheat_medusa")
                String cmdPrefix = command.split("\\s+")[0].toLowerCase(Locale.ROOT);
                if (notWorkingSet.contains(cmdPrefix)) {
                    continue;
                }

                if (!offCommand.isEmpty()) {
                    hasExplicitOffCommand = true;
                }
                rawList.add(new CommandItem(slug, title, titleEn, command, offCommand));
            }

            if (hasExplicitOffCommand) {
                return rawList;
            }

            // Legacy format fallback: automatically merge "_on" and "_off" pairs if present
            List<CommandItem> merged = new ArrayList<>();
            java.util.Set<String> handledSlugs = new java.util.HashSet<>();

            for (CommandItem item : rawList) {
                if (handledSlugs.contains(item.slug)) continue;

                if (item.slug.endsWith("_on")) {
                    String baseSlug = item.slug.substring(0, item.slug.length() - 3);
                    String offSlug = baseSlug + "_off";
                    CommandItem matchingOff = null;
                    for (CommandItem other : rawList) {
                        if (other.slug.equals(offSlug)) {
                            matchingOff = other;
                            break;
                        }
                    }
                    if (matchingOff != null) {
                        handledSlugs.add(item.slug);
                        handledSlugs.add(matchingOff.slug);
                        merged.add(new CommandItem(baseSlug, item.title, item.titleEn, item.command, matchingOff.command));
                        continue;
                    }
                } else if (item.slug.endsWith("_off")) {
                    String baseSlug = item.slug.substring(0, item.slug.length() - 4);
                    if (handledSlugs.contains(baseSlug + "_on")) {
                        continue;
                    }
                }
                handledSlugs.add(item.slug);
                merged.add(item);
            }
            return merged;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<CommandItem> getHardcodedDefaults() {
        List<CommandItem> list = new ArrayList<>();
        list.add(new CommandItem("jump", "Activar Súper Salto", "Enable Super Jump", "cheat_super_jump 1", "cheat_super_jump 0"));
        list.add(new CommandItem("god", "Activar Modo Dios", "Enable God Mode", "cheat_deathless_player 1", "cheat_deathless_player 0"));
        list.add(new CommandItem("ammo", "Activar Munición Infinita", "Enable Infinite Ammo", "cheat_infinite_ammo 1", "cheat_infinite_ammo 0"));
        list.add(new CommandItem("clip", "Activar Cargador sin Fondo", "Enable Bottomless Clip", "cheat_bottomless_clip 1", "cheat_bottomless_clip 0"));
        list.add(new CommandItem("omnipotent", "Activar Omnipotencia", "Enable Omnipotent", "cheat_omnipotent 1", "cheat_omnipotent 0"));
        list.add(new CommandItem("possession", "Activar Posesión por Contacto", "Enable Bump Possession", "cheat_bump_possession 1", "cheat_bump_possession 0"));
        list.add(new CommandItem("ai_inert", "Congelar Inteligencia Artificial", "Freeze AI", "ai_inert 1", "ai_inert 0"));
        list.add(new CommandItem("wireframe", "Activar Modo Alámbrico", "Enable Wireframe", "rasterizer_wireframe 1", "rasterizer_wireframe 0"));
        list.add(new CommandItem("stats", "Mostrar Estadísticas de FPS", "Show FPS Stats", "set rasterizer_stats 1", "set rasterizer_stats 0"));
        list.add(new CommandItem("fog", "Activar Niebla Atmosférica", "Enable Atmospheric Fog", "set rasterizer_fog_atmosphere 1", "set rasterizer_fog_atmosphere 0"));
        list.add(new CommandItem("textures", "Activar Texturas Difusas", "Enable Diffuse Textures", "set rasterizer_environment_diffuse_textures 1", "set rasterizer_environment_diffuse_textures 0"));
        list.add(new CommandItem("spawn_weapons", "Generar Todas las Armas", "Spawn All Weapons", "cheat_all_weapons"));
        list.add(new CommandItem("spawn_powerups", "Generar Todos los Powerups", "Spawn All Powerups", "cheat_all_powerups"));
        list.add(new CommandItem("teleport_cam", "Teletransportar Jugador a la Cámara", "Teleport Player to Camera", "cheat_teleport_to_camera"));
        return list;
    }
}
