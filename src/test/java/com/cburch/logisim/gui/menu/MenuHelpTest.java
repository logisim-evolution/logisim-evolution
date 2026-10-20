/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;
import org.junit.jupiter.api.Test;

class MenuHelpTest {

  private static final ClassLoader RESOURCE_LOADER = MenuHelp.class.getClassLoader();

  @Test
  void keepsAvailableLocalizedHelpSet() {
    final var resolved = MenuHelp.resolveHelpSet(RESOURCE_LOADER, "doc/doc_de.hs");

    assertNotNull(resolved);
    assertEquals("doc/doc_de.hs", resolved.path());
  }

  @Test
  void fallsBackToEnglishWhenLocalizedHelpSetIsMissing() {
    final var resolved = MenuHelp.resolveHelpSet(RESOURCE_LOADER, "doc/doc_es.hs");

    assertNotNull(resolved);
    assertEquals("doc/doc_en.hs", resolved.path());
  }

  @Test
  void blankHelpSetConfigurationUsesEnglish() {
    final var resolved = MenuHelp.resolveHelpSet(RESOURCE_LOADER, " ");

    assertNotNull(resolved);
    assertEquals("doc/doc_en.hs", resolved.path());
  }

  @Test
  void missingEnglishHelpSetCannotBeResolved() {
    final var emptyLoader =
        new ClassLoader(null) {
          @Override
          public URL getResource(String name) {
            return null;
          }
        };

    assertNull(MenuHelp.resolveHelpSet(emptyLoader, "doc/doc_es.hs"));
  }
}
