package com.halo.decomp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Standalone tactical HUD Editor for Spartan CE.
 * Engineered for 100% unobstructed view, pixel-perfect scaling, and zero off-screen buttons.
 */
public class HudEditorActivity extends Activity {
    private static final String PREFS_TOUCH = "touch-layout-v1";

    private SharedPreferences touchPrefs;
    private TouchLayout layout;
    private float controlsOpacity = 1.0f;
    private float sensitivity = 2.0f;

    private HudCanvasView canvasView;
    private FrameLayout rootLayout;
    private LinearLayout topBar;
    private LinearLayout bottomControlPanel;
    private LinearLayout emptyHintPanel;
    private Button btnToggleUi;

    private TextView tvSelectedButton;
    private TextView tvSizeValue;
    private SeekBar sbSize;
    private Button btnToggleVisibility;
    private TextView tvOpacityValue;

    private int selectedControl = -1;
    private boolean uiHidden = false;

    private static final String[] CONTROL_NAMES = {
        "A / Saltar", "B / Golpe", "X / Recargar", "Y / Cambiar Arma",
        "Disparo Principal", "Granada", "Agacharse", "Zoom / Mira",
        "Linterna", "Tipo de Granada", "Pausa", "Atrás",
        "Cruceta Arriba", "Cruceta Abajo", "Cruceta Izquierda", "Cruceta Derecha",
        "Palanca Movimiento", "Disparo Izquierdo", "Modo Cámara"
    };

    private static final String[] PICKER_OPTIONS = {
        "🎮 Cruceta D-Pad",
        "🕹️ Palanca de Movimiento",
        "🅰️ A / Saltar",
        "🅱️ B / Golpe cuerpo a cuerpo",
        "✖️ X / Recargar arma",
        "🆈 Y / Cambiar de arma",
        "🎯 Disparo Principal (Gatillo Der.)",
        "💣 Lanzar Granada (Gatillo Izq.)",
        "🛡️ Agacharse",
        "🔭 Zoom / Mira telescópica",
        "🔦 Linterna táctica",
        "🔄 Cambiar Tipo de Granada",
        "⏸️ Pausa",
        "⬅️ Atrás / Menú",
        "🔫 Disparo Secundario / Izq.",
        "📷 Alternar Cámara"
    };

    private static final int[] PICKER_INDICES = {
        12, 16, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 17, 18
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TouchIcons.init(this);

        touchPrefs = getSharedPreferences(PREFS_TOUCH, Context.MODE_PRIVATE);
        controlsOpacity = touchPrefs.getFloat("controls-opacity", 1.0f);
        sensitivity = touchPrefs.getFloat("look-sensitivity", 2.0f);

        loadLayout();

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.rgb(8, 12, 20)); // Obsidian #080C14

        // 1. Fullscreen Interactive HUD Canvas
        canvasView = new HudCanvasView(this);
        rootLayout.addView(canvasView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Compact Floating Top-Left Header (Leaves Top-Center completely open for Pause/Back/Camera)
        topBar = buildTopBar();
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
            dp(250), ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP | Gravity.START;
        topLp.setMargins(dp(14), dp(10), 0, 0);
        rootLayout.addView(topBar, topLp);

        // 3. Compact Floating Inspector Card (Positioned in Center Safe Zone)
        bottomControlPanel = buildBottomPanel();
        FrameLayout.LayoutParams inspectorLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inspectorLp.gravity = Gravity.CENTER;
        bottomControlPanel.setVisibility(View.GONE);
        rootLayout.addView(bottomControlPanel, inspectorLp);

        // 4. Center Hint Pill when no button is selected
        emptyHintPanel = buildEmptyHintPanel();
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.gravity = Gravity.CENTER;
        rootLayout.addView(emptyHintPanel, hintLp);

        // 5. Floating toggle button if UI is hidden
        btnToggleUi = new Button(this);
        btnToggleUi.setText("👁 Mostrar Editor");
        btnToggleUi.setTextColor(Color.rgb(0, 229, 255));
        btnToggleUi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnToggleUi.setPadding(dp(12), dp(4), dp(12), dp(4));
        btnToggleUi.setMinHeight(0);
        btnToggleUi.setMinimumHeight(0);
        btnToggleUi.setMinWidth(0);
        btnToggleUi.setMinimumWidth(0);
        btnToggleUi.setIncludeFontPadding(false);
        btnToggleUi.setBackground(createCardBg(Color.argb(220, 10, 15, 24), Color.argb(140, 0, 229, 255), 12));
        btnToggleUi.setVisibility(View.GONE);
        btnToggleUi.setOnClickListener(v -> toggleUiVisibility());
        FrameLayout.LayoutParams toggleLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        toggleLp.gravity = Gravity.TOP | Gravity.START;
        toggleLp.setMargins(dp(14), dp(10), 0, 0);
        rootLayout.addView(btnToggleUi, toggleLp);

        setContentView(rootLayout);
    }

