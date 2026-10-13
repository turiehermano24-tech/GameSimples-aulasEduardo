package br.edu.uemg.gamesimples;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;

public class Cannon {

    private int baseRadius;          // tamanho da base (a bola na ponta esquerda)
    private int barrelLength;        // comprimento do canudo
    private Point barrelEnd = new Point();  // o pontinho na ponta do canudo
    private double barrelAngle;      // o ângulo que o canudo está apontando
    private Cannonball cannonball;   // a última bola disparada (se ainda estiver voando)
    private Paint paint = new Paint();      // a tinta preta do canhão
    private CannonView view;         // a tela onde o canhão vive

    // construtor: monta o canhão e deixa o canudo apontando reto
    public Cannon(CannonView view, int baseRadius, int barrelLength,
                  int barrelWidth) {
        this.view = view;                          // guarda a tela
        this.baseRadius = baseRadius;              // guarda o tamanho da base
        this.barrelLength = barrelLength;          // guarda o comprimento do canudo
        paint.setStrokeWidth(barrelWidth);         // engrossa o traço do canudo
        paint.setColor(Color.BLACK);               // pinta o canhão de preto
        align(Math.PI / 2);                         // 🔴 aponta o canudo reto pra direita
    }

    // aponta o canudo para o angulo dado (em radianos)
    public void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;                              // [L1] lembra o angulo
        barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));  // [L2] ponta pra direita
        barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle))  // [L3] ponta pra cima/baixo
                + view.getScreenHeight() / 2;                          // [L4] ...medida do meio
    }

    // cria e dispara a bola na direção que o canudo aponta
    public void fireCannonball() {
        // [L1] velocidade da bola no lado horizontal (pra direita)
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * Math.sin(barrelAngle));

        // [L2] velocidade da bola no lado vertical (pra cima/baixo)
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * -Math.cos(barrelAngle));

        // [L3] tamanho (raio) da bola
        int radius = (int) (view.getScreenHeight() *
                CannonView.CANNONBALL_RADIUS_PERCENT);

        // [L4] fabrica a bola e a esconde dentro do cano
        cannonball = new Cannonball(view, Color.BLACK,
                CannonView.CANNON_SOUND_ID, -radius,
                view.getScreenHeight() / 2 - radius, radius, velocityX,
                velocityY);

        // [L5] toca o "pum" do tiro
        cannonball.playSound();
    }

    // desenha o canhao na folha (cano + base)
    public void draw(Canvas canvas) {
        // desenha o canudo: uma linha do meio da parede esquerda ate a ponta
        canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x,
                barrelEnd.y, paint);

        // desenha a base: um circulo com o centro na borda esquerda,
        // entao so a metade de dentro aparece (o "semicírculo" preto)
        canvas.drawCircle(0, (int) view.getScreenHeight() / 2,
                (int) baseRadius, paint);
    }

    // devolve a bola que o canhao disparou (ou "nada" se nao tem nenhuma voando)
    public Cannonball getCannonball() {
        return cannonball;
    }

    // apaga a bola do bolso do canhao (quando ela sai da tela ou acerta algo)
    public void removeCannonball() {
        cannonball = null;
    }

    Bundle saveState() {
        Bundle state = new Bundle();
        state.putDouble("angle", barrelAngle);
        if (cannonball != null && cannonball.isOnScreen())
            state.putBundle("ball", cannonball.saveState());
        return state;
    }

    void restoreState(Bundle state) {
        align(state.getDouble("angle", Math.PI / 2));
        Bundle ball = state.getBundle("ball");
        if (ball != null) {
            cannonball = new Cannonball(view, Color.BLACK,
                    CannonView.CANNON_SOUND_ID, 0, 0, 1, 0, 0);
            cannonball.restoreState(ball);
        } else {
            cannonball = null;
        }
    }
}
