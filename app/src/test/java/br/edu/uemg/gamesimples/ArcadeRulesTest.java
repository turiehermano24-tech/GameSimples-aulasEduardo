package br.edu.uemg.gamesimples;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ArcadeRulesTest {
    @Test public void startsAtOriginalSpeedAndAddsTenPercentPerLevel() {
        assertEquals(1, ArcadeRules.speedMultiplier(1), .0001);
        assertEquals(1.1, ArcadeRules.speedMultiplier(2), .0001);
        assertEquals(1.5, ArcadeRules.speedMultiplier(6), .0001);
    }

    @Test public void speedRemainsBoundedOnLongRuns() {
        assertEquals(1.8, ArcadeRules.speedMultiplier(9), .0001);
        assertEquals(1.8, ArcadeRules.speedMultiplier(1000), .0001);
    }

    @Test public void nextLevelCarriesTimeWithinPlayableBounds() {
        assertEquals(10, ArcadeRules.nextLevelTime(2), 0);
        assertEquals(17.5, ArcadeRules.nextLevelTime(17.5), 0);
        assertEquals(30, ArcadeRules.nextLevelTime(80), 0);
    }
}
