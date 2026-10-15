package br.edu.uemg.gamesimples;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback {


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


    private enum GameStatus { RUNNING, PAUSED, WON, LOST }

    // Todo acesso ao estado da partida usa este mesmo bloqueio, inclusive o toque.
    private final Object stateLock = new Object();
    private final Activity activity;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint();
    private final Paint overlayPaint = new Paint();
    private final RectF pauseButton = new RectF();
    private final RectF pauseMenuButton = new RectF();
    private Runnable onMenuRequested;
    private final SparseIntArray soundMap = new SparseIntArray(3);
    private SoundPool soundPool;
    private CannonThread cannonThread;
    private AlertDialog resultDialog;
    private boolean resultPending;
    private boolean activityResumed;
    private boolean surfaceReady;
    private boolean resourcesReleased;
    private boolean consumeTouchGesture;
    private Bundle pendingState;

    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;
    private int screenWidth;
    private int screenHeight;
    private GameStatus status = GameStatus.RUNNING;
    private double timeLeft;
    private int shotsFired;
    private double totalElapsedTime;
    private ScoreTracker score = new ScoreTracker();
    private boolean arcade;
    private int level = 1;
    private final GameRecords records;
    private final HitFeedback feedback = new HitFeedback();

    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;
        records = new GameRecords(context);
        getHolder().addCallback(this);
        setFocusable(true);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool = new SoundPool.Builder().setMaxStreams(3)
                .setAudioAttributes(audio).build();
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));
        backgroundPaint.setColor(Color.WHITE);
        overlayPaint.setColor(0xCCFFFFFF);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        synchronized (stateLock) {
            Bundle saved = cannon == null ? pendingState : saveGameState();
            screenWidth = w;
            screenHeight = h;
            textPaint.setTextSize((float) (TEXT_SIZE_PERCENT * h));
            float margin = h * 0.04f;
            float buttonHeight = Math.max(48 * getResources().getDisplayMetrics().density,
                    h * 0.12f);
            pauseButton.set(w - Math.max(w * 0.15f, buttonHeight * 2.4f) - margin,
                    margin, w - margin, margin + buttonHeight);
            pauseMenuButton.set(w * .37f, h * .76f, w * .63f, h * .76f + buttonHeight);
            createGame();
            if (saved != null) applySavedState(saved);
            pendingState = null;
        }
        startThreadIfReady();
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    public void playSound(int soundId) {
        // Ganhos por efeito: tiro discreto, acerto destacado e bloqueio intermediário.
        float volume;
        switch (soundId) {
            case CANNON_SOUND_ID: volume = .30f; break;
            case TARGET_SOUND_ID: volume = .50f; break;
            case BLOCKER_SOUND_ID: volume = .40f; break;
            default: return;
        }
        if (soundPool != null)
            soundPool.play(soundMap.get(soundId), volume, volume, 1, 0, 1f);
    }

    // Somente cria os elementos. Iniciar/parar a thread é responsabilidade do ciclo de vida.
    private void createGame() {
        level = 1;
        createRound();
        timeLeft = 10;
        shotsFired = 0;
        score = new ScoreTracker();
        totalElapsedTime = 0;
        status = GameStatus.RUNNING;
    }

    private void createRound() {
        feedback.clear();
        double speed = arcade ? ArcadeRules.speedMultiplier(level) : 1;
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));
        Random random = new Random();
        targets = new ArrayList<>();
        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) * screenHeight);
        for (int n = 0; n < TARGET_PIECES; n++) {
            float velocity = (float) (speed * screenHeight * (TARGET_MIN_SPEED_PERCENT
                    + random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT)));
            int color = getResources().getColor(n % 2 == 0 ? R.color.dark : R.color.light,
                    getContext().getTheme());
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight), -velocity));
            targetX += (int) ((TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth);
        }
        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (speed * BLOCKER_SPEED_PERCENT * screenHeight));
    }

    public void newGame(boolean arcade) {
        synchronized (stateLock) { this.arcade = arcade; }
        newGame();
    }

    public void newGame() {
        synchronized (stateLock) {
            if (screenWidth <= 0 || resourcesReleased) return;
            createGame();
        }
        hideSystemBars();
        startThreadIfReady();
    }

    private void nextLevel() {
        synchronized (stateLock) {
            if (!arcade || status != GameStatus.WON || resourcesReleased) return;
            level++;
            timeLeft = ArcadeRules.nextLevelTime(timeLeft);
            createRound();
            status = GameStatus.RUNNING;
            records.record(true, score.getPoints(), level);
            if (cannonThread != null) cannonThread.resetClock = true;
        }
        startThreadIfReady();
    }

    // Passos pequenos reduzem a chance de atravessar um alvo entre dois desenhos.
    private void updateGame(double elapsedSeconds) {
        double remaining = elapsedSeconds;
        while (remaining > 0 && status == GameStatus.RUNNING) {
            if (timeLeft <= 0) { status = GameStatus.LOST; break; }
            double step = Math.min(Math.min(remaining, 1.0 / 240), timeLeft);
            Cannonball ball = cannon.getCannonball();
            if (ball != null) ball.update(step);
            blocker.update(step);
            for (Target target : targets) target.update(step);
            totalElapsedTime += step;
            timeLeft -= step;
            feedback.update(step);
            testForCollisions();
            if (targets.isEmpty()) status = GameStatus.WON;
            else if (timeLeft <= 0) status = GameStatus.LOST;
            timeLeft = Math.max(0, timeLeft);
            remaining -= step;
        }
    }

    private void testForCollisions() {
        Cannonball ball = cannon.getCannonball();
        if (ball == null) return;
        if (!ball.isOnScreen()) {
            if (score.getStreak() > 0) feedback.show(getResources().getString(R.string.miss_feedback),
                    screenWidth * .3f, screenHeight * .35f, 0xFFB43C43);
            score.registerMiss();
            cannon.removeCannonball();
            return;
        }
        // O bloqueador fica antes dos alvos na trajetória.
        if (ball.collidesWith(blocker)) {
            blocker.playSound();
            ball.reverseVelocityX();
            timeLeft -= blocker.getMissPenalty();
            score.registerMiss();
            feedback.show(getResources().getString(R.string.block_feedback),
                    blocker.shape.centerX(), blocker.shape.centerY(), 0xFFB43C43);
            return;
        }
        for (int n = 0; n < targets.size(); n++) {
            if (ball.collidesWith(targets.get(n))) {
                targets.get(n).playSound();
                timeLeft += targets.get(n).getHitReward();
                int award = ScoreTracker.BASE_POINTS * score.getNextMultiplier();
                score.registerHit();
                records.record(arcade, score.getPoints(), level);
                feedback.show(getResources().getString(R.string.hit_feedback, award),
                        targets.get(n).shape.centerX(), targets.get(n).shape.centerY(), 0xFF116B56);
                targets.remove(n);
                cannon.removeCannonball();
                break;
            }
        }
    }

    private boolean isFinished() {
        return status == GameStatus.WON || status == GameStatus.LOST;
    }

    // Executado no loop com stateLock adquirido; a janela é criada na thread da interface.
    private void requestResultDialog() {
        if (resultPending || resultDialog != null || !activityResumed) return;
        resultPending = true;
        activity.runOnUiThread(() -> {
            synchronized (stateLock) {
                resultPending = false;
                if (!activityResumed || !surfaceReady || resourcesReleased || !isFinished()
                        || resultDialog != null || activity.isFinishing() || activity.isDestroyed()) return;
                records.record(arcade, score.getPoints(), level);
                boolean next = arcade && status == GameStatus.WON;
                String stats = getResources().getString(R.string.results_format,
                        score.getPoints(), score.getHits(), shotsFired,
                        score.getAccuracy(shotsFired), score.getBestStreak(), totalElapsedTime);
                String title = next ? getResources().getString(R.string.level_complete, level)
                        : getResources().getString(status == GameStatus.WON ? R.string.win : R.string.lose);
                String message = next ? getResources().getString(R.string.arcade_results,
                        level, records.bestScore(true), stats)
                        : getResources().getString(R.string.final_record, stats, records.bestScore(arcade));
                resultDialog = new AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(next ? R.string.next_level : R.string.reset_game, (dialog, which) -> {
                            synchronized (stateLock) { resultDialog = null; }
                            if (next) nextLevel();
                            else newGame();
                        })
                        .setNeutralButton(R.string.back_to_menu, (dialog, which) -> {
                            synchronized (stateLock) { resultDialog = null; }
                            if (onMenuRequested != null) onMenuRequested.run();
                        }).create();
                resultDialog.show();
            }
        });
    }

    private void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);
        float textSize = (float) (TEXT_SIZE_PERCENT * screenHeight);
        cannon.draw(canvas);
        Cannonball ball = cannon.getCannonball();
        if (ball != null && ball.isOnScreen()) ball.draw(canvas);
        blocker.draw(canvas);
        for (Target target : targets) target.draw(canvas);
        drawScoreboard(canvas, textSize);
        feedback.draw(canvas, screenWidth, screenHeight);
        if (status == GameStatus.RUNNING) {
            textPaint.setTextSize(textSize);
            textPaint.setColor(0xFF263238);
            canvas.drawRoundRect(pauseButton, 12, 12, textPaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.pause_game), pauseButton.centerX(),
                    pauseButton.centerY() - (textPaint.ascent() + textPaint.descent()) / 2, textPaint);
        } else if (status == GameStatus.PAUSED) {
            canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(textSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.game_paused),
                    screenWidth / 2f, screenHeight * 0.45f, textPaint);
            textPaint.setTextSize(textSize * 0.75f);
            canvas.drawText(getResources().getString(R.string.tap_to_continue),
                    screenWidth / 2f, screenHeight * 0.56f, textPaint);
            drawFittedText(canvas, getResources().getString(R.string.combo_help),
                    screenWidth / 2f, screenHeight * 0.66f,
                    screenWidth * 0.9f, textSize * 0.65f);
            textPaint.setColor(0xFF263238);
            canvas.drawRoundRect(pauseMenuButton, 12, 12, textPaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(textSize * .8f);
            canvas.drawText(getResources().getString(R.string.back_to_menu),
                    pauseMenuButton.centerX(), pauseMenuButton.centerY()
                            - (textPaint.ascent() + textPaint.descent()) / 2, textPaint);
        }
    }

    private void drawScoreboard(Canvas canvas, float textSize) {
        float margin = screenHeight * 0.04f;
        // O placar ocupa a área à esquerda do bloqueador, sem esconder os alvos.
        float availableWidth = screenWidth * (float) BLOCKER_X_PERCENT - 2 * margin;
        float columnWidth = availableWidth / 2;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.BLACK);
        drawFittedText(canvas, getResources().getString(R.string.time_remaining_format, timeLeft),
                margin, screenHeight * 0.09f, columnWidth - margin / 2, textSize);
        drawFittedText(canvas, getResources().getString(R.string.score_format, score.getPoints()),
                margin + columnWidth, screenHeight * 0.09f, columnWidth, textSize);
        textPaint.setColor(score.getStreak() > 0 ? 0xFF1565C0 : 0xFF455A64);
        drawFittedText(canvas, getResources().getString(R.string.combo_format,
                        score.getStreak(), score.getNextMultiplier()),
                margin, screenHeight * 0.16f, availableWidth, textSize * 0.75f);
        if (arcade) {
            textPaint.setColor(0xFF6B4B16);
            drawFittedText(canvas, getResources().getString(R.string.arcade_hud, level, targets.size()),
                    margin, screenHeight * .22f, availableWidth, textSize * .6f);
        }
    }

    private void drawFittedText(Canvas canvas, String text, float x, float y,
                                float maxWidth, float desiredSize) {
        textPaint.setTextSize(desiredSize);
        float measuredWidth = textPaint.measureText(text);
        if (measuredWidth > maxWidth && maxWidth > 0)
            textPaint.setTextSize(desiredSize * maxWidth / measuredWidth);
        canvas.drawText(text, x, y, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        synchronized (stateLock) {
            if (cannon == null || !activityResumed || !surfaceReady || isFinished()) return true;
            if (consumeTouchGesture) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                    consumeTouchGesture = false;
                return true;
            }
            if (status == GameStatus.PAUSED) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (pauseMenuButton.contains(event.getX(), event.getY()) && onMenuRequested != null) {
                        consumeTouchGesture = true;
                        // Não encerrar/join a thread enquanto stateLock está adquirido.
                        post(onMenuRequested);
                        return true;
                    }
                    status = GameStatus.RUNNING;
                    consumeTouchGesture = true;
                    // A retomada não dispara nem cobra o intervalo desde o último quadro pausado.
                    if (cannonThread != null) cannonThread.resetClock = true;
                }
                return true;
            }
            if (pauseButton.contains(event.getX(), event.getY())) {
                if (action == MotionEvent.ACTION_DOWN) {
                    status = GameStatus.PAUSED;
                    consumeTouchGesture = true;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                cannon.align(Math.atan2(Math.max(0, event.getX()), screenHeight / 2.0 - event.getY()));
                Cannonball ball = cannon.getCannonball();
                if (ball == null || !ball.isOnScreen()) {
                    // O toque pode chegar antes do loop remover uma bola que já saiu.
                    if (ball != null) score.registerMiss();
                    cannon.fireCannonball();
                    shotsFired++;
                }
            }
        }
        if (action == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override
    public boolean performClick() { super.performClick(); return true; }

    public void setOnMenuRequestedListener(Runnable listener) { onMenuRequested = listener; }

    public boolean canContinueGame() {
        synchronized (stateLock) {
            if (cannon == null) {
                if (pendingState == null) return false;
                String savedStatus = pendingState.getString("status", "PAUSED");
                return savedStatus.equals("RUNNING") || savedStatus.equals("PAUSED")
                        || (pendingState.getBoolean("arcade") && savedStatus.equals("WON"));
            }
            return !isFinished() || (arcade && status == GameStatus.WON);
        }
    }

    public void continueGame() {
        synchronized (stateLock) {
            if (status == GameStatus.PAUSED) status = GameStatus.RUNNING;
            if (cannonThread != null) cannonThread.resetClock = true;
        }
        resumeGame();
    }

    public void resumeGame() {
        synchronized (stateLock) { activityResumed = true; }
        hideSystemBars();
        startThreadIfReady();
    }

    public void stopGame() {
        synchronized (stateLock) {
            activityResumed = false;
            consumeTouchGesture = false;
            if (cannon != null && status == GameStatus.RUNNING) status = GameStatus.PAUSED;
            if (resultDialog != null) { resultDialog.dismiss(); resultDialog = null; }
        }
        stopThread();
    }

    // Chamados apenas na thread da interface. Nunca aguardar join segurando stateLock.
    private void startThreadIfReady() {
        synchronized (stateLock) {
            if (!activityResumed || !surfaceReady || cannon == null || resourcesReleased
                    || cannonThread != null) return;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }
    }

    private void stopThread() {
        CannonThread thread = cannonThread;
        if (thread == null) return;
        thread.running = false;
        thread.interrupt();
        boolean interrupted = false;
        while (thread.isAlive()) {
            try { thread.join(); }
            catch (InterruptedException e) { interrupted = true; }
        }
        cannonThread = null;
        if (interrupted) Thread.currentThread().interrupt();
    }

    public void releaseResources() {
        stopGame();
        synchronized (stateLock) {
            resourcesReleased = true;
            if (soundPool != null) { soundPool.release(); soundPool = null; }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (stateLock) { surfaceReady = true; }
        startThreadIfReady();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startThreadIfReady();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (stateLock) {
            surfaceReady = false;
            if (cannon != null && status == GameStatus.RUNNING) status = GameStatus.PAUSED;
        }
        stopThread();
    }

    public Bundle saveGameState() {
        synchronized (stateLock) {
            if (cannon == null) return pendingState == null ? null : new Bundle(pendingState);
            Bundle saved = new Bundle();
            saved.putString("status", status.name());
            saved.putBoolean("arcade", arcade);
            saved.putInt("level", level);
            saved.putDouble("timeLeft", timeLeft);
            saved.putInt("shotsFired", shotsFired);
            saved.putDouble("elapsed", totalElapsedTime);
            Bundle points = new Bundle();
            points.putInt("points", score.getPoints());
            points.putInt("hits", score.getHits());
            points.putInt("streak", score.getStreak());
            points.putInt("bestStreak", score.getBestStreak());
            saved.putBundle("score", points);
            saved.putBundle("cannon", cannon.saveState());
            saved.putBundle("blocker", blocker.saveState());
            ArrayList<Bundle> pieces = new ArrayList<>();
            for (Target target : targets) pieces.add(target.saveState());
            saved.putParcelableArrayList("targets", pieces);
            return saved;
        }
    }

    public void restoreGameState(Bundle saved) {
        if (saved == null) return;
        synchronized (stateLock) {
            if (cannon == null) pendingState = new Bundle(saved);
            else applySavedState(saved);
        }
    }

    private void applySavedState(Bundle saved) {
        arcade = saved.getBoolean("arcade", false);
        level = Math.max(1, saved.getInt("level", 1));
        feedback.clear();
        timeLeft = saved.getDouble("timeLeft");
        shotsFired = saved.getInt("shotsFired");
        totalElapsedTime = saved.getDouble("elapsed");
        Bundle points = saved.getBundle("score");
        score = points == null ? new ScoreTracker() : new ScoreTracker(
                points.getInt("points"), points.getInt("hits"),
                points.getInt("streak"), points.getInt("bestStreak"));
        status = GameStatus.valueOf(saved.getString("status", "PAUSED"));
        // Restaurar uma partida ativa sempre dá ao jogador tempo de se preparar.
        if (status == GameStatus.RUNNING) status = GameStatus.PAUSED;
        cannon.restoreState(saved.getBundle("cannon"));
        blocker.restoreState(saved.getBundle("blocker"));
        targets.clear();
        ArrayList<Bundle> pieces = saved.getParcelableArrayList("targets");
        if (pieces != null) for (Bundle piece : pieces) {
            Target target = new Target(this, Color.BLACK, HIT_REWARD, 0, 0, 1, 1, 0);
            target.restoreState(piece);
            targets.add(target);
        }
    }

    private final class CannonThread extends Thread {
        private final SurfaceHolder holder;
        private volatile boolean running = true;
        private boolean resetClock;

        CannonThread(SurfaceHolder holder) {
            super("CannonThread");
            this.holder = holder;
        }

        @Override
        public void run() {
            long previous = System.nanoTime();
            while (running) {
                long frameStart = System.nanoTime();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    synchronized (stateLock) {
                        long now = System.nanoTime();
                        double elapsed = resetClock ? 0 : (now - previous) / 1_000_000_000.0;
                        resetClock = false;
                        previous = now;
                        if (canvas != null && running && activityResumed && surfaceReady) {
                            if (status == GameStatus.RUNNING) updateGame(elapsed);
                            drawGameElements(canvas);
                            if (isFinished()) requestResultDialog();
                        }
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }
                // Limita o desenho a aproximadamente 60 quadros/s sem ocupar a CPU em espera.
                long wait = 16_666_667 - (System.nanoTime() - frameStart);
                if (wait > 0 && running) {
                    try { Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000)); }
                    catch (InterruptedException e) { running = false; }
                }
            }
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
