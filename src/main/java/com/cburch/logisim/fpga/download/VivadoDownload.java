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
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class VivadoDownload implements VendorDownload {

  private final VendorSoftware vivadoVendor =
      VendorSoftware.getSoftware(VendorSoftware.VENDOR_VIVADO);
  private final String ScriptPath;
  private final String SandboxPath;
  private final String xdcPath;
  private final String vivadoProjectPath;
  private final Netlist RootNetList;
  private MappableResourcesContainer MapInfo;
  private final BoardInformation BoardInfo;
  private final ArrayList<String> Entities;
  private final ArrayList<String> Architectures;

  private static String _bitStreamPath;
  private static final String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
  private static final String GENERATE_BITSTREAM_FILE = "vivadoGenerateBitStream.tcl";
  private static final String LOAD_BITSTEAM_FILE = "vivadoLoadBitStream.tcl";
  private static final String XDC_FILE = "vivadoConstraints.xdc";
  private static final String VIVADO_PROJECT_NAME = "vp";

  public VivadoDownload(
      String ProjectPath,
      Netlist RootNetList,
      BoardInformation BoardInfo,
      ArrayList<String> Entities,
      ArrayList<String> Architectures) {
    this.SandboxPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.SANDBOX_PATH);
    this.ScriptPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.SCRIPT_PATH);
    this.xdcPath = DownloadBase.getDirectoryLocation(ProjectPath, DownloadBase.XDC_PATH);
    this.RootNetList = RootNetList;
    this.BoardInfo = BoardInfo;
    this.Entities = Entities;
    this.Architectures = Architectures;
    this.vivadoProjectPath = this.SandboxPath + File.separator + VIVADO_PROJECT_NAME;
    _bitStreamPath =
        vivadoProjectPath
            + File.separator
            + VIVADO_PROJECT_NAME
            + ".runs"
            + File.separator
            + "impl_1"
            + File.separator
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + ".bit";
    _bitStreamPath = _bitStreamPath.replace("\\", "/");
  }

  @Override
  public int GetNumberOfStages() {
    return 2;
  }

  @Override
  public String GetStageMessage(int stage) {
    switch (stage) {
      case 0:
        return S.get("VivadoProject");
      case 1:
        return S.get("VivadoBitstream");
      default:
        return "Unknown";
    }
  }

  @Override
  public ProcessBuilder PerformStep(int stage) {
    switch (stage) {
      case 0:
        return Stage0Project();
      case 1:
        return Stage1Bit();
      default:
        return null;
    }
  }

  @Override
  public boolean readyForDownload() {
    return new File(_bitStreamPath).exists();
  }

  @Override
  public ProcessBuilder DownloadToBoard() {
    var command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + LOAD_BITSTEAM_FILE);
    final var stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  @Override
  public boolean CreateDownloadScripts() {
    // create project files
    var createProjectFile = FileWriter.GetFilePointer(ScriptPath, CREATE_PROJECT_TCL);
    var xdcFile = FileWriter.GetFilePointer(xdcPath, XDC_FILE);
    var generateBitstreamFile = FileWriter.GetFilePointer(ScriptPath, GENERATE_BITSTREAM_FILE);
    var loadBitstreamFile = FileWriter.GetFilePointer(ScriptPath, LOAD_BITSTEAM_FILE);
    if (createProjectFile == null
        || xdcFile == null
        || generateBitstreamFile == null
        || loadBitstreamFile == null) {
      createProjectFile = new File(ScriptPath + CREATE_PROJECT_TCL);
      xdcFile = new File(xdcPath, XDC_FILE);
      generateBitstreamFile = new File(ScriptPath, GENERATE_BITSTREAM_FILE);
      loadBitstreamFile = new File(ScriptPath, LOAD_BITSTEAM_FILE);
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
            + BoardInfo.fpga.getPart()
            + BoardInfo.fpga.getPackage()
            + BoardInfo.fpga.getSpeedGrade()
            + " [current_project]");
    contents.add("set_property target_language VHDL [current_project]");
    // add all entities and architectures
    for (var entity : Entities) {
      contents.add("add_files \"" + entity + "\"");
    }
    for (var architecture : Architectures) {
      contents.add("add_files \"" + architecture + "\"");
    }
    // add xdc constraints
    contents.add("add_files -fileset constrs_1 \"" + xdcFile.getAbsolutePath().replace("\\", "/") + "\"");
    contents.add("exit");
    if (!FileWriter.WriteContents(createProjectFile, contents)) return false;
    contents.clear();

    // fill the xdc file
    if (RootNetList.numberOfClockTrees() > 0 || RootNetList.requiresGlobalClockConnection()) {
      final var clockPin = BoardInfo.fpga.getClockPinLocation();
      final var clockSignal = TickComponentHDLGeneratorFactory.FPGA_CLOCK;
      final var getPortsString = " [get_ports {" + clockSignal + "}]";
      contents.add("set_property PACKAGE_PIN " + clockPin + getPortsString);

      if (BoardInfo.fpga.getClockStandard() != IoStandards.DEFAULT_STANDARD
          && BoardInfo.fpga.getClockStandard() != IoStandards.UNKNOWN) {
        final var clockIoStandard = IoStandards.Behavior_strings[BoardInfo.fpga.getClockStandard()];
        contents.add("    set_property IOSTANDARD " + clockIoStandard + getPortsString);
      }

      final var clockFrequency = BoardInfo.fpga.getClockFrequency();
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
    if (!FileWriter.WriteContents(xdcFile, contents)) return false;
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
    if (!FileWriter.WriteContents(generateBitstreamFile, contents)) return false;
    contents.clear();

    // load bitstream
    final var JTAGPos = String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition());
    final var lindex = "[lindex [get_hw_devices] " + JTAGPos + "]";
    contents.add("open_hw");
    contents.add("connect_hw_server");
    contents.add("open_hw_target");
    contents.add("set_property PROGRAM.FILE {" + _bitStreamPath + "} " + lindex);
    contents.add("current_hw_device " + lindex);
    contents.add("refresh_hw_device -update_hw_probes false " + lindex);
    contents.add("program_hw_device " + lindex);
    contents.add("close_hw");
    contents.add("exit");
    return FileWriter.WriteContents(loadBitstreamFile, contents);
  }

  private ArrayList<String> getPinLocStrings() {
    final var contents = new LineBuffer();
    for (final var key : MapInfo.getMappableResources().keySet()) {
      final var map = MapInfo.getMappableResources().get(key);
      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i) && !map.isInternalMapped(i)) {
          final var netName = (map.isExternalInverted(i) ? "n_" : "") + map.getHdlString(i);
          // Note {{2}} is wrapped in additional {}!
          contents.add("set_property PACKAGE_PIN {{1}} [get_ports {{{2}}}]", map.getPinLocation(i), netName);
          final var info = map.getFpgaInfo(i);
          if (info != null) {
            final var ioStandard = info.GetIOStandard();
            if (ioStandard != IoStandards.UNKNOWN && ioStandard != IoStandards.DEFAULT_STANDARD)
              contents.add("    set_property IOSTANDARD {{1}} [get_ports {{{2}}}]", IoStandards.GetConstraintedIoStandard(info.GetIOStandard()), netName);
          }
        }
      }
    }
    final var LedArrayMap = DownloadBase.getLedArrayMaps(MapInfo, RootNetList, BoardInfo);
    for (final var key : LedArrayMap.keySet()) {
      contents.add("set_property PACKAGE_PIN {{1}} [get_ports {{{2}}}]", key, LedArrayMap.get(key));
    }
    return contents.get();
  }

  @Override
  public void SetMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
  }

  private ProcessBuilder Stage0Project() {
    var command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + CREATE_PROJECT_TCL);
    final var stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  private ProcessBuilder Stage1Bit() {
    var command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + GENERATE_BITSTREAM_FILE);
    final var stage1 = new ProcessBuilder(command);
    stage1.directory(new File(SandboxPath));
    return stage1;
  }

  @Override
  public boolean BoardConnected() {
    // TODO Detect if a board is connected, and in case of multiple boards select the one that
    // should be used
    return true;
  }

}
