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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ToplevelHdlGeneratorFactoryTest {

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

  @Test
  void zeroPinComponentErrorUsesCurrentLocaleAndKeepsEmptyMapBehavior() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      assertZeroPinComponentError(
          Locale.ENGLISH,
          "BUG: Found a component with no pins. Please report this occurance!");
      assertZeroPinComponentError(
          Locale.SIMPLIFIED_CHINESE,
          "程序错误：发现一个没有引脚的元件。请报告此问题！");
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertZeroPinComponentError(Locale locale, String expectedError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var component = mock(MapComponent.class);
    when(component.getNrOfPins()).thenReturn(0);

    assertEquals(Map.of(), ToplevelHdlGeneratorFactory.getToplevelWires(component));

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_NORMAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
}
