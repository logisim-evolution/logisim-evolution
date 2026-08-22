/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.FpgaClass;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DownloadLocalizationTest {

  private final Locale originalLocale = LocaleManager.getLocale();

  @AfterEach
  void restoreGlobals() {
    Reporter.report.setGuiLogger(null);
    LocaleManager.setLocale(originalLocale);
  }

  @Test
  void unknownDownloadTargetUsesCurrentLocaleAndRemainsFatal() {
    final var project = mock(Project.class);
    final var file = mock(LogisimFile.class);
    final var root = mock(Circuit.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(file.getCircuit("Main")).thenReturn(root);
    final var board = new BoardInformation();
    board.fpga = mock(FpgaClass.class);
    when(board.fpga.getVendor()).thenReturn(Character.MAX_VALUE);

    assertFatalError(
        Locale.ENGLISH,
        "BUG: Tried to Download to an unknown target",
        () -> new Download(project, "Main", 1.0, board, null, false, false, true, 1.0, 1.0));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "程序错误：尝试下载到未知目标",
        () -> new Download(project, "Main", 1.0, board, null, false, false, true, 1.0, 1.0));
  }

  @Test
  void missingToplevelSheetUsesCurrentLocaleAndKeepsFalseResult() {
    final var project = mock(Project.class);
    final var file = mock(LogisimFile.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(file.getCircuit("Missing")).thenReturn(null);

    assertFatalError(
        Locale.ENGLISH,
        "Toplevel sheet \"Missing\" not found in project!",
        () -> assertFalse(newDownloadWithoutBoard(project, "Missing").runTty()));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "项目中未找到顶层电路“Missing”！",
        () -> assertFalse(newDownloadWithoutBoard(project, "Missing").runTty()));
  }

  private Download newDownloadWithoutBoard(Project project, String circuitName) {
    return new Download(
        project, circuitName, 1.0, null, null, false, false, true, 1.0, 1.0);
  }

  private void assertFatalError(Locale locale, String expectedError, Runnable action) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);

    action.run();

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(
        SimpleDrcContainer.LEVEL_FATAL, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }
}
