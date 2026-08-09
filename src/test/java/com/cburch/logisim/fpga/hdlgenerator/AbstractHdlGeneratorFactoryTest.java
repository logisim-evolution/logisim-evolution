/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AbstractHdlGeneratorFactoryTest {

  @Test
  void clockWarningsUseCurrentLocaleAndKeepContext() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      LocaleManager.setLocale(Locale.ENGLISH);
      assertEquals(
          "Component \"Clock\" in circuit \"main\" has no clock connection!",
          AbstractHdlGeneratorFactory.getClockWarning(
              "HDLGenerator_NoClockConnection", "Clock", "main"));
      assertEquals(
          "Component \"Clock\" in circuit \"main\" has a gated clock connection!",
          AbstractHdlGeneratorFactory.getClockWarning(
              "HDLGenerator_GatedClockConnection", "Clock", "main"));

      LocaleManager.setLocale(Locale.SIMPLIFIED_CHINESE);
      assertEquals(
          "元件 \"Clock\" 在电路 \"main\" 中没有时钟连接!",
          AbstractHdlGeneratorFactory.getClockWarning(
              "HDLGenerator_NoClockConnection", "Clock", "main"));
      assertEquals(
          "元件 \"Clock\" 在电路 \"main\" 中有门控时钟连接!",
          AbstractHdlGeneratorFactory.getClockWarning(
              "HDLGenerator_GatedClockConnection", "Clock", "main"));
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }
}
