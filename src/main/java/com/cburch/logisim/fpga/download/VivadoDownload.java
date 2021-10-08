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
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHdlGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VivadoDownload implements VendorDownload {

  private final VendorSoftware vivadoVendor = VendorSoftware.getSoftware(VendorSoftware.VENDOR_VIVADO);
  private final String scriptPath;
  private final String sandboxPath;
  private final String xdcPath;
  private final String vivadoProjectPath;
  private final Netlist rootNetList;
  private MappableResourcesContainer mapInfo;
  private final BoardInformation boardInfo;
  private final List<String> entities;
  private final List<String> architectures;

  private static String bitStreamPath;
  private static final String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
  private static final String GENERATE_BITSTREAM_FILE = "vivadoGenerateBitStream.tcl";
  private static final String LOAD_BITSTEAM_FILE = "vivadoLoadBitStream.tcl";
  private static final String XDC_FILE = "vivadoConstraints.xdc";
  private static final String VIVADO_PROJECT_NAME = "vp";

  public VivadoDownload(
      String projectPath,
      Netlist rootNetList,
      BoardInformation boardInfo,
      List<String> entities,
      List<String> architectures) {
    this.sandboxPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.SANDBOX_PATH);
    this.scriptPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.SCRIPT_PATH);
    this.xdcPath = DownloadBase.getDirectoryLocation(projectPath, DownloadBase.XDC_PATH);
    this.rootNetList = rootNetList;
    this.boardInfo = boardInfo;
    this.entities = entities;
    this.architectures = architectures;
    this.vivadoProjectPath = this.sandboxPath + File.separator + VIVADO_PROJECT_NAME;
    bitStreamPath =
        vivadoProjectPath
            + File.separator
            + VIVADO_PROJECT_NAME
            + ".runs"
            + File.separator
            + "impl_1"
            + File.separator
            + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME
            + ".bit";
    bitStreamPath = bitStreamPath.replace("\\", "/");
  }

  @Override
  public int getNumberOfStages() {
    return 2;
  }

  @Override
  public String getStageMessage(int stage) {
    return switch (stage) {
      case 0 -> S.get("VivadoProject");
      case 1 -> S.get("VivadoBitstream");
      default -> "Unknown";
    };
  }

  @Override
  public ProcessBuilder performStep(int stage) {
    return switch (stage) {
      case 0 -> stage0Project();
      case 1 -> stage1Bit();
      default -> null;
    };
  }

  @Override
  public boolean readyForDownload() {
    return new File(bitStreamPath).exists();
  }

  @Override
  public ProcessBuilder downloadToBoard() {
    final var command = LineBuffer.getBuffer();
    command.add(vivadoVendor.getBinaryPath(0))
        .add("-mode")
        .add("batch")
        .add("-source")
        .add(scriptPath + File.separator + LOAD_BITSTEAM_FILE);
    final var stage0 = new ProcessBuilder(command.get());
    stage0.directory(new File(sandboxPath));
    return stage0;
  }

  @Override
  public boolean createDownloadScripts() {
    // create project files
    var createProjectFile = FileWriter.getFilePointer(scriptPath, CREATE_PROJECT_TCL);
    var xdcFile = FileWriter.getFilePointer(xdcPath, XDC_FILE);
    var generateBitstreamFile = FileWriter.getFilePointer(scriptPath, GENERATE_BITSTREAM_FILE);
    var loadBitstreamFile = FileWriter.getFilePointer(scriptPath, LOAD_BITSTEAM_FILE);
    if (createProjectFile == null
        || xdcFile == null
        || generateBitstreamFile == null
        || loadBitstreamFile == null) {
      createProjectFile = new File(scriptPath + CREATE_PROJECT_TCL);
      xdcFile = new File(xdcPath, XDC_FILE);
      generateBitstreamFile = new File(scriptPath, GENERATE_BITSTREAM_FILE);
      loadBitstreamFile = new File(scriptPath, LOAD_BITSTEAM_FILE);
      return createProjectFile.exists()
          && xdcFile.exists()
          && generateBitstreamFile.exists()
          && loadBitstreamFile.exists();
    }

    // fill create project TCL script
    var contents = new ArrayList<String>();
    contents.add(
        "create_project "
            + VIVADO_PROJECT_NAME
            + " \""
            + vivadoProjectPath.replace("\\", "/")
            + "\"");
    contents.add(
        "set_property part "
            + boardInfo.fpga.getPart()
            + boardInfo.fpga.getPackage()
            + boardInfo.fpga.getSpeedGrade()
            + " [current_project]");
    contents.add("set_property target_language VHDL [current_project]");
    // add all entities and architectures
    for (final var entity : entities) {
      contents.add("add_files \"" + entity + "\"");
    }
    for (final var architecture : architectures) {
      contents.add("add_files \"" + architecture + "\"");
    }
    // add xdc constraints
    contents.add("add_files -fileset constrs_1 \"" + xdcFile.getAbsolutePath().replace("\\", "/") + "\"");
    contents.add("exit");
    if (!FileWriter.writeContents(createProjectFile, contents)) return false;
    contents.clear();

    // fill the xdc file
    if (rootNetList.numberOfClockTrees() > 0 || rootNetList.requiresGlobalClockConnection()) {
      final var clockPin = boardInfo.fpga.getClockPinLocation();
      final var clockSignal = TickComponentHdlGeneratorFactory.FPGA_CLOCK;
      final var getPortsString = " [get_ports {" + clockSignal + "}]";
      contents.add("set_property PACKAGE_PIN " + clockPin + getPortsString);

      if (boardInfo.fpga.getClockStandard() != IoStandards.DEFAULT_STANDARD
          && boardInfo.fpga.getClockStandard() != IoStandards.UNKNOWN) {
        final var clockIoStandard = IoStandards.BEHAVIOR_STRINGS[boardInfo.fpga.getClockStandard()];
        contents.add("    set_property IOSTANDARD " + clockIoStandard + getPortsString);
      }

      final var clockFrequency = boardInfo.fpga.getClockFrequency();
      var clockPeriod = 1000000000.0 / (double) clockFrequency;
      contents.add(
          "    create_clock -add -name sys_clk_pin -period "
              + String.format(Locale.US, "%.2f", clockPeriod)
              + " -waveform {0 "
              + String.format("%1$,.0f", clockPeriod / 2)
              + "} "
              + getPortsString);
      contents.add("");
    }

    contents.addAll(getPinLocStrings());
    if (!FileWriter.writeContents(xdcFile, contents)) return false;
    contents.clear();

    // generate bitstream
    var openProjectPath = vivadoProjectPath + File.separator + VIVADO_PROJECT_NAME + ".xpr";
    openProjectPath = openProjectPath.replace("\\", "/");
    contents.add("open_project -verbose " + openProjectPath);
    contents.add("update_compile_order -fileset sources_1");
    contents.add("launch_runs synth_1");
    contents.add("wait_on_run synth_1");
    contents.add("launch_runs impl_1 -to_step write_bitstream -jobs 8");
    contents.add("wait_on_run impl_1");
    contents.add("exit");
    if (!FileWriter.writeContents(generateBitstreamFile, contents)) return false;
    contents.clear();

    // load bitstream
    final var jtagPos = String.valueOf(boardInfo.fpga.getFpgaJTAGChainPosition());
    final var lindex = "[lindex [get_hw_devices] " + jtagPos + "]";
    contents.add("open_hw");
    contents.add("connect_hw_server");
    contents.add("open_hw_target");
    contents.add("set_property PROGRAM.FILE {" + bitStreamPath + "} " + lindex);
    contents.add("current_hw_device " + lindex);
    contents.add("refresh_hw_device -update_hw_probes false " + lindex);
    contents.add("program_hw_device " + lindex);
    contents.add("close_hw");
    contents.add("exit");
    return FileWriter.writeContents(loadBitstreamFile, contents);
  }

  private List<String> getPinLocStrings() {
    final var contents = LineBuffer.getBuffer();
    for (final var key : mapInfo.getMappableResources().keySet()) {
      final var map = mapInfo.getMappableResources().get(key);
      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.isOpenMapped(i) && !map.isConstantMapped(i) && !map.isInternalMapped(i)) {
          final var netName = (map.isExternalInverted(i) ? "n_" : "") + map.getHdlString(i);
          // Note {{2}} is wrapped in additional {}!
          contents.add("set_property PACKAGE_PIN {{1}} [get_ports {{{2}}}]", map.getPinLocation(i), netName);
          final var info = map.getFpgaInfo(i);
          if (info != null) {
            final var ioStandard = info.getIoStandard();
            if (ioStandard != IoStandards.UNKNOWN && ioStandard != IoStandards.DEFAULT_STANDARD)
              contents.add("    set_property IOSTANDARD {{1}} [get_ports {{{2}}}]", IoStandards.getConstraintedIoStandard(info.getIoStandard()), netName);
          }
        }
      }
    }
    final var LedArrayMap = DownloadBase.getLedArrayMaps(mapInfo, rootNetList, boardInfo);
    for (final var key : LedArrayMap.keySet()) {
      contents.add("set_property PACKAGE_PIN {{1}} [get_ports {{{2}}}]", key, LedArrayMap.get(key));
    }
    return contents.get();
  }

  @Override
  public void setMapableResources(MappableResourcesContainer resources) {
    mapInfo = resources;
  }

  private ProcessBuilder stage0Project() {
    final var command = LineBuffer.getBuffer();
    command
        .add(vivadoVendor.getBinaryPath(0))
        .add("-mode")
        .add("batch")
        .add("-source")
        .add(scriptPath + File.separator + CREATE_PROJECT_TCL);

    final var stage0 = new ProcessBuilder(command.get());
    stage0.directory(new File(sandboxPath));
    return stage0;
  }

  private ProcessBuilder stage1Bit() {
    final var command = LineBuffer.getBuffer();
    command
        .add(vivadoVendor.getBinaryPath(0))
        .add("-mode")
        .add("batch")
        .add("-source")
        .add(scriptPath + File.separator + GENERATE_BITSTREAM_FILE);
    final var stage1 = new ProcessBuilder(command.get());
    stage1.directory(new File(sandboxPath));
    return stage1;
  }

  @Override
  public boolean isBoardConnected() {
    // TODO Detect if a board is connected, and in case of multiple boards select the one that
    // should be used
    return true;
  }

}
