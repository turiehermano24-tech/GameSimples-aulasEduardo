package br.edu.uemg.gamesimples;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/** Regressões que precisam do Canvas, Bundle e ciclo de vida reais do Android. */
@RunWith(AndroidJUnit4.class)
public class GameContinuityTest {
    @org.junit.Rule public final RecordIsolationRule records = new RecordIsolationRule();
    private ActivityScenario<MainActivity> launchGame() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.playButton)).perform(click());
        return scenario;
    }

    private CannonView view(MainActivity activity) {
        return activity.findViewById(R.id.cannonView);
    }

    private void tap(CannonView view, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0);
        view.onTouchEvent(down);
        view.onTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private Bundle snapshot(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Bundle> saved = new AtomicReference<>();
        scenario.onActivity(activity -> saved.set(view(activity).saveGameState()));
        return saved.get();
    }

    @Test
    public void diagonalShotSurvivesFirstFrameAndKeepsFractionalMotion() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Cannon cannon = new Cannon(game, 40, 100, 20);
                cannon.align(Math.PI / 4);
                cannon.fireCannonball();
                Cannonball ball = cannon.getCannonball();
                float initial = ball.shape.left;
                ball.update(0.001);
                assertTrue("A bola deve entrar na tela, sem desaparecer na origem", ball.isOnScreen());
                assertTrue(ball.shape.left > initial);

                Cannonball slow = new Cannonball(game, Color.BLACK, 0, 100, 200, 10, 20, 0);
                slow.update(0.001);
                assertEquals(100.02f, slow.shape.left, 0.001f);
            });
        }
    }

    @Test
    public void movementIsConsistentAtThirtyAndOneHundredTwentyFrames() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Cannonball a = new Cannonball(game, Color.BLACK, 0, 100, 200, 10, 123, 0);
                Cannonball b = new Cannonball(game, Color.BLACK, 0, 100, 200, 10, 123, 0);
                for (int i = 0; i < 30; i++) a.update(1.0 / 30);
                for (int i = 0; i < 120; i++) b.update(1.0 / 120);
                assertEquals(a.shape.left, b.shape.left, 0.01f);
                assertEquals(223, a.shape.left, 0.01f);
            });
        }
    }

    @Test
    public void pauseFreezesTimeAndContinueDoesNotFire() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                tap(game, game.getScreenWidth() * 0.9f, game.getScreenHeight() * 0.1f);
            });
            Bundle paused = snapshot(scenario);
            assertEquals("PAUSED", paused.getString("status"));
            SystemClock.sleep(350); // Tempo real precisa passar para detectar cronômetro ativo durante pausa.
            assertEquals(paused.getDouble("timeLeft"), snapshot(scenario).getDouble("timeLeft"), 0);
            captureScreen("paused.png");
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                tap(game, game.getScreenWidth() / 2f, game.getScreenHeight() / 2f);
                assertEquals("RUNNING", game.saveGameState().getString("status"));
                assertEquals(paused.getInt("shotsFired"), game.saveGameState().getInt("shotsFired"));
            });
            SystemClock.sleep(150);
            assertTrue(snapshot(scenario).getDouble("timeLeft") < paused.getDouble("timeLeft"));
        }
    }

    @Test
    public void backgroundReturnKeepsMatchPausedAndSingleThread() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.moveToState(Lifecycle.State.CREATED);
            Bundle before = snapshot(scenario);
            assertEquals(0, gameThreadCount());
            SystemClock.sleep(350);
            scenario.moveToState(Lifecycle.State.RESUMED);
            Bundle after = snapshot(scenario);
            assertEquals("PAUSED", after.getString("status"));
            assertEquals(before.getDouble("timeLeft"), after.getDouble("timeLeft"), 0);
            assertEquals(before.getInt("shotsFired"), after.getInt("shotsFired"));
            assertEquals(1, gameThreadCount());
        }
        assertEquals(0, gameThreadCount());
    }

    @Test
    public void recreationRestoresTargetsBallAndStatistics() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Bundle saved = game.saveGameState();
                saved.putDouble("timeLeft", 7.25);
                saved.putDouble("elapsed", 4.5);
                saved.putInt("shotsFired", 3);
                saved.putBundle("score", scoreState(300, 2, 2, 2));
                ArrayList<Bundle> targets = saved.getParcelableArrayList("targets");
                targets.remove(0);
                targets.remove(0);
                saved.putParcelableArrayList("targets", targets);
                Cannonball ball = new Cannonball(game, Color.BLACK, 1, 200, 200, 20, 100, 50);
                saved.getBundle("cannon").putBundle("ball", ball.saveState());
                game.restoreGameState(saved);
                game.resumeGame();
            });
            Bundle before = snapshot(scenario);
            scenario.recreate();
            Bundle restored = snapshot(scenario);
            assertEquals("PAUSED", restored.getString("status"));
            assertEquals(7.25, restored.getDouble("timeLeft"), 0);
            assertEquals(4.5, restored.getDouble("elapsed"), 0);
            assertEquals(3, restored.getInt("shotsFired"));
            assertEquals(7, restored.getParcelableArrayList("targets").size());
            assertEquals(300, restored.getBundle("score").getInt("points"));
            assertEquals(2, restored.getBundle("score").getInt("hits"));
            assertEquals(2, restored.getBundle("score").getInt("streak"));
            assertEquals(2, restored.getBundle("score").getInt("bestStreak"));
            Bundle ballBefore = before.getBundle("cannon").getBundle("ball");
            Bundle ballAfter = restored.getBundle("cannon").getBundle("ball");
            assertNotNull(ballAfter);
            assertEquals(ballBefore.getFloat("left"), ballAfter.getFloat("left"), 0.001f);
            assertEquals(ballBefore.getFloat("top"), ballAfter.getFloat("top"), 0.001f);
            assertEquals(1, gameThreadCount());
        }
    }

    @Test
    public void restoredResultCanRestartWithoutDuplicateThread() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                Bundle saved = game.saveGameState();
                saved.putString("status", "LOST");
                saved.putDouble("timeLeft", 0);
                saved.putBundle("score", scoreState(600, 3, 0, 3));
                game.restoreGameState(saved);
            });
            SystemClock.sleep(150);
            onView(withText(R.string.lose)).inRoot(isDialog()).check(matches(isDisplayed()));
            scenario.recreate();
            SystemClock.sleep(150);
            onView(withText(R.string.lose)).inRoot(isDialog()).check(matches(isDisplayed()));
            assertEquals(600, snapshot(scenario).getBundle("score").getInt("points"));
            onView(withText(R.string.reset_game)).inRoot(isDialog()).perform(click());
            Bundle restarted = snapshot(scenario);
            assertEquals("RUNNING", restarted.getString("status"));
            assertEquals(9, restarted.getParcelableArrayList("targets").size());
            assertEquals(0, restarted.getInt("shotsFired"));
            assertEquals(0, restarted.getBundle("score").getInt("points"));
            assertEquals(0, restarted.getBundle("score").getInt("streak"));
            assertEquals(0, restarted.getBundle("score").getInt("bestStreak"));
            assertTrue(restarted.getDouble("timeLeft") > 8);
            assertEquals(1, gameThreadCount());
        }
    }

    @Test
    public void lastTargetWinsAndAppliesTimeReward() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Bundle saved = game.saveGameState();
                saved.putDouble("timeLeft", 0.05);
                saved.putInt("shotsFired", 9);
                saved.putBundle("score", scoreState(3000, 8, 8, 8));
                int y = game.getScreenHeight() / 2;
                ArrayList<Bundle> targets = new ArrayList<>();
                targets.add(new Target(game, Color.BLUE, 3, 200, y, 30, 100, 0).saveState());
                saved.putParcelableArrayList("targets", targets);
                saved.getBundle("cannon").putBundle("ball",
                        new Cannonball(game, Color.BLACK, 1, 190, y + 20, 20, 100, 0).saveState());
                game.restoreGameState(saved);
                game.resumeGame();
                tap(game, game.getScreenWidth() / 2f, y);
            });
            SystemClock.sleep(250);
            Bundle won = snapshot(scenario);
            assertEquals("WON", won.getString("status"));
            assertTrue(won.getParcelableArrayList("targets").isEmpty());
            assertTrue(won.getDouble("timeLeft") > 3);
            assertEquals(3500, won.getBundle("score").getInt("points"));
            assertEquals(9, won.getBundle("score").getInt("bestStreak"));
            onView(withText(R.string.win)).inRoot(isDialog()).check(matches(isDisplayed()));
            captureScreen("score-result.png");
        }
    }

    @Test
    public void blockerPenaltyEndsMatchOnceWithoutNegativeTime() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Bundle saved = game.saveGameState();
                saved.putDouble("timeLeft", 1);
                saved.putInt("shotsFired", 3);
                saved.putBundle("score", scoreState(300, 2, 2, 2));
                int y = game.getScreenHeight() / 2;
                saved.putBundle("blocker", new Blocker(game, Color.BLACK, 2,
                        200, y, 30, 100, 0).saveState());
                saved.getBundle("cannon").putBundle("ball",
                        new Cannonball(game, Color.BLACK, 1, 190, y + 20, 20, 100, 0).saveState());
                game.restoreGameState(saved);
                game.resumeGame();
                tap(game, game.getScreenWidth() / 2f, y);
            });
            SystemClock.sleep(250);
            Bundle lost = snapshot(scenario);
            assertEquals("LOST", lost.getString("status"));
            assertEquals(0, lost.getDouble("timeLeft"), 0);
            assertEquals(9, lost.getParcelableArrayList("targets").size());
            assertEquals(300, lost.getBundle("score").getInt("points"));
            assertEquals(0, lost.getBundle("score").getInt("streak"));
            assertEquals(2, lost.getBundle("score").getInt("bestStreak"));
            onView(withText(R.string.lose)).inRoot(isDialog()).check(matches(isDisplayed()));
            captureScreen("result.png");
        }
    }

    @Test
    public void hitAwardsComboOnceAndKeepsOriginalTimeBonus() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Bundle saved = game.saveGameState();
                saved.putDouble("timeLeft", 4);
                saved.putInt("shotsFired", 2);
                saved.putBundle("score", scoreState(100, 1, 1, 1));
                int y = game.getScreenHeight() / 2;
                ArrayList<Bundle> targets = saved.getParcelableArrayList("targets");
                targets.remove(0); // Um alvo já foi destruído pelo primeiro disparo.
                targets.set(0, new Target(game, Color.BLUE, 3, 200, y, 30, 100, 0).saveState());
                saved.putParcelableArrayList("targets", targets);
                saved.getBundle("cannon").putBundle("ball",
                        new Cannonball(game, Color.BLACK, 1, 190, y + 20, 20, 100, 0).saveState());
                game.restoreGameState(saved);
                game.resumeGame();
                tap(game, game.getScreenWidth() / 2f, y);
            });
            SystemClock.sleep(250);
            Bundle saved = snapshot(scenario);
            assertEquals("RUNNING", saved.getString("status"));
            assertEquals(300, saved.getBundle("score").getInt("points"));
            assertEquals(2, saved.getBundle("score").getInt("streak"));
            assertEquals(7, saved.getParcelableArrayList("targets").size());
            assertTrue(saved.getDouble("timeLeft") > 5);
            assertTrue(saved.getDouble("timeLeft") <= 7);
            captureScreen("score-hud.png");
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                tap(game, game.getScreenWidth() * 0.9f, game.getScreenHeight() * 0.1f);
            });
            Bundle paused = snapshot(scenario);
            assertEquals("PAUSED", paused.getString("status"));
            SystemClock.sleep(150);
            assertEquals(300, snapshot(scenario).getBundle("score").getInt("points"));
            assertEquals(2, snapshot(scenario).getBundle("score").getInt("streak"));
        }
    }

    @Test
    public void shotLeavingScreenBreaksComboWithoutTimePenalty() {
        try (ActivityScenario<MainActivity> scenario = launchGame()) {
            scenario.onActivity(activity -> {
                CannonView game = view(activity);
                game.stopGame();
                Bundle saved = game.saveGameState();
                saved.putDouble("timeLeft", 9);
                saved.putBundle("score", scoreState(300, 2, 2, 2));
                saved.getBundle("cannon").putBundle("ball",
                        new Cannonball(game, Color.BLACK, 1, game.getScreenWidth() + 10,
                                200, 20, 100, 0).saveState());
                game.restoreGameState(saved);
                game.resumeGame();
                tap(game, game.getScreenWidth() / 2f, game.getScreenHeight() / 2f);
            });
            SystemClock.sleep(250);
            Bundle saved = snapshot(scenario);
            assertEquals(300, saved.getBundle("score").getInt("points"));
            assertEquals(0, saved.getBundle("score").getInt("streak"));
            assertEquals(2, saved.getBundle("score").getInt("bestStreak"));
            assertNull(saved.getBundle("cannon").getBundle("ball"));
            assertTrue(saved.getDouble("timeLeft") > 8);
        }
    }

    private Bundle scoreState(int points, int hits, int streak, int bestStreak) {
        Bundle score = new Bundle();
        score.putInt("points", points);
        score.putInt("hits", hits);
        score.putInt("streak", streak);
        score.putInt("bestStreak", bestStreak);
        return score;
    }

    private void captureScreen(String name) {
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("test-screenshots");
        Bitmap screen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screen);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            screen.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (IOException e) {
            throw new AssertionError("Não foi possível salvar a captura do teste", e);
        } finally {
            screen.recycle();
        }
    }

    private int gameThreadCount() {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet())
            if (thread.isAlive() && thread.getName().equals("CannonThread")) count++;
        return count;
    }
}
