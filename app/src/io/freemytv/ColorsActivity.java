package io.freemytv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The home screen's background: 150 dark and 150 light colors in two tabs, picked with the remote, with a live preview. */
public class ColorsActivity extends Activity {
    private static final int COLUMNS = 15, ROWS = 10;
    // The first row: the original twelve, then three neutrals. The other nine rows come from hue x lightness.
    private static final int[] FIRST_ROW = {
            0xFF111113, 0xFF000000, 0xFF0B1A2A, 0xFF0E2A47, 0xFF1B0F2E, 0xFF2A1248,
            0xFF0F2A1D, 0xFF0C2E2A, 0xFF2A0F14, 0xFF3A1A10, 0xFF1F2430, 0xFF262631,
            0xFF050507, 0xFF18181B, 0xFF202024};
    // Saturation and lightness of rows 2 to 10: dark ones get the home screen's light text, light ones its dark text.
    private static final float[][] DARK_TONES = {
            {0.65f, 0.09f}, {0.65f, 0.12f}, {0.65f, 0.15f}, {0.65f, 0.18f}, {0.65f, 0.22f},
            {0.35f, 0.10f}, {0.35f, 0.14f}, {0.35f, 0.18f}, {0.35f, 0.23f}};
    private static final float[][] LIGHT_TONES = {
            {0.85f, 0.93f}, {0.85f, 0.89f}, {0.80f, 0.85f}, {0.75f, 0.81f}, {0.70f, 0.77f},
            {0.45f, 0.95f}, {0.45f, 0.91f}, {0.40f, 0.86f}, {0.40f, 0.80f}};

