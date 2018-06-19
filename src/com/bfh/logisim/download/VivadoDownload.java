package com.bfh.logisim.download;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.IoStandards;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.proj.Projects;

public class VivadoDownload {

	/* TODO There are duplicated code lines amongst the 3 file AlteraDownload / Vivado / Xillinx
	 * it should be sorted by using a base class to all 3 of them
	 */
	public static boolean Download(String scriptPath, String sandboxPath, FPGAReport myReporter, boolean downloadOnly,
			boolean DownloadBitstream) {
		GridBagConstraints gbc = new GridBagConstraints();
		JFrame panel = new JFrame("Vivado Downloading");
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);
		JLabel locText = new JLabel("Generating FPGA files and performing download; this may take a while");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(locText, gbc);
		JProgressBar progres = new JProgressBar(0, 3);
		progresVal = 0;
		progres.setValue(progresVal);
		progres.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(progres, gbc);
		panel.pack();

		/* To allow no window mode */
		if (Projects.getTopFrame() != null) {
			panel.setLocation(Projects.getCenteredLoc(panel.getWidth(), panel.getHeight() * 4));
			panel.setVisible(true);
		} else {
			panel.setVisible(false);
		}

		Rectangle labelRect = locText.getBounds();
		labelRect.x = 0;
		labelRect.y = 0;
		locText.paintImmediately(labelRect);

		VendorSoftware vivadoVendor = VendorSoftware.getSoftware(VendorSoftware.VendorVivado);

		// Create Vivado project
		if (!downloadOnly) {
			boolean status = executeTclScript(vivadoVendor.getBinaryPath(0),
					scriptPath + File.separator + CREATE_PROJECT_TCL,
					"Create Vivado project",
					sandboxPath, myReporter, locText, progres);
			if (!status) {
				panel.dispose();
				return false;
			}
		}

		// Generate bitstream
		if (!downloadOnly) {
			boolean status = executeTclScript(vivadoVendor.getBinaryPath(0),
					scriptPath + File.separator + GENERATE_BITSTREAM_FILE,
					"Generate bitstream",
					sandboxPath, myReporter, locText, progres);
			if (!status) {
				panel.dispose();
				return false;
			}
			boolean bitFileExists = new File(_bitStreamPath).exists();
			if (!bitFileExists) {
				myReporter.AddFatalError("Could not generate bitfile! Check Console tab for more details.");
				panel.dispose();
				return false;
			}
		}

