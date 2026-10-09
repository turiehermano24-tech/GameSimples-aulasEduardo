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

    // called when the size of the SurfaceView changes,
    // such as when it's first added to the View hierarchy
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;   // guarda a largura nova da tela
        screenHeight = h;  // guarda a altura nova da tela

        // ajusta o tamanho da letra do cronômetro
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true); // deixa a letra lisa, sem serrilhado
    }

    // devolve a largura da tela (a "fita métrica")
    public int getScreenWidth() {
        return screenWidth;
    }

    // devolve a altura da tela
    public int getScreenHeight() {
        return screenHeight;
    }

    // toca o som que está guardado na gaveta "soundId"
    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    // reset all the screen elements and start a new game
    public void newGame() {
        // fabrica um canhao novo, do tamanho da tela
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random();        // dado pra sortear velocidade dos alvos
        targets = new ArrayList<>();         // lista vazia onde vamos por os 9 alvos

        // x do PRIMEIRO alvo (contando da esquerda)
        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

        // y dos alvos: deixa todos na altura do MEIO da tela
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
                screenHeight);

        // repete 9 vezes, uma por alvo (n = 0,1,2,...,8)
        for (int n = 0; n < TARGET_PIECES; n++) {

            // sorteia uma velocidade "entre mín e máx", escalada pela altura da tela
            double velocity = screenHeight * (random.nextDouble() *
                    (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
                    TARGET_MIN_SPEED_PERCENT);

            // alterna a cor dos alvos entre azul (dark) e amarelo (light)
            int color = (n % 2 == 0) ?
                    getResources().getColor(R.color.dark,
                            getContext().getTheme()) :
                    getResources().getColor(R.color.light,
                            getContext().getTheme());

            velocity *= -1; // inverte o sinal da velocidade sorteada

            // monta o alvo e enfia na fila (lista)
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));
            // anda pra direita: largura do alvo atual + uma fresta de espaco
            targetX += (int) (TARGET_WIDTH_PERCENT * screenWidth +
                    TARGET_SPACING_PERCENT * screenWidth);
        } // fecha a esteira (o laço for) — os 9 alvos ja estao na fila
        // fabrica o ESCUDO (um só) no meio da tela, cor amarela, subindo/descendo
        // fabrica o ESCUDO (um so) no meio da tela, cor PRETA, subindo/descendo
        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        // zera o relogio (10s) e o placar antes de comecar a partida
        timeLeft = 10;             // o cronometro comeca em 10 segundos e DESCE
        shotsFired = 0;            // contador de tiros
        totalElapsedTime = 0.0;    // tempo total decorrido

        // se e um recomeco (a partida anterior tinha acabado), fabrica um motor NOVO
        if (gameOver) {
            gameOver = false;                              // marca que agora esta rolando
            cannonThread = new CannonThread(getHolder());  // motor novo (o velho morreu)
            cannonThread.start();                          // da a partida nele
        }

        hideSystemBars(); // esconde as barrinhas do celular (modo imersivo)
    } // fecha o newGame// fecha o newGame

    // chamado repetidamente pela CannonThread pra mexer nas pecas
    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0;   // passa de milissegundos p/ segundos

        // se existe uma bola voando, da o empurrao nela
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval);   // da o empurrao no escudo

        for (GameElement target : targets)   // da o empurrao em cada um dos alvos
            target.update(interval);
        timeLeft -= interval;   // o relogio "sangra" o tempo que passou
        // se o relogio chegou a zero -> perdeu
        if (timeLeft <= 0) {
            timeLeft = 0.0;                              // trava em 0 (evita negativo na tela)
            gameOver = true;                             // marca que a partida acabou
            cannonThread.setRunning(false);              // desliga o motor
            showGameOverDialog(R.string.lose);           // janelinha "You lose!"
        }

        // se acabaram todos os alvos -> ganhou
        if (targets.isEmpty()) {
            cannonThread.setRunning(false);              // desliga o motor
            showGameOverDialog(R.string.win);            // janelinha "You win!"
            gameOver = true;                             // marca que a partida acabou
        }
    } // fecha o updatePositions

    // mira o canhao no ponto tocado e dispara se nao houver bola na tela
    public void alignAndFireCannonball(MotionEvent event) {
        // [L1] guarda a posicao do dedo como um ponto inteiro (x, y)
        Point touchPoint = new Point((int) event.getX(),
                (int) event.getY());

        // [L2] quanto o dedo esta ACIMA (+) ou ABAIXO (-) do meio da tela
        double centerMinusY = (screenHeight / 2 - touchPoint.y);

        double angle = 0;                               // [L1] prepara o angulo
        angle = Math.atan2(touchPoint.x, centerMinusY); // [L2] o transferidor mede o grau
        cannon.align(angle);                            // [L3] aponta o canudo nesse grau

} // fecha a classe CannonView

