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
import com.cburch.logisim.fpga.data.IoComponentTypes;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.SevenSegmentScanningDriving;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.SynthesizedClockHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.SynthesizedClockHdlGeneratorInstanceFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHdlGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.LedArrayGenericHdlGeneratorFactory;
import com.cburch.logisim.std.io.SevenSegmentScanningGenericHdlGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class DownloadBase {

  protected Project myProject;
  protected BoardInformation myBoardInformation = null;
  protected MappableResourcesContainer myMappableResources;
  protected double preMultiplier = 1.0;
  protected double preDivider = 1.0;
  static final String[] HDLPaths = {
    HdlGeneratorFactory.VERILOG.toLowerCase(),
    HdlGeneratorFactory.VHDL.toLowerCase(),
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

  protected boolean isClockScalingRequested() {
    return !(preDivider == 1.0 && preMultiplier == 1.0);
  }

  public long getSynthesizedFrequency() {
    if (isClockScalingRequested()) {
      return Math.round(myBoardInformation.fpga.getClockFrequency() * preMultiplier / preDivider);
    } else {
      return myBoardInformation.fpga.getClockFrequency();
    }
  }

  protected boolean isVendorSoftwarePresent() {
    return VendorSoftware.toolsPresent(
        myBoardInformation.fpga.getVendor(),
        VendorSoftware.getToolPath(myBoardInformation.fpga.getVendor()));
  }

  protected boolean mapDesign(String circuitName) {
    final var myFile = myProject.getLogisimFile();
    final var rootSheet = myFile.getCircuit(circuitName);
    if (rootSheet == null) {
      Reporter.report.addError("INTERNAL ERROR: Circuit not found ?!?");
      return false;
    }
    if (myBoardInformation == null) {
      Reporter.report.addError("INTERNAL ERROR: No board information available ?!?");
      return false;
    }

    final var boardComponents = myBoardInformation.getComponents();
    Reporter.report.addInfo("The Board " + myBoardInformation.getBoardName() + " has:");
    for (final var key : boardComponents.keySet()) {
      Reporter.report.addInfo(boardComponents.get(key).size() + " " + key + "(s)");
    }
    /*
     * At this point I require 2 sorts of information: 1) A hierarchical
     * netlist of all the wires that needs to be bubbled up to the toplevel
     * in order to connect the LEDs, Buttons, etc. (hence for the HDL
     * generation). 2) A list with all components that are required to be
     * mapped to PCB components. Identification can be done by a hierarchy
     * name plus component/sub-circuit name
     */
    myMappableResources = rootSheet.getBoardMap(myBoardInformation.getBoardName());
    if (myMappableResources == null) {
      myMappableResources = new MappableResourcesContainer(myBoardInformation, rootSheet);
    } else {
      myMappableResources.updateMapableComponents();
    }

    return true;
  }

  protected boolean mapDesignCheckIOs() {
    if (myMappableResources.isCompletelyMapped()) return true;
    final var confirm =
        OptionPane.showConfirmDialog(
            myProject.getFrame(),
            S.get("FpgaNotCompleteMap"),
            S.get("FpgaIncompleteMap"),
            OptionPane.YES_NO_OPTION);
    return confirm == OptionPane.YES_OPTION;
  }

  protected boolean performDrc(String circuitName, String HDLType) {
    final var root = myProject.getLogisimFile().getCircuit(circuitName);
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
    var projectDir =
        AppPreferences.FPGA_Workspace.get() + File.separator + myProject.getLogisimFile().getName();
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
            + myProject.getLogisimFile().getName())) {
      Reporter.report.addFatalError(
          "Unable to create directory: \""
              + AppPreferences.FPGA_Workspace.get()
              + File.separator
              + myProject.getLogisimFile().getName()
              + "\"");
      return false;
    }
    final var projectDir = getProjDir(selectedCircuit);
    final var rootSheet = myProject.getLogisimFile().getCircuit(selectedCircuit);
    if (!cleanDirectory(projectDir)) {
      Reporter.report.addFatalError(
          "Unable to cleanup old project files in directory: \"" + projectDir + "\"");
      return false;
    }
    if (!genDirectory(projectDir)) {
      Reporter.report.addFatalError("Unable to create directory: \"" + projectDir + "\"");
      return false;
    }
    for (final var hdlPath : HDLPaths) {
      if (!genDirectory(projectDir + hdlPath)) {
        Reporter.report.addFatalError(
            "Unable to create directory: \"" + projectDir + hdlPath + "\"");
        return false;
      }
    }

    final var generatedHDLComponents = new HashSet<String>();
    var worker = rootSheet.getSubcircuitFactory().getHDLGenerator(rootSheet.getStaticAttributes());
    if (worker == null) {
      Reporter.report.addFatalError("Internal error on HDL generation, null pointer exception");
      return false;
    }
    if (!worker.generateAllHDLDescriptions(generatedHDLComponents, projectDir, null)) {
      return false;
    }
    // Instantiate the clock synthesizer component
    SynthesizedClockHdlGeneratorFactory synthesizer;
    try {
      synthesizer = SynthesizedClockHdlGeneratorInstanceFactory.getSynthesizedClockHdlGeneratorFactory(
        myBoardInformation.fpga.getTechnology(),
        myBoardInformation.fpga.getVendor(),
        isClockScalingRequested(), 
        myBoardInformation.fpga.getClockFrequency(),
        preMultiplier,
        preDivider);
    } catch (Exception e) {
      Reporter.report.addFatalError(e.getMessage());
      return false;
    }
    /* Here we generate the top-level shell */
    if (rootSheet.getNetList().numberOfClockTrees() > 0) {
      // Write the clock synthesizer component
      if (!Hdl.writeEntity(
          projectDir + synthesizer.getRelativeDirectory(),
          synthesizer.getEntity(
              rootSheet.getNetList(), null, SynthesizedClockHdlGeneratorFactory.HDL_IDENTIFIER),
          SynthesizedClockHdlGeneratorFactory.HDL_IDENTIFIER)) {
        return false;
      }
      if (!Hdl.writeArchitecture(
          projectDir + synthesizer.getRelativeDirectory(),
          synthesizer.getArchitecture(
              rootSheet.getNetList(), null, SynthesizedClockHdlGeneratorFactory.HDL_IDENTIFIER),
          SynthesizedClockHdlGeneratorFactory.HDL_IDENTIFIER)) {
        return false;
      }

      final var ticker =
          new TickComponentHdlGeneratorFactory(
              getSynthesizedFrequency(),
              frequency /* , boardFreq.isSelected() */);
      if (!Hdl.writeEntity(
          projectDir + ticker.getRelativeDirectory(),
          ticker.getEntity(
              rootSheet.getNetList(), null, TickComponentHdlGeneratorFactory.HDL_IDENTIFIER),
          TickComponentHdlGeneratorFactory.HDL_IDENTIFIER)) {
        return false;
      }
      if (!Hdl.writeArchitecture(
          projectDir + ticker.getRelativeDirectory(),
          ticker.getArchitecture(
              rootSheet.getNetList(), null, TickComponentHdlGeneratorFactory.HDL_IDENTIFIER),
          TickComponentHdlGeneratorFactory.HDL_IDENTIFIER)) {
        return false;
      }

      final var clockGen =
          rootSheet
              .getNetList()
              .getAllClockSources()
              .get(0)
              .getFactory()
              .getHDLGenerator(
                  rootSheet.getNetList().getAllClockSources().get(0).getAttributeSet());
      final var compName =
          rootSheet.getNetList().getAllClockSources().get(0).getFactory().getHDLName(null);
      if (!Hdl.writeEntity(
          projectDir + clockGen.getRelativeDirectory(),
          clockGen.getEntity(rootSheet.getNetList(), null, compName),
          compName)) {
        return false;
      }
      if (!Hdl.writeArchitecture(
          projectDir + clockGen.getRelativeDirectory(),
          clockGen.getArchitecture(rootSheet.getNetList(), null, compName),
          compName)) {
        return false;
      }
    }
    final var top =
        new ToplevelHdlGeneratorFactory(
            getSynthesizedFrequency(), frequency, rootSheet, myMappableResources, synthesizer);
    if (top.hasLedArray()) {
      for (var type : LedArrayDriving.DRIVING_STRINGS) {
        if (top.hasLedArrayType(type)) {
          worker = LedArrayGenericHdlGeneratorFactory.getSpecificHDLGenerator(type);
          final var name = LedArrayGenericHdlGeneratorFactory.getSpecificHDLName(type);
          if (worker != null && name != null) {
            if (!Hdl.writeEntity(
                projectDir + worker.getRelativeDirectory(),
                worker.getEntity(rootSheet.getNetList(), null, name),
                name)) {
              return false;
            }
            if (!Hdl.writeArchitecture(
                projectDir + worker.getRelativeDirectory(),
                worker.getArchitecture(rootSheet.getNetList(), null, name),
                name)) {
              return false;
            }
          }
        }
      }
    }
    if (top.hasScanningSevenSeg()) {
      for (var type : SevenSegmentScanningDriving.DRIVING_STRINGS) {
        if (top.hasScanningSevenSegmentType(type)) {
          worker = SevenSegmentScanningGenericHdlGenerator.getSpecificHDLGenerator(type);
          final var name = SevenSegmentScanningGenericHdlGenerator.getSpecificHDLName(type);
          if (worker != null && name != null) {
            if (!Hdl.writeEntity(
                projectDir + worker.getRelativeDirectory(), 
                worker.getEntity(rootSheet.getNetList(), null, name),
                name)) {
              return false;
            }
            if (!Hdl.writeArchitecture(
                projectDir + worker.getRelativeDirectory(),
                worker.getArchitecture(rootSheet.getNetList(), null, name), 
                name)) {
              return false;
            }
          }
        }
      }
    }
    if (!Hdl.writeEntity(
        projectDir + top.getRelativeDirectory(),
        top.getEntity(
            rootSheet.getNetList(), null, ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME),
        ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME)) {
      return false;
    }
    return Hdl.writeArchitecture(
        projectDir + top.getRelativeDirectory(),
        top.getArchitecture(
            rootSheet.getNetList(), null, ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME),
        ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME);
  }

  protected boolean genDirectory(String dirPath) {
    try {
      var dir = new File(dirPath);
      return dir.exists() ? true : dir.mkdirs();
    } catch (Exception e) {
      Reporter.report.addFatalError("Could not check/create directory :" + dirPath);
      return false;
    }
  }

  protected void getVhdlFiles(
      String sourcePath,
      String path,
      ArrayList<String> entities,
      ArrayList<String> behaviors,
      String type) {
    final var dir = new File(path);
    final var files = dir.listFiles();
    for (final var thisFile : files) {
      if (thisFile.isDirectory()) {
        if (path.endsWith(File.separator)) {
          getVhdlFiles(sourcePath, path + thisFile.getName(), entities, behaviors, type);
        } else {
          getVhdlFiles(
              sourcePath, path + File.separator + thisFile.getName(), entities, behaviors, type);
        }
      } else {
        final var entityMask =
            (type.equals(HdlGeneratorFactory.VHDL)) ? FileWriter.ENTITY_EXTENSION + ".vhd" : ".v";
        final var architectureMask =
            (type.equals(HdlGeneratorFactory.VHDL))
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
    final var base =
        (projectBase.endsWith(File.separator)) ? projectBase : projectBase + File.separator;
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
      Reporter.report.addFatalError("Could not remove directory tree :" + dir);
      return false;
    }
  }

  public static Map<String, String> getScanningMaps(
      MappableResourcesContainer maps, Netlist nets, BoardInformation board) {
    final var pinMaps = new HashMap<String, String>();
    var hasMappedClockedArray = false;
    var hasScanningSevenSegment = false;
    for (final var comp : maps.getIoComponentInformation().getComponents()) {
      if (comp.getType().equals(IoComponentTypes.LedArray)) {
        if (comp.hasMap()) {
          hasMappedClockedArray |=
              LedArrayGenericHdlGeneratorFactory.requiresClock(comp.getArrayDriveMode());
          for (var pin = 0; pin < comp.getExternalPinCount(); pin++) {
            pinMaps.put(
                LedArrayGenericHdlGeneratorFactory.getExternalSignalName(
                    comp.getArrayDriveMode(),
                    comp.getNrOfRows(),
                    comp.getNrOfColumns(),
                    comp.getArrayId(),
                    pin),
                comp.getPinLocation(pin));
          }
        }
      }
      if (comp.getType().equals(IoComponentTypes.SevenSegmentScanning)) {
        if (comp.hasMap()) {
          hasScanningSevenSegment = true;
          for (var pin = 0; pin < comp.getExternalPinCount(); pin++) {
            pinMaps.put(
                SevenSegmentScanningGenericHdlGenerator.getExternalSignalName(
                    comp.getNrOfRows(), 
                    comp.getArrayId(), 
                    pin), 
                comp.getPinLocation(pin));
          }
        }
      }
    }
    if ((hasMappedClockedArray || hasScanningSevenSegment)
        && (nets.numberOfClockTrees() == 0)
        && !nets.requiresGlobalClockConnection()) {
      pinMaps.put(
          TickComponentHdlGeneratorFactory.FPGA_CLOCK, board.fpga.getClockPinLocation());
    }
    return pinMaps;
  }
  
}
