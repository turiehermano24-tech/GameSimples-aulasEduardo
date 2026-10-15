package br.edu.uemg.gamesimples;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.rules.ExternalResource;
import java.util.Map;

/** Os cenários com pontuações fabricadas não devem alterar os recordes do jogador. */
public final class RecordIsolationRule extends ExternalResource {
    private SharedPreferences preferences;
    private Map<String, ?> saved;

    @Override protected void before() {
        preferences = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("game_records", Context.MODE_PRIVATE);
        saved = preferences.getAll();
        preferences.edit().clear().commit();
    }

    @Override protected void after() {
        SharedPreferences.Editor restore = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : saved.entrySet())
            restore.putInt(entry.getKey(), (Integer) entry.getValue());
        restore.commit();
    }
}
