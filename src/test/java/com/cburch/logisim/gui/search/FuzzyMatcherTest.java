/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FuzzyMatcherTest {

  /** Convenience wrapper asserting a match happened, and returning its score. */
  private static int score(String query, String candidate) {
    final var match = FuzzyMatcher.match(query, candidate);
    assertNotNull(match, () -> "expected \"" + query + "\" to match \"" + candidate + "\"");
    return match.score();
  }

  /** Query characters must appear in order, but need not be adjacent. */
  @Test
  public void testMatchesSubsequence() {
    assertNotNull(FuzzyMatcher.match("expim", "File › Export Image"));
    assertNotNull(FuzzyMatcher.match("save", "File › Save As…"));
    assertNotNull(FuzzyMatcher.match("fs", "File › Save"));
  }

  /** Characters that are absent, or present but out of order, must not match. */
  @Test
  public void testRejectsNonSubsequence() {
    assertNull(FuzzyMatcher.match("xyz", "File › Save"));
    // Both letters occur, but "v" comes after "s" in the candidate, not before.
    assertNull(FuzzyMatcher.match("vs", "Save"));
    assertNull(FuzzyMatcher.match("saving", "Save"));
  }

  @Test
  public void testHandlesEmptyAndNullInput() {
    assertNull(FuzzyMatcher.match("", "File › Save"));
    assertNull(FuzzyMatcher.match("save", ""));
    assertNull(FuzzyMatcher.match(null, "File › Save"));
    assertNull(FuzzyMatcher.match("save", null));
  }

  @Test
  public void testIsCaseInsensitive() {
    assertNotNull(FuzzyMatcher.match("SAVE", "File › Save"));
    assertNotNull(FuzzyMatcher.match("save", "FILE › SAVE"));
  }

  /** The reported positions are what the results list emboldens, so they must be exact. */
  @Test
  public void testReportsMatchedPositions() {
    final var match = FuzzyMatcher.match("ei", "Export Image");
    assertNotNull(match);
    assertArrayEquals(new int[] {0, 7}, match.positions());
  }

  /**
   * Acronym queries are the whole point of word-boundary bonuses: "ei" must be read as the initials
   * of "Export Image" rather than as the "E" and the "i" inside the first word.
   */
  @Test
  public void testPrefersWordBoundaries() {
    final var match = FuzzyMatcher.match("ei", "Exit Image");
    assertNotNull(match);
    assertArrayEquals(new int[] {0, 5}, match.positions());
  }

  /** A tighter, better-aligned match must outrank a scattered one for the same query. */
  @Test
  public void testRanksTightMatchesHigher() {
    // The second candidate is an accidental acronym of the query; reading it literally must win.
    assertTrue(score("save", "Save") > score("save", "Save Appearance View Export"));
    assertTrue(score("ei", "Export Image") > score("ei", "Revert Circuit"));
    assertTrue(score("print", "Print") > score("print", "Printer Setup Options"));
  }

  /**
   * A query covering all of a short candidate must beat the same query covering only the start of a
   * long one, so that "Save" wins over an item that merely begins with the same word.
   */
  @Test
  public void testPrefersCandidatesTheQueryCovers() {
    assertTrue(score("save", "File › Save") > score("save", "File › Save Selection As Image"));
  }

  /** Long menu paths must not sink an otherwise perfect match on the item itself. */
  @Test
  public void testCapsLeadingPenalty() {
    final var shallow = score("stats", "Project › Get Circuit Statistics");
    final var deep = score("stats", "Project › Analyze › Reports › Get Circuit Statistics");
    // The deeper path costs something, but the capped penalty keeps the two close together.
    assertTrue(deep > 0);
    assertTrue(shallow - deep <= 15);
  }

  /** Adjacent matches score better than the same characters spread out over the candidate. */
  @Test
  public void testConsecutiveMatchesBeatGappedOnes() {
    // Deliberately free of word boundaries, so only the sequential bonus separates the two.
    assertTrue(score("abc", "abcdef") > score("abc", "axbxcxdef"));
  }
}
