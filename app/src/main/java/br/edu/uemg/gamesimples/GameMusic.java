package br.edu.uemg.gamesimples;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Trilha contínua no menu e na partida, enquanto o aplicativo está em primeiro plano. */
final class GameMusic {
    // Ganho da trilha; o volume de mídia do aparelho continua sob controle do jogador.
    private static final float MUSIC_VOLUME = .2f;
    private final Context context;
    private final SharedPreferences preferences;
    private final AudioManager audioManager;
    private final AudioAttributes attributes;
    private final AudioManager.OnAudioFocusChangeListener focusListener = this::onFocusChanged;
    private AudioFocusRequest focusRequest;
    private MediaPlayer player;
    private boolean active;
    private boolean enabled;
    private boolean focusHeld;
    private boolean focusRequested;
    private boolean released;

    GameMusic(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE);
        enabled = preferences.getBoolean("menu_music", true);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(focusListener, new Handler(Looper.getMainLooper()))
                    .build();
        }
    }

    boolean isEnabled() { return enabled; }
    boolean isPlaying() { return player != null && player.isPlaying(); }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        preferences.edit().putBoolean("menu_music", enabled).apply();
        updatePlayback();
    }

    void setActive(boolean active) {
        this.active = active;
        updatePlayback();
    }

    private void updatePlayback() {
        if (released) return;
        if (!active || !enabled) {
            pause();
            abandonFocus();
            return;
        }
        if (!focusRequested) {
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                result = audioManager.requestAudioFocus(focusRequest);
            else
                result = audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            focusRequested = focusHeld;
        }
        if (!focusHeld) return;
        if (player == null) {
            // Faixa fornecida: the_mountain-game-game-music-508018.mp3.
            player = MediaPlayer.create(context, R.raw.menu_theme, attributes, 0);
            if (player == null) { abandonFocus(); return; }
            player.setLooping(true);
            player.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
            player.setOnErrorListener((failed, what, extra) -> {
                failed.release();
                if (player == failed) player = null;
                abandonFocus();
                return true;
            });
        }
        if (!player.isPlaying()) player.start();
    }

    private void onFocusChanged(int change) {
        if (released) return;
        if (change == AudioManager.AUDIOFOCUS_GAIN) {
            focusHeld = true;
            updatePlayback();
        } else {
            focusHeld = false;
            pause();
            if (change == AudioManager.AUDIOFOCUS_LOSS) abandonFocus();
        }
    }

    private void pause() {
        if (player != null && player.isPlaying()) player.pause();
    }

    private void abandonFocus() {
        if (focusRequested) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                audioManager.abandonAudioFocusRequest(focusRequest);
            else audioManager.abandonAudioFocus(focusListener);
        }
        focusRequested = false;
        focusHeld = false;
    }

    void release() {
        setActive(false);
        released = true;
        if (player != null) { player.release(); player = null; }
    }
}