		// Download to board
		// only if the bitfile exists
		boolean bitFileExists = new File(_bitStreamPath).exists();
		if (bitFileExists) {
			if (!DownloadBitstream) {
				return true;
			}

			Object[] options = { "Yes, download","No, abort" };
			if (JOptionPane.showOptionDialog(
					progres,
					"Verify that your board is connected and you are ready to download.",
					"Ready to download ?", JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE, null, options, options[0]) != JOptionPane.YES_OPTION) {
				myReporter.AddWarning("Download aborted.");
				panel.dispose();
				return false;
			}

			boolean status = executeTclScript(vivadoVendor.getBinaryPath(0),
					scriptPath + File.separator + LOAD_BITSTEAM_FILE,
					"Downloading bitfile",
					sandboxPath, myReporter, locText, progres);
			panel.dispose();
			return status;
		} else {
			myReporter.AddFatalError("No bitfile found!");
			panel.dispose();
			return false;
		}
	}

	private static int progresVal = 0;
	private static boolean executeTclScript(String binary, String tclScript, String message, String sandboxPath,
			FPGAReport myReporter, JLabel locText, JProgressBar progres) {
		try {
			locText.setText(message);
			Rectangle labelRect = locText.getBounds();
			labelRect.x = 0;
			labelRect.y = 0;
			locText.paintImmediately(labelRect);
			progres.setValue(progresVal++);
			Rectangle progRect = progres.getBounds();
			progRect.x = 0;
			progRect.y = 0;
			progres.paintImmediately(progRect);
			List<String> command = new ArrayList<String>();
			command.add(binary);
			command.add("-mode");
			command.add("batch");
			command.add("-source");
			command.add(tclScript);
			ProcessBuilder vivado1 = new ProcessBuilder(command);
			vivado1.directory(new File(sandboxPath));
			final Process createProject = vivado1.start();
			InputStream is = createProject.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			myReporter.ClsScr();
			while ((line = br.readLine()) != null) {
				myReporter.print(line);
			}
			createProject.waitFor();
			if (createProject.exitValue() != 0) {
				myReporter.AddFatalError("Failed to run tcl script, see Console tab for more details");
				return false;
			}
		} catch (IOException e) {
			myReporter.AddFatalError("Internal Error during Vivado download");
			return false;
		} catch (InterruptedException e) {
			myReporter.AddFatalError("Internal Error during Vivado download");
			return false;
		}
		return true;
	}

	public static boolean GenerateScripts(FPGAReport myReporter,
			String projectPath, String scriptPath, String xdcPath,
			String sandBoxPath, Netlist rootNetlist, MappableResourcesContainer mapInfo,
			BoardInformation boardInfo, ArrayList<String> entities,
			ArrayList<String> architectures, String HDLType,
			boolean writeToFlash) {

		// create project files
		File createProjectFile = FileWriter.GetFilePointer(scriptPath, CREATE_PROJECT_TCL, myReporter);
		File xdcFile = FileWriter.GetFilePointer(xdcPath, XDC_FILE, myReporter);
		File generateBitstreamFile = FileWriter.GetFilePointer(scriptPath, GENERATE_BITSTREAM_FILE, myReporter);
		File loadBitstreamFile = FileWriter.GetFilePointer(scriptPath, LOAD_BITSTEAM_FILE, myReporter);
		if (createProjectFile == null || xdcFile == null || generateBitstreamFile == null || loadBitstreamFile == null) {
			createProjectFile = new File(scriptPath + CREATE_PROJECT_TCL);
			xdcFile = new File(xdcPath, XDC_FILE);
			generateBitstreamFile = new File(scriptPath, GENERATE_BITSTREAM_FILE);
			loadBitstreamFile = new File(scriptPath, LOAD_BITSTEAM_FILE);
			return createProjectFile.exists() && xdcFile.exists() && generateBitstreamFile.exists() && loadBitstreamFile.exists();
		}

		String vivadoProjectPath = sandBoxPath + File.separator + VIVADO_PROJECT_NAME;

		// fill create project TCL script
		ArrayList<String> contents = new ArrayList<String>();
		contents.add("create_project " + VIVADO_PROJECT_NAME + " \"" + vivadoProjectPath.replace("\\", "/") + "\"");
		contents.add("set_property part " +
				boardInfo.fpga.getPart() +
				boardInfo.fpga.getPackage() +
				boardInfo.fpga.getSpeedGrade() +
				" [current_project]");
		contents.add("set_property target_language VHDL [current_project]");
		// add all entities and architectures
		for (String entity : entities) {
			contents.add("add_files \"" + entity + "\"");
		}
		for (String architecture : architectures) {
			contents.add("add_files \"" + architecture + "\"");
		}
		// add xdc constraints
		contents.add("add_files -fileset constrs_1 \"" + xdcFile.getAbsolutePath().replace("\\", "/") + "\"");
		contents.add("exit");
		if (!FileWriter.WriteContents(createProjectFile, contents, myReporter))
			return false;
		contents.clear();

		// fill the xdc file
		if (rootNetlist.NumberOfClockTrees() > 0) {
			String clockPin       = boardInfo.fpga.getClockPinLocation();
			String clockSignal    = TickComponentHDLGeneratorFactory.FPGAClock;
			String getPortsString = " [get_ports {" + clockSignal + "}]";
			contents.add("set_property PACKAGE_PIN " + clockPin + getPortsString);

			if (boardInfo.fpga.getClockStandard() != IoStandards.DefaulStandard
					&& boardInfo.fpga.getClockStandard() != IoStandards.Unknown) {
				String clockIoStandard = IoStandards.Behavior_strings[boardInfo.fpga.getClockStandard()];
				contents.add("    set_property IOSTANDARD " + clockIoStandard + getPortsString);
			}

			Long clockFrequency = boardInfo.fpga.getClockFrequency();
			double clockPeriod  = 1000000000.0 / clockFrequency;
			contents.add("    create_clock -add -name sys_clk_pin -period " + String.format(Locale.US, "%.2f", clockPeriod) + " -waveform {0 " + String.format("%1$,.0f", clockPeriod / 2) + "} " + getPortsString);
			contents.add("");
		}

		contents.addAll(mapInfo.GetFPGAPinLocs(VendorSoftware.VendorVivado));
		if (!FileWriter.WriteContents(xdcFile, contents, myReporter))
			return false;
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
		if (!FileWriter.WriteContents(generateBitstreamFile, contents, myReporter))
			return false;
		contents.clear();

		// load bitstream
		String JTAGPos = String.valueOf(boardInfo.fpga.getFpgaJTAGChainPosition());
		String lindex = "[lindex [get_hw_devices] " + JTAGPos + "]";
		contents.add("open_hw");
		contents.add("connect_hw_server");
		contents.add("open_hw_target");
		_bitStreamPath = vivadoProjectPath + File.separator + VIVADO_PROJECT_NAME + ".runs"
				+ File.separator + "impl_1" + File.separator + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".bit";
		_bitStreamPath = _bitStreamPath.replace("\\", "/");
		contents.add("set_property PROGRAM.FILE {" + _bitStreamPath + "} " + lindex);
		contents.add("current_hw_device " + lindex);
		contents.add("refresh_hw_device -update_hw_probes false " + lindex);
		contents.add("program_hw_device " + lindex);
		contents.add("close_hw");
		contents.add("exit");
		return FileWriter.WriteContents(loadBitstreamFile, contents, myReporter);
	}

	private static String _bitStreamPath = "";
	private final static String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
	private final static String GENERATE_BITSTREAM_FILE = "vivadoGenerateBitStream.tcl";
	private final static String LOAD_BITSTEAM_FILE = "vivadoLoadBitStream.tcl";
	private final static String XDC_FILE = "vivadoConstraints.xdc";
	private final static String VIVADO_PROJECT_NAME = "vp";
}
