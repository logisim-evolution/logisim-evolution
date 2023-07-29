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

import java.io.File;
import java.util.List;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHdlGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.LineBuffer;

public class OpenFpgaDownload  implements VendorDownload {

  private final VendorSoftware openfpgaVendor = VendorSoftware.getSoftware(VendorSoftware.VENDOR_OPENFPGA);
  private final String scriptPath;
  private final String projectPath;
  private final String sandboxPath;
  private final Netlist rootNetList;
  private MappableResourcesContainer mapInfo;
  private final BoardInformation boardInfo;
  private final List<String> entities;
  private final List<String> architectures;
  private final String HdlType;
  private final boolean writeToFlash;
  
  private final String YOSYS_SCRIPT_FILE = "yosys.script";
  private final String JSON_FILE = "toplevel.json";
  private final String PIN_CONSTRAINT_FILE = "toplevel.lpf";
  private final String NEXT_NPR_RESULT = "toplevel.config";
  private final String BIT_FILE = "toplevel.bit";

  public OpenFpgaDownload(
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
    this.rootNetList = rootNetList;
    this.boardInfo = boardInfo;
    this.entities = entities;
    this.architectures = architectures;
    this.HdlType = hdlType;
    this.writeToFlash = writeToFlash;
  }

  
  @Override
  public int getNumberOfStages() {
    return HdlType.equals(HdlGeneratorFactory.VHDL) ? 4 : 3;
  }

  @Override
  public String getStageMessage(int stage) {
    if (HdlType.equals(HdlGeneratorFactory.VHDL)) {
      return switch (stage) {
        case 0 -> S.get("OpenFpgaGhdl");
        case 1 -> S.get("OpenFpgaYosys");
        case 2 -> S.get("OpenFpganNextpnr");
        case 3 -> S.get("OpenFpganEcppack");
        default -> "unknown";
      };
    }
    return switch (stage) {
      case 0 -> S.get("OpenFpgaYosys");
      case 1 -> S.get("OpenFpganNextpnr");
      case 2 -> S.get("OpenFpganEcppack");
      default -> "unknown";
    };
  }

  @Override
  public ProcessBuilder performStep(int stage) {
    if (HdlType.equals(HdlGeneratorFactory.VHDL)) {
      return switch (stage) {
        case 0 -> stageGhdl();
        case 1 -> stageYosys();
        case 2 -> stageNextpnrEcp5();
        case 3 -> stageEcppack();
        default -> null;
      };
    }
    return switch (stage) {
      case 0 -> stageYosys();
      case 1 -> stageNextpnrEcp5();
      case 2 -> stageEcppack();
      default -> null;
    };
  }

  @Override
  public boolean readyForDownload() {
    return new File(sandboxPath + BIT_FILE).exists();
  }

  @Override
  public ProcessBuilder downloadToBoard() {
    final var command = LineBuffer.getBuffer();
    command.add(openfpgaVendor.getBinaryPath(4));
    if (writeToFlash) command.add("-f");
    command.add(BIT_FILE);
    final var stage = new ProcessBuilder(command.get());
    stage.directory(new File(sandboxPath));
    return stage;
  }

  @Override
  public boolean createDownloadScripts() {
    var yosysScript = FileWriter.getFilePointer(scriptPath, YOSYS_SCRIPT_FILE);
    var constraintFile = FileWriter.getFilePointer(scriptPath, PIN_CONSTRAINT_FILE);
    if (yosysScript == null || constraintFile == null) {
      yosysScript = new File(scriptPath + YOSYS_SCRIPT_FILE);
      constraintFile = new File(scriptPath + PIN_CONSTRAINT_FILE);
      return yosysScript.exists() && constraintFile.exists();
    }
    final var content = LineBuffer.getBuffer();
    if (HdlType.equals(HdlGeneratorFactory.VHDL)) {
      content
          .add("ghdl {{1}}", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME)
          .add("synth_ecp5 -json {{1}}", JSON_FILE);
    } else {
      for (final var vfile : entities) content.add("read -sv {{1}}", vfile);
      content
          .add("hierarchy -top {{1}}", ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME)
          .add("synth_ecp5 -json {{1}}", JSON_FILE);
    }
    if (!FileWriter.writeContents(yosysScript, content.get())) return false;
    content.clear();
    content.add("BLOCK RESETPATHS;").add("BLOCK ASYNCPATHS;\n");
    if (rootNetList.numberOfClockTrees() > 0 || rootNetList.requiresGlobalClockConnection()) {
      content
          .add("LOCATE COMP \"{{1}}\" SITE \"{{2}}\";", TickComponentHdlGeneratorFactory.FPGA_CLOCK, boardInfo.fpga.getClockPinLocation())
          .add("FREQUENCY PORT \"{{1}}\" {{2}};", TickComponentHdlGeneratorFactory.FPGA_CLOCK, Download.getClockFrequencyString(boardInfo).toUpperCase());
    }
    content.add(getPinLocations());
    return FileWriter.writeContents(constraintFile, content.get());
  }

