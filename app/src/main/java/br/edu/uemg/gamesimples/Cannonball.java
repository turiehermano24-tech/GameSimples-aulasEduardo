package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.Rect;

public class Cannonball extends GameElement {

    private float velocityX;   // velocidade de ir pra direita/esquerda
    private boolean onScreen;  // "a bola ainda está na tela?"

    // construtor: recebe o RAIO e monta a "caixa quadrada" da bola
    public Cannonball(CannonView view, int color, int soundId, int x,
                      int y, int radius, float velocityX, float velocityY) {
        super(view, color, soundId, x, y,
                2 * radius, 2 * radius, velocityY);
        this.velocityX = velocityX;
        onScreen = true;
    }

    // mede o raio da bola (metade do lado da "caixa")
    private int getRadius() {
        return (shape.right - shape.left) / 2;
    }

    // a bola esbarrou em algo E ainda está indo pra frente?
    public boolean collidesWith(GameElement element) {
        return (Rect.intersects(shape, element.shape) && velocityX > 0);
    }

    // ainda dá pra ver a bola na tela?
    public boolean isOnScreen() {
        return onScreen;
    }

    // dá meia-volta na horizontal (bateu no escudo e volta)
    public void reverseVelocityX() {
        velocityX *= -1;
    }

    // updates the Cannonball's position
    @Override
    public void update(double interval) {
        super.update(interval);                      // [L1] deixa o molde cuidar do sobe-e-desce
        shape.offset((int) (velocityX * interval), 0); // [L2] empurra a bola pra direita

        // se a bola escapou por alguma das 4 bordas da mesa...
        if (shape.top < 0 || shape.left < 0 ||
                shape.bottom > view.getScreenHeight() ||
                shape.right > view.getScreenWidth())
            onScreen = false;   // ...desliga o sinalizador: pode esquecer ela

    }
    // desenha a bola como círculo (em vez de caixa)
    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(), shape.top + getRadius(),
                getRadius(), paint);
    }
}