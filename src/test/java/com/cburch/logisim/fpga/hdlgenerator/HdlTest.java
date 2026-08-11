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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.cburch.logisim.TestBase;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.util.LocaleManager;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HdlTest extends TestBase {

  private record Item(String methodName, String vhdl, String verilog) {}

  @AfterEach
  void detachReporterGui() {
    Reporter.report.setGuiLogger(null);
  }

  @Test
  void emptyHdlDescriptionErrorsUseCurrentLocaleAndKeepComponentName() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      assertEmptyDescriptionErrors(
          Locale.ENGLISH,
          "INTERNAL ERROR: Empty entity description received!",
          "INTERNAL ERROR: Empty behavior description for Component 'TestComponent' received!");
      assertEmptyDescriptionErrors(
          Locale.SIMPLIFIED_CHINESE,
          "内部错误：收到的实体描述为空！",
          "内部错误：收到的元件“TestComponent”行为描述为空！");
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }

  /** Ensures HDL markers are as we expect them. */
  @Test
  public void testMarkers() {
    final var tests =
        List.of(
            new Item("bracketOpen", "(", "["),
            new Item("bracketClose", ")", "]"),
            new Item("getRemarkChar", "-", "*"),
            new Item("getRemarkBlockStart", "---", "/**"),
            new Item("getRemarkBlockEnd", "---", "**/"),
            new Item("getRemarkBlockLineStart", "-- ", "** "),
            new Item("getRemarkBlockLineEnd", " --", " **"),
            new Item("getLineCommentStart", "-- ", "// "));

    for (final var test : tests) {
      assertEquals(test.vhdl, callMockedHdl(test.methodName, true));
      assertEquals(test.verilog, callMockedHdl(test.methodName, false));
    }
  }

  /**
   * Ensures return values of specified methods are the same length for both VHDL and Verilog and
   * that this length matches value of Hdl.REMARK_MARKER_LENGTH constant.
   */
  @Test
  public void testMarkersLength() {
    final var tests =
        List.of(
            "getRemarkBlockStart",
            "getRemarkBlockEnd",
            "getRemarkBlockLineStart",
            "getRemarkBlockLineEnd");

    for (final var test : tests) {
      final var vhdl = callMockedHdl(test, true);
      final var verilog = callMockedHdl(test, false);
      assertEquals(vhdl.length(), Hdl.REMARK_MARKER_LENGTH);
      assertEquals(verilog.length(), Hdl.REMARK_MARKER_LENGTH);
    }
  }

  protected String callMockedHdl(String methodName, boolean isVhdl) {
    String result = null;
    try {
      final var mockedHdl = mockStatic(Hdl.class, Mockito.CALLS_REAL_METHODS);
      final var method = Hdl.class.getMethod(methodName);
      mockedHdl.when(Hdl::isVhdl).thenReturn(isVhdl);
      result = (String) method.invoke(mockedHdl);
      mockedHdl.close();
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ex) {
      ex.printStackTrace();
      fail();
    }
    return result;
  }

  private void assertEmptyDescriptionErrors(
      Locale locale, String expectedEntityError, String expectedBehaviorError) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var error = ArgumentCaptor.forClass(Object.class);

    try (final var mockedHdl = mockStatic(Hdl.class, Mockito.CALLS_REAL_METHODS)) {
      mockedHdl.when(Hdl::isVhdl).thenReturn(true);
      assertFalse(Hdl.writeEntity("unused", List.of(), "TestComponent"));
    }
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedEntityError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());

    clearInvocations(reporterGui);
    assertFalse(Hdl.writeArchitecture("unused", List.of(), "TestComponent"));
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedBehaviorError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
} // end of Test class
