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
import java.util.List;

public class XilinxDownload implements VendorDownload {

  private final VendorSoftware xilinxVendor = VendorSoftware.getSoftware(VendorSoftware.VENDOR_XILINX);
  private final String scriptPath;
  private final String projectPath;
  private final String sandboxPath;
  private final String ucfPath;
  private final Netlist rootNetList;
  private MappableResourcesContainer mapInfo;
  private final BoardInformation boardInfo;
  private final List<String> entities;
  private final List<String> architectures;
  private final String HdlType;
  private final String bitfileExt;
  private final boolean isCpld;
  private final boolean writeToFlash;

  private static final String VHDL_LIST_FILE = "XilinxVHDLList.prj";
  private static final String SCRIPT_FILE = "XilinxScript.cmd";
  private static final String UCF_FILE = "XilinxConstraints.ucf";
  private static final String DOWNLOAD_FILE = "XilinxDownload";
  private static final String MCS_FILE = "XilinxProm.mcs";

  private static final Integer BUFFER_SIZE = 16 * 1024;

  public XilinxDownload(
      String projectPath,
      Netlist rootNetList,
      BoardInformation boardInfo,
      List<String> entities,
      List<String> architectures,
      String hdlType,
      boolean writeToFlash) {
    this.projectPath = projectPath;
    this.sandboxPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.SANDBOX_PATH);
    this.scriptPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.SCRIPT_PATH);
    this.ucfPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.UCF_PATH);
    this.rootNetList = rootNetList;
    this.boardInfo = boardInfo;
    this.entities = entities;
    this.architectures = architectures;
    this.HdlType = hdlType;
    this.writeToFlash = writeToFlash;

    final var part = boardInfo.fpga.getPart().toUpperCase();
    isCpld = part.startsWith("XC2C") || part.startsWith("XA2C") || part.startsWith("XCR3") || part.startsWith("XC9500") || part.startsWith("XA9500");
    bitfileExt = isCpld ? "jed" : "bit";
  }

  @Override
  public int getNumberOfStages() {
    return 5;
  }

  @Override
  public String getStageMessage(int stage) {
    return switch (stage) {
      case 0 -> S.get("XilinxSynth");
      case 1 -> S.get("XilinxContraints");
      case 2 -> S.get("XilinxMap");
      case 3 -> S.get("XilinxPAR");
      case 4 -> S.get("XilinxBit");
      default -> "unknown";
    };
  }

  @Override
  public ProcessBuilder performStep(int stage) {
    return switch (stage) {
      case 0 -> stage0Synth();
      case 1 -> stage1Constraints();
      case 2 -> stage2Map();
      case 3 -> stage3Par();
      case 4 -> stage4Bit();
      default -> null;
    };
  }

  @Override
  public boolean readyForDownload() {
    return new File(sandboxPath + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME + "." + bitfileExt).exists();
  }

  @Override
  public ProcessBuilder downloadToBoard() {
    if (!boardInfo.fpga.isUsbTmcDownloadRequired()) {
      var command = new ArrayList<String>();
      command.add(xilinxVendor.getBinaryPath(5));
      command.add("-batch");
      command.add(scriptPath.replace(projectPath, "../") + File.separator + DOWNLOAD_FILE);
      final var xilinx = new ProcessBuilder(command);
      xilinx.directory(new File(sandboxPath));
      return xilinx;
    } else {
      Reporter.report.clearConsole();
      /* Here we do the USBTMC Download */
      var usbtmcdevice = new File("/dev/usbtmc0").exists();
      if (!usbtmcdevice) {
        Reporter.report.addFatalError(S.get("XilinxUsbTmc"));
        return null;
      }
      var bitfile = new File(sandboxPath + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME + "." + bitfileExt);
      var bitfileBuffer = new byte[BUFFER_SIZE];
      var bitfileBufferSize = 0;
      BufferedInputStream bitfileIn;
      try {
        bitfileIn = new BufferedInputStream(new FileInputStream(bitfile));
      } catch (FileNotFoundException e) {
        Reporter.report.addFatalError(S.get("XilinxOpenFailure", bitfile));
        return null;
      }
      var usbtmc = new File("/dev/usbtmc0");
      BufferedOutputStream usbtmcOut;
      try {
        usbtmcOut = new BufferedOutputStream(new FileOutputStream(usbtmc));
        usbtmcOut.write("FPGA ".getBytes());
        bitfileBufferSize = bitfileIn.read(bitfileBuffer, 0, BUFFER_SIZE);
        while (bitfileBufferSize > 0) {
          usbtmcOut.write(bitfileBuffer, 0, bitfileBufferSize);
          bitfileBufferSize = bitfileIn.read(bitfileBuffer, 0, BUFFER_SIZE);
        }
        usbtmcOut.close();
        bitfileIn.close();
      } catch (IOException e) {
        Reporter.report.addFatalError(S.get("XilinxUsbTmcError"));
      }
    }
    return null;
  }

  @Override
  public boolean createDownloadScripts() {
    final var jtagPos = String.valueOf(boardInfo.fpga.getFpgaJTAGChainPosition());
    var scriptFile = FileWriter.getFilePointer(scriptPath, SCRIPT_FILE);
    var vhdlListFile = FileWriter.getFilePointer(scriptPath, VHDL_LIST_FILE);
    var ucfFile = FileWriter.getFilePointer(ucfPath, UCF_FILE);
    var downloadFile = FileWriter.getFilePointer(scriptPath, DOWNLOAD_FILE);
    if (scriptFile == null || vhdlListFile == null || ucfFile == null || downloadFile == null) {
      scriptFile = new File(scriptPath + SCRIPT_FILE);
      vhdlListFile = new File(scriptPath + VHDL_LIST_FILE);
      ucfFile = new File(ucfPath + UCF_FILE);
      downloadFile = new File(scriptPath + DOWNLOAD_FILE);
      return scriptFile.exists()
          && vhdlListFile.exists()
          && ucfFile.exists()
          && downloadFile.exists();
    }
    final var contents = LineBuffer.getBuffer()
            .pair("JTAGPos", jtagPos)
            .pair("fileExt", bitfileExt)
            .pair("fileBaseName", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME)
            .pair("mcsFile", scriptPath + File.separator + MCS_FILE)
            .pair("hdlType", HdlType.toUpperCase());

    for (var entity : entities) contents.add("{{hdlType}} work \"{{1}}\"", entity);
    for (var arch : architectures) contents.add("{{hdlType}} work \"{{1}}\"", arch);
    if (!FileWriter.writeContents(vhdlListFile, contents.get())) return false;

    contents
          .clear()
          .add(
              "run -top {{1}} -ofn logisim.ngc -ofmt NGC -ifn {{2}}{{3}} -ifmt mixed -p {{4}}",
              ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME,
              scriptPath.replace(projectPath, "../"),
              VHDL_LIST_FILE,
              getFpgaDeviceString(boardInfo));

    if (!FileWriter.writeContents(scriptFile, contents.get())) return false;

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
      if (!isCpld) {
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
    if (!FileWriter.writeContents(downloadFile, contents.get())) return false;

    contents.clear();
    if (rootNetList.numberOfClockTrees() > 0 || rootNetList.requiresGlobalClockConnection()) {
      contents
          .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
          .pair("clockFreq", Download.getClockFrequencyString(boardInfo))
          .pair("clockPin", getXilinxClockPin(boardInfo))
          .add("""
            NET "{{clock}}" {{clockPin}} ;
            NET "{{clock}}" TNM_NET = "{{clock}}" ;
            TIMESPEC "TS_{{clock}}" = PERIOD "{{clock}}" {{clockFreq}} HIGH 50 % ;
            """);
    }
    contents.add(getPinLocStrings());
    return FileWriter.writeContents(ucfFile, contents.get());
  }

  private ArrayList<String> getPinLocStrings() {
    var contents = new ArrayList<String>();
    var temp = new StringBuilder();
    for (var key : mapInfo.getMappableResources().keySet()) {
      var map = mapInfo.getMappableResources().get(key);
      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.isOpenMapped(i) && !map.isConstantMapped(i) && !map.isInternalMapped(i)) {
          temp.setLength(0);
          temp.append("NET \"");
          if (map.isExternalInverted(i)) temp.append("n_");
          temp.append(map.getHdlString(i)).append("\" ");
          temp.append("LOC = \"").append(map.getPinLocation(i)).append("\" ");
          final var info = map.getFpgaInfo(i);
          if (info != null) {
            if (info.getPullBehavior() != PullBehaviors.UNKNOWN
                && info.getPullBehavior() != PullBehaviors.FLOAT) {
              temp.append("| ")
                  .append(PullBehaviors.getContraintedPullString(info.getPullBehavior()))
                  .append(" ");
            }
            if (info.getDrive() != DriveStrength.UNKNOWN
                && info.getDrive() != DriveStrength.DEFAULT_STENGTH) {
              temp.append("| DRIVE = ")
                  .append(DriveStrength.GetContraintedDriveStrength(info.getDrive())).append(" ");
            }
            if (info.getIoStandard() != IoStandards.UNKNOWN
                && info.getIoStandard() != IoStandards.DEFAULT_STANDARD) {
              temp.append("| IOSTANDARD = ")
                  .append(IoStandards.getConstraintedIoStandard(info.getIoStandard()))
                  .append(" ");
            }
          }
          temp.append(";");
          contents.add(temp.toString());
        }
      }
    }
    final var LedArrayMap = DownloadBase.getLedArrayMaps(mapInfo, rootNetList, boardInfo);
    for (var key : LedArrayMap.keySet()) {
      contents.add("NET \"" + LedArrayMap.get(key) + "\" LOC=\"" + key + "\";");
    }
    return contents;
  }

  @Override
  public void setMapableResources(MappableResourcesContainer resources) {
    mapInfo = resources;
  }

  private ProcessBuilder stage0Synth() {
    final var command = LineBuffer.getBuffer();
    command
        .add(xilinxVendor.getBinaryPath(0))
        .add("-ifn")
        .add(scriptPath.replace(projectPath, "../") + File.separator + SCRIPT_FILE)
        .add("-ofn")
        .add("logisim.log");
    final var stage0 = new ProcessBuilder(command.get());
    stage0.directory(new File(sandboxPath));
    return stage0;
  }

  private ProcessBuilder stage1Constraints() {
    final var command = LineBuffer.getBuffer();
    command
        .add(xilinxVendor.getBinaryPath(1))
        .add("-intstyle")
        .add("ise")
        .add("-uc")
        .add(ucfPath.replace(projectPath, "../") + File.separator + UCF_FILE)
        .add("logisim.ngc")
        .add("logisim.ngd");
    final var stage1 = new ProcessBuilder(command.get());
    stage1.directory(new File(sandboxPath));
    return stage1;
  }

  private ProcessBuilder stage2Map() {
    if (isCpld) return null; /* mapping is skipped for the CPLD target*/
    final var command = LineBuffer.getBuffer();
    command
        .add(xilinxVendor.getBinaryPath(2))
        .add("-intstyle")
        .add("ise")
        .add("-o")
        .add("logisim_map")
        .add("logisim.ngd");
    final var stage2 = new ProcessBuilder(command.get());
    stage2.directory(new File(sandboxPath));
    return stage2;
  }

  private ProcessBuilder stage3Par() {
    final var command = LineBuffer.getBuffer();
    if (!isCpld) {
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
    stage3.directory(new File(sandboxPath));
    return stage3;
  }

  private ProcessBuilder stage4Bit() {
    final var command = LineBuffer.getBuffer();
    if (!isCpld) {
      command.add(xilinxVendor.getBinaryPath(4)).add("-w");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_UP) command.add("-g").add("UnusedPin:PULLUP");
      if (boardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PULL_DOWN) command.add("-g").add("UnusedPin:PULLDOWN");
      command.add("-g").add("StartupClk:CCLK").add("logisim_par").add("{{1}}.bit", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME);
    } else {
      command.add(xilinxVendor.getBinaryPath(7)).add("-i").add("logisim.vm6");
    }
    final var stage4 = new ProcessBuilder(command.get());
    stage4.directory(new File(sandboxPath));
    return stage4;
  }

  private static String getFpgaDeviceString(BoardInformation currentBoard) {
    final var fpga = currentBoard.fpga;
    return String.format("%s-%s-%s", fpga.getPart(), fpga.getPackage(), fpga.getSpeedGrade());
  }

  private static String getXilinxClockPin(BoardInformation currentBoard) {
    final var result = new StringBuilder();
    result.append("LOC = \"").append(currentBoard.fpga.getClockPinLocation()).append("\"");
    if (currentBoard.fpga.getClockPull() == PullBehaviors.PULL_UP) {
      result.append(" | PULLUP");
    }
    if (currentBoard.fpga.getClockPull() == PullBehaviors.PULL_DOWN) {
      result.append(" | PULLDOWN");
    }
    if (currentBoard.fpga.getClockStandard() != IoStandards.DEFAULT_STANDARD
        && currentBoard.fpga.getClockStandard() != IoStandards.UNKNOWN) {
      result.append(" | IOSTANDARD = ")
          .append(IoStandards.BEHAVIOR_STRINGS[currentBoard.fpga.getClockStandard()]);
    }
    return result.toString();
  }

  @Override
  public boolean isBoardConnected() {
    // TODO: Detect if a board is connected, and in case of multiple boards select the one that should be used
    return true;
  }

}
