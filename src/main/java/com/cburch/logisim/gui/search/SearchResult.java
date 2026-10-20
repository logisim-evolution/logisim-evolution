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
 * A {@link SearchCandidate} that matched a query, together with why it matched.
 *
 * <p>Scores are produced by the providers themselves, so that a provider talking
 * to a remote service can rank using whatever the service returns. To keep results
 * from different providers comparable in one list, local providers should score
 * with {@link FuzzyMatcher}, and remote ones should map their own relevance onto a
 * similar range - roughly 0 for a weak match and a few hundred for an excellent
 * one. {@link SearchProvider#getPriority()} breaks ties between providers.
 *
 * @param candidate  what matched
 * @param score      relevance; higher sorts earlier
 * @param highlights indices into {@link SearchCandidate#displayText()} to
 *                   emphasise, ascending; may be empty when the provider has
 *                   nothing to highlight
 */
public record SearchResult(SearchCandidate candidate, int score, int[] highlights) {

  /**
   * Shared empty highlight array, for results that matched without character
   * positions.
   */
  public static final int[] NO_HIGHLIGHTS = new int[0];

  /**
   * Builds a result with no highlighted characters.
   */
  public static SearchResult of(SearchCandidate candidate, int score) {
    return new SearchResult(candidate, score, NO_HIGHLIGHTS);
  }
}
