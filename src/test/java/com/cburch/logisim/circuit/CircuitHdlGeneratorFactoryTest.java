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
import com.cburch.logisim.fpga.designrulecheck.ConnectionEnd;
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.Net;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
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

  @Test
  void unconnectedOutputWarningsUseCurrentLocaleAndKeepGroundingBehavior() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      assertUnconnectedOutputWarnings(
          Locale.ENGLISH,
          "Found an unconnected output pin, tied the pin to ground!",
          "Found an unconnected output bus pin, tied all the pin bits to ground!",
          "Found an unconnected output bus pin, tied bit 0 to ground!");
      assertUnconnectedOutputWarnings(
          Locale.SIMPLIFIED_CHINESE,
          "发现未连接的输出引脚；已将该引脚接地！",
          "发现未连接的输出总线引脚；已将该总线引脚的所有位接地！",
          "发现未连接的输出总线引脚；已将第 0 位接地！");
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }

  private void assertUnconnectedOutputWarnings(
      Locale locale, String pinWarning, String busWarning, String busBitWarning) {
    setLocaleAndReloadBundles(locale);
    assertUnconnectedOutputPinWarning(pinWarning);
    assertUnconnectedOutputBusWarning(busWarning);
    assertPartiallyConnectedOutputBusWarning(busBitWarning);
  }

  private void assertUnconnectedOutputPinWarning(String expectedWarning) {
    final var reporterGui = attachReporterGui();
    final var component = mock(netlistComponent.class);
    final var connection = mock(ConnectionEnd.class);
    when(component.nrOfEnds()).thenReturn(1);
    when(component.getEnd(0)).thenReturn(connection);
    when(connection.getNrOfBits()).thenReturn(1);
    when(connection.isOutputEnd()).thenReturn(false);
    when(component.isEndConnected(0)).thenReturn(false);

    assertEquals(
        Map.of("output", Hdl.zeroBit()),
        CircuitHdlGeneratorFactory.getSignalMap("output", component, 0, null));
    assertSingleSevereWarning(reporterGui, expectedWarning);
  }

  private void assertUnconnectedOutputBusWarning(String expectedWarning) {
    final var reporterGui = attachReporterGui();
    final var component = mock(netlistComponent.class);
    final var connection = mock(ConnectionEnd.class);
    when(component.nrOfEnds()).thenReturn(1);
    when(component.getEnd(0)).thenReturn(connection);
    when(connection.getNrOfBits()).thenReturn(2);
    when(connection.isOutputEnd()).thenReturn(false);
    when(connection.get((byte) 0)).thenReturn(mock(ConnectionPoint.class));
    when(connection.get((byte) 1)).thenReturn(mock(ConnectionPoint.class));

    assertEquals(
        Map.of("output", Hdl.getZeroVector(2, true)),
        CircuitHdlGeneratorFactory.getSignalMap("output", component, 0, null));
    assertSingleSevereWarning(reporterGui, expectedWarning);
  }

  private void assertPartiallyConnectedOutputBusWarning(String expectedWarning) {
    final var reporterGui = attachReporterGui();
    final var component = mock(netlistComponent.class);
    final var connection = mock(ConnectionEnd.class);
    final var disconnectedPoint = mock(ConnectionPoint.class);
    final var connectedPoint = mock(ConnectionPoint.class);
    final var connectedNet = mock(Net.class);
    final var netlist = mock(Netlist.class);
    when(component.nrOfEnds()).thenReturn(1);
    when(component.getEnd(0)).thenReturn(connection);
    when(connection.getNrOfBits()).thenReturn(2);
    when(connection.isOutputEnd()).thenReturn(false);
    when(connection.get((byte) 0)).thenReturn(disconnectedPoint);
    when(connection.get((byte) 1)).thenReturn(connectedPoint);
    when(connectedPoint.getParentNet()).thenReturn(connectedNet);
    when(connectedNet.getBitWidth()).thenReturn(1);
    when(netlist.isContinuesBus(component, 0)).thenReturn(false);
    when(netlist.getNetId(connectedNet)).thenReturn(7);

    final var signal =
        CircuitHdlGeneratorFactory.getSignalMap("output", component, 0, netlist);

    assertEquals(2, signal.size());
    assertEquals(
        Hdl.zeroBit(), signal.get(LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", "output", 0)));
    assertSingleSevereWarning(reporterGui, expectedWarning);
  }

  private FpgaReportTabbedPane attachReporterGui() {
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    return reporterGui;
  }

  private void assertSingleSevereWarning(
      FpgaReportTabbedPane reporterGui, String expectedWarning) {
    final var warning = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addWarning(warning.capture());
    assertEquals(expectedWarning, warning.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_SEVERE,
        ((SimpleDrcContainer) warning.getValue()).getSeverity());
  }

  private void assertInvalidSolderPointError(Locale locale, String expectedError) {
    setLocaleAndReloadBundles(locale);
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

  private void setLocaleAndReloadBundles(Locale locale) {
    Hdl.zeroBit();
    com.cburch.logisim.fpga.Strings.S.get("HdlUnconnectedOutputPinWarning");
    LocaleManager.setLocale(
        Locale.ENGLISH.equals(locale) ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH);
    LocaleManager.setLocale(locale);
  }
}
