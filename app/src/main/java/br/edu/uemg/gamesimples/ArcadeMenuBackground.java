package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Cenário estático: estrelas, horizonte e moldura de uma máquina arcade. */
final class ArcadeMenuBackground extends Drawable {
    private final Paint ink = new Paint();

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float w = bounds.width(), h = bounds.height();
        float unit = Math.max(1, h / 240f);
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        ink.setColor(0xFF0B1020);
        canvas.drawRect(0, 0, w, h, ink);
        for (int i = 0; i < 42; i++) {
            float x = ((i * 137 + 29) % 997) / 997f * w;
            float y = ((i * 67 + 11) % 241) / 241f * h;
            ink.setColor(i % 4 == 0 ? 0xFF475079 : 0xFF242E4D);
            canvas.drawRect(x, y, x + unit, y + unit, ink);
        }
        ink.setColor(0xFF182541);
        ink.setStrokeWidth(unit);
        float horizon = h * .83f;
        for (int i = -6; i <= 6; i++)
            canvas.drawLine(w / 2 + i * w / 14, horizon, w / 2 + i * w / 6, h, ink);
        for (int i = 0; i < 4; i++) {
            float y = horizon + (h - horizon) * i * i / 9;
            canvas.drawLine(0, y, w, y, ink);
        }
        ink.setColor(0xFF34354F);
        canvas.drawRect(5 * unit, 5 * unit, w - 5 * unit, 6 * unit, ink);
        canvas.drawRect(5 * unit, h - 6 * unit, w - 5 * unit, h - 5 * unit, ink);
        ink.setColor(0xFF5CE4CE);
        for (int corner = 0; corner < 4; corner++) {
            canvas.save();
            canvas.translate(corner % 2 == 0 ? 5 * unit : w - 5 * unit,
                    corner < 2 ? 5 * unit : h - 5 * unit);
            canvas.scale(corner % 2 == 0 ? 1 : -1, corner < 2 ? 1 : -1);
            canvas.drawRect(0, 0, 12 * unit, 2 * unit, ink);
            canvas.drawRect(0, 0, 2 * unit, 8 * unit, ink);
            canvas.restore();
        }
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { ink.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
