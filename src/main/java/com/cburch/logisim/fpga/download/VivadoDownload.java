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
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VivadoDownload implements VendorDownload {

  private VendorSoftware vivadoVendor = VendorSoftware.getSoftware(VendorSoftware.VendorVivado);
  private String ScriptPath;
  private String SandboxPath;
  private String xdcPath;
  private String vivadoProjectPath;
  private FPGAReport Reporter;
  private Netlist RootNetList;
  private MappableResourcesContainer MapInfo;
  private BoardInformation BoardInfo;
  private ArrayList<String> Entities;
  private ArrayList<String> Architectures;

  private static String _bitStreamPath;
  private static final String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
  private static final String GENERATE_BITSTREAM_FILE = "vivadoGenerateBitStream.tcl";
  private static final String LOAD_BITSTEAM_FILE = "vivadoLoadBitStream.tcl";
  private static final String XDC_FILE = "vivadoConstraints.xdc";
  private static final String VIVADO_PROJECT_NAME = "vp";

  public VivadoDownload(
      String ProjectPath,
      FPGAReport Reporter,
      Netlist RootNetList,
      BoardInformation BoardInfo,
      ArrayList<String> Entities,
      ArrayList<String> Architectures) {
    this.SandboxPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.SandboxPath);
    this.ScriptPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.ScriptPath);
    this.xdcPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.XDCPath);
    this.Reporter = Reporter;
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
    List<String> command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + LOAD_BITSTEAM_FILE);
    ProcessBuilder stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  @Override
  public boolean CreateDownloadScripts() {
    // create project files
    File createProjectFile = FileWriter.GetFilePointer(ScriptPath, CREATE_PROJECT_TCL, Reporter);
    File xdcFile = FileWriter.GetFilePointer(xdcPath, XDC_FILE, Reporter);
    File generateBitstreamFile =
        FileWriter.GetFilePointer(ScriptPath, GENERATE_BITSTREAM_FILE, Reporter);
    File loadBitstreamFile = FileWriter.GetFilePointer(ScriptPath, LOAD_BITSTEAM_FILE, Reporter);
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
    ArrayList<String> contents = new ArrayList<String>();
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
    for (String entity : Entities) {
      contents.add("add_files \"" + entity + "\"");
    }
    for (String architecture : Architectures) {
      contents.add("add_files \"" + architecture + "\"");
    }
    // add xdc constraints
    contents.add(
        "add_files -fileset constrs_1 \"" + xdcFile.getAbsolutePath().replace("\\", "/") + "\"");
    contents.add("exit");
    if (!FileWriter.WriteContents(createProjectFile, contents, Reporter)) return false;
    contents.clear();

    // fill the xdc file
    if (RootNetList.NumberOfClockTrees() > 0) {
      String clockPin = BoardInfo.fpga.getClockPinLocation();
      String clockSignal = TickComponentHDLGeneratorFactory.FPGAClock;
      String getPortsString = " [get_ports {" + clockSignal + "}]";
      contents.add("set_property PACKAGE_PIN " + clockPin + getPortsString);

      if (BoardInfo.fpga.getClockStandard() != IoStandards.DefaulStandard
          && BoardInfo.fpga.getClockStandard() != IoStandards.Unknown) {
        String clockIoStandard = IoStandards.Behavior_strings[BoardInfo.fpga.getClockStandard()];
        contents.add("    set_property IOSTANDARD " + clockIoStandard + getPortsString);
      }

      Long clockFrequency = BoardInfo.fpga.getClockFrequency();
      double clockPeriod = 1000000000.0 / clockFrequency;
      contents.add(
          "    create_clock -add -name sys_clk_pin -period "
              + String.format(Locale.US, "%.2f", clockPeriod)
              + " -waveform {0 "
              + String.format("%1$,.0f", clockPeriod / 2)
              + "} "
              + getPortsString);
      contents.add("");
    }

    contents.addAll(GetPinLocStrings());
    if (!FileWriter.WriteContents(xdcFile, contents, Reporter)) return false;
    contents.clear();

    // generate bitstream
    String openProjectPath = vivadoProjectPath + File.separator + VIVADO_PROJECT_NAME + ".xpr";
    openProjectPath = openProjectPath.replace("\\", "/");
    contents.add("open_project -verbose " + openProjectPath);
    contents.add("update_compile_order -fileset sources_1");
    contents.add("launch_runs synth_1");
    contents.add("wait_on_run synth_1");
    contents.add("launch_runs impl_1 -to_step write_bitstream -jobs 8");
    contents.add("wait_on_run impl_1");
    contents.add("exit");
    if (!FileWriter.WriteContents(generateBitstreamFile, contents, Reporter)) return false;
    contents.clear();

    // load bitstream
    String JTAGPos = String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition());
    String lindex = "[lindex [get_hw_devices] " + JTAGPos + "]";
    contents.add("open_hw");
    contents.add("connect_hw_server");
    contents.add("open_hw_target");
    contents.add("set_property PROGRAM.FILE {" + _bitStreamPath + "} " + lindex);
    contents.add("current_hw_device " + lindex);
    contents.add("refresh_hw_device -update_hw_probes false " + lindex);
    contents.add("program_hw_device " + lindex);
    contents.add("close_hw");
    contents.add("exit");
    return FileWriter.WriteContents(loadBitstreamFile, contents, Reporter);
  }
  
  private ArrayList<String> GetPinLocStrings() {
    ArrayList<String> contents = new ArrayList<String>();
    for (ArrayList<String> key : MapInfo.getMappableResources().keySet()) {
      MapComponent map = MapInfo.getMappableResources().get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i)) {
          String netName = (map.isExternalInverted(i) ? "n_" : "")+map.getHdlString(i);
          contents.add("set_property PACKAGE_PIN " + map.getPinLocation(i) + " [get_ports {" + netName + "}]");
          FPGAIOInformationContainer info = map.getFpgaInfo(i);
          if (info != null) {
            if (info.GetIOStandard() != IoStandards.Unknown && info.GetIOStandard() != IoStandards.DefaulStandard) {
              contents.add(
                  "    set_property IOSTANDARD "
                      + IoStandards.GetConstraintedIoStandard(info.GetIOStandard())
                      + " [get_ports {"
                      + netName
                      + "}]");
            }
            if (info.GetIOStandard() != IoStandards.Unknown && info.GetIOStandard() != IoStandards.DefaulStandard) {
              contents.add(
                  "    set_property IOSTANDARD "
                      + IoStandards.GetConstraintedIoStandard(info.GetIOStandard())
                      + " [get_ports {"
                      + netName
                      + "}]");
            }
          }
        }
      }
    }
    return contents;
  }

  @Override
  public void SetMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
  }

  private ProcessBuilder Stage0Project() {
    List<String> command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + CREATE_PROJECT_TCL);
    ProcessBuilder stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  private ProcessBuilder Stage1Bit() {
    List<String> command = new ArrayList<String>();
    command.add(vivadoVendor.getBinaryPath(0));
    command.add("-mode");
    command.add("batch");
    command.add("-source");
    command.add(ScriptPath + File.separator + GENERATE_BITSTREAM_FILE);
    ProcessBuilder stage1 = new ProcessBuilder(command);
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
