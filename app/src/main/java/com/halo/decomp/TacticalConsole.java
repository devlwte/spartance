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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kyrn.spartan.R;

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
                root.setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 16));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#F5080C14")); // Premium deep obsidian matte
                bg.setCornerRadius(dp(activity, 16));
                bg.setStroke(dp(activity, 1.2f), Color.parseColor("#1B2A3B")); // Subtle tactical slate
                root.setBackground(bg);

                // --- HEADER BAR ---
                LinearLayout header = new LinearLayout(activity);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setPadding(0, 0, 0, dp(activity, 12));

                // Left: Title & Subtitle Group
                LinearLayout titleGroup = new LinearLayout(activity);
                titleGroup.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams titleGroupLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

                TextView tvTitle = new TextView(activity);
                tvTitle.setText("CONSOLA TÁCTICA");
                tvTitle.setTextColor(Color.parseColor("#00E5FF"));
                tvTitle.setTextSize(13.5f);
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tvTitle.setLetterSpacing(0.04f);
                titleGroup.addView(tvTitle);

                TextView tvSub = new TextView(activity);
                tvSub.setText("SPARTAN ENGINE • COMANDOS");
                tvSub.setTextColor(Color.parseColor("#5A6E82"));
                tvSub.setTextSize(8.5f);
                tvSub.setTypeface(Typeface.DEFAULT_BOLD);
                tvSub.setLetterSpacing(0.06f);
                titleGroup.addView(tvSub);

                header.addView(titleGroup, titleGroupLp);

                // Action buttons sizing
                int btnSize = dp(activity, 32);
                int iconInnerSize = dp(activity, 16);

                // Reload / Sync button (uses reload_commands.svg)
                LinearLayout btnSync = new LinearLayout(activity);
                btnSync.setOrientation(LinearLayout.HORIZONTAL);
                btnSync.setGravity(Gravity.CENTER);
                btnSync.setClickable(true);
                btnSync.setFocusable(true);
                LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(btnSize, btnSize);
                syncLp.rightMargin = dp(activity, 8);

                GradientDrawable syncBg = new GradientDrawable();
                syncBg.setColor(Color.parseColor("#121924"));
                syncBg.setCornerRadius(dp(activity, 16)); // Sleek circular
                syncBg.setStroke(dp(activity, 1), Color.parseColor("#223347"));
                btnSync.setBackground(syncBg);

                ImageView ivSync = new ImageView(activity);
                ivSync.setImageDrawable(activity.getDrawable(R.drawable.ic_reload_commands));
                LinearLayout.LayoutParams ivSyncLp = new LinearLayout.LayoutParams(iconInnerSize, iconInnerSize);
                ivSync.setLayoutParams(ivSyncLp);
                btnSync.addView(ivSync);

                btnSync.setOnClickListener(v -> {
                    Toast.makeText(activity, "Sincronizando comandos desde GitHub...", Toast.LENGTH_SHORT).show();
                    CommandManager.syncRemote(activity, () -> {
                        Toast.makeText(activity, "⚡ Comandos actualizados", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        show(activity);
                    });
                });
                header.addView(btnSync, syncLp);

                // Close button (Minimalist only '✕' icon)
                LinearLayout btnClose = new LinearLayout(activity);
                btnClose.setOrientation(LinearLayout.HORIZONTAL);
                btnClose.setGravity(Gravity.CENTER);
                btnClose.setClickable(true);
                btnClose.setFocusable(true);
                LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(btnSize, btnSize);

                GradientDrawable closeBg = new GradientDrawable();
                closeBg.setColor(Color.parseColor("#121924"));
                closeBg.setCornerRadius(dp(activity, 16)); // Sleek circular
                closeBg.setStroke(dp(activity, 1), Color.parseColor("#223347"));
                btnClose.setBackground(closeBg);

                ImageView ivClose = new ImageView(activity);
                ivClose.setImageDrawable(activity.getDrawable(R.drawable.ic_console_close));
                LinearLayout.LayoutParams ivCloseLp = new LinearLayout.LayoutParams(iconInnerSize, iconInnerSize);
                ivClose.setLayoutParams(ivCloseLp);
                btnClose.addView(ivClose);

                btnClose.setOnClickListener(v -> dialog.dismiss());
                header.addView(btnClose, closeLp);

                root.addView(header);

                // --- SEARCH / CUSTOM COMMAND ROW ---
                LinearLayout customRow = new LinearLayout(activity);
                customRow.setOrientation(LinearLayout.HORIZONTAL);
                customRow.setGravity(Gravity.CENTER_VERTICAL);
                customRow.setPadding(0, 0, 0, dp(activity, 10));

                final EditText etSearch = new EditText(activity);
                etSearch.setHint("Buscar truco o escribir comando...");
                etSearch.setHintTextColor(Color.parseColor("#4D5E70"));
                etSearch.setTextColor(Color.WHITE);
                etSearch.setTextSize(11.5f);
                etSearch.setSingleLine(true);
                GradientDrawable etBg = new GradientDrawable();
                etBg.setColor(Color.parseColor("#090E16"));
                etBg.setCornerRadius(dp(activity, 8));
                etBg.setStroke(dp(activity, 1), Color.parseColor("#1A2738"));
                etSearch.setBackground(etBg);
                etSearch.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
                LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, dp(activity, 38), 1f);
                etLp.rightMargin = dp(activity, 8);
                customRow.addView(etSearch, etLp);

                // Minimalist Execute Button with play_command.svg icon
                LinearLayout btnSend = new LinearLayout(activity);
                btnSend.setOrientation(LinearLayout.HORIZONTAL);
                btnSend.setGravity(Gravity.CENTER);
                btnSend.setClickable(true);
                btnSend.setFocusable(true);
                GradientDrawable sendBg = new GradientDrawable();
                sendBg.setColor(Color.parseColor("#00E5FF")); // Halo electric cyan
                sendBg.setCornerRadius(dp(activity, 8));
                btnSend.setBackground(sendBg);
                btnSend.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 14), dp(activity, 6));

                ImageView ivPlay = new ImageView(activity);
                ivPlay.setImageDrawable(activity.getDrawable(R.drawable.ic_play_command));
                int playIconSize = dp(activity, 13);
                LinearLayout.LayoutParams ivPlayLp = new LinearLayout.LayoutParams(playIconSize, playIconSize);
                ivPlayLp.rightMargin = dp(activity, 6);
                btnSend.addView(ivPlay, ivPlayLp);

                TextView tvSend = new TextView(activity);
                tvSend.setText("EJECUTAR");
                tvSend.setTextColor(Color.parseColor("#0A0D14"));
                tvSend.setTextSize(11);
                tvSend.setTypeface(Typeface.DEFAULT_BOLD);
                btnSend.addView(tvSend);

                LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 38));
                btnSend.setOnClickListener(v -> {
                    String input = etSearch.getText().toString().trim();
                    if (!input.isEmpty()) {
                        execute(activity, input, input);
                        dialog.dismiss();
                    }
                });
                customRow.addView(btnSend, sendLp);

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
            row.setPadding(dp(ctx, 14), dp(ctx, 9), dp(ctx, 14), dp(ctx, 9));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(ctx, 8));
            if (isActive) {
                rowBg.setColor(Color.parseColor("#1800E676")); // Subtle refined emerald glow
                rowBg.setStroke(dp(ctx, 1.2f), Color.parseColor("#00E676"));
            } else if (isToggle) {
                rowBg.setColor(Color.parseColor("#0B1017")); // Deep matte obsidian
                rowBg.setStroke(dp(ctx, 1), Color.parseColor("#192535"));
            } else {
                rowBg.setColor(Color.parseColor("#0C1420")); // Subtle cyan tint
                rowBg.setStroke(dp(ctx, 1), Color.parseColor("#2600E5FF"));
            }
            row.setBackground(rowBg);

            // Left: Title
            TextView tvTitle = new TextView(ctx);
            tvTitle.setText(cleanTitle);
            tvTitle.setTextColor(isActive ? Color.parseColor("#EAFFF1") : Color.parseColor("#E2E8F0"));
            tvTitle.setTextSize(11.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tvTitle, titleLp);

            // Right: Status Badge
            TextView tvBadge = new TextView(ctx);
            tvBadge.setTextSize(9.5f);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setPadding(dp(ctx, 8), dp(ctx, 3), dp(ctx, 8), dp(ctx, 3));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dp(ctx, 4));

            if (isActive) {
                tvBadge.setText("● ACTIVO");
                tvBadge.setTextColor(Color.parseColor("#00E676"));
                badgeBg.setColor(Color.parseColor("#2600E676"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#00E676"));
            } else if (isToggle) {
                tvBadge.setText("○ INACTIVO");
                tvBadge.setTextColor(Color.parseColor("#64748B"));
                badgeBg.setColor(Color.parseColor("#121A24"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#233346"));
            } else {
                tvBadge.setText("⚡ ACCIÓN");
                tvBadge.setTextColor(Color.parseColor("#00E5FF"));
                badgeBg.setColor(Color.parseColor("#1A00E5FF"));
                badgeBg.setStroke(dp(ctx, 1), Color.parseColor("#3300E5FF"));
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
            lp.bottomMargin = dp(ctx, 5);
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
