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
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.IoStandards;
import com.bfh.logisim.fpgaboardeditor.PullBehaviors;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.gui.FPGACliGuiFabric;
import com.bfh.logisim.gui.IFPGAFrame;
import com.bfh.logisim.gui.IFPGAGrid;
import com.bfh.logisim.gui.IFPGAGridLayout;
import com.bfh.logisim.gui.IFPGALabel;
import com.bfh.logisim.gui.IFPGAProgressBar;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.proj.Projects;

public class XilinxDownload {
	/* TODO There are duplicated code lines amongst the 3 file AlteraDownload / Vivado / Xillinx
	 * it should be sorted by using a base class to all 3 of them
	 */
	public static boolean Download(
			BoardInformation BoardInfo, String scriptPath, String UcfPath,
			String ProjectPath, String SandboxPath, FPGAReport MyReporter, boolean DownloadBitstream) {
		VendorSoftware xilinxVendor = VendorSoftware.getSoftware(VendorSoftware.VendorXilinx);
		boolean IsCPLD = BoardInfo.fpga.getPart().toUpperCase()
				.startsWith("XC2C")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XA2C")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XCR3")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XC9500")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XA9500");
		String BitfileExt = (IsCPLD) ? "jed" : "bit";
		boolean BitFileExists = new File(SandboxPath
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "."
				+ BitfileExt).exists();
		IFPGAGrid gbc = FPGACliGuiFabric.getFPGAGrid() ;
		IFPGAFrame panel = FPGACliGuiFabric.getFPGAFrame("Xilinx Downloading");
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		IFPGAGridLayout thisLayout = FPGACliGuiFabric.getFPGAGridLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x,mlocation.y);
		IFPGALabel LocText = FPGACliGuiFabric.getFPGALabel("Generating FPGA files and performing download; this may take a while");

		gbc.setGridx(0);
		gbc.setGridy(1);
		gbc.setFill(GridBagConstraints.HORIZONTAL);
		panel.add(LocText, gbc);
		IFPGAProgressBar progres = FPGACliGuiFabric.getFPGAProgressBar(0,  xilinxVendor.getBinaries().length);
		progres.setValue(0);
		progres.setStringPainted(true);
		gbc.setGridx(0);
		gbc.setGridy(2);
		gbc.setFill(GridBagConstraints.HORIZONTAL);
		panel.add(progres, gbc);
		panel.pack();

		panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
				panel.getHeight() * 4));
		panel.setVisible(true);
		Rectangle labelRect = LocText.getBounds();

		labelRect.x = 0;
		labelRect.y = 0;
		LocText.paintImmediately(labelRect);

		List<String> command = new ArrayList<String>();
		if (!BitFileExists) {
			try {
				LocText.setText("Synthesizing Project");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				Rectangle ProgRect = progres.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progres.paintImmediately(ProgRect);
				command.clear();
				command.add(xilinxVendor.getBinaryPath(0));
				command.add("-ifn");
				command.add(scriptPath.replace(ProjectPath, "../")
						+ File.separator + script_file);
				command.add("-ofn");
				command.add("logisim.log");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					.AddFatalError("Failed to Synthesize Xilinx project; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}

		if (!BitFileExists) {
			try {
				LocText.setText("Adding contraints");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				progres.setValue(1);
				Rectangle ProgRect = progres.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progres.paintImmediately(ProgRect);
				command.clear();
				command.add(xilinxVendor.getBinaryPath(1));
				command.add("-intstyle");
				command.add("ise");
				command.add("-uc");
				command.add(UcfPath.replace(ProjectPath, "../")
						+ File.separator + ucf_file);
				command.add("logisim.ngc");
				command.add("logisim.ngd");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					.AddFatalError("Failed to add Xilinx constraints; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}

		if (!BitFileExists && !IsCPLD) {
			try {
				LocText.setText("Mapping Design");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				progres.setValue(2);
				Rectangle ProgRect = progres.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progres.paintImmediately(ProgRect);
				command.clear();
				command.add(xilinxVendor.getBinaryPath(2));
				command.add("-intstyle");
				command.add("ise");
				command.add("-o");
				command.add("logisim_map");
				command.add("logisim.ngd");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					.AddFatalError("Failed to map Xilinx design; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists) {
			try {
				LocText.setText("Place and routing Design");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				progres.setValue(3);
				Rectangle ProgRect = progres.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progres.paintImmediately(ProgRect);
				command.clear();
				if (!IsCPLD) {
					command.add(xilinxVendor.getBinaryPath(3));
					command.add("-w");
					command.add("-intstyle");
					command.add("ise");
					command.add("-ol");
					command.add("high");
					command.add("logisim_map");
					command.add("logisim_par");
					command.add("logisim_map.pcf");
				} else {
					command.add(xilinxVendor.getBinaryPath(6));
					command.add("-p");
					command.add(BoardInfo.fpga.getPart().toUpperCase() + "-"
							+ BoardInfo.fpga.getSpeedGrade() + "-"
							+ BoardInfo.fpga.getPackage().toUpperCase());
					command.add("-intstyle");
					command.add("ise");
					/* TODO: do correct termination type */
					command.add("-terminate");
					if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
						command.add("pullup");
					} else if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
						command.add("pulldown");
					} else {
						command.add("float");
					}
					command.add("-loc");
					command.add("on");
					command.add("-log");
					command.add("logisim_cpldfit.log");
					command.add("logisim.ngd");
				}
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					.AddFatalError("Failed to P&R Xilinx design; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists) {

			if (!DownloadBitstream) {
				return true;
			}

			try {
				LocText.setText("Generating Bitfile");
				labelRect = LocText.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				LocText.paintImmediately(labelRect);
				progres.setValue(4);
				Rectangle ProgRect = progres.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progres.paintImmediately(ProgRect);
				command.clear();
				if (!IsCPLD) {
					command.add(xilinxVendor.getBinaryPath(4));
					command.add("-w");
					if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
						command.add("-g");
						command.add("UnusedPin:PULLUP");
					}
					if (BoardInfo.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
						command.add("-g");
						command.add("UnusedPin:PULLDOWN");
					}
					command.add("-g");
					command.add("StartupClk:CCLK");
					command.add("logisim_par");
					command.add(ToplevelHDLGeneratorFactory.FPGAToplevelName
							+ ".bit");
				} else {
					command.add(xilinxVendor.getBinaryPath(7));
					command.add("-i");
					command.add("logisim.vm6");
				}
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					.AddFatalError("Failed generate bitfile; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				MyReporter
				.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		try {
			LocText.setText("Downloading Bitfile");
			labelRect = LocText.getBounds();
			labelRect.x = 0;
			labelRect.y = 0;
			LocText.paintImmediately(labelRect);
			progres.setValue(5);
			Rectangle ProgRect = progres.getBounds();
			ProgRect.x = 0;
			ProgRect.y = 0;
			progres.paintImmediately(ProgRect);
			Object[] options = { "Yes, download","No, abort" };
			/* TODO remove in case of cli mode */
			if (FPGACliGuiFabric.getFPGAOptionPanel()
					.doshowOptionDialog(
							progres,
							"Verify that your board is connected and you are ready to download.",
							"Ready to download ?", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options, options[0]) != JOptionPane.YES_OPTION) {
				MyReporter.AddWarning("Download aborted.");
				panel.dispose();
				return false;
			}
			/* Until here update of status window */
			if (!BoardInfo.fpga.USBTMCDownloadRequired()) {
				command.clear();
				command.add(xilinxVendor.getBinaryPath(5));
				command.add("-batch");
				command.add(scriptPath.replace(ProjectPath, "../")
						+ File.separator + download_file);
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(SandboxPath));
				final Process CreateProject = Xilinx.start();
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
					MyReporter.AddFatalError("Failed in downloading");
					panel.dispose();
					return false;
				}
				/* Until here is the standard download with programmer */
			} else {
				MyReporter.ClsScr();
				/* Here we do the USBTMC Download */
				boolean usbtmcdevice = new File("/dev/usbtmc0").exists();
				if (!usbtmcdevice) {
					MyReporter.AddFatalError("Could not find usbtmc device");
					panel.dispose();
					return false;
				}
				File bitfile = new File(SandboxPath
						+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "."
						+ BitfileExt);
				byte[] bitfile_buffer = new byte[BUFFER_SIZE];
				int bitfile_buffer_size;
				BufferedInputStream bitfile_in = new BufferedInputStream(
						new FileInputStream(bitfile));
				File usbtmc = new File("/dev/usbtmc0");
				BufferedOutputStream usbtmc_out = new BufferedOutputStream(
						new FileOutputStream(usbtmc));
				usbtmc_out.write("FPGA ".getBytes());
				bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0,
						BUFFER_SIZE);
				while (bitfile_buffer_size > 0) {
					usbtmc_out.write(bitfile_buffer, 0, bitfile_buffer_size);
					bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0,
							BUFFER_SIZE);
				}
				usbtmc_out.close();
				bitfile_in.close();
			}
		} catch (IOException e) {
			MyReporter.AddFatalError("Internal Error during Xilinx download");
			panel.dispose();
			return false;
		} catch (InterruptedException e) {
			MyReporter.AddFatalError("Internal Error during Xilinx download");
			panel.dispose();
			return false;
		}

		panel.dispose();
		return true;
	}

	public static boolean GenerateISEScripts(FPGAReport MyReporter,
			String ProjectPath, String ScriptPath, String UcfPath,
			Netlist RootNetlist, MappableResourcesContainer MapInfo,
			BoardInformation BoardInfo, ArrayList<String> Entities,
			ArrayList<String> Architectures, String HDLType,
			boolean writeToFlash) {
		boolean IsCPLD = BoardInfo.fpga.getPart().toUpperCase()
				.startsWith("XC2C")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XA2C")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XCR3")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XC9500")
				|| BoardInfo.fpga.getPart().toUpperCase().startsWith("XA9500");
		String JTAGPos = String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition());
		String BitfileExt = (IsCPLD) ? "jed" : "bit";
		File ScriptFile = FileWriter.GetFilePointer(ScriptPath, script_file,
				MyReporter);
		File VhdlListFile = FileWriter.GetFilePointer(ScriptPath,
				vhdl_list_file, MyReporter);
		File UcfFile = FileWriter.GetFilePointer(UcfPath, ucf_file, MyReporter);
		File DownloadFile = FileWriter.GetFilePointer(ScriptPath,
				download_file, MyReporter);
		if (ScriptFile == null || VhdlListFile == null || UcfFile == null
				|| DownloadFile == null) {
			ScriptFile = new File(ScriptPath + script_file);
			VhdlListFile = new File(ScriptPath + vhdl_list_file);
			UcfFile = new File(UcfPath + ucf_file);
			DownloadFile = new File(ScriptPath + download_file);
			return ScriptFile.exists() && VhdlListFile.exists()
					&& UcfFile.exists() && DownloadFile.exists();
		}
		ArrayList<String> Contents = new ArrayList<String>();
		for (int i = 0; i < Entities.size(); i++) {
			Contents.add(HDLType.toUpperCase() + " work \"" + Entities.get(i)
			+ "\"");
		}
		for (int i = 0; i < Architectures.size(); i++) {
			Contents.add(HDLType.toUpperCase() + " work \""
					+ Architectures.get(i) + "\"");
		}
		if (!FileWriter.WriteContents(VhdlListFile, Contents, MyReporter))
			return false;
		Contents.clear();
		Contents.add("run -top " + ToplevelHDLGeneratorFactory.FPGAToplevelName
				+ " -ofn logisim.ngc -ofmt NGC -ifn "
				+ ScriptPath.replace(ProjectPath, "../") + vhdl_list_file
				+ " -ifmt mixed -p " + GetFPGADeviceString(BoardInfo));
		if (!FileWriter.WriteContents(ScriptFile, Contents, MyReporter))
			return false;
		Contents.clear();
		Contents.add("setmode -bscan");
		if (writeToFlash && BoardInfo.fpga.isFlashDefined()) {
			if (BoardInfo.fpga.getFlashName() == null) {
				MyReporter.AddFatalError("Unable to find the flash on " + BoardInfo.getBoardName());
			}
			String FlashPos = String.valueOf(BoardInfo.fpga.getFlashJTAGChainPosition());
			String McsFile = ScriptPath + File.separator + mcs_file;
			Contents.add("setmode -pff");
			Contents.add("setSubMode -pffserial");
			Contents.add("addPromDevice -p " + JTAGPos + " -size 0 -name " + BoardInfo.fpga.getFlashName());
			Contents.add("addDesign -version 0 -name \"0\"");
			Contents.add("addDeviceChain -index 0");
			Contents.add("addDevice -p " + JTAGPos + " -file " + ToplevelHDLGeneratorFactory.FPGAToplevelName + "." + BitfileExt);
			Contents.add("generate -format mcs -fillvalue FF -output " + McsFile);
			Contents.add("setMode -bs");
			Contents.add("setCable -port auto");
			Contents.add("identify");
			Contents.add("assignFile -p " + FlashPos + " -file " + McsFile);
			Contents.add("program -p " + FlashPos + " -e -v");
		} else {
			Contents.add("setcable -p auto");
			Contents.add("identify");
			if (!IsCPLD) {
				Contents.add("assignFile -p " + JTAGPos + " -file "
						+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "."
						+ BitfileExt);
				Contents.add("program -p " + JTAGPos + " -onlyFpga");
			} else {
				Contents.add("assignFile -p " + JTAGPos + " -file logisim."
						+ BitfileExt);
				Contents.add("program -p " + JTAGPos + " -e");
			}
		}
		Contents.add("quit");
		if (!FileWriter.WriteContents(DownloadFile, Contents, MyReporter))
			return false;
		Contents.clear();
		if (RootNetlist.NumberOfClockTrees() > 0) {
			Contents.add("NET \"" + TickComponentHDLGeneratorFactory.FPGAClock
					+ "\" " + GetXilinxClockPin(BoardInfo) + " ;");
			Contents.add("NET \"" + TickComponentHDLGeneratorFactory.FPGAClock
					+ "\" TNM_NET = \""
					+ TickComponentHDLGeneratorFactory.FPGAClock + "\" ;");
			Contents.add("TIMESPEC \"TS_"
					+ TickComponentHDLGeneratorFactory.FPGAClock
					+ "\" = PERIOD \""
					+ TickComponentHDLGeneratorFactory.FPGAClock + "\" "
					+ GetClockFrequencyString(BoardInfo) + " HIGH 50 % ;");
			Contents.add("");
		}
		Contents.addAll(MapInfo.GetFPGAPinLocs(VendorSoftware.VendorXilinx));
		return FileWriter.WriteContents(UcfFile, Contents, MyReporter);
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

	private static String GetFPGADeviceString(BoardInformation CurrentBoard) {
		StringBuffer result = new StringBuffer();
		result.append(CurrentBoard.fpga.getPart());
		result.append("-");
		result.append(CurrentBoard.fpga.getPackage());
		result.append("-");
		result.append(CurrentBoard.fpga.getSpeedGrade());
		return result.toString();
	}

	private static String GetXilinxClockPin(BoardInformation CurrentBoard) {
		StringBuffer result = new StringBuffer();
		result.append("LOC = \"" + CurrentBoard.fpga.getClockPinLocation()
		+ "\"");
		if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PullUp) {
			result.append(" | PULLUP");
		}
		if (CurrentBoard.fpga.getClockPull() == PullBehaviors.PullDown) {
			result.append(" | PULLDOWN");
		}
		if (CurrentBoard.fpga.getClockStandard() != IoStandards.DefaulStandard
				&& CurrentBoard.fpga.getClockStandard() != IoStandards.Unknown) {
			result.append(" | IOSTANDARD = "
					+ IoStandards.Behavior_strings[CurrentBoard.fpga
					                               .getClockStandard()]);
		}
		return result.toString();
	}

	private final static String vhdl_list_file = "XilinxVHDLList.prj";

	private final static String script_file = "XilinxScript.cmd";

	private final static String ucf_file = "XilinxConstraints.ucf";

	private final static String download_file = "XilinxDownload";

	private final static String mcs_file = "XilinxProm.mcs";

	private final static Integer BUFFER_SIZE = 16 * 1024;

}
