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
  protected MappableResourcesContainer MyMappableResources;
  static final String[] HDLPaths = {
    HDLGeneratorFactory.VERILOG.toLowerCase(),
    HDLGeneratorFactory.VHDL.toLowerCase(),
    "scripts",
    "sandbox",
    "ucf",
    "xdc"
  };
  public static final Integer VerilogSourcePath = 0;
  public static final Integer VHDLSourcePath = 1;
  public static final Integer ScriptPath = 2;
  public static final Integer SandboxPath = 3;
  public static final Integer UCFPath = 4;
  public static final Integer XDCPath = 5;

  protected boolean VendorSoftwarePresent() {
    return VendorSoftware.toolsPresent(
        MyBoardInformation.fpga.getVendor(),
        VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor()));
  }

  protected boolean MapDesign(String CircuitName) {
    final var myfile = MyProject.getLogisimFile();
    final var RootSheet = myfile.getCircuit(CircuitName);
    if (RootSheet == null) {
      Reporter.Report.AddError("INTERNAL ERROR: Circuit not found ?!?");
      return false;
    }
    if (MyBoardInformation == null) {
      Reporter.Report.AddError("INTERNAL ERROR: No board information available ?!?");
      return false;
    }

    var BoardComponents = MyBoardInformation.GetComponents();
    Reporter.Report.AddInfo("The Board " + MyBoardInformation.getBoardName() + " has:");
    for (var key : BoardComponents.keySet()) {
      Reporter.Report.AddInfo(BoardComponents.get(key).size() + " " + key + "(s)");
    }
    /*
     * At this point I require 2 sorts of information: 1) A hierarchical
     * netlist of all the wires that needs to be bubbled up to the toplevel
     * in order to connect the LEDs, Buttons, etc. (hence for the HDL
     * generation). 2) A list with all components that are required to be
     * mapped to PCB components. Identification can be done by a hierarchy
     * name plus component/sub-circuit name
     */
    MyMappableResources = RootSheet.getBoardMap(MyBoardInformation.getBoardName());
    if (MyMappableResources == null) 
      MyMappableResources = new MappableResourcesContainer(MyBoardInformation, RootSheet);
    else
      MyMappableResources.updateMapableComponents();

    return true;
  }

  protected boolean MapDesignCheckIOs() {
    if (MyMappableResources.isCompletelyMapped()) return true;
    var confirm = OptionPane.showConfirmDialog(MyProject.getFrame(), S.get("FpgaNotCompleteMap"), 
        S.get("FpgaIncompleteMap"), OptionPane.YES_NO_OPTION);
    return confirm == OptionPane.YES_OPTION;
  }

  protected boolean performDRC(String CircuitName, String HDLType) {
    final var root = MyProject.getLogisimFile().getCircuit(CircuitName);
    var SheetNames = new ArrayList<String>();
    var DRCResult = Netlist.DRC_PASSED;
    if (root == null) {
      DRCResult |= Netlist.DRC_ERROR;
    } else {
      root.getNetList().clear();
      DRCResult = root.getNetList().DesignRuleCheckResult(true, SheetNames);
    }
    return (DRCResult == Netlist.DRC_PASSED);
  }

  protected String GetProjDir(String selectedCircuit) {
    var ProjectDir = AppPreferences.FPGA_Workspace.get() + File.separator + MyProject.getLogisimFile().getName();
    if (!ProjectDir.endsWith(File.separator)) {
      ProjectDir += File.separator;
    }
    ProjectDir += CorrectLabel.getCorrectLabel(selectedCircuit) + File.separator;
    return ProjectDir;
  }

  protected boolean writeHDL(String selectedCircuit, Double frequency) {
    if (!GenDirectory(
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
    final var ProjectDir = GetProjDir(selectedCircuit);
    final var RootSheet = MyProject.getLogisimFile().getCircuit(selectedCircuit);
    if (!CleanDirectory(ProjectDir)) {
      Reporter.Report.AddFatalError(
          "Unable to cleanup old project files in directory: \"" + ProjectDir + "\"");
      return false;
    }
    if (!GenDirectory(ProjectDir)) {
      Reporter.Report.AddFatalError("Unable to create directory: \"" + ProjectDir + "\"");
      return false;
    }
    for (var hdlPath : HDLPaths) {
      if (!GenDirectory(ProjectDir + hdlPath)) {
        Reporter.Report.AddFatalError(
            "Unable to create directory: \"" + ProjectDir + hdlPath + "\"");
        return false;
      }
    }

    var GeneratedHDLComponents = new HashSet<String>();
    var Worker = RootSheet.getSubcircuitFactory().getHDLGenerator(RootSheet.getStaticAttributes());
    if (Worker == null) {
      Reporter.Report.AddFatalError("Internal error on HDL generation, null pointer exception");
      return false;
    }
    if (!Worker.GenerateAllHDLDescriptions(GeneratedHDLComponents, ProjectDir, null)) {
      return false;
    }
    /* Here we generate the top-level shell */
    if (RootSheet.getNetList().NumberOfClockTrees() > 0) {
      var Ticker = new TickComponentHDLGeneratorFactory(
          MyBoardInformation.fpga.getClockFrequency(),
          frequency /* , boardFreq.isSelected() */);
      if (!AbstractHDLGeneratorFactory.WriteEntity(
          ProjectDir + Ticker.GetRelativeDirectory(),
          Ticker.GetEntity(
              RootSheet.getNetList(),
              null,
              Ticker.getComponentStringIdentifier()),
          Ticker.getComponentStringIdentifier())) {
        return false;
      }
      if (!AbstractHDLGeneratorFactory.WriteArchitecture(
          ProjectDir + Ticker.GetRelativeDirectory(),
          Ticker.GetArchitecture(
              RootSheet.getNetList(),
              null,
              Ticker.getComponentStringIdentifier()),
          Ticker.getComponentStringIdentifier())) {
        return false;
      }

      final var ClockGen = RootSheet.getNetList()
          .GetAllClockSources()
          .get(0)
          .getFactory()
          .getHDLGenerator(RootSheet.getNetList().GetAllClockSources().get(0).getAttributeSet());
      final var CompName = RootSheet.getNetList().GetAllClockSources().get(0).getFactory().getHDLName(null);
      if (!AbstractHDLGeneratorFactory.WriteEntity(
          ProjectDir + ClockGen.GetRelativeDirectory(),
          ClockGen.GetEntity(RootSheet.getNetList(), null, CompName),
          CompName)) {
        return false;
      }
      if (!AbstractHDLGeneratorFactory.WriteArchitecture(
          ProjectDir + ClockGen.GetRelativeDirectory(),
          ClockGen.GetArchitecture(RootSheet.getNetList(), null, CompName),
          CompName)) {
        return false;
      }
    }
    final var Top = new ToplevelHDLGeneratorFactory(MyBoardInformation.fpga.getClockFrequency(), 
        frequency, RootSheet, MyMappableResources);
    if (Top.hasLedArray()) {
      for (var type : LedArrayDriving.Driving_strings) {
        if (Top.hasLedArrayType(type)) {
          Worker = LedArrayGenericHDLGeneratorFactory.getSpecificHDLGenerator(type);
          final var name = LedArrayGenericHDLGeneratorFactory.getSpecificHDLName(type);
          if (Worker != null && name != null) {
            if (!AbstractHDLGeneratorFactory.WriteEntity(
                ProjectDir + Worker.GetRelativeDirectory(),
                Worker.GetEntity(RootSheet.getNetList(), null, name),
                Worker.getComponentStringIdentifier())) {
              return false;
            }
            if (!AbstractHDLGeneratorFactory.WriteArchitecture(
                ProjectDir + Worker.GetRelativeDirectory(),
                Worker.GetArchitecture(RootSheet.getNetList(), null, name),
                Worker.getComponentStringIdentifier())) {
              return false;
            }
          }
        }
      }
    }
    if (!AbstractHDLGeneratorFactory.WriteEntity(
        ProjectDir + Top.GetRelativeDirectory(),
        Top.GetEntity(RootSheet.getNetList(), null, ToplevelHDLGeneratorFactory.FPGAToplevelName),
        Top.getComponentStringIdentifier())) {
      return false;
    }
    return AbstractHDLGeneratorFactory.WriteArchitecture(
        ProjectDir + Top.GetRelativeDirectory(),
        Top.GetArchitecture(RootSheet.getNetList(), null, ToplevelHDLGeneratorFactory.FPGAToplevelName),
        Top.getComponentStringIdentifier());
  }

  protected boolean GenDirectory(String dir) {
    try {
      var Dir = new File(dir);
      if (Dir.exists()) {
        return true;
      }
      return Dir.mkdirs();
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Could not check/create directory :" + dir);
      return false;
    }
  }

  protected void GetVHDLFiles(
      String SourcePath,
      String Path,
      ArrayList<String> Entities,
      ArrayList<String> Behaviors,
      String HDLType) {
    var Dir = new File(Path);
    var Files = Dir.listFiles();
    for (var thisFile : Files) {
      if (thisFile.isDirectory()) {
        if (Path.endsWith(File.separator)) {
          GetVHDLFiles(SourcePath, Path + thisFile.getName(), Entities, Behaviors, HDLType);
        } else {
          GetVHDLFiles(SourcePath, Path + File.separator + thisFile.getName(), Entities, Behaviors, HDLType);
        }
      } else {
        var EntityMask = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? FileWriter.EntityExtension + ".vhd" : ".v";
        var ArchitecturMask = (HDLType.equals(HDLGeneratorFactory.VHDL))
            ? FileWriter.ArchitectureExtension + ".vhd"
            : "#not_searched#";
        if (thisFile.getName().endsWith(EntityMask)) {
          Entities.add((Path + File.separator + thisFile.getName()).replace("\\", "/"));
        } else if (thisFile.getName().endsWith(ArchitecturMask)) {
          Behaviors.add((Path + File.separator + thisFile.getName()).replace("\\", "/"));
        }
      }
    }
  }

  public static String GetDirectoryLocation(String ProjectBase, int Identifier) {
    var Base = (ProjectBase.endsWith(File.separator)) ? ProjectBase : ProjectBase + File.separator;
    if (Identifier >= HDLPaths.length) return null;
    return Base + HDLPaths[Identifier] + File.separator;
  }

  private boolean CleanDirectory(String dir) {
    try {
      final var thisDir = new File(dir);
      if (!thisDir.exists()) {
        return true;
      }
      for (var theFiles : thisDir.listFiles()) {
        if (theFiles.isDirectory()) {
          if (!CleanDirectory(theFiles.getPath())) {
            return false;
          }
        } else {
          if (!theFiles.delete()) {
            return false;
          }
        }
      }
      return thisDir.delete();
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Could not remove directory tree :" + dir);
      return false;
    }
  }
  
  public static HashMap<String, String> getLedArrayMaps(MappableResourcesContainer maps,
      Netlist nets,
      BoardInformation board) {
    var ledArrayMaps = new HashMap<String, String>();
    var hasMappedClockedArray = false;
    for (var comp : maps.getIOComponentInformation().getComponents()) {
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
    if (hasMappedClockedArray && (nets.NumberOfClockTrees() == 0) && !nets.RequiresGlobalClockConnection()) {
      ledArrayMaps.put(TickComponentHDLGeneratorFactory.FPGAClock, board.fpga.getClockPinLocation());
    }
    return ledArrayMaps;
  }
}
