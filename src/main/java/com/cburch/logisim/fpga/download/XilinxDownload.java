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

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XilinxDownload implements VendorDownload {

  private VendorSoftware xilinxVendor = VendorSoftware.getSoftware(VendorSoftware.VendorXilinx);
  private String ScriptPath;
  private String ProjectPath;
  private String SandboxPath;
  private String UcfPath;
  private FPGAReport Reporter;
  private Netlist RootNetList;
  private MappableResourcesContainer MapInfo;
  private BoardInformation BoardInfo;
  private ArrayList<String> Entities;
  private ArrayList<String> Architectures;
  private String HDLType;
  private String BitfileExt;
  private boolean IsCPLD;
  private boolean writeToFlash;

  private static final String vhdl_list_file = "XilinxVHDLList.prj";
  private static final String script_file = "XilinxScript.cmd";
  private static final String ucf_file = "XilinxConstraints.ucf";
  private static final String download_file = "XilinxDownload";
  private static final String mcs_file = "XilinxProm.mcs";

  private static final Integer BUFFER_SIZE = 16 * 1024;

  public XilinxDownload(
      String ProjectPath,
      FPGAReport Reporter,
      Netlist RootNetList,
      BoardInformation BoardInfo,
      ArrayList<String> Entities,
      ArrayList<String> Architectures,
      String HDLType,
      boolean WriteToFlash) {
    this.ProjectPath = ProjectPath;
    this.SandboxPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.SandboxPath);
    this.ScriptPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.ScriptPath);
    this.UcfPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.UCFPath);
    this.Reporter = Reporter;
    this.RootNetList = RootNetList;
    this.BoardInfo = BoardInfo;
    this.Entities = Entities;
    this.Architectures = Architectures;
    this.HDLType = HDLType;
    this.writeToFlash = WriteToFlash;
    IsCPLD =
        BoardInfo.fpga.getPart().toUpperCase().startsWith("XC2C")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XA2C")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XCR3")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XC9500")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XA9500");
    BitfileExt = (IsCPLD) ? "jed" : "bit";
  }

  @Override
  public int GetNumberOfStages() {
    return 5;
  }

  @Override
  public String GetStageMessage(int stage) {
    switch (stage) {
      case 0:
        return S.get("XilinxSynth");
      case 1:
        return S.get("XilinxContraints");
      case 2:
        return S.get("XilinxMap");
      case 3:
        return S.get("XilinxPAR");
      case 4:
        return S.get("XilinxBit");
      default:
        return "unknown";
    }
  }

  @Override
  public ProcessBuilder PerformStep(int stage) {
    switch (stage) {
      case 0:
        return Stage0Synth();
      case 1:
        return Stage1Constraints();
      case 2:
        return Stage2Map();
      case 3:
        return Stage3PAR();
      case 4:
        return Stage4Bit();
      default:
        return null;
    }
  }

  @Override
  public boolean readyForDownload() {
    return new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + "." + BitfileExt)
        .exists();
  }

  @Override
  public ProcessBuilder DownloadToBoard() {
    if (!BoardInfo.fpga.USBTMCDownloadRequired()) {
      List<String> command = new ArrayList<String>();
      command.add(xilinxVendor.getBinaryPath(5));
      command.add("-batch");
      command.add(ScriptPath.replace(ProjectPath, "../") + File.separator + download_file);
      ProcessBuilder Xilinx = new ProcessBuilder(command);
      Xilinx.directory(new File(SandboxPath));
      return Xilinx;
    } else {
      Reporter.ClsScr();
      /* Here we do the USBTMC Download */
      boolean usbtmcdevice = new File("/dev/usbtmc0").exists();
      if (!usbtmcdevice) {
        Reporter.AddFatalError(S.get("XilinxUsbTmc"));
        return null;
      }
      File bitfile =
          new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + "." + BitfileExt);
      byte[] bitfile_buffer = new byte[BUFFER_SIZE];
      int bitfile_buffer_size;
      BufferedInputStream bitfile_in;
      try {
        bitfile_in = new BufferedInputStream(new FileInputStream(bitfile));
      } catch (FileNotFoundException e) {
        Reporter.AddFatalError(S.fmt("XilinxOpenFailure", bitfile));
        return null;
      }
      File usbtmc = new File("/dev/usbtmc0");
      BufferedOutputStream usbtmc_out;
      try {
        usbtmc_out = new BufferedOutputStream(new FileOutputStream(usbtmc));
        usbtmc_out.write("FPGA ".getBytes());
        bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0, BUFFER_SIZE);
        while (bitfile_buffer_size > 0) {
          usbtmc_out.write(bitfile_buffer, 0, bitfile_buffer_size);
          bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0, BUFFER_SIZE);
        }
        usbtmc_out.close();
        bitfile_in.close();
      } catch (IOException e) {
        Reporter.AddFatalError(S.get("XilinxUsbTmcError"));
      }
    }
    return null;
  }

  @Override
  public boolean CreateDownloadScripts() {
    String JTAGPos = String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition());
    File ScriptFile = FileWriter.GetFilePointer(ScriptPath, script_file, Reporter);
    File VhdlListFile = FileWriter.GetFilePointer(ScriptPath, vhdl_list_file, Reporter);
    File UcfFile = FileWriter.GetFilePointer(UcfPath, ucf_file, Reporter);
    File DownloadFile = FileWriter.GetFilePointer(ScriptPath, download_file, Reporter);
    if (ScriptFile == null || VhdlListFile == null || UcfFile == null || DownloadFile == null) {
      ScriptFile = new File(ScriptPath + script_file);
      VhdlListFile = new File(ScriptPath + vhdl_list_file);
      UcfFile = new File(UcfPath + ucf_file);
      DownloadFile = new File(ScriptPath + download_file);
      return ScriptFile.exists()
          && VhdlListFile.exists()
          && UcfFile.exists()
          && DownloadFile.exists();
    }
    ArrayList<String> Contents = new ArrayList<String>();
    for (int i = 0; i < Entities.size(); i++) {
      Contents.add(HDLType.toUpperCase() + " work \"" + Entities.get(i) + "\"");
    }
    for (int i = 0; i < Architectures.size(); i++) {
      Contents.add(HDLType.toUpperCase() + " work \"" + Architectures.get(i) + "\"");
    }
    if (!FileWriter.WriteContents(VhdlListFile, Contents, Reporter)) return false;
    Contents.clear();
    Contents.add(
        "run -top "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + " -ofn logisim.ngc -ofmt NGC -ifn "
            + ScriptPath.replace(ProjectPath, "../")
            + vhdl_list_file
            + " -ifmt mixed -p "
            + GetFPGADeviceString(BoardInfo));
    if (!FileWriter.WriteContents(ScriptFile, Contents, Reporter)) return false;
    Contents.clear();
    Contents.add("setmode -bscan");
    if (writeToFlash && BoardInfo.fpga.isFlashDefined()) {
      if (BoardInfo.fpga.getFlashName() == null) {
        Reporter.AddFatalError(S.fmt("XilinxFlashMissing", BoardInfo.getBoardName()));
      }
      String FlashPos = String.valueOf(BoardInfo.fpga.getFlashJTAGChainPosition());
      String McsFile = ScriptPath + File.separator + mcs_file;
      Contents.add("setmode -pff");
      Contents.add("setSubMode -pffserial");
      Contents.add(
          "addPromDevice -p " + JTAGPos + " -size 0 -name " + BoardInfo.fpga.getFlashName());
      Contents.add("addDesign -version 0 -name \"0\"");
      Contents.add("addDeviceChain -index 0");
      Contents.add(
          "addDevice -p "
              + JTAGPos
              + " -file "
              + ToplevelHDLGeneratorFactory.FPGAToplevelName
              + "."
              + BitfileExt);
      Contents.add("generate -format mcs -fillvalue FF -output " + McsFile);
      Contents.add("setMode -bs");
      Contents.add("setCable -port auto");
      Contents.add("identify");
      Contents.add("assignFile -p " + FlashPos + " -file " + McsFile);
      Contents.add("program -p " + FlashPos + " -e -v");
    } else {
      Contents.add("setcable -p auto");
      Contents.add("identify");
      if (!IsCPLD) {
        Contents.add(
            "assignFile -p "
                + JTAGPos
                + " -file "
                + ToplevelHDLGeneratorFactory.FPGAToplevelName
                + "."
                + BitfileExt);
        Contents.add("program -p " + JTAGPos + " -onlyFpga");
      } else {
        Contents.add("assignFile -p " + JTAGPos + " -file logisim." + BitfileExt);
        Contents.add("program -p " + JTAGPos + " -e");
      }
    }
    Contents.add("quit");
    if (!FileWriter.WriteContents(DownloadFile, Contents, Reporter)) return false;
    Contents.clear();
    if (RootNetList.NumberOfClockTrees() > 0) {
      Contents.add(
          "NET \""
              + TickComponentHDLGeneratorFactory.FPGAClock
              + "\" "
              + GetXilinxClockPin(BoardInfo)
              + " ;");
      Contents.add(
          "NET \""
              + TickComponentHDLGeneratorFactory.FPGAClock
              + "\" TNM_NET = \""
              + TickComponentHDLGeneratorFactory.FPGAClock
              + "\" ;");
      Contents.add(
          "TIMESPEC \"TS_"
              + TickComponentHDLGeneratorFactory.FPGAClock
              + "\" = PERIOD \""
              + TickComponentHDLGeneratorFactory.FPGAClock
              + "\" "
              + Download.GetClockFrequencyString(BoardInfo)
              + " HIGH 50 % ;");
      Contents.add("");
    }
    Contents.addAll(GetPinLocStrings());
    return FileWriter.WriteContents(UcfFile, Contents, Reporter);
  }
  
  private ArrayList<String> GetPinLocStrings() {
    ArrayList<String> Contents = new ArrayList<String>();
    StringBuffer Temp = new StringBuffer();
    for (ArrayList<String> key : MapInfo.getMappableResources().keySet()) {
      MapComponent map = MapInfo.getMappableResources().get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i)) {
          Temp.setLength(0);
          Temp.append("NET \"");
          if (map.isExternalInverted(i)) Temp.append("n_");
          Temp.append(map.getHdlString(i)+"\" ");
          Temp.append("LOC = \""+map.getPinLocation(i)+"\" ");
          FPGAIOInformationContainer info = map.getFpgaInfo(i);
          if (info != null) {
            if (info.GetPullBehavior() != PullBehaviors.Unknown && info.GetPullBehavior() != PullBehaviors.Float) {
              Temp.append("| " + PullBehaviors.getContraintedPullString(info.GetPullBehavior()) + " ");
            }
            if (info.GetDrive() != DriveStrength.Unknown
                && info.GetDrive() != DriveStrength.DefaulStength) {
              Temp.append("| DRIVE = " + DriveStrength.GetContraintedDriveStrength(info.GetDrive()) + " ");
            }
            if (info.GetIOStandard() != IoStandards.Unknown && info.GetIOStandard() != IoStandards.DefaulStandard) {
              Temp.append("| IOSTANDARD = " + IoStandards.GetConstraintedIoStandard(info.GetIOStandard()) + " ");
            }
          }
          Temp.append(";");
          Contents.add(Temp.toString());
        }
      }
    }
    return Contents;
  }

  @Override
  public void SetMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
  }

  private ProcessBuilder Stage0Synth() {
    List<String> command = new ArrayList<String>();
    command.add(xilinxVendor.getBinaryPath(0));
    command.add("-ifn");
    command.add(ScriptPath.replace(ProjectPath, "../") + File.separator + script_file);
    command.add("-ofn");
    command.add("logisim.log");
    ProcessBuilder stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  private ProcessBuilder Stage1Constraints() {
    List<String> command = new ArrayList<String>();
    command.add(xilinxVendor.getBinaryPath(1));
    command.add("-intstyle");
    command.add("ise");
    command.add("-uc");
    command.add(UcfPath.replace(ProjectPath, "../") + File.separator + ucf_file);
    command.add("logisim.ngc");
    command.add("logisim.ngd");
    ProcessBuilder stage1 = new ProcessBuilder(command);
    stage1.directory(new File(SandboxPath));
    return stage1;
  }

  private ProcessBuilder Stage2Map() {
    if (IsCPLD) return null; /* mapping is skipped for the CPLD target*/
    List<String> command = new ArrayList<String>();
    command.add(xilinxVendor.getBinaryPath(2));
    command.add("-intstyle");
    command.add("ise");
    command.add("-o");
    command.add("logisim_map");
    command.add("logisim.ngd");
    ProcessBuilder stage2 = new ProcessBuilder(command);
    stage2.directory(new File(SandboxPath));
    return stage2;
  }

  private ProcessBuilder Stage3PAR() {
    List<String> command = new ArrayList<String>();
    if (!IsCPLD) {
      command.add(xilinxVendor.getBinaryPath(3));
      command.add("-w");
      command.add("-intstyle");
      command.add("ise");
      command.add("-ol");
      command.add("high");
      command.add("logisim_map");
      command.add("logisim_par");
      command.add("logisim_map.pcf");
    } else {
      command.add(xilinxVendor.getBinaryPath(6));
      command.add("-p");
      command.add(
          BoardInfo.fpga.getPart().toUpperCase()
              + "-"
              + BoardInfo.fpga.getSpeedGrade()
              + "-"
              + BoardInfo.fpga.getPackage().toUpperCase());
      command.add("-intstyle");
      command.add("ise");
      /* TODO: do correct termination type */
      command.add("-terminate");
      if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
        command.add("pullup");
      } else if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
        command.add("pulldown");
      } else {
        command.add("float");
      }
      command.add("-loc");
      command.add("on");
      command.add("-log");
      command.add("logisim_cpldfit.log");
      command.add("logisim.ngd");
    }
    ProcessBuilder stage3 = new ProcessBuilder(command);
    stage3.directory(new File(SandboxPath));
    return stage3;
  }

  private ProcessBuilder Stage4Bit() {
    List<String> command = new ArrayList<String>();
    if (!IsCPLD) {
      command.add(xilinxVendor.getBinaryPath(4));
      command.add("-w");
      if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
        command.add("-g");
        command.add("UnusedPin:PULLUP");
      }
      if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
        command.add("-g");
        command.add("UnusedPin:PULLDOWN");
      }
      command.add("-g");
      command.add("StartupClk:CCLK");
      command.add("logisim_par");
      command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName + ".bit");
    } else {
      command.add(xilinxVendor.getBinaryPath(7));
      command.add("-i");
      command.add("logisim.vm6");
    }
    ProcessBuilder stage4 = new ProcessBuilder(command);
    stage4.directory(new File(SandboxPath));
    return stage4;
  }

  private static String GetFPGADeviceString(BoardInformation CurrentBoard) {
    StringBuffer result = new StringBuffer();
    result.append(CurrentBoard.fpga.getPart());
    result.append("-");
    result.append(CurrentBoard.fpga.getPackage());
    result.append("-");
    result.append(CurrentBoard.fpga.getSpeedGrade());
    return result.toString();
  }

  private static String GetXilinxClockPin(BoardInformation CurrentBoard) {
    StringBuffer result = new StringBuffer();
    result.append("LOC = \"" + CurrentBoard.fpga.getClockPinLocation() + "\"");
    if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PullUp) {
      result.append(" | PULLUP");
    }
    if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PullDown) {
      result.append(" | PULLDOWN");
    }
    if (CurrentBoard.fpga.getClockStandard() != IoStandards.DefaulStandard
        && CurrentBoard.fpga.getClockStandard() != IoStandards.Unknown) {
      result.append(
          " | IOSTANDARD = " + IoStandards.Behavior_strings[CurrentBoard.fpga.getClockStandard()]);
    }
    return result.toString();
  }

  @Override
  public boolean BoardConnected() {
    // TODO Detect if a board is connected, and in case of multiple boards select the one that
    // should be used
    return true;
  }
  
}
