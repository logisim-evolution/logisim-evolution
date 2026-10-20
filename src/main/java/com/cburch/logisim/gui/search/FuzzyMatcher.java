/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

/**
 * Sub-sequence ("fuzzy") matcher used to rank action-search candidates.
 * <p>
 * The query characters must occur in the candidate in order, but need not be
 * adjacent, soc {@code "expim"} matches {@code "Export Image"}. Matches landing on
 * a word boundary, or running consecutively, score much higher than matches
 * scattered through the middle of words. That is what makes acronym-style queries
 * ({@code "ei"} for {@code "Export Image"}) rank ahead of the coincidental matches
 * they would otherwise be buried under.
 * <p>
 * Scoring is an exact dynamic program rather than the usual greedy walk, so the
 * reported score is always the best available alignment. It runs in `O(query *
 * candidate)`; both are short enough here that re-ranking the whole action list on
 * every keystroke is not noticeable.
 */
public final class FuzzyMatcher {

  /**
   * Awarded when a character matches immediately after the previous match.
   */
  private static final int SEQUENTIAL_BONUS = 30;

  /**
   * Awarded when a character matches right after a separator, e.g. a space or a
   * dash.
   */
  private static final int SEPARATOR_BONUS = 30;

  /**
   * Awarded when a character matches on a camelCase hump.
   */
  private static final int CAMEL_BONUS = 30;

  /**
   * Awarded when a character matches the very first character of the candidate.
   */
  private static final int FIRST_LETTER_BONUS = 15;

  /**
   * Charged per candidate character skipped before the first match.
   */
  private static final int LEADING_LETTER_PENALTY = -5;

  /**
   * Floor for the accumulated {@link #LEADING_LETTER_PENALTY}, so long menu paths
   * stay findable.
   */
  private static final int MAX_LEADING_LETTER_PENALTY = -15;

  /**
   * Charged per candidate character left over after the last match, so that a
   * query covering the whole of a short candidate beats the same query covering
   * the start of a long one - otherwise "Save" and "Save Appearance View Export"
   * would rank identically for {@code "save"}.
   */
  private static final int TRAILING_LETTER_PENALTY = -1;

  /**
   * Floor for the accumulated {@link #TRAILING_LETTER_PENALTY}.
   */
  private static final int MAX_TRAILING_LETTER_PENALTY = -15;

  /**
   * Charged per candidate character skipped between two matches.
   */
  private static final int UNMATCHED_LETTER_PENALTY = -1;

  /**
   * Stands in for "this alignment is impossible"; low enough that it never wins a
   * max().
   */
  private static final int NO_SCORE = Integer.MIN_VALUE / 2;

  private FuzzyMatcher() {
    throw new UnsupportedOperationException("Utility class, do not instantiate.");
  }

  /**
   * Result of a successful match.
   *
   * @param score     relative quality of the match; only comparable against other
   *                  matches of the same query
   * @param positions indices into the candidate that the query characters
   *                  matched, ascending
   */
  public record Match(int score, int[] positions) {
  }

  /**
   * Matches {@code query} against {@code candidate}, ignoring case.
   *
   * @return the best alignment, or {@code null} when the candidate does not
   *     contain the query as a sub-sequence
   */
  public static Match match(String query, String candidate) {
    if (query == null || candidate == null) return null;
    final var queryLen = query.length();
    final var candLen = candidate.length();
    if (queryLen == 0 || candLen == 0 || queryLen > candLen) return null;

    // best[i][j] is the score of matching query[0..i] with query[i] landing exactly on
    // candidate[j]; from[i][j] records which candidate index query[i - 1] used, so the winning
    // alignment can be walked back afterwards for highlighting.
    final var best = new int[queryLen][candLen];
    final var from = new int[queryLen][candLen];

    for (var j = 0; j < candLen; j++) {
      if (matches(query.charAt(0), candidate.charAt(j))) {
        final var leading = Math.max(MAX_LEADING_LETTER_PENALTY, LEADING_LETTER_PENALTY * j);
        best[0][j] = charScore(candidate, j) + leading;
      } else {
        best[0][j] = NO_SCORE;
      }
      from[0][j] = -1;
    }

    for (var i = 1; i < queryLen; i++) {
      // Running maximum over every j' < j of best[i - 1][j'] with the gap penalty factored out.
      // Because the penalty is linear in the gap length it can be split into a part that depends
      // only on j' and a part that depends only on j, which keeps this loop linear.
      var carriedBest = NO_SCORE;
      var carriedIndex = -1;
      for (var j = 0; j < candLen; j++) {
        if (j > 0) {
          final var previous = best[i - 1][j - 1];
          if (previous > NO_SCORE) {
            final var normalized = previous - UNMATCHED_LETTER_PENALTY * (j - 1);
            if (normalized > carriedBest) {
              carriedBest = normalized;
              carriedIndex = j - 1;
            }
          }
        }

        if (j == 0 || !matches(query.charAt(i), candidate.charAt(j))) {
          best[i][j] = NO_SCORE;
          from[i][j] = -1;
          continue;
        }

        var bestPredecessor = NO_SCORE;
        var bestPredecessorIndex = -1;
        if (carriedBest > NO_SCORE) {
          bestPredecessor = carriedBest + UNMATCHED_LETTER_PENALTY * (j - 1);
          bestPredecessorIndex = carriedIndex;
        }
        // An immediately preceding match earns the sequential bonus, which the gap-based term
        // above cannot express.
        final var adjacent = best[i - 1][j - 1];
        if (adjacent > NO_SCORE && adjacent + SEQUENTIAL_BONUS > bestPredecessor) {
          bestPredecessor = adjacent + SEQUENTIAL_BONUS;
          bestPredecessorIndex = j - 1;
        }

        if (bestPredecessorIndex < 0) {
          best[i][j] = NO_SCORE;
          from[i][j] = -1;
        } else {
          best[i][j] = bestPredecessor + charScore(candidate, j);
          from[i][j] = bestPredecessorIndex;
        }
      }
    }

    // The trailing penalty depends on where the last character landed, so it has to be weighed
    // while picking the winning end position rather than tacked on afterwards.
    var finalScore = NO_SCORE;
    var finalIndex = -1;
    for (var j = 0; j < candLen; j++) {
      if (best[queryLen - 1][j] <= NO_SCORE) continue;
      final var trailing =
          Math.max(MAX_TRAILING_LETTER_PENALTY, TRAILING_LETTER_PENALTY * (candLen - 1 - j));
      final var total = best[queryLen - 1][j] + trailing;
      if (total > finalScore || finalIndex < 0) {
        finalScore = total;
        finalIndex = j;
      }
    }
    if (finalIndex < 0) return null;

    final var positions = new int[queryLen];
    var cursor = finalIndex;
    for (var i = queryLen - 1; i >= 0; i--) {
      positions[i] = cursor;
      cursor = from[i][cursor];
    }
    return new Match(finalScore, positions);
  }

  /**
   * Bonus earned by matching at candidate position {@code index}, ignoring any
   * gap penalties.
   */
  private static int charScore(String candidate, int index) {
    if (index == 0) return FIRST_LETTER_BONUS + SEPARATOR_BONUS;
    final var previous = candidate.charAt(index - 1);
    if (!Character.isLetterOrDigit(previous)) return SEPARATOR_BONUS;
    if (Character.isLowerCase(previous) && Character.isUpperCase(candidate.charAt(index))) {
      return CAMEL_BONUS;
    }
    return 0;
  }

  private static boolean matches(char query, char candidate) {
    return Character.toLowerCase(query) == Character.toLowerCase(candidate);
  }
}
