package com.halo.decomp;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Halo engine display and graphics configuration in config.toml
 * and SharedPreferences.
 */
public final class EngineConfigManager {
    public static final String PREFS_ENGINE = "engine-config-prefs";

    public static float getFov(Context context) {
        return context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).getFloat("fov", 90.0f);
    }

    public static void setFov(Context context, float fov) {
        context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).edit().putFloat("fov", fov).apply();
        syncToConfigFile(context);
    }

    public static boolean isPerPixelLighting(Context context) {
        return context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).getBoolean("per_pixel_lighting", true);
    }

    public static void setPerPixelLighting(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).edit().putBoolean("per_pixel_lighting", enabled).apply();
        syncToConfigFile(context);
    }

    public static String getAntiAliasing(Context context) {
        return context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).getString("anti_aliasing", "fxaa");
    }

    public static void setAntiAliasing(Context context, String aa) {
        context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).edit().putString("anti_aliasing", aa).apply();
        syncToConfigFile(context);
    }

    public static int getShadowResolution(Context context) {
        return context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).getInt("shadow_resolution", 512);
    }

    public static void setShadowResolution(Context context, int res) {
        context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE).edit().putInt("shadow_resolution", res).apply();
        syncToConfigFile(context);
    }

    public static void syncToConfigFile(Context context) {
        new Thread(() -> {
            try {
                File dir = context.getExternalFilesDir(null);
                if (dir == null) return;
                File config = new File(dir, "config.toml");
                if (!config.exists()) return;

                SharedPreferences p = context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE);
                float fov = p.getFloat("fov", 90.0f);
                boolean perPixel = p.getBoolean("per_pixel_lighting", true);
                String aa = p.getString("anti_aliasing", "fxaa");
                int shadow = p.getInt("shadow_resolution", 512);

                String text = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);

                text = updateTomlKey(text, "fov", String.format(java.util.Locale.US, "%.1f", fov));
                text = updateTomlKey(text, "per_pixel_lighting", String.valueOf(perPixel));
                text = updateTomlKey(text, "anti_aliasing", "\"" + aa + "\"");
                text = updateTomlKey(text, "shadow_resolution", String.valueOf(shadow));

                Files.write(config.toPath(), text.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }).start();
    }

    private static String updateTomlKey(String text, String key, String value) {
        Pattern p = Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "\\s*=\\s*).*?$");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.replaceFirst("$1" + Matcher.quoteReplacement(value));
        }
        return text;
    }
}
