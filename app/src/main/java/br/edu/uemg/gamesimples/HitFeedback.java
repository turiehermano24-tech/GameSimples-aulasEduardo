package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Efeito curto em Canvas. O tempo só avança junto com a simulação do jogo. */
final class HitFeedback {
    private final Paint ink = new Paint();
    private String message = "";
    private float x, y;
    private int color;
    private double age = 1;

    void show(String message, float x, float y, int color) {
        this.message = message;
        this.x = x;
        this.y = y;
        this.color = color;
        age = 0;
    }

    void update(double seconds) { age += seconds; }
    void clear() { age = 1; }

    void draw(Canvas canvas, int width, int height) {
        if (age >= .8) return;
        float pixel = Math.max(2, height / 160f);
        ink.setColor(color);
        ink.setAlpha((int) (255 * (1 - age / .8)));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            float distance = (float) (age * height * .15);
            float px = x + (float) Math.cos(angle) * distance;
            float py = y + (float) Math.sin(angle) * distance;
            canvas.drawRect(px, py, px + pixel, py + pixel, ink);
        }
        ink.setAntiAlias(true);
        ink.setTextSize(height * .035f);
        ink.setTextAlign(Paint.Align.CENTER);
        float half = ink.measureText(message) / 2;
        float labelX = Math.max(half + pixel, Math.min(width - half - pixel, x));
        float labelY = Math.max(height * .28f, y - (float) age * height * .08f);
        canvas.drawText(message, labelX, labelY, ink);
        ink.setAntiAlias(false);
        ink.setAlpha(255);
    }
}
