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

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.LineBuffer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class XilinxDownload implements VendorDownload {

  private final VendorSoftware xilinxVendor =
      VendorSoftware.getSoftware(VendorSoftware.VENDOR_XILINX);
  private final String ScriptPath;
  private final String ProjectPath;
  private final String SandboxPath;
  private final String UcfPath;
  private final Netlist RootNetList;
  private MappableResourcesContainer MapInfo;
  private final BoardInformation boardInfo;
  private final ArrayList<String> Entities;
  private final ArrayList<String> architectures;
  private final String HDLType;
  private final String bitfileExt;
  private final boolean IsCPLD;
  private final boolean writeToFlash;

  private static final String VHDL_LIST_FILE = "XilinxVHDLList.prj";
  private static final String SCRIPT_FILE = "XilinxScript.cmd";
  private static final String UCF_FILE = "XilinxConstraints.ucf";
  private static final String DOWNLOAD_FILE = "XilinxDownload";
  private static final String MCS_FILE = "XilinxProm.mcs";

  private static final Integer BUFFER_SIZE = 16 * 1024;

  public XilinxDownload(
      String ProjectPath,
      Netlist RootNetList,
      BoardInformation BoardInfo,
      ArrayList<String> Entities,
      ArrayList<String> Architectures,
      String HDLType,
      boolean WriteToFlash) {
    this.ProjectPath = ProjectPath;
    this.SandboxPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.SANDBOX_PATH);
    this.ScriptPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.SCRIPT_PATH);
    this.UcfPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.UCF_PATH);
    this.RootNetList = RootNetList;
    this.boardInfo = BoardInfo;
    this.Entities = Entities;
    this.architectures = Architectures;
    this.HDLType = HDLType;
    this.writeToFlash = WriteToFlash;
    IsCPLD =
        BoardInfo.fpga.getPart().toUpperCase().startsWith("XC2C")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XA2C")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XCR3")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XC9500")
            || BoardInfo.fpga.getPart().toUpperCase().startsWith("XA9500");
    bitfileExt = (IsCPLD) ? "jed" : "bit";
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
    return new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + "." + bitfileExt).exists();
  }

  @Override
  public ProcessBuilder DownloadToBoard() {
    if (!boardInfo.fpga.USBTMCDownloadRequired()) {
      var command = new ArrayList<String>();
      command.add(xilinxVendor.getBinaryPath(5));
      command.add("-batch");
      command.add(ScriptPath.replace(ProjectPath, "../") + File.separator + DOWNLOAD_FILE);
      final var Xilinx = new ProcessBuilder(command);
      Xilinx.directory(new File(SandboxPath));
      return Xilinx;
    } else {
      Reporter.Report.ClsScr();
      /* Here we do the USBTMC Download */
      var usbtmcdevice = new File("/dev/usbtmc0").exists();
      if (!usbtmcdevice) {
        Reporter.Report.AddFatalError(S.get("XilinxUsbTmc"));
        return null;
      }
      var bitfile = new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + "." + bitfileExt);
      var bitfile_buffer = new byte[BUFFER_SIZE];
      var bitfile_buffer_size = 0;
      BufferedInputStream bitfile_in;
      try {
        bitfile_in = new BufferedInputStream(new FileInputStream(bitfile));
      } catch (FileNotFoundException e) {
        Reporter.Report.AddFatalError(S.get("XilinxOpenFailure", bitfile));
        return null;
      }
      var usbtmc = new File("/dev/usbtmc0");
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
        Reporter.Report.AddFatalError(S.get("XilinxUsbTmcError"));
      }
    }
    return null;
  }

  @Override
  public boolean CreateDownloadScripts() {
    final var JTAGPos = String.valueOf(boardInfo.fpga.getFpgaJTAGChainPosition());
    var ScriptFile = FileWriter.GetFilePointer(ScriptPath, SCRIPT_FILE);
    var VhdlListFile = FileWriter.GetFilePointer(ScriptPath, VHDL_LIST_FILE);
    var UcfFile = FileWriter.GetFilePointer(UcfPath, UCF_FILE);
    var DownloadFile = FileWriter.GetFilePointer(ScriptPath, DOWNLOAD_FILE);
    if (ScriptFile == null || VhdlListFile == null || UcfFile == null || DownloadFile == null) {
      ScriptFile = new File(ScriptPath + SCRIPT_FILE);
      VhdlListFile = new File(ScriptPath + VHDL_LIST_FILE);
      UcfFile = new File(UcfPath + UCF_FILE);
      DownloadFile = new File(ScriptPath + DOWNLOAD_FILE);
      return ScriptFile.exists()
          && VhdlListFile.exists()
          && UcfFile.exists()
          && DownloadFile.exists();
    }
    var contents = new LineBuffer();
    for (var entity : Entities) contents.add("{{1}} work \"{{2}}\"", HDLType.toUpperCase(), entity);
    for (var arch : architectures) contents.add("{{1}} work \"{{2}}\"", HDLType.toUpperCase(), arch);
    if (!FileWriter.WriteContents(VhdlListFile, contents.get())) return false;

    contents.clear();
    contents.add(
        "run -top {{1}} -ofn logisim.ngc -ofmt NGC -ifn {{2}}{{3}} -ifmt mixed -p {{4}}",
        ToplevelHDLGeneratorFactory.FPGAToplevelName,
        ScriptPath.replace(ProjectPath, "../"),
        VHDL_LIST_FILE,
        GetFPGADeviceString(boardInfo));

    if (!FileWriter.WriteContents(ScriptFile, contents.get())) return false;

    contents.clear();
    contents.add("setmode -bscan");
    if (writeToFlash && boardInfo.fpga.isFlashDefined()) {
      if (boardInfo.fpga.getFlashName() == null) {
        Reporter.Report.AddFatalError(S.get("XilinxFlashMissing", boardInfo.getBoardName()));
      }
      final var flashPos = String.valueOf(boardInfo.fpga.getFlashJTAGChainPosition());
      final var mcsFile = ScriptPath + File.separator + MCS_FILE;
      contents
          .add("setmode -pff")
          .add("setSubMode -pffserial")
          .add("addPromDevice -p {{1}} -size 0 -name {{2}}", JTAGPos, boardInfo.fpga.getFlashName())
          .add("addDesign -version 0 -name \"0\"")
          .add("addDeviceChain -index 0")
          .add("addDevice -p {{1}} -file {{2}}.{{3}}", JTAGPos, ToplevelHDLGeneratorFactory.FPGAToplevelName, bitfileExt)
          .add("generate -format mcs -fillvalue FF -output {{1}}", mcsFile)
          .add("setMode -bs")
          .add("setCable -port auto")
          .add("identify")
          .add("assignFile -p {{1}} -file {{2}}", flashPos, mcsFile)
          .add("program -p {{1}} -e -v", flashPos);
    } else {
      contents.add("setcable -p auto").add("identify");
      if (!IsCPLD) {
        contents
            .add("assignFile -p {{1}} -file {{2}}.{{3}}", JTAGPos, ToplevelHDLGeneratorFactory.FPGAToplevelName, bitfileExt)
            .add("program -p {{1}} -onlyFpga", JTAGPos);
      } else {
        contents
            .add("assignFile -p {{1}} -file logisim.{{2}}", JTAGPos, bitfileExt)
            .add("program -p {{1}} -e", JTAGPos);
      }
    }
    contents.add("quit");
    if (!FileWriter.WriteContents(DownloadFile, contents.get())) return false;

    contents.clear();
    if (RootNetList.numberOfClockTrees() > 0 || RootNetList.requiresGlobalClockConnection()) {
      contents
          .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK)
          .pair("clockFreq", Download.GetClockFrequencyString(boardInfo))
          .pair("clockPin", GetXilinxClockPin(boardInfo))
          .addLines(
            "NET \"{{clock}}\" {{clockPin}} ;",
            "NET \"{{clock}}\" TNM_NET = \"{{clock}}\" ;",
            "TIMESPEC \"TS_{{clock}}\" = PERIOD \"{{clock}}\" {{clockFreq}} HIGH 50 % ;",
            "");
    }
    contents.add(getPinLocStrings());
    return FileWriter.WriteContents(UcfFile, contents.get());
  }

  private ArrayList<String> getPinLocStrings() {
    var Contents = new ArrayList<String>();
    var Temp = new StringBuilder();
    for (var key : MapInfo.getMappableResources().keySet()) {
      var map = MapInfo.getMappableResources().get(key);
      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i) && !map.isInternalMapped(i)) {
          Temp.setLength(0);
          Temp.append("NET \"");
          if (map.isExternalInverted(i)) Temp.append("n_");
          Temp.append(map.getHdlString(i)).append("\" ");
          Temp.append("LOC = \"").append(map.getPinLocation(i)).append("\" ");
          final var info = map.getFpgaInfo(i);
          if (info != null) {
            if (info.GetPullBehavior() != PullBehaviors.UNKNOWN
                && info.GetPullBehavior() != PullBehaviors.FLOAT) {
              Temp.append("| ")
                  .append(PullBehaviors.getContraintedPullString(info.GetPullBehavior()))
                  .append(" ");
            }
            if (info.GetDrive() != DriveStrength.UNKNOWN
                && info.GetDrive() != DriveStrength.DEFAULT_STENGTH) {
              Temp.append("| DRIVE = ")
                  .append(DriveStrength.GetContraintedDriveStrength(info.GetDrive())).append(" ");
            }
            if (info.GetIOStandard() != IoStandards.UNKNOWN
                && info.GetIOStandard() != IoStandards.DEFAULT_STANDARD) {
              Temp.append("| IOSTANDARD = ")
                  .append(IoStandards.GetConstraintedIoStandard(info.GetIOStandard()))
                  .append(" ");
            }
          }
          Temp.append(";");
          Contents.add(Temp.toString());
        }
      }
    }
    final var LedArrayMap = DownloadBase.getLedArrayMaps(MapInfo, RootNetList, boardInfo);
    for (var key : LedArrayMap.keySet()) {
      Contents.add("NET \"" + LedArrayMap.get(key) + "\" LOC=\"" + key + "\";");
    }
    return Contents;
  }

  @Override
  public void SetMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
  }

  private ProcessBuilder Stage0Synth() {
    final var command = new LineBuffer();
    command
        .add(xilinxVendor.getBinaryPath(0))
        .add("-ifn")
        .add(ScriptPath.replace(ProjectPath, "../") + File.separator + SCRIPT_FILE)
        .add("-ofn")
        .add("logisim.log");
    final var stage0 = new ProcessBuilder(command.get());
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  private ProcessBuilder Stage1Constraints() {
    final var command = new LineBuffer();
    command
        .add(xilinxVendor.getBinaryPath(1))
        .add("-intstyle")
        .add("ise")
        .add("-uc")
        .add(UcfPath.replace(ProjectPath, "../") + File.separator + UCF_FILE)
        .add("logisim.ngc")
        .add("logisim.ngd");
    final var stage1 = new ProcessBuilder(command.get());
    stage1.directory(new File(SandboxPath));
    return stage1;
  }

  private ProcessBuilder Stage2Map() {
    if (IsCPLD) return null; /* mapping is skipped for the CPLD target*/
    final var command = new LineBuffer();
    command
        .add(xilinxVendor.getBinaryPath(2))
        .add("-intstyle")
        .add("ise")
        .add("-o")
        .add("logisim_map")
        .add("logisim.ngd");
    final var stage2 = new ProcessBuilder(command.get());
    stage2.directory(new File(SandboxPath));
    return stage2;
  }

  private ProcessBuilder Stage3PAR() {
    final var command = new LineBuffer();
    if (!IsCPLD) {
      command
          .add(xilinxVendor.getBinaryPath(3))
          .add("-w")
          .add("-intstyle")
          .add("ise")
          .add("-ol")
          .add("high")
          .add("logisim_map")
          .add("logisim_par")
          .add("logisim_map.pcf");
    } else {
      final var pinPullBehavior = switch (boardInfo.fpga.getUnusedPinsBehavior()) {
        case PullBehaviors.PULL_UP -> "pullup";
        case PullBehaviors.PULL_DOWN -> "pulldown";
        default -> "float";
      };
      final var fpga = boardInfo.fpga;
      command
          .add(xilinxVendor.getBinaryPath(6))
          .add("-p")
          .add("{{1}}-{{2}}-{{3}}", fpga.getPart().toUpperCase(), fpga.getSpeedGrade(), fpga.getPackage().toUpperCase())
          .add("-intstyle")
          .add("ise")
          /* TODO: do correct termination type */
          .add("-terminate")
          .add(pinPullBehavior)
          .add("-loc")
          .add("on")
          .add("-log")
          .add("logisim_cpldfit.log")
          .add("logisim.ngd");
    }
    final var stage3 = new ProcessBuilder(command.get());
    stage3.directory(new File(SandboxPath));
    return stage3;
  }

  private ProcessBuilder Stage4Bit() {
    var command = new LineBuffer();
    if (!IsCPLD) {
      command.add(xilinxVendor.getBinaryPath(4)).add("-w");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_UP) command.add("-g").add("UnusedPin:PULLUP");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_DOWN) command.add("-g").add("UnusedPin:PULLDOWN");
      command.add("-g").add("StartupClk:CCLK").add("logisim_par").add("{{1}}.bit", ToplevelHDLGeneratorFactory.FPGAToplevelName);
    } else {
      command.add(xilinxVendor.getBinaryPath(7)).add("-i").add("logisim.vm6");
    }
    final var stage4 = new ProcessBuilder(command.get());
    stage4.directory(new File(SandboxPath));
    return stage4;
  }

  private static String GetFPGADeviceString(BoardInformation CurrentBoard) {
    return CurrentBoard.fpga.getPart()
        + "-"
        + CurrentBoard.fpga.getPackage()
        + "-"
        + CurrentBoard.fpga.getSpeedGrade();
  }

  private static String GetXilinxClockPin(BoardInformation CurrentBoard) {
    var result = new StringBuilder();
    result.append("LOC = \"").append(CurrentBoard.fpga.getClockPinLocation()).append("\"");
    if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PULL_UP) {
      result.append(" | PULLUP");
    }
    if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PULL_DOWN) {
      result.append(" | PULLDOWN");
    }
    if (CurrentBoard.fpga.getClockStandard() != IoStandards.DEFAULT_STANDARD
        && CurrentBoard.fpga.getClockStandard() != IoStandards.UNKNOWN) {
      result.append(" | IOSTANDARD = ")
          .append(IoStandards.Behavior_strings[CurrentBoard.fpga.getClockStandard()]);
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
