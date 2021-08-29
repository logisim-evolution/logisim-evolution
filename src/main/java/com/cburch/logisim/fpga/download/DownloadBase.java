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
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.LedArrayGenericHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class DownloadBase {

  protected Project MyProject;
  protected BoardInformation MyBoardInformation = null;
  protected MappableResourcesContainer myMappableResources;
  static final String[] HDLPaths = {
    HDLGeneratorFactory.VERILOG.toLowerCase(),
    HDLGeneratorFactory.VHDL.toLowerCase(),
    "scripts",
    "sandbox",
    "ucf",
    "xdc"
  };
  public static final Integer VERILOG_SOURCE_PATH = 0;
  public static final Integer VHDL_SOURCE_PATH = 1;
  public static final Integer SCRIPT_PATH = 2;
  public static final Integer SANDBOX_PATH = 3;
  public static final Integer UCF_PATH = 4;
  public static final Integer XDC_PATH = 5;

  protected boolean VendorSoftwarePresent() {
    return VendorSoftware.toolsPresent(
        MyBoardInformation.fpga.getVendor(),
        VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor()));
  }

  protected boolean MapDesign(String CircuitName) {
    final var myFile = MyProject.getLogisimFile();
    final var rootSheet = myFile.getCircuit(CircuitName);
    if (rootSheet == null) {
      Reporter.Report.AddError("INTERNAL ERROR: Circuit not found ?!?");
      return false;
    }
    if (MyBoardInformation == null) {
      Reporter.Report.AddError("INTERNAL ERROR: No board information available ?!?");
      return false;
    }

    final var boardComponents = MyBoardInformation.GetComponents();
    Reporter.Report.AddInfo("The Board " + MyBoardInformation.getBoardName() + " has:");
    for (final var key : boardComponents.keySet()) {
      Reporter.Report.AddInfo(boardComponents.get(key).size() + " " + key + "(s)");
    }
    /*
     * At this point I require 2 sorts of information: 1) A hierarchical
     * netlist of all the wires that needs to be bubbled up to the toplevel
     * in order to connect the LEDs, Buttons, etc. (hence for the HDL
     * generation). 2) A list with all components that are required to be
     * mapped to PCB components. Identification can be done by a hierarchy
     * name plus component/sub-circuit name
     */
    myMappableResources = rootSheet.getBoardMap(MyBoardInformation.getBoardName());
    if (myMappableResources == null)
      myMappableResources = new MappableResourcesContainer(MyBoardInformation, rootSheet);
    else
      myMappableResources.updateMapableComponents();

    return true;
  }

  protected boolean mapDesignCheckIOs() {
    if (myMappableResources.isCompletelyMapped()) return true;
    final var confirm = OptionPane.showConfirmDialog(MyProject.getFrame(), S.get("FpgaNotCompleteMap"),
        S.get("FpgaIncompleteMap"), OptionPane.YES_NO_OPTION);
    return confirm == OptionPane.YES_OPTION;
  }

  protected boolean performDRC(String CircuitName, String HDLType) {
    final var root = MyProject.getLogisimFile().getCircuit(CircuitName);
    final var sheetNames = new ArrayList<String>();
    var drcResult = Netlist.DRC_PASSED;
    if (root == null) {
      drcResult |= Netlist.DRC_ERROR;
    } else {
      root.getNetList().clear();
      drcResult = root.getNetList().designRuleCheckResult(true, sheetNames);
    }
    return drcResult == Netlist.DRC_PASSED;
  }

  protected String getProjDir(String selectedCircuit) {
    var projectDir = AppPreferences.FPGA_Workspace.get() + File.separator + MyProject.getLogisimFile().getName();
    if (!projectDir.endsWith(File.separator)) {
      projectDir += File.separator;
    }
    projectDir += CorrectLabel.getCorrectLabel(selectedCircuit) + File.separator;
    return projectDir;
  }

  protected boolean writeHDL(String selectedCircuit, Double frequency) {
    if (!genDirectory(
        AppPreferences.FPGA_Workspace.get()
            + File.separator
            + MyProject.getLogisimFile().getName())) {
      Reporter.Report.AddFatalError(
          "Unable to create directory: \""
              + AppPreferences.FPGA_Workspace.get()
              + File.separator
              + MyProject.getLogisimFile().getName()
              + "\"");
      return false;
    }
    final var projectDir = getProjDir(selectedCircuit);
    final var rootSheet = MyProject.getLogisimFile().getCircuit(selectedCircuit);
    if (!cleanDirectory(projectDir)) {
      Reporter.Report.AddFatalError(
          "Unable to cleanup old project files in directory: \"" + projectDir + "\"");
      return false;
    }
    if (!genDirectory(projectDir)) {
      Reporter.Report.AddFatalError("Unable to create directory: \"" + projectDir + "\"");
      return false;
    }
    for (final var hdlPath : HDLPaths) {
      if (!genDirectory(projectDir + hdlPath)) {
        Reporter.Report.AddFatalError("Unable to create directory: \"" + projectDir + hdlPath + "\"");
        return false;
      }
    }

    final var generatedHDLComponents = new HashSet<String>();
    var worker = rootSheet.getSubcircuitFactory().getHDLGenerator(rootSheet.getStaticAttributes());
    if (worker == null) {
      Reporter.Report.AddFatalError("Internal error on HDL generation, null pointer exception");
      return false;
    }
    if (!worker.GenerateAllHDLDescriptions(generatedHDLComponents, projectDir, null)) {
      return false;
    }
    /* Here we generate the top-level shell */
    if (rootSheet.getNetList().numberOfClockTrees() > 0) {
      final var ticker = new TickComponentHDLGeneratorFactory(MyBoardInformation.fpga.getClockFrequency(), frequency /* , boardFreq.isSelected() */);
      if (!AbstractHDLGeneratorFactory.WriteEntity(
          projectDir + ticker.GetRelativeDirectory(),
          ticker.GetEntity(rootSheet.getNetList(), null, ticker.getComponentStringIdentifier()),
          ticker.getComponentStringIdentifier())) {
        return false;
      }
      if (!AbstractHDLGeneratorFactory.WriteArchitecture(
          projectDir + ticker.GetRelativeDirectory(),
          ticker.GetArchitecture(rootSheet.getNetList(), null, ticker.getComponentStringIdentifier()), ticker.getComponentStringIdentifier())) {
        return false;
      }

      final var clockGen = rootSheet.getNetList()
          .getAllClockSources()
          .get(0)
          .getFactory()
          .getHDLGenerator(rootSheet.getNetList().getAllClockSources().get(0).getAttributeSet());
      final var compName = rootSheet.getNetList().getAllClockSources().get(0).getFactory().getHDLName(null);
      if (!AbstractHDLGeneratorFactory.WriteEntity(
          projectDir + clockGen.GetRelativeDirectory(),
          clockGen.GetEntity(rootSheet.getNetList(), null, compName),
          compName)) {
        return false;
      }
      if (!AbstractHDLGeneratorFactory.WriteArchitecture(
          projectDir + clockGen.GetRelativeDirectory(),
          clockGen.GetArchitecture(rootSheet.getNetList(), null, compName),
          compName)) {
        return false;
      }
    }
    final var top = new ToplevelHDLGeneratorFactory(MyBoardInformation.fpga.getClockFrequency(),
        frequency, rootSheet, myMappableResources);
    if (top.hasLedArray()) {
      for (var type : LedArrayDriving.DRIVING_STRINGS) {
        if (top.hasLedArrayType(type)) {
          worker = LedArrayGenericHDLGeneratorFactory.getSpecificHDLGenerator(type);
          final var name = LedArrayGenericHDLGeneratorFactory.getSpecificHDLName(type);
          if (worker != null && name != null) {
            if (!AbstractHDLGeneratorFactory.WriteEntity(
                projectDir + worker.GetRelativeDirectory(),
                worker.GetEntity(rootSheet.getNetList(), null, name),
                worker.getComponentStringIdentifier())) {
              return false;
            }
            if (!AbstractHDLGeneratorFactory.WriteArchitecture(
                projectDir + worker.GetRelativeDirectory(),
                worker.GetArchitecture(rootSheet.getNetList(), null, name),
                worker.getComponentStringIdentifier())) {
              return false;
            }
          }
        }
      }
    }
    if (!AbstractHDLGeneratorFactory.WriteEntity(
        projectDir + top.GetRelativeDirectory(),
        top.GetEntity(rootSheet.getNetList(), null, ToplevelHDLGeneratorFactory.FPGAToplevelName),
        top.getComponentStringIdentifier())) {
      return false;
    }
    return AbstractHDLGeneratorFactory.WriteArchitecture(
        projectDir + top.GetRelativeDirectory(),
        top.GetArchitecture(rootSheet.getNetList(), null, ToplevelHDLGeneratorFactory.FPGAToplevelName),
        top.getComponentStringIdentifier());
  }

  protected boolean genDirectory(String dirPath) {
    try {
      var dir = new File(dirPath);
      return dir.exists() ? true : dir.mkdirs();
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Could not check/create directory :" + dirPath);
      return false;
    }
  }

  protected void getVhdlFiles(String sourcePath, String path, ArrayList<String> entities, ArrayList<String> behaviors, String type) {
    final var dir = new File(path);
    final var files = dir.listFiles();
    for (final var thisFile : files) {
      if (thisFile.isDirectory()) {
        if (path.endsWith(File.separator)) {
          getVhdlFiles(sourcePath, path + thisFile.getName(), entities, behaviors, type);
        } else {
          getVhdlFiles(sourcePath, path + File.separator + thisFile.getName(), entities, behaviors, type);
        }
      } else {
        final var entityMask = (type.equals(HDLGeneratorFactory.VHDL)) ? FileWriter.ENTITY_EXTENSION + ".vhd" : ".v";
        final var architectureMask = (type.equals(HDLGeneratorFactory.VHDL))
            ? FileWriter.ARCHITECTURE_EXTENSION + ".vhd"
            : "#not_searched#";
        if (thisFile.getName().endsWith(entityMask)) {
          entities.add((path + File.separator + thisFile.getName()).replace("\\", "/"));
        } else if (thisFile.getName().endsWith(architectureMask)) {
          behaviors.add((path + File.separator + thisFile.getName()).replace("\\", "/"));
        }
      }
    }
  }

  public static String getDirectoryLocation(String projectBase, int identifier) {
    final var base = (projectBase.endsWith(File.separator)) ? projectBase : projectBase + File.separator;
    if (identifier >= HDLPaths.length) return null;
    return base + HDLPaths[identifier] + File.separator;
  }

  private boolean cleanDirectory(String dir) {
    try {
      final var thisDir = new File(dir);
      if (!thisDir.exists()) return true;
      for (var theFiles : thisDir.listFiles()) {
        if (theFiles.isDirectory()) {
          if (!cleanDirectory(theFiles.getPath())) return false;
        } else {
          if (!theFiles.delete()) return false;
        }
      }
      return thisDir.delete();
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Could not remove directory tree :" + dir);
      return false;
    }
  }

  public static HashMap<String, String> getLedArrayMaps(MappableResourcesContainer maps, Netlist nets, BoardInformation board) {
    final var ledArrayMaps = new HashMap<String, String>();
    var hasMappedClockedArray = false;
    for (final var comp : maps.getIOComponentInformation().getComponents()) {
      if (comp.GetType().equals(IOComponentTypes.LEDArray)) {
        if (comp.hasMap()) {
          hasMappedClockedArray |= LedArrayGenericHDLGeneratorFactory.requiresClock(comp.getArrayDriveMode());
          for (var pin = 0; pin < comp.getExternalPinCount(); pin++) {
            ledArrayMaps.put(LedArrayGenericHDLGeneratorFactory.getExternalSignalName(
                comp.getArrayDriveMode(),
                comp.getNrOfRows(),
                comp.getNrOfColumns(),
                comp.getArrayId(),
                pin), comp.getPinLocation(pin));
          }
        }
      }
    }
    if (hasMappedClockedArray && (nets.numberOfClockTrees() == 0) && !nets.requiresGlobalClockConnection()) {
      ledArrayMaps.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, board.fpga.getClockPinLocation());
    }
    return ledArrayMaps;
  }
}
