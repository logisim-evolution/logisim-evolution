/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.TestBase;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CircuitHdlGeneratorFactoryTest extends TestBase {

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

  @Test
  void invalidSolderPointErrorUsesCurrentLocaleAndKeepsComponentName() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      assertInvalidSolderPointError(
          Locale.ENGLISH,
          "INTERNAL ERROR: Component tried to index non-existing SolderPoint: 'TestComponent'");
      assertInvalidSolderPointError(
          Locale.SIMPLIFIED_CHINESE,
          "内部错误：元件尝试索引不存在的连接点：\"TestComponent\"");
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertInvalidSolderPointError(Locale locale, String expectedError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var component = mock(netlistComponent.class);
    final var circuitComponent = mock(Component.class);
    final var attributes = mock(AttributeSet.class);
    when(component.nrOfEnds()).thenReturn(0);
    when(component.getComponent()).thenReturn(circuitComponent);
    when(circuitComponent.getAttributeSet()).thenReturn(attributes);
    when(attributes.getValue(StdAttr.LABEL)).thenReturn("TestComponent");

    assertEquals(
        Map.of(), CircuitHdlGeneratorFactory.getSignalMap("unused", component, 0, null));

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
}
