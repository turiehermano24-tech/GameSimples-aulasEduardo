package br.edu.uemg.gamesimples;

public class Blocker extends GameElement {

    private int missPenalty;   // quantos segundos este escudo rouba

    // construtor
    public Blocker(CannonView view, int color, int missPenalty, int x,
                   int y, int width, int length, float velocityY) {
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length,
                velocityY);
        this.missPenalty = missPenalty;
    }

    // devolve quantos segundos este escudo rouba
    public int getMissPenalty() {
        return missPenalty;
    }
}