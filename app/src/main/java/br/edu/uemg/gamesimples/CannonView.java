package br.edu.uemg.gamesimples;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final String TAG = "CannonView";        // rótulo p/ log de erro

    // penalidades/bônus de tempo (em segundos)
    public static final int MISS_PENALTY = 2;              // tira 2s ao bater no blocker
    public static final int HIT_REWARD  = 3;               // dá +3s ao destruir um alvo

    // proporções do CANHÃO (fração da tela)
    public static final double CANNON_BASE_RADIUS_PERCENT   = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT  = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // proporções da BOLA DE CANHÃO
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT  = 3.0 / 2;

    // proporções dos ALVOS
    public static final double TARGET_WIDTH_PERCENT        = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT       = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT      = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT      = 1.0 / 60;
    public static final double TARGET_PIECES               = 9;
    public static final double TARGET_MIN_SPEED_PERCENT    = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT    = 6.0 / 4;

    // proporções do BLOQUEADOR
    public static final double BLOCKER_WIDTH_PERCENT  = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT      = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT  = 1.0;

    // tamanho do texto do cronômetro
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;


    private CannonThread cannonThread;     // 🔴 vermelho: a inner class ainda não existe (some na Etapa 13)
    private Activity activity;             // p/ mostrar o diálogo de fim no fio da GUI
    private boolean dialogIsDisplayed = false; // evita reabrir diálogo/ligar fio em cima

    // objetos do jogo
    private Cannon cannon;                 // o canhão (classe já criada vazia → ok)
    private Blocker blocker;               // o bloqueador (ok)
    private ArrayList<Target> targets;     // lista dos 9 alvos (ok)

    // dimensões reais da tela (preenchidas em onSizeChanged)
    private int screenWidth;
    private int screenHeight;

    // estado e estatísticas da partida
    private boolean gameOver;              // o jogo acabou?
    private double timeLeft;               // segundos restantes
    private int shotsFired;                // quantos tiros o usuário deu
    private double totalElapsedTime;       // tempo total decorrido

    // IDs lógicos dos 3 sons (nós que inventamos: 0,1,2)
    public static final int TARGET_SOUND_ID  = 0;
    public static final int CANNON_SOUND_ID  = 1;
    public static final int BLOCKER_SOUND_ID = 2;

    private SoundPool soundPool;           // motor que toca os efeitos
    private SparseIntArray soundMap;       // liga nosso ID (0/1/2) ao ID real do SoundPool

    private Paint textPaint;               // "caneta" do texto do cronômetro
    private Paint backgroundPaint;         // "caneta" que limpa o fundo (branco)

    // constructor
    public CannonView(Context context, AttributeSet attrs){
        super(context, attrs);                       // [L1]
        activity = (Activity) context;               // [L2]
        getHolder().addCallback(this);               // [L3]

        // ---- configuração do motor de som (bloco) ----
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        // ---- carregar os 3 .wav e registrar no mapa (bloco) ----
        soundMap = new SparseIntArray(3);
        soundMap.put(TARGET_SOUND_ID,  soundPool.load(context, R.raw.target_hit,  1));
        soundMap.put(CANNON_SOUND_ID,  soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        // ---- criar as "canetas" de desenho (bloco) ----
        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
    }
}