  @Override
  public void setMapableResources(MappableResourcesContainer resources) {
    mapInfo = resources;
  }

  @Override
  public boolean isBoardConnected() {
    // TODO: Detect if a board is connected, and in case of multiple boards select the one that should be used 
    return true;
  }
  
  private List<String> getPinLocations() {
    final var pinInfo = LineBuffer.getBuffer();
    for (final var key : mapInfo.getMappableResources().keySet()) {
      final var map = mapInfo.getMappableResources().get(key);
      for (var pin = 0; pin < map.getNrOfPins(); pin++) {
        if (map.isMapped(pin) && !map.isOpenMapped(pin) && !map.isConstantMapped(pin) && !map.isInternalMapped(pin)) {
          pinInfo.add("LOCATE COMP \"{{1}}{{2}}\" SITE \"{{3}}\";", 
              map.isExternalInverted(pin) ? "n_" : "",
              map.getHdlString(pin),
              map.getPinLocation(pin).toUpperCase());
          final var info = map.getFpgaInfo(pin);
          if (info != null) {
            final var pullString = PullBehaviors.getPullString(info.getPullBehavior());
            final String pull = (pullString == null) ? " PULLMODE=NONE" :
                LineBuffer.format(" PULLMODE={{1}}", pullString.toUpperCase());
            final var ioTypeString = IoStandards.getIoString(info.getIoStandard());
            final String ioType = (ioTypeString == null) ? "" :
                LineBuffer.format(" IO_TYPE={{1}}", ioTypeString.toUpperCase());
            final var driveString = DriveStrength.getDriveString(info.getDrive());
            final String drive = (driveString == null) ? "" :
                LineBuffer.format(" DRIVE={{1}}", driveString);
            if (pullString != null || ioTypeString != null || driveString != null)
              pinInfo.add("IOBUF PORT \"{{1}}{{2}}\"{{3}}{{4}}{{5}};",
                  map.isExternalInverted(pin) ? "n_" : "",
                  map.getHdlString(pin),
                  pull,
                  ioType,
                  drive);
          }
        }
      }
    }
    final var LedArrayMap = DownloadBase.getScanningMaps(mapInfo, rootNetList, boardInfo);
    // TODO: add pull, drive and ioStandard
    for (var key : LedArrayMap.keySet()) {
      pinInfo.add("LOCATE COMP \"{{1}}\" SITE \"{{2}}\";", key, LedArrayMap.get(key));
    }
    return pinInfo.get();
  }

  private ProcessBuilder stageGhdl() {
    final var command = LineBuffer.getBuffer();
    command
        .add(openfpgaVendor.getBinaryPath(0))
        .add("-a");
    for (final var entity : entities)
      command.add(entity);
    for (final var archi : architectures)
      command.add(archi);
    final var stage = new ProcessBuilder(command.get());
    stage.directory(new File(sandboxPath));
    return stage;
  }
  
  private ProcessBuilder stageYosys() {
    final var command = LineBuffer.getBuffer();
    if (HdlType.equals(HdlGeneratorFactory.VHDL)) {
      command
          .add(openfpgaVendor.getBinaryPath(1))
          .add("-m")
          .add("ghdl.so")
          .add("-s")
          .add("{{1}}{{2}}", scriptPath.replace(projectPath, "../"), YOSYS_SCRIPT_FILE);
    } else {
      command
          .add(openfpgaVendor.getBinaryPath(1))
          .add("-s")
          .add("{{1}}{{2}}", scriptPath.replace(projectPath, "../"), YOSYS_SCRIPT_FILE);
    }
    final var stage = new ProcessBuilder(command.get());
    stage.directory(new File(sandboxPath));
    return stage;
  }
  
  private ProcessBuilder stageNextpnrEcp5() {
    final var command = LineBuffer.getBuffer();
    command
        .add(openfpgaVendor.getBinaryPath(2))
        .add("--{{1}}", boardInfo.fpga.getPart())
        .add("--package")
        .add(boardInfo.fpga.getPackage().toUpperCase())
        .add("--json")
        .add(JSON_FILE)
        .add("--lpf")
        .add("{{1}}{{2}}", scriptPath.replace(projectPath, "../"), PIN_CONSTRAINT_FILE)
        .add("--textcfg")
        .add(NEXT_NPR_RESULT);
    final var stage = new ProcessBuilder(command.get());
    stage.directory(new File(sandboxPath));
    return stage;
  }
  
  private ProcessBuilder stageEcppack() {
    final var command = LineBuffer.getBuffer();
    command
        .add(openfpgaVendor.getBinaryPath(3))
        .add("--compress")
        .add("--freq")
        .add("62.0")
        .add("--input")
        .add(NEXT_NPR_RESULT)
        .add("--bit")
        .add(BIT_FILE);
    final var stage = new ProcessBuilder(command.get());
    stage.directory(new File(sandboxPath));
    return stage;
  }
}
