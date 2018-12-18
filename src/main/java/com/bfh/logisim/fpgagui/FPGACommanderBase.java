package com.bfh.logisim.fpgagui;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.download.AlteraDownload;
import com.bfh.logisim.download.VivadoDownload;
import com.bfh.logisim.download.XilinxDownload;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;


public abstract class FPGACommanderBase {

	protected Project MyProject;
	protected FPGAReport MyReporter;
	protected BoardInformation MyBoardInformation = null;
	protected MappableResourcesContainer MyMappableResources;
	private String[] HDLPaths = { HDLGeneratorFactory.VERILOG.toLowerCase(),
			HDLGeneratorFactory.VHDL.toLowerCase(), "scripts", "sandbox", "ucf", "xdc"};
	@SuppressWarnings("unused")
	private static final Integer VerilogSourcePath = 0;
	@SuppressWarnings("unused")
	protected static final Integer VHDLSourcePath = 1;
	protected static final Integer ScriptPath = 2;
	protected static final Integer SandboxPath = 3;
	protected static final Integer UCFPath = 4;
	protected static final Integer XDCPath = 5;

	public FPGACommanderBase() {
		// TODO Auto-generated constructor stub
	}

	protected abstract boolean DownLoad(boolean skipVHDL, String CircuitName);


	protected boolean canDownload() {
		if (!VendorSoftware.toolsPresent(MyBoardInformation.fpga.getVendor(),
				VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor())))
			return false;
		return AppPreferences.DownloadToBoard.get();
	}

	protected boolean  MapDesign(String CircuitName) {
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		Netlist RootNetlist = RootSheet.getNetList();
		if (MyBoardInformation == null) {
			MyReporter
			.AddError("INTERNAL ERROR: No board information available ?!?");
			return false;
		}

		Map<String, ArrayList<Integer>> BoardComponents = MyBoardInformation
				.GetComponents();
		MyReporter.AddInfo("The Board " + MyBoardInformation.getBoardName()
		+ " has:");
		for (String key : BoardComponents.keySet()) {
			MyReporter.AddInfo(BoardComponents.get(key).size() + " " + key
					+ "(s)");
		}
		/*
		 * At this point I require 2 sorts of information: 1) A hierarchical
		 * netlist of all the wires that needs to be bubbled up to the toplevel
		 * in order to connect the LEDs, Buttons, etc. (hence for the HDL
		 * generation). 2) A list with all components that are required to be
		 * mapped to PCB components. Identification can be done by a hierarchy
		 * name plus component/sub-circuit name
		 */
		MyMappableResources = new MappableResourcesContainer(
				MyBoardInformation, RootNetlist);
		if (!MyMappableResources.IsMappable(BoardComponents, MyReporter)) {
			return false;
		}

		return true;
	}

	protected boolean MapDesignCheckIOs() {
		if (MyMappableResources.UnmappedList().isEmpty()) {
			MyMappableResources.BuildIOMappingInformation();
			return true;
		}

		return false;
	}

	protected boolean performDRC(String CircuitName, String HDLType) {
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		ArrayList<String> SheetNames = new ArrayList<String>();
		int DRCResult = Netlist.DRC_PASSED;
		if (root == null) {
			DRCResult |= Netlist.DRC_ERROR;
		} else {
			root.getNetList().clear();
			DRCResult = root.getNetList().DesignRuleCheckResult(MyReporter,
					HDLType, true, SheetNames);
		}
		return (DRCResult == Netlist.DRC_PASSED);
	}

	protected boolean writeHDL(String selectedCircuit, Double frequency) {
		String CircuitName = selectedCircuit;
		if (!GenDirectory(AppPreferences.FPGA_Workspace.get() + File.separator
				+ MyProject.getLogisimFile().getName())) {
			MyReporter.AddFatalError("Unable to create directory: \""
					+ AppPreferences.FPGA_Workspace.get() + File.separator
					+ MyProject.getLogisimFile().getName() + "\"");
			return false;
		}
		String ProjectDir = AppPreferences.FPGA_Workspace.get() + File.separator
				+ MyProject.getLogisimFile().getName();
		if (!ProjectDir.endsWith(File.separator)) {
			ProjectDir += File.separator;
		}
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
				+ File.separator;
		if (!CleanDirectory(ProjectDir)) {
			MyReporter
			.AddFatalError("Unable to cleanup old project files in directory: \""
					+ ProjectDir + "\"");
			return false;
		}
		if (!GenDirectory(ProjectDir)) {
			MyReporter.AddFatalError("Unable to create directory: \""
					+ ProjectDir + "\"");
			return false;
		}
		for (int i = 0; i < HDLPaths.length; i++) {
			if (!GenDirectory(ProjectDir + HDLPaths[i])) {
				MyReporter.AddFatalError("Unable to create directory: \""
						+ ProjectDir + HDLPaths[i] + "\"");
				return false;
			}
		}

		Set<String> GeneratedHDLComponents = new HashSet<String>();
		HDLGeneratorFactory Worker = RootSheet.getSubcircuitFactory()
				.getHDLGenerator(AppPreferences.HDL_Type.get(),
						RootSheet.getStaticAttributes());
		if (Worker == null) {
			MyReporter
			.AddFatalError("Internal error on HDL generation, null pointer exception");
			return false;
		}
		if (!Worker.GenerateAllHDLDescriptions(GeneratedHDLComponents,
				ProjectDir, null, MyReporter, AppPreferences.HDL_Type.get())) {
			return false;
		}
		/* Here we generate the top-level shell */
		if (RootSheet.getNetList().NumberOfClockTrees() > 0) {
			TickComponentHDLGeneratorFactory Ticker = new TickComponentHDLGeneratorFactory(
					MyBoardInformation.fpga.getClockFrequency(),
					frequency/* , boardFreq.isSelected() */);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
					+ Ticker.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
					Ticker.GetEntity(
							RootSheet.getNetList(), null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							AppPreferences.HDL_Type.get()), Ticker
					.getComponentStringIdentifier(), MyReporter,
					AppPreferences.HDL_Type.get())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ Ticker.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
					Ticker.GetArchitecture(RootSheet.getNetList(), null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							AppPreferences.HDL_Type.get()), Ticker
					.getComponentStringIdentifier(), MyReporter,
					AppPreferences.HDL_Type.get())) {
				return false;
			}

			HDLGeneratorFactory ClockGen = RootSheet
					.getNetList()
					.GetAllClockSources()
					.get(0)
					.getFactory()
					.getHDLGenerator(
							AppPreferences.HDL_Type.get(),
							RootSheet.getNetList().GetAllClockSources().get(0)
							.getAttributeSet());
			String CompName = RootSheet.getNetList().GetAllClockSources()
					.get(0).getFactory().getHDLName(null);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
					+ ClockGen.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
					ClockGen.GetEntity(
							RootSheet.getNetList(), null, CompName, MyReporter,
							AppPreferences.HDL_Type.get()), CompName, MyReporter,
					AppPreferences.HDL_Type.get())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ ClockGen.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
					ClockGen.GetArchitecture(RootSheet.getNetList(), null,
							CompName, MyReporter, AppPreferences.HDL_Type.get()),
					CompName, MyReporter, AppPreferences.HDL_Type.get())) {
				return false;
			}
		}
		Worker = new ToplevelHDLGeneratorFactory(
				MyBoardInformation.fpga.getClockFrequency(),
				frequency, RootSheet, MyMappableResources);
		if (!AbstractHDLGeneratorFactory.WriteEntity(
				ProjectDir
				+ Worker.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
				Worker.GetEntity(RootSheet.getNetList(), null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, AppPreferences.HDL_Type.get()), Worker
				.getComponentStringIdentifier(), MyReporter,
				AppPreferences.HDL_Type.get())) {
			return false;
		}
		if (!AbstractHDLGeneratorFactory.WriteArchitecture(
				ProjectDir
				+ Worker.GetRelativeDirectory(AppPreferences.HDL_Type.get()),
				Worker.GetArchitecture(RootSheet.getNetList(), null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, AppPreferences.HDL_Type.get()), Worker
				.getComponentStringIdentifier(), MyReporter,
				AppPreferences.HDL_Type.get())) {
			return false;
		}

		return true;
	}

	protected boolean GenDirectory(String dir) {
		try {
			File Dir = new File(dir);
			if (Dir.exists()) {
				return true;
			}
			return Dir.mkdirs();
		} catch (Exception e) {
			MyReporter
			.AddFatalError("Could not check/create directory :" + dir);
			return false;
		}
	}


	private void GetVHDLFiles(String SourcePath, String Path,
			ArrayList<String> Entities, ArrayList<String> Behaviors,
			String HDLType) {
		File Dir = new File(Path);
		File[] Files = Dir.listFiles();
		for (File thisFile : Files) {
			if (thisFile.isDirectory()) {
				if (Path.endsWith(File.separator)) {
					GetVHDLFiles(SourcePath, Path + thisFile.getName(),
							Entities, Behaviors, HDLType);
				} else {
					GetVHDLFiles(SourcePath,
							Path + File.separator + thisFile.getName(),
							Entities, Behaviors, HDLType);
				}
			} else {
				String EntityMask = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? FileWriter.EntityExtension
						+ ".vhd"
						: ".v";
				String ArchitecturMask = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? FileWriter.ArchitectureExtension
						+ ".vhd"
						: "#not_searched#";
				if (thisFile.getName().endsWith(EntityMask)) {
					Entities.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
				} else if (thisFile.getName().endsWith(ArchitecturMask)) {
					Behaviors.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
				}
			}
		}
	}


	protected boolean DownLoadDesign(boolean generateOnly, boolean downloadOnly, String CircuitName,
			boolean writeToFlash, boolean downloadDesign) {
		if (generateOnly && downloadOnly) {
			MyReporter.AddError("Can not have skip VHDL generation and generate HDL only in the same time...");
			return false;
		}

		String ProjectDir = AppPreferences.FPGA_Workspace.get() + File.separator
				+ MyProject.getLogisimFile().getName();
		if (!ProjectDir.endsWith(File.separator)) {
			ProjectDir += File.separator;
		}

		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
				+ File.separator;
		String SourcePath = ProjectDir + AppPreferences.HDL_Type.get().toLowerCase()
				+ File.separator;
		ArrayList<String> Entities = new ArrayList<String>();
		ArrayList<String> Behaviors = new ArrayList<String>();
		GetVHDLFiles(ProjectDir, SourcePath, Entities, Behaviors,
				AppPreferences.HDL_Type.get());
		if (MyBoardInformation.fpga.getVendor() == VendorSoftware.VendorAltera) {
			if (AlteraDownload.GenerateQuartusScript(MyReporter, ProjectDir
					+ HDLPaths[ScriptPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					AppPreferences.HDL_Type.get())) {
				return AlteraDownload.Download(ProjectDir
						+ HDLPaths[ScriptPath] + File.separator, SourcePath,
						ProjectDir + HDLPaths[SandboxPath] + File.separator,
						MyReporter, downloadDesign);
			}
		} else if (MyBoardInformation.fpga.getVendor() == VendorSoftware.VendorXilinx) {
			if (XilinxDownload.GenerateISEScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[UCFPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					AppPreferences.HDL_Type.get(),
					writeToFlash)
					&& !generateOnly) {
				return XilinxDownload.Download(MyBoardInformation,
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[UCFPath] + File.separator,
						ProjectDir, ProjectDir + HDLPaths[SandboxPath]
								+ File.separator, MyReporter, downloadDesign);
			}
		} else if (MyBoardInformation.fpga.getVendor() == VendorSoftware.VendorVivado) {
			if (VivadoDownload.GenerateScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[XDCPath] + File.separator,
					ProjectDir + HDLPaths[SandboxPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					AppPreferences.HDL_Type.get(),
					writeToFlash)
					&& !generateOnly) {
				return VivadoDownload.Download(
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[SandboxPath] + File.separator,
						MyReporter, downloadOnly, downloadDesign);
			}
		}

		return false;
	}


	private boolean CleanDirectory(String dir) {
		try {
			File thisDir = new File(dir);
			if (!thisDir.exists()) {
				return true;
			}
			for (File theFiles : thisDir.listFiles()) {
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
			if (!thisDir.delete()) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			MyReporter.AddFatalError("Could not remove directory tree :" + dir);
			return false;
		}
	}
}
