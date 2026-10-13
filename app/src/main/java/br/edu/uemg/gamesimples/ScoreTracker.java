package br.edu.uemg.gamesimples;

/** Regras de pontuação independentes do desenho e do Android. */
public final class ScoreTracker {
    public static final int BASE_POINTS = 100;
    public static final int MAX_MULTIPLIER = 5;

    private int points;
    private int hits;
    private int streak;
    private int bestStreak;

    public ScoreTracker() { }

    public ScoreTracker(int points, int hits, int streak, int bestStreak) {
        this.points = points;
        this.hits = hits;
        this.streak = streak;
        this.bestStreak = bestStreak;
    }

    public void registerHit() {
        // Calcula o prêmio antes de avançar a sequência: o primeiro acerto vale 100.
        points += BASE_POINTS * getNextMultiplier();
        hits++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
    }

    public void registerMiss() {
        // Uma falha não retira os pontos nem apaga a melhor sequência da partida.
        streak = 0;
    }

    public int getPoints() { return points; }
    public int getHits() { return hits; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }
    public int getNextMultiplier() { return Math.min(streak + 1, MAX_MULTIPLIER); }

    public int getAccuracy(int shotsFired) {
        return shotsFired == 0 ? 0 : Math.round(100f * hits / shotsFired);
    }
}
