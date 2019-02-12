/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.fpga.download;

import static com.cburch.logisim.fpga.Strings.S;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardInformation;
import com.cburch.logisim.fpga.fpgaboardeditor.PullBehaviors;
import com.cburch.logisim.fpga.fpgagui.FPGACommanderBase;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;

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
	
	public AlteraDownload(String ProjectPath,
			              FPGAReport Reporter,
			              Netlist RootNetList,
			              BoardInformation BoardInfo,
			              ArrayList<String> Entities,
			              ArrayList<String> Architectures,
			              String HDLType) {
		this.ProjectPath = ProjectPath;
		this.SandboxPath = FPGACommanderBase.GetDirectoryLocation(ProjectPath, FPGACommanderBase.SandboxPath);
		this.ScriptPath = FPGACommanderBase.GetDirectoryLocation(ProjectPath, FPGACommanderBase.ScriptPath);
		this.Reporter = Reporter;
		this.RootNetList = RootNetList;
		this.BoardInfo = BoardInfo;
		this.Entities = Entities;
		this.Architectures = Architectures;
		this.HDLType = HDLType;
		cablename = "usb-blaster";
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
			case 0 : return S.get("AlteraProject");
			case 1 : return S.get("AlteraOptimize");
			case 2 : return S.get("AlteraSyntPRBit");
			default: return "Unknown, bizar";
		}
	}

	@Override
	public ProcessBuilder PerformStep(int stage) {
		switch (stage) {
			case 0 : return Stage0Project();
			case 1 : return Stage1Optimize();
			case 2 : return Stage2SPRBit();
			default : return null;
		}
	}

	@Override
	public boolean readyForDownload() {
		boolean SofFile = new File(SandboxPath+ToplevelHDLGeneratorFactory.FPGAToplevelName+".sof").exists();
		boolean PofFile = new File(SandboxPath+ToplevelHDLGeneratorFactory.FPGAToplevelName+".pof").exists();
		return SofFile|PofFile;
	}
	
	@Override
	public ProcessBuilder DownloadToBoard() {
		List<String> command = new ArrayList<String>();
		command.add(alteraVendor.getBinaryPath(1));
		command.add("-c");
		command.add(cablename);
		command.add("-m");
		command.add("jtag");
		command.add("-o");
		// if there is no .sof generated, try with the .pof
		if (new File(SandboxPath
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof")
				.exists()) {
			command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName
					+ ".sof");
		} else {
			command.add("P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName
					+ ".pof");
		}
		ProcessBuilder Down = new ProcessBuilder(command);
		Down.directory(new File(SandboxPath));
		return Down;
	}

	private ProcessBuilder Stage0Project() {
		List<String> command = new ArrayList<String>();
		command.add(alteraVendor.getBinaryPath(0));
		command.add("-t");
		command.add(ScriptPath.replace(ProjectPath, ".."
				+ File.separator)
				+ "AlteraDownload.tcl");
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
		File ScriptFile = FileWriter.GetFilePointer(ScriptPath,
				"AlteraDownload.tcl", Reporter);
		if (ScriptFile == null) {
			ScriptFile = new File(ScriptPath + "AlteraDownload.tcl");
			return ScriptFile.exists();
		}
		String FileType = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? "VHDL_FILE"
				: "VERILOG_FILE";
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("# Load Quartus II Tcl Project package");
		Contents.add("package require ::quartus::project");
		Contents.add("");
		Contents.add("set need_to_close_project 0");
		Contents.add("set make_assignments 1");
		Contents.add("");
		Contents.add("# Check that the right project is open");
		Contents.add("if {[is_project_open]} {");
		Contents.add("    if {[string compare $quartus(project) \""
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "\"]} {");
		Contents.add("        puts \"Project "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName
				+ " is not open\"");
		Contents.add("        set make_assignments 0");
		Contents.add("    }");
		Contents.add("} else {");
		Contents.add("    # Only open if not already open");
		Contents.add("    if {[project_exists "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "]} {");
		Contents.add("        project_open -revision "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + " "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName);
		Contents.add("    } else {");
		Contents.add("        project_new -revision "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + " "
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
			Contents.add("    set_global_assignment -name " + FileType + " \""
					+ Entities.get(i) + "\"");
		}
		for (int i = 0; i < Architectures.size(); i++) {
			Contents.add("    set_global_assignment -name " + FileType + " \""
					+ Architectures.get(i) + "\"");
		}
		Contents.add("");
		Contents.add("    # Map fpga_clk and ionets to fpga pins");
		if (RootNetList.NumberOfClockTrees() > 0) {
			Contents.add("    set_location_assignment "
					+ BoardInfo.fpga.getClockPinLocation() + " -to "
					+ TickComponentHDLGeneratorFactory.FPGAClock);
		}
		Contents.addAll(MapInfo.GetFPGAPinLocs(VendorSoftware.VendorAltera));
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


	private static ArrayList<String> GetAlteraAssignments(
			BoardInformation CurrentBoard) {
		ArrayList<String> result = new ArrayList<String>();
		String Assignment = "    set_global_assignment -name ";
		result.add(Assignment + "FAMILY \"" + CurrentBoard.fpga.getTechnology()
		+ "\"");
		result.add(Assignment + "DEVICE " + CurrentBoard.fpga.getPart());
		String[] Package = CurrentBoard.fpga.getPackage().split(" ");
		result.add(Assignment + "DEVICE_FILTER_PACKAGE " + Package[0]);
		result.add(Assignment + "DEVICE_FILTER_PIN_COUNT " + Package[1]);
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.Float) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT TRI-STATED\"");
		}
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLUP\"");
		}
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLDOWN\"");
		}
		result.add(Assignment + "FMAX_REQUIREMENT \""
				+ Download.GetClockFrequencyString(CurrentBoard) + "\"");
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
			Reporter.print("===> "+S.get("AlteraDetectDevice"));
			Reporter.print("===");
			if (Download.execute(Detect, response, Reporter)!= null)
				return false;
		} catch (IOException | InterruptedException e) {
			return false;
		}
		ArrayList<String> Devices = Devices(response);
		if (Devices == null)
			return false;
		if (Devices.size()==1)
			return true;
		String selection = Download.ChooseBoard(Devices);
		if (selection == null)
			return false;
		cablename = selection;
		return true;
	}
	
	private ArrayList<String> Devices(ArrayList<String> lines) {
		/* This code originates from Kevin Walsh */
		ArrayList<String> dev = new ArrayList<String>();
		for (String line : lines) {
			int n = dev.size()+1;
			if (!line.matches("^"+n+"\\) .*"))
				continue;
			line = line.replaceAll("^"+n+"\\) ", "");
			dev.add(line.trim());
		}
		if (dev.size()==0)
			return null;
		return dev;
	}

}
