/**
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

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.ComponentMapParser;
import com.cburch.logisim.fpga.gui.ComponentMapDialog;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class Download extends DownloadBase implements Runnable, WindowListener {

  private boolean StopRequested = false;

  private boolean DownloadOnly;
  private boolean HdlOnly;
  private char Vendor;
  private boolean UseGui;
  private JProgressBar MyProgress;
  private VendorDownload Downloader;
  private String TopLevelSheet;
  private double TickFrequency;
  private static int BasicSteps = 5;
  private String MapFileName;
  ArrayList<String> Entities = new ArrayList<String>();
  ArrayList<String> Architectures = new ArrayList<String>();

  private Process Executable;
  private Object lock = new Object();
  private JFrame parent;

  
  private ArrayList<ActionListener> Listeners = new ArrayList<ActionListener>();

  public Download(
	      Project MyProject,
	      String TopLevelSheet,
	      double TickFrequency,
	      FPGAReport MyReporter,
	      BoardInformation MyBoardInformation,
	      String MapFileName,
	      boolean writeToFlash,
	      boolean DownloadOnly,
	      boolean gegerateHdlOnly,
	      JProgressBar Progress,
	      JFrame myParent) {
    MyProgress = Progress;
    parent = myParent;
    SetUpDownload(MyProject, TopLevelSheet, TickFrequency, MyReporter,
       MyBoardInformation, MapFileName, writeToFlash, DownloadOnly,gegerateHdlOnly);  
  }

  public Download(
      Project MyProject,
      String TopLevelSheet,
      double TickFrequency,
      FPGAReport MyReporter,
      BoardInformation MyBoardInformation,
      String MapFileName,
      boolean writeToFlash,
      boolean DownloadOnly,
      boolean gegerateHdlOnly) {
    SetUpDownload(MyProject, TopLevelSheet, TickFrequency, MyReporter,
        MyBoardInformation, MapFileName, writeToFlash, DownloadOnly,gegerateHdlOnly);  
  }
  
  private void SetUpDownload(
      Project MyProject,
      String TopLevelSheet,
      double TickFrequency,
      FPGAReport MyReporter,
      BoardInformation MyBoardInformation,
      String MapFileName,
      boolean writeToFlash,
      boolean DownloadOnly,
      boolean gegerateHdlOnly) {
    this.MyProject = MyProject;
    this.MyReporter = MyReporter;
    this.MyBoardInformation = MyBoardInformation;
    this.DownloadOnly = DownloadOnly;
    this.Vendor = MyBoardInformation.fpga.getVendor();
    this.UseGui = !Main.headless;
    this.TopLevelSheet = TopLevelSheet;
    this.TickFrequency = TickFrequency;
    this.MapFileName = MapFileName;
    this.HdlOnly = gegerateHdlOnly;
    Circuit RootSheet = MyProject.getLogisimFile().getCircuit(TopLevelSheet);
    int steps = BasicSteps;
    switch (Vendor) {
      case VendorSoftware.VendorAltera:
        Downloader =
            new AlteraDownload(
                GetProjDir(TopLevelSheet),
                MyReporter,
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures,
                AppPreferences.HDL_Type.get(),
                writeToFlash);
        break;
      case VendorSoftware.VendorXilinx:
        Downloader =
            new XilinxDownload(
                GetProjDir(TopLevelSheet),
                MyReporter,
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures,
                AppPreferences.HDL_Type.get(),
                writeToFlash);
        break;
      case VendorSoftware.VendorVivado:
        Downloader =
            new VivadoDownload(
                GetProjDir(TopLevelSheet),
                MyReporter,
                RootSheet.getNetList(),
                MyBoardInformation,
                Entities,
                Architectures);
        break;
      default:
        MyReporter.AddFatalError("BUG: Tried to Download to an unknown target");
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
    if (Listeners.contains(listener)) Listeners.remove(Listeners.indexOf(listener));
  }

  private void fireEvent(ActionEvent e) {
    for (int i = 0; i < Listeners.size(); i++) {
      Listeners.get(i).actionPerformed(e);
    }
  }

  @Override
  public void run() {
    if (PrepareDownLoad() && VendorSoftwarePresent() && !HdlOnly) {
      try {
        String error = download();
        if (error != null) MyReporter.AddFatalError(error);
      } catch (IOException e) {
        MyReporter.AddFatalError(S.fmt("FPGAIOError", VendorSoftware.getVendorString(Vendor)));
        e.printStackTrace();
      } catch (InterruptedException e) {
        MyReporter.AddError(S.fmt("FPGAInterruptedError", VendorSoftware.getVendorString(Vendor)));
      }
    }
    fireEvent(new ActionEvent(this, 1, "DownloadDone"));
  }

  public boolean runtty() {
    if (!PrepareDownLoad()) return false;
    if (HdlOnly) return true;
    if (!VendorSoftwarePresent()) return false;
    try {
      String error = download();
      if (error != null) {
        MyReporter.AddFatalError(error);
        return false;
      }
    } catch (IOException e) {
      MyReporter.AddFatalError(S.fmt("FPGAIOError", VendorSoftware.getVendorString(Vendor)));
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      MyReporter.AddError(S.fmt("FPGAInterruptedError", VendorSoftware.getVendorString(Vendor)));
      return false;
    }
    return true;
  }

  private String download() throws IOException, InterruptedException {
    MyReporter.ClsScr();
    if (!DownloadOnly || !Downloader.readyForDownload()) {
      for (int stages = 0; stages < Downloader.GetNumberOfStages(); stages++) {
        if (StopRequested) return S.get("FPGAInterrupted");
        ProcessBuilder CurrentStage = Downloader.PerformStep(stages);
        if (CurrentStage != null) {
          String result = execute(Downloader.GetStageMessage(stages), CurrentStage);
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
    ProcessBuilder DownloadBitfile = Downloader.DownloadToBoard();
    if (DownloadBitfile != null) return execute(S.get("FPGADownloadBitfile"), DownloadBitfile);
    else return null;
  }

  public static String execute(
      ProcessBuilder process, ArrayList<String> Report, FPGAReport MyReporter)
      throws IOException, InterruptedException {
    Process Executable = process.start();
    InputStream is = Executable.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;
    while ((line = br.readLine()) != null) {
      if (MyReporter != null) MyReporter.print(line);
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
    MyReporter.print(" ");
    MyReporter.print("==>");
    MyReporter.print("==> " + StageName);
    MyReporter.print("==>");
    synchronized (lock) {
      Executable = process.start();
    }
    InputStream is = Executable.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;
    while ((line = br.readLine()) != null) {
      MyReporter.print(line);
    }
    Executable.waitFor();
    isr.close();
    br.close();
    if (Executable.exitValue() != 0) {
      return S.fmt("FPGAExecutionFailure", StageName);
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
    String Name = MyProject.getLogisimFile().getName();
    if (Name.contains(" ")) {
      MyReporter.AddFatalError(S.fmt("FPGANameContainsSpaces", Name));
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
        MapPannel = new ComponentMapDialog( parent, 
                MyProject.getLogisimFile().getLoader().getMainFile().getAbsolutePath(),
                MyBoardInformation, MyMappableResources);
      } else {
        MapPannel = new ComponentMapDialog( parent , "", MyBoardInformation, MyMappableResources);
      }
      if (!MapPannel.run()) {
        MyReporter.AddError(S.get("FPGADownloadAborted"));
        return false;
      }
    } else {
      if (MapFileName != null) {
        File MapFile = new File(MapFileName);
        if (!MapFile.exists()) return false;
        ComponentMapParser cmp = new ComponentMapParser(MapFile, MyMappableResources, MyBoardInformation);
        cmp.parseFile();
      }
    }
    if (!MapDesignCheckIOs()) {
      MyReporter.AddError(S.fmt("FPGAMapNotComplete", MyBoardInformation.getBoardName()));
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
    String ProjectPath = GetProjDir(TopLevelSheet);
    String SourcePath = ProjectPath + AppPreferences.HDL_Type.get().toLowerCase() + File.separator;
    GetVHDLFiles(ProjectPath, SourcePath, Entities, Architectures, AppPreferences.HDL_Type.get());
    if (UseGui) {
      MyProgress.setValue(4);
      MyProgress.setString(S.get("FPGAState4"));
    }
    Downloader.SetMapableResources(MyMappableResources);
    /* Stage 4 Create Download Scripts */
    return CreateDownloadScripts();
  }

  @Override
  public void windowOpened(WindowEvent e) {}

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

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowActivated(WindowEvent e) {}

  @Override
  public void windowDeactivated(WindowEvent e) {}

  public static String GetClockFrequencyString(BoardInformation CurrentBoard) {
    long clkfreq = CurrentBoard.fpga.getClockFrequency();
    if (clkfreq % 1000000 == 0) {
      clkfreq /= 1000000;
      return Long.toString(clkfreq) + " MHz ";
    } else if (clkfreq % 1000 == 0) {
      clkfreq /= 1000;
      return Long.toString(clkfreq) + " kHz ";
    }
    return Long.toString(clkfreq);
  }

  public static String ChooseBoard(List<String> devices) {
    /* This code is based on the version of Kevin Walsh */
    if (Main.hasGui()) {
      String[] choices = new String[devices.size()];
      for (int i = 0; i < devices.size(); i++) choices[i] = devices.get(i);
      String choice =
          (String)
              OptionPane.showInputDialog(
                  null,
                  S.fmt("FPGAMultipleBoards", devices.size()),
                  S.get("FPGABoardSelection"),
                  OptionPane.QUESTION_MESSAGE,
                  null,
                  choices,
                  choices[0]);
      return choice;
    } else {
      /* TODO: add none gui selection */
      return null;
    }
  }

}
