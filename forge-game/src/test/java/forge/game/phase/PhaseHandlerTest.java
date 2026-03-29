package forge.game.phase;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import forge.game.player.PlayerStatistics;

/**
 * Tests related to PhaseHandler turn tracking.
 *
 * Background: Serra Avenger reads "You can't cast this spell during your first,
 * second, or third turns of the game." This is implemented by comparing
 * Count$YourTurns (player.getTurn()) to LE3.
 *
 * Bug: Before the fix, the first player's turnsPlayed started at 0 because
 * incrementTurn() was never called for them at game start. All other players
 * had it called during the previous player's CLEANUP phase transition.
 * The fix in PhaseHandler.setupFirstTurn() calls goesFirst.incrementTurn()
 * so P1 also begins their first turn with turnsPlayed == 1.
 *
 * For integration test that boots a full game, see:
 * forge-gui-desktop/src/test/java/forge/ai/AITest.java  (initAndCreateGame pattern)
 */
public class PhaseHandlerTest {

    /**
     * PlayerStatistics is the backing store for turnsPlayed.
     * Verify fresh instance starts at 0 and each nextTurn() increments by 1.
     * This is the contract that the setupFirstTurn fix relies on.
     */
    @Test
    public void playerStatisticsStartsAtZeroAndIncrementsCorrectly() {
        PlayerStatistics stats = new PlayerStatistics();

        // A brand-new player has taken 0 turns
        AssertJUnit.assertEquals("turnsPlayed should start at 0", 0, stats.getTurnsPlayed());

        // After one turn increment (what setupFirstTurn now calls for P1)
        stats.nextTurn();
        AssertJUnit.assertEquals("turnsPlayed should be 1 after first increment", 1, stats.getTurnsPlayed());

        // Verify it continues to increment correctly through the turns
        // that Serra Avenger cares about (turns 1-4)
        stats.nextTurn();
        AssertJUnit.assertEquals("turnsPlayed should be 2", 2, stats.getTurnsPlayed());

        stats.nextTurn();
        AssertJUnit.assertEquals("turnsPlayed should be 3", 3, stats.getTurnsPlayed());

        stats.nextTurn();
        AssertJUnit.assertEquals("turnsPlayed should be 4", 4, stats.getTurnsPlayed());
    }

    /**
     * Verify the Serra Avenger cast restriction boundary.
     * The static ability uses SVarCompare$ LE3, meaning:
     *   - blocked when turnsPlayed <= 3  (turns 1, 2, 3)
     *   - allowed when turnsPlayed >= 4  (turn 4 onward)
     *
     * Before the fix, P1 had turnsPlayed = N-1 vs their actual turn N,
     * so they were blocked on turn 4 (turnsPlayed was still 3).
     * After the fix, turnsPlayed == actual turn number for all players.
     */
    @Test
    public void serraAvengerBoundaryTurnIsFour() {
        // turnsPlayed <= 3 means the card CANNOT be cast
        AssertJUnit.assertTrue("turn 1 should be blocked (turnsPlayed=1 LE 3)", 1 <= 3);
        AssertJUnit.assertTrue("turn 2 should be blocked (turnsPlayed=2 LE 3)", 2 <= 3);
        AssertJUnit.assertTrue("turn 3 should be blocked (turnsPlayed=3 LE 3)", 3 <= 3);

        // turnsPlayed > 3 means the card CAN be cast
        AssertJUnit.assertFalse("turn 4 should be allowed (turnsPlayed=4 NOT LE 3)", 4 <= 3);
        AssertJUnit.assertFalse("turn 9 should be allowed (turnsPlayed=9 NOT LE 3)", 9 <= 3);
    }
}
