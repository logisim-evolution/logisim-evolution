/*
 * Logisim-evolution - digital logic design tool
 * Copyright by the logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.Main;
import com.cburch.logisim.util.LocaleManager;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StartupTest {

  @Test
  void localeOptionAppliesToHelpText() {
    final var originalOut = System.out;
    final var originalLocale = LocaleManager.getLocale();
    final var originalHeadless = Main.headless;
    final var output = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

      Startup.parseArgs(new String[] {"--tty", "table", "--locale", "zh", "--help"});

      final var helpText = output.toString(StandardCharsets.UTF_8);
      assertTrue(helpText.contains("显示本参数摘要帮助页。"));
      assertFalse(helpText.contains("Displays this argument summary help page."));
    } finally {
      System.setOut(originalOut);
      LocaleManager.setLocale(originalLocale);
      Main.headless = originalHeadless;
    }
  }
}
