package com.halo.decomp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spartan CE - Dynamic Tactical Cheats Console.
 * Loads dynamic commands from GitHub JSON (with offline fallbacks),
 * executes instantly, and auto-closes without freezing gameplay.
 */
public final class TacticalConsole {

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

                // Auto-sync in background so next time it has latest
                CommandManager.syncRemote(activity, null);

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
                header.setPadding(0, 0, 0, dp(activity, 8));

                TextView tvTitle = new TextView(activity);
                tvTitle.setText("⚡ CONSOLA TÁCTICA");
                tvTitle.setTextColor(Color.parseColor("#00E5FF"));
                tvTitle.setTextSize(15);
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                header.addView(tvTitle, titleLp);

                // Refresh / Sync from GitHub button
                Button btnSync = new Button(activity);
                btnSync.setText("🔄 ACTUALIZAR");
                btnSync.setTextColor(Color.parseColor("#AEE7FF"));
                btnSync.setTextSize(10);
                btnSync.setTypeface(Typeface.DEFAULT_BOLD);
                GradientDrawable syncBg = new GradientDrawable();
                syncBg.setColor(Color.parseColor("#2200E5FF"));
                syncBg.setCornerRadius(dp(activity, 6));
                syncBg.setStroke(dp(activity, 1), Color.parseColor("#4400E5FF"));
                btnSync.setBackground(syncBg);
                btnSync.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));
                LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                syncLp.rightMargin = dp(activity, 8);
                btnSync.setOnClickListener(v -> {
                    Toast.makeText(activity, "Sincronizando comandos desde GitHub...", Toast.LENGTH_SHORT).show();
                    CommandManager.syncRemote(activity, () -> {
                        Toast.makeText(activity, "⚡ Comandos actualizados", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        show(activity);
                    });
                });
                header.addView(btnSync, syncLp);

                // Close button
                Button btnClose = new Button(activity);
                btnClose.setText("✕ CERRAR");
                btnClose.setTextColor(Color.WHITE);
                btnClose.setTextSize(10);
                btnClose.setTypeface(Typeface.DEFAULT_BOLD);
                GradientDrawable closeBg = new GradientDrawable();
                closeBg.setColor(Color.parseColor("#33FF3344"));
                closeBg.setCornerRadius(dp(activity, 6));
                closeBg.setStroke(dp(activity, 1), Color.parseColor("#FF5252"));
                btnClose.setBackground(closeBg);
                btnClose.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 10), dp(activity, 4));
                btnClose.setOnClickListener(v -> dialog.dismiss());
                header.addView(btnClose);

                root.addView(header);

                // --- SEARCH / CUSTOM COMMAND ROW ---
                LinearLayout customRow = new LinearLayout(activity);
                customRow.setOrientation(LinearLayout.HORIZONTAL);
                customRow.setGravity(Gravity.CENTER_VERTICAL);
                customRow.setPadding(0, 0, 0, dp(activity, 8));

                final EditText etSearch = new EditText(activity);
                etSearch.setHint("Buscar truco o escribir comando...");
                etSearch.setHintTextColor(Color.parseColor("#778899"));
                etSearch.setTextColor(Color.WHITE);
                etSearch.setTextSize(12);
                etSearch.setSingleLine(true);
                GradientDrawable etBg = new GradientDrawable();
                etBg.setColor(Color.parseColor("#44101722"));
                etBg.setCornerRadius(dp(activity, 8));
                etBg.setStroke(dp(activity, 1), Color.parseColor("#334455"));
                etSearch.setBackground(etBg);
                etSearch.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
                LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                etLp.rightMargin = dp(activity, 8);
                customRow.addView(etSearch, etLp);

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
                    String input = etSearch.getText().toString().trim();
                    if (!input.isEmpty()) {
                        execute(activity, input, input);
                        dialog.dismiss();
                    }
                });
                customRow.addView(btnSend);

                root.addView(customRow);

                // --- DYNAMIC COMMANDS LIST (SCROLLABLE) ---
                ScrollView scroll = new ScrollView(activity);
                final LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final List<CommandManager.CommandItem> allItems = CommandManager.getCommands(activity);
                renderCommandList(activity, listContainer, dialog, allItems, "");

                // Live filter as user types
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        renderCommandList(activity, listContainer, dialog, allItems, s.toString().trim());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });

                scroll.addView(listContainer);
                LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 220));
                root.addView(scroll, scrollLp);

                dialog.setContentView(root);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.getWindow().setLayout(dp(activity, 480), ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                dialog.show();
            } catch (Exception ignored) {}
        });
    }

    private static void renderCommandList(Context ctx, LinearLayout container, Dialog dialog,
                                          List<CommandManager.CommandItem> items, String filter) {
        container.removeAllViews();
        String lowerFilter = filter.toLowerCase(Locale.ROOT);

        int count = 0;
        for (CommandManager.CommandItem item : items) {
            String cleanTitle = item.getCleanTitle();
            String cmd = item.command;
            String offCmd = item.offCommand;

            if (!filter.isEmpty()
                    && !cleanTitle.toLowerCase(Locale.ROOT).contains(lowerFilter)
                    && !item.getDisplayTitle().toLowerCase(Locale.ROOT).contains(lowerFilter)
                    && !cmd.toLowerCase(Locale.ROOT).contains(lowerFilter)
                    && !offCmd.toLowerCase(Locale.ROOT).contains(lowerFilter)) {
                continue;
            }

            count++;
            boolean isToggle = item.isToggle();
            boolean isActive = isToggle && CommandManager.isCheatActive(ctx, item.slug);

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setClickable(true);
            row.setFocusable(true);
            row.setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(ctx, 8));
            if (isActive) {
                rowBg.setColor(Color.parseColor("#2600E676")); // Emerald green active glow
                rowBg.setStroke(dp(ctx, 1.5f), Color.parseColor("#00E676"));
            } else if (isToggle) {
                rowBg.setColor(Color.parseColor("#14101722")); // Dark subtle translucent
                rowBg.setStroke(dp(ctx, 1), Color.parseColor("#2B3B4C"));
            } else {
                rowBg.setColor(Color.parseColor("#1400E5FF")); // Instant action cyan
                rowBg.setStroke(dp(ctx, 1), Color.parseColor("#3300E5FF"));
            }
            row.setBackground(rowBg);

            // Left: Title
            TextView tvTitle = new TextView(ctx);
            tvTitle.setText(cleanTitle);
            tvTitle.setTextColor(isActive ? Color.parseColor("#E0FFE8") : Color.WHITE);
            tvTitle.setTextSize(12);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tvTitle, titleLp);

            // Right: Status Badge
            TextView tvBadge = new TextView(ctx);
            tvBadge.setTextSize(10);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setPadding(dp(ctx, 8), dp(ctx, 3), dp(ctx, 8), dp(ctx, 3));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dp(ctx, 4));

            if (isActive) {
                tvBadge.setText("● ACTIVO");
                tvBadge.setTextColor(Color.parseColor("#00E676"));
                badgeBg.setColor(Color.parseColor("#3300E676"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#00E676"));
            } else if (isToggle) {
                tvBadge.setText("○ DESACTIVADO");
                tvBadge.setTextColor(Color.parseColor("#8899A6"));
                badgeBg.setColor(Color.parseColor("#1A8899A6"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#338899A6"));
            } else {
                tvBadge.setText("⚡ EJECUTAR");
                tvBadge.setTextColor(Color.parseColor("#00E5FF"));
                badgeBg.setColor(Color.parseColor("#2200E5FF"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#4400E5FF"));
            }
            tvBadge.setBackground(badgeBg);
            row.addView(tvBadge);

            row.setOnClickListener(v -> {
                if (isToggle) {
                    boolean newState = !isActive;
                    CommandManager.setCheatActive(ctx, item.slug, newState);
                    String toRun = newState ? item.command : item.offCommand;
                    String feedback = cleanTitle + (newState ? ": ACTIVADO" : ": DESACTIVADO");
                    execute(ctx, feedback, toRun);
                } else {
                    execute(ctx, item.getDisplayTitle(), item.command);
                }
                dialog.dismiss();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(ctx, 6);
            container.addView(row, lp);
        }

        if (count == 0) {
            TextView empty = new TextView(ctx);
            empty.setText("No se encontraron trucos. Presiona 'EJECUTAR' para enviarlo como comando libre.");
            empty.setTextColor(Color.parseColor("#88AACC"));
            empty.setTextSize(11);
            empty.setPadding(dp(ctx, 12), dp(ctx, 16), dp(ctx, 12), dp(ctx, 16));
            empty.setGravity(Gravity.CENTER);
            container.addView(empty);
        }
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
                // Also send "(set ...)" variant if it's a cheat toggle
                if (cmd.startsWith("cheat_") && (cmd.endsWith(" 1") || cmd.endsWith(" 0"))) {
                    w.println("(set " + cmd + ")");
                    w.println("set " + cmd);
                }
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
                // If it's a toggle command and doesn't have "set", also try standard typing
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
