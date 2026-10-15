package br.edu.uemg.gamesimples;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MenuFlowTest {
    @org.junit.Rule public final RecordIsolationRule records = new RecordIsolationRule();
    private SharedPreferences preferences;
    private boolean originalMusic;

    @Before
    public void enableMusicForTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE);
        originalMusic = preferences.getBoolean("menu_music", true);
        preferences.edit().putBoolean("menu_music", true).commit();
    }

    @After
    public void restoreMusicPreference() {
        preferences.edit().putBoolean("menu_music", originalMusic).commit();
    }

    private MainActivityFragment fragment(MainActivity activity) {
        return (MainActivityFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    private CannonView game(MainActivity activity) { return activity.findViewById(R.id.cannonView); }

    private Bundle snapshot(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Bundle> state = new AtomicReference<>();
        scenario.onActivity(activity -> state.set(game(activity).saveGameState()));
        return state.get();
    }

    @Test
    public void musicContinuesFromMenuIntoGameAndStopsInBackground() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
            onView(withId(R.id.continueButton)).check(matches(not(isDisplayed())));
            SystemClock.sleep(600);
            assertEquals(10, snapshot(scenario).getDouble("timeLeft"), 0);
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
            capture("menu.png");
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
            // A transição da janela do emulador pode ainda estar animando após RESUMED.
            // Aqui verificamos o ciclo de vida do áudio; os demais testes usam toques reais.
            scenario.onActivity(activity -> activity.findViewById(R.id.playButton).performClick());
            SystemClock.sleep(200);
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
            assertEquals("RUNNING", snapshot(scenario).getString("status"));
            assertTrue(snapshot(scenario).getDouble("timeLeft") < 10);
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
            assertEquals("PAUSED", snapshot(scenario).getString("status"));
            scenario.recreate();
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
        }
    }

    @Test
    public void muteSurvivesRecreationAndInstructionsDoNotStartGame() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.musicButton)).perform(scrollTo(), click());
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            scenario.recreate();
            onView(withId(R.id.musicButton)).check(matches(withText(R.string.music_off)));
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            onView(withId(R.id.helpButton)).perform(scrollTo(), click());
            onView(withText(org.hamcrest.Matchers.containsString("Toque ou arraste")))
                    .inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(R.string.understood)).inRoot(isDialog()).perform(click());
            assertEquals(10, snapshot(scenario).getDouble("timeLeft"), 0);
            onView(withId(R.id.playButton)).perform(scrollTo(), click());
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            scenario.recreate();
            scenario.onActivity(activity -> assertFalse(fragment(activity).isMusicPlaying()));
            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());
            onView(withId(R.id.musicButton)).perform(scrollTo(), click());
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
        }
    }

    @Test
    public void backToMenuPreservesMatchThroughRecreationAndContinue() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.playButton)).perform(click());
            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());
            Bundle before = snapshot(scenario);
            assertEquals("PAUSED", before.getString("status"));
            onView(withId(R.id.continueButton)).check(matches(isDisplayed()));
            SystemClock.sleep(300);
            assertEquals(before.getDouble("timeLeft"), snapshot(scenario).getDouble("timeLeft"), 0);
            scenario.recreate();
            onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
            assertEquals(before.getDouble("timeLeft"), snapshot(scenario).getDouble("timeLeft"), 0);
            onView(withId(R.id.continueButton)).check(matches(isDisplayed()));
            capture("menu-continue.png");
            onView(withId(R.id.playButton)).perform(scrollTo(), click());
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click());
            assertEquals(before.getDouble("timeLeft"), snapshot(scenario).getDouble("timeLeft"), 0);
            // O fechamento da janela nativa pode ainda estar animando após o Espresso ficar ocioso.
            InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(250, 3000);
            onView(withId(R.id.continueButton)).perform(scrollTo(), click());
            onView(withId(R.id.cannonView)).check(matches(isDisplayed()));
            assertEquals("RUNNING", snapshot(scenario).getString("status"));
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
        }
    }

    @Test
    public void pauseMenuButtonReturnsWithoutFiringAndAllowsFreshMatch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.playButton)).perform(click());
            scenario.onActivity(activity -> {
                CannonView view = game(activity);
                tap(view, .9f, .1f);
                assertEquals("PAUSED", view.saveGameState().getString("status"));
                tap(view, .5f, .8f);
            });
            onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
            assertEquals(0, snapshot(scenario).getInt("shotsFired"));
            onView(withId(R.id.playButton)).perform(scrollTo(), click());
            onView(withText(R.string.new_match)).inRoot(isDialog()).perform(click());
            assertEquals("RUNNING", snapshot(scenario).getString("status"));
            assertEquals(0, snapshot(scenario).getInt("shotsFired"));
            scenario.onActivity(activity -> assertTrue(fragment(activity).isMusicPlaying()));
        }
    }

    private void tap(CannonView view, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                view.getWidth() * x, view.getHeight() * y, 0);
        MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP,
                view.getWidth() * x, view.getHeight() * y, 0);
        view.onTouchEvent(down);
        view.onTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    @Test
    public void resultMenuOffersFreshGameInsteadOfContinuingFinishedMatch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.playButton)).perform(click());
            scenario.onActivity(activity -> {
                Bundle saved = game(activity).saveGameState();
                saved.putString("status", "LOST");
                saved.putDouble("timeLeft", 0);
                game(activity).restoreGameState(saved);
            });
            SystemClock.sleep(250);
            onView(withText(R.string.back_to_menu)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
            onView(withId(R.id.continueButton)).check(matches(not(isDisplayed())));
            onView(withId(R.id.playButton)).check(matches(withText(R.string.play_game))).perform(click());
            Bundle fresh = snapshot(scenario);
            assertEquals("RUNNING", fresh.getString("status"));
            assertEquals(0, fresh.getInt("shotsFired"));
            assertEquals(0, fresh.getBundle("score").getInt("points"));
            assertEquals(9, fresh.getParcelableArrayList("targets").size());
        }
    }

    private void capture(String name) throws Exception {
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("test-screenshots");
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream file = new FileOutputStream(new File(directory, name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, file);
        } finally { bitmap.recycle(); }
    }
}
