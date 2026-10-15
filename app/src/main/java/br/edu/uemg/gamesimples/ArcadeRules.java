package br.edu.uemg.gamesimples;

/** Progressão limitada para manter as fases rápidas, mas ainda jogáveis. */
final class ArcadeRules {
    private ArcadeRules() { }

    static double speedMultiplier(int level) {
        return 1 + Math.min(8, Math.max(0, level - 1)) * .1;
    }

    static double nextLevelTime(double remaining) {
        // Aproveita o tempo conquistado, com piso e teto para evitar partidas triviais.
        return Math.max(10, Math.min(30, remaining));
    }
}
