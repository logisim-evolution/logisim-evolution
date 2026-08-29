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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AbstractHdlGeneratorFactoryTest {

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

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

  @Test
  void missingParameterErrorUsesCurrentLocaleAndKeepsFatalReturnBehavior() {
    final var originalLocale = LocaleManager.getLocale();
    final var originalHdlType = AppPreferences.HdlType.get();
    try {
      AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
      assertMissingParameterError(
          Locale.ENGLISH,
          "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
      assertMissingParameterError(
          Locale.SIMPLIFIED_CHINESE,
          "内部错误：HDL 生成过程中缺少参数，生成的 HDL 代码将无法工作！");
    } finally {
      AppPreferences.HdlType.set(originalHdlType);
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertMissingParameterError(Locale locale, String expectedError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var netlist = mock(Netlist.class);
    final var attrs = mock(AttributeSet.class);
    when(netlist.projName()).thenReturn("TestProject");

    assertNull(new MissingParameterGenerator().getArchitecture(netlist, attrs, "TestComponent"));

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }

  private static class MissingParameterGenerator extends AbstractHdlGeneratorFactory {
    private MissingParameterGenerator() {
      myPorts.add(Port.INPUT, "input", -1, 0);
    }
  }
}
