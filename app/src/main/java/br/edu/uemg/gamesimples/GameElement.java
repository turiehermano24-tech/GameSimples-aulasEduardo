package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {

    protected Paint paint = new Paint();   // a tinta desta peça
    protected CannonView view;             // a tela onde a peça vive
    protected Rect shape;                  // o retângulo invisível do lugar dela
    private float velocityY;               // velocidade de sobe-e-desce
    private int soundId;                   // etiqueta do som da peça

    // construtor: recebe as medidas e monta a peça
    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY) {
        this.view = view;                                  // guarda a tela
        paint.setColor(color);                             // pinta com a cor que chegou
        shape = new Rect(x, y, x + width, y + length);     // marca o retângulo
        this.soundId = soundId;                            // guarda a etiqueta do som
        this.velocityY = velocityY;                        // guarda a velocidade
    }

    // atualiza a posição e verifica se bateu no topo/fundo da tela
    public void update(double interval) {
        shape.offset(0, (int) (velocityY * interval));   // [L1] move na vertical

        if (shape.top < 0 && velocityY < 0 ||            // [L2] bateu no teto ou no chão?
                shape.bottom > view.getScreenHeight() && velocityY > 0)
            velocityY *= -1;                              // [L3] se sim, inverte a direção
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