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

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHdlGeneratorFactory;
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
  public int getNumberOfStages() {
    return 5;
  }

  @Override
  public String getStageMessage(int stage) {
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
  public ProcessBuilder performStep(int stage) {
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
    return new File(SandboxPath + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME + "." + bitfileExt).exists();
  }

  @Override
  public ProcessBuilder downloadToBoard() {
    if (!boardInfo.fpga.isUsbTmcDownloadRequired()) {
      var command = new ArrayList<String>();
      command.add(xilinxVendor.getBinaryPath(5));
      command.add("-batch");
      command.add(ScriptPath.replace(ProjectPath, "../") + File.separator + DOWNLOAD_FILE);
      final var Xilinx = new ProcessBuilder(command);
      Xilinx.directory(new File(SandboxPath));
      return Xilinx;
    } else {
      Reporter.report.clearConsole();
      /* Here we do the USBTMC Download */
      var usbtmcdevice = new File("/dev/usbtmc0").exists();
      if (!usbtmcdevice) {
        Reporter.report.addFatalError(S.get("XilinxUsbTmc"));
        return null;
      }
      var bitfile = new File(SandboxPath + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME + "." + bitfileExt);
      var bitfile_buffer = new byte[BUFFER_SIZE];
      var bitfile_buffer_size = 0;
      BufferedInputStream bitfile_in;
      try {
        bitfile_in = new BufferedInputStream(new FileInputStream(bitfile));
      } catch (FileNotFoundException e) {
        Reporter.report.addFatalError(S.get("XilinxOpenFailure", bitfile));
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
        Reporter.report.addFatalError(S.get("XilinxUsbTmcError"));
      }
    }
    return null;
  }

  @Override
  public boolean createDownloadScripts() {
    final var JTAGPos = String.valueOf(boardInfo.fpga.getFpgaJTAGChainPosition());
    var ScriptFile = FileWriter.getFilePointer(ScriptPath, SCRIPT_FILE);
    var VhdlListFile = FileWriter.getFilePointer(ScriptPath, VHDL_LIST_FILE);
    var UcfFile = FileWriter.getFilePointer(UcfPath, UCF_FILE);
    var DownloadFile = FileWriter.getFilePointer(ScriptPath, DOWNLOAD_FILE);
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
    final var contents = LineBuffer.getBuffer()
            .pair("JTAGPos", JTAGPos)
            .pair("fileExt", bitfileExt)
            .pair("fileBaseName", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME)
            .pair("mcsFile", ScriptPath + File.separator + MCS_FILE)
            .pair("hdlType", HDLType.toUpperCase().toUpperCase());

    for (var entity : Entities) contents.add("{{hdlType}} work \"{{1}}\"", entity);
    for (var arch : architectures) contents.add("{{hdlType}} work \"{{1}}\"", arch);
    if (!FileWriter.writeContents(VhdlListFile, contents.get())) return false;

    contents
          .clear()
          .add(
              "run -top {{1}} -ofn logisim.ngc -ofmt NGC -ifn {{2}}{{3}} -ifmt mixed -p {{4}}",
              ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME,
              ScriptPath.replace(ProjectPath, "../"),
              VHDL_LIST_FILE,
              GetFPGADeviceString(boardInfo));

    if (!FileWriter.writeContents(ScriptFile, contents.get())) return false;

    contents.clear();
    contents.add("setmode -bscan");

    if (writeToFlash && boardInfo.fpga.isFlashDefined()) {
      if (boardInfo.fpga.getFlashName() == null) {
        Reporter.report.addFatalError(S.get("XilinxFlashMissing", boardInfo.getBoardName()));
      }

      contents.pair("flashPos", String.valueOf(boardInfo.fpga.getFlashJTAGChainPosition()))
              .pair("flashName", boardInfo.fpga.getFlashName())
              .add("""
                setmode -pff
                setSubMode -pffserial
                addPromDevice -p {{JTAGPos}} -size 0 -name {{flashName}}
                addDesign -version 0 -name "0"
                addDeviceChain -index 0
                addDevice -p {{JTAGPos}} -file {{fileBaseName}}.{{fileExt}}
                generate -format mcs -fillvalue FF -output {{mcsFile}}
                setMode -bs
                setCable -port auto
                identify
                assignFile -p {{flashPos}} -file {{mcsFile}}
                program -p {{flashPos}} -e -v
                """);
    } else {
      contents.add("setcable -p auto").add("identify");
      if (!IsCPLD) {
        contents.add("""
            assignFile -p {{JTAGPos}} -file {{fileBaseName}}.{{fileExt}}
            program -p {{JTAGPos}} -onlyFpga
            """);
      } else {
        contents.add("""
            assignFile -p {{JTAGPos}} -file logisim.{{fileExt}}
            program -p {{JTAGPos}} -e
            """);
      }
    }
    contents.add("quit");
    if (!FileWriter.writeContents(DownloadFile, contents.get())) return false;

    contents.clear();
    if (RootNetList.numberOfClockTrees() > 0 || RootNetList.requiresGlobalClockConnection()) {
      contents
          .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
          .pair("clockFreq", Download.getClockFrequencyString(boardInfo))
          .pair("clockPin", GetXilinxClockPin(boardInfo))
          .add("""
            NET "{{clock}}" {{clockPin}} ;
            NET "{{clock}}" TNM_NET = "{{clock}}" ;
            TIMESPEC "TS_{{clock}}" = PERIOD "{{clock}}" {{clockFreq}} HIGH 50 % ;
            """);
    }
    contents.add(getPinLocStrings());
    return FileWriter.writeContents(UcfFile, contents.get());
  }

  private ArrayList<String> getPinLocStrings() {
    var Contents = new ArrayList<String>();
    var Temp = new StringBuilder();
    for (var key : MapInfo.getMappableResources().keySet()) {
      var map = MapInfo.getMappableResources().get(key);
      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.isOpenMapped(i) && !map.isConstantMapped(i) && !map.isInternalMapped(i)) {
          Temp.setLength(0);
          Temp.append("NET \"");
          if (map.isExternalInverted(i)) Temp.append("n_");
          Temp.append(map.getHdlString(i)).append("\" ");
          Temp.append("LOC = \"").append(map.getPinLocation(i)).append("\" ");
          final var info = map.getFpgaInfo(i);
          if (info != null) {
            if (info.getPullBehavior() != PullBehaviors.UNKNOWN
                && info.getPullBehavior() != PullBehaviors.FLOAT) {
              Temp.append("| ")
                  .append(PullBehaviors.getContraintedPullString(info.getPullBehavior()))
                  .append(" ");
            }
            if (info.getDrive() != DriveStrength.UNKNOWN
                && info.getDrive() != DriveStrength.DEFAULT_STENGTH) {
              Temp.append("| DRIVE = ")
                  .append(DriveStrength.GetContraintedDriveStrength(info.getDrive())).append(" ");
            }
            if (info.getIoStandard() != IoStandards.UNKNOWN
                && info.getIoStandard() != IoStandards.DEFAULT_STANDARD) {
              Temp.append("| IOSTANDARD = ")
                  .append(IoStandards.getConstraintedIoStandard(info.getIoStandard()))
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
  public void setMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
  }

  private ProcessBuilder Stage0Synth() {
    final var command = LineBuffer.getBuffer();
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
    final var command = LineBuffer.getBuffer();
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
    final var command = LineBuffer.getBuffer();
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
    final var command = LineBuffer.getBuffer();
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
    var command = LineBuffer.getBuffer();
    if (!IsCPLD) {
      command.add(xilinxVendor.getBinaryPath(4)).add("-w");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_UP) command.add("-g").add("UnusedPin:PULLUP");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_DOWN) command.add("-g").add("UnusedPin:PULLDOWN");
      command.add("-g").add("StartupClk:CCLK").add("logisim_par").add("{{1}}.bit", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME);
    } else {
      command.add(xilinxVendor.getBinaryPath(7)).add("-i").add("logisim.vm6");
    }
    final var stage4 = new ProcessBuilder(command.get());
    stage4.directory(new File(SandboxPath));
    return stage4;
  }

  private static String GetFPGADeviceString(BoardInformation currentBoard) {
    final var fpga = currentBoard.fpga;
    return String.format("%s-%s-%s", fpga.getPart(), fpga.getPackage(), fpga.getSpeedGrade());
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
          .append(IoStandards.BEHAVIOR_STRINGS[CurrentBoard.fpga.getClockStandard()]);
    }
    return result.toString();
  }

  @Override
  public boolean isBoardConnected() {
    // TODO: Detect if a board is connected, and in case of multiple boards select the one that should be used
    return true;
  }

}
