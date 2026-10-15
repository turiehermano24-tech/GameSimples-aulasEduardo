package br.edu.uemg.gamesimples;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ArcadeFlowTest {
    @Rule public final RecordIsolationRule records = new RecordIsolationRule();

    private CannonView game(MainActivity activity) { return activity.findViewById(R.id.cannonView); }

    private Bundle snapshot(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Bundle> state = new AtomicReference<>();
        scenario.onActivity(activity -> state.set(game(activity).saveGameState()));
        return state.get();
    }

    private void startArcade() {
        onView(withId(R.id.modeButton)).perform(scrollTo(), click());
        onView(withId(R.id.modeButton)).check(matches(withText(R.string.mode_arcade)));
        onView(withId(R.id.playButton)).perform(scrollTo(), click());
    }

    private Bundle points(int value, int hits) {
        Bundle points = new Bundle();
        points.putInt("points", value);
        points.putInt("hits", hits);
        points.putInt("streak", hits);
        points.putInt("bestStreak", hits);
        return points;
    }

    @Test public void lastHitAdvancesAfterMenuAndRecreationKeepingScoreAndCombo() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            startArcade();
            scenario.onActivity(activity -> {
                CannonView view = game(activity);
                view.stopGame();
                Bundle state = view.saveGameState();
                assertTrue(state.getBoolean("arcade"));
                state.putDouble("timeLeft", .05);
                state.putInt("shotsFired", 9);
                state.putBundle("score", points(3000, 8));
                int y = view.getScreenHeight() / 2;
                ArrayList<Bundle> targets = new ArrayList<>();
                targets.add(new Target(view, Color.BLUE, 3, 200, y, 30, 100, 0).saveState());
                state.putParcelableArrayList("targets", targets);
                state.getBundle("cannon").putBundle("ball",
                        new Cannonball(view, Color.BLACK, 1, 190, y + 20, 20, 100, 0).saveState());
                view.restoreGameState(state);
                view.continueGame();
            });
            SystemClock.sleep(250);
            assertEquals("WON", snapshot(scenario).getString("status"));
            assertEquals(3500, snapshot(scenario).getBundle("score").getInt("points"));
            onView(withText(R.string.next_level)).inRoot(isDialog()).check(matches(isDisplayed()));
            capture("arcade-level-complete.png");
            scenario.recreate();
            onView(withText(R.string.back_to_menu)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.continueButton)).check(matches(isDisplayed()));
            // Selecionar Clássico para uma nova partida não altera a partida Arcade em andamento.
            onView(withId(R.id.modeButton)).perform(scrollTo(), click());
            onView(withId(R.id.continueButton)).perform(scrollTo(), click());
            onView(withText(R.string.next_level)).inRoot(isDialog()).perform(click());
            Bundle next = snapshot(scenario);
            assertTrue(next.getBoolean("arcade"));
            assertEquals(2, next.getInt("level"));
            assertEquals(9, next.getParcelableArrayList("targets").size());
            assertEquals(3500, next.getBundle("score").getInt("points"));
            assertEquals(9, next.getBundle("score").getInt("streak"));
            assertEquals(9, next.getInt("shotsFired"));
            assertTrue(next.getDouble("timeLeft") > 8 && next.getDouble("timeLeft") <= 10);
            assertEquals(1.1, Math.abs(next.getBundle("blocker").getFloat("velocityY")), .001);
            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());
            scenario.recreate();
            assertEquals(2, snapshot(scenario).getInt("level"));
            assertEquals(3500, snapshot(scenario).getBundle("score").getInt("points"));
            onView(withId(R.id.modeButton)).check(matches(withText(R.string.mode_classic)));
            onView(withId(R.id.modeButton)).perform(scrollTo(), click());
            capture("menu-8bit-continue.png");
            onView(withId(R.id.continueButton)).perform(scrollTo(), click());
            assertEquals("RUNNING", snapshot(scenario).getString("status"));
            capture("arcade-phase-two.png");
        }
    }

    @Test public void lateLevelCapsSpeedAndTimeAndLossRestartsArcadeFromOne() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            startArcade();
            scenario.onActivity(activity -> {
                Bundle state = game(activity).saveGameState();
                state.putInt("level", 20);
                state.putDouble("timeLeft", 70);
                state.putString("status", "WON");
                state.putBundle("score", points(5000, 18));
                state.putParcelableArrayList("targets", new ArrayList<Bundle>());
                game(activity).restoreGameState(state);
            });
            SystemClock.sleep(200);
            onView(withText(R.string.next_level)).inRoot(isDialog()).perform(click());
            Bundle next = snapshot(scenario);
            assertEquals(21, next.getInt("level"));
            assertTrue(next.getDouble("timeLeft") > 28 && next.getDouble("timeLeft") <= 30);
            assertEquals(1.8, Math.abs(next.getBundle("blocker").getFloat("velocityY")), .001);
            scenario.onActivity(activity -> {
                Bundle state = game(activity).saveGameState();
                state.putString("status", "LOST");
                state.putDouble("timeLeft", 0);
                game(activity).restoreGameState(state);
            });
            SystemClock.sleep(200);
            onView(withText(R.string.reset_game)).inRoot(isDialog()).perform(click());
            Bundle fresh = snapshot(scenario);
            assertTrue(fresh.getBoolean("arcade"));
            assertEquals(1, fresh.getInt("level"));
            assertEquals(0, fresh.getBundle("score").getInt("points"));
            assertEquals(0, fresh.getInt("shotsFired"));
            assertEquals(9, fresh.getParcelableArrayList("targets").size());
        }
    }

    @Test public void recordsRemainSeparateAndSurviveNewActivity() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        GameRecords store = new GameRecords(context);
        store.record(false, 600, 1);
        store.record(true, 3500, 3);
        store.record(true, 100, 1);
        GameRecords reopened = new GameRecords(context);
        assertEquals(600, reopened.bestScore(false));
        assertEquals(3500, reopened.bestScore(true));
        assertEquals(3, reopened.bestLevel());
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.recordText)).check(matches(withText(context.getString(R.string.classic_record, 600))));
            onView(withId(R.id.modeButton)).perform(scrollTo(), click());
            onView(withId(R.id.recordText)).check(matches(withText(context.getString(R.string.arcade_record, 3500, 3))));
            scenario.recreate();
            onView(withId(R.id.modeButton)).check(matches(withText(R.string.mode_arcade)));
            capture("menu-8bit.png");
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.modeButton)).perform(scrollTo(), click());
            onView(withId(R.id.recordText)).check(matches(withText(context.getString(R.string.arcade_record, 3500, 3))));
        }
    }

    private void capture(String name) throws Exception {
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("test-screenshots");
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            image.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally { image.recycle(); }
    }
}
