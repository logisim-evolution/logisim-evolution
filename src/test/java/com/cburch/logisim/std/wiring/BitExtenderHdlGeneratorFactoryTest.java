/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BitExtenderHdlGeneratorFactoryTest {

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

  @Test
  void floatingInputErrorUsesCurrentLocaleAndKeepsEmptyBufferBehavior() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      assertFloatingInputError(
          Locale.ENGLISH,
          "Bit Extender component has floating input connection in circuit: TestCircuit");
      assertFloatingInputError(
          Locale.SIMPLIFIED_CHINESE,
          "电路 TestCircuit 中的位扩展器元件存在悬空输入连接");
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertFloatingInputError(Locale locale, String expectedError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var component = mock(netlistComponent.class);
    when(component.nrOfEnds()).thenReturn(2);
    when(component.isEndConnected(1)).thenReturn(false);

    assertTrue(
        new BitExtenderHdlGeneratorFactory()
            .getInlinedCode(null, 1L, component, "TestCircuit")
            .isEmpty());

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_NORMAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
}
