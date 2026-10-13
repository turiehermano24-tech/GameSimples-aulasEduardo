package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;

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
    private float getRadius() {
        return (shape.right - shape.left) / 2;
    }

    // a bola esbarrou em algo E ainda está indo pra frente?
    public boolean collidesWith(GameElement element) {
        return (RectF.intersects(shape, element.shape) && velocityX > 0);
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
        // O projétil não rebate no teto/chão como os alvos.
        shape.offset((float) (velocityX * interval), (float) (velocityY * interval));
        // Só sai quando está totalmente fora. Ao nascer, parte da bola fica no canhão.
        if (shape.bottom < 0 || shape.right < 0 ||
                shape.top > view.getScreenHeight() ||
                shape.left > view.getScreenWidth())
            onScreen = false;

    }

    @Override
    Bundle saveState() {
        Bundle state = super.saveState();
        state.putFloat("velocityX", velocityX / view.getScreenWidth());
        state.putBoolean("onScreen", onScreen);
        return state;
    }

    @Override
    void restoreState(Bundle state) {
        super.restoreState(state);
        // Mantém a bola circular mesmo se a proporção da janela mudar.
        float centerX = shape.centerX();
        float radius = shape.height() / 2;
        shape.left = centerX - radius;
        shape.right = centerX + radius;
        velocityX = state.getFloat("velocityX") * view.getScreenWidth();
        onScreen = state.getBoolean("onScreen");
    }
    // desenha a bola como círculo (em vez de caixa)
    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(), shape.top + getRadius(),
                getRadius(), paint);
    }
}
