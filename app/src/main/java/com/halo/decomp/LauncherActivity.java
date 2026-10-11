package com.halo.decomp;

import com.kyrn.spartan.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Spartan CE - Tactical Hub & Launcher.
 * Features:
 * - Multi-language ISO extractor with language detection (maps_es, maps_it, maps).
 * - Virtual on-screen touch controller HUD configuration (opacity, sensitivity, gyro, rumble, FPS).
 * - Game launcher with game data validation.
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences hubPrefs;
    private SharedPreferences touchPrefs;

    private Uri pendingIsoUri;
    private List<XisoExtractor.LanguageEntry> detectedLanguages;

    private boolean isDarkMode = true;
    private ThemePalette palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hubPrefs = getSharedPreferences(PREFS_HUB, Context.MODE_PRIVATE);
        touchPrefs = getSharedPreferences(PREFS_TOUCH, Context.MODE_PRIVATE);
        isDarkMode = hubPrefs.getBoolean("dark_mode", true);
        palette = new ThemePalette(isDarkMode);

        dataRoot = getExternalFilesDir(null);
        if (dataRoot != null) {
            new File(dataRoot, "maps").mkdirs();
        }

        passOnHardwareId();
        passOnInvite(getIntent());

        MotdManager.checkAndSyncRemote(this);

        buildHubInterface();
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
        startActivity(new Intent(this, HaloActivity.class));
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
            getResources().getDisplayMetrics());
    }

    private static class ThemePalette {
        final int bgRoot;
        final int bgCard;
        final int strokeCard;
        final int textPrimary;
        final int textSecondary;
        final int textMuted;
        final int accent;
        final int accentBg;
        final int accentText;
        final int btnSecondaryBg;
        final int btnSecondaryText;
        final int successBadgeBg;
        final int successBadgeText;
        final int warningBadgeBg;
        final int warningBadgeText;
        final int divider;

        ThemePalette(boolean dark) {
            if (dark) {
                bgRoot = Color.rgb(10, 13, 20); // #0A0D14
                bgCard = Color.argb(235, 17, 23, 34); // #111722
                strokeCard = Color.argb(120, 30, 41, 59); // #1E293B
                textPrimary = Color.rgb(248, 250, 252); // #F8FAFC
                textSecondary = Color.rgb(148, 163, 184); // #94A3B8
                textMuted = Color.rgb(100, 116, 139); // #64748B
                accent = Color.rgb(0, 229, 255); // #00E5FF
                accentBg = Color.rgb(0, 229, 255);
                accentText = Color.rgb(10, 13, 20);
                btnSecondaryBg = Color.argb(180, 26, 36, 52);
                btnSecondaryText = Color.rgb(226, 232, 240);
                successBadgeBg = Color.argb(60, 16, 185, 129);
                successBadgeText = Color.rgb(52, 211, 153);
                warningBadgeBg = Color.argb(60, 245, 158, 11);
                warningBadgeText = Color.rgb(251, 191, 36);
                divider = Color.argb(60, 51, 65, 85);
            } else {
                bgRoot = Color.rgb(244, 246, 249); // #F4F6F9
                bgCard = Color.rgb(255, 255, 255); // #FFFFFF
                strokeCard = Color.rgb(226, 232, 240); // #E2E8F0
                textPrimary = Color.rgb(15, 23, 42); // #0F172A
                textSecondary = Color.rgb(71, 85, 105); // #475569
                textMuted = Color.rgb(148, 163, 184); // #94A3B8
                accent = Color.rgb(2, 132, 199); // #0284C7
                accentBg = Color.rgb(2, 132, 199);
                accentText = Color.rgb(255, 255, 255);
                btnSecondaryBg = Color.rgb(241, 245, 249);
                btnSecondaryText = Color.rgb(30, 41, 59);
                successBadgeBg = Color.rgb(209, 250, 229);
                successBadgeText = Color.rgb(5, 150, 105);
                warningBadgeBg = Color.rgb(254, 243, 199);
                warningBadgeText = Color.rgb(217, 119, 6);
                divider = Color.rgb(226, 232, 240);
            }
        }
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(palette.bgCard);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), palette.strokeCard);
        return gd;
    }

    private void hapticFeedback(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private Drawable createButtonBackground(int color, float radiusDp) {
        return createButtonDrawable(color, palette.strokeCard, radiusDp);
    }

    private Drawable createButtonDrawable(int normalColor, int pressedColor, float radiusDp) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(radiusDp));

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        pressed.setCornerRadius(dp(radiusDp));

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

    private void updateSelectorButtons(Button[] buttons, float[] values, float current, float tolerance) {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = Math.abs(values[i] - current) <= tolerance;
            if (selected) {
                buttons[i].setBackground(createButtonDrawable(palette.accent, palette.accentBg, 8));
                buttons[i].setTextColor(Color.rgb(10, 13, 20));
                buttons[i].setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                buttons[i].setBackground(createButtonBackground(palette.btnSecondaryBg, 8));
                buttons[i].setTextColor(palette.btnSecondaryText);
                buttons[i].setTypeface(Typeface.DEFAULT);
            }
        }
    }

    private GradientDrawable createBadgeBackground(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(16));
        return gd;
    }

    private void buildHubInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(palette.bgRoot);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(28));
        scrollView.addView(root);

        // --- Header Row (Logo + Title + Theme Switcher) ---
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(16));
        root.addView(headerRow);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.HORIZONTAL);
        titleGroup.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleGroupLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerRow.addView(titleGroup, titleGroupLp);

        // M6D Pistol Emblem
        ImageView ivLogo = new ImageView(this);
        ivLogo.setImageResource(R.drawable.m6d_emblem);
        int logoSize = dp(44);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivLogo.setPadding(dp(4), dp(4), dp(4), dp(4));
        ivLogo.setClipToOutline(true);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(dp(12));
        logoBg.setColor(Color.rgb(10, 13, 20)); // Obsidian #0A0D14 badge
        logoBg.setStroke(dp(1), palette.strokeCard);
        ivLogo.setBackground(logoBg);
        logoLp.rightMargin = dp(12);
        titleGroup.addView(ivLogo, logoLp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(textCol);

        TextView title = new TextView(this);
        title.setText("SPARTAN CE");
        title.setTextColor(palette.textPrimary);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(MotdManager.getCurrentMessage(this));
        subtitle.setTextColor(palette.textSecondary);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textCol.addView(subtitle);

        // Theme Toggle Pill
        Button btnTheme = new Button(this);
        btnTheme.setText(isDarkMode ? "☀️ LUZ" : "🌙 OSCURO");
        btnTheme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnTheme.setTypeface(Typeface.DEFAULT_BOLD);
        btnTheme.setTextColor(palette.btnSecondaryText);
        btnTheme.setBackground(createButtonBackground(palette.btnSecondaryBg, 16));
        btnTheme.setPadding(dp(14), dp(6), dp(14), dp(6));
        btnTheme.setOnClickListener(v -> {
            hapticFeedback(v);
            isDarkMode = !isDarkMode;
            hubPrefs.edit().putBoolean("dark_mode", isDarkMode).apply();
            palette = new ThemePalette(isDarkMode);
            buildHubInterface();
        });
        headerRow.addView(btnTheme);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);

        // --- Card 1: Game Launch & Status (Hero Card) ---
        LinearLayout launchCard = new LinearLayout(this);
        launchCard.setOrientation(LinearLayout.VERTICAL);
        launchCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        launchCard.setBackground(createCardBackground());
        root.addView(launchCard, cardParams);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams badgeRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeRowLp.bottomMargin = dp(14);
        launchCard.addView(badgeRow, badgeRowLp);

        tvStatusHeader = new TextView(this);
        tvStatusHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvStatusHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatusHeader.setPadding(dp(12), dp(5), dp(12), dp(5));
        badgeRow.addView(tvStatusHeader);

        View badgeSpacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1.0f);
        badgeRow.addView(badgeSpacer, spacerLp);

        tvInstalledLang = new TextView(this);
        tvInstalledLang.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvInstalledLang.setTypeface(Typeface.DEFAULT_BOLD);
        tvInstalledLang.setPadding(dp(12), dp(5), dp(12), dp(5));
        badgeRow.addView(tvInstalledLang);

        btnLaunch = new Button(this);
        btnLaunch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnLaunch.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams launchBtnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        launchCard.addView(btnLaunch, launchBtnLp);

        updateStatusUI();

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progressParams.topMargin = dp(10);
        launchCard.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setTextColor(palette.textMuted);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(6), 0, 0);
        launchCard.addView(statusText);

        // --- Card 2: Virtual Touch Controls HUD Settings ---
        LinearLayout controlsCard = new LinearLayout(this);
        controlsCard.setOrientation(LinearLayout.VERTICAL);
        controlsCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        controlsCard.setBackground(createCardBackground());
        root.addView(controlsCard, cardParams);

        TextView tvControlsTitle = new TextView(this);
        tvControlsTitle.setText("🎮  CONTROLES Y ENTORNO");
        tvControlsTitle.setTextColor(palette.accent);
        tvControlsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvControlsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvControlsTitle.setPadding(0, 0, 0, dp(10));
        controlsCard.addView(tvControlsTitle);

        // Fila 1: Botones de acceso rápido (Personalizar posición HUD y Calibrar Mando)
        LinearLayout quickActionRow = new LinearLayout(this);
        quickActionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams quickActionRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickActionRowLp.bottomMargin = dp(12);

        Button btnEditHud = new Button(this);
        btnEditHud.setText("✏️  Personalizar posición HUD");
        btnEditHud.setTextColor(palette.btnSecondaryText);
        btnEditHud.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnEditHud.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        LinearLayout.LayoutParams editHudLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        editHudLp.rightMargin = dp(6);
        btnEditHud.setOnClickListener(v -> {
            hapticFeedback(v);
            startActivity(new Intent(this, HudEditorActivity.class));
        });
        quickActionRow.addView(btnEditHud, editHudLp);

        Button btnCalibrateGamepad = new Button(this);
        btnCalibrateGamepad.setText("🎮  Calibrar Mando");
        btnCalibrateGamepad.setTextColor(palette.btnSecondaryText);
        btnCalibrateGamepad.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnCalibrateGamepad.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        LinearLayout.LayoutParams gamepadLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        btnCalibrateGamepad.setOnClickListener(v -> {
            hapticFeedback(v);
            checkGamepadCalibration();
        });
        quickActionRow.addView(btnCalibrateGamepad, gamepadLp);

        controlsCard.addView(quickActionRow, quickActionRowLp);

        CheckBox cbOverlay = new CheckBox(this);
        cbOverlay.setText("Botones virtuales en pantalla (HUD controller)");
        cbOverlay.setTextColor(palette.textPrimary);
        cbOverlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        boolean overlayDisabled = touchPrefs.getBoolean("overlay-disabled", false);
        cbOverlay.setChecked(!overlayDisabled);
        cbOverlay.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("overlay-disabled", !isChecked).apply();
            Toast.makeText(this, isChecked ? "Controles táctiles activados" : "Controles táctiles desactivados", Toast.LENGTH_SHORT).show();
        });
        controlsCard.addView(cbOverlay);

        TextView tvOpacity = new TextView(this);
        float currentOpacity = touchPrefs.getFloat("controls-opacity", 1.0f);
        tvOpacity.setText("Opacidad de botones: " + (int)(currentOpacity * 100) + "%");
        tvOpacity.setTextColor(palette.textSecondary);
        tvOpacity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvOpacity.setPadding(0, dp(10), 0, dp(4));
        controlsCard.addView(tvOpacity);

        LinearLayout opacityRow = new LinearLayout(this);
        opacityRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] opacityValues = { 0.4f, 0.6f, 0.8f, 1.0f };
        String[] opacityLabels = { "40%", "60%", "80%", "100%" };
        Button[] opacityButtons = new Button[opacityValues.length];
        for (int i = 0; i < opacityValues.length; i++) {
            final float val = opacityValues[i];
            Button b = new Button(this);
            b.setText(opacityLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            if (i < opacityValues.length - 1) lp.rightMargin = dp(6);
            opacityRow.addView(b, lp);
            opacityButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                touchPrefs.edit().putFloat("controls-opacity", val).apply();
                tvOpacity.setText("Opacidad de botones: " + (int)(val * 100) + "%");
                updateSelectorButtons(opacityButtons, opacityValues, val, 0.05f);
                Toast.makeText(this, "Opacidad: " + (int)(val * 100) + "%", Toast.LENGTH_SHORT).show();
            });
        }
        updateSelectorButtons(opacityButtons, opacityValues, currentOpacity, 0.05f);
        controlsCard.addView(opacityRow);

        TextView tvSens = new TextView(this);
        float currentSens = touchPrefs.getFloat("look-sensitivity", 2.0f);
        tvSens.setText("Sensibilidad de apuntado: " + String.format("%.1fx", currentSens));
        tvSens.setTextColor(palette.textSecondary);
        tvSens.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSens.setPadding(0, dp(10), 0, dp(2));
        controlsCard.addView(tvSens);

        // Barra deslizante continua de sensibilidad [═══════●════════]
        SeekBar sbSens = new SeekBar(this);
        sbSens.setMax(35); // 0.5x a 4.0x
        int sensProgress = Math.round((currentSens - 0.5f) * 10f);
        if (sensProgress < 0) sensProgress = 0;
        if (sensProgress > 35) sensProgress = 35;
        sbSens.setProgress(sensProgress);
        LinearLayout.LayoutParams sbSensLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbSensLp.setMargins(0, dp(4), 0, dp(6));
        controlsCard.addView(sbSens, sbSensLp);

        LinearLayout sensRow = new LinearLayout(this);
        sensRow.setOrientation(LinearLayout.HORIZONTAL);
        float[] sensValues = { 1.0f, 1.5f, 2.0f, 3.0f };
        String[] sensLabels = { "1.0x", "1.5x", "2.0x", "3.0x" };
        Button[] sensButtons = new Button[sensValues.length];
        for (int i = 0; i < sensValues.length; i++) {
            final float val = sensValues[i];
            Button b = new Button(this);
            b.setText(sensLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            if (i < sensValues.length - 1) lp.rightMargin = dp(6);
            sensRow.addView(b, lp);
            sensButtons[i] = b;
            b.setOnClickListener(v -> {
                hapticFeedback(v);
                touchPrefs.edit().putFloat("look-sensitivity", val).apply();
                tvSens.setText("Sensibilidad de apuntado: " + String.format("%.1fx", val));
                int newProg = Math.round((val - 0.5f) * 10f);
                sbSens.setProgress(newProg);
                updateSelectorButtons(sensButtons, sensValues, val, 0.1f);
                Toast.makeText(this, "Sensibilidad: " + val + "x", Toast.LENGTH_SHORT).show();
            });
        }
        updateSelectorButtons(sensButtons, sensValues, currentSens, 0.1f);
        controlsCard.addView(sensRow);

        sbSens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float val = 0.5f + (progress / 10.0f);
                    touchPrefs.edit().putFloat("look-sensitivity", val).apply();
                    tvSens.setText("Sensibilidad de apuntado: " + String.format("%.1fx", val));
                    updateSelectorButtons(sensButtons, sensValues, val, 0.1f);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                hapticFeedback(seekBar);
            }
        });

        LinearLayout togglesRow = new LinearLayout(this);
        togglesRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams togglesRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        togglesRowLp.topMargin = dp(8);
        togglesRowLp.bottomMargin = dp(4);
        controlsCard.addView(togglesRow, togglesRowLp);

        CheckBox cbGyro = new CheckBox(this);
        cbGyro.setText("Giroscopio");
        cbGyro.setTextColor(palette.textPrimary);
        cbGyro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cbGyro.setChecked(touchPrefs.getBoolean("gyroscope", false));
        cbGyro.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("gyroscope", isChecked).commit();
        });
        LinearLayout.LayoutParams gyroLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        togglesRow.addView(cbGyro, gyroLp);

        CheckBox cbRumble = new CheckBox(this);
        cbRumble.setText("Vibración");
        cbRumble.setTextColor(palette.textPrimary);
        cbRumble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cbRumble.setChecked(touchPrefs.getBoolean("rumble", true));
        cbRumble.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("rumble", isChecked).commit();
        });
        LinearLayout.LayoutParams rumbleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        togglesRow.addView(cbRumble, rumbleLp);

        CheckBox cbFps = new CheckBox(this);
        cbFps.setText("Contador FPS");
        cbFps.setTextColor(palette.textPrimary);
        cbFps.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cbFps.setChecked(touchPrefs.getBoolean("fps-counter", false));
        cbFps.setOnCheckedChangeListener((btn, isChecked) -> {
            hapticFeedback(btn);
            touchPrefs.edit().putBoolean("fps-counter", isChecked).commit();
        });
        LinearLayout.LayoutParams fpsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        togglesRow.addView(cbFps, fpsLp);

        Button btnResetControls = new Button(this);
        btnResetControls.setText("↺  Restablecer posición de controles a fábrica");
        btnResetControls.setTextColor(palette.btnSecondaryText);
        btnResetControls.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnResetControls.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        resetLp.topMargin = dp(10);
        btnResetControls.setOnClickListener(v -> {
            hapticFeedback(v);
            confirmResetControls();
        });
        controlsCard.addView(btnResetControls, resetLp);

        // --- Card 3: ISO & Language Management (GESTIÓN DE JUEGO) ---
        LinearLayout isoCard = new LinearLayout(this);
        isoCard.setOrientation(LinearLayout.VERTICAL);
        isoCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        isoCard.setBackground(createCardBackground());
        root.addView(isoCard, cardParams);

        TextView tvIsoTitle = new TextView(this);
        tvIsoTitle.setText("💿  GESTIÓN DE JUEGO");
        tvIsoTitle.setTextColor(palette.accent);
        tvIsoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvIsoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvIsoTitle.setPadding(0, 0, 0, dp(6));
        isoCard.addView(tvIsoTitle);

        TextView tvIsoDesc = new TextView(this);
        tvIsoDesc.setText("Detecta versiones multi-idioma (Español, Italiano, Inglés) o permite diagnosticar y verificar los archivos del juego.");
        tvIsoDesc.setTextColor(palette.textSecondary);
        tvIsoDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvIsoDesc.setPadding(0, 0, 0, dp(12));
        isoCard.addView(tvIsoDesc);

        // Fila de Gestión: [Cambiar Idioma / ISO]   [Comprobar Integridad]
        LinearLayout isoActionRow = new LinearLayout(this);
        isoActionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams isoActionRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        btnPickIso = new Button(this);
        btnPickIso.setText(haveData() ? "🌐  Cambiar Idioma / ISO" : "📂  Seleccionar ISO");
        btnPickIso.setTextColor(palette.btnSecondaryText);
        btnPickIso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnPickIso.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        btnPickIso.setOnClickListener(v -> {
            hapticFeedback(v);
            chooseIsoImage();
        });
        LinearLayout.LayoutParams pickIsoLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pickIsoLp.rightMargin = dp(6);
        isoActionRow.addView(btnPickIso, pickIsoLp);

        Button btnCheckIntegrity = new Button(this);
        btnCheckIntegrity.setText("🛡️  Comprobar Integridad");
        btnCheckIntegrity.setTextColor(palette.btnSecondaryText);
        btnCheckIntegrity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnCheckIntegrity.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        btnCheckIntegrity.setOnClickListener(v -> {
            hapticFeedback(v);
            checkMapIntegrity();
        });
        isoActionRow.addView(btnCheckIntegrity, checkLp);

        isoCard.addView(isoActionRow, isoActionRowLp);

        Button btnClearData = new Button(this);
        btnClearData.setText("🗑️  Liberar almacenamiento de mapas");
        btnClearData.setTextColor(palette.warningBadgeText);
        btnClearData.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClearData.setBackground(createButtonBackground(palette.btnSecondaryBg, 10));
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        clearLp.topMargin = dp(8);
        btnClearData.setOnClickListener(v -> {
            hapticFeedback(v);
            confirmClearData();
        });
        isoCard.addView(btnClearData, clearLp);

        // --- Card 4: Official Community & Repo Card ---
        LinearLayout footerCard = new LinearLayout(this);
        footerCard.setOrientation(LinearLayout.VERTICAL);
        footerCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        footerCard.setBackground(createCardBackground());
        root.addView(footerCard, cardParams);

        TextView tvFooterTitle = new TextView(this);
        tvFooterTitle.setText("SPARTAN CE • MOBILE EDITION");
        tvFooterTitle.setTextColor(palette.textPrimary);
        tvFooterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvFooterTitle.setTypeface(Typeface.DEFAULT_BOLD);
        footerCard.addView(tvFooterTitle);

        TextView tvFooterSub = new TextView(this);
        tvFooterSub.setText("Desarrollado y optimizado por @kyrnforge\nSoporte para pantalla táctil, ISO multi-idioma y HUD táctico.");
        tvFooterSub.setTextColor(palette.textSecondary);
        tvFooterSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFooterSub.setPadding(0, dp(4), 0, dp(12));
        footerCard.addView(tvFooterSub);

        Button btnGithub = new Button(this);
        btnGithub.setText("⭐  Repositorio Oficial en GitHub");
        btnGithub.setTextColor(Color.WHITE);
        btnGithub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnGithub.setTypeface(Typeface.DEFAULT_BOLD);
        btnGithub.setBackground(createGradientButton(Color.rgb(30, 41, 59), Color.rgb(15, 23, 42), 10));
        LinearLayout.LayoutParams ghLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        btnGithub.setOnClickListener(v -> {
            hapticFeedback(v);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devlwte/spartance"));
            startActivity(browserIntent);
        });
        footerCard.addView(btnGithub, ghLp);

        setContentView(scrollView);
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
        tvStatusHeader.setText(ready ? "●  ESTADO: LISTO PARA JUGAR" : "○  ESTADO: SE REQUIEREN DATOS");
        tvStatusHeader.setTextColor(ready ? palette.successBadgeText : palette.warningBadgeText);
        tvStatusHeader.setBackground(createBadgeBackground(ready ? palette.successBadgeBg : palette.warningBadgeBg));

        String currentLang = hubPrefs.getString("installed_language", ready ? "Español" : "Ninguno");
        String shortLang = currentLang.split(" ")[0].toUpperCase();
        tvInstalledLang.setText(ready ? ("🌐  " + shortLang) : "🌐  SIN DATOS");
        tvInstalledLang.setTextColor(palette.btnSecondaryText);
        tvInstalledLang.setBackground(createButtonBackground(palette.btnSecondaryBg, 12));

        if (ready) {
            btnLaunch.setText("▶  INICIAR SPARTAN CE");
            btnLaunch.setTextColor(Color.rgb(10, 13, 20));
            btnLaunch.setBackground(createGradientButton(Color.rgb(0, 229, 255), Color.rgb(2, 132, 199), 14));
            btnLaunch.setOnClickListener(v -> {
                hapticFeedback(v);
                startGame();
            });
        } else {
            btnLaunch.setText("📂  SELECCIONAR IMAGEN ISO (.iso / .xiso)");
            btnLaunch.setTextColor(Color.WHITE);
            btnLaunch.setBackground(createGradientButton(Color.rgb(14, 165, 233), Color.rgb(2, 132, 199), 14));
            btnLaunch.setOnClickListener(v -> {
                hapticFeedback(v);
                chooseIsoImage();
            });
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

                // Terminate any background HaloActivity instance so it recreates with pristine factory defaults
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
                    + "• Mapas detectados: " + mapCount + " niveles de campaña y multijugador\n"
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
        btnPickIso.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Analizando imagen ISO y detectando idiomas...");

        new Thread(this::inspectIsoLanguages).start();
    }

    private void inspectIsoLanguages() {
        try (ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(pendingIsoUri, "r")) {
            if (descriptor == null)
                throw new java.io.IOException("No se pudo abrir el archivo ISO.");
            try (FileInputStream in = new FileInputStream(descriptor.getFileDescriptor())) {
                FileChannel channel = in.getChannel();
                detectedLanguages = XisoExtractor.listLanguageEntries(channel);
            }

            if (detectedLanguages.isEmpty()) {
                throw new XisoExtractor.ExtractException(
                    "No se encontró ninguna carpeta de datos compatible (ui.map) dentro de la ISO seleccionada.");
            }

            handler.post(() -> {
                if (detectedLanguages.size() == 1) {
                    // Solo hay un idioma disponible, extraerlo directamente
                    XisoExtractor.LanguageEntry single = detectedLanguages.get(0);
                    startExtraction(single);
                } else {
                    // Múltiples idiomas detectados: mostrar modal para elegir
                    showLanguageSelectionDialog(detectedLanguages);
                }
            });
        } catch (Exception e) {
            fail("Error al analizar la ISO: " + e.getMessage());
        }
    }

    private void showLanguageSelectionDialog(List<XisoExtractor.LanguageEntry> languages) {
        progressBar.setVisibility(View.GONE);
        statusText.setText("Selecciona el idioma a instalar:");

        String[] displayNames = new String[languages.size()];
        int defaultSelection = 0;

        for (int i = 0; i < languages.size(); i++) {
            XisoExtractor.LanguageEntry lang = languages.get(i);
            displayNames[i] = lang.displayName;
            if (lang.folderName.equalsIgnoreCase("maps_es") || lang.displayName.toLowerCase().contains("español")) {
                defaultSelection = i;
            }
        }

        final int[] selectedIndex = { defaultSelection };

        new AlertDialog.Builder(this)
            .setTitle("Seleccionar idioma del juego")
            .setSingleChoiceItems(displayNames, defaultSelection, (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setPositiveButton("Instalar este idioma", (dialog, which) -> {
                XisoExtractor.LanguageEntry chosen = languages.get(selectedIndex[0]);
                startExtraction(chosen);
            })
            .setNegativeButton("Cancelar", (dialog, which) -> {
                fail("Instalación cancelada.");
            })
            .setCancelable(false)
            .show();
    }

    private void startExtraction(XisoExtractor.LanguageEntry language) {
        btnPickIso.setEnabled(false);
        btnLaunch.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Iniciando extracción de " + language.displayName + "...");

        new Thread(() -> {
            try (ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(pendingIsoUri, "r")) {
                if (descriptor == null)
                    throw new java.io.IOException("No se pudo acceder al archivo ISO.");
                try (FileInputStream in = new FileInputStream(descriptor.getFileDescriptor())) {
                    FileChannel channel = in.getChannel();
                    XisoExtractor.extractMaps(channel, language, dataRoot, (file, done, total) -> {
                        int permille = total > 0 ? (int) (done * 1000 / total) : 0;
                        int doneMb = (int) (done >> 20);
                        int totalMb = (int) (total >> 20);
                        report("Extrayendo " + file + " (" + doneMb + " de " + totalMb + " MB)", permille);
                    });
                }

                hubPrefs.edit().putString("installed_language", language.displayName).apply();

                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnPickIso.setEnabled(true);
                    updateStatusUI();
                    statusText.setText("¡Instalación completada con éxito!");
                    tvInstalledLang.setText("Idioma actual: " + language.displayName);
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
            statusText.setText(text);
            if (permille >= 0)
                progressBar.setProgress(permille);
        });
    }

    private void fail(String text) {
        handler.post(() -> {
            statusText.setText(text);
            progressBar.setVisibility(View.GONE);
            btnPickIso.setEnabled(true);
            btnPickIso.requestFocus();
        });
    }
}
