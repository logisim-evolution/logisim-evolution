/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleManager;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BinToBcdHdlGeneratorFactoryTest {

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

  @Test
  void unsupportedVerilogErrorUsesCurrentLocaleAndKeepsBufferBehavior() {
    final var originalLocale = LocaleManager.getLocale();
    final var originalHdlType = AppPreferences.HdlType.get();
    try {
      AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
      assertUnsupportedVerilogError(
          Locale.ENGLISH,
          "Strange, this should not happen as Verilog is not yet supported!\n");
      assertUnsupportedVerilogError(
          Locale.SIMPLIFIED_CHINESE,
          "奇怪，这不应该发生，因为尚不支持 Verilog！\n");
    } finally {
      AppPreferences.HdlType.set(originalHdlType);
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertUnsupportedVerilogError(Locale locale, String expectedError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var attrs = mock(AttributeSet.class);
    when(attrs.getValue(BinToBcd.ATTR_BinBits)).thenReturn(BitWidth.create(9));

    assertEquals(
        List.of(""), new BinToBcdHdlGeneratorFactory().getModuleFunctionality(null, attrs).get());

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
}
