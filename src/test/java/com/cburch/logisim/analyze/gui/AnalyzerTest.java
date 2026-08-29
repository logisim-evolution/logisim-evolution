/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;

class AnalyzerTest {
  @Test
  void localeChangeKeepsManualWindowSizeWhenPreferredSizeIsSmaller() {
    final var current = new Dimension(800, 600);
    final var preferred = new Dimension(700, 500);

    assertEquals(current, Analyzer.expandedSizeForLocaleChange(current, preferred));
  }

  @Test
  void localeChangeExpandsWindowWhenPreferredSizeGrows() {
    final var current = new Dimension(800, 600);
    final var preferred = new Dimension(900, 650);

    assertEquals(preferred, Analyzer.expandedSizeForLocaleChange(current, preferred));
  }

  @Test
  void localeChangeExpandsOnlyTheDimensionThatNeedsMoreSpace() {
    final var current = new Dimension(800, 600);
    final var preferred = new Dimension(900, 500);

    assertEquals(new Dimension(900, 600), Analyzer.expandedSizeForLocaleChange(current, preferred));
  }
}
