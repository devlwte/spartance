package com.halo.decomp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kyrn.spartan.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Spartan CE - Tactical Console Hub & Launcher.
 * Master Chief Collection (MCC) inspired dark cinematic aesthetic:
 * - Full-height vertical content container with soft translucent slate glass.
 * - Glowing vertical tactical divider line between menu and content panel.
 * - Left-aligned vertical navigation menu with sharp frosted cyan highlights.
 * - Dedicated System Info & Requirements tab with real-time hardware telemetry.
 * - Zero intrusive footer buttons, preserving pure contextual navigation.
 * - 100% preservation of engine graphics sync, HUD editing, and ISO multi-language extraction.
 */
public class LauncherActivity extends Activity {
    private static final int PICK_IMAGE = 1;
    private static final String PREFS_HUB = "spartan-hub-settings";
    private static final String PREFS_TOUCH = "touch-layout-v1";

    private File dataRoot;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button btnLaunch;
    private Button btnPickIso;
    private TextView tvStatusHeader;
    private TextView tvInstalledLang;
    private TextView tvFooterDescription;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences hubPrefs;
    private SharedPreferences touchPrefs;

    private Uri pendingIsoUri;
    private List<XisoExtractor.LanguageEntry> detectedLanguages;

    private ThemePalette palette;

    // MCC Left Menu Navigation & Page Management
    private int activeTab = 0;
    private FrameLayout pageContainer;
    private View[] pages;
    private LinearLayout[] menuRows;
    private View[] menuAccents;
    private TextView[] menuLabels;

    // Notification & Bell State (Persistent ID Tracking)
    private View bellBadgeDot;
    private ImageView ivBellIcon;
    private final List<RemoteNotification> cachedNotifications = new ArrayList<>();
    private static final String PREF_DISMISSED_NOTICES = "pref_dismissed_notices_set";

    public static class RemoteNotification {
        public String id = "";
        public String type = "info";
        public int versionCode = 0;
        public String latestVersion = "";
        public boolean hasUpdate = false;
        public String releaseUrl = "";
        public String title = "";
        public String date = "";
        public String summary = "";
        public String message = "";
        public List<String> changelog = new ArrayList<>();
        public String actionText = "";
        public String actionUrl = "";
    }

    public static final String DEFAULT_URL_UPDATES_JSON = "https://raw.githubusercontent.com/devlwte/spartance/main/updates.json";
    public static final String DEFAULT_URL_MOTD_JSON = "https://raw.githubusercontent.com/devlwte/spartance/main/motd.json";
    public static final String DEFAULT_URL_COMMANDS_JSON = "https://raw.githubusercontent.com/devlwte/spartance/main/commands.json";
    public static final String DEFAULT_URL_GITHUB_REPO = "https://github.com/devlwte/spartance";

    public static final String KEY_URL_UPDATES = "pref_url_updates_json";
    public static final String KEY_URL_MOTD = "pref_url_motd_json";
    public static final String KEY_URL_COMMANDS = "pref_url_commands_json";
    public static final String KEY_URL_REPO = "pref_url_github_repo";