    private void loadLayout() {
        String configuration = touchPrefs.getString("configuration", null);
        if (configuration != null) {
            try {
                TouchLayout.Configuration saved = TouchLayout.importConfiguration(configuration);
                layout = saved.layout;
                sensitivity = saved.sensitivity;
            } catch (Exception e) {
                layout = new TouchLayout();
            }
        } else {
            layout = new TouchLayout();
        }
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
            getResources().getDisplayMetrics());
    }

    private GradientDrawable createCardBg(int bgColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), strokeColor);
        return gd;
    }

    private GradientDrawable createGradientBtn(int colorStart, int colorEnd, int radiusDp) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[] { colorStart, colorEnd });
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private Button createTacticalButton(String text, int textColor, float textSizeSp, int bgColor, int strokeColor, int radiusDp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        btn.setPadding(0, 0, 0, 0);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setIncludeFontPadding(false);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(createCardBg(bgColor, strokeColor, radiusDp));
        return btn;
    }

    private Button createTacticalGradientBtn(String text, int textColor, float textSizeSp, int colorStart, int colorEnd, int radiusDp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(0, 0, 0, 0);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setIncludeFontPadding(false);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(createGradientBtn(colorStart, colorEnd, radiusDp));
        return btn;
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setBackground(createCardBg(Color.argb(235, 10, 15, 24), Color.argb(120, 0, 229, 255), 12));

        // Fila 1: Botones principales
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        Button btnCancel = createTacticalButton("✕ Salir", Color.rgb(203, 213, 225), 10,
            Color.argb(120, 30, 41, 59), Color.argb(60, 255, 255, 255), 6);
        btnCancel.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(30), 1.0f);
        cancelLp.rightMargin = dp(4);
        row1.addView(btnCancel, cancelLp);

        Button btnSave = createTacticalGradientBtn("💾 Guardar", Color.rgb(10, 15, 24), 10,
            Color.rgb(0, 229, 255), Color.rgb(2, 132, 199), 6);
        btnSave.setOnClickListener(v -> saveAndExit());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(30), 1.2f);
        saveLp.rightMargin = dp(4);
        row1.addView(btnSave, saveLp);

        Button btnReset = createTacticalButton("↺ Fábrica", Color.rgb(245, 158, 11), 10,
            Color.argb(80, 245, 158, 11), Color.argb(120, 245, 158, 11), 6);
        btnReset.setOnClickListener(v -> confirmReset());
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, dp(30), 1.0f);
        row1.addView(btnReset, resetLp);

        bar.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Fila 2: [👁 Ocultar] [Opac: 100%] [====o====]
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2Lp.topMargin = dp(5);
        row2.setLayoutParams(r2Lp);

        Button btnHide = createTacticalButton("👁 Ocultar", Color.rgb(148, 163, 184), 9,
            Color.argb(90, 30, 41, 59), Color.argb(50, 255, 255, 255), 6);
        btnHide.setOnClickListener(v -> toggleUiVisibility());
        LinearLayout.LayoutParams hideLp = new LinearLayout.LayoutParams(dp(62), dp(24));
        hideLp.rightMargin = dp(6);
        row2.addView(btnHide, hideLp);

        tvOpacityValue = new TextView(this);
        tvOpacityValue.setText("Opac: " + Math.round(controlsOpacity * 100) + "%");
        tvOpacityValue.setTextColor(Color.rgb(148, 163, 184));
        tvOpacityValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvOpacityValue.setPadding(0, 0, dp(4), 0);
        row2.addView(tvOpacityValue);

        SeekBar sbOpacity = new SeekBar(this);
        sbOpacity.setMax(80); // 20% a 100%
        sbOpacity.setProgress((int) Math.round((controlsOpacity - 0.2f) * 100));
        sbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    controlsOpacity = 0.2f + (progress / 100.0f);
                    tvOpacityValue.setText("Opac: " + Math.round(controlsOpacity * 100) + "%");
                    canvasView.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams sbOpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row2.addView(sbOpacity, sbOpLp);

        bar.addView(row2);
        return bar;
    }

    private LinearLayout buildEmptyHintPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(14), dp(8), dp(14), dp(8));
        panel.setBackground(createCardBg(Color.argb(210, 10, 15, 24), Color.argb(80, 0, 229, 255), 12));

        TextView tvHint = new TextView(this);
        tvHint.setText("👆 Toca cualquier botón en pantalla");
        tvHint.setTextColor(Color.rgb(148, 163, 184));
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        panel.addView(tvHint);

        Button btnChoose = createTacticalButton("🔍 Elegir botón...", Color.rgb(0, 229, 255), 11,
            Color.argb(120, 30, 41, 59), Color.argb(70, 0, 229, 255), 8);
        btnChoose.setPadding(dp(10), 0, dp(10), 0);
        btnChoose.setOnClickListener(v -> showButtonPickerDialog());
        LinearLayout.LayoutParams chooseLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        chooseLp.leftMargin = dp(10);
        panel.addView(btnChoose, chooseLp);

        return panel;
    }

    private LinearLayout buildBottomPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(8), dp(10), dp(8));
        panel.setBackground(createCardBg(Color.argb(240, 10, 15, 24), Color.argb(120, 0, 229, 255), 14));

        // --- FILA 1: Botones anterior/siguiente, Nombre, Visibilidad, Lista, Listo ---
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        Button btnPrev = createTacticalButton("◀", Color.WHITE, 12,
            Color.argb(120, 30, 41, 59), Color.argb(70, 255, 255, 255), 6);
        btnPrev.setOnClickListener(v -> cycleControl(-1));
        row1.addView(btnPrev, new LinearLayout.LayoutParams(dp(26), dp(26)));

        Button btnNext = createTacticalButton("▶", Color.WHITE, 12,
            Color.argb(120, 30, 41, 59), Color.argb(70, 255, 255, 255), 6);
        btnNext.setOnClickListener(v -> cycleControl(1));
        LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(dp(26), dp(26));
        nextLp.leftMargin = dp(4);
        nextLp.rightMargin = dp(6);
        row1.addView(btnNext, nextLp);

        // Nombre del botón
        tvSelectedButton = new TextView(this);
        tvSelectedButton.setTextColor(Color.rgb(0, 229, 255));
        tvSelectedButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSelectedButton.setTypeface(Typeface.DEFAULT_BOLD);
        tvSelectedButton.setPadding(0, 0, dp(8), 0);
        row1.addView(tvSelectedButton);

        // Botón Ocultar / Mostrar
        btnToggleVisibility = createTacticalButton("👁 Visible", Color.rgb(52, 211, 153), 10,
            Color.argb(100, 30, 41, 59), Color.argb(50, 255, 255, 255), 6);
        btnToggleVisibility.setPadding(dp(6), 0, dp(6), 0);
        btnToggleVisibility.setOnClickListener(v -> toggleSelectedVisibility());
        LinearLayout.LayoutParams togLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
        togLp.rightMargin = dp(6);
        row1.addView(btnToggleVisibility, togLp);

        // Botón Elegir botón de la lista
        Button btnList = createTacticalButton("🔍 Lista", Color.rgb(203, 213, 225), 10,
            Color.argb(100, 30, 41, 59), Color.argb(50, 255, 255, 255), 6);
        btnList.setPadding(dp(6), 0, dp(6), 0);
        btnList.setOnClickListener(v -> showButtonPickerDialog());
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
        listLp.rightMargin = dp(6);
        row1.addView(btnList, listLp);

        // Botón Listo / Deseleccionar
        Button btnDone = createTacticalButton("✓ Ok", Color.rgb(203, 213, 225), 10,
            Color.argb(80, 30, 41, 59), Color.argb(40, 255, 255, 255), 6);
        btnDone.setPadding(dp(8), 0, dp(8), 0);
        btnDone.setOnClickListener(v -> {
            selectedControl = -1;
            updateSelectionUI();
            canvasView.invalidate();
        });
        row1.addView(btnDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)));

        panel.addView(row1);

        // --- FILA 2: Control de Tamaño y Micro-Ajuste de posición ---
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row2Lp.topMargin = dp(6);
        row2.setLayoutParams(row2Lp);

        tvSizeValue = new TextView(this);
        tvSizeValue.setTextColor(Color.WHITE);
        tvSizeValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvSizeValue.setPadding(0, 0, dp(4), 0);
        row2.addView(tvSizeValue);

        Button btnMinus = createTacticalButton("-", Color.WHITE, 14,
            Color.argb(120, 30, 41, 59), Color.argb(60, 255, 255, 255), 6);
        btnMinus.setTypeface(Typeface.DEFAULT_BOLD);
        btnMinus.setOnClickListener(v -> adjustSelectedSize(-0.05f));
        row2.addView(btnMinus, new LinearLayout.LayoutParams(dp(24), dp(24)));

        sbSize = new SeekBar(this);
        sbSize.setMax(150); // 50% a 200%
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && selectedControl >= 0 && selectedControl < layout.size()) {
                    float newSize = 0.5f + (progress / 100.0f);
                    applySizeToSelected(newSize);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.leftMargin = dp(2);
        sbLp.rightMargin = dp(2);
        row2.addView(sbSize, sbLp);

        Button btnPlus = createTacticalButton("+", Color.WHITE, 14,
            Color.argb(120, 30, 41, 59), Color.argb(60, 255, 255, 255), 6);
        btnPlus.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlus.setOnClickListener(v -> adjustSelectedSize(0.05f));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        plusLp.rightMargin = dp(10);
        row2.addView(btnPlus, plusLp);

        // Micro-Ajustes con flechas tácticas
        TextView tvNudgeLabel = new TextView(this);
        tvNudgeLabel.setText("Mover:");
        tvNudgeLabel.setTextColor(Color.rgb(148, 163, 184));
        tvNudgeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvNudgeLabel.setPadding(0, 0, dp(4), 0);
        row2.addView(tvNudgeLabel);

        Button btnLeft = createTacticalButton("◀", Color.WHITE, 10,
            Color.argb(100, 30, 41, 59), Color.argb(40, 255, 255, 255), 6);
        btnLeft.setOnClickListener(v -> nudgeSelected(-3f, 0));
        row2.addView(btnLeft, new LinearLayout.LayoutParams(dp(24), dp(24)));

        Button btnUp = createTacticalButton("▲", Color.WHITE, 10,
            Color.argb(100, 30, 41, 59), Color.argb(40, 255, 255, 255), 6);
        btnUp.setOnClickListener(v -> nudgeSelected(0, -3f));
        LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        upLp.leftMargin = dp(2);
        row2.addView(btnUp, upLp);

        Button btnDown = createTacticalButton("▼", Color.WHITE, 10,
            Color.argb(100, 30, 41, 59), Color.argb(40, 255, 255, 255), 6);
        btnDown.setOnClickListener(v -> nudgeSelected(0, 3f));
        LinearLayout.LayoutParams dnLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        dnLp.leftMargin = dp(2);
        row2.addView(btnDown, dnLp);

        Button btnRight = createTacticalButton("▶", Color.WHITE, 10,
            Color.argb(100, 30, 41, 59), Color.argb(40, 255, 255, 255), 6);
        btnRight.setOnClickListener(v -> nudgeSelected(3f, 0));
        LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        rtLp.leftMargin = dp(2);
        row2.addView(btnRight, rtLp);

        panel.addView(row2);
        return panel;
    }

    private void toggleUiVisibility() {
        uiHidden = !uiHidden;
        if (uiHidden) {
            topBar.setVisibility(View.GONE);
            bottomControlPanel.setVisibility(View.GONE);
            emptyHintPanel.setVisibility(View.GONE);
            btnToggleUi.setVisibility(View.VISIBLE);
        } else {
            topBar.setVisibility(View.VISIBLE);
            btnToggleUi.setVisibility(View.GONE);
            updateSelectionUI();
        }
    }

    private void showButtonPickerDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Seleccionar botón para editar")
            .setItems(PICKER_OPTIONS, (dialog, which) -> {
                int targetType = PICKER_INDICES[which];
                // Buscar control correspondiente
                int found = -1;
                for (int i = 0; i < layout.size(); i++) {
                    if (layout.type(i) == targetType) {
                        found = i;
                        break;
                    }
                }
                if (found >= 0) {
                    selectedControl = found;
                    updateSelectionUI();
                    canvasView.invalidate();
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void cycleControl(int direction) {
        if (layout.size() == 0) return;
        if (selectedControl < 0) {
            selectedControl = 0;
        } else {
            selectedControl = (selectedControl + direction + layout.size()) % layout.size();
        }
        updateSelectionUI();
        canvasView.invalidate();
    }

    private void nudgeSelected(float dx, float dy) {
        if (selectedControl < 0 || selectedControl >= layout.size()) return;
        int type = layout.type(selectedControl);
        if (type >= 12 && type <= 15) {
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t >= 12 && t <= 15) {
                    layout.move(i, layout.x(i) + dx, layout.y(i) + dy);
                }
            }
        } else {
            layout.move(selectedControl, layout.x(selectedControl) + dx, layout.y(selectedControl) + dy);
        }
        canvasView.invalidate();
    }

    private void updateSelectionUI() {
        if (uiHidden) return;
        if (selectedControl < 0 || selectedControl >= layout.size()) {
            bottomControlPanel.setVisibility(View.GONE);
            emptyHintPanel.setVisibility(View.VISIBLE);
            return;
        }

        emptyHintPanel.setVisibility(View.GONE);
        bottomControlPanel.setVisibility(View.VISIBLE);

        int type = layout.type(selectedControl);
        boolean isDpad = (type >= 12 && type <= 15);

        String name = isDpad ? "🎮 CRUCETA D-PAD" : ("🎮 " + (type < CONTROL_NAMES.length ? CONTROL_NAMES[type] : "Control " + selectedControl));
        tvSelectedButton.setText(name);

        float currentSize = layout.sizeScale(selectedControl);
        int percent = Math.round(currentSize * 100);
        tvSizeValue.setText("Tam: " + percent + "%");
        sbSize.setProgress(Math.max(0, Math.min(150, Math.round((currentSize - 0.5f) * 100))));

        boolean shown = layout.shown(selectedControl);
        btnToggleVisibility.setText(shown ? "👁 Visible" : "🚫 Oculto");
        btnToggleVisibility.setTextColor(shown ? Color.rgb(52, 211, 153) : Color.rgb(248, 113, 113));

        // Ajuste adaptativo de posición del panel: si el botón está abajo, coloca el panel arriba
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bottomControlPanel.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        lp.topMargin = 0;
        lp.bottomMargin = 0;
        bottomControlPanel.setLayoutParams(lp);
    }

    private void applySizeToSelected(float newSize) {
        if (selectedControl < 0 || selectedControl >= layout.size()) return;
        newSize = Math.max(0.5f, Math.min(2.0f, newSize));
        int type = layout.type(selectedControl);

        if (type >= 12 && type <= 15) {
            int up = -1, down = -1, left = -1, right = -1;
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t == 12 && up < 0) up = i;
                else if (t == 13 && down < 0) down = i;
                else if (t == 14 && left < 0) left = i;
                else if (t == 15 && right < 0) right = i;
            }
            float cx = (left >= 0 && right >= 0) ? (layout.x(left) + layout.x(right)) / 2f : layout.x(selectedControl);
            float cy = (up >= 0 && down >= 0) ? (layout.y(up) + layout.y(down)) / 2f : layout.y(selectedControl);
            float offset = 28f * newSize;

            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t == 12) { layout.setSize(i, newSize); layout.move(i, cx, cy - offset); }
                else if (t == 13) { layout.setSize(i, newSize); layout.move(i, cx, cy + offset); }
                else if (t == 14) { layout.setSize(i, newSize); layout.move(i, cx - offset, cy); }
                else if (t == 15) { layout.setSize(i, newSize); layout.move(i, cx + offset, cy); }
            }
        } else {
            layout.setSize(selectedControl, newSize);
        }

        updateSelectionUI();
        canvasView.invalidate();
    }

    private void adjustSelectedSize(float delta) {
        if (selectedControl < 0 || selectedControl >= layout.size()) return;
        float currentSize = layout.sizeScale(selectedControl);
        applySizeToSelected(currentSize + delta);
    }

    private void toggleSelectedVisibility() {
        if (selectedControl < 0 || selectedControl >= layout.size()) return;
        int type = layout.type(selectedControl);
        boolean isDpad = (type >= 12 && type <= 15);

        if (isDpad) {
            boolean anyShown = false;
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t >= 12 && t <= 15 && layout.shown(i)) anyShown = true;
            }
            boolean target = !anyShown;
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t >= 12 && t <= 15) layout.setShown(i, target);
            }
        } else {
            layout.setShown(selectedControl, !layout.shown(selectedControl));
        }

        updateSelectionUI();
        canvasView.invalidate();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
            .setTitle("↺ Restablecer posiciones de fábrica")
            .setMessage("¿Deseas restaurar la posición y tamaño predeterminados de fábrica de todos los controles virtuales?")
            .setPositiveButton("Restablecer", (dialog, which) -> {
                layout = new TouchLayout();
                canvasView.setupBounds();
                selectedControl = -1;
                updateSelectionUI();
                canvasView.invalidate();
                Toast.makeText(this, "↺ Controles restablecidos a valores de fábrica.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void saveAndExit() {
        try {
            String exported = layout.exportConfiguration(sensitivity);
            SharedPreferences.Editor editor = touchPrefs.edit();
            editor.putString("configuration", exported);
            editor.putFloat("controls-opacity", controlsOpacity);
            editor.putFloat("look-sensitivity", sensitivity);
            editor.putBoolean("swipe-layout", true);

            // Limpiar claves legacy individuales
            for (int i = 0; i < TouchLayout.MAX_CONTROLS; i++) {
                editor.remove("x" + i);
                editor.remove("y" + i);
            }
            editor.commit();

            if (HaloActivity.instance != null) {
                HaloActivity.instance.finish();
            }

            Toast.makeText(this, "✅ Configuración guardada correctamente.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar configuración: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // --- Custom Canvas View ---
    private class HudCanvasView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scale = 1.0f;
        float logicalWidth = 960, logicalHeight = 540;

        private boolean isDragging = false;
        private float dragStartX = 0, dragStartY = 0;

        public HudCanvasView(Context context) {
            super(context);
        }

        void setupBounds() {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            float width = Math.max(1, getWidth()), height = Math.max(1, getHeight());
            // Escala exacta idéntica al motor del juego:
            scale = Math.max(0.01f, Math.min(width / 960f, height / 540f));
            logicalWidth = width / scale;
            logicalHeight = height / scale;
            // Adapta las coordenadas a las dimensiones lógicas completas sin offset
            layout.bounds(logicalWidth, logicalHeight);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            setupBounds();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float rawX = event.getX();
            float rawY = event.getY();
            float lx = rawX / scale;
            float ly = rawY / scale;

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                int hit = findControlHit(lx, ly);
                if (hit >= 0) {
                    selectedControl = hit;
                    isDragging = true;
                    dragStartX = lx - layout.x(hit);
                    dragStartY = ly - layout.y(hit);
                    if (bottomControlPanel != null) bottomControlPanel.setAlpha(0.25f);
                    updateSelectionUI();
                    invalidate();
                    return true;
                } else {
                    if (selectedControl >= 0) {
                        selectedControl = -1;
                        updateSelectionUI();
                        invalidate();
                    }
                }
            } else if (action == MotionEvent.ACTION_MOVE && isDragging && selectedControl >= 0) {
                float targetX = lx - dragStartX;
                float targetY = ly - dragStartY;

                int type = layout.type(selectedControl);
                if (type >= 12 && type <= 15) {
                    // Mover todos los brazos de la cruceta juntos de forma unificada
                    float dx = targetX - layout.x(selectedControl);
                    float dy = targetY - layout.y(selectedControl);
                    for (int i = 0; i < layout.size(); i++) {
                        int t = layout.type(i);
                        if (t >= 12 && t <= 15) {
                            layout.move(i, layout.x(i) + dx, layout.y(i) + dy);
                        }
                    }
                } else {
                    layout.move(selectedControl, targetX, targetY);
                }
                invalidate();
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDragging = false;
                if (bottomControlPanel != null) bottomControlPanel.setAlpha(1.0f);
            }
            return super.onTouchEvent(event);
        }

        private int findControlHit(float x, float y) {
            // Dar prioridad al control ya seleccionado si se toca sobre él
            if (selectedControl >= 0 && selectedControl < layout.size()) {
                float scx = layout.x(selectedControl), scy = layout.y(selectedControl), scr = layout.radius(selectedControl);
                if ((x - scx) * (x - scx) + (y - scy) * (y - scy) <= scr * scr * 1.5f) {
                    return selectedControl;
                }
            }
            for (int i = 0; i < layout.size(); i++) {
                float cx = layout.x(i);
                float cy = layout.y(i);
                float r = layout.radius(i);
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r * 1.4f) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            TouchIcons.opacity = controlsOpacity;

            // 1. Cuadrícula táctica en coordenadas de pantalla
            drawTacticalGrid(canvas);

            canvas.save();
            canvas.scale(scale, scale);

            // 2. Palanca de movimiento (LEFT)
            if (layout.size() > TouchLayout.LEFT) {
                drawMovementStick(canvas);
            }

            // 3. Cruceta unificada (D-Pad)
            drawUnifiedDpad(canvas);

            // 4. Todos los demás botones estándar
            for (int i = 0; i < layout.size(); i++) {
                int type = layout.type(i);
                if (type == TouchLayout.LEFT || (type >= 12 && type <= 15)) continue;

                float x = layout.x(i);
                float y = layout.y(i);
                float r = layout.radius(i);
                boolean isSelected = (selectedControl == i);
                boolean shown = layout.shown(i);

                drawStandardButton(canvas, i, type, x, y, r, isSelected, shown);
            }

            // 5. Retícula de selección activa
            if (selectedControl >= 0 && selectedControl < layout.size()) {
                int selType = layout.type(selectedControl);
                if (selType >= 12 && selType <= 15) {
                    drawDpadSelectionRing(canvas);
                }
            }

            canvas.restore();
        }

        private void drawTacticalGrid(Canvas canvas) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.0f);
            paint.setColor(Color.argb(16, 0, 229, 255)); // Faint cyan grid

            float step = 40.0f * scale;
            for (float x = 0; x < getWidth(); x += step) {
                canvas.drawLine(x, 0, x, getHeight(), paint);
            }
            for (float y = 0; y < getHeight(); y += step) {
                canvas.drawLine(0, y, getWidth(), y, paint);
            }

            // Marcas en las esquinas
            paint.setStrokeWidth(2.0f);
            paint.setColor(Color.argb(80, 0, 229, 255));
            float w = getWidth(), h = getHeight();
            float len = 20.0f * scale;
            // Top-left
            canvas.drawLine(12, 12, 12 + len, 12, paint);
            canvas.drawLine(12, 12, 12, 12 + len, paint);
            // Top-right
            canvas.drawLine(w - 12, 12, w - 12 - len, 12, paint);
            canvas.drawLine(w - 12, 12, w - 12, 12 + len, paint);
            // Bottom-left
            canvas.drawLine(12, h - 12, 12 + len, h - 12, paint);
            canvas.drawLine(12, h - 12, 12, h - 12 - len, paint);
            // Bottom-right
            canvas.drawLine(w - 12, h - 12, w - 12 - len, h - 12, paint);
            canvas.drawLine(w - 12, h - 12, w - 12, h - 12 - len, paint);
        }

        private void drawMovementStick(Canvas canvas) {
            float x = layout.x(TouchLayout.LEFT);
            float y = layout.y(TouchLayout.LEFT);
            float r = layout.radius(TouchLayout.LEFT);
            float capR = 24 * layout.sizeScale(TouchLayout.LEFT);
            boolean isSelected = (selectedControl == TouchLayout.LEFT);
            int a = Math.max(15, (int)(255 * controlsOpacity));

            // Base housing cup (dark recessed Xbox ring)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(a, 26, 26, 26));
            canvas.drawCircle(x, y, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(isSelected ? 3.0f : 2.0f);
            paint.setColor(isSelected ? Color.rgb(0, 229, 255) : Color.argb(a, 50, 50, 50));
            canvas.drawCircle(x, y, r, paint);

            // Xbox Thumbstick cap
            if (TouchIcons.drawStick != null) {
                int ir = Math.round(capR * 1.15f);
                TouchIcons.drawStick.setBounds((int)(x - ir), (int)(y - ir), (int)(x + ir), (int)(y + ir));
                TouchIcons.drawStick.setAlpha(a);
                TouchIcons.drawStick.draw(canvas);
            } else {
                drawXboxDisc(canvas, x, y, capR, isSelected);
            }

            if (isSelected) {
                drawSelectionIndicator(canvas, x, y, r * 1.25f);
            }
        }

        private void drawUnifiedDpad(Canvas canvas) {
            int up = -1, down = -1, left = -1, right = -1;
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t == 12 && up < 0) up = i;
                else if (t == 13 && down < 0) down = i;
                else if (t == 14 && left < 0) left = i;
                else if (t == 15 && right < 0) right = i;
            }
            if (up < 0 || down < 0 || left < 0 || right < 0) return;

            float cx = (layout.x(left) + layout.x(right)) / 2f;
            float cy = (layout.y(up) + layout.y(down)) / 2f;
            float r = (layout.radius(up) + layout.radius(down) + layout.radius(left) + layout.radius(right)) / 4f;
            float hw = r * 0.76f;

            float x0 = layout.x(left) - r;
            float x1 = cx - hw;
            float x2 = cx + hw;
            float x3 = layout.x(right) + r;

            float y0 = layout.y(up) - r;
            float y1 = cy - hw;
            float y2 = cy + hw;
            float y3 = layout.y(down) + r;

            if (TouchIcons.drawDpad != null) {
                int a = Math.max(20, (int)(255 * controlsOpacity));
                TouchIcons.drawDpad.setBounds((int)x0, (int)y0, (int)x3, (int)y3);
                TouchIcons.drawDpad.setAlpha(a);
                TouchIcons.drawDpad.draw(canvas);
                return;
            }

            Path crossPath = new Path();
            crossPath.moveTo(x1, y0); crossPath.lineTo(x2, y0); crossPath.lineTo(x2, y1);
            crossPath.lineTo(x3, y1); crossPath.lineTo(x3, y2); crossPath.lineTo(x2, y2);
            crossPath.lineTo(x2, y3); crossPath.lineTo(x1, y3); crossPath.lineTo(x1, y2);
            crossPath.lineTo(x0, y2); crossPath.lineTo(x0, y1); crossPath.lineTo(x1, y1);
            crossPath.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(Math.max(15, (int)(255 * controlsOpacity)), 42, 42, 42));
            canvas.drawPath(crossPath, paint);
        }

        private void drawDpadSelectionRing(Canvas canvas) {
            int up = -1, down = -1, left = -1, right = -1;
            for (int i = 0; i < layout.size(); i++) {
                int t = layout.type(i);
                if (t == 12 && up < 0) up = i;
                else if (t == 13 && down < 0) down = i;
                else if (t == 14 && left < 0) left = i;
                else if (t == 15 && right < 0) right = i;
            }
            if (left >= 0 && right >= 0 && up >= 0 && down >= 0) {
                float cx = (layout.x(left) + layout.x(right)) / 2f;
                float cy = (layout.y(up) + layout.y(down)) / 2f;
                float r = (layout.x(right) - layout.x(left)) * 0.85f;
                drawSelectionIndicator(canvas, cx, cy, r);
            }
        }

        private void drawXboxDisc(Canvas canvas, float x, float y, float radius, boolean isSelected) {
            paint.setStyle(Paint.Style.FILL);
            int a = Math.max(15, (int)(255 * controlsOpacity));

            // 1. Dark Xbox Button Base Disc (#2a2a2a)
            paint.setColor(Color.argb(a, 42, 42, 42));
            canvas.drawCircle(x, y, radius, paint);

            // 2. Beveled Top Circle (#383838 / #525252 stroke)
            float topR = radius * 0.88f;
            float topOffsetY = radius * 0.04f;
            paint.setColor(Color.argb(a, isSelected ? 75 : 56, isSelected ? 75 : 56, isSelected ? 75 : 56));
            canvas.drawCircle(x, y - topOffsetY, topR, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, radius * 0.06f));
            paint.setColor(Color.argb(a, isSelected ? 170 : 85, isSelected ? 170 : 85, isSelected ? 170 : 85));
            canvas.drawCircle(x, y - topOffsetY, topR, paint);

            // 3. Subtle dark outer rim
            paint.setStrokeWidth(1.5f);
            paint.setColor(Color.argb((int)(a * 0.8f), 26, 26, 26));
            canvas.drawCircle(x, y, radius, paint);
        }

        private void drawStandardButton(Canvas canvas, int index, int type, float x, float y, float radius, boolean isSelected, boolean shown) {
            if (!shown) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.2f);
                paint.setColor(Color.argb(70, 248, 113, 113));
                canvas.drawCircle(x, y, radius, paint);
                TouchIcons.draw(canvas, paint, type, x, y, radius * 0.65f, false);
                return;
            }

            if (type <= 3 || type == 10 || type == 11) {
                if (isSelected) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3.0f);
                    paint.setColor(Color.rgb(0, 229, 255));
                    canvas.drawCircle(x, y, radius + 2, paint);
                } else {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1.5f);
                    int aStroke = Math.max(15, (int)(150 * controlsOpacity));
                    paint.setColor(Color.argb(aStroke, 255, 255, 255));
                    canvas.drawCircle(x, y, radius, paint);
                }
                TouchIcons.draw(canvas, paint, type, x, y, radius, isSelected);
            } else {
                drawXboxDisc(canvas, x, y, radius, isSelected);
                TouchIcons.draw(canvas, paint, type, x, y, radius * 0.72f, isSelected);
            }

            if (isSelected) {
                drawSelectionIndicator(canvas, x, y, radius * 1.3f);
            }
        }

        private void drawSelectionIndicator(Canvas canvas, float cx, float cy, float radius) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);
            paint.setColor(Color.rgb(0, 229, 255));
            canvas.drawCircle(cx, cy, radius, paint);

            float tick = 8.0f;
            canvas.drawLine(cx - radius - tick, cy, cx - radius + tick, cy, paint);
            canvas.drawLine(cx + radius - tick, cy, cx + radius + tick, cy, paint);
            canvas.drawLine(cx, cy - radius - tick, cx, cy - radius + tick, paint);
            canvas.drawLine(cx, cy + radius - tick, cx, cy + radius + tick, paint);
        }
    }
}
