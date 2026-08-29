/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import javax.swing.Icon;

/**
 * One thing a SearchProvider can offer, independent of any particular query.
 * <p>
 * Candidates are deliberately plain data plus a {@link Runnable}: a provider
 * decides what "activating" its results means - clicking a menu item, selecting a
 * circuit, opening a browser - and the search dialog never needs to know. Anything
 * Swing-related must be resolved while building the candidate, on the event
 * dispatch thread, because matching may later run off it.
 *
 * @param title   primary label, e.g. {@code "Export Image"}
 * @param context secondary label placed before the title, e.g. {@code "File"}; may
 *                be empty
 * @param icon    optional icon shown at the start of the row
 * @param hint    optional right-aligned annotation, e.g. an accelerator or a URL
 * @param enabled whether the candidate can currently be activated; disabled ones
 *                are still listed, greyed out, because knowing something exists but
 *                is unavailable beats not finding it
 * @param action  what to run when the candidate is chosen; invoked on the event
 *                dispatch thread
 */
public record SearchCandidate(
    String title, String context, Icon icon, String hint, boolean enabled,
    Runnable action) {

  /**
   * Separates {@link #context()} from {@link #title()} in the displayed text.
   */
  public static final String CONTEXT_SEPARATOR = " › ";

  /**
   * Builds a candidate with no icon and no hint.
   */
  public static SearchCandidate of(String title, String context, boolean enabled, Runnable action) {
    return new SearchCandidate(title, context, null, "", enabled, action);
  }

  /**
   * The full text shown in the results list, and the text queries are matched
   * against.
   */
  public String displayText() {
    return context == null || context.isEmpty() ? title : context + CONTEXT_SEPARATOR + title;
  }

  /**
   * Index in {@link #displayText()} at which {@link #title()} begins.
   */
  public int titleOffset() {
    return displayText().length() - title.length();
  }
}
