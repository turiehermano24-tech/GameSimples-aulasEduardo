package br.edu.uemg.gamesimples;

import android.content.Context;
import android.content.SharedPreferences;

/** Recordes locais separados por modo, preservados ao fechar o aplicativo. */
final class GameRecords {
    private final SharedPreferences preferences;

    GameRecords(Context context) {
        preferences = context.getSharedPreferences("game_records", Context.MODE_PRIVATE);
    }

    int bestScore(boolean arcade) { return preferences.getInt(arcade ? "arcade_score" : "classic_score", 0); }
    int bestLevel() { return preferences.getInt("arcade_level", 0); }

    void record(boolean arcade, int points, int level) {
        SharedPreferences.Editor changes = preferences.edit();
        boolean changed = false;
        if (points > bestScore(arcade)) {
            changes.putInt(arcade ? "arcade_score" : "classic_score", points);
            changed = true;
        }
        if (arcade && level > bestLevel()) {
            changes.putInt("arcade_level", level);
            changed = true;
        }
        if (changed) changes.apply();
    }
}
