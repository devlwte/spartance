package com.halo.decomp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLInputConnection;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Spartan CE - Floating Tactical Cheats Console.
 * Displays friendly titles, executes commands instantly, and auto-closes
 * to prevent game freezing.
 */
public final class TacticalConsole {

    private static final Map<String, Boolean> CHEAT_STATES = new HashMap<>();

    public static void ensureTelnetConsole(Context context) {
        new Thread(() -> {
            try {
                File dir = context.getExternalFilesDir(null);
                if (dir == null) return;
                File config = new File(dir, "config.toml");
                if (config.exists()) {
                    String text = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
                    if (text.contains("telnet_console = false")) {
                        text = text.replace("telnet_console = false", "telnet_console = true");
                        Files.write(config.toPath(), text.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public static void show(Context context) {
        if (!(context instanceof Activity)) return;
        final Activity activity = (Activity) context;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        activity.runOnUiThread(() -> {
            try {
                ensureTelnetConsole(activity);

                final Dialog dialog = new Dialog(activity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                // Main card container
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#EE0B0E14")); // Dark obsidian translucent
                bg.setCornerRadius(dp(activity, 14));
                bg.setStroke(dp(activity, 1.5f), Color.parseColor("#00E5FF")); // Cyan HUD border
                root.setBackground(bg);

                // --- HEADER BAR ---
                LinearLayout header = new LinearLayout(activity);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setPadding(0, 0, 0, dp(activity, 10));

                TextView tvTitle = new TextView(activity);
                tvTitle.setText("⚡ CONSOLA DE TRUCOS");
                tvTitle.setTextColor(Color.parseColor("#00E5FF"));
                tvTitle.setTextSize(15);
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                header.addView(tvTitle, titleLp);

                Button btnClose = new Button(activity);
                btnClose.setText("✕ CERRAR");
                btnClose.setTextColor(Color.WHITE);
                btnClose.setTextSize(11);
                btnClose.setTypeface(Typeface.DEFAULT_BOLD);
                GradientDrawable closeBg = new GradientDrawable();
                closeBg.setColor(Color.parseColor("#33FF3344"));
                closeBg.setCornerRadius(dp(activity, 8));
                closeBg.setStroke(dp(activity, 1), Color.parseColor("#FF5252"));
                btnClose.setBackground(closeBg);
                btnClose.setPadding(dp(activity, 12), dp(activity, 4), dp(activity, 12), dp(activity, 4));
                btnClose.setOnClickListener(v -> dialog.dismiss());
                header.addView(btnClose);

                root.addView(header);

                // --- CUSTOM COMMAND INPUT ---
                LinearLayout customRow = new LinearLayout(activity);
                customRow.setOrientation(LinearLayout.HORIZONTAL);
                customRow.setGravity(Gravity.CENTER_VERTICAL);
                customRow.setPadding(0, 0, 0, dp(activity, 10));

                final EditText etCommand = new EditText(activity);
                etCommand.setHint("Escribir comando... (ej: cheat_all_weapons)");
                etCommand.setHintTextColor(Color.parseColor("#778899"));
                etCommand.setTextColor(Color.WHITE);
                etCommand.setTextSize(12);
                etCommand.setSingleLine(true);
                GradientDrawable etBg = new GradientDrawable();
                etBg.setColor(Color.parseColor("#44101722"));
                etBg.setCornerRadius(dp(activity, 8));
                etBg.setStroke(dp(activity, 1), Color.parseColor("#334455"));
                etCommand.setBackground(etBg);
                etCommand.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
                LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                etLp.rightMargin = dp(activity, 8);
                customRow.addView(etCommand, etLp);

                Button btnSend = new Button(activity);
                btnSend.setText("EJECUTAR");
                btnSend.setTextColor(Color.parseColor("#0A0D14"));
                btnSend.setTextSize(11);
                btnSend.setTypeface(Typeface.DEFAULT_BOLD);
                GradientDrawable sendBg = new GradientDrawable();
                sendBg.setColor(Color.parseColor("#00E5FF"));
                sendBg.setCornerRadius(dp(activity, 8));
                btnSend.setBackground(sendBg);
                btnSend.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
                btnSend.setOnClickListener(v -> {
                    String input = etCommand.getText().toString().trim();
                    if (!input.isEmpty()) {
                        execute(activity, input, input);
                        dialog.dismiss();
                    }
                });
                customRow.addView(btnSend);

                root.addView(customRow);

                // --- CHEATS LIST (SCROLLABLE) ---
                ScrollView scroll = new ScrollView(activity);
                LinearLayout list = new LinearLayout(activity);
                list.setOrientation(LinearLayout.VERTICAL);

                // Cheat items with short Spanish titles
                addCheatToggle(activity, list, dialog, "🚀 Súper Salto", "cheat_super_jump");
                addCheatToggle(activity, list, dialog, "🛡️ Modo Dios (Invencible)", "cheat_deathless_player");
                addCheatToggle(activity, list, dialog, "♾️ Munición Infinita", "cheat_infinite_ammo");
                addCheatToggle(activity, list, dialog, "🔫 Cargador Sin Fondo (No Recargar)", "cheat_bottomless_clip");
                addCheatToggle(activity, list, dialog, "🛩️ Modo Jetpack (Volar)", "cheat_jetpack");
                addCheatToggle(activity, list, dialog, "👻 Camuflaje / Invisible", "cheat_active_camouflage");
                addCheatToggle(activity, list, dialog, "💥 Matar de 1 Toque (Omnipotente)", "cheat_omnipotent");
                addCheatToggle(activity, list, dialog, "👁️ Modo Medusa (Mirada Mortal)", "cheat_medusa");
                addCheatAction(activity, list, dialog, "📦 Aparecer Todas las Armas", "cheat_all_weapons");
                addCheatAction(activity, list, dialog, "🚗 Aparecer Todos los Vehículos", "cheat_all_vehicles");
                addCheatAction(activity, list, dialog, "🏃 Cámara Lenta (0.5x)", "game_speed 0.5");
                addCheatAction(activity, list, dialog, "⚡ Velocidad Normal (1.0x)", "game_speed 1.0");
                addCheatAction(activity, list, dialog, "💾 Guardar Checkpoint", "game_save");
                addCheatAction(activity, list, dialog, "🔄 Cargar Checkpoint", "game_revert");

                scroll.addView(list);
                LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 200));
                root.addView(scroll, scrollLp);

                dialog.setContentView(root);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.getWindow().setLayout(dp(activity, 460), ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                dialog.show();
            } catch (Exception ignored) {}
        });
    }

    private static void addCheatToggle(Context ctx, LinearLayout parent, Dialog dialog, String label, String cmdBase) {
        boolean current = CHEAT_STATES.getOrDefault(cmdBase, false);
        Button btn = new Button(ctx);
        btn.setText(label + (current ? " [ACTIVO]" : " [APAGADO]"));
        btn.setTextColor(current ? Color.parseColor("#00E5FF") : Color.WHITE);
        btn.setTextSize(12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(current ? Color.parseColor("#2200E5FF") : Color.parseColor("#22FFFFFF"));
        btnBg.setCornerRadius(dp(ctx, 8));
        btnBg.setStroke(dp(ctx, 1), current ? Color.parseColor("#00E5FF") : Color.parseColor("#33FFFFFF"));
        btn.setBackground(btnBg);
        btn.setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8));

        btn.setOnClickListener(v -> {
            boolean next = !CHEAT_STATES.getOrDefault(cmdBase, false);
            CHEAT_STATES.put(cmdBase, next);
            String fullCmd = cmdBase + (next ? " 1" : " 0");
            execute(ctx, label + (next ? " (Activado)" : " (Desactivado)"), fullCmd);
            dialog.dismiss();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 6);
        parent.addView(btn, lp);
    }

    private static void addCheatAction(Context ctx, LinearLayout parent, Dialog dialog, String label, String cmd) {
        Button btn = new Button(ctx);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1AFFFFFF"));
        btnBg.setCornerRadius(dp(ctx, 8));
        btnBg.setStroke(dp(ctx, 1), Color.parseColor("#2AFFFFFF"));
        btn.setBackground(btnBg);
        btn.setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8));

        btn.setOnClickListener(v -> {
            execute(ctx, label, cmd);
            dialog.dismiss();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 6);
        parent.addView(btn, lp);
    }

    public static void execute(final Context context, final String title, final String command) {
        if (command == null || command.trim().isEmpty()) return;
        final String cmd = command.trim();

        Toast.makeText(context, "⚡ " + title, Toast.LENGTH_SHORT).show();

        // 1. Send via local telnet console socket (127.0.0.1:2323) in background thread
        new Thread(() -> {
            try (Socket s = new Socket("127.0.0.1", 2323);
                 PrintWriter w = new PrintWriter(s.getOutputStream(), true)) {
                w.println(cmd);
            } catch (Exception ignored) {}
        }).start();

        // 2. Automated keystroke sequence into SDL console with exact timing
        // Opens console (~), types command, hits ENTER, and immediately closes (ESC + ~)
        // so gameplay never freezes!
        Handler handler = new Handler(Looper.getMainLooper());

        // Step 1: Open console (~)
        try {
            SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_GRAVE);
            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_GRAVE);
        } catch (Throwable ignored) {}

        // Step 2 (after 45ms): Type command string and press ENTER
        handler.postDelayed(() -> {
            try {
                for (int i = 0; i < cmd.length(); i++) {
                    char c = cmd.charAt(i);
                    SDLInputConnection.nativeGenerateScancodeForUnichar(c);
                }
                SDLInputConnection.nativeCommitText(cmd, 0);

                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ENTER);
            } catch (Throwable ignored) {}

            // Step 3 (after another 45ms): Close console so it NEVER freezes
            handler.postDelayed(() -> {
                try {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE);
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_GRAVE);
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_GRAVE);
                } catch (Throwable ignored) {}
            }, 45);
        }, 45);
    }

    private static int dp(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
