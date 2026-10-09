package br.edu.uemg.gamesimples;

public class Target extends GameElement {

    private int hitReward;   // quantos segundos este alvo te dá

    // construtor
    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length,
                velocityY);
        this.hitReward = hitReward;
    }

    // devolve quantos segundos este alvo te dá
    public int getHitReward() {
        return hitReward;
    }
}