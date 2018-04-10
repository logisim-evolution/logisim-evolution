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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.PullBehaviors;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.proj.Projects;

public class AlteraDownload {

	/* TODO There are duplicated code lines amongst the 3 file AlteraDownload / Vivado / Xillinx
	 * it should be sorted by using a base class to all 3 of them
	 */
	public static boolean Download(String scriptPath,
			String ProjectPath, String SandboxPath, FPGAReport MyReporter, boolean DownloadBitstream) {
		VendorSoftware alteraVendor = VendorSoftware.getSoftware(VendorSoftware.VendorAltera);
		boolean SofFileExists = new File(SandboxPath
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof")
				.exists();
		GridBagConstraints gbc = new GridBagConstraints();
		JFrame panel = new JFrame("Altera Downloading");
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);
		JLabel LocText = new JLabel(
				"Generating FPGA files and performing download; this may take a while");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(LocText, gbc);
		JProgressBar progres = new JProgressBar(0, 5);
		progres.setValue(1);
		progres.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(progres, gbc);
		panel.pack();

		if (Projects.getTopFrame() != null) {
			panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
					panel.getHeight() * 4));
			panel.setVisible(true);
		} else {
			panel.setVisible(false);
		}

		Rectangle labelRect = LocText.getBounds();
		labelRect.x = 0;
		labelRect.y = 0;
		LocText.paintImmediately(labelRect);
		List<String> command = new ArrayList<String>();
		if (!SofFileExists) {
			try {
				LocText.setText("Creating Project");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				command.add(alteraVendor.getBinaryPath(0));
				command.add("-t");
				command.add(scriptPath.replace(ProjectPath, ".."
						+ File.separator)
						+ "AlteraDownload.tcl");
				ProcessBuilder Altera1 = new ProcessBuilder(command);
				Altera1.directory(new File(SandboxPath));
				final Process CreateProject = Altera1.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				MyReporter.ClsScr();
				while ((line = br.readLine()) != null) {
					MyReporter.print(line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					MyReporter
					.AddFatalError("Failed to Create a Quartus Project, cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			}
		}
		progres.setValue(2);
		Rectangle ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		command.clear();
		if (!SofFileExists) {
			try {
				LocText.setText("Optimize Project");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				command.add(alteraVendor.getBinaryPath(2));
				command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
				command.add("--optimize=area");
				ProcessBuilder Altera1 = new ProcessBuilder(command);
				Altera1.directory(new File(SandboxPath));
				final Process CreateProject = Altera1.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				MyReporter.ClsScr();
				while ((line = br.readLine()) != null) {
					MyReporter.print(line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					MyReporter
					.AddFatalError("Failed to optimize (AREA) Project, cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			}
		}
		LocText.setText("Synthesizing and creating configuration file (this may take a while)");
		labelRect = LocText.getBounds();
		labelRect.x = 0;
		labelRect.y = 0;
		LocText.paintImmediately(labelRect);
		progres.setValue(3);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		if (!SofFileExists) {
			try {
				command.clear();
				command.add(alteraVendor.getBinaryPath(0));
				command.add("--flow");
				command.add("compile");
				command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName);
				ProcessBuilder Altera1 = new ProcessBuilder(command);
				Altera1.directory(new File(SandboxPath));
				final Process CreateProject = Altera1.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				MyReporter.ClsScr();
				while ((line = br.readLine()) != null) {
					MyReporter.print(line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					MyReporter
					.AddFatalError("Failed to synthesize design and to create the configuration files, cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Altera download");
				panel.dispose();
				return false;
			}
		}

		if (!DownloadBitstream) {
			return true;
		}

		LocText.setText("Downloading");
		Object[] options = { "Yes, download","No, abort" };
		if (JOptionPane
				.showOptionDialog(
						progres,
						"Verify that your board is connected and you are ready to download.",
						"Ready to download ?", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[0]) != JOptionPane.YES_OPTION) {
			MyReporter.AddWarning("Download aborted.");
			panel.dispose();
			return false;
		}

		labelRect = LocText.getBounds();
		labelRect.x = 0;
		labelRect.y = 0;
		LocText.paintImmediately(labelRect);
		progres.setValue(4);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		try {
			command.clear();
			command.add(alteraVendor.getBinaryPath(1));
			command.add("-c");
			command.add("usb-blaster");
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
			MyReporter.AddInfo(command.toString());
			ProcessBuilder Altera1 = new ProcessBuilder(command);
			Altera1.directory(new File(SandboxPath));
			final Process CreateProject = Altera1.start();
			InputStream is = CreateProject.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			MyReporter.ClsScr();
			while ((line = br.readLine()) != null) {
				MyReporter.print(line);
			}
			CreateProject.waitFor();
			if (CreateProject.exitValue() != 0) {
				MyReporter
				.AddFatalError("Failed to Download design; did you connect the board?");
				panel.dispose();
				return false;
			}
		} catch (IOException e) {
			MyReporter.AddFatalError("Internal Error during Altera download");
			panel.dispose();
			return false;
		} catch (InterruptedException e) {
			MyReporter.AddFatalError("Internal Error during Altera download");
			panel.dispose();
			return false;
		}

		panel.dispose();
		return true;
	}

	public static boolean GenerateQuartusScript(FPGAReport MyReporter,
			String ScriptPath, Netlist RootNetList,
			MappableResourcesContainer MapInfo, BoardInformation BoardInfo,
			ArrayList<String> Entities, ArrayList<String> Architectures,
			String HDLType) {
		File ScriptFile = FileWriter.GetFilePointer(ScriptPath,
				"AlteraDownload.tcl", MyReporter);
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
		return FileWriter.WriteContents(ScriptFile, Contents, MyReporter);
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
				+ GetClockFrequencyString(CurrentBoard) + "\"");
		result.add(Assignment + "RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
		result.add(Assignment + "CYCLONEII_RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
		return result;
	}

	private static String GetClockFrequencyString(BoardInformation CurrentBoard) {
		long clkfreq = CurrentBoard.fpga.getClockFrequency();
		if (clkfreq % 1000000 == 0) {
			clkfreq /= 1000000;
			return Long.toString(clkfreq) + " MHz ";
		} else if (clkfreq % 1000 == 0) {
			clkfreq /= 1000;
			return Long.toString(clkfreq) + " kHz ";
		}
		return Long.toString(clkfreq);
	}
}
