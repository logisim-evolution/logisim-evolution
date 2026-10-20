/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import java.util.List;

/**
 * A source of searchable things, plugged into the omni-search dialog.
 *
 * <p>The dialog itself is only a hub: it collects providers from
 * {@link SearchProviders}, fans the
 * query out to each of them and merges what comes back into one ranked list. Adding
 * a new kind of searchable thing - circuits in the project, placeable components,
 * preference pages, an online documentation index - is therefore a matter of
 * writing a provider and registering it, with no changes to the dialog.
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #prepare(SearchContext)} always runs on the event dispatch thread, and
 * is the only
 * place a provider may touch Swing state. {@link #search(SearchQuery)} runs on the
 * EDT for ordinary providers, but on a background thread for those that report
 * {@link #isAsynchronous()} - so a provider that queries a website must declare
 * itself asynchronous, must do its Swing work in {@code prepare}, and must not
 * block the caller for long.
 *
 * <p>Most local providers should extend {@link IndexedSearchProvider} rather than
 * implementing this
 * interface directly; it supplies the matching and scoring.
 */
public interface SearchProvider {

  /**
   * Localized name of this provider, used to label its results.
   */
  String getDisplayName();

  /**
   * Snapshots whatever this provider needs, on the event dispatch thread, each
   * time the dialog opens.
   *
   * @param context the window the search was opened from
   */
  default void prepare(SearchContext context) {
    // Nothing to prepare by default.
  }

  /**
   * Returns this provider's matches for {@code query}, scored as described on
   * {@link SearchResult}.
   *
   * <p>An empty query means "offer everything", and providers that would be
   * expensive or noisy in
   * that case - a remote provider especially - should simply return an empty
   * list.
   */
  List<SearchResult> search(SearchQuery query);

  /**
   * Whether {@link #search(SearchQuery)} must run off the event dispatch thread.
   * Results from an asynchronous provider are merged into the list as they
   * arrive, so a slow one never holds up the rest.
   */
  default boolean isAsynchronous() {
    return false;
  }

  /**
   * Breaks ties between providers whose results scored equally; higher wins.
   */
  default int getPriority() {
    return 0;
  }

  /**
   * Whether this provider should be consulted at all, for example given a user
   * preference.
   */
  default boolean isAvailable(SearchContext context) {
    return true;
  }
}
