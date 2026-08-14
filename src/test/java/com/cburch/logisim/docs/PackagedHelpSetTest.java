/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.help.HelpSet;
import org.junit.jupiter.api.Test;

class PackagedHelpSetTest {

  private static final ClassLoader RESOURCE_LOADER = PackagedHelpSetTest.class.getClassLoader();
  private static final List<String> LANGUAGES = List.of("de", "en", "fr", "pt", "ru", "zh");
  private static final List<String> HELP_ACTIONS =
      List.of(
          "javax.help.BackAction",
          "javax.help.ForwardAction",
          "javax.help.HomeAction",
          "javax.help.SeparatorAction",
          "javax.help.FavoritesAction");

  @Test
  void packagedHelpSetsLoadWithLegacyBehavior() throws Exception {
    for (final var language : LANGUAGES) {
      final var resource = "doc/doc_" + language + ".hs";
      final var resources = Collections.list(RESOURCE_LOADER.getResources(resource));
      assertEquals(1, resources.size(), "Expected exactly one packaged resource " + resource);

      final var helpSet = new HelpSet(RESOURCE_LOADER, resources.getFirst());
      assertEquals(language.equals("zh") ? "Logisim - 帮助" : "Logisim - Help", helpSet.getTitle());
      assertEquals("top", helpSet.getHomeID().getIDString());
      for (final var target : List.of("top", "guide", "tutorial", "libs")) {
        assertTrue(helpSet.getCombinedMap().isValidID(target, helpSet), language + ": " + target);
      }

      final var toc = helpSet.getNavigatorView("TOC");
      final var tocLanguage = language.equals("pt") ? "en" : language;
      assertEquals(Locale.forLanguageTag(tocLanguage), toc.getLocale());
      assertEquals(language.equals("zh") ? "目录" : "Table Of Contents", toc.getLabel());
      assertEquals(tocLanguage + "/contents.xml", toc.getParameters().get("data"));
      assertEquals("javax.help.UniteAppendMerge", toc.getMergeType());

      final var search = helpSet.getNavigatorView("Search");
      assertEquals(language.equals("zh") ? "查找" : "Search", search.getLabel());
      final var searchLanguage = language.equals("zh") ? "en" : language;
      assertEquals("search_lookup_" + searchLanguage, search.getParameters().get("data"));
      assertEquals(
          "com.sun.java.help.search.DefaultSearchEngine",
          search.getParameters().get("engine"));

      final var favorites = helpSet.getNavigatorView("Favorites");
      assertEquals(language.equals("zh") ? "收藏" : "Favorites", favorites.getLabel());

      final var presentation = helpSet.getDefaultPresentation();
      assertNotNull(presentation);
      assertEquals("main window", presentation.getName());
      assertEquals(new Point(200, 10), presentation.getLocation());
      assertTrue(presentation.isToolbar());
      final var actionNames = new ArrayList<String>();
      final var actions = presentation.getHelpActions(helpSet, null);
      while (actions.hasMoreElements()) {
        actionNames.add(actions.nextElement().getClass().getName());
      }
      assertEquals(HELP_ACTIONS, actionNames);
    }
  }
}
