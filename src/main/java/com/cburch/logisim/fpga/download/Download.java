/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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

  private boolean StopRequested = false;

  private boolean DownloadOnly;
  private boolean HdlOnly;
  private char Vendor;
  private boolean UseGui;
  private JProgressBar MyProgress;
  private VendorDownload Downloader;
  private String TopLevelSheet;
  private double TickFrequency;
  private static final int BasicSteps = 5;
  private String MapFileName;
  final ArrayList<String> Entities = new ArrayList<>();
  final ArrayList<String> Architectures = new ArrayList<>();

  private Process Executable;
  private final Object lock = new Object();
  private JFrame parent;


  private final ArrayList<ActionListener> Listeners = new ArrayList<>();

  public Download(
      Project MyProject,
      String TopLevelSheet,
      double TickFrequency,
      BoardInformation MyBoardInformation,
      String MapFileName,
      boolean writeToFlash,
      boolean DownloadOnly,
      boolean gegerateHdlOnly,
      JProgressBar Progress,
      JFrame myParent) {
    MyProgress = Progress;
    parent = myParent;
    SetUpDownload(
        MyProject,
        TopLevelSheet,
        TickFrequency,
        MyBoardInformation,
        MapFileName,
        writeToFlash,
        DownloadOnly,
        gegerateHdlOnly);
  }

  public Download(
      Project MyProject,
      String TopLevelSheet,
      double TickFrequency,
      BoardInformation MyBoardInformation,
      String MapFileName,
      boolean writeToFlash,
      boolean DownloadOnly,
      boolean gegerateHdlOnly) {
    SetUpDownload(
        MyProject,
        TopLevelSheet,
        TickFrequency,
        MyBoardInformation,
        MapFileName,
        writeToFlash,
        DownloadOnly,
        gegerateHdlOnly);
  }

  private void SetUpDownload(
      Project MyProject,
      String TopLevelSheet,
      double TickFrequency,
      BoardInformation MyBoardInformation,
      String MapFileName,
      boolean writeToFlash,
      boolean DownloadOnly,
      boolean gegerateHdlOnly) {
    this.MyProject = MyProject;
    this.MyBoardInformation = MyBoardInformation;
    this.DownloadOnly = DownloadOnly;
    this.HdlOnly = gegerateHdlOnly;
    if (MyBoardInformation == null) {
      this.HdlOnly = true;
      this.Vendor = ' ';
    } else {
      this.Vendor = MyBoardInformation.fpga.getVendor();
    }
    this.UseGui = !Main.headless;
    this.TopLevelSheet = TopLevelSheet;
    this.TickFrequency = TickFrequency;
    this.MapFileName = MapFileName;
    final var RootSheet = MyProject.getLogisimFile().getCircuit(TopLevelSheet);
    if (RootSheet == null) return;
    var steps = BasicSteps;
    switch (Vendor) {
      case VendorSoftware.VENDOR_ALTERA:
        Downloader =
            new AlteraDownload(
                getProjDir(TopLevelSheet),
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures,
                AppPreferences.HDL_Type.get(),
                writeToFlash);
        break;
      case VendorSoftware.VENDOR_XILINX:
        Downloader =
            new XilinxDownload(
                getProjDir(TopLevelSheet),
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures,
                AppPreferences.HDL_Type.get(),
                writeToFlash);
        break;
      case VendorSoftware.VENDOR_VIVADO:
        Downloader =
            new VivadoDownload(
                getProjDir(TopLevelSheet),
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures);
        break;
      default:
        Reporter.Report.AddFatalError("BUG: Tried to Download to an unknown target");
        return;
    }
    if (MyProgress == null) UseGui = false;
    if (UseGui) {
      if (Downloader != null) steps += Downloader.GetNumberOfStages();
      MyProgress.setMaximum(steps);
      MyProgress.setString(S.get("FpgaDownloadInfo"));
    }
  }

  public void DoDownload() {
    new Thread(this).start();
  }

  public void stop() {
    StopRequested = true;
    MyProgress.setString(S.get("FpgaGuiCanceling"));
  }

  public boolean CreateDownloadScripts() {
    if (Downloader != null) return Downloader.CreateDownloadScripts();
    return false;
  }

  public void AddListener(ActionListener listener) {
    if (!Listeners.contains(listener)) Listeners.add(listener);
  }

  public void RemoveListener(ActionListener listener) {
    Listeners.remove(listener);
  }

  private void fireEvent(ActionEvent e) {
    for (var listener : Listeners) {
      listener.actionPerformed(e);
    }
  }

  @Override
  public void run() {
    if (PrepareDownLoad() && VendorSoftwarePresent() && !HdlOnly) {
      try {
        var error = download();
        if (error != null) Reporter.Report.AddFatalError(error);
      } catch (IOException e) {
        Reporter.Report.AddFatalError(S.get("FPGAIOError", VendorSoftware.getVendorString(Vendor)));
        e.printStackTrace();
      } catch (InterruptedException e) {
        Reporter.Report.AddError(S.get("FPGAInterruptedError", VendorSoftware.getVendorString(Vendor)));
      }
    }
    fireEvent(new ActionEvent(this, 1, "DownloadDone"));
  }

  public boolean runtty() {
    final var root = MyProject.getLogisimFile().getCircuit(TopLevelSheet);
    if (root != null) {
      root.Annotate(MyProject, false, false);
    } else {
      Reporter.Report.AddFatalError(
          "Toplevel sheet \"" + TopLevelSheet + "\" not found in project!");
      return false;
    }
    if (!PrepareDownLoad()) return false;
    if (HdlOnly) return true;
    if (!VendorSoftwarePresent()) return false;
    try {
      var error = download();
      if (error != null) {
        Reporter.Report.AddFatalError(error);
        return false;
      }
    } catch (IOException e) {
      Reporter.Report.AddFatalError(S.get("FPGAIOError", VendorSoftware.getVendorString(Vendor)));
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      Reporter.Report.AddError(S.get("FPGAInterruptedError", VendorSoftware.getVendorString(Vendor)));
      return false;
    }
    return true;
  }

  private String download() throws IOException, InterruptedException {
    Reporter.Report.ClsScr();
    if (!DownloadOnly || !Downloader.readyForDownload()) {
      for (var stages = 0; stages < Downloader.GetNumberOfStages(); stages++) {
        if (StopRequested) return S.get("FPGAInterrupted");
        var CurrentStage = Downloader.PerformStep(stages);
        if (CurrentStage != null) {
          var result = execute(Downloader.GetStageMessage(stages), CurrentStage);
          if (result != null) return result;
        }
        if (UseGui) MyProgress.setValue(stages + BasicSteps);
      }
    }
    if (UseGui) MyProgress.setValue(Downloader.GetNumberOfStages() + BasicSteps - 1);
    if (HdlOnly) return null;
    if (StopRequested) return S.get("FPGAInterrupted");
    Object[] options = {S.get("FPGADownloadOk"), S.get("FPGADownloadCancel")};
    if (UseGui)
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
    if (!Downloader.BoardConnected()) return S.get("FPGABoardNotConnected");
    var DownloadBitfile = Downloader.DownloadToBoard();
    if (DownloadBitfile != null) return execute(S.get("FPGADownloadBitfile"), DownloadBitfile);
    else return null;
  }

  public static String execute(
      ProcessBuilder process, ArrayList<String> Report)
      throws IOException, InterruptedException {
    var Executable = process.start();
    var is = Executable.getInputStream();
    var isr = new InputStreamReader(is);
    var br = new BufferedReader(isr);
    var line = "";
    while ((line = br.readLine()) != null) {
      Reporter.Report.print(line);
      if (Report != null) Report.add(line);
    }
    Executable.waitFor();
    isr.close();
    br.close();
    if (Executable.exitValue() != 0) return S.get("FPGAStaticExecutionFailure");
    return null;
  }

  private String execute(String StageName, ProcessBuilder process)
      throws IOException, InterruptedException {
    if (UseGui) MyProgress.setString(StageName);
    Reporter.Report.print(" ");
    Reporter.Report.print("==>");
    Reporter.Report.print("==> " + StageName);
    Reporter.Report.print("==>");
    synchronized (lock) {
      Executable = process.start();
    }
    var is = Executable.getInputStream();
    var isr = new InputStreamReader(is);
    var br = new BufferedReader(isr);
    var line = "";
    while ((line = br.readLine()) != null) {
      Reporter.Report.print(line);
    }
    Executable.waitFor();
    isr.close();
    br.close();
    if (Executable.exitValue() != 0) {
      return S.get("FPGAExecutionFailure", StageName);
    }
    return null;
  }

  private boolean PrepareDownLoad() {
    if (DownloadOnly && Downloader.readyForDownload()) return true;
    /* Stage 0 DRC */
    if (UseGui) MyProgress.setString(S.get("FPGAState0"));
    if (!performDRC(TopLevelSheet, AppPreferences.HDL_Type.get())) {
      return false;
    }
    final var Name = MyProject.getLogisimFile().getName();
    if (Name.contains(" ")) {
      Reporter.Report.AddFatalError(S.get("FPGANameContainsSpaces", Name));
      return false;
    }
    /* Stage 1 Is design map able on Board */
    if (UseGui) {
      MyProgress.setValue(1);
      MyProgress.setString(S.get("FPGAState2"));
    }
    if (!MapDesign(TopLevelSheet)) {
      return false;
    }
    if (UseGui) {
      /* Stage 2 Map design on board */
      MyProgress.setValue(2);
      MyProgress.setString(S.get("FPGAState3"));
      ComponentMapDialog MapPannel;
      if (MyProject.getLogisimFile().getLoader().getMainFile() != null) {
        MapPannel = new ComponentMapDialog(parent,
                MyProject.getLogisimFile().getLoader().getMainFile().getAbsolutePath(),
                MyBoardInformation, myMappableResources);
      } else {
        MapPannel = new ComponentMapDialog(parent, "", MyBoardInformation, myMappableResources);
      }
      if (!MapPannel.run()) {
        Reporter.Report.AddError(S.get("FPGADownloadAborted"));
        return false;
      }
    } else {
      if (MapFileName != null) {
        var MapFile = new File(MapFileName);
        if (!MapFile.exists()) return false;
        var cmp = new ComponentMapParser(MapFile, myMappableResources, MyBoardInformation);
        cmp.parseFile();
      }
    }
    if (!mapDesignCheckIOs()) {
      Reporter.Report.AddError(S.get("FPGAMapNotComplete", MyBoardInformation.getBoardName()));
      return false;
    }
    /* Stage 3 HDL generation */
    if (UseGui) {
      MyProgress.setValue(3);
      MyProgress.setString(S.get("FPGAState1"));
    }
    if (TickFrequency <= 0) TickFrequency = 1;
    if (TickFrequency > (MyBoardInformation.fpga.getClockFrequency() / 4))
      TickFrequency = MyBoardInformation.fpga.getClockFrequency() / 4;
    if (!writeHDL(TopLevelSheet, TickFrequency)) {
      return false;
    }
    final var ProjectPath = getProjDir(TopLevelSheet);
    final var SourcePath = ProjectPath + AppPreferences.HDL_Type.get().toLowerCase() + File.separator;
    getVhdlFiles(ProjectPath, SourcePath, Entities, Architectures, AppPreferences.HDL_Type.get());
    if (UseGui) {
      MyProgress.setValue(4);
      MyProgress.setString(S.get("FPGAState4"));
    }
    Downloader.SetMapableResources(myMappableResources);
    /* Stage 4 Create Download Scripts */
    return CreateDownloadScripts();
  }

  @Override
  public void windowClosing(WindowEvent e) {
    MyProgress.setString(S.get("FPGACancelWait"));
    StopRequested = true;
    synchronized (lock) {
      if (Executable != null) {
        Executable.destroy();
      }
    }
  }

  public static String GetClockFrequencyString(BoardInformation CurrentBoard) {
    var clkfreq = CurrentBoard.fpga.getClockFrequency();
    if (clkfreq % 1000000 == 0) {
      clkfreq /= 1000000;
      return clkfreq + " MHz ";
    } else if (clkfreq % 1000 == 0) {
      clkfreq /= 1000;
      return clkfreq + " kHz ";
    }
    return Long.toString(clkfreq);
  }

  public static String ChooseBoard(List<String> devices) {
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
