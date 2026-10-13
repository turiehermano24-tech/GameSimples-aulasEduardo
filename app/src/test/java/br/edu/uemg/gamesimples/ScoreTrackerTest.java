package br.edu.uemg.gamesimples;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreTrackerTest {
    @Test
    public void perfectMatchCapsMultiplierButKeepsCountingStreak() {
        ScoreTracker score = new ScoreTracker();
        int[] expected = {100, 300, 600, 1000, 1500, 2000, 2500, 3000, 3500};
        for (int points : expected) {
            score.registerHit();
            assertEquals(points, score.getPoints());
        }
        assertEquals(5, score.getNextMultiplier());
        assertEquals(9, score.getHits());
        assertEquals(9, score.getStreak());
        assertEquals(9, score.getBestStreak());
    }

    @Test
    public void missBreaksComboWithoutTakingEarnedPoints() {
        ScoreTracker score = new ScoreTracker();
        score.registerHit();
        score.registerHit();
        score.registerMiss();
        // Um projétil rebatido também sai da tela: a segunda notificação é inofensiva.
        score.registerMiss();
        assertEquals(300, score.getPoints());
        assertEquals(0, score.getStreak());
        assertEquals(1, score.getNextMultiplier());
        assertEquals(2, score.getBestStreak());
        score.registerHit();
        assertEquals(400, score.getPoints());
        assertEquals(3, score.getHits());
        assertEquals(2, score.getBestStreak());
    }

    @Test
    public void restoredComboContinuesAtTheSameReward() {
        ScoreTracker score = new ScoreTracker(600, 3, 3, 3);
        score.registerHit();
        assertEquals(1000, score.getPoints());
        assertEquals(4, score.getHits());
        assertEquals(4, score.getStreak());
        assertEquals(4, score.getBestStreak());
    }

    @Test
    public void accuracyHandlesNoShotsAndRoundsPercentage() {
        ScoreTracker score = new ScoreTracker();
        assertEquals(0, score.getAccuracy(0));
        score.registerHit();
        assertEquals(100, score.getAccuracy(1));
        assertEquals(33, score.getAccuracy(3));
        score.registerHit();
        assertEquals(67, score.getAccuracy(3));
    }

    @Test
    public void newMatchStartsWithNoScoreOrCombo() {
        ScoreTracker score = new ScoreTracker();
        assertEquals(0, score.getPoints());
        assertEquals(0, score.getHits());
        assertEquals(0, score.getStreak());
        assertEquals(0, score.getBestStreak());
        assertEquals(1, score.getNextMultiplier());
    }
}