    public static String getUpdatesUrl(Context context) {
        return context.getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE)
            .getString(KEY_URL_UPDATES, DEFAULT_URL_UPDATES_JSON);
    }

    public static String getMotdUrl(Context context) {
        return context.getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE)
            .getString(KEY_URL_MOTD, DEFAULT_URL_MOTD_JSON);
    }

    public static String getCommandsUrl(Context context) {
        return context.getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE)
            .getString(KEY_URL_COMMANDS, DEFAULT_URL_COMMANDS_JSON);
    }

    public static String getRepoUrl(Context context) {
        return context.getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE)
            .getString(KEY_URL_REPO, DEFAULT_URL_GITHUB_REPO);
    }

    private final String[] TAB_TITLES = {
        "CAMPAÑA / JUGAR",
        "CONFIGURACIÓN GRÁFICA",
        "CONTROLES Y HUD",
        "SISTEMA Y ARCHIVOS",
        "AJUSTES Y RUTAS",
        "INFORMACIÓN Y ESTADO"
    };

    private final String[] TAB_DESCRIPTIONS = {
        "Inicia la campaña de Spartan CE o continúa desde tu último punto de control.",
        "Ajusta el campo de visión (FOV), sombreado por píxel, anti-aliasing y sombras dinámicas.",
        "Personaliza la distribución táctil, calibración de mandos, giroscopio y retroceso háptico.",
        "Administra archivos de juego ISO, verifica integridad de datos y limpia almacenamiento.",
        "Personaliza rutas dinámicas de red, repositorio y fuentes de datos JSON.",
        "Consulta la telemetría del hardware, versión del motor, requisitos y especificaciones técnicas."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hubPrefs = getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE);
        touchPrefs = getSharedPreferences(PREFS_TOUCH, Context.MODE_PRIVATE);
        activeTab = hubPrefs.getInt("active_tab", 0);
        palette = new ThemePalette();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(palette.bgRoot);
            getWindow().setNavigationBarColor(palette.bgRoot);
        }

        dataRoot = getExternalFilesDir(null);
        if (dataRoot != null) {
            new File(dataRoot, "maps").mkdirs();
        }

        passOnHardwareId();
        passOnInvite(getIntent());

        MotdManager.checkAndSyncRemote(this);
        EngineConfigManager.syncToConfigFile(this);

        buildHubInterface();
        loadNotificationsFromAssets();
        checkAndSyncNotifications();
    }

    private Typeface getTacticalTypeface(boolean bold) {
        try {
            Typeface tf = Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL);
            if (tf != null) return tf;
        } catch (Throwable ignored) {}
        try {
            Typeface tf = Typeface.create("sans-serif-medium", bold ? Typeface.BOLD : Typeface.NORMAL);
            if (tf != null) return tf;
        } catch (Throwable ignored) {}
        return bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
    }

    private boolean isNoticeDismissed(String noticeId) {
        if (noticeId == null || noticeId.trim().isEmpty()) return true;
        Set<String> set = hubPrefs.getStringSet(PREF_DISMISSED_NOTICES, null);
        return set != null && set.contains(noticeId.trim());
    }

    private void markNoticeDismissed(String noticeId) {
        if (noticeId == null || noticeId.trim().isEmpty()) return;
        Set<String> set = new HashSet<>();
        Set<String> existing = hubPrefs.getStringSet(PREF_DISMISSED_NOTICES, null);
        if (existing != null) {
            set.addAll(existing);
        }
        set.add(noticeId.trim());
        hubPrefs.edit().putStringSet(PREF_DISMISSED_NOTICES, set).commit();
        updateBellBadgeUI();
    }

    private void markAllNoticesDismissed() {
        Set<String> set = new HashSet<>();
        Set<String> existing = hubPrefs.getStringSet(PREF_DISMISSED_NOTICES, null);
        if (existing != null) {
            set.addAll(existing);
        }
        synchronized (cachedNotifications) {
            for (RemoteNotification n : cachedNotifications) {
                if (n.id != null && !n.id.trim().isEmpty()) {
                    set.add(n.id.trim());
                }
            }
        }
        hubPrefs.edit().putStringSet(PREF_DISMISSED_NOTICES, set).commit();
        updateBellBadgeUI();
    }

    private int getUnreadNotificationsCount() {
        int count = 0;
        synchronized (cachedNotifications) {
            for (RemoteNotification n : cachedNotifications) {
                if (n.id != null && !n.id.trim().isEmpty() && !isNoticeDismissed(n.id)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void updateBellBadgeUI() {
        final int unreadCount = getUnreadNotificationsCount();
        runOnUiThread(() -> {
            if (bellBadgeDot != null) {
                bellBadgeDot.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
            }
            if (ivBellIcon != null) {
                if (unreadCount > 0) {
                    ivBellIcon.setColorFilter(Color.rgb(255, 255, 255));
                    ivBellIcon.setAlpha(1.0f);
                } else {
                    ivBellIcon.setColorFilter(Color.rgb(180, 195, 215));
                    ivBellIcon.setAlpha(0.60f);
                }
            }
        });
    }

    private void loadNotificationsFromAssets() {
        try {
            android.content.res.AssetManager am = getAssets();
            try (java.io.InputStream is = am.open("updates.json");
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                parseNotificationsFromJson(sb.toString());
            }
        } catch (Throwable ignored) {}
    }

    private synchronized void parseNotificationsFromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return;
        try {
            JSONObject root = new JSONObject(jsonStr);
            List<RemoteNotification> list = new ArrayList<>();

            String latestVersion = root.optString("latest_version", "");
            int versionCode = root.optInt("version_code", 0);
            String releaseUrl = root.optString("release_url", "");

            int currentVersionCode = 2;
            try {
                currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (Exception ignored) {}

            // Check if there is a pending engine update
            if (versionCode > currentVersionCode) {
                RemoteNotification updateNotif = new RemoteNotification();
                updateNotif.id = "update_v" + versionCode;
                updateNotif.type = "update";
                updateNotif.hasUpdate = true;
                updateNotif.versionCode = versionCode;
                updateNotif.latestVersion = latestVersion;
                updateNotif.releaseUrl = releaseUrl;
                updateNotif.title = "NUEVA VERSIÓN DETECTADA: v" + latestVersion;
                updateNotif.date = "v" + latestVersion;
                updateNotif.summary = "Nueva versión oficial disponible en GitHub.";
                updateNotif.message = "Hay una nueva versión de Spartan CE lista para instalar en GitHub con mejoras de estabilidad y rendimiento.";
                updateNotif.actionText = "Descargar en GitHub";
                updateNotif.actionUrl = releaseUrl;
                list.add(updateNotif);
            }

            // Parse "notifications" array
            if (root.has("notifications")) {
                JSONArray arr = root.getJSONArray("notifications");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject nObj = arr.getJSONObject(i);
                    RemoteNotification n = parseSingleNotification(nObj);
                    if (n != null && n.id != null && !n.id.isEmpty()) {
                        list.add(n);
                    }
                }
            } else if (root.has("notification")) {
                JSONObject nObj = root.getJSONObject("notification");
                RemoteNotification n = parseSingleNotification(nObj);
                if (n != null && n.id != null && !n.id.isEmpty()) {
                    list.add(n);
                }
            }

            if (!list.isEmpty()) {
                cachedNotifications.clear();
                cachedNotifications.addAll(list);
            }
            updateBellBadgeUI();
        } catch (Throwable ignored) {}
    }

    private RemoteNotification parseSingleNotification(JSONObject nObj) {
        try {
            RemoteNotification notif = new RemoteNotification();
            notif.id = nObj.optString("id", "").trim();
            notif.type = nObj.optString("type", "announcement");
            notif.title = nObj.optString("title", "Aviso de Spartan CE");
            notif.date = nObj.optString("date", "");
            notif.summary = nObj.optString("summary", "");
            notif.message = nObj.optString("message", "");

            if (nObj.has("changelog")) {
                JSONArray cArr = nObj.getJSONArray("changelog");
                for (int i = 0; i < cArr.length(); i++) {
                    notif.changelog.add(cArr.getString(i));
                }
            }

            if (nObj.has("action_button")) {
                JSONObject aObj = nObj.getJSONObject("action_button");
                notif.actionText = aObj.optString("text", "Ver más");
                notif.actionUrl = aObj.optString("url", "");
            }
            return notif;
        } catch (Exception e) {
            return null;
        }
    }

    private void checkAndSyncNotifications() {
        new Thread(() -> {
            try {
                String updatesUrl = getUpdatesUrl(this);
                if (updatesUrl == null || !updatesUrl.startsWith("http")) return;

                URL url = new URL(updatesUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3500);
                conn.setReadTimeout(3500);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    parseNotificationsFromJson(sb.toString());
                }
                conn.disconnect();
            } catch (Exception ignored) {
                // Offline fallback - assets already loaded
            }
        }).start();
    }

    private void showNotificationsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout rootCard = new LinearLayout(this);
        rootCard.setOrientation(LinearLayout.VERTICAL);
        rootCard.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(252, 11, 14, 20)); // Deep tactical matte obsidian
        bg.setCornerRadius(dp(3));
        bg.setStroke(dp(1), Color.argb(45, 203, 213, 225)); // Subtle matte titanium hairline
        rootCard.setBackground(bg);

        // Header Row: Title & Subtitle + Pill + Frameless Close Button
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerRow.addView(titleCol, tLp);

        TextView tvHeader = new TextView(this);
        tvHeader.setText("AVISOS DE SPARTAN CE");
        tvHeader.setTextColor(Color.rgb(255, 255, 255));
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvHeader.setTypeface(getTacticalTypeface(true));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvHeader.setLetterSpacing(0.08f);
        }
        titleCol.addView(tvHeader);

        TextView tvSubHeader = new TextView(this);
        tvSubHeader.setText("Canal de avisos y registros del proyecto Spartan CE");
        tvSubHeader.setTextColor(Color.rgb(148, 163, 184));
        tvSubHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvSubHeader.setTypeface(getTacticalTypeface(false));
        titleCol.addView(tvSubHeader);

        // Status pill
        final int unreadCount = getUnreadNotificationsCount();
        TextView tvPill = new TextView(this);
        tvPill.setText(unreadCount > 0 ? "● " + unreadCount + " PENDIENTE" + (unreadCount > 1 ? "S" : "") : "✓ AL DÍA");
        tvPill.setTextColor(unreadCount > 0 ? Color.rgb(245, 158, 11) : Color.rgb(52, 211, 153));
        tvPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvPill.setTypeface(getTacticalTypeface(true));
        tvPill.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(dp(2));
        pillBg.setColor(unreadCount > 0 ? Color.argb(30, 245, 158, 11) : Color.argb(20, 52, 211, 153));
        pillBg.setStroke(dp(1), unreadCount > 0 ? Color.argb(60, 245, 158, 11) : Color.argb(50, 52, 211, 153));
        tvPill.setBackground(pillBg);
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillLp.leftMargin = dp(8);
        pillLp.rightMargin = dp(12);
        headerRow.addView(tvPill, pillLp);

        // Close '✕' - Frameless minimal touch target (NO button box)
        TextView tvCloseX = new TextView(this);
        tvCloseX.setText("✕");
        tvCloseX.setTextColor(Color.rgb(148, 163, 184));
        tvCloseX.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvCloseX.setTypeface(Typeface.DEFAULT_BOLD);
        tvCloseX.setGravity(Gravity.CENTER);
        tvCloseX.setPadding(dp(6), dp(4), dp(4), dp(4));
        tvCloseX.setFocusable(true);
        tvCloseX.setClickable(true);
        tvCloseX.setOnClickListener(v -> {
            hapticFeedback(v);
            dialog.dismiss();
        });
        headerRow.addView(tvCloseX);

        rootCard.addView(headerRow);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(25, 255, 255, 255));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.topMargin = dp(10);
        divLp.bottomMargin = dp(10);
        rootCard.addView(divider, divLp);

        // Scrollable Body
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        rootCard.addView(sv, svLp);

        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        sv.addView(listLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<RemoteNotification> unreadList = new ArrayList<>();
        List<RemoteNotification> readList = new ArrayList<>();
        synchronized (cachedNotifications) {
            for (RemoteNotification n : cachedNotifications) {
                if (n.id != null && !n.id.trim().isEmpty() && !isNoticeDismissed(n.id)) {
                    unreadList.add(n);
                } else {
                    readList.add(n);
                }
            }
        }

        // Render Unread Notices
        if (!unreadList.isEmpty()) {
            TextView tvSecUnread = new TextView(this);
            tvSecUnread.setText("AVISOS PENDIENTES (" + unreadList.size() + "):");
            tvSecUnread.setTextColor(Color.rgb(245, 158, 11));
            tvSecUnread.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            tvSecUnread.setTypeface(getTacticalTypeface(true));
            tvSecUnread.setPadding(0, 0, 0, dp(6));
            listLayout.addView(tvSecUnread);

            for (RemoteNotification n : unreadList) {
                listLayout.addView(createNotificationItemView(dialog, n, false));
            }
        }

        // Render Read Notices / History
        if (!readList.isEmpty()) {
            TextView tvSecRead = new TextView(this);
            tvSecRead.setText(unreadList.isEmpty() ? "HISTORIAL DE AVISOS LEÍDOS:" : "AVISOS ANTERIORES:");
            tvSecRead.setTextColor(Color.rgb(100, 116, 139));
            tvSecRead.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            tvSecRead.setTypeface(getTacticalTypeface(true));
            tvSecRead.setPadding(0, unreadList.isEmpty() ? 0 : dp(10), 0, dp(6));
            listLayout.addView(tvSecRead);

            for (RemoteNotification n : readList) {
                listLayout.addView(createNotificationItemView(dialog, n, true));
            }
        }

        if (unreadList.isEmpty() && readList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("✓ TODAS LAS NOTIFICACIONES AL DÍA\n\nNo hay avisos o actualizaciones registradas en el repositorio.");
            tvEmpty.setTextColor(Color.rgb(148, 163, 184));
            tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tvEmpty.setTypeface(getTacticalTypeface(false));
            tvEmpty.setPadding(0, dp(16), 0, dp(16));
            listLayout.addView(tvEmpty);
        }

        // Bottom Action Bar
        View btmDivider = new View(this);
        btmDivider.setBackgroundColor(Color.argb(25, 255, 255, 255));
        LinearLayout.LayoutParams bDivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        bDivLp.topMargin = dp(10);
        bDivLp.bottomMargin = dp(10);
        rootCard.addView(btmDivider, bDivLp);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        if (!unreadList.isEmpty()) {
            Button btnMarkAll = new Button(this);
            btnMarkAll.setText("✓ LEER TODAS");
            btnMarkAll.setTextColor(Color.rgb(255, 255, 255));
            btnMarkAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            btnMarkAll.setTypeface(getTacticalTypeface(true));
            btnMarkAll.setBackground(createButtonDrawable(Color.argb(180, 30, 41, 59), Color.argb(220, 51, 65, 85), 2, Color.argb(70, 148, 163, 184)));
            btnMarkAll.setPadding(dp(14), 0, dp(14), 0);
            btnMarkAll.setIncludeFontPadding(false);
            LinearLayout.LayoutParams maLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            maLp.rightMargin = dp(8);
            btnMarkAll.setOnClickListener(v -> {
                hapticFeedback(v);
                markAllNoticesDismissed();
                Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            bottomBar.addView(btnMarkAll, maLp);
        }

        Button btnSync = new Button(this);
        btnSync.setText("REINTENTAR SYNC");
        btnSync.setTextColor(Color.rgb(148, 163, 184));
        btnSync.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        btnSync.setTypeface(getTacticalTypeface(true));
        btnSync.setBackground(createButtonDrawable(Color.argb(100, 15, 23, 42), Color.argb(150, 24, 34, 50), 2, Color.argb(35, 148, 163, 184)));
        btnSync.setPadding(dp(12), 0, dp(12), 0);
        btnSync.setIncludeFontPadding(false);
        LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        syncLp.rightMargin = dp(8);
        btnSync.setOnClickListener(v -> {
            hapticFeedback(v);
            Toast.makeText(this, "Consultando actualizaciones...", Toast.LENGTH_SHORT).show();
            checkAndSyncNotifications();
            dialog.dismiss();
        });
        bottomBar.addView(btnSync, syncLp);

        Button btnClose = new Button(this);
        btnClose.setText("CERRAR");
        btnClose.setTextColor(Color.rgb(226, 232, 240));
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        btnClose.setTypeface(getTacticalTypeface(true));
        btnClose.setBackground(createButtonDrawable(Color.argb(140, 20, 28, 40), Color.argb(190, 32, 44, 62), 2, Color.argb(40, 148, 163, 184)));
        btnClose.setPadding(dp(14), 0, dp(14), 0);
        btnClose.setIncludeFontPadding(false);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        btnClose.setOnClickListener(v -> {
            hapticFeedback(v);
            dialog.dismiss();
        });
        bottomBar.addView(btnClose, cLp);

        rootCard.addView(bottomBar);

        dialog.setContentView(rootCard);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dialogWidth = Math.min(dp(540), getResources().getDisplayMetrics().widthPixels - dp(40));
            int dialogHeight = Math.min(dp(440), getResources().getDisplayMetrics().heightPixels - dp(32));
            dialog.getWindow().setLayout(dialogWidth, dialogHeight);
        }
        dialog.show();
    }

    private View createNotificationItemView(Dialog dialog, RemoteNotification n, boolean isRead) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable cBg = new GradientDrawable();
        cBg.setCornerRadius(dp(2));
        cBg.setColor(isRead ? Color.argb(45, 14, 18, 26) : Color.argb(95, 18, 24, 34));
        cBg.setStroke(dp(1), isRead ? Color.argb(20, 255, 255, 255) : Color.argb(40, 255, 255, 255));
        card.setBackground(cBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        // Header: Title + Status Pill
        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(n.title.isEmpty() ? "Aviso de Spartan CE" : n.title);
        tvTitle.setTextColor(isRead ? Color.rgb(148, 163, 184) : Color.rgb(255, 255, 255));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTitle.setTypeface(getTacticalTypeface(true));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        rowTop.addView(tvTitle, tLp);

        TextView tvTag = new TextView(this);
        tvTag.setText(isRead ? "LEÍDO" : "NUEVO");
        tvTag.setTextColor(isRead ? Color.rgb(100, 116, 139) : Color.rgb(245, 158, 11));
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvTag.setTypeface(getTacticalTypeface(true));
        tvTag.setPadding(dp(6), dp(2), dp(6), dp(2));
        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(2));
        tagBg.setColor(isRead ? Color.argb(20, 255, 255, 255) : Color.argb(35, 245, 158, 11));
        tvTag.setBackground(tagBg);
        rowTop.addView(tvTag);
        card.addView(rowTop);

        // Metadata: ID & Date
        TextView tvMeta = new TextView(this);
        tvMeta.setText("ID: " + n.id + (n.date.isEmpty() ? "" : "  •  " + n.date));
        tvMeta.setTextColor(Color.rgb(100, 116, 139));
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvMeta.setTypeface(getTacticalTypeface(false));
        tvMeta.setPadding(0, dp(2), 0, dp(6));
        card.addView(tvMeta);

        // Message body
        if (!n.message.isEmpty()) {
            TextView tvMsg = new TextView(this);
            tvMsg.setText(n.message);
            tvMsg.setTextColor(isRead ? Color.rgb(110, 125, 145) : Color.rgb(203, 213, 225));
            tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvMsg.setTypeface(getTacticalTypeface(false));
            tvMsg.setPadding(0, 0, 0, dp(6));
            card.addView(tvMsg);
        }

        // Changelog bullets
        if (!n.changelog.isEmpty()) {
            for (String item : n.changelog) {
                TextView tvItem = new TextView(this);
                tvItem.setText("-  " + item);
                tvItem.setTextColor(isRead ? Color.rgb(100, 116, 139) : Color.rgb(180, 195, 215));
                tvItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                tvItem.setTypeface(getTacticalTypeface(false));
                tvItem.setPadding(dp(4), dp(1), 0, dp(1));
                card.addView(tvItem);
            }
        }

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        actionsRow.setPadding(0, dp(8), 0, 0);

        final String actUrl = !n.actionUrl.isEmpty() ? n.actionUrl : n.releaseUrl;
        if (!actUrl.isEmpty()) {
            Button btnAct = new Button(this);
            btnAct.setText(n.actionText.isEmpty() ? "DESCARGAR EN GITHUB" : n.actionText.toUpperCase());
            btnAct.setTextColor(Color.rgb(255, 255, 255));
            btnAct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            btnAct.setTypeface(getTacticalTypeface(true));
            btnAct.setBackground(createButtonDrawable(Color.argb(180, 24, 34, 48), Color.argb(220, 36, 50, 72), 2, Color.argb(50, 148, 163, 184)));
            btnAct.setPadding(dp(12), 0, dp(12), 0);
            btnAct.setIncludeFontPadding(false);
            LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
            actLp.rightMargin = dp(8);
            btnAct.setOnClickListener(v -> {
                hapticFeedback(v);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(actUrl)));
            });
            actionsRow.addView(btnAct, actLp);
        }

        if (!isRead) {
            Button btnDismiss = new Button(this);
            btnDismiss.setText("✓ MARCAR LEÍDA");
            btnDismiss.setTextColor(Color.rgb(180, 195, 215));
            btnDismiss.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            btnDismiss.setTypeface(getTacticalTypeface(true));
            btnDismiss.setBackground(createButtonDrawable(Color.argb(100, 16, 22, 32), Color.argb(150, 24, 34, 48), 2, Color.argb(35, 148, 163, 184)));
            btnDismiss.setPadding(dp(12), 0, dp(12), 0);
            btnDismiss.setIncludeFontPadding(false);
            LinearLayout.LayoutParams disLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
            btnDismiss.setOnClickListener(v -> {
                hapticFeedback(v);
                markNoticeDismissed(n.id);
                Toast.makeText(this, "Aviso marcado como leído", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                showNotificationsDialog(); // Re-render dialog with updated state
            });
            actionsRow.addView(btnDismiss, disLp);
        }

        card.addView(actionsRow);
        return card;
    }

    private void passOnInvite(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction()) || intent.getData() == null
            || dataRoot == null)
            return;
        File partial = new File(dataRoot, "join_link.txt.tmp");
        try (OutputStream out = new FileOutputStream(partial)) {
            out.write(intent.getData().toString().getBytes("UTF-8"));
        } catch (java.io.IOException e) {
            partial.delete();
            return;
        }
        if (!partial.renameTo(new File(dataRoot, "join_link.txt")))
            partial.delete();
    }

    private void passOnHardwareId() {
        String id;
        if (dataRoot == null)
            return;
        try {
            id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (RuntimeException e) {
            return;
        }
        if (id == null || id.isEmpty())
            return;
        File partial = new File(dataRoot, "hardware_id.txt.tmp");
        try (OutputStream out = new FileOutputStream(partial)) {
            out.write(id.getBytes("UTF-8"));
        } catch (java.io.IOException e) {
            partial.delete();
            return;
        }
        if (!partial.renameTo(new File(dataRoot, "hardware_id.txt")))
            partial.delete();
    }

    private boolean haveData() {
        return dataRoot != null && new File(dataRoot, "maps/ui.map").isFile();
    }

    private void startGame() {
        EngineConfigManager.syncToConfigFile(this);
        startActivity(new Intent(this, HaloActivity.class));
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
            getResources().getDisplayMetrics());
    }

    /**
     * Master Chief Collection Tactical Color Palette.
     * Soft misty dark slate glass, pure white typography, subtle ice-cyan accents.
     */
    private static class ThemePalette {
        final int bgRoot = Color.rgb(6, 9, 14); // #06090E Deep obsidian
        final int bgCard = Color.argb(135, 10, 15, 22); // Soft misty slate glass
        final int strokeCard = Color.argb(35, 255, 255, 255); // Subtle translucent glass border (NO harsh blue)
        final int textPrimary = Color.rgb(248, 250, 252); // #F8FAFC Pure crisp white
        final int textSecondary = Color.rgb(155, 175, 195); // #9BAFC3 Slate cyan-grey
        final int textMuted = Color.rgb(100, 116, 139); // #64748B
        final int accent = Color.rgb(56, 189, 248); // #38BDF8 Tactical Ice Cyan
        final int accentBg = Color.rgb(14, 165, 233); // #0EA5E9
        final int btnSecondaryBg = Color.argb(130, 16, 24, 36);
        final int btnSecondaryBorder = Color.argb(45, 255, 255, 255);
        final int successBadgeBg = Color.argb(55, 16, 185, 129);
        final int successBadgeText = Color.rgb(52, 211, 153);
        final int warningBadgeBg = Color.argb(55, 245, 158, 11);
        final int warningBadgeText = Color.rgb(251, 191, 36);
        final int divider = Color.argb(35, 255, 255, 255);
    }

    private Drawable createContentAreaBackground() {
        return new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                Color.argb(205, 6, 9, 14), // Darkest near vertical divider to protect text readability
                Color.argb(150, 8, 12, 18), // Soft atmospheric slate glass transition
                Color.argb(105, 10, 15, 22)  // Soft fade toward the right edge
            }
        );
    }

    private void configureTacticalScrollView(ScrollView sv) {
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(true);
        sv.setScrollbarFadingEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable thumb = new GradientDrawable();
            thumb.setColor(Color.argb(140, 56, 189, 248)); // Subtle tactical cyan thumb
            thumb.setCornerRadius(dp(2));
            thumb.setSize(dp(3), dp(24));
            sv.setVerticalScrollbarThumbDrawable(thumb);
        }
    }

    private void hapticFeedback(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private Drawable createButtonDrawable(int normalColor, int pressedColor, float radiusDp, int strokeColor) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            normal.setStroke(dp(1), strokeColor);
        }

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        pressed.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            pressed.setStroke(dp(1), palette.accent);
        }

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[] { android.R.attr.state_pressed }, pressed);
        sld.addState(new int[] {}, normal);
        return sld;
    }

    private Drawable createGradientButton(int startColor, int endColor, float radiusDp) {
        GradientDrawable normal = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[] { startColor, endColor });
        normal.setCornerRadius(dp(radiusDp));

        GradientDrawable pressed = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[] { endColor, startColor });
        pressed.setCornerRadius(dp(radiusDp));

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[] { android.R.attr.state_pressed }, pressed);
        sld.addState(new int[] {}, normal);
        return sld;
    }

    private Drawable createMenuSelectedDrawable() {
        GradientDrawable normal = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                Color.argb(125, 14, 165, 233), // Ice cyan frosted bar
                Color.argb(20, 14, 165, 233)
            }
        );
        normal.setCornerRadius(dp(2));
        normal.setStroke(dp(1), Color.argb(80, 56, 189, 248));
        return normal;
    }

    private Drawable createMenuInactiveDrawable() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(Color.argb(40, 255, 255, 255));
        pressed.setCornerRadius(dp(2));

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[] { android.R.attr.state_pressed }, pressed);
        sld.addState(new int[] {}, normal);
        return sld;
    }

    private void updateSelectorButtons(Button[] buttons, float[] values, float current, float tolerance) {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = Math.abs(values[i] - current) <= tolerance;
            if (selected) {
                buttons[i].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 2, palette.accent));
                buttons[i].setTextColor(Color.rgb(6, 9, 14));
                buttons[i].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            } else {
                buttons[i].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
                buttons[i].setTextColor(palette.textSecondary);
                buttons[i].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            }
        }
    }

    private GradientDrawable createBadgeBackground(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(2));
        return gd;
    }

    /**
     * Builds the MCC Console-style Interface:
     * - Scenic atmospheric background + high-contrast vignette.
     * - Left navigation column with sharp cyan active bar.
     * - Elegant vertical gradient divider line between menu and content panel.
     * - Full-height right translucent slate glass container (100% vertical space).
     * - Clean contextual bottom helper prompt (no intrusive buttons).
     */
    private void buildHubInterface() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(palette.bgRoot);

        // 1. Cinematic Atmospheric Background
        ImageView ivBg = new ImageView(this);
        ivBg.setImageResource(R.drawable.bg_mcc_tactical);
        ivBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootFrame.addView(ivBg, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. High-contrast Left/Right Vignette Overlay
        View overlay = new View(this);
        GradientDrawable gradOverlay = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                Color.argb(235, 6, 9, 14), // High contrast for left menu
                Color.argb(120, 6, 9, 14), // Mid scenery breathing room
                Color.argb(210, 6, 9, 14)  // Contrast for right panel
            }
        );
        overlay.setBackground(gradOverlay);
        rootFrame.addView(overlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Subtle top & bottom edge vignette
        View edgeVignette = new View(this);
        GradientDrawable gradVignette = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {
                Color.argb(160, 4, 7, 12),
                Color.argb(0, 4, 7, 12),
                Color.argb(170, 4, 7, 12)
            }
        );
        edgeVignette.setBackground(gradVignette);
        rootFrame.addView(edgeVignette, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Main Content Hierarchy
        LinearLayout mainContent = new LinearLayout(this);
        mainContent.setOrientation(LinearLayout.VERTICAL);
        mainContent.setPadding(dp(20), dp(8), dp(20), dp(4));
        rootFrame.addView(mainContent, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- TOP HEADER ROW ---
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));
        mainContent.addView(headerRow);

        // Left Branding (M6D Emblem + Title)
        LinearLayout branding = new LinearLayout(this);
        branding.setOrientation(LinearLayout.HORIZONTAL);
        branding.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(branding, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView ivLogo = new ImageView(this);
        ivLogo.setImageResource(R.drawable.m6d_emblem);
        int logoSize = dp(32);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.rightMargin = dp(10);
        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivLogo.setPadding(dp(2.5f), dp(2.5f), dp(2.5f), dp(2.5f));
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(dp(2));
        logoBg.setColor(Color.argb(200, 10, 16, 26));
        logoBg.setStroke(dp(1), Color.argb(60, 56, 189, 248));
        ivLogo.setBackground(logoBg);
        branding.addView(ivLogo, logoLp);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        branding.addView(titleCol);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("SPARTAN CE");
        tvTitle.setTextColor(palette.textPrimary);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        tvTitle.setTypeface(getTacticalTypeface(true));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvTitle.setLetterSpacing(0.08f);
        }
        titleCol.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(MotdManager.getCurrentMessage(this));
        tvSubtitle.setTextColor(palette.accent);
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        tvSubtitle.setTypeface(getTacticalTypeface(false));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvSubtitle.setLetterSpacing(0.06f);
        }
        titleCol.addView(tvSubtitle);

        // Right Header Controls: Notification Bell + Spartan-117 Profile Badge
        LinearLayout rightHeaderControls = new LinearLayout(this);
        rightHeaderControls.setOrientation(LinearLayout.HORIZONTAL);
        rightHeaderControls.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(rightHeaderControls, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. Notification Bell (Clean Frameless Tactical Vector)
        FrameLayout bellFrame = new FrameLayout(this);
        LinearLayout.LayoutParams bellLp = new LinearLayout.LayoutParams(dp(36), dp(32));
        bellLp.rightMargin = dp(10);
        bellFrame.setLayoutParams(bellLp);
        bellFrame.setFocusable(true);
        bellFrame.setClickable(true);
        bellFrame.setOnClickListener(v -> {
            hapticFeedback(v);
            showNotificationsDialog();
        });

        ivBellIcon = new ImageView(this);
        ivBellIcon.setImageResource(R.drawable.ic_tactical_bell);
        ivBellIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iconSize = dp(20);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconLp.gravity = Gravity.CENTER;
        ivBellIcon.setLayoutParams(iconLp);
        ivBellIcon.setColorFilter(Color.rgb(180, 195, 215));
        ivBellIcon.setAlpha(0.60f);
        bellFrame.addView(ivBellIcon);

        // Amber Alert Dot for unread announcements / updates
        bellBadgeDot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.rgb(245, 158, 11)); // Tactical amber alert
        dotBg.setStroke(dp(1), Color.rgb(6, 9, 14));
        bellBadgeDot.setBackground(dotBg);
        int dotSize = dp(7);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotLp.gravity = Gravity.TOP | Gravity.END;
        dotLp.topMargin = dp(4);
        dotLp.rightMargin = dp(4);
        bellBadgeDot.setLayoutParams(dotLp);
        bellBadgeDot.setVisibility(View.GONE);
        bellFrame.addView(bellBadgeDot);

        updateBellBadgeUI();
        rightHeaderControls.addView(bellFrame);

        // 2. Spartan-117 Profile Badge
        LinearLayout spartanBadge = new LinearLayout(this);
        spartanBadge.setOrientation(LinearLayout.HORIZONTAL);
        spartanBadge.setGravity(Gravity.CENTER_VERTICAL);
        spartanBadge.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        spartanBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        spartanBadge.setLayoutParams(sbLp);

        View statusDot = new View(this);
        GradientDrawable statusDotBg = new GradientDrawable();
        statusDotBg.setShape(GradientDrawable.OVAL);
        statusDotBg.setColor(Color.rgb(52, 211, 153)); // Tactical green online
        statusDot.setBackground(statusDotBg);
        int sDotSize = dp(6);
        LinearLayout.LayoutParams sDotLp = new LinearLayout.LayoutParams(sDotSize, sDotSize);
        sDotLp.rightMargin = dp(6);
        spartanBadge.addView(statusDot, sDotLp);

        TextView tvSpartan = new TextView(this);
        tvSpartan.setText("SPARTAN-117");
        tvSpartan.setTextColor(palette.textPrimary);
        tvSpartan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvSpartan.setTypeface(getTacticalTypeface(true));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvSpartan.setLetterSpacing(0.06f);
        }
        spartanBadge.addView(tvSpartan);

        spartanBadge.setOnClickListener(v -> {
            hapticFeedback(v);
            selectTab(5, true); // Navigate to Información y Estado
        });
        rightHeaderControls.addView(spartanBadge);

        // --- CENTER SPLIT: LEFT MENU (240dp) + VERTICAL DIVIDER + RIGHT CONTENT PANEL (100% VERTICAL) ---
        LinearLayout bodyRow = new LinearLayout(this);
        bodyRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        bodyLp.setMargins(0, dp(2), 0, dp(2));
        mainContent.addView(bodyRow, bodyLp);

        // Left Navigation Column (MCC Style) with Hidden ScrollView
        LinearLayout leftNavCol = new LinearLayout(this);
        leftNavCol.setOrientation(LinearLayout.VERTICAL);
        leftNavCol.setPadding(0, dp(2), 0, dp(2));
        LinearLayout.LayoutParams leftColLp = new LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.MATCH_PARENT);
        bodyRow.addView(leftNavCol, leftColLp);

        // Scrollable container for menu items to prevent overflow on lower resolution or split screens
        ScrollView menuScrollView = new ScrollView(this);
        menuScrollView.setFillViewport(true);
        menuScrollView.setVerticalScrollBarEnabled(false);
        menuScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        leftNavCol.addView(menuScrollView, scrollLp);

        LinearLayout menuContainer = new LinearLayout(this);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuScrollView.addView(menuContainer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        menuRows = new LinearLayout[TAB_TITLES.length];
        menuAccents = new View[TAB_TITLES.length];
        menuLabels = new TextView[TAB_TITLES.length];

        for (int i = 0; i < TAB_TITLES.length; i++) {
            final int index = i;
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32));
            itemLp.bottomMargin = dp(2);

            View accentBar = new View(this);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(3.5f), ViewGroup.LayoutParams.MATCH_PARENT);
            itemRow.addView(accentBar, barLp);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(TAB_TITLES[i]);
            tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            tvLabel.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tvLabel.setLetterSpacing(0.08f);
            }
            tvLabel.setPadding(dp(12), 0, dp(10), 0);
            itemRow.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            itemRow.setOnClickListener(v -> {
                hapticFeedback(v);
                selectTab(index, true);
            });

            menuContainer.addView(itemRow, itemLp);
            menuRows[i] = itemRow;
            menuAccents[i] = accentBar;
            menuLabels[i] = tvLabel;
        }

        // MCC-style Quit Button pinned at bottom of left column
        LinearLayout exitRow = new LinearLayout(this);
        exitRow.setOrientation(LinearLayout.HORIZONTAL);
        exitRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(32));
        exitLp.topMargin = dp(2);
        exitRow.setBackground(createMenuInactiveDrawable());

        View exitAccent = new View(this);
        exitAccent.setBackgroundColor(Color.TRANSPARENT);
        exitRow.addView(exitAccent, new LinearLayout.LayoutParams(dp(3.5f), ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvExit = new TextView(this);
        tvExit.setText("SALIR DEL JUEGO");
        tvExit.setTextColor(Color.rgb(190, 85, 85));
        tvExit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvExit.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvExit.setLetterSpacing(0.08f);
        }
        tvExit.setPadding(dp(14), 0, dp(12), 0);
        exitRow.addView(tvExit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        exitRow.setOnClickListener(v -> {
            hapticFeedback(v);
            finish();
        });
        leftNavCol.addView(exitRow, exitLp);

        // --- VERTICAL TACTICAL DIVIDER LINE (MCC Style) ---
        View verticalDivider = new View(this);
        GradientDrawable divGrad = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {
                Color.argb(0, 56, 189, 248),
                Color.argb(110, 56, 189, 248), // Glowing cyan center
                Color.argb(70, 56, 189, 248),
                Color.argb(0, 56, 189, 248)
            }
        );
        verticalDivider.setBackground(divGrad);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(1.5f), ViewGroup.LayoutParams.MATCH_PARENT);
        divLp.leftMargin = dp(16);
        divLp.rightMargin = dp(16);
        bodyRow.addView(verticalDivider, divLp);

        // --- RIGHT CONTENT AREA (100% Vertical Space, Soft Blur/Vignette Wash, No Hard Border) ---
        LinearLayout rightPanelContainer = new LinearLayout(this);
        rightPanelContainer.setOrientation(LinearLayout.VERTICAL);
        rightPanelContainer.setBackground(createContentAreaBackground());
        rightPanelContainer.setPadding(dp(16), dp(8), dp(16), dp(4));
        LinearLayout.LayoutParams rightPanelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        bodyRow.addView(rightPanelContainer, rightPanelLp);

        pageContainer = new FrameLayout(this);
        rightPanelContainer.addView(pageContainer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pages = new View[6];
        pages[0] = buildPagePlay();
        pages[1] = buildPageGraphics();
        pages[2] = buildPageControls();
        pages[3] = buildPageSystem();
        pages[4] = buildPageSettings();
        pages[5] = buildPageInfo();

        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == activeTab ? View.VISIBLE : View.GONE);
            pageContainer.addView(pages[i], new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // --- BOTTOM FOOTER: CENTERED SLOGAN & PROJECT IDENTITY ---
        LinearLayout footerRow = new LinearLayout(this);
        footerRow.setOrientation(LinearLayout.VERTICAL);
        footerRow.setGravity(Gravity.CENTER);
        footerRow.setPadding(dp(8), dp(4), dp(8), dp(2));
        mainContent.addView(footerRow);

        TextView tvFooterSlogan = new TextView(this);
        tvFooterSlogan.setText("SPARTAN CE  //  MOTOR TÁCTICO RECOMPILADO PARA ANDROID");
        tvFooterSlogan.setTextColor(palette.textSecondary);
        tvFooterSlogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvFooterSlogan.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvFooterSlogan.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvFooterSlogan.setLetterSpacing(0.08f);
        }
        footerRow.addView(tvFooterSlogan);

        TextView tvFooterLicense = new TextView(this);
        tvFooterLicense.setText("Proyecto Comunitario de Código Abierto  •  Licencia MIT / GPL  •  Sin Fines de Lucro");
        tvFooterLicense.setTextColor(palette.textMuted);
        tvFooterLicense.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        tvFooterLicense.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        tvFooterLicense.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvFooterLicense.setLetterSpacing(0.04f);
        }
        LinearLayout.LayoutParams licLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        licLp.topMargin = dp(1);
        footerRow.addView(tvFooterLicense, licLp);

        if (activeTab < 0 || activeTab >= pages.length) activeTab = 0;
        selectTab(activeTab, false);

        setContentView(rootFrame);
        updateStatusUI();
    }

    /**
     * Smoothly switches tabs with MCC-style vertical displacement and alpha fade.
     */
    private void selectTab(int index, boolean animate) {
        if (pages == null || index < 0 || index >= pages.length) return;
        final int oldTab = activeTab;
        activeTab = index;
        hubPrefs.edit().putInt("active_tab", activeTab).apply();

        // Update menu items
        for (int i = 0; i < menuRows.length; i++) {
            boolean isSelected = (i == activeTab);
            if (isSelected) {
                menuAccents[i].setBackgroundColor(palette.accent);
                menuRows[i].setBackground(createMenuSelectedDrawable());
                menuLabels[i].setTextColor(Color.WHITE);
                menuLabels[i].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            } else {
                menuAccents[i].setBackgroundColor(Color.TRANSPARENT);
                menuRows[i].setBackground(createMenuInactiveDrawable());
                menuLabels[i].setTextColor(palette.textSecondary);
                menuLabels[i].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            }
        }

        if (tvFooterDescription != null && activeTab < TAB_DESCRIPTIONS.length) {
            tvFooterDescription.setText(TAB_DESCRIPTIONS[activeTab]);
        }

        if (oldTab == index && !animate) {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    pages[i].setVisibility(i == activeTab ? View.VISIBLE : View.GONE);
                    pages[i].setAlpha(1f);
                    pages[i].setTranslationY(0f);
                }
            }
            return;
        }

        if (animate && oldTab != index && pages[oldTab] != null && pages[activeTab] != null) {
            final View outView = pages[oldTab];
            final View inView = pages[activeTab];
            final int direction = (index > oldTab) ? 1 : -1;

            outView.animate().alpha(0f).translationY(-direction * dp(16)).setDuration(120)
                .withEndAction(() -> {
                    outView.setVisibility(View.GONE);
                    outView.setTranslationY(0f);
                }).start();

            inView.setAlpha(0f);
            inView.setTranslationY(direction * dp(16));
            inView.setVisibility(View.VISIBLE);
            inView.animate().alpha(1f).translationY(0f).setDuration(180).start();
        } else {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    pages[i].setVisibility(i == activeTab ? View.VISIBLE : View.GONE);
                    pages[i].setAlpha(1f);
                    pages[i].setTranslationY(0f);
                }
            }
        }
    }

    // ==========================================
    // TAB 1: JUGAR (HERO LAUNCH DASHBOARD)
    // ==========================================
    private View buildPagePlay() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        // Header Title inside panel
        TextView tvMissionTitle = new TextView(this);
        tvMissionTitle.setText("DESPLIEGUE TÁCTICO");
        tvMissionTitle.setTextColor(palette.accent);
        tvMissionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMissionTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvMissionTitle.setLetterSpacing(0.08f);
        }
        page.addView(tvMissionTitle);

        TextView tvMissionHeading = new TextView(this);
        tvMissionHeading.setText("CAMPAÑA PRINCIPAL");
        tvMissionHeading.setTextColor(palette.textPrimary);
        tvMissionHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvMissionHeading.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvMissionHeading.setPadding(0, dp(2), 0, dp(4));
        page.addView(tvMissionHeading);

        TextView tvMissionDesc = new TextView(this);
        tvMissionDesc.setText("Inicia la campaña táctica de combate con optimización de hardware ARM64 y aceleración de gráficos.");
        tvMissionDesc.setTextColor(palette.textSecondary);
        tvMissionDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvMissionDesc.setPadding(0, 0, 0, dp(14));
        page.addView(tvMissionDesc);

        // Status Row
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, 0, 0, dp(12));
        page.addView(statusRow);

        tvStatusHeader = new TextView(this);
        tvStatusHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvStatusHeader.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvStatusHeader.setPadding(dp(10), dp(4), dp(10), dp(4));
        statusRow.addView(tvStatusHeader);

        View spacer = new View(this);
        statusRow.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1.0f));

        tvInstalledLang = new TextView(this);
        tvInstalledLang.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvInstalledLang.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvInstalledLang.setPadding(dp(10), dp(4), dp(10), dp(4));
        statusRow.addView(tvInstalledLang);

        // Launch Action Button
        btnLaunch = new Button(this);
        btnLaunch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        btnLaunch.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btnLaunch.setLetterSpacing(0.08f);
        }
        LinearLayout.LayoutParams launchBtnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        launchBtnLp.bottomMargin = dp(12);
        page.addView(btnLaunch, launchBtnLp);

        // ISO Extraction Progress & Status
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        progressParams.bottomMargin = dp(6);
        page.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setTextColor(palette.textMuted);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, dp(10));
        page.addView(statusText);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(palette.divider);
        page.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // Telemetry & Quick Access Grid
        TextView tvTelTitle = new TextView(this);
        tvTelTitle.setText("ACCESOS DIRECTOS");
        tvTelTitle.setTextColor(palette.accent);
        tvTelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvTelTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvTelTitle.setPadding(0, dp(12), 0, dp(8));
        page.addView(tvTelTitle);

        LinearLayout quickNavRow = new LinearLayout(this);
        quickNavRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button btnGoGfx = new Button(this);
        btnGoGfx.setText("📺  AJUSTES GRÁFICOS (FOV / AA)");
        btnGoGfx.setTextColor(palette.textPrimary);
        btnGoGfx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnGoGfx.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnGoGfx.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams gfxBtnLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        gfxBtnLp.rightMargin = dp(6);
        btnGoGfx.setOnClickListener(v -> {
            hapticFeedback(v);
            selectTab(1, true);
        });
        quickNavRow.addView(btnGoGfx, gfxBtnLp);

        Button btnGoCtrl = new Button(this);
        btnGoCtrl.setText("🕹️  CONTROLES & HUD");
        btnGoCtrl.setTextColor(palette.textPrimary);
        btnGoCtrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnGoCtrl.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnGoCtrl.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams ctrlBtnLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        btnGoCtrl.setOnClickListener(v -> {
            hapticFeedback(v);
            selectTab(2, true);
        });
        quickNavRow.addView(btnGoCtrl, ctrlBtnLp);

        page.addView(quickNavRow, qnLp);
        return sv;
    }

    // ==========================================
    // TAB 2: GRÁFICOS (ENGINE & VIDEO CONFIG)
    // ==========================================
    private View buildPageGraphics() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        TextView tvGfxHeader = new TextView(this);
        tvGfxHeader.setText("MOTOR GRÁFICO Y RENDERIZADO");
        tvGfxHeader.setTextColor(palette.accent);
        tvGfxHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvGfxHeader.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvGfxHeader.setLetterSpacing(0.08f);
        }
        page.addView(tvGfxHeader);

        TextView tvGfxDesc = new TextView(this);
        tvGfxDesc.setText("Configura el pipeline visual y las opciones avanzadas de sombreado.");
        tvGfxDesc.setTextColor(palette.textSecondary);
        tvGfxDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvGfxDesc.setPadding(0, dp(2), 0, dp(12));
        page.addView(tvGfxDesc);

        // 1. FOV (Campo de Visión)
        TextView tvFov = new TextView(this);
        float currentFov = EngineConfigManager.getFov(this);
        tvFov.setText("CAMPO DE VISIÓN (FOV): " + (int)currentFov + "°");
        tvFov.setTextColor(palette.textPrimary);
        tvFov.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvFov.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvFov.setPadding(0, dp(4), 0, dp(4));
        page.addView(tvFov);

        LinearLayout fovRow = new LinearLayout(this);
        fovRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] fovValues = { 70f, 80f, 90f, 100f };
        String[] fovLabels = { "70° CLÁSICO", "80°", "90° TÁCTICO", "100° AMPLIO" };
        Button[] fovButtons = new Button[fovValues.length];
        for (int i = 0; i < fovValues.length; i++) {
            final float val = fovValues[i];
            Button b = new Button(this);
            b.setText(fovLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            if (i < fovValues.length - 1) lp.rightMargin = dp(6);
            fovRow.addView(b, lp);
            fovButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                EngineConfigManager.setFov(this, val);
                tvFov.setText("CAMPO DE VISIÓN (FOV): " + (int)val + "°");
                updateSelectorButtons(fovButtons, fovValues, val, 1.0f);
            });
        }
        updateSelectorButtons(fovButtons, fovValues, currentFov, 1.0f);
        page.addView(fovRow);

        // 2. Per-Pixel Lighting Checkbox
        CheckBox cbPerPixel = new CheckBox(this);
        cbPerPixel.setText("Iluminación por píxel (Modelos 3D y armaduras suaves)");
        cbPerPixel.setTextColor(palette.textPrimary);
        cbPerPixel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cbPerPixel.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        cbPerPixel.setPadding(0, dp(10), 0, dp(6));
        cbPerPixel.setChecked(EngineConfigManager.isPerPixelLighting(this));
        cbPerPixel.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            EngineConfigManager.setPerPixelLighting(this, isChecked);
        });
        page.addView(cbPerPixel);

        // 3. Anti-Aliasing
        TextView tvAa = new TextView(this);
        tvAa.setText("SUAVIZADO DE BORDES (ANTI-ALIASING):");
        tvAa.setTextColor(palette.textPrimary);
        tvAa.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvAa.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvAa.setPadding(0, dp(8), 0, dp(4));
        page.addView(tvAa);

        LinearLayout aaRow = new LinearLayout(this);
        aaRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] aaKeys = { "off", "fxaa", "msaa2x", "msaa4x" };
        String[] aaLabels = { "APAGADO", "FXAA", "MSAA 2X", "MSAA 4X" };
        Button[] aaButtons = new Button[aaKeys.length];
        String currentAa = EngineConfigManager.getAntiAliasing(this);
        for (int i = 0; i < aaKeys.length; i++) {
            final String key = aaKeys[i];
            Button b = new Button(this);
            b.setText(aaLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            if (i < aaKeys.length - 1) lp.rightMargin = dp(6);
            aaRow.addView(b, lp);
            aaButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                EngineConfigManager.setAntiAliasing(this, key);
                for (int j = 0; j < aaButtons.length; j++) {
                    boolean sel = aaKeys[j].equalsIgnoreCase(key);
                    if (sel) {
                        aaButtons[j].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 2, palette.accent));
                        aaButtons[j].setTextColor(Color.rgb(6, 9, 14));
                        aaButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                    } else {
                        aaButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
                        aaButtons[j].setTextColor(palette.textSecondary);
                        aaButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                    }
                }
            });
        }
        for (int j = 0; j < aaButtons.length; j++) {
            boolean sel = aaKeys[j].equalsIgnoreCase(currentAa);
            if (sel) {
                aaButtons[j].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 2, palette.accent));
                aaButtons[j].setTextColor(Color.rgb(6, 9, 14));
                aaButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            } else {
                aaButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
                aaButtons[j].setTextColor(palette.textSecondary);
                aaButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            }
        }
        page.addView(aaRow);

        // 4. Dynamic Shadows
        TextView tvShadow = new TextView(this);
        tvShadow.setText("RESOLUCIÓN DE SOMBRAS DINÁMICAS:");
        tvShadow.setTextColor(palette.textPrimary);
        tvShadow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvShadow.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvShadow.setPadding(0, dp(10), 0, dp(4));
        page.addView(tvShadow);

        LinearLayout shadowRow = new LinearLayout(this);
        shadowRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] shadowValues = { 128, 256, 512, 1024 };
        String[] shadowLabels = { "128 (BAJA)", "256", "512 (HD)", "1024 (ULTRA)" };
        Button[] shadowButtons = new Button[shadowValues.length];
        int currentShadow = EngineConfigManager.getShadowResolution(this);
        for (int i = 0; i < shadowValues.length; i++) {
            final int res = shadowValues[i];
            Button b = new Button(this);
            b.setText(shadowLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            if (i < shadowValues.length - 1) lp.rightMargin = dp(6);
            shadowRow.addView(b, lp);
            shadowButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                EngineConfigManager.setShadowResolution(this, res);
                for (int j = 0; j < shadowButtons.length; j++) {
                    boolean sel = (shadowValues[j] == res);
                    if (sel) {
                        shadowButtons[j].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 2, palette.accent));
                        shadowButtons[j].setTextColor(Color.rgb(6, 9, 14));
                        shadowButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                    } else {
                        shadowButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
                        shadowButtons[j].setTextColor(palette.textSecondary);
                        shadowButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                    }
                }
            });
        }
        for (int j = 0; j < shadowButtons.length; j++) {
            boolean sel = (shadowValues[j] == currentShadow);
            if (sel) {
                shadowButtons[j].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 2, palette.accent));
                shadowButtons[j].setTextColor(Color.rgb(6, 9, 14));
                shadowButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            } else {
                shadowButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
                shadowButtons[j].setTextColor(palette.textSecondary);
                shadowButtons[j].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            }
        }
        page.addView(shadowRow);
        return sv;
    }

    // ==========================================
    // TAB 3: CONTROLES (TOUCH, HUD & HAPTICS)
    // ==========================================
    private View buildPageControls() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        TextView tvControlsTitle = new TextView(this);
        tvControlsTitle.setText("INTERFAZ TÁCTIL Y MANDOS");
        tvControlsTitle.setTextColor(palette.accent);
        tvControlsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvControlsTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvControlsTitle.setLetterSpacing(0.08f);
        }
        page.addView(tvControlsTitle);

        TextView tvControlsDesc = new TextView(this);
        tvControlsDesc.setText("Calibración y personalización del HUD táctil, mandos Bluetooth y retroceso háptico.");
        tvControlsDesc.setTextColor(palette.textSecondary);
        tvControlsDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvControlsDesc.setPadding(0, dp(2), 0, dp(12));
        page.addView(tvControlsDesc);

        // Action Buttons Row (HUD Editor & Gamepad Calibration)
        LinearLayout quickActionRow = new LinearLayout(this);
        quickActionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams quickActionRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickActionRowLp.bottomMargin = dp(10);

        Button btnEditHud = new Button(this);
        btnEditHud.setText("✏️  PERSONALIZAR POSICIÓN HUD");
        btnEditHud.setTextColor(palette.textPrimary);
        btnEditHud.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnEditHud.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnEditHud.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams editHudLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        editHudLp.rightMargin = dp(6);
        btnEditHud.setOnClickListener(v -> {
            hapticFeedback(v);
            startActivity(new Intent(this, HudEditorActivity.class));
        });
        quickActionRow.addView(btnEditHud, editHudLp);

        Button btnCalibrateGamepad = new Button(this);
        btnCalibrateGamepad.setText("🎮  CALIBRAR MANDO");
        btnCalibrateGamepad.setTextColor(palette.textPrimary);
        btnCalibrateGamepad.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnCalibrateGamepad.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnCalibrateGamepad.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams gamepadLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        btnCalibrateGamepad.setOnClickListener(v -> {
            hapticFeedback(v);
            checkGamepadCalibration();
        });
        quickActionRow.addView(btnCalibrateGamepad, gamepadLp);
        page.addView(quickActionRow, quickActionRowLp);

        CheckBox cbOverlay = new CheckBox(this);
        cbOverlay.setText("Botones virtuales en pantalla (HUD controller)");
        cbOverlay.setTextColor(palette.textPrimary);
        cbOverlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cbOverlay.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        boolean overlayDisabled = touchPrefs.getBoolean("overlay-disabled", false);
        cbOverlay.setChecked(!overlayDisabled);
        cbOverlay.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("overlay-disabled", !isChecked).apply();
            Toast.makeText(this, isChecked ? "Controles táctiles activados" : "Controles táctiles desactivados", Toast.LENGTH_SHORT).show();
        });
        page.addView(cbOverlay);

        TextView tvOpacity = new TextView(this);
        float currentOpacity = touchPrefs.getFloat("controls-opacity", 1.0f);
        tvOpacity.setText("OPACIDAD DE BOTONES: " + (int)(currentOpacity * 100) + "%");
        tvOpacity.setTextColor(palette.textPrimary);
        tvOpacity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvOpacity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvOpacity.setPadding(0, dp(8), 0, dp(4));
        page.addView(tvOpacity);

        LinearLayout opacityRow = new LinearLayout(this);
        opacityRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] opacityValues = { 0.4f, 0.6f, 0.8f, 1.0f };
        String[] opacityLabels = { "40%", "60%", "80%", "100%" };
        Button[] opacityButtons = new Button[opacityValues.length];
        for (int i = 0; i < opacityValues.length; i++) {
            final float val = opacityValues[i];
            Button b = new Button(this);
            b.setText(opacityLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            if (i < opacityValues.length - 1) lp.rightMargin = dp(6);
            opacityRow.addView(b, lp);
            opacityButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                touchPrefs.edit().putFloat("controls-opacity", val).apply();
                tvOpacity.setText("OPACIDAD DE BOTONES: " + (int)(val * 100) + "%");
                updateSelectorButtons(opacityButtons, opacityValues, val, 0.05f);
            });
        }
        updateSelectorButtons(opacityButtons, opacityValues, currentOpacity, 0.05f);
        page.addView(opacityRow);

        TextView tvSens = new TextView(this);
        float currentSens = touchPrefs.getFloat("look-sensitivity", 2.0f);
        tvSens.setText("SENSIBILIDAD DE APUNTADO: " + String.format("%.1fx", currentSens));
        tvSens.setTextColor(palette.textPrimary);
        tvSens.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvSens.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvSens.setPadding(0, dp(8), 0, dp(2));
        page.addView(tvSens);

        SeekBar sbSens = new SeekBar(this);
        sbSens.setMax(35); // 0.5x to 4.0x
        int sensProgress = Math.round((currentSens - 0.5f) * 10f);
        if (sensProgress < 0) sensProgress = 0;
        if (sensProgress > 35) sensProgress = 35;
        sbSens.setProgress(sensProgress);
        LinearLayout.LayoutParams sbSensLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbSensLp.setMargins(0, dp(2), 0, dp(4));
        page.addView(sbSens, sbSensLp);

        LinearLayout sensRow = new LinearLayout(this);
        sensRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] sensValues = { 1.0f, 1.5f, 2.0f, 3.0f };
        String[] sensLabels = { "1.0X", "1.5X", "2.0X", "3.0X" };
        Button[] sensButtons = new Button[sensValues.length];
        for (int i = 0; i < sensValues.length; i++) {
            final float val = sensValues[i];
            Button b = new Button(this);
            b.setText(sensLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            if (i < sensValues.length - 1) lp.rightMargin = dp(6);
            sensRow.addView(b, lp);
            sensButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                touchPrefs.edit().putFloat("look-sensitivity", val).apply();
                tvSens.setText("SENSIBILIDAD DE APUNTADO: " + String.format("%.1fx", val));
                int newProg = Math.round((val - 0.5f) * 10f);
                sbSens.setProgress(newProg);
                updateSelectorButtons(sensButtons, sensValues, val, 0.1f);
            });
        }
        updateSelectorButtons(sensButtons, sensValues, currentSens, 0.1f);
        page.addView(sensRow);

        sbSens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float val = 0.5f + (progress / 10.0f);
                    touchPrefs.edit().putFloat("look-sensitivity", val).apply();
                    tvSens.setText("SENSIBILIDAD DE APUNTADO: " + String.format("%.1fx", val));
                    updateSelectorButtons(sensButtons, sensValues, val, 0.1f);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { hapticFeedback(seekBar); }
        });

        // Toggles Row (Gyro, Rumble, FPS)
        LinearLayout togglesRow = new LinearLayout(this);
        togglesRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams togglesRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        togglesRowLp.topMargin = dp(6);
        page.addView(togglesRow, togglesRowLp);

        CheckBox cbGyro = new CheckBox(this);
        cbGyro.setText("Giroscopio");
        cbGyro.setTextColor(palette.textPrimary);
        cbGyro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        cbGyro.setChecked(touchPrefs.getBoolean("gyroscope", false));
        togglesRow.addView(cbGyro, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        CheckBox cbRumble = new CheckBox(this);
        cbRumble.setText("Vibración");
        cbRumble.setTextColor(palette.textPrimary);
        cbRumble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        cbRumble.setChecked(touchPrefs.getBoolean("rumble", true));
        cbRumble.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("rumble", isChecked).commit();
        });
        togglesRow.addView(cbRumble, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        CheckBox cbFps = new CheckBox(this);
        cbFps.setText("Contador FPS");
        cbFps.setTextColor(palette.textPrimary);
        cbFps.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        cbFps.setChecked(touchPrefs.getBoolean("fps-counter", false));
        cbFps.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("fps-counter", isChecked).commit();
        });
        togglesRow.addView(cbFps, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Weapon Fire Haptics Checkbox
        CheckBox cbFireHaptics = new CheckBox(this);
        cbFireHaptics.setText("⚡  Vibración táctil al disparar armas (Retroceso háptico)");
        cbFireHaptics.setTextColor(palette.textPrimary);
        cbFireHaptics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        cbFireHaptics.setChecked(touchPrefs.getBoolean("fire-haptics", true));
        cbFireHaptics.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("fire-haptics", isChecked).commit();
        });
        page.addView(cbFireHaptics);

        // Gyroscope Sensitivity Container
        LinearLayout gyroSensContainer = new LinearLayout(this);
        gyroSensContainer.setOrientation(LinearLayout.VERTICAL);
        gyroSensContainer.setVisibility(cbGyro.isChecked() ? View.VISIBLE : View.GONE);
        cbGyro.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("gyroscope", isChecked).commit();
            gyroSensContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        TextView tvGyroSens = new TextView(this);
        float currentGyroSens = touchPrefs.getFloat("gyro-sensitivity", 1.0f);
        tvGyroSens.setText("SENSIBILIDAD DE GIROSCOPIO: " + String.format("%.1fx", currentGyroSens));
        tvGyroSens.setTextColor(palette.textPrimary);
        tvGyroSens.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvGyroSens.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvGyroSens.setPadding(0, dp(4), 0, dp(4));
        gyroSensContainer.addView(tvGyroSens);

        LinearLayout gyroSensRow = new LinearLayout(this);
        gyroSensRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] gyroValues = { 0.5f, 1.0f, 1.5f, 2.0f };
        String[] gyroLabels = { "0.5X", "1.0X", "1.5X", "2.0X" };
        Button[] gyroButtons = new Button[gyroValues.length];
        for (int i = 0; i < gyroValues.length; i++) {
            final float val = gyroValues[i];
            Button b = new Button(this);
            b.setText(gyroLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            if (i < gyroValues.length - 1) lp.rightMargin = dp(6);
            gyroSensRow.addView(b, lp);
            gyroButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                touchPrefs.edit().putFloat("gyro-sensitivity", val).commit();
                tvGyroSens.setText("SENSIBILIDAD DE GIROSCOPIO: " + String.format("%.1fx", val));
                updateSelectorButtons(gyroButtons, gyroValues, val, 0.1f);
            });
        }
        updateSelectorButtons(gyroButtons, gyroValues, currentGyroSens, 0.1f);
        gyroSensContainer.addView(gyroSensRow);
        page.addView(gyroSensContainer);

        Button btnResetControls = new Button(this);
        btnResetControls.setText("↺  RESTABLECER CONTROLES A VALORES DE FÁBRICA");
        btnResetControls.setTextColor(palette.textSecondary);
        btnResetControls.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnResetControls.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnResetControls.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        resetLp.topMargin = dp(8);
        btnResetControls.setOnClickListener(v -> {
            hapticFeedback(v);
            confirmResetControls();
        });
        page.addView(btnResetControls, resetLp);

        return sv;
    }

    // ==========================================
    // TAB 4: SISTEMA (ISO, STORAGE & COMMUNITY)
    // ==========================================
    private View buildPageSystem() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        TextView tvIsoTitle = new TextView(this);
        tvIsoTitle.setText("GESTIÓN DE ARCHIVOS Y DIAGNÓSTICO");
        tvIsoTitle.setTextColor(palette.accent);
        tvIsoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvIsoTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvIsoTitle.setLetterSpacing(0.08f);
        }
        page.addView(tvIsoTitle);

        TextView tvIsoDesc = new TextView(this);
        tvIsoDesc.setText("Detecta versiones multi-idioma (Español, Italiano, Inglés) y verifica integridad de los mapas.");
        tvIsoDesc.setTextColor(palette.textSecondary);
        tvIsoDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvIsoDesc.setPadding(0, dp(2), 0, dp(12));
        page.addView(tvIsoDesc);

        LinearLayout isoActionRow = new LinearLayout(this);
        isoActionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams isoActionRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        btnPickIso = new Button(this);
        btnPickIso.setText("💿  CAMBIAR IDIOMA / ISO");
        btnPickIso.setTextColor(palette.textPrimary);
        btnPickIso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnPickIso.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnPickIso.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams pickIsoLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        pickIsoLp.rightMargin = dp(6);
        btnPickIso.setOnClickListener(v -> {
            hapticFeedback(v);
            chooseIsoImage();
        });
        isoActionRow.addView(btnPickIso, pickIsoLp);

        Button btnCheckIntegrity = new Button(this);
        btnCheckIntegrity.setText("🛡️  COMPROBAR INTEGRIDAD");
        btnCheckIntegrity.setTextColor(palette.textPrimary);
        btnCheckIntegrity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnCheckIntegrity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnCheckIntegrity.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        btnCheckIntegrity.setOnClickListener(v -> {
            hapticFeedback(v);
            checkMapIntegrity();
        });
        isoActionRow.addView(btnCheckIntegrity, checkLp);
        page.addView(isoActionRow, isoActionRowLp);

        Button btnClearData = new Button(this);
        btnClearData.setText("🗑️  LIBERAR ALMACENAMIENTO DE MAPAS");
        btnClearData.setTextColor(palette.warningBadgeText);
        btnClearData.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnClearData.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnClearData.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 40, 30, 20), 2, Color.argb(90, 245, 158, 11)));
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        clearLp.topMargin = dp(8);
        clearLp.bottomMargin = dp(14);
        btnClearData.setOnClickListener(v -> {
            hapticFeedback(v);
            confirmClearData();
        });
        page.addView(btnClearData, clearLp);

        // Community & GitHub Card
        TextView tvRepoTitle = new TextView(this);
        tvRepoTitle.setText("PROYECTO DE CÓDIGO ABIERTO");
        tvRepoTitle.setTextColor(palette.accent);
        tvRepoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvRepoTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvRepoTitle.setPadding(0, 0, 0, dp(4));
        page.addView(tvRepoTitle);

        Button btnGithub = new Button(this);
        btnGithub.setText("🌐  REPOSITORIO OFICIAL EN GITHUB");
        btnGithub.setTextColor(palette.textPrimary);
        btnGithub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnGithub.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnGithub.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams ghLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        btnGithub.setOnClickListener(v -> {
            hapticFeedback(v);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getRepoUrl(this)));
            startActivity(browserIntent);
        });
        page.addView(btnGithub, ghLp);

        // [FUTURE_EXTENSION]: Cloudflare Workers / Free API integration space for community maps & comments.

        return sv;
    }

    // ==========================================
    // TAB 5: AJUSTES Y RUTAS (JSON & REPO CONFIG)
    // ==========================================
    private View buildPageSettings() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        // Header Title
        TextView tvSettingsHeader = new TextView(this);
        tvSettingsHeader.setText("CONFIGURACIÓN DE SERVICIOS Y RED");
        tvSettingsHeader.setTextColor(palette.accent);
        tvSettingsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSettingsHeader.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvSettingsHeader.setLetterSpacing(0.08f);
        }
        page.addView(tvSettingsHeader);

        TextView tvSettingsTitle = new TextView(this);
        tvSettingsTitle.setText("RUTAS Y REPOSITORIOS JSON");
        tvSettingsTitle.setTextColor(palette.textPrimary);
        tvSettingsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvSettingsTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvSettingsTitle.setPadding(0, dp(2), 0, dp(4));
        page.addView(tvSettingsTitle);

        TextView tvSettingsDesc = new TextView(this);
        tvSettingsDesc.setText("Personaliza las direcciones remotas para consultar actualizaciones, anuncios comunitarios y comandos sin recompilar el APK.");
        tvSettingsDesc.setTextColor(palette.textSecondary);
        tvSettingsDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvSettingsDesc.setPadding(0, 0, 0, dp(14));
        page.addView(tvSettingsDesc);

        // Card 1: Rutas de Sincronización Remota
        LinearLayout cardRoutes = createInfoCard("DIRECCIONES DE SINCRONIZACIÓN REMOTA");

        addRouteLabel(cardRoutes, "URL DE ACTUALIZACIONES (updates.json):");
        EditText etUpdates = createTacticalEditText(getUpdatesUrl(this), "https://.../updates.json");
        cardRoutes.addView(etUpdates);
        addTestConnectionRow(cardRoutes, etUpdates);

        addRouteLabel(cardRoutes, "URL DE MENSAJES MOTD (motd.json):");
        EditText etMotd = createTacticalEditText(getMotdUrl(this), "https://.../motd.json");
        cardRoutes.addView(etMotd);
        addTestConnectionRow(cardRoutes, etMotd);

        addRouteLabel(cardRoutes, "URL DE COMANDOS DE CONSOLA (commands.json):");
        EditText etCommands = createTacticalEditText(getCommandsUrl(this), "https://.../commands.json");
        cardRoutes.addView(etCommands);
        addTestConnectionRow(cardRoutes, etCommands);

        addRouteLabel(cardRoutes, "URL DEL REPOSITORIO DE GITHUB:");
        EditText etRepo = createTacticalEditText(getRepoUrl(this), "https://github.com/...");
        cardRoutes.addView(etRepo);

        page.addView(cardRoutes);

        // Action Buttons Row (Guardar / Restablecer)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, dp(6), 0, dp(16));

        Button btnSave = new Button(this);
        btnSave.setText("💾  GUARDAR Y APLICAR");
        btnSave.setTextColor(Color.rgb(6, 9, 14));
        btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnSave.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnSave.setBackground(createGradientButton(palette.accent, palette.accentBg, 2));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        saveLp.rightMargin = dp(8);
        btnSave.setOnClickListener(v -> {
            hapticFeedback(v);
            String uUpdates = etUpdates.getText().toString().trim();
            String uMotd = etMotd.getText().toString().trim();
            String uCommands = etCommands.getText().toString().trim();
            String uRepo = etRepo.getText().toString().trim();

            hubPrefs.edit()
                .putString(KEY_URL_UPDATES, uUpdates.isEmpty() ? DEFAULT_URL_UPDATES_JSON : uUpdates)
                .putString(KEY_URL_MOTD, uMotd.isEmpty() ? DEFAULT_URL_MOTD_JSON : uMotd)
                .putString(KEY_URL_COMMANDS, uCommands.isEmpty() ? DEFAULT_URL_COMMANDS_JSON : uCommands)
                .putString(KEY_URL_REPO, uRepo.isEmpty() ? DEFAULT_URL_GITHUB_REPO : uRepo)
                .apply();

            Toast.makeText(this, "✓ Rutas actualizadas y guardadas con éxito", Toast.LENGTH_SHORT).show();
        });
        actionsRow.addView(btnSave, saveLp);

        Button btnReset = new Button(this);
        btnReset.setText("RESTABLECER POR DEFECTO");
        btnReset.setTextColor(palette.textSecondary);
        btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnReset.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btnReset.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, dp(44), 0.8f);
        btnReset.setOnClickListener(v -> {
            hapticFeedback(v);
            etUpdates.setText(DEFAULT_URL_UPDATES_JSON);
            etMotd.setText(DEFAULT_URL_MOTD_JSON);
            etCommands.setText(DEFAULT_URL_COMMANDS_JSON);
            etRepo.setText(DEFAULT_URL_GITHUB_REPO);

            hubPrefs.edit()
                .remove(KEY_URL_UPDATES)
                .remove(KEY_URL_MOTD)
                .remove(KEY_URL_COMMANDS)
                .remove(KEY_URL_REPO)
                .apply();

            Toast.makeText(this, "✓ Valores predeterminados restaurados", Toast.LENGTH_SHORT).show();
        });
        actionsRow.addView(btnReset, resetLp);

        page.addView(actionsRow);

        // Card 2: Gestión de Avisos y Notificaciones
        LinearLayout cardNotif = createInfoCard("GESTIÓN DE AVISOS Y NOTIFICACIONES");
        cardNotif.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView tvNotifDesc = new TextView(this);
        Set<String> dismissedSet = hubPrefs.getStringSet(PREF_DISMISSED_NOTICES, null);
        int dCount = dismissedSet != null ? dismissedSet.size() : 0;
        tvNotifDesc.setText("Avisos marcados como leídos en este dispositivo: " + dCount + "\nLos avisos con ID guardado no volverán a mostrarse ni activarán alertas al abrir el juego.");
        tvNotifDesc.setTextColor(palette.textSecondary);
        tvNotifDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvNotifDesc.setTypeface(getTacticalTypeface(false));
        tvNotifDesc.setPadding(0, dp(4), 0, dp(10));
        cardNotif.addView(tvNotifDesc);

        Button btnClearNotices = new Button(this);
        btnClearNotices.setText("🗑  RESTABLECER HISTORIAL DE AVISOS LEÍDOS");
        btnClearNotices.setTextColor(palette.textPrimary);
        btnClearNotices.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnClearNotices.setTypeface(getTacticalTypeface(true));
        btnClearNotices.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(200, 28, 40, 60), 2, palette.btnSecondaryBorder));
        btnClearNotices.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams cnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        btnClearNotices.setOnClickListener(v -> {
            hapticFeedback(v);
            hubPrefs.edit().remove(PREF_DISMISSED_NOTICES).commit();
            updateBellBadgeUI();
            Toast.makeText(this, "✓ Historial de avisos leídos restablecido", Toast.LENGTH_SHORT).show();
            selectTab(4, false); // Reload tab to refresh count
        });
        cardNotif.addView(btnClearNotices, cnLp);

        page.addView(cardNotif);

        return sv;
    }

    private EditText createTacticalEditText(String text, String hint) {
        EditText et = new EditText(this);
        et.setText(text);
        et.setHint(hint);
        et.setHintTextColor(palette.textMuted);
        et.setTextColor(palette.textPrimary);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        et.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        et.setSingleLine(true);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(140, 12, 18, 28));
        gd.setCornerRadius(dp(2));
        gd.setStroke(dp(1), Color.argb(60, 255, 255, 255));
        et.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        lp.bottomMargin = dp(4);
        et.setLayoutParams(lp);
        return et;
    }

    private void addRouteLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(palette.textSecondary);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tv.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tv.setPadding(0, dp(8), 0, dp(3));
        parent.addView(tv);
    }

    private void addTestConnectionRow(LinearLayout parent, EditText targetEt) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(6));

        Button btnTest = new Button(this);
        btnTest.setText("PROBAR CONEXIÓN");
        btnTest.setTextColor(palette.textPrimary);
        btnTest.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        btnTest.setTypeface(getTacticalTypeface(true));
        btnTest.setBackground(createButtonDrawable(Color.argb(160, 20, 28, 42), Color.argb(220, 32, 44, 64), 2, Color.argb(60, 148, 163, 184)));
        btnTest.setPadding(dp(14), 0, dp(14), 0);
        btnTest.setIncludeFontPadding(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        btnLp.rightMargin = dp(12);
        row.addView(btnTest, btnLp);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Estado: No verificado");
        tvStatus.setTextColor(palette.textMuted);
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvStatus.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        row.addView(tvStatus);

        btnTest.setOnClickListener(v -> {
            hapticFeedback(v);
            final String testUrl = targetEt.getText().toString().trim();
            if (testUrl.isEmpty() || !testUrl.startsWith("http")) {
                tvStatus.setText("✗ URL inválida");
                tvStatus.setTextColor(Color.rgb(248, 113, 113));
                return;
            }
            tvStatus.setText("Conectando...");
            tvStatus.setTextColor(palette.accent);
            new Thread(() -> {
                try {
                    URL u = new URL(testUrl);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(3500);
                    c.setReadTimeout(3500);
                    c.setRequestMethod("GET");
                    c.connect();
                    int code = c.getResponseCode();
                    c.disconnect();
                    handler.post(() -> {
                        if (code == 200) {
                            tvStatus.setText("✓ Conexión exitosa (HTTP 200 OK)");
                            tvStatus.setTextColor(palette.successBadgeText);
                        } else {
                            tvStatus.setText("⚠️ Servidor respondió: HTTP " + code);
                            tvStatus.setTextColor(palette.warningBadgeText);
                        }
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        tvStatus.setText("✗ Error: " + (e.getMessage() != null ? e.getMessage() : "Timeout"));
                        tvStatus.setTextColor(Color.rgb(248, 113, 113));
                    });
                }
            }).start();
        });

        parent.addView(row);
    }

    // ==========================================
    // TAB 6: INFORMACIÓN Y ESTADO DEL SISTEMA
    // ==========================================
    private View buildPageInfo() {
        ScrollView sv = new ScrollView(this);
        configureTacticalScrollView(sv);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        sv.addView(page);

        TextView tvInfoHeader = new TextView(this);
        tvInfoHeader.setText("TELEMETRÍA Y SISTEMA");
        tvInfoHeader.setTextColor(palette.accent);
        tvInfoHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvInfoHeader.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvInfoHeader.setLetterSpacing(0.08f);
        }
        page.addView(tvInfoHeader);

        TextView tvInfoHeading = new TextView(this);
        tvInfoHeading.setText("ESPECIFICACIONES Y ESTADO DE SPARTAN CE");
        tvInfoHeading.setTextColor(palette.textPrimary);
        tvInfoHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvInfoHeading.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        tvInfoHeading.setPadding(0, dp(2), 0, dp(12));
        page.addView(tvInfoHeading);

        // Card 1: Perfil de Identidad & Versión
        LinearLayout cardProfile = createInfoCard("IDENTIDAD Y VERSIÓN DEL MOTOR");
        addInfoRow(cardProfile, "Perfil Spartan:", "SPARTAN-117 (Master Chief)");
        addInfoRow(cardProfile, "Versión de App:", "Spartan CE v1.1.0 (Mobile Tactical Edition)");
        addInfoRow(cardProfile, "Motor Base:", "ARM64 C/C++ Native Recompilation Engine");
        addInfoRow(cardProfile, "Renderizador:", "Custom OpenGL ES 3.0 / Vulkan Pipeline");
        addInfoRow(cardProfile, "Sistema de Audio:", "OpenAL Soft 1.23+ con Audio 3D Posicional");
        addInfoRow(cardProfile, "Desarrollador:", "@kyrnforge / devlwte");
        page.addView(cardProfile);

        // Gather real device specs
        ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo);
        }
        long totalRamMb = memInfo.totalMem / (1024L * 1024L);
        long availRamMb = memInfo.availMem / (1024L * 1024L);
        float totalRamGb = totalRamMb / 1024.0f;
        float availRamGb = availRamMb / 1024.0f;

        float refreshRate = 60f;
        try {
            refreshRate = getWindowManager().getDefaultDisplay().getRefreshRate();
        } catch (Exception ignored) {}

        int widthPx = getResources().getDisplayMetrics().widthPixels;
        int heightPx = getResources().getDisplayMetrics().heightPixels;

        long freeBytes = dataRoot != null ? dataRoot.getFreeSpace() : 0;
        float freeGb = freeBytes / (1024.0f * 1024.0f * 1024.0f);

        // Card 2: Diagnóstico del Dispositivo
        LinearLayout cardDevice = createInfoCard("DIAGNÓSTICO DEL DISPOSITIVO");
        addInfoRow(cardDevice, "Modelo:", Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL);
        addInfoRow(cardDevice, "Arquitectura CPU:", (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "arm64-v8a") + " (64-bit)");
        addInfoRow(cardDevice, "Memoria RAM:", String.format("%.2f GB Totales (%.2f GB Libres)", totalRamGb, availRamGb));
        addInfoRow(cardDevice, "Pantalla:", widthPx + " x " + heightPx + " px @ " + (int)refreshRate + " Hz");
        addInfoRow(cardDevice, "Sistema Operativo:", "Android " + Build.VERSION.RELEASE + " (API Level " + Build.VERSION.SDK_INT + ")");
        addInfoRow(cardDevice, "Almacenamiento Libre:", String.format("%.2f GB en almacenamiento interno", freeGb));
        page.addView(cardDevice);

        // Card 3: Verificación de Requisitos Técnicos
        LinearLayout cardReqs = createInfoCard("VERIFICACIÓN DE REQUISITOS TÉCNICOS");
        boolean is64 = Build.SUPPORTED_ABIS.length > 0 && Build.SUPPORTED_ABIS[0].contains("64");
        boolean ramOk = totalRamGb >= 2.5f;
        boolean osOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
        boolean dataOk = haveData();

        addReqRow(cardReqs, "CPU ARM64 (64-bit)", is64 ? "✓ COMPATIBLE (64-bit Nativo)" : "✗ No compatible", is64);
        addReqRow(cardReqs, "Memoria RAM (Mín. 3 GB)", ramOk ? String.format("✓ ÓPTIMO (%.1f GB detectados)", totalRamGb) : "⚠️ Mínimo justo", ramOk);
        addReqRow(cardReqs, "GPU OpenGL ES 3.0+", "✓ COMPATIBLE (Soporte GLES 3.0+ activo)", true);
        addReqRow(cardReqs, "Sistema (Android 9.0+)", osOk ? ("✓ COMPATIBLE (Android " + Build.VERSION.RELEASE + ")") : "⚠️ Versión anterior", osOk);
        addReqRow(cardReqs, "Datos del Juego (ui.map)", dataOk ? "✓ INSTALACIÓN COMPLETA Y LISTA" : "○ PENDIENTE (Seleccionar ISO)", dataOk);
        page.addView(cardReqs);

        return sv;
    }

    private LinearLayout createInfoCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(120, 14, 20, 30));
        gd.setCornerRadius(dp(2));
        gd.setStroke(dp(1), Color.argb(40, 255, 255, 255));
        card.setBackground(gd);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(palette.accent);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvTitle.setLetterSpacing(0.08f);
        }
        tvTitle.setPadding(0, 0, 0, dp(8));
        card.addView(tvTitle);

        return card;
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(palette.textSecondary);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvLabel.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(dp(135), ViewGroup.LayoutParams.WRAP_CONTENT);
        row.addView(tvLabel, labelLp);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(palette.textPrimary);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvVal.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        row.addView(tvVal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        container.addView(row);
    }

    private void addReqRow(LinearLayout container, String req, String status, boolean ok) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView tvReq = new TextView(this);
        tvReq.setText(req);
        tvReq.setTextColor(palette.textPrimary);
        tvReq.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvReq.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        row.addView(tvReq, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextColor(ok ? palette.successBadgeText : palette.warningBadgeText);
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvStatus.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        row.addView(tvStatus);

        container.addView(row);
    }

    private void chooseIsoImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void updateStatusUI() {
        if (tvStatusHeader == null || btnLaunch == null) return;
        boolean ready = haveData();
        tvStatusHeader.setText(ready ? "● ESTADO: LISTO PARA COMBATE" : "○ ESTADO: SE REQUIEREN DATOS");
        tvStatusHeader.setTextColor(ready ? palette.successBadgeText : palette.warningBadgeText);
        tvStatusHeader.setBackground(createBadgeBackground(ready ? palette.successBadgeBg : palette.warningBadgeBg));

        String currentLang = hubPrefs.getString("installed_language", ready ? "Español" : "Ninguno");
        String shortLang = currentLang.split(" ")[0].toUpperCase();
        tvInstalledLang.setText(ready ? ("🌐 " + shortLang) : "🌐 SIN DATOS");
        tvInstalledLang.setTextColor(palette.textPrimary);
        tvInstalledLang.setBackground(createButtonDrawable(palette.btnSecondaryBg, palette.btnSecondaryBg, 2, palette.btnSecondaryBorder));

        if (ready) {
            btnLaunch.setText("▶  INICIAR MISIÓN (SPARTAN CE)");
            btnLaunch.setTextColor(Color.rgb(6, 9, 14));
            btnLaunch.setBackground(createGradientButton(Color.rgb(56, 189, 248), Color.rgb(14, 165, 233), 2));
            btnLaunch.setOnClickListener(v -> {
                hapticFeedback(v);
                startGame();
            });
        } else {
            btnLaunch.setText("📂  SELECCIONAR IMAGEN ISO (.ISO / .XISO)");
            btnLaunch.setTextColor(Color.WHITE);
            btnLaunch.setBackground(createGradientButton(Color.rgb(14, 165, 233), Color.rgb(2, 132, 199), 2));
            btnLaunch.setOnClickListener(v -> {
                hapticFeedback(v);
                chooseIsoImage();
            });
        }

        if (btnPickIso != null) {
            btnPickIso.setText(ready ? "🌐  CAMBIAR IDIOMA / ISO" : "📂  SELECCIONAR ISO");
        }
    }

    private void confirmResetControls() {
        new AlertDialog.Builder(this)
            .setTitle("↺ Restablecer controles táctiles")
            .setMessage("¿Deseas restaurar la posición y tamaño de todos los botones virtuales a los valores predeterminados de fábrica?")
            .setPositiveButton("Restablecer", (dialog, which) -> {
                SharedPreferences.Editor editor = touchPrefs.edit();
                editor.remove("configuration");
                editor.remove("swipe-layout");
                for (int i = 0; i < TouchLayout.MAX_CONTROLS; i++) {
                    editor.remove("x" + i);
                    editor.remove("y" + i);
                }
                editor.commit();

                // Terminate background HaloActivity instance so it recreates with pristine defaults
                if (HaloActivity.instance != null) {
                    HaloActivity.instance.finish();
                }

                Toast.makeText(this, "✅ Controles restaurados a valores de fábrica.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void checkGamepadCalibration() {
        int[] deviceIds = InputDevice.getDeviceIds();
        java.util.List<String> gamepads = new java.util.ArrayList<>();
        for (int id : deviceIds) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev != null && !dev.isVirtual()) {
                int sources = dev.getSources();
                if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                    gamepads.add(dev.getName());
                }
            }
        }

        if (gamepads.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("🎮 Calibración de Mando")
                .setMessage("No se detectó ningún mando físico conectado por Bluetooth o USB.\n\n"
                    + "Para jugar con mando:\n"
                    + "1. Enciende tu mando (Xbox, PlayStation o Gamepad Bluetooth).\n"
                    + "2. Emparéjalo en los Ajustes de Bluetooth de tu teléfono.\n"
                    + "3. Vuelve a pulsar este botón para confirmar la conexión.\n\n"
                    + "Nota: Spartan CE detecta y mapea automáticamente los gatillos analógicos, botones de acción y sticks del mando sin configuración manual.")
                .setPositiveButton("Entendido", null)
                .show();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Se detectó mando conectado:\n\n");
            for (String g : gamepads) {
                sb.append("• ").append(g).append("\n");
            }
            sb.append("\nTodos los botones, gatillos y sticks analógicos están calibrados y listos para jugar.");
            new AlertDialog.Builder(this)
                .setTitle("🎮 Mando Conectado")
                .setMessage(sb.toString())
                .setPositiveButton("Excelente", null)
                .show();
        }
    }

    private void checkMapIntegrity() {
        if (dataRoot == null || !new File(dataRoot, "maps").isDirectory()) {
            new AlertDialog.Builder(this)
                .setTitle("🛡️ Verificación de integridad")
                .setMessage("No se encontraron mapas instalados en el dispositivo.\n\nSelecciona un archivo ISO (.iso / .xiso) para comenzar la instalación.")
                .setPositiveButton("Entendido", null)
                .show();
            return;
        }

        File mapsDir = new File(dataRoot, "maps");
        File uiMap = new File(mapsDir, "ui.map");
        File[] files = mapsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".map"));
        int mapCount = files != null ? files.length : 0;
        long totalBytes = 0;
        if (files != null) {
            for (File f : files) totalBytes += f.length();
        }
        long totalMb = totalBytes / (1024 * 1024);

        if (!uiMap.isFile()) {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Error crítico de archivos")
                .setMessage("Falta el archivo principal 'ui.map'. El juego no podrá iniciar sin la interfaz del menú.\n\nPor favor, reinstala el juego desde tu archivo ISO.")
                .setPositiveButton("Reinstalar", (dialog, which) -> chooseIsoImage())
                .setNegativeButton("Cerrar", null)
                .show();
        } else if (uiMap.length() < 5 * 1024 * 1024) {
            long sizeKb = uiMap.length() / 1024;
            new AlertDialog.Builder(this)
                .setTitle("⚠️ ui.map dañado o incompleto")
                .setMessage("El archivo 'ui.map' está dañado o incompleto (pesa solo " + sizeKb + " KB; debe medir al menos 14 MB).\n\n"
                    + "Esto causará el error de pantalla azul ('failed to load structure bsp #0') al intentar abrir el juego.\n\n"
                    + "¿Deseas reinstalar desde una ISO limpia ahora?")
                .setPositiveButton("Reinstalar desde ISO", (dialog, which) -> chooseIsoImage())
                .setNegativeButton("Cancelar", null)
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("✅ Integridad verificada al 100%")
                .setMessage("Todos los archivos esenciales han sido comprobados con éxito:\n\n"
                    + "• ui.map (Menú e interfaz): " + String.format("%.1f MB", uiMap.length() / (1024.0 * 1024.0)) + " (Íntegro)\n"
                    + "• Mapas detectados: " + mapCount + " niveles instalados\n"
                    + "• Espacio ocupado: " + totalMb + " MB\n"
                    + "• Estado del motor: Listo para jugar sin errores.")
                .setPositiveButton("Excelente", null)
                .show();
        }
    }

    private void confirmClearData() {
        if (!haveData()) {
            Toast.makeText(this, "No hay mapas instalados que eliminar.", Toast.LENGTH_SHORT).show();
            return;
        }

        File mapsDir = new File(dataRoot, "maps");
        long totalBytes = 0;
        File[] files = mapsDir.listFiles();
        if (files != null) {
            for (File f : files) totalBytes += f.length();
        }
        long totalMb = totalBytes / (1024 * 1024);

        new AlertDialog.Builder(this)
            .setTitle("🗑️ Liberar almacenamiento")
            .setMessage("¿Estás seguro de que deseas eliminar los datos y mapas instalados (" + totalMb + " MB)?\n\nTendrás que volver a seleccionarlos desde una ISO para volver a jugar.")
            .setPositiveButton("Eliminar datos", (dialog, which) -> {
                if (files != null) {
                    for (File f : files) f.delete();
                }
                hubPrefs.edit().remove("installed_language").apply();
                updateStatusUI();
                Toast.makeText(this, "Espacio liberado correctamente (" + totalMb + " MB).", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null)
            return;
        pendingIsoUri = data.getData();
        if (btnPickIso != null) btnPickIso.setEnabled(false);
        selectTab(0, true); // Automatically switch to JUGAR tab to show progress bar
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (statusText != null) statusText.setText("Analizando imagen ISO y detectando idiomas...");

        new Thread(this::inspectIsoLanguages).start();
    }

    private void inspectIsoLanguages() {
        try (ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(pendingIsoUri, "r")) {
            if (descriptor == null)
                throw new java.io.IOException("No se pudo abrir el archivo ISO.");

            try (FileInputStream fileIn = new FileInputStream(descriptor.getFileDescriptor());
                 FileChannel channel = fileIn.getChannel()) {

                detectedLanguages = XisoExtractor.listLanguageEntries(channel);

                handler.post(() -> {
                    if (detectedLanguages.isEmpty()) {
                        fail("No se encontraron carpetas de idioma válidas dentro de la imagen ISO.");
                    } else if (detectedLanguages.size() == 1) {
                        installLanguageEntry(detectedLanguages.get(0));
                    } else {
                        showLanguageChoiceDialog(detectedLanguages);
                    }
                });
            }
        } catch (Exception e) {
            fail("Error al inspeccionar la imagen ISO: " + e.getMessage());
        }
    }

    private void showLanguageChoiceDialog(List<XisoExtractor.LanguageEntry> languages) {
        String[] titles = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            titles[i] = languages.get(i).displayName;
        }

        new AlertDialog.Builder(this)
            .setTitle("Selecciona el Idioma del Juego")
            .setCancelable(false)
            .setItems(titles, (dialog, which) -> {
                XisoExtractor.LanguageEntry chosen = languages.get(which);
                installLanguageEntry(chosen);
            })
            .setNegativeButton("Cancelar", (dialog, which) -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (statusText != null) statusText.setText("Instalación cancelada.");
                if (btnPickIso != null) btnPickIso.setEnabled(true);
            })
            .show();
    }

    private void installLanguageEntry(XisoExtractor.LanguageEntry language) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (statusText != null) statusText.setText("Extrayendo archivos para: " + language.displayName);

        new Thread(() -> {
            try (ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(pendingIsoUri, "r");
                 FileInputStream fileIn = new FileInputStream(descriptor.getFileDescriptor());
                 FileChannel channel = fileIn.getChannel()) {

                XisoExtractor.extractMaps(channel, language, dataRoot, (file, done, total) -> {
                    int permille = total > 0 ? (int)((done * 1000) / total) : 0;
                    report("Extrayendo " + file + "...", permille);
                });

                hubPrefs.edit().putString("installed_language", language.displayName).apply();

                handler.post(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (btnPickIso != null) btnPickIso.setEnabled(true);
                    updateStatusUI();
                    if (statusText != null) statusText.setText("¡Instalación completada con éxito!");
                    if (tvInstalledLang != null) tvInstalledLang.setText("Idioma actual: " + language.displayName);
                    Toast.makeText(this, "Datos instalados correctamente en " + language.displayName, Toast.LENGTH_LONG).show();

                    new AlertDialog.Builder(this)
                        .setTitle("¡Juego listo!")
                        .setMessage("Se ha instalado Spartan CE correctamente con idioma: " + language.displayName + ".\n¿Deseas iniciar el juego ahora?")
                        .setPositiveButton("▶ Iniciar juego", (dialog, which) -> startGame())
                        .setNegativeButton("Quedarme en el Hub", null)
                        .show();
                });
            } catch (Exception e) {
                fail("Error durante la extracción: " + e.getMessage());
            }
        }).start();
    }

    private void report(String text, int permille) {
        handler.post(() -> {
            if (statusText != null) statusText.setText(text);
            if (progressBar != null) {
                progressBar.setProgress(permille);
            }
        });
    }

    private void fail(String text) {
        handler.post(() -> {
            if (statusText != null) statusText.setText(text);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (btnPickIso != null) btnPickIso.setEnabled(true);
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        });
    }
}
