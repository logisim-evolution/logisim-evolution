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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleManager;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DownloadBaseLocalizationTest {

  private final Locale originalLocale = LocaleManager.getLocale();
  private final String originalWorkspace = AppPreferences.FPGA_Workspace.get();

  @AfterEach
  void restoreGlobals() {
    Reporter.report.setGuiLogger(null);
    LocaleManager.setLocale(originalLocale);
    AppPreferences.FPGA_Workspace.set(originalWorkspace);
  }

  @Test
  void missingCircuitAndBoardInformationUseCurrentLocaleAndRemainNormalErrors() {
    final var project = mock(Project.class);
    final var file = mock(LogisimFile.class);
    final var circuit = mock(Circuit.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(file.getCircuit("Missing")).thenReturn(null);
    when(file.getCircuit("Main")).thenReturn(circuit);

    assertNormalError(
        Locale.ENGLISH,
        "Circuit not found.",
        () -> assertFalse(new TestDownloadBase(project, null).mapDesignForTest("Missing")));
    assertNormalError(
        Locale.SIMPLIFIED_CHINESE,
        "未找到电路",
        () -> assertFalse(new TestDownloadBase(project, null).mapDesignForTest("Missing")));
    assertNormalError(
        Locale.ENGLISH,
        "No board information is available.",
        () -> assertFalse(new TestDownloadBase(project, null).mapDesignForTest("Main")));
    assertNormalError(
        Locale.SIMPLIFIED_CHINESE,
        "没有可用的板卡信息",
        () -> assertFalse(new TestDownloadBase(project, null).mapDesignForTest("Main")));
  }

  @Test
  void boardContentsInformationUsesCurrentLocaleAndKeepsMappingBehavior() {
    assertBoardContentsInfo(Locale.ENGLISH, "The Board TestBoard has:", "2 LED(s)");
    assertBoardContentsInfo(Locale.SIMPLIFIED_CHINESE, "板卡 TestBoard 包含：", "2 个 LED");
  }

  @Test
  void workspaceCreationAndCleanupFailuresUseCurrentLocaleAndRemainFatal() {
    AppPreferences.FPGA_Workspace.set("TestWorkspace");
    final var project = projectNamed("TestProject", mock(Circuit.class));
    final var workspaceProject = "TestWorkspace" + File.separator + "TestProject";
    final var circuitProject = workspaceProject + File.separator + "Main" + File.separator;

    assertFatalError(
        Locale.ENGLISH,
        "Unable to create directory: \"" + workspaceProject + "\"",
        () -> assertFalse(new TestDownloadBase(project, null, true, false).writeHdlForTest("Main")));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "无法创建目录：“" + workspaceProject + "”",
        () -> assertFalse(new TestDownloadBase(project, null, true, false).writeHdlForTest("Main")));
    assertFatalError(
        Locale.ENGLISH,
        "Unable to cleanup old project files in directory: \"" + circuitProject + "\"",
        () -> assertFalse(new TestDownloadBase(project, null, false, true).writeHdlForTest("Main")));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "无法清理目录“" + circuitProject + "”中的旧工程文件",
        () -> assertFalse(new TestDownloadBase(project, null, false, true).writeHdlForTest("Main")));
  }

  @Test
  void nullHdlGeneratorUsesCurrentLocaleAndKeepsFalseResult() {
    AppPreferences.FPGA_Workspace.set("TestWorkspace");
    final var circuit = mock(Circuit.class);
    final var factory = mock(SubcircuitFactory.class);
    final var attrs = mock(AttributeSet.class);
    when(circuit.getSubcircuitFactory()).thenReturn(factory);
    when(circuit.getStaticAttributes()).thenReturn(attrs);
    when(factory.getHDLGenerator(attrs)).thenReturn(null);
    final var project = projectNamed("TestProject", circuit);

    assertFatalError(
        Locale.ENGLISH,
        "No HDL generator is available for the top-level circuit.",
        () -> assertFalse(new TestDownloadBase(project, null).writeHdlForTest("Main")));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "顶层电路没有可用的 HDL 生成器",
        () -> assertFalse(new TestDownloadBase(project, null).writeHdlForTest("Main")));
  }

  @Test
  void directoryExceptionMessagesUseCurrentLocaleAndKeepFalseResults() {
    assertFatalError(
        Locale.ENGLISH,
        "Could not check/create directory :null",
        () -> assertFalse(new TestDownloadBase(null, null).genDirectoryForTest(null)));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "无法检查或创建目录：null",
        () -> assertFalse(new TestDownloadBase(null, null).genDirectoryForTest(null)));
    assertFatalError(
        Locale.ENGLISH,
        "Could not remove directory tree :null",
        () -> assertFalse(new TestDownloadBase(null, null).cleanDirectoryForTest(null)));
    assertFatalError(
        Locale.SIMPLIFIED_CHINESE,
        "无法移除目录树：null",
        () -> assertFalse(new TestDownloadBase(null, null).cleanDirectoryForTest(null)));
  }

  private void assertBoardContentsInfo(Locale locale, String boardInfo, String componentInfo) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);
    final var project = mock(Project.class);
    final var file = mock(LogisimFile.class);
    final var circuit = mock(Circuit.class);
    final var board = mock(BoardInformation.class);
    final var maps = mock(MappableResourcesContainer.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(file.getCircuit("Main")).thenReturn(circuit);
    when(board.getBoardName()).thenReturn("TestBoard");
    when(board.getComponents()).thenReturn(Map.of("LED", new ArrayList<>(java.util.List.of(1, 1))));
    when(circuit.getBoardMap("TestBoard")).thenReturn(maps);

    assertTrue(new TestDownloadBase(project, board).mapDesignForTest("Main"));

    verify(reporterGui).addInfo(boardInfo);
    verify(reporterGui).addInfo(componentInfo);
  }

  private Project projectNamed(String name, Circuit circuit) {
    final var project = mock(Project.class);
    final var file = mock(LogisimFile.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(file.getName()).thenReturn(name);
    when(file.getCircuit("Main")).thenReturn(circuit);
    return project;
  }

  private void assertNormalError(Locale locale, String expectedError, Runnable action) {
    assertReporterError(locale, expectedError, SimpleDrcContainer.LEVEL_NORMAL, action);
  }

  private void assertFatalError(Locale locale, String expectedError, Runnable action) {
    assertReporterError(locale, expectedError, SimpleDrcContainer.LEVEL_FATAL, action);
  }

  private void assertReporterError(
      Locale locale, String expectedError, int expectedSeverity, Runnable action) {
    LocaleManager.setLocale(locale);
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);

    action.run();

    final var error = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui).addErrors(error.capture());
    assertEquals(expectedError, error.getValue().toString());
    assertEquals(expectedSeverity, ((SimpleDrcContainer) error.getValue()).getSeverity());
  }

  private static class TestDownloadBase extends DownloadBase {
    private final ArrayDeque<Boolean> directoryResults = new ArrayDeque<>();
    private final boolean cleanupResult;

    private TestDownloadBase(Project project, BoardInformation board, boolean cleanupResult, Boolean... directoryResults) {
      myProject = project;
      myBoardInformation = board;
      this.cleanupResult = cleanupResult;
      this.directoryResults.addAll(java.util.List.of(directoryResults));
    }

    private TestDownloadBase(Project project, BoardInformation board) {
      this(project, board, true);
    }

    private boolean mapDesignForTest(String circuitName) {
      return mapDesign(circuitName);
    }

    private boolean writeHdlForTest(String circuitName) {
      return writeHDL(circuitName, 1.0);
    }

    private boolean genDirectoryForTest(String path) {
      return super.genDirectory(path);
    }

    private boolean cleanDirectoryForTest(String path) {
      return super.cleanDirectory(path);
    }

    @Override
    protected boolean genDirectory(String path) {
      return directoryResults.isEmpty() || directoryResults.removeFirst();
    }

    @Override
    boolean cleanDirectory(String path) {
      return cleanupResult;
    }
  }
}
