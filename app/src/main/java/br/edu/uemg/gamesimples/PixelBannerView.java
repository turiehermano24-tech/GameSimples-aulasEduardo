package br.edu.uemg.gamesimples;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Arte original em uma grade de pixels; sem fontes ou imagens externas. */
public final class PixelBannerView extends View {
    private final Paint ink = new Paint();
    private static final String LETTERS = "ACEGMNO";
    private static final String[] GLYPHS = {
            "01110/11011/11011/11111/11011/11011/11011",
            "01111/11000/11000/11000/11000/11000/01111",
            "11111/11000/11000/11110/11000/11000/11111",
            "01111/11000/11000/11011/11011/11011/01111",
            "11011/11111/11111/11011/11011/11011/11011",
            "11001/11101/11101/11011/11011/11011/11001",
            "01110/11011/11011/11011/11011/11011/01110"
    };

    public PixelBannerView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int pixel = Math.max(1, Math.min(getWidth() / 112, getHeight() / 80));
        canvas.save();
        canvas.translate(Math.round((getWidth() - pixel * 112) / 2f),
                Math.round((getHeight() - pixel * 80) / 2f));
        canvas.scale(pixel, pixel);
        word(canvas, "CANNON", 5, 5, 3, 0xFF7A354F);
        word(canvas, "CANNON", 2, 2, 3, 0xFFFFD477);
        word(canvas, "GAME", 13, 30, 4, 0xFF1E5371);
        word(canvas, "GAME", 10, 27, 4, 0xFF5CE4CE);
        // Pequena arena em pixel art com plataforma, trilha do tiro e alvos.
        block(canvas, 2, 77, 108, 2, 0xFF273755);
        block(canvas, 2, 76, 108, 1, 0xFF437E88);
        block(canvas, 7, 72, 25, 4, 0xFF344967);
        block(canvas, 9, 68, 15, 5, 0xFF5CE4CE);
        block(canvas, 18, 64, 12, 5, 0xFF5CE4CE);
        block(canvas, 26, 60, 9, 5, 0xFF5CE4CE);
        block(canvas, 27, 60, 8, 1, 0xFFB1FFEB);
        block(canvas, 10, 72, 5, 4, 0xFFFFD477);
        block(canvas, 24, 72, 5, 4, 0xFFFFD477);
        for (int i = 0; i < 4; i++) block(canvas, 43 + i * 6, 63, 2, 1, 0xFF527082);
        block(canvas, 66, 62, 3, 3, 0xFFFFF0B6);
        for (int i = 0; i < 4; i++) {
            int x = 77 + i * 8;
            int y = 60 + (i % 2) * 5;
            block(canvas, x + 1, y + 1, 5, 10, 0xFF793A52);
            block(canvas, x, y, 5, 10, i % 2 == 0 ? 0xFFFF847E : 0xFFFFD477);
            block(canvas, x, y, 2, 7, 0xFFFFE9AB);
        }
        canvas.restore();
    }

    private void word(Canvas canvas, String text, int x, int y, int cell, int color) {
        for (int i = 0; i < text.length(); i++) {
            String glyph = GLYPHS[LETTERS.indexOf(text.charAt(i))];
            for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++)
                if (glyph.charAt(row * 6 + col) == '1')
                    block(canvas, x + i * 6 * cell + col * cell, y + row * cell, cell, cell, color);
        }
    }

    private void block(Canvas canvas, int x, int y, int w, int h, int color) {
        ink.setColor(color);
        canvas.drawRect(x, y, x + w, y + h, ink);
    }
}
