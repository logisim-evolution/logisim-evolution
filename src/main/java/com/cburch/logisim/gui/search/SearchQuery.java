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
 * A single query issued to the providers.
 *
 * <p>Wrapping what is currently just a string keeps the provider signature stable:
 * filters, result limits or a cancellation token can be added here later without
 * touching every provider.
 *
 * @param text what the user typed, already trimmed; empty means "give all you have"
 */
public record SearchQuery(String text) {

  public boolean isEmpty() {
    return text.isEmpty();
  }
}