    private final List<Swatch> swatches = new ArrayList<>();
    private Preview preview;
    private TextView hex, hint;
    private final TextView[] tabs = new TextView[2];
    private int[] darkColors, lightColors;
    private int tab;     // 0 = dark, 1 = light
    private float k = 1;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        k = getResources().getDisplayMetrics().widthPixels / 1920f;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(0xFF000000);
        root.setPadding(px(96), px(56), px(96), px(40));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(Ui.tr("Fundo", "Background", "Fondo"), 44, Ui.brand, 0xFFF5F5F7, 8));
        left.addView(text(Ui.tr("A cor de fundo da tela inicial", "The home screen's background color",
                "El color de fondo de la pantalla de inicio"), 24, Ui.regular, 0x80FFFFFF, 28));
        darkColors = palette(false);
        lightColors = palette(true);
        left.addView(tabBar());
        int[] colors = darkColors;
        for (int row = 0; row < ROWS; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < COLUMNS; col++) {
                Swatch s = new Swatch(this, colors[row * COLUMNS + col]);
                swatches.add(s);
                line.addView(s);
            }
            left.addView(line);
        }
        root.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        preview = new Preview(this);
        right.addView(preview, new LinearLayout.LayoutParams(px(480), px(300)));
        hex = text("", 34, Ui.regular, 0xFFF5F5F7, 4);
        hex.setPadding(0, px(32), 0, 0);
        right.addView(hex);
        hint = text("", 22, Ui.regular, 0x80FFFFFF, 0);
        right.addView(hint);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(px(480), LinearLayout.LayoutParams.MATCH_PARENT);
        rightLp.setMargins(px(72), 0, 0, 0);
        root.addView(right, rightLp);

        setContentView(root);
        int current = Prefs.backgroundColor();
        tab = Prefs.isDark(current) ? 0 : 1;
        showTab(tab);
        Swatch start = swatches.get(0);
        for (Swatch s : swatches) if (s.color == current) start = s;
        refresh();
        start.requestFocus();
        show(start.color);
    }

    /** "Dark" and "Light": focusing one switches the grid, like tabs on Apple TV. */
    private LinearLayout tabBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, 0, 0, px(20));
        String[] names = {Ui.tr("Escuro", "Dark", "Oscuro"), Ui.tr("Claro", "Light", "Claro")};
        for (int i = 0; i < 2; i++) {
            final int index = i;
            TextView t = text(names[i], 28, Ui.medium, 0x66FFFFFF, 0);
            t.setPadding(px(18), px(8), px(18), px(8));
            t.setFocusable(true);
            t.setClickable(true);
            t.setOnClickListener(v -> showTab(index));
            t.setOnFocusChangeListener((v, has) -> { if (has) showTab(index); });
            tabs[i] = t;
            bar.addView(t);
        }
        return bar;
    }

    private void showTab(int index) {
        tab = index;
        int[] colors = index == 0 ? darkColors : lightColors;
        for (int i = 0; i < swatches.size(); i++) swatches.get(i).setColor(colors[i]);
        for (int i = 0; i < 2; i++) {
            tabs[i].setTextColor(i == index ? 0xFFFFFFFF : 0x66FFFFFF);
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(px(24));
            pill.setColor(i == index ? 0x1FFFFFFF : 0);
            tabs[i].setBackground(pill);
        }
        refresh();
    }

    private int px(float v) {
        return Math.round(v * k);
    }

    /** The 150 colors of a tab, row by row; the light first row is whites and soft greys. */
    private static int[] palette(boolean light) {
        int[] all = new int[COLUMNS * ROWS];
        float[][] tones = light ? LIGHT_TONES : DARK_TONES;
        if (light) {
            for (int c = 0; c < COLUMNS; c++) all[c] = hsl(0, 0, 1f - 0.02f * c);
        } else {
            System.arraycopy(FIRST_ROW, 0, all, 0, COLUMNS);
        }
        for (int r = 0; r < tones.length; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                all[(r + 1) * COLUMNS + c] = hsl(c * 360f / COLUMNS, tones[r][0], tones[r][1]);
            }
        }
        return all;
    }

    private static int hsl(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = l - c / 2;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return 0xFF000000 | Math.round((r + m) * 255) << 16 | Math.round((g + m) * 255) << 8 | Math.round((b + m) * 255);
    }

    /** Sizes scale with the screen's width, so the TV's density doesn't change them. */
    private TextView text(String s, int size, android.graphics.Typeface face, int color, int bottom) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k);
        t.setTypeface(face);
        t.setTextColor(color);
        t.setPadding(0, 0, 0, px(bottom));
        return t;
    }

    /** Marks the background in use. */
    private void refresh() {
        int background = Prefs.backgroundColor();
        for (Swatch s : swatches) s.setInUse(s.color == background);
    }

    /** The preview and the hex follow the focused color. */
    private void show(int color) {
        preview.show(color);
        hex.setText(String.format(Locale.US, "#%06X", color & 0xFFFFFF));
        hint.setText(color == Prefs.backgroundColor() ? Ui.tr("Em uso", "In use", "En uso") : "OK");
    }

    /** One color: a rounded square, with a white ring on the one in use and a small lift when focused. */
    private final class Swatch extends View {
        int color;
        private final GradientDrawable fill = new GradientDrawable();

        Swatch(Context c, int color) {
            super(c);
            this.color = color;
            setFocusable(true);
            setClickable(true);
            fill.setShape(GradientDrawable.RECTANGLE);
            fill.setCornerRadius(px(10));
            fill.setColor(color);
            // a faint edge, so the darkest colors don't vanish into the black page
            fill.setStroke(Math.max(1, px(1)), 0x33808080);
            // inset, so the ring of the one in use is drawn inside the box instead of cut off
            setBackground(new InsetDrawable(fill, px(5)));
            setLayoutParams(new LinearLayout.LayoutParams(px(68), px(68)));
            setOnClickListener(v -> {
                Prefs.setBackgroundColor(this.color);
                refresh();
                show(this.color);
            });
            setOnFocusChangeListener((v, has) -> {
                setScaleX(has ? 1.18f : 1f);
                setScaleY(has ? 1.18f : 1f);
                if (has) {
                    show(this.color);
                    // the row is drawn in order, so lift the focused one above its neighbours
                    setElevation(px(8));
                } else {
                    setElevation(0);
                }
            });
        }

        void setColor(int color) {
            this.color = color;
            fill.setColor(color);
            invalidate();
        }

        void setInUse(boolean on) {
            fill.setStroke(on ? Math.max(2, px(3)) : Math.max(1, px(1)), on ? 0xFFFFFFFF : 0x33808080);
            invalidate();
        }
    }

    /** The home screen on the focused color: the name, the date, a clock, and the shelf under them. */
    private final class Preview extends View {
        private final Paint backing = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint plate = new Paint(Paint.ANTI_ALIAS_FLAG), tile = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();

        Preview(Context c) {
            super(c);
            plate.setColor(0x0DFFFFFF);
            tile.setColor(0x26FFFFFF);
            ink.setColor(0xFFFFFFFF);
        }

        private boolean darkInk;

        void show(int background) {
            backing.setColor(background);
            darkInk = !Prefs.isDark(background);
            plate.setColor(darkInk ? 0x0F000000 : 0x0DFFFFFF);
            tile.setColor(darkInk ? 0x1F000000 : 0x26FFFFFF);
            ink.setColor(darkInk ? 0xFF1D1D1F : 0xFFFFFFFF);
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), u = w / 480f;
            box.set(0, 0, w, h);
            c.drawRoundRect(box, 20 * u, 20 * u, backing);
            ink.setAlpha(150);
            c.drawRoundRect(32 * u, 36 * u, 132 * u, 42 * u, 3 * u, 3 * u, ink);
            ink.setAlpha(90);
            c.drawRoundRect(32 * u, 58 * u, 112 * u, 63 * u, 2.5f * u, 2.5f * u, ink);
            ink.setAlpha(235);
            c.drawRoundRect(32 * u, 84 * u, 52 * u, 134 * u, 2 * u, 2 * u, ink);
            c.drawRoundRect(64 * u, 84 * u, 120 * u, 134 * u, 2 * u, 2 * u, ink);
            c.drawRoundRect(132 * u, 84 * u, 188 * u, 134 * u, 2 * u, 2 * u, ink);
            box.set(60 * u, h - 90 * u, w - 60 * u, h - 24 * u);
            c.drawRoundRect(box, 18 * u, 18 * u, plate);
            float tw = (box.width() - 5 * 12 * u) / 4;
            for (int i = 0; i < 4; i++) {
                float x = box.left + 12 * u + i * (tw + 12 * u);
                c.drawRoundRect(x, box.top + 10 * u, x + tw, box.bottom - 10 * u, 7 * u, 7 * u, tile);
            }
        }
    }
}
