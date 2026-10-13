package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;

public class GameElement {

    protected Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected CannonView view;             // a tela onde a peça vive
    protected RectF shape;                // preserva frações de pixel entre quadros
    protected float velocityY;
    private int soundId;                   // etiqueta do som da peça

    // construtor: recebe as medidas e monta a peça
    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY) {
        this.view = view;                                  // guarda a tela
        paint.setColor(color);                             // pinta com a cor que chegou
        shape = new RectF(x, y, x + width, y + length);
        this.soundId = soundId;                            // guarda a etiqueta do som
        this.velocityY = velocityY;                        // guarda a velocidade
    }

    // atualiza a posição e verifica se bateu no topo/fundo da tela
    public void update(double interval) {
        shape.offset(0, (float) (velocityY * interval));
        if (shape.top < 0 && velocityY < 0) {
            shape.offset(0, -shape.top);
            velocityY *= -1;
        } else if (shape.bottom > view.getScreenHeight() && velocityY > 0) {
            shape.offset(0, view.getScreenHeight() - shape.bottom);
            velocityY *= -1;
        }
    }

    // Coordenadas proporcionais permitem restaurar em uma superfície de outro tamanho.
    Bundle saveState() {
        Bundle state = new Bundle();
        state.putFloat("left", shape.left / view.getScreenWidth());
        state.putFloat("top", shape.top / view.getScreenHeight());
        state.putFloat("width", shape.width() / view.getScreenWidth());
        state.putFloat("height", shape.height() / view.getScreenHeight());
        state.putFloat("velocityY", velocityY / view.getScreenHeight());
        state.putInt("color", paint.getColor());
        return state;
    }

    void restoreState(Bundle state) {
        float x = state.getFloat("left") * view.getScreenWidth();
        float y = state.getFloat("top") * view.getScreenHeight();
        shape.set(x, y, x + state.getFloat("width") * view.getScreenWidth(),
                y + state.getFloat("height") * view.getScreenHeight());
        velocityY = state.getFloat("velocityY") * view.getScreenHeight();
        paint.setColor(state.getInt("color"));
    }

    // carimba esta peça na folha (desenha o retângulo)
    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    // liga pra tela e pede pra tocar o som desta peça
    public void playSound() {
        view.playSound(soundId);
    }

}
