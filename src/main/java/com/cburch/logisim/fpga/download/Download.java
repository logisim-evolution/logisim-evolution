/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.Main;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.ComponentMapParser;
import com.cburch.logisim.fpga.gui.ComponentMapDialog;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class Download extends DownloadBase implements Runnable, BaseWindowListenerContract {

  private boolean stopRequested = false;

  private boolean downloadOnly;
  private boolean generateHdlOnly;
  private char vendor;
  private boolean useGui;
  private JProgressBar progressBar;
  private VendorDownload downloader;
  private String topLevelSheet;
  private double tickFrequency;
  private static final int basicSteps = 5;
  private String mapFileName;
  final ArrayList<String> entities = new ArrayList<>();
  final ArrayList<String> architectures = new ArrayList<>();

  private Process executable;
  private final Object lock = new Object();
  private JFrame parent;

  private final ArrayList<ActionListener> listeners = new ArrayList<>();

  public Download(
      Project myProject,
      String topLevelSheet,
      double tickFrequency,
      BoardInformation myBoardInformation,
      String mapFileName,
      boolean writeToFlash,
      boolean downloadOnly,
      boolean gegerateHdlOnly,
      JProgressBar progressBar,
      JFrame myParent) {
    this.progressBar = progressBar;
    parent = myParent;
    setUpDownload(
        myProject,
        topLevelSheet,
        tickFrequency,
        myBoardInformation,
        mapFileName,
        writeToFlash,
        downloadOnly,
        gegerateHdlOnly);
  }

  public Download(
      Project myProject,
      String topLevelSheet,
      double tickFrequency,
      BoardInformation myBoardInformation,
      String mapFileName,
      boolean writeToFlash,
      boolean downloadOnly,
      boolean generateHdlOnly) {
    setUpDownload(
        myProject,
        topLevelSheet,
        tickFrequency,
        myBoardInformation,
        mapFileName,
        writeToFlash,
        downloadOnly,
        generateHdlOnly);
  }

  private void setUpDownload(
      Project myProject,
      String topLevelSheet,
      double tickFrequency,
      BoardInformation myBoardInformation,
      String mapFileName,
      boolean writeToFlash,
      boolean downloadOnly,
      boolean generateHdlOnly) {
    this.myProject = myProject;
    this.myBoardInformation = myBoardInformation;
    this.downloadOnly = downloadOnly;
    this.generateHdlOnly = generateHdlOnly;
    if (myBoardInformation == null) {
      this.generateHdlOnly = true;
      this.vendor = ' ';
    } else {
      this.vendor = myBoardInformation.fpga.getVendor();
    }
    this.useGui = !Main.headless;
    this.topLevelSheet = topLevelSheet;
    this.tickFrequency = tickFrequency;
    this.mapFileName = mapFileName;
    final var rootSheet = myProject.getLogisimFile().getCircuit(topLevelSheet);
    if (rootSheet == null) return;
    var steps = basicSteps;
    if (!this.generateHdlOnly && useGui) rootSheet.setDownloadBoard(myBoardInformation.getBoardName());
    switch (vendor) {
      case VendorSoftware.VENDOR_ALTERA:
        downloader =
            new AlteraDownload(
                getProjDir(topLevelSheet),
                rootSheet.getNetList(),
                myBoardInformation,
                    entities,
                    architectures,
                AppPreferences.HdlType.get(),
                writeToFlash);
        break;
      case VendorSoftware.VENDOR_XILINX:
        downloader =
            new XilinxDownload(
                getProjDir(topLevelSheet),
                rootSheet.getNetList(),
                myBoardInformation,
                    entities,
                    architectures,
                AppPreferences.HdlType.get(),
                writeToFlash);
        break;
      case VendorSoftware.VENDOR_VIVADO:
        downloader =
            new VivadoDownload(
                getProjDir(topLevelSheet),
                rootSheet.getNetList(),
                myBoardInformation,
                    entities,
                    architectures);
        break;
      default:
        Reporter.report.addFatalError("BUG: Tried to Download to an unknown target");
        return;
    }
    if (progressBar == null) useGui = false;
    if (useGui) {
      if (downloader != null) steps += downloader.getNumberOfStages();
      progressBar.setMaximum(steps);
      progressBar.setString(S.get("FpgaDownloadInfo"));
    }
  }

  public void doDownload() {
    new Thread(this).start();
  }

  public void stop() {
    stopRequested = true;
    progressBar.setString(S.get("FpgaGuiCanceling"));
  }

  public boolean createDownloadScripts() {
    if (downloader != null) return downloader.createDownloadScripts();
    return false;
  }

  public void addListener(ActionListener listener) {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  public void removeListener(ActionListener listener) {
    listeners.remove(listener);
  }

  private void fireEvent(ActionEvent e) {
    for (var listener : listeners) {
      listener.actionPerformed(e);
    }
  }

  @Override
  public void run() {
    if (prepareDownload() && isVendorSoftwarePresent() && !generateHdlOnly) {
      try {
        var error = download();
        if (error != null) Reporter.report.addFatalError(error);
      } catch (IOException e) {
        Reporter.report.addFatalError(S.get("FPGAIOError", VendorSoftware.getVendorString(vendor)));
        e.printStackTrace();
      } catch (InterruptedException e) {
        Reporter.report.addError(S.get("FPGAInterruptedError", VendorSoftware.getVendorString(vendor)));
      }
    }
    fireEvent(new ActionEvent(this, 1, "DownloadDone"));
  }

  public boolean runTty() {
    final var root = myProject.getLogisimFile().getCircuit(topLevelSheet);
    if (root != null) {
      root.annotate(myProject, false, false);
    } else {
      Reporter.report.addFatalError("Toplevel sheet \"" + topLevelSheet + "\" not found in project!");
      return false;
    }
    if (!prepareDownload()) return false;
    if (generateHdlOnly) return true;
    if (!isVendorSoftwarePresent()) return false;
    try {
      var error = download();
      if (error != null) {
        Reporter.report.addFatalError(error);
        return false;
      }
    } catch (IOException e) {
      Reporter.report.addFatalError(S.get("FPGAIOError", VendorSoftware.getVendorString(vendor)));
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      Reporter.report.addError(S.get("FPGAInterruptedError", VendorSoftware.getVendorString(vendor)));
      return false;
    }
    return true;
  }

  private String download() throws IOException, InterruptedException {
    Reporter.report.clearConsole();
    if (!downloadOnly || !downloader.readyForDownload()) {
      for (var stages = 0; stages < downloader.getNumberOfStages(); stages++) {
        if (stopRequested) return S.get("FPGAInterrupted");
        final var currentStage = downloader.performStep(stages);
        if (currentStage != null) {
          final var result = execute(downloader.getStageMessage(stages), currentStage);
          if (result != null) return result;
        }
        if (useGui) progressBar.setValue(stages + basicSteps);
      }
    }
    if (useGui) progressBar.setValue(downloader.getNumberOfStages() + basicSteps - 1);
    if (generateHdlOnly) return null;
    if (stopRequested) return S.get("FPGAInterrupted");
    Object[] options = {S.get("FPGADownloadOk"), S.get("FPGADownloadCancel")};
    if (useGui)
      if (OptionPane.showOptionDialog(
              null,
              S.get("FPGAVerifyMsg1"),
              S.get("FPGAVerifyMsg2"),
              OptionPane.YES_NO_OPTION,
              OptionPane.WARNING_MESSAGE,
              null,
              options,
              options[0])
          != OptionPane.YES_OPTION) {
        return S.get("FPGADownloadAborted");
      }
    if (!downloader.isBoardConnected()) return S.get("FPGABoardNotConnected");
    var DownloadBitfile = downloader.downloadToBoard();
    if (DownloadBitfile != null) return execute(S.get("FPGADownloadBitfile"), DownloadBitfile);
    else return null;
  }

  public static String execute(ProcessBuilder process, List<String> report) throws IOException, InterruptedException {
    var executable = process.start();
    var is = executable.getInputStream();
    var isr = new InputStreamReader(is);
    var br = new BufferedReader(isr);
    var line = "";
    while ((line = br.readLine()) != null) {
      Reporter.report.print(line);
      if (report != null) report.add(line);
    }
    executable.waitFor();
    isr.close();
    br.close();
    if (executable.exitValue() != 0) return S.get("FPGAStaticExecutionFailure");
    return null;
  }

  private String execute(String StageName, ProcessBuilder process) throws IOException, InterruptedException {
    if (useGui) progressBar.setString(StageName);
    Reporter.report.print(" ");
    Reporter.report.print("==>");
    Reporter.report.print("==> " + StageName);
    Reporter.report.print("==>");
    synchronized (lock) {
      executable = process.start();
    }
    var is = executable.getInputStream();
    var isr = new InputStreamReader(is);
    var br = new BufferedReader(isr);
    var line = "";
    while ((line = br.readLine()) != null) {
      Reporter.report.print(line);
    }
    executable.waitFor();
    isr.close();
    br.close();
    if (executable.exitValue() != 0) {
      return S.get("FPGAExecutionFailure", StageName);
    }
    return null;
  }

  private boolean prepareDownload() {
    if (downloadOnly && downloader.readyForDownload()) return true;
    /* Stage 0 DRC */
    if (useGui) progressBar.setString(S.get("FPGAState0"));
    if (!performDrc(topLevelSheet, AppPreferences.HdlType.get())) {
      return false;
    }
    final var name = myProject.getLogisimFile().getName();
    if (name.contains(" ")) {
      Reporter.report.addFatalError(S.get("FPGANameContainsSpaces", name));
      return false;
    }
    /* Stage 1 Is design map able on Board */
    if (useGui) {
      progressBar.setValue(1);
      progressBar.setString(S.get("FPGAState2"));
    }
    if (!mapDesign(topLevelSheet)) {
      return false;
    }
    if (useGui) {
      /* Stage 2 Map design on board */
      progressBar.setValue(2);
      progressBar.setString(S.get("FPGAState3"));
      ComponentMapDialog mapPanel;
      if (myProject.getLogisimFile().getLoader().getMainFile() != null) {
        mapPanel = new ComponentMapDialog(parent,
                myProject.getLogisimFile().getLoader().getMainFile().getAbsolutePath(),
                myBoardInformation, myMappableResources);
      } else {
        mapPanel = new ComponentMapDialog(parent, "", myBoardInformation, myMappableResources);
      }
      if (!mapPanel.run()) {
        Reporter.report.addError(S.get("FPGADownloadAborted"));
        return false;
      }
    } else {
      if (mapFileName != null) {
        var mapFile = new File(mapFileName);
        if (!mapFile.exists()) return false;
        var cmp = new ComponentMapParser(mapFile, myMappableResources, myBoardInformation);
        cmp.parseFile();
      }
    }
    if (!mapDesignCheckIOs()) {
      Reporter.report.addError(S.get("FPGAMapNotComplete", myBoardInformation.getBoardName()));
      return false;
    }
    /* Stage 3 HDL generation */
    if (useGui) {
      progressBar.setValue(3);
      progressBar.setString(S.get("FPGAState1"));
    }
    if (tickFrequency <= 0) tickFrequency = 1;
    if (tickFrequency > (myBoardInformation.fpga.getClockFrequency() / 4))
      tickFrequency = myBoardInformation.fpga.getClockFrequency() / 4;
    if (!writeHDL(topLevelSheet, tickFrequency)) {
      return false;
    }
    final var projectPath = getProjDir(topLevelSheet);
    final var sourcePath = projectPath + AppPreferences.HdlType.get().toLowerCase() + File.separator;
    getVhdlFiles(projectPath, sourcePath, entities, architectures, AppPreferences.HdlType.get());
    if (useGui) {
      progressBar.setValue(4);
      progressBar.setString(S.get("FPGAState4"));
    }
    downloader.setMapableResources(myMappableResources);
    /* Stage 4 Create Download Scripts */
    return createDownloadScripts();
  }

  @Override
  public void windowClosing(WindowEvent e) {
    progressBar.setString(S.get("FPGACancelWait"));
    stopRequested = true;
    synchronized (lock) {
      if (executable != null) {
        executable.destroy();
      }
    }
  }

  public static String getClockFrequencyString(BoardInformation currentBoard) {
    var clkfreq = currentBoard.fpga.getClockFrequency();
    if (clkfreq % 1000000 == 0) {
      clkfreq /= 1000000;
      return clkfreq + " MHz ";
    } else if (clkfreq % 1000 == 0) {
      clkfreq /= 1000;
      return clkfreq + " kHz ";
    }
    return Long.toString(clkfreq);
  }

  public static String chooseBoard(List<String> devices) {
    /* This code is based on the version of Kevin Walsh */
    if (Main.hasGui()) {
      var choices = new String[devices.size()];
      for (var i = 0; i < devices.size(); i++) choices[i] = devices.get(i);
      return (String)
          OptionPane.showInputDialog(
              null,
              S.get("FPGAMultipleBoards", devices.size()),
              S.get("FPGABoardSelection"),
              OptionPane.QUESTION_MESSAGE,
              null,
              choices,
              choices[0]);
    } else {
      /* TODO: add none gui selection */
      return null;
    }
  }

}
