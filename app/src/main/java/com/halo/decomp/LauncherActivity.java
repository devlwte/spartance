package com.halo.decomp;

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
import android.widget.Button;
import android.widget.CheckBox;
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
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Spartan CE - Tactical Console Hub & Launcher.
 * Master Chief Collection (MCC) inspired dark cinematic aesthetic:
 * - Left-aligned vertical tactical menu with sharp frosted cyan highlights.
 * - Atmospheric dark scenic landscape background with high-contrast gradient vignette.
 * - Sleek translucent console cards with sharp 2-3dp corners and thin ice-blue borders.
 * - Dynamic bottom description prompt and controller button hints.
 * - 100% preservation of all engine graphics, HUD customization, and ISO extraction features.
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

    private final String[] TAB_TITLES = {
        "CAMPAÑA / JUGAR",
        "CONFIGURACIÓN GRÁFICA",
        "CONTROLES Y HUD",
        "SISTEMA Y ARCHIVOS"
    };

    private final String[] TAB_DESCRIPTIONS = {
        "Inicia la campaña de Spartan CE o continúa desde tu último punto de control.",
        "Ajusta el campo de visión (FOV), sombreado por píxel, anti-aliasing y sombras dinámicas.",
        "Personaliza la posición del HUD táctil, calibración de mandos, giroscopio y retroceso háptico.",
        "Administra archivos de juego ISO, verifica integridad de datos y limpia almacenamiento."
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
     * Cold dark slate glass surfaces, pure white typography, ice-cyan accents.
     */
    private static class ThemePalette {
        final int bgRoot = Color.rgb(6, 9, 14); // #06090E Deep obsidian
        final int bgCard = Color.argb(215, 10, 16, 26); // Dark translucent slate glass
        final int strokeCard = Color.argb(85, 56, 189, 248); // Subtle 1px tactical ice border
        final int textPrimary = Color.rgb(248, 250, 252); // #F8FAFC Pure crisp white
        final int textSecondary = Color.rgb(148, 163, 184); // #94A3B8 Slate grey
        final int textMuted = Color.rgb(100, 116, 139); // #64748B
        final int accent = Color.rgb(56, 189, 248); // #38BDF8 Tactical Ice Cyan
        final int accentBg = Color.rgb(14, 165, 233); // #0EA5E9
        final int btnSecondaryBg = Color.argb(160, 18, 26, 40);
        final int btnSecondaryBorder = Color.argb(65, 255, 255, 255);
        final int successBadgeBg = Color.argb(55, 16, 185, 129);
        final int successBadgeText = Color.rgb(52, 211, 153);
        final int warningBadgeBg = Color.argb(55, 245, 158, 11);
        final int warningBadgeText = Color.rgb(251, 191, 36);
        final int divider = Color.argb(50, 56, 189, 248);
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(palette.bgCard);
        gd.setCornerRadius(dp(3)); // Sharp console-grade tactical edge
        gd.setStroke(dp(1), palette.strokeCard);
        return gd;
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
                Color.argb(25, 14, 165, 233)
            }
        );
        normal.setCornerRadius(dp(2));
        normal.setStroke(dp(1), Color.argb(90, 56, 189, 248));
        return normal;
    }

    private Drawable createMenuInactiveDrawable() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(Color.argb(45, 255, 255, 255));
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
                buttons[i].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
                buttons[i].setTextColor(palette.textSecondary);
                buttons[i].setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            }
        }
    }

    private GradientDrawable createBadgeBackground(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(3));
        return gd;
    }

    private LinearLayout.LayoutParams createCardParams() {
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        return cardParams;
    }

    /**
     * Builds the MCC Console-style Interface:
     * - Scenic scenic background + vignette.
     * - Header with Logo, Game Title and Gamer/Telemetry Card.
     * - Left-aligned vertical navigation menu with sharp cyan active bar.
     * - Right translucent glass panel with hardware-accelerated transitions.
     * - Bottom contextual helper prompt and button action bar.
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
                Color.argb(135, 6, 9, 14), // Mid scenery breathing room
                Color.argb(225, 6, 9, 14)  // Contrast for right panel
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
                Color.argb(170, 4, 7, 12),
                Color.argb(0, 4, 7, 12),
                Color.argb(180, 4, 7, 12)
            }
        );
        edgeVignette.setBackground(gradVignette);
        rootFrame.addView(edgeVignette, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Main Content Hierarchy
        LinearLayout mainContent = new LinearLayout(this);
        mainContent.setOrientation(LinearLayout.VERTICAL);
        mainContent.setPadding(dp(24), dp(16), dp(24), dp(12));
        rootFrame.addView(mainContent, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- TOP HEADER ROW ---
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(10));
        mainContent.addView(headerRow);

        // Left Branding (M6D Emblem + Title)
        LinearLayout branding = new LinearLayout(this);
        branding.setOrientation(LinearLayout.HORIZONTAL);
        branding.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(branding, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView ivLogo = new ImageView(this);
        ivLogo.setImageResource(R.drawable.m6d_emblem);
        int logoSize = dp(38);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.rightMargin = dp(12);
        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivLogo.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(dp(3));
        logoBg.setColor(Color.argb(200, 10, 16, 26));
        logoBg.setStroke(dp(1), palette.strokeCard);
        ivLogo.setBackground(logoBg);
        branding.addView(ivLogo, logoLp);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        branding.addView(titleCol);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("SPARTAN CE");
        tvTitle.setTextColor(palette.textPrimary);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        tvTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvTitle.setLetterSpacing(0.08f);
        }
        titleCol.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(MotdManager.getCurrentMessage(this));
        tvSubtitle.setTextColor(palette.accent);
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvSubtitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvSubtitle.setLetterSpacing(0.06f);
        }
        titleCol.addView(tvSubtitle);

        // Right Gamer / Telemetry Badge (MCC Profile Card)
        LinearLayout gamerCard = new LinearLayout(this);
        gamerCard.setOrientation(LinearLayout.HORIZONTAL);
        gamerCard.setGravity(Gravity.CENTER_VERTICAL);
        gamerCard.setPadding(dp(12), dp(5), dp(12), dp(5));
        GradientDrawable gcBg = new GradientDrawable();
        gcBg.setColor(Color.argb(175, 12, 18, 28));
        gcBg.setCornerRadius(dp(3));
        gcBg.setStroke(dp(1), Color.argb(70, 56, 189, 248));
        gamerCard.setBackground(gcBg);
        headerRow.addView(gamerCard);

        TextView tvStatusDot = new TextView(this);
        tvStatusDot.setText("● ");
        tvStatusDot.setTextColor(palette.successBadgeText);
        tvStatusDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        gamerCard.addView(tvStatusDot);

        TextView tvGamerTag = new TextView(this);
        tvGamerTag.setText("SPARTAN-117");
        tvGamerTag.setTextColor(palette.textPrimary);
        tvGamerTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvGamerTag.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        gamerCard.addView(tvGamerTag);

        TextView tvSep = new TextView(this);
        tvSep.setText("   |   ");
        tvSep.setTextColor(palette.textMuted);
        tvSep.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        gamerCard.addView(tvSep);

        TextView tvEngineStatus = new TextView(this);
        tvEngineStatus.setText("ARM64 • V1.1.0");
        tvEngineStatus.setTextColor(palette.accent);
        tvEngineStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvEngineStatus.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        gamerCard.addView(tvEngineStatus);

        // --- CENTER SPLIT: LEFT MENU (250dp) + RIGHT CONTENT PANEL (weight 1.0f) ---
        LinearLayout bodyRow = new LinearLayout(this);
        bodyRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        bodyLp.setMargins(0, dp(8), 0, dp(8));
        mainContent.addView(bodyRow, bodyLp);

        // Left Navigation Column (MCC Style)
        LinearLayout leftNavCol = new LinearLayout(this);
        leftNavCol.setOrientation(LinearLayout.VERTICAL);
        leftNavCol.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams leftColLp = new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT);
        bodyRow.addView(leftNavCol, leftColLp);

        menuRows = new LinearLayout[TAB_TITLES.length];
        menuAccents = new View[TAB_TITLES.length];
        menuLabels = new TextView[TAB_TITLES.length];

        for (int i = 0; i < TAB_TITLES.length; i++) {
            final int index = i;
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            itemLp.bottomMargin = dp(6);

            View accentBar = new View(this);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(3.5f), ViewGroup.LayoutParams.MATCH_PARENT);
            itemRow.addView(accentBar, barLp);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(TAB_TITLES[i]);
            tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            tvLabel.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tvLabel.setLetterSpacing(0.08f);
            }
            tvLabel.setPadding(dp(14), 0, dp(12), 0);
            itemRow.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            itemRow.setOnClickListener(v -> {
                hapticFeedback(v);
                selectTab(index, true);
            });

            leftNavCol.addView(itemRow, itemLp);
            menuRows[i] = itemRow;
            menuAccents[i] = accentBar;
            menuLabels[i] = tvLabel;
        }

        // Flexible spacer before Exit button
        View menuSpacer = new View(this);
        leftNavCol.addView(menuSpacer, new LinearLayout.LayoutParams(0, 0, 1.0f));

        // MCC-style Quit Button
        LinearLayout exitRow = new LinearLayout(this);
        exitRow.setOrientation(LinearLayout.HORIZONTAL);
        exitRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        exitRow.setBackground(createMenuInactiveDrawable());

        View exitAccent = new View(this);
        exitAccent.setBackgroundColor(Color.TRANSPARENT);
        exitRow.addView(exitAccent, new LinearLayout.LayoutParams(dp(3.5f), ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvExit = new TextView(this);
        tvExit.setText("SALIR DEL JUEGO");
        tvExit.setTextColor(Color.rgb(180, 80, 80));
        tvExit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
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

        // Right Content Area (Console-Grade Slate Glass Container)
        LinearLayout rightPanelContainer = new LinearLayout(this);
        rightPanelContainer.setOrientation(LinearLayout.VERTICAL);
        rightPanelContainer.setBackground(createCardBackground());
        rightPanelContainer.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams rightPanelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        rightPanelLp.leftMargin = dp(20);
        bodyRow.addView(rightPanelContainer, rightPanelLp);

        pageContainer = new FrameLayout(this);
        rightPanelContainer.addView(pageContainer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pages = new View[4];
        pages[0] = buildPagePlay();
        pages[1] = buildPageGraphics();
        pages[2] = buildPageControls();
        pages[3] = buildPageSystem();

        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == activeTab ? View.VISIBLE : View.GONE);
            pageContainer.addView(pages[i], new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // --- BOTTOM FOOTER: CONTEXTUAL DESCRIPTION + BUTTON PROMPTS ---
        LinearLayout footerRow = new LinearLayout(this);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);
        footerRow.setPadding(dp(4), dp(6), dp(4), dp(2));
        mainContent.addView(footerRow);

        tvFooterDescription = new TextView(this);
        tvFooterDescription.setTextColor(palette.textSecondary);
        tvFooterDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvFooterDescription.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvFooterDescription.setLetterSpacing(0.04f);
        }
        footerRow.addView(tvFooterDescription, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Console Button Prompts (Touch-clickable shortcuts)
        LinearLayout promptsRow = new LinearLayout(this);
        promptsRow.setOrientation(LinearLayout.HORIZONTAL);
        promptsRow.setGravity(Gravity.CENTER_VERTICAL);
        footerRow.addView(promptsRow);

        promptsRow.addView(createFooterActionBadge("Ⓐ INICIAR", v -> {
            hapticFeedback(v);
            if (haveData()) startGame(); else chooseIsoImage();
        }));
        promptsRow.addView(createFooterActionBadge("Ⓧ HUD", v -> {
            hapticFeedback(v);
            startActivity(new Intent(this, HudEditorActivity.class));
        }));
        promptsRow.addView(createFooterActionBadge("Ⓨ ISO", v -> {
            hapticFeedback(v);
            chooseIsoImage();
        }));

        if (activeTab < 0 || activeTab >= pages.length) activeTab = 0;
        selectTab(activeTab, false);

        setContentView(rootFrame);
        updateStatusUI();
    }

    private View createFooterActionBadge(String text, View.OnClickListener listener) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        badge.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        badge.setTextColor(palette.textPrimary);
        badge.setPadding(dp(9), dp(3), dp(9), dp(3));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(160, 18, 26, 40));
        gd.setCornerRadius(dp(2));
        gd.setStroke(dp(1), Color.argb(60, 56, 189, 248));
        badge.setBackground(gd);
        badge.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        badge.setLayoutParams(lp);
        return badge;
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
        sv.setFillViewport(true);
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
        btnGoGfx.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        btnGoCtrl.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        sv.setFillViewport(true);
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
                        aaButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
                aaButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
                        shadowButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
                shadowButtons[j].setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        sv.setFillViewport(true);
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
        btnEditHud.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        btnCalibrateGamepad.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        btnResetControls.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        sv.setFillViewport(true);
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
        btnPickIso.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        btnCheckIntegrity.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
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
        btnClearData.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 40, 30, 20), 2, Color.argb(90, 245, 158, 11)));
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
        btnGithub.setBackground(createButtonDrawable(palette.btnSecondaryBg, Color.argb(220, 28, 40, 60), 2, palette.btnSecondaryBorder));
        LinearLayout.LayoutParams ghLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        btnGithub.setOnClickListener(v -> {
            hapticFeedback(v);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devlwte/spartance"));
            startActivity(browserIntent);
        });
        page.addView(btnGithub, ghLp);

        // [FUTURE_EXTENSION]: Cloudflare Workers / Free API integration space for community maps & comments.
        // Ready for lightweight HTTP GET / POST endpoints with rate limiting & caching.

        return sv;
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
            btnLaunch.setBackground(createGradientButton(Color.rgb(56, 189, 248), Color.rgb(14, 165, 233), 3));
            btnLaunch.setOnClickListener(v -> {
                hapticFeedback(v);
                startGame();
            });
        } else {
            btnLaunch.setText("📂  SELECCIONAR IMAGEN ISO (.ISO / .XISO)");
            btnLaunch.setTextColor(Color.WHITE);
            btnLaunch.setBackground(createGradientButton(Color.rgb(14, 165, 233), Color.rgb(2, 132, 199), 3));
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
