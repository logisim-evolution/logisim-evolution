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
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AlteraDownload implements VendorDownload {

  private VendorSoftware alteraVendor = VendorSoftware.getSoftware(VendorSoftware.VendorAltera);
  private String ScriptPath;
  private String ProjectPath;
  private String SandboxPath;
  private FPGAReport Reporter;
  private Netlist RootNetList;
  private MappableResourcesContainer MapInfo;
  private BoardInformation BoardInfo;
  private ArrayList<String> Entities;
  private ArrayList<String> Architectures;
  private String HDLType;
  private String cablename;
  private boolean WriteToFlash;

  private static String AlteraTclFile = "AlteraDownload.tcl";
  private static String AlteraCofFile = "AlteraFlash.cof";

  public AlteraDownload(
      String ProjectPath,
      FPGAReport Reporter,
      Netlist RootNetList,
      BoardInformation BoardInfo,
      ArrayList<String> Entities,
      ArrayList<String> Architectures,
      String HDLType,
      boolean WriteToFlash) {
    this.ProjectPath = ProjectPath;
    this.SandboxPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.SandboxPath);
    this.ScriptPath = DownloadBase.GetDirectoryLocation(ProjectPath, DownloadBase.ScriptPath);
    this.Reporter = Reporter;
    this.RootNetList = RootNetList;
    this.BoardInfo = BoardInfo;
    this.Entities = Entities;
    this.Architectures = Architectures;
    this.HDLType = HDLType;
    this.WriteToFlash = WriteToFlash;
    cablename = "";
  }

  public void SetMapableResources(MappableResourcesContainer resources) {
    MapInfo = resources;
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
    boolean SofFile =
        new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists();
    boolean PofFile =
        new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".pof").exists();
    return SofFile | PofFile;
  }

  @Override
  public ProcessBuilder DownloadToBoard() {
    if (WriteToFlash) {
      if (!DoFlashing()) return null;
    }
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("-c");
    command.add(cablename);
    command.add("-m");
    command.add("jtag");
    command.add("-o");
    // if there is no .sof generated, try with the .pof
    if (new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists()) {
      command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof"+
                  "@"+BoardInfo.fpga.getFpgaJTAGChainPosition());
    } else {
      command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".pof"+
                  "@"+BoardInfo.fpga.getFpgaJTAGChainPosition());
    }
    ProcessBuilder Down = new ProcessBuilder(command);
    Down.directory(new File(SandboxPath));
    return Down;
  }

  private ProcessBuilder Stage0Project() {
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(0));
    command.add("-t");
    command.add(ScriptPath.replace(ProjectPath, ".." + File.separator) + AlteraTclFile);
    ProcessBuilder stage0 = new ProcessBuilder(command);
    stage0.directory(new File(SandboxPath));
    System.out.println(command);
    return stage0;
  }

  private ProcessBuilder Stage1Optimize() {
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(2));
    command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
    command.add("--optimize=area");
    ProcessBuilder stage1 = new ProcessBuilder(command);
    stage1.directory(new File(SandboxPath));
    return stage1;
  }

  private ProcessBuilder Stage2SPRBit() {
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(0));
    command.add("--flow");
    command.add("compile");
    command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
    ProcessBuilder stage2 = new ProcessBuilder(command);
    stage2.directory(new File(SandboxPath));
    return stage2;
  }

  @Override
  public boolean CreateDownloadScripts() {
    File ScriptFile = FileWriter.GetFilePointer(ScriptPath, AlteraTclFile, Reporter);
    if (ScriptFile == null) {
      ScriptFile = new File(ScriptPath + AlteraTclFile);
      return ScriptFile.exists();
    }
    String FileType = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? "VHDL_FILE" : "VERILOG_FILE";
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("# Load Quartus II Tcl Project package");
    Contents.add("package require ::quartus::project");
    Contents.add("");
    Contents.add("set need_to_close_project 0");
    Contents.add("set make_assignments 1");
    Contents.add("");
    Contents.add("# Check that the right project is open");
    Contents.add("if {[is_project_open]} {");
    Contents.add(
        "    if {[string compare $quartus(project) \""
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + "\"]} {");
    Contents.add(
        "        puts \"Project "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + " is not open\"");
    Contents.add("        set make_assignments 0");
    Contents.add("    }");
    Contents.add("} else {");
    Contents.add("    # Only open if not already open");
    Contents.add(
        "    if {[project_exists " + ToplevelHDLGeneratorFactory.FPGAToplevelName + "]} {");
    Contents.add(
        "        project_open -revision "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + " "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName);
    Contents.add("    } else {");
    Contents.add(
        "        project_new -revision "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName
            + " "
            + ToplevelHDLGeneratorFactory.FPGAToplevelName);
    Contents.add("    }");
    Contents.add("    set need_to_close_project 1");
    Contents.add("}");
    Contents.add("# Make assignments");
    Contents.add("if {$make_assignments} {");
    Contents.addAll(GetAlteraAssignments(BoardInfo));
    Contents.add("");
    Contents.add("    # Include all entities and gates");
    Contents.add("");
    for (int i = 0; i < Entities.size(); i++) {
      Contents.add("    set_global_assignment -name " + FileType + " \"" + Entities.get(i) + "\"");
    }
    for (int i = 0; i < Architectures.size(); i++) {
      Contents.add(
          "    set_global_assignment -name " + FileType + " \"" + Architectures.get(i) + "\"");
    }
    Contents.add("");
    Contents.add("    # Map fpga_clk and ionets to fpga pins");
    if (RootNetList.NumberOfClockTrees() > 0) {
      Contents.add(
          "    set_location_assignment "
              + BoardInfo.fpga.getClockPinLocation()
              + " -to "
              + TickComponentHDLGeneratorFactory.FPGAClock);
    }
    Contents.addAll(GetPinLocStrings());
    Contents.add("    # Commit assignments");
    Contents.add("    export_assignments");
    Contents.add("");
    Contents.add("    # Close project");
    Contents.add("    if {$need_to_close_project} {");
    Contents.add("        project_close");
    Contents.add("    }");
    Contents.add("}");
    return FileWriter.WriteContents(ScriptFile, Contents, Reporter);
  }
  
  private ArrayList<String> GetPinLocStrings() {
    ArrayList<String> Contents = new ArrayList<String>();
    StringBuffer Temp = new StringBuffer();
    for (ArrayList<String> key : MapInfo.getMappableResources().keySet()) {
      MapComponent map = MapInfo.getMappableResources().get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        Temp.setLength(0);
        Temp.append("    set_location_assignment ");
        if (map.isMapped(i) && !map.IsOpenMapped(i) && !map.IsConstantMapped(i)) {
          Temp.append(map.getPinLocation(i)+" -to ");
          if (map.isExternalInverted(i)) Temp.append("n_");
          Temp.append(map.getHdlString(i));
          Contents.add(Temp.toString());
          if (map.requiresPullup(i)) {
            Temp.setLength(0);
            Temp.append("    set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to ");
            if (map.isExternalInverted(i)) Temp.append("n_");
            Temp.append(map.getHdlString(i));
            Contents.add(Temp.toString());
          }
        }
      }
    }
    return Contents;
  }

  private static ArrayList<String> GetAlteraAssignments(BoardInformation CurrentBoard) {
    ArrayList<String> result = new ArrayList<String>();
    String Assignment = "    set_global_assignment -name ";
    result.add(Assignment + "FAMILY \"" + CurrentBoard.fpga.getTechnology() + "\"");
    result.add(Assignment + "DEVICE " + CurrentBoard.fpga.getPart());
    String[] Package = CurrentBoard.fpga.getPackage().split(" ");
    result.add(Assignment + "DEVICE_FILTER_PACKAGE " + Package[0]);
    result.add(Assignment + "DEVICE_FILTER_PIN_COUNT " + Package[1]);
    if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.Float) {
      result.add(Assignment + "RESERVE_ALL_UNUSED_PINS \"AS INPUT TRI-STATED\"");
    }
    if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
      result.add(Assignment + "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLUP\"");
    }
    if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
      result.add(Assignment + "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLDOWN\"");
    }
    result.add(
        Assignment + "FMAX_REQUIREMENT \"" + Download.GetClockFrequencyString(CurrentBoard) + "\"");
    result.add(Assignment + "RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
    result.add(Assignment + "CYCLONEII_RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
    return result;
  }

  @Override
  public boolean BoardConnected() {
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("--list");
    ProcessBuilder Detect = new ProcessBuilder(command);
    Detect.directory(new File(SandboxPath));
    ArrayList<String> response = new ArrayList<String>();
    try {
      Reporter.print("");
      Reporter.print("===");
      Reporter.print("===> " + S.get("AlteraDetectDevice"));
      Reporter.print("===");
      if (Download.execute(Detect, response, Reporter) != null) return false;
    } catch (IOException | InterruptedException e) {
      return false;
    }
    ArrayList<String> Devices = Devices(response);
    if (Devices == null) return false;
    if (Devices.size() == 1) {
      cablename = Devices.get(0);
      return true;
    }
    String selection = Download.ChooseBoard(Devices);
    if (selection == null) return false;
    cablename = selection;
    return true;
  }

  private ArrayList<String> Devices(ArrayList<String> lines) {
    /* This code originates from Kevin Walsh */
    ArrayList<String> dev = new ArrayList<String>();
    for (String line : lines) {
      int n = dev.size() + 1;
      if (!line.matches("^" + n + "\\) .*")) continue;
      line = line.replaceAll("^" + n + "\\) ", "");
      dev.add(line.trim());
    }
    if (dev.size() == 0) return null;
    return dev;
  }

  private boolean DoFlashing() {
    if (!CreateCofFile()) {
      Reporter.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!CreateJicFile()) {
      Reporter.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!LoadProgrammerSof()) {
      Reporter.AddError(S.get("AlteraFlashError"));
      return false;
    }
    if (!FlashDevice()) {
      Reporter.AddError(S.get("AlteraFlashError"));
      return false;
    }
    return true;
  }

  private boolean FlashDevice() {
    String JicFile = ToplevelHDLGeneratorFactory.FPGAToplevelName + ".jic";
    Reporter.print("==>");
    Reporter.print("==> " + S.get("AlteraFlash"));
    Reporter.print("==>");
    if (!new File(SandboxPath + JicFile).exists()) {
      Reporter.AddError(S.fmt("AlteraFlashError", JicFile));
      return false;
    }
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("-c");
    command.add(cablename);
    command.add("-m");
    command.add("jtag");
    command.add("-o");
    command.add("P;" + JicFile);
    ProcessBuilder Prog = new ProcessBuilder(command);
    Prog.directory(new File(SandboxPath));
    try {
      String result = Download.execute(Prog, null, Reporter);
      if (result != null) {
        Reporter.AddFatalError(S.get("AlteraFlashFailure"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.AddFatalError(S.get("AlteraFlashFailure"));
      return false;
    }
    return true;
  }

  private boolean LoadProgrammerSof() {
    String FpgaDevice = StripPackageSpeed();
    String ProgrammerSofFile =
        new File(VendorSoftware.GetToolPath(VendorSoftware.VendorAltera)).getParent()
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
    Reporter.print("==>");
    Reporter.print("==> " + S.get("AlteraProgSof"));
    Reporter.print("==>");
    if (!new File(ProgrammerSofFile).exists()) {
      Reporter.AddError(S.fmt("AlteraProgSofError", ProgrammerSofFile));
      return false;
    }
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(1));
    command.add("-c");
    command.add(cablename);
    command.add("-m");
    command.add("jtag");
    command.add("-o");
    command.add("P;" + ProgrammerSofFile);
    ProcessBuilder Prog = new ProcessBuilder(command);
    Prog.directory(new File(SandboxPath));
    try {
      String result = Download.execute(Prog, null, Reporter);
      if (result != null) {
        Reporter.AddFatalError(S.get("AlteraProgSofFailure"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.AddFatalError(S.get("AlteraProgSofFailure"));
      return false;
    }
    return true;
  }

  private String StripPackageSpeed() {
    /* For the Cyclone IV devices the name used for Syntesis is in form
     * EP4CE15F23C8. For the programmer sof-file (for flash writing) we need to strip the part F23C8.
     * For future supported devices this should be checked.
     */
    String FpgaDevice = BoardInfo.fpga.getPart();
    int index = FpgaDevice.indexOf("F");
    return FpgaDevice.substring(0, index);
  }

  private boolean CreateJicFile() {
    if (!new File(ScriptPath + AlteraCofFile).exists()) {
      Reporter.AddError(S.get("AlteraNoCof"));
      return false;
    }
    Reporter.print("==>");
    Reporter.print("==> " + S.get("AlteraJicFile"));
    Reporter.print("==>");
    List<String> command = new ArrayList<String>();
    command.add(alteraVendor.getBinaryPath(3));
    command.add("-c");
    command.add((ScriptPath + AlteraCofFile).replace(ProjectPath, "../"));
    ProcessBuilder Jic = new ProcessBuilder(command);
    Jic.directory(new File(SandboxPath));
    try {
      String result = Download.execute(Jic, null, Reporter);
      if (result != null) {
        Reporter.AddFatalError(S.get("AlteraJicFileError"));
        return false;
      }
    } catch (IOException | InterruptedException e) {
      Reporter.AddFatalError(S.get("AlteraJicFileError"));
      return false;
    }

    return true;
  }

  private boolean CreateCofFile() {
    if (!new File(SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof").exists()) {
      Reporter.AddFatalError(S.get("AlteraNoSofFile"));
      return false;
    }
    Reporter.print("==>");
    Reporter.print("==> " + S.get("AlteraCofFile"));
    Reporter.print("==>");
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document CofFile = docBuilder.newDocument();
      CofFile.setXmlStandalone(true);
      Element rootElement = CofFile.createElement("cof");
      CofFile.appendChild(rootElement);
      AddElement("eprom_name", BoardInfo.fpga.getFlashName(), rootElement, CofFile);
      AddElement("flash_loader_device", StripPackageSpeed(), rootElement, CofFile);
      AddElement(
          "output_filename",
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
      Element BitFile = CofFile.createElement("bit0");
      SofData.appendChild(BitFile);
      AddElement(
          "sof_filename",
          SandboxPath + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof",
          BitFile,
          CofFile);
      AddElement("version", "10", rootElement, CofFile);
      AddElement("create_cvp_file", "0", rootElement, CofFile);
      AddElement("create_hps_iocsr", "0", rootElement, CofFile);
      AddElement("auto_create_rpd", "0", rootElement, CofFile);
      AddElement("rpd_little_endian", "1", rootElement, CofFile);
      Element Options = CofFile.createElement("options");
      rootElement.appendChild(Options);
      AddElement("map_file", "0", Options, CofFile);
      Element AdvancedOptions = CofFile.createElement("advanced_options");
      rootElement.appendChild(AdvancedOptions);
      AddElement("ignore_epcs_id_check", "2", AdvancedOptions, CofFile);
      AddElement("ignore_condone_check", "2", AdvancedOptions, CofFile);
      AddElement("plc_adjustment", "0", AdvancedOptions, CofFile);
      AddElement("post_chain_bitstream_pad_bytes", "-1", AdvancedOptions, CofFile);
      AddElement("post_device_bitstream_pad_bytes", "-1", AdvancedOptions, CofFile);
      AddElement("bitslice_pre_padding", "1", AdvancedOptions, CofFile);
      TransformerFactory transformerfac = TransformerFactory.newInstance();
      Transformer transformer = transformerfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.ENCODING, "US-ASCII");
      transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
      DOMSource source = new DOMSource(CofFile);
      StreamResult result = new StreamResult(new File(ScriptPath + AlteraCofFile));
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      Reporter.AddError(S.get("AlteraErrorCof"));
      return false;
    }
    return true;
  }

  private void AddElement(String ElementName, String ElementValue, Element root, Document doc) {
    Element NamedElement = doc.createElement(ElementName);
    NamedElement.appendChild(doc.createTextNode(ElementValue));
    root.appendChild(NamedElement);
  }

}
