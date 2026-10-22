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

/** The home screen's background, picked from swatches with the remote, with a preview. */
public class ColorsActivity extends Activity {
    // Dark, so the home screen's light text stays readable on any of them.
    private static final int[] BACKGROUNDS = {
            0xFF111113, 0xFF000000, 0xFF0B1A2A, 0xFF0E2A47, 0xFF1B0F2E, 0xFF2A1248,
            0xFF0F2A1D, 0xFF0C2E2A, 0xFF2A0F14, 0xFF3A1A10, 0xFF1F2430, 0xFF262631};

    private final List<Swatch> swatches = new ArrayList<>();
    private Preview preview;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(0xFF0A0A0C);
        root.setPadding(96, 72, 96, 72);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(Ui.tr("Fundo", "Background", "Fondo"), 56, Ui.displaySemibold, 0xFFF5F5F7));
        left.addView(text(Ui.tr("A cor de fundo da tela inicial", "The home screen's background color",
                "El color de fondo de la pantalla de inicio"), 26, Ui.regular, 0xFF8E8E93));
        for (int row = 0; row < BACKGROUNDS.length / 6; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(0, 40, 0, 0);
            for (int col = 0; col < 6; col++) {
                Swatch s = new Swatch(this, BACKGROUNDS[row * 6 + col]);
                swatches.add(s);
                line.addView(s);
            }
            left.addView(line);
        }
        root.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.25f));

        preview = new Preview(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        previewLp.setMargins(64, 0, 0, 0);
        root.addView(preview, previewLp);

        setContentView(root);
        refresh();
        if (!swatches.isEmpty()) swatches.get(0).requestFocus();
    }

    /** Sizes in pixels, so the TV's density doesn't change them. */
    private TextView text(String s, int px, android.graphics.Typeface face, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(face);
        t.setTextColor(color);
        t.setPadding(0, 0, 0, 16);
        return t;
    }

    /** Marks the background in use and redraws the preview. */
    private void refresh() {
        int background = Prefs.backgroundColor();
        for (Swatch s : swatches) s.setInUse(s.color == background);
        preview.show(background);
    }

    /** One color: a rounded square, with a white ring on the one in use and a lift when focused. */
    private final class Swatch extends View {
        final int color;
        private final GradientDrawable fill = new GradientDrawable();

        Swatch(Context c, int color) {
            super(c);
            this.color = color;
            setFocusable(true);
            setClickable(true);
            fill.setShape(GradientDrawable.RECTANGLE);
            fill.setCornerRadius(18 * getResources().getDisplayMetrics().density);
            fill.setColor(color);
            // inset, so the white ring of the one in use is drawn inside the box instead of cut off
            setBackground(new InsetDrawable(fill, 6));
            setLayoutParams(new LinearLayout.LayoutParams(124, 104));
            setOnClickListener(v -> {
                Prefs.setBackgroundColor(color);
                refresh();
            });
            setOnFocusChangeListener((v, has) -> {
                setScaleX(has ? 1.1f : 1f);
                setScaleY(has ? 1.1f : 1f);
            });
        }

        void setInUse(boolean on) {
            fill.setStroke(on ? 6 : 0, 0xFFFFFFFF);
            invalidate();
        }
    }

    /** The home screen's glass box on the chosen background: a name bar, a date bar and three cards under it. */
    private final class Preview extends View {
        private final Paint backing = new Paint(Paint.ANTI_ALIAS_FLAG), glass = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), card = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF frame = new RectF(), box = new RectF(), tile = new RectF();

        Preview(Context c) {
            super(c);
            glass.setColor(0x1AFFFFFF);
            bar.setColor(0x80FFFFFF);
            card.setColor(0x14FFFFFF);
        }

        void show(int background) {
            backing.setColor(background);
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float d = getResources().getDisplayMetrics().density;
            float w = getWidth(), h = getHeight(), r = 28 * d;
            frame.set(0, 0, w, h);
            c.drawRoundRect(frame, r, r, backing);
            box.set(40 * d, 40 * d, w - 40 * d, h * 0.6f);
            c.drawRoundRect(box, 24 * d, 24 * d, glass);
            float x = 72 * d;
            c.drawRoundRect(x, 92 * d, x + 200 * d, 108 * d, 8 * d, 8 * d, bar);
            c.drawRoundRect(x, 140 * d, x + 110 * d, 150 * d, 5 * d, 5 * d, bar);
            float cw = (w - 80 * d - 2 * 16 * d) / 3, top = h * 0.68f, ch = cw * 9 / 16;
            for (int i = 0; i < 3; i++) {
                float left = 40 * d + i * (cw + 16 * d);
                tile.set(left, top, left + cw, top + ch);
                c.drawRoundRect(tile, 16 * d, 16 * d, card);
            }
        }
    }
}
