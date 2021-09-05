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
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AlteraDownload implements VendorDownload {

  private final VendorSoftware alteraVendor =
      VendorSoftware.getSoftware(VendorSoftware.VENDOR_ALTERA);
  private final String ScriptPath;
  private final String ProjectPath;
  private final String SandboxPath;
  private final Netlist RootNetList;
  private MappableResourcesContainer mapInfo;
  private final BoardInformation boardInfo;
  private final ArrayList<String> entities;
  private final ArrayList<String> architectures;
  private final String HDLType;
  private String cablename;
  private final boolean WriteToFlash;

  private static final String alteraTclFile = "AlteraDownload.tcl";
  private static final String AlteraCofFile = "AlteraFlash.cof";

  public AlteraDownload(
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
    this.RootNetList = RootNetList;
    this.boardInfo = BoardInfo;
    this.entities = Entities;
    this.architectures = Architectures;
    this.HDLType = HDLType;
    this.WriteToFlash = WriteToFlash;
    cablename = "";
  }

  @Override
  public void SetMapableResources(MappableResourcesContainer resources) {
    mapInfo = resources;
  }

  @Override
  public int GetNumberOfStages() {
    return 3;
  }

  @Override
  public String GetStageMessage(int stage) {
    switch (stage) {
      case 0:
        return S.get("AlteraProject");
      case 1:
        return S.get("AlteraOptimize");
      case 2:
        return S.get("AlteraSyntPRBit");
      default:
        return "Unknown, bizar";
    }
  }

  @Override
  public ProcessBuilder PerformStep(int stage) {
    switch (stage) {
      case 0:
        return Stage0Project();
      case 1:
        return Stage1Optimize();
      case 2:
        return Stage2SPRBit();
      default:
        return null;
    }
  }

  @Override
  public boolean readyForDownload() {
    final var SofFile = new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists();
    final var PofFile = new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".pof").exists();
    return SofFile || PofFile;
  }

  @Override
  public ProcessBuilder DownloadToBoard() {
    if (WriteToFlash) {
      if (!DoFlashing()) return null;
    }
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("-c");
    command.add(cablename);
    command.add("-m");
    command.add("jtag");
    command.add("-o");
    // if there is no .sof generated, try with the .pof
    if (new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists()) {
      command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof"
                  + "@" + boardInfo.fpga.getFpgaJTAGChainPosition());
    } else {
      command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".pof"
                  + "@" + boardInfo.fpga.getFpgaJTAGChainPosition());
    }
    var Down = new ProcessBuilder(command);
    Down.directory(new File(SandboxPath));
    return Down;
  }

  private ProcessBuilder Stage0Project() {
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(0));
    command.add("-t");
    command.add(ScriptPath.replace(ProjectPath, ".." + File.separator) + alteraTclFile);
    final var stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    return stage0;
  }

  private ProcessBuilder Stage1Optimize() {
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(2));
    command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
    command.add("--optimize=area");
    final var stage1 = new ProcessBuilder(command);
    stage1.directory(new File(SandboxPath));
    return stage1;
  }

  private ProcessBuilder Stage2SPRBit() {
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(0));
    command.add("--flow");
    command.add("compile");
    command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
    final var stage2 = new ProcessBuilder(command);
    stage2.directory(new File(SandboxPath));
    return stage2;
  }

  @Override
  public boolean CreateDownloadScripts() {
    var scriptFile = FileWriter.GetFilePointer(ScriptPath, alteraTclFile);
    if (scriptFile == null) {
      scriptFile = new File(ScriptPath + alteraTclFile);
      return scriptFile.exists();
    }
    final var fileType = HDLType.equals(HDLGeneratorFactory.VHDL) ? "VHDL_FILE" : "VERILOG_FILE";
    final var contents = new LineBuffer();
    contents
        .pair("topLevelName", ToplevelHDLGeneratorFactory.FPGAToplevelName)
        .pair("fileType", fileType)
        .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);

    contents
        .addLines(
            "# Load Quartus II Tcl Project package",
            "package require ::quartus::project",
            "",
            "set need_to_close_project 0",
            "set make_assignments 1",
            "",
            "# Check that the right project is open",
            "if {[is_project_open]} {",
            "    if {[string compare $quartus(project) \"{{topLevelName}}\"]} {",
            "        puts \"Project {{topLevelName}} is not open\"",
            "        set make_assignments 0",
            "    }",
            "} else {",
            "    # Only open if not already open",
            "    if {[project_exists {{topLevelName}}]} {",
            "        project_open -revision {{topLevelName}} {{topLevelName}}",
            "    } else {",
            "        project_new -revision {{topLevelName}} {{topLevelName}}",
            "    }",
            "    set need_to_close_project 1",
            "}",
            "# Make assignments",
            "if {$make_assignments} {")
        .add(getAlteraAssignments(boardInfo))
        .addLines(
            "",
            "    # Include all entities and gates",
            "");
    for (var entity : entities) {
      contents.add("    set_global_assignment -name {{fileType}} \"{{1}}\"", entity);
    }
    for (var architecture : architectures) {
      contents.add("    set_global_assignment -name {{fileType}} \"{{1}}\"", architecture);
    }
    contents.add("");
    contents.add("    # Map fpga_clk and ionets to fpga pins");
    if (RootNetList.numberOfClockTrees() > 0 || RootNetList.requiresGlobalClockConnection()) {
      contents.add("    set_location_assignment {{1}} -to {{clock}}", boardInfo.fpga.getClockPinLocation());
    }
    contents
        .add(getPinLocStrings())
        .addLines(
            "    # Commit assignments",
            "    export_assignments",
            "",
            "    # Close project",
            "    if {$need_to_close_project} {",
            "        project_close",
            "    }",
            "}");
    return FileWriter.WriteContents(scriptFile, contents.get());
  }

  private ArrayList<String> getPinLocStrings() {
    final var contents = new LineBuffer();

    for (final var key : mapInfo.getMappableResources().keySet()) {
      final var map = mapInfo.getMappableResources().get(key);

      for (var i = 0; i < map.getNrOfPins(); i++) {
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i) && !map.isInternalMapped(i)) {
          final var pairs = new LineBuffer.Pairs()
                  .add("pinLoc", map.getPinLocation(i))
                  .add("inv", map.isExternalInverted(i) ? "n_" : "")
                  .add("hdlStr", map.getHdlString(i));
          contents.add("set_location_assignment {{pinLoc}} -to {{inv}}{{hdlStr}}", pairs);
          if (map.requiresPullup(i))
            contents.add("set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to {{inv}}{{hdlStr}}", pairs);
        }
      }
    }
    final var ledArrayMap = DownloadBase.getLedArrayMaps(mapInfo, RootNetList, boardInfo);
    for (final var key : ledArrayMap.keySet())
      contents.add("set_location_assignment {{1}} -to {{2}}", ledArrayMap.get(key), key);
    return contents.getWithIndent(4);
  }

  private static ArrayList<String> getAlteraAssignments(BoardInformation currentBoard) {
    final var pkg = currentBoard.fpga.getPackage().split(" ");
    final var currentBehavior = currentBoard.fpga.getUnusedPinsBehavior();
    final var behavior = switch (currentBehavior) {
      case PullBehaviors.PULL_UP -> "PULLUP";
      case PullBehaviors.PULL_DOWN -> "PULLDOWN";
      case PullBehaviors.FLOAT -> "TRI-STATED";
      default -> throw new IllegalStateException("Unexpected value: " + currentBehavior);
    };

    return (new LineBuffer())
        .pair("assignName", "set_global_assignment -name")
        .add("{{assignName}} FAMILY \"{{1}}\"", currentBoard.fpga.getTechnology())
        .add("{{assignName}} DEVICE {{1}}", currentBoard.fpga.getPart())
        .add("{{assignName}} DEVICE_FILTER_PACKAGE {{1}}", pkg[0])
        .add("{{assignName}} DEVICE_FILTER_PIN_COUNT {{1}}", pkg[1])
        .add("{{assignName}} RESERVE_ALL_UNUSED_PINS \"AS INPUT {{1}}\"", behavior)
        .add("{{assignName}} FMAX_REQUIREMENT \"{{1}}\"", Download.GetClockFrequencyString(currentBoard))
        .add("{{assignName}} RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"")
        .add("{{assignName}} CYCLONEII_RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"")
        .getWithIndent();
  }

  @Override
  public boolean BoardConnected() {
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("--list");
    final var Detect = new ProcessBuilder(command);
    Detect.directory(new File(SandboxPath));
    var response = new ArrayList<String>();
    try {
      Reporter.Report.print("");
      Reporter.Report.print("===");
      Reporter.Report.print("===> " + S.get("AlteraDetectDevice"));
      Reporter.Report.print("===");
      if (Download.execute(Detect, response) != null) return false;
    } catch (IOException | InterruptedException e) {
      return false;
    }
    var Devices = Devices(response);
    if (Devices == null) return false;
    if (Devices.size() == 1) {
      cablename = Devices.get(0);
      return true;
    }
    var selection = Download.ChooseBoard(Devices);
    if (selection == null) return false;
    cablename = selection;
    return true;
  }

  private ArrayList<String> Devices(ArrayList<String> lines) {
    /* This code originates from Kevin Walsh */
    var dev = new ArrayList<String>();
    for (var line : lines) {
      var n = dev.size() + 1;
      if (!line.matches("^" + n + "\\) .*")) continue;
      line = line.replaceAll("^" + n + "\\) ", "");
      dev.add(line.trim());
    }
    if (dev.size() == 0) return null;
    return dev;
  }

  private boolean DoFlashing() {
    if (!CreateCofFile()) {
      Reporter.Report.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!CreateJicFile()) {
      Reporter.Report.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!LoadProgrammerSof()) {
      Reporter.Report.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!FlashDevice()) {
      Reporter.Report.AddError(S.get("AlteraFlashError"));
      return false;
    }
    return true;
  }

  private boolean FlashDevice() {
    final var jicFile = ToplevelHDLGeneratorFactory.FPGAToplevelName + ".jic";
    Reporter.Report.print("==>");
    Reporter.Report.print("==> " + S.get("AlteraFlash"));
    Reporter.Report.print("==>");
    if (!new File(SandboxPath + jicFile).exists()) {
      Reporter.Report.AddError(S.get("AlteraFlashError", jicFile));
      return false;
    }
    final var command = new LineBuffer();
    command
        .add(alteraVendor.getBinaryPath(1))
        .add("-c")
        .add(cablename)
        .add("-m")
        .add("jtag")
        .add("-o")
        .add("P;{{1}}", jicFile);
    final var prog = new ProcessBuilder(command.get());
    prog.directory(new File(SandboxPath));
    try {
      final var result = Download.execute(prog, null);
      if (result != null) {
        Reporter.Report.AddFatalError(S.get("AlteraFlashFailure"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.Report.AddFatalError(S.get("AlteraFlashFailure"));
      return false;
    }
    return true;
  }

  private boolean LoadProgrammerSof() {
    final var FpgaDevice = StripPackageSpeed();
    final var ProgrammerSofFile = new File(VendorSoftware.GetToolPath(VendorSoftware.VENDOR_ALTERA)).getParent()
        + File.separator
        + "common"
        + File.separator
        + "devinfo"
        + File.separator
        + "programmer"
        + File.separator
        + "sfl_"
        + FpgaDevice.toLowerCase()
        + ".sof";
    Reporter.Report.print("==>");
    Reporter.Report.print("==> " + S.get("AlteraProgSof"));
    Reporter.Report.print("==>");
    if (!new File(ProgrammerSofFile).exists()) {
      Reporter.Report.AddError(S.get("AlteraProgSofError", ProgrammerSofFile));
      return false;
    }
    final var command = new LineBuffer();
    command.add(alteraVendor.getBinaryPath(1))
            .add("-c")
            .add(cablename)
            .add("-m")
            .add("jtag")
            .add("-o")
            .add("P;{{1}}", ProgrammerSofFile);
    final var prog = new ProcessBuilder(command.get());
    prog.directory(new File(SandboxPath));
    try {
      final var result = Download.execute(prog, null);
      if (result != null) {
        Reporter.Report.AddFatalError(S.get("AlteraProgSofFailure"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.Report.AddFatalError(S.get("AlteraProgSofFailure"));
      return false;
    }
    return true;
  }

  private String StripPackageSpeed() {
    /* For the Cyclone IV devices the name used for Syntesis is in form
     * EP4CE15F23C8. For the programmer sof-file (for flash writing) we need to strip
     * the part F23C8. For future supported devices this should be checked.
     */
    final var FpgaDevice = boardInfo.fpga.getPart();
    final var index = FpgaDevice.indexOf("F");
    return FpgaDevice.substring(0, index);
  }

  private boolean CreateJicFile() {
    if (!new File(ScriptPath + AlteraCofFile).exists()) {
      Reporter.Report.AddError(S.get("AlteraNoCof"));
      return false;
    }
    Reporter.Report.print("==>");
    Reporter.Report.print("==> " + S.get("AlteraJicFile"));
    Reporter.Report.print("==>");
    var command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(3));
    command.add("-c");
    command.add((ScriptPath + AlteraCofFile).replace(ProjectPath, "../"));
    final var Jic = new ProcessBuilder(command);
    Jic.directory(new File(SandboxPath));
    try {
      final var result = Download.execute(Jic, null);
      if (result != null) {
        Reporter.Report.AddFatalError(S.get("AlteraJicFileError"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.Report.AddFatalError(S.get("AlteraJicFileError"));
      return false;
    }

    return true;
  }

  private boolean CreateCofFile() {
    if (!new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists()) {
      Reporter.Report.AddFatalError(S.get("AlteraNoSofFile"));
      return false;
    }
    Reporter.Report.print("==>");
    Reporter.Report.print("==> " + S.get("AlteraCofFile"));
    Reporter.Report.print("==>");
    try {
      var docFactory = DocumentBuilderFactory.newInstance();
      var docBuilder = docFactory.newDocumentBuilder();
      var CofFile = docBuilder.newDocument();
      CofFile.setXmlStandalone(true);
      var rootElement = CofFile.createElement("cof");
      CofFile.appendChild(rootElement);
      AddElement("eprom_name", boardInfo.fpga.getFlashName(), rootElement, CofFile);
      AddElement("flash_loader_device", StripPackageSpeed(), rootElement, CofFile);
      AddElement("output_filename",
          SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".jic",
          rootElement,
          CofFile);
      AddElement("n_pages", "1", rootElement, CofFile);
      AddElement("width", "1", rootElement, CofFile);
      AddElement("mode", "7", rootElement, CofFile);
      Element SofData = CofFile.createElement("sof_data");
      rootElement.appendChild(SofData);
      AddElement("user_name", "Page_0", SofData, CofFile);
      AddElement("page_flags", "1", SofData, CofFile);
      var BitFile = CofFile.createElement("bit0");
      SofData.appendChild(BitFile);
      AddElement("sof_filename",
          SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof",
          BitFile,
          CofFile);
      AddElement("version", "10", rootElement, CofFile);
      AddElement("create_cvp_file", "0", rootElement, CofFile);
      AddElement("create_hps_iocsr", "0", rootElement, CofFile);
      AddElement("auto_create_rpd", "0", rootElement, CofFile);
      AddElement("rpd_little_endian", "1", rootElement, CofFile);
      var Options = CofFile.createElement("options");
      rootElement.appendChild(Options);
      AddElement("map_file", "0", Options, CofFile);
      var AdvancedOptions = CofFile.createElement("advanced_options");
      rootElement.appendChild(AdvancedOptions);
      AddElement("ignore_epcs_id_check", "2", AdvancedOptions, CofFile);
      AddElement("ignore_condone_check", "2", AdvancedOptions, CofFile);
      AddElement("plc_adjustment", "0", AdvancedOptions, CofFile);
      AddElement("post_chain_bitstream_pad_bytes", "-1", AdvancedOptions, CofFile);
      AddElement("post_device_bitstream_pad_bytes", "-1", AdvancedOptions, CofFile);
      AddElement("bitslice_pre_padding", "1", AdvancedOptions, CofFile);
      var transformerfac = TransformerFactory.newInstance();
      var transformer = transformerfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.ENCODING, "US-ASCII");
      transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
      var source = new DOMSource(CofFile);
      var result = new StreamResult(new File(ScriptPath + AlteraCofFile));
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      Reporter.Report.AddError(S.get("AlteraErrorCof"));
      return false;
    }
    return true;
  }

  private void AddElement(String ElementName, String ElementValue, Element root, Document doc) {
    var NamedElement = doc.createElement(ElementName);
    NamedElement.appendChild(doc.createTextNode(ElementValue));
    root.appendChild(NamedElement);
  }

}
