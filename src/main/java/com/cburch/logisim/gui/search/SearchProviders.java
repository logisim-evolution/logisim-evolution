/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import com.cburch.logisim.gui.search.providers.MenuSearchProvider;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of the {@link SearchProvider}s the omni-search dialog consults.
 *
 * <p>Providers are held in registration order and consulted in that order, which
 * is also the order results appear in when nothing distinguishes them by score.
 * Registering from a static initialiser is fine, but registering from the code that
 * owns the searchable thing is usually clearer.
 */
public final class SearchProviders {

  private static final CopyOnWriteArrayList<SearchProvider> providers = new CopyOnWriteArrayList<>();

  static {
    register(new MenuSearchProvider());
  }

  private SearchProviders() {
    throw new UnsupportedOperationException("Utility class, do not instantiate.");
  }

  /**
   * Adds {@code provider}, ignoring a provider that is already registered.
   */
  public static void register(SearchProvider provider) {
    if (provider != null) providers.addIfAbsent(provider);
  }

  public static void unregister(SearchProvider provider) {
    providers.remove(provider);
  }

  /**
   * Every registered provider, in registration order.
   */
  public static List<SearchProvider> getAll() {
    return List.copyOf(providers);
  }

  /**
   * The providers that report themselves usable for {@code context}.
   */
  public static List<SearchProvider> getAvailable(SearchContext context) {
    return providers.stream().filter(provider -> provider.isAvailable(context)).toList();
  }
}
