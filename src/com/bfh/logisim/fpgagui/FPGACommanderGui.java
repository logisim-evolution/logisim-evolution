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

package com.bfh.logisim.fpgagui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.SimpleDRCContainer;
import com.bfh.logisim.download.AlteraDownload;
import com.bfh.logisim.download.VivadoDownload;
import com.bfh.logisim.download.XilinxDownload;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardReaderClass;
import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public class FPGACommanderGui implements ActionListener,LibraryListener,ProjectListener,SimulatorListener,CircuitListener,WindowListener,
                                         MouseListener{

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getClickCount()>1) {
			
			if (e.getSource().equals(tabbedPane)) {
				if (tabbedPane.getComponentCount()>0) {
					if (tabbedPane.getSelectedComponent().equals(panelInfos)) {
						InfoWindow.setVisible(true);
						tabbedPane.remove(tabbedPane.getSelectedIndex());
					} else
					if (tabbedPane.getSelectedComponent().equals(panelWarnings)) {
						WarningWindow.setVisible(true);
						tabbedPane.remove(tabbedPane.getSelectedIndex());
					} else
					if (tabbedPane.getSelectedComponent().equals(panelErrors)) {
						ErrorWindow.setVisible(true);
						clearDRCTrace();
						tabbedPane.remove(tabbedPane.getSelectedIndex());
					} else
					if (tabbedPane.getSelectedComponent().equals(panelConsole)) {
						ConsoleWindow.setVisible(true);
						tabbedPane.remove(tabbedPane.getSelectedIndex());
					}
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getSource().equals(Errors)||
			e.getSource().equals(ErrorWindow.getListObject())) {
			clearDRCTrace();
			int idx = -1;
			if (e.getSource().equals(Errors)) 
				idx = Errors.getSelectedIndex();
			else
				idx = ErrorWindow.getListObject().getSelectedIndex();
			if (idx >= 0) {
				if (ErrorsList.getElementAt(idx) instanceof SimpleDRCContainer) {
					GenerateDRCTrace((SimpleDRCContainer)ErrorsList.getElementAt(idx));
				}
			}
		} else 
		if (e.getSource().equals(Warnings)||
			e.getSource().equals(WarningWindow.getListObject())) {
			clearDRCTrace();
			int idx = -1;
			if (e.getSource().equals(Warnings))
				idx = Warnings.getSelectedIndex();
			else
				idx = WarningWindow.getListObject().getSelectedIndex();
			if (idx >= 0)
				if (WarningsList.getElementAt(idx) instanceof SimpleDRCContainer)
					GenerateDRCTrace((SimpleDRCContainer)WarningsList.getElementAt(idx));
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	@Override
	public void windowOpened(WindowEvent e) {
		if (e.getSource().equals(panel)) {
			InfoWindow.setVisible(InfoWindow.IsActivated());
			WarningWindow.setVisible(WarningWindow.IsActivated());
			ErrorWindow.setVisible(ErrorWindow.IsActivated());
			ConsoleWindow.setVisible(ConsoleWindow.IsActivated());
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(InfoWindow)) {
			tabbedPane.add(panelInfos, 0);
			tabbedPane.setTitleAt(0, "Infos (" + consoleInfos.size() + ")");
			tabbedPane.setSelectedIndex(0);
		}
		if (e.getSource().equals(WarningWindow)) {
			int idx = tabbedPane.getComponentCount();
			Set<Component> comps = new HashSet<Component>(Arrays.asList(tabbedPane.getComponents()));
			if (comps.contains(panelConsole))
				idx = tabbedPane.indexOfComponent(panelConsole);
			if (comps.contains(panelErrors))
				idx = tabbedPane.indexOfComponent(panelErrors);
			tabbedPane.add(panelWarnings, idx);
			tabbedPane.setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
		}
		if (e.getSource().equals(ErrorWindow)) {
			int idx = tabbedPane.getComponentCount();
			Set<Component> comps = new HashSet<Component>(Arrays.asList(tabbedPane.getComponents()));
			if (comps.contains(panelConsole))
				idx = tabbedPane.indexOfComponent(panelConsole);
			tabbedPane.add(panelErrors, idx);
			tabbedPane.setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
			clearDRCTrace();
		}
		if (e.getSource().equals(ConsoleWindow)) {
			tabbedPane.add(panelConsole, tabbedPane.getComponentCount());
		}
		if (e.getSource().equals(panel)) {
			InfoWindow.setVisible(false);
			WarningWindow.setVisible(false);
			ErrorWindow.setVisible(false);
			ConsoleWindow.setVisible(false);
			clearDRCTrace();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
		if (e.getSource().equals(panel)) {
			InfoWindow.setVisible(InfoWindow.IsActivated());
			WarningWindow.setVisible(WarningWindow.IsActivated());
			ErrorWindow.setVisible(ErrorWindow.IsActivated());
			ConsoleWindow.setVisible(ConsoleWindow.IsActivated());
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
	 
	
	public void libraryChanged(LibraryEvent event) {
		if (event.getAction() == LibraryEvent.ADD_TOOL
			|| event.getAction() == LibraryEvent.REMOVE_TOOL) {
			RebuildCircuitSelection();
		}
	}

	public void projectChanged(ProjectEvent event) {
		if (event.getAction() == ProjectEvent.ACTION_SET_CURRENT) {
			SetCurrentSheet(event.getCircuit().getName());
		} else if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
			RebuildCircuitSelection();
		}
	}
	
	public void propagationCompleted(SimulatorEvent e) {
	}

	public void simulatorStateChanged(SimulatorEvent e) {
		ChangeTickFrequency();
	}

	public void tickCompleted(SimulatorEvent e) {
	}
	
	public void circuitChanged(CircuitEvent event) {
		int act = event.getAction();

		if (act == CircuitEvent.ACTION_SET_NAME) {
			RebuildCircuitSelection();
		}
		clearDRCTrace();
	}
	

	public static final int FONT_SIZE = 12;
	private JFrame panel;
	private ComponentMapDialog MapPannel;
	private JLabel textMainCircuit = new JLabel("Choose main circuit ");
	private JLabel textTargetBoard = new JLabel("Choose target board ");
	private JLabel textTargetFreq = new JLabel("Choose tick frequency ");
	private JLabel textAnnotation = new JLabel("Annotation method");
	private JLabel boardPic = new JLabel();
	private BoardIcon boardIcon = null;
	private JButton annotateButton = new JButton();
	private JButton validateButton = new JButton();
	private JCheckBox writeToFlash = new JCheckBox("Write to flash?");
	private JComboBox<String> boardsList = new JComboBox<>();
	private JComboBox<String> circuitsList = new JComboBox<>();
	private JComboBox<String> frequenciesList = new JComboBox<>();
	private JComboBox<String> annotationList = new JComboBox<>();
	private JButton HDLType = new JButton();
	private JButton HDLOnly = new JButton();
	private JButton ToolPath = new JButton();
	private JButton Workspace = new JButton();
	private JCheckBox skipHDL = new JCheckBox("Skip VHDL generation?");
	private JTextArea textAreaInfo = new JTextArea(10, 50);
	private JTextArea textAreaConsole = new JTextArea(10, 50);
	private JComponent panelInfos = new JPanel();
	private JComponent panelWarnings = new JPanel();
	private JComponent panelErrors = new JPanel();
	private JComponent panelConsole = new JPanel();
	private static final String OnlyHDLMessage = "Generate HDL only";
	private static final String HDLandDownloadMessage = "Download to board";
	private JTabbedPane tabbedPane = new JTabbedPane();
	private LinkedList<String> consoleInfos = new LinkedList<String>();
	private LinkedList<String> consoleConsole = new LinkedList<String>();
	private Project MyProject;
	private Settings MySettings = new Settings();
	private BoardInformation MyBoardInformation = null;
	private MappableResourcesContainer MyMappableResources;
	private String[] HDLPaths = { Settings.VERILOG.toLowerCase(),
			Settings.VHDL.toLowerCase(), "scripts", "sandbox", "ucf", "xdc"};
	@SuppressWarnings("unused")
	private static final Integer VerilogSourcePath = 0;
	@SuppressWarnings("unused")
	private static final Integer VHDLSourcePath = 1;
	private static final Integer ScriptPath = 2;
	private static final Integer SandboxPath = 3;
	private static final Integer UCFPath = 4;
	private static final Integer XDCPath = 5;
	private FPGAReport MyReporter = new FPGAReport(this);
	
	private FPGACommanderListModel WarningsList = new FPGACommanderListModel();
	private JList<Object> Warnings = new JList<Object>();
	private FPGACommanderListWindow WarningWindow = new FPGACommanderListWindow("FPGACommander: Warnings",Color.ORANGE,true,WarningsList);

	private FPGACommanderListModel ErrorsList = new FPGACommanderListModel();
	private JList<Object> Errors = new JList<Object>();
	private FPGACommanderListWindow ErrorWindow = new FPGACommanderListWindow("FPGACommander: Errors",Color.RED,true,ErrorsList);


	private FPGACommanderTextWindow InfoWindow;
	private FPGACommanderTextWindow ConsoleWindow;
	private boolean DRCTraceActive = false;
	private SimpleDRCContainer ActiveDRCContainer;

	public FPGACommanderGui(Project Main) {
		MyProject = Main;
		InfoWindow = new FPGACommanderTextWindow("FPGACommander: Infos",Color.GRAY,true);
		ConsoleWindow = new FPGACommanderTextWindow("FPGACommander: Console",Color.LIGHT_GRAY,false);
		panel = new JFrame("FPGA Commander : " + MyProject.getLogisimFile().getName());
		panel.setResizable(false);
		panel.setAlwaysOnTop(false);
		panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		panel.addWindowListener(this);
		InfoWindow.addWindowListener(this);
		WarningWindow.addWindowListener(this);
		WarningWindow.setSize(new Dimension(740,400));
		WarningWindow.getListObject().addMouseListener(this);
		ErrorWindow.addWindowListener(this);
		ErrorWindow.setSize(new Dimension(740,400));
		ErrorWindow.getListObject().addMouseListener(this);
		ConsoleWindow.addWindowListener(this);
		
		GridBagLayout thisLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(thisLayout);
		
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);

		// change main circuit
		circuitsList.setEnabled(true);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		textMainCircuit.setEnabled(true);
		panel.add(textMainCircuit, c);
		c.gridx = 1;
		c.gridwidth = 2;
		// circuitsList.addActionListener(this);
		circuitsList.setActionCommand("mainCircuit");
		int i = 0;
		RebuildCircuitSelection();
		MyProject.addProjectListener(this);
		MyProject.getLogisimFile().addLibraryListener(this);
		circuitsList.setActionCommand("Circuit");
		circuitsList.addActionListener(this);
		panel.add(circuitsList, c);

		// Big TODO: add in all classes (Settings and this one) support for
		// board xmls stored on disc (rather than in the resources directory)
		// change target board
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		textTargetBoard.setEnabled(true);
		panel.add(textTargetBoard, c);
		c.gridx = 1;
		c.gridwidth = 2;
		boardsList.addItem("Other");
		i = 1;
		for (String boardname : MySettings.GetBoardNames()) {
			boardsList.addItem(boardname);
			if (boardname.equals(MySettings.GetSelectedBoard())) {
				boardsList.setSelectedIndex(i);
			}
			i++;
		}
		boardsList.setEnabled(true);
		boardsList.addActionListener(this);
		boardsList.setActionCommand("targetBoard");
		panel.add(boardsList, c);

		// select clock frequency
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 4;
		textTargetFreq.setEnabled(true);
		panel.add(textTargetFreq, c);
		frequenciesList.setEnabled(true);
		for (String freq : MenuSimulate.getTickFrequencyStrings()) {
			frequenciesList.addItem(freq);
		}
		for (i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
			if (MenuSimulate.SupportedTickFrequencies[i].equals(MyProject
					.getSimulator().getTickFrequency())) {
				frequenciesList.setSelectedIndex(i);
			}
		}
		frequenciesList.setActionCommand("Frequency");
		frequenciesList.addActionListener(this);
		c.gridx = 1;
		panel.add(frequenciesList, c);
		MyProject.getSimulator().addSimulatorListener(this);

		c.gridx = 2;
		skipHDL.setVisible(true);
		panel.add(skipHDL, c);

		// select annotation level
		c.gridx = 0;
		c.gridy = 5;
		textAnnotation.setEnabled(true);
		panel.add(textAnnotation, c);
		annotationList.addItem("Relabel all components");
		annotationList.addItem("Label only the components without a label");
		annotationList.setSelectedIndex(1);
		c.gridwidth = 2;
		c.gridx = 1;
		panel.add(annotationList, c);

		/* Read the selected board information to retrieve board picture */
		MyBoardInformation = new BoardReaderClass(
				MySettings.GetSelectedBoardFileName()).GetBoardInformation();
		MyBoardInformation
				.setBoardName(boardsList.getSelectedItem().toString());
		boardIcon = new BoardIcon(MyBoardInformation.GetImage());
		// set board image on panel creation
		boardPic.setIcon(boardIcon);
		c.gridx = 3;
		c.gridy = 2;
		c.gridheight = 5;
		// c.gridwidth = 2;
		panel.add(boardPic, c);

		c.gridheight = 1;
		// c.gridwidth = 1;

		// validate button
		validateButton.setActionCommand("Download");
		validateButton.setText("Download");
		validateButton.addActionListener(this);
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 6;
		panel.add(validateButton, c);

		// write to flash
		writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
		writeToFlash.setSelected(false);
		c.gridx = 2;
		c.gridy = 6;
		panel.add(writeToFlash, c);

		// annotate button
		annotateButton.setActionCommand("annotate");
		annotateButton.setText("Annotate");
		annotateButton.addActionListener(this);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 6;
		panel.add(annotateButton, c);

		// HDL Type Button
		HDLType.setText(MySettings.GetHDLType());
		HDLType.setActionCommand("HDLType");
		HDLType.addActionListener(this);
		c.gridx = 0;
		c.gridy = 0;
		panel.add(HDLType, c);

		// HDL Only Radio
		if (Settings.vendors.get(MyBoardInformation.fpga.getVendor()).getToolPath().equals(Settings.Unknown)) {
			if (!MySettings.GetHDLOnly()) {
				MySettings.SetHdlOnly(true);
				MySettings.UpdateSettingsFile();
			}
			HDLOnly.setText(OnlyHDLMessage);
			HDLOnly.setEnabled(false);
		} else if (MySettings.GetHDLOnly()) {
			HDLOnly.setText(OnlyHDLMessage);
		} else {
			HDLOnly.setText(HDLandDownloadMessage);
		}
		HDLOnly.setActionCommand("HDLOnly");
		HDLOnly.addActionListener(this);
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 0;
		panel.add(HDLOnly, c);

		// Tool Path
		ToolPath.setText("Toolpath");
		ToolPath.setActionCommand("ToolPath");
		ToolPath.addActionListener(this);
		c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = 0;
		panel.add(ToolPath, c);

		// Workspace
		Workspace.setText("Workspace");
		Workspace.setActionCommand("Workspace");
		Workspace.addActionListener(this);
		c.gridx = 4;
		c.gridy = 0;
		panel.add(Workspace, c);

		// output console
		Color fg = Color.GRAY;
		Color bg = Color.black;

		textAreaInfo.setForeground(fg);
		textAreaInfo.setBackground(bg);
		textAreaInfo.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		Warnings.setBackground(bg);
		Warnings.setForeground(Color.ORANGE);
		Warnings.setSelectionBackground(Color.ORANGE);
		Warnings.setSelectionForeground(bg);
		Warnings.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		Warnings.setModel(WarningsList);
		Warnings.setCellRenderer(WarningsList.getMyRenderer(true));
		Warnings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Warnings.addMouseListener(this);
		Errors.setBackground(bg);
		Errors.setForeground(Color.RED);
		Errors.setSelectionBackground(Color.RED);
		Errors.setSelectionForeground(bg);
		Errors.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		Errors.setModel(ErrorsList);
		Errors.setCellRenderer(ErrorsList.getMyRenderer(true));
		Errors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Errors.addMouseListener(this);
		
		textAreaConsole.setForeground(Color.LIGHT_GRAY);
		textAreaConsole.setBackground(bg);
		textAreaConsole.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		JScrollPane textMessages = new JScrollPane(textAreaInfo);
		JScrollPane textWarnings = new JScrollPane(Warnings);
		JScrollPane textErrors = new JScrollPane(Errors);
		JScrollPane textConsole = new JScrollPane(textAreaConsole);
		textMessages
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textMessages
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		textWarnings
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textWarnings
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		textErrors
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textErrors
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		textConsole
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textConsole
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		GridLayout consolesLayout = new GridLayout(1, 1);

		panelInfos.setLayout(consolesLayout);
		panelWarnings.setLayout(consolesLayout);
		panelErrors.setLayout(consolesLayout);
		panelConsole.setLayout(consolesLayout);

		panelInfos.add(textMessages);
		panelInfos.setName("Infos (0)");
		panelWarnings.add(textWarnings);
		panelWarnings.setName("Warnings (0)");
		panelErrors.add(textErrors);
		panelErrors.setName("Errors (0)");
		panelConsole.add(textConsole);
		panelConsole.setName("Console");
		
		tabbedPane.add(panelInfos); // index 0
		tabbedPane.add(panelWarnings); // index 1
		tabbedPane.add(panelErrors); // index 2
		tabbedPane.add(panelConsole); // index 3
		tabbedPane.addMouseListener(this);

		textAreaInfo.setEditable(false);
		textAreaConsole.setEditable(false);

		consoleInfos.clear();
		consoleConsole.clear();

		textAreaInfo.setText(null);
		textAreaConsole.setText(null);

		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 5;
		tabbedPane.setPreferredSize(new Dimension(700, 20 * FONT_SIZE));
		panel.add(tabbedPane, c);

		panel.pack();
		/*
		 * panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
		 * panel.getHeight()));
		 */
		panel.setLocationRelativeTo(null);
		panel.setVisible(false);
		
		
		if (MyProject.getLogisimFile().getLoader().getMainFile() != null) {
			MapPannel = new ComponentMapDialog(panel, MyProject
					.getLogisimFile().getLoader().getMainFile()
					.getAbsolutePath());
		} else {
			MapPannel = new ComponentMapDialog(panel, "");
		}
		MapPannel.SetBoardInformation(MyBoardInformation);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("annotate")) {
			Annotate(annotationList.getSelectedIndex() == 0);
		} else if (e.getActionCommand().equals("Workspace")) {
			selectWorkSpace();
		} else if (e.getActionCommand().equals("HDLType")) {
			handleHDLType();
		} else if (e.getActionCommand().equals("ToolPath")) {
			selectToolPath();
		} else if (e.getActionCommand().equals("HDLOnly")) {
			handleHDLOnly();
		} else if (e.getActionCommand().equals("Download")) {
			DownLoad();
		} else if (e.getActionCommand().equals("targetBoard")) {
			if (!boardsList.getSelectedItem().equals("Other")) {
				MySettings.SetSelectedBoard(boardsList.getSelectedItem()
						.toString());
				MySettings.UpdateSettingsFile();
				MyBoardInformation = new BoardReaderClass(
						MySettings.GetSelectedBoardFileName())
						.GetBoardInformation();
				MyBoardInformation.setBoardName(boardsList.getSelectedItem()
						.toString());
				MapPannel.SetBoardInformation(MyBoardInformation);
				boardIcon = new BoardIcon(MyBoardInformation.GetImage());
				boardPic.setIcon(boardIcon);
				boardPic.repaint();
				if (Settings.vendors.get(MyBoardInformation.fpga.getVendor()).getToolPath().equals(Settings.Unknown)) {
					if (!MySettings.GetHDLOnly()) {
						MySettings.SetHdlOnly(true);
						MySettings.UpdateSettingsFile();
					}
					HDLOnly.setText(OnlyHDLMessage);
					HDLOnly.setEnabled(false);
				} else if (MySettings.GetHDLOnly()) {
					HDLOnly.setText(OnlyHDLMessage);
				} else {
					HDLOnly.setText(HDLandDownloadMessage);
				}
				writeToFlash.setSelected(false);
				writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
			} else {
				String NewBoardFileName = GetBoardFile();
				MyBoardInformation = new BoardReaderClass(NewBoardFileName).GetBoardInformation();
				if (MyBoardInformation == null) {
					for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
						if (boardsList.getItemAt(index).equals(MySettings.GetSelectedBoard()))
							boardsList.setSelectedIndex(index);
					this.AddErrors("\""+NewBoardFileName+"\" does not has the proper format for a board file!\n");
				} else {
					String[] Parts = NewBoardFileName.split(File.separator);
					String BoardInfo = Parts[Parts.length-1].replace(".xml", "");
					Boolean CanAdd = true;
					for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
						if (boardsList.getItemAt(index).equals(BoardInfo)) {
							this.AddErrors("A board with the name \""+BoardInfo+"\" already exisits, cannot add new board descriptor\n");
							CanAdd = false;
						}
					if (CanAdd) {
						MySettings.AddExternalBoard(NewBoardFileName);
						MySettings.SetSelectedBoard(BoardInfo);
						MySettings.UpdateSettingsFile();
						boardsList.addItem(BoardInfo);
						for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
							if (boardsList.getItemAt(index).equals(BoardInfo))
								boardsList.setSelectedIndex(index);
						MyBoardInformation.setBoardName(BoardInfo);
						MapPannel.SetBoardInformation(MyBoardInformation);
						boardIcon = new BoardIcon(MyBoardInformation.GetImage());
						boardPic.setIcon(boardIcon);
						boardPic.repaint();
						if (Settings.vendors.get(MyBoardInformation.fpga.getVendor()).getToolPath().equals(Settings.Unknown)) {
							if (!MySettings.GetHDLOnly()) {
								MySettings.SetHdlOnly(true);
								MySettings.UpdateSettingsFile();
							}
							HDLOnly.setText(OnlyHDLMessage);
							HDLOnly.setEnabled(false);
						} else if (MySettings.GetHDLOnly()) {
							HDLOnly.setText(OnlyHDLMessage);
						} else {
							HDLOnly.setText(HDLandDownloadMessage);
						}
						writeToFlash.setSelected(false);
						writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
					} else {
						for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
							if (boardsList.getItemAt(index).equals(MySettings.GetSelectedBoard()))
								boardsList.setSelectedIndex(index);
					}
				}
				
			}
		}
	}

	public void AddConsole(String Message) {
		consoleConsole.add(Message + "\n");
		StringBuffer Lines = new StringBuffer();
		for (String mes : consoleConsole) {
			Lines.append(mes);
		}
		textAreaConsole.setText(Lines.toString());
		ConsoleWindow.add(Lines.toString());
		int idx = tabbedPane.indexOfComponent(panelConsole);
		if (idx >= 0) {
			tabbedPane.setSelectedIndex(idx);
			Rectangle rect = tabbedPane.getBounds();
			rect.x = 0;
			rect.y = 0;
			tabbedPane.paintImmediately(rect);
		}
	}

	public void AddErrors(Object Message) {
		ErrorsList.add(Message);
		int idx = tabbedPane.indexOfComponent(panelErrors);
		if (idx >= 0) {
			tabbedPane.setSelectedIndex(idx);
			tabbedPane.setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
			Rectangle rect = tabbedPane.getBounds();
			rect.x = 0;
			rect.y = 0;
			tabbedPane.paintImmediately(rect);
		}
	}

	public void AddInfo(Object Message) {
		StringBuffer Line = new StringBuffer();
		if (consoleInfos.size() < 9) {
			Line.append("    ");
		} else if (consoleInfos.size() < 99) {
			Line.append("   ");
		} else if (consoleInfos.size() < 999) {
			Line.append("  ");
		} else if (consoleInfos.size() < 9999) {
			Line.append(" ");
		}
		Line.append(Integer.toString(consoleInfos.size() + 1) + "> " + Message.toString()
				+ "\n");
		consoleInfos.add(Line.toString());
		Line.setLength(0);
		for (String mes : consoleInfos) {
			Line.append(mes);
		}
		textAreaInfo.setText(Line.toString());
		InfoWindow.add(Line.toString());
		int idx = tabbedPane.indexOfComponent(panelInfos);
		if (idx >= 0) {
			tabbedPane.setSelectedIndex(idx);
			tabbedPane.setTitleAt(idx, "Infos (" + consoleInfos.size() + ")");
			Rectangle rect = tabbedPane.getBounds();
			rect.x = 0;
			rect.y = 0;
			tabbedPane.paintImmediately(rect);
		}
	}

	public void AddWarning(Object Message) {
		WarningsList.add(Message);
		int idx = tabbedPane.indexOfComponent(panelWarnings);
		if (idx >= 0) {
			tabbedPane.setSelectedIndex(idx);
			tabbedPane.setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
			Rectangle rect = tabbedPane.getBounds();
			rect.x = 0;
			rect.y = 0;
			tabbedPane.paintImmediately(rect);
		}
	}

	private void Annotate(boolean ClearExistingLabels) {
		clearAllMessages();
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		if (root != null) {
			if (ClearExistingLabels) {
				root.ClearAnnotationLevel();
			}
			root.Annotate(ClearExistingLabels, MyReporter);
			MyReporter.AddInfo("Annotation done");
			/* TODO: Dirty hack, see Circuit.java function Annotate for details */
			MyProject.repaintCanvas();
			MyProject.getLogisimFile().setDirty(true);
		}
	}

	private void ChangeTickFrequency() {
		for (int i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
			if (MenuSimulate.SupportedTickFrequencies[i].equals(MyProject
					.getSimulator().getTickFrequency())) {
				if (i != frequenciesList.getSelectedIndex()) {
					frequenciesList.setSelectedIndex(i);
				}
				break;
			}
		}
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

	private void clearAllMessages() {
		clearDRCTrace();
		textAreaInfo.setText(null);
		consoleInfos.clear();
		int idx = tabbedPane.indexOfComponent(panelInfos);
		if (idx >= 0) {
			tabbedPane.setTitleAt(idx, "Infos (" + consoleInfos.size() + ")");
			tabbedPane.setSelectedIndex(idx);
		}
		WarningsList.clear();
		idx = tabbedPane.indexOfComponent(panelWarnings);
		if (idx >= 0)
			tabbedPane.setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
		ErrorsList.clear();
		idx = tabbedPane.indexOfComponent(panelErrors);
		if (idx >= 0)
			tabbedPane.setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
		InfoWindow.clear();
		ConsoleWindow.clear();
		Rectangle rect = tabbedPane.getBounds();
		rect.x = 0;
		rect.y = 0;
		tabbedPane.paintImmediately(rect);
	}

	public void ClearConsole() {
		consoleConsole.clear();
		int idx = tabbedPane.indexOfComponent(panelConsole);
		if (idx >= 0)
			tabbedPane.setSelectedIndex(idx);
		textAreaConsole.setText(null);
		Rectangle rect = tabbedPane.getBounds();
		rect.x = 0;
		rect.y = 0;
		tabbedPane.paintImmediately(rect);
	}

	private void DownLoad() {
		if (MySettings.GetHDLOnly() || !skipHDL.isSelected()) {
			if (!performDRC()) {
				return;
			}
			if (!MapDesign()) {
				return;
			}
			if (!writeHDL()) {
				return;
			}
		}
		if (!MySettings.GetHDLOnly() || skipHDL.isSelected()) {
			DownLoadDesign(MySettings.GetHDLOnly(), skipHDL.isSelected());
		}
	}

	private void DownLoadDesign(boolean generateOnly, boolean downloadOnly) {
		if (generateOnly && downloadOnly) {
			MyReporter.AddError("Can not have skip VHDL generation and generate HDL only in the same time...");
			return;
		}
		if (!MapPannel.isDoneAssignment()) {
			MyReporter.AddError("Download to board canceled");
			return;
		}

		String CircuitName = circuitsList.getSelectedItem().toString();
		String ProjectDir = MySettings.GetWorkspacePath() + File.separator
				+ MyProject.getLogisimFile().getName();
		if (!ProjectDir.endsWith(File.separator)) {
			ProjectDir += File.separator;
		}
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
				+ File.separator;
		String SourcePath = ProjectDir + MySettings.GetHDLType().toLowerCase()
				+ File.separator;
		ArrayList<String> Entities = new ArrayList<String>();
		ArrayList<String> Behaviors = new ArrayList<String>();
		GetVHDLFiles(ProjectDir, SourcePath, Entities, Behaviors,
				MySettings.GetHDLType());
		if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera) {
			if (AlteraDownload.GenerateQuartusScript(MyReporter, ProjectDir
					+ HDLPaths[ScriptPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					MySettings.GetHDLType())) {
				AlteraDownload.Download(MySettings, ProjectDir
						+ HDLPaths[ScriptPath] + File.separator, SourcePath,
						ProjectDir + HDLPaths[SandboxPath] + File.separator,
						MyReporter);
			}
		} else if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorXilinx) {
			if (XilinxDownload.GenerateISEScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[UCFPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					MySettings.GetHDLType(),
					writeToFlash.isSelected())
					&& !generateOnly) {
				XilinxDownload.Download(MySettings, MyBoardInformation,
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[UCFPath] + File.separator,
						ProjectDir, ProjectDir + HDLPaths[SandboxPath]
								+ File.separator, MyReporter);
			}
		} else if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorVivado) {
			if (VivadoDownload.GenerateScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[XDCPath] + File.separator,
					ProjectDir + HDLPaths[SandboxPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					MySettings.GetHDLType(),
					writeToFlash.isSelected())
					&& !generateOnly) {
				VivadoDownload.Download(
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[SandboxPath] + File.separator,
						MyReporter, downloadOnly);
			}
		}
	}

	private boolean GenDirectory(String dir) {
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
				String EntityMask = (HDLType.equals(Settings.VHDL)) ? FileWriter.EntityExtension
						+ ".vhd"
						: ".v";
				String ArchitecturMask = (HDLType.equals(Settings.VHDL)) ? FileWriter.ArchitectureExtension
						+ ".vhd"
						: "#not_searched#";
				if (thisFile.getName().endsWith(EntityMask)) {
					Entities.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
					// Entities.add((Path+File.separator+thisFile.getName()).replace(SourcePath,
					// "../"));
				} else if (thisFile.getName().endsWith(ArchitecturMask)) {
					Behaviors.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
					// Behaviors.add((Path+File.separator+thisFile.getName()).replace(SourcePath,
					// "../"));
				}
			}
		}
	}

	private void handleHDLOnly() {
		if (HDLOnly.getText().equals(OnlyHDLMessage)) {
			HDLOnly.setText(HDLandDownloadMessage);
			MySettings.SetHdlOnly(false);
		} else {
			HDLOnly.setText(OnlyHDLMessage);
			MySettings.SetHdlOnly(true);
		}
	}

	private void handleHDLType() {
		if (MySettings.GetHDLType().equals(Settings.VHDL)) {
			MySettings.SetHDLType(Settings.VERILOG);
		} else {
			MySettings.SetHDLType(Settings.VHDL);
		}
		HDLType.setText(MySettings.GetHDLType());
		if (!MySettings.UpdateSettingsFile()) {
			AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
		} else {
			AddInfo("Updated the FPGACommander settings file");
		}
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		if (root != null) {
			root.ClearAnnotationLevel();
		}
	}

	private boolean MapDesign() {
		String CircuitName = circuitsList.getSelectedItem().toString();
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

		MapPannel.SetBoardInformation(MyBoardInformation);
		MapPannel.SetMappebleComponents(MyMappableResources);
		panel.setVisible(false);
		MapPannel.SetVisible(true);
		panel.setVisible(true);
		if (MyMappableResources.UnmappedList().isEmpty()) {
			MyMappableResources.BuildIOMappingInformation();
			return true;
		}

		MyReporter
				.AddError("Not all IO components have been mapped to the board "
						+ MyBoardInformation.getBoardName()
						+ " please map all components to continue!");
		return false;
	}

	private boolean performDRC() {
		clearAllMessages();
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		ArrayList<String> SheetNames = new ArrayList<String>();
		int DRCResult = Netlist.DRC_PASSED;
		if (root == null) {
			DRCResult |= Netlist.DRC_ERROR;
		} else {
			root.getNetList().clear();
			DRCResult = root.getNetList().DesignRuleCheckResult(MyReporter,
					HDLType.getText(), true, SheetNames);
		}
		return (DRCResult == Netlist.DRC_PASSED);
	}

	private void RebuildCircuitSelection() {
		circuitsList.removeAllItems();
		panel.setTitle("FPGA Commander : "
				+ MyProject.getLogisimFile().getName());
		int i = 0;
		for (Circuit thisone : MyProject.getLogisimFile().getCircuits()) {
			circuitsList.addItem(thisone.getName());
			thisone.removeCircuitListener(this);
			thisone.addCircuitListener(this);
			if (thisone.getName().equals(
					MyProject.getCurrentCircuit().getName())) {
				circuitsList.setSelectedIndex(i);
			}
			i++;
		}
	}

	private void selectToolPath() {
		String ToolPath = Settings.vendors.get(MyBoardInformation.fpga.getVendor()).getToolPath();
		JFileChooser fc = new JFileChooser(ToolPath);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File test = new File(ToolPath);
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle(FPGAClass.Vendors[MyBoardInformation.fpga.getVendor()]
				+ " Design Suite Path Selection");
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			ToolPath = file.getPath();
			if (!ToolPath.endsWith(File.separator)) {
				ToolPath += File.separator;
			}
			if (MySettings.setToolPath(MyBoardInformation.fpga.getVendor(), ToolPath)) {
				HDLOnly.setEnabled(true);
				MySettings.SetHdlOnly(false);
				HDLOnly.setText(HDLandDownloadMessage);
				if (!MySettings.UpdateSettingsFile()) {
					AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
				} else {
					AddInfo("Updated the FPGACommander settings file");
				}

			} else {
				AddErrors("***FATAL*** Required programs of the " +
						Settings.vendors.get(MyBoardInformation.fpga.getVendor()).getName()
						+ " toolsuite not found! Ignoring update.");
			}
		}
	}
	
	private String GetBoardFile() {
		JFileChooser fc = new JFileChooser(MySettings.GetWorkspacePath());
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Board files", "xml", "xml");
		fc.setFileFilter(filter);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File test = new File(MySettings.GetWorkspacePath());
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle("Board description selection");
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			return file.getPath();
		} else return "";
	}

	private void selectWorkSpace() {
		JFileChooser fc = new JFileChooser(MySettings.GetWorkspacePath());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File test = new File(MySettings.GetWorkspacePath());
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle("Workspace Directory Selection");
		boolean ValidWorkpath = false;
		while (!ValidWorkpath) {
			int retval = fc.showOpenDialog(null);
			if (retval != JFileChooser.APPROVE_OPTION)
				return;
			if (fc.getSelectedFile().getAbsolutePath().contains(" ")) {
				JOptionPane.showMessageDialog(null,"Workspace directory may not contain spaces!","Workspace Directory Selection",JOptionPane.ERROR_MESSAGE);
			} else {
				ValidWorkpath = true;
			}
		}
		File file = fc.getSelectedFile();
		if (file.getPath().endsWith(File.separator)) {
			MySettings.SetWorkspacePath(file.getPath());
		} else {
			MySettings.SetWorkspacePath(file.getPath() + File.separator);
		}
		if (!MySettings.UpdateSettingsFile()) {
			AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
		} else {
			AddInfo("Updated the FPGACommander settings file");
		}
	}

	private void SetCurrentSheet(String Name) {
		for (int i = 0; i < circuitsList.getItemCount(); i++) {
			if (circuitsList.getItemAt(i).equals(Name)) {
				circuitsList.setSelectedIndex(i);
				circuitsList.repaint();
				return;
			}
		}
	}

	public void ShowGui() {
		if (!panel.isVisible()) {
			panel.setVisible(true);
		} else {
			panel.setVisible(false);
			panel.setVisible(true);
		}
	}

	private boolean writeHDL() {
		String CircuitName = circuitsList.getSelectedItem().toString();
		if (!GenDirectory(MySettings.GetWorkspacePath() + File.separator
				+ MyProject.getLogisimFile().getName())) {
			MyReporter.AddFatalError("Unable to create directory: \""
					+ MySettings.GetWorkspacePath() + File.separator
					+ MyProject.getLogisimFile().getName() + "\"");
			return false;
		}
		String ProjectDir = MySettings.GetWorkspacePath() + File.separator
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
				.getHDLGenerator(MySettings.GetHDLType(),
						RootSheet.getStaticAttributes());
		if (Worker == null) {
			MyReporter
					.AddFatalError("Internal error on HDL generation, null pointer exception");
			return false;
		}
		if (!Worker.GenerateAllHDLDescriptions(GeneratedHDLComponents,
				ProjectDir, null, MyReporter, MySettings.GetHDLType())) {
			return false;
		}
		/* Here we generate the top-level shell */
		if (RootSheet.getNetList().NumberOfClockTrees() > 0) {
			TickComponentHDLGeneratorFactory Ticker = new TickComponentHDLGeneratorFactory(
					MyBoardInformation.fpga.getClockFrequency(),
					MenuSimulate.SupportedTickFrequencies[frequenciesList
							.getSelectedIndex()]/* , boardFreq.isSelected() */);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
							+ Ticker.GetRelativeDirectory(MySettings
									.GetHDLType()), Ticker.GetEntity(
							RootSheet.getNetList(), null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							MySettings.GetHDLType()), Ticker
							.getComponentStringIdentifier(), MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ Ticker.GetRelativeDirectory(MySettings.GetHDLType()),
					Ticker.GetArchitecture(RootSheet.getNetList(), null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							MySettings.GetHDLType()), Ticker
							.getComponentStringIdentifier(), MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			HDLGeneratorFactory ClockGen = RootSheet
					.getNetList()
					.GetAllClockSources()
					.get(0)
					.getFactory()
					.getHDLGenerator(
							MySettings.GetHDLType(),
							RootSheet.getNetList().GetAllClockSources().get(0)
									.getAttributeSet());
			String CompName = RootSheet.getNetList().GetAllClockSources()
					.get(0).getFactory().getHDLName(null);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
							+ ClockGen.GetRelativeDirectory(MySettings
									.GetHDLType()), ClockGen.GetEntity(
							RootSheet.getNetList(), null, CompName, MyReporter,
							MySettings.GetHDLType()), CompName, MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ ClockGen.GetRelativeDirectory(MySettings.GetHDLType()),
					ClockGen.GetArchitecture(RootSheet.getNetList(), null,
							CompName, MyReporter, MySettings.GetHDLType()),
					CompName, MyReporter, MySettings.GetHDLType())) {
				return false;
			}
		}
		Worker = new ToplevelHDLGeneratorFactory(
				MyBoardInformation.fpga.getClockFrequency(),
				MenuSimulate.SupportedTickFrequencies[frequenciesList
						.getSelectedIndex()], RootSheet, MyMappableResources);
		if (!AbstractHDLGeneratorFactory.WriteEntity(
				ProjectDir
						+ Worker.GetRelativeDirectory(MySettings.GetHDLType()),
				Worker.GetEntity(RootSheet.getNetList(), null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, MySettings.GetHDLType()), Worker
						.getComponentStringIdentifier(), MyReporter, MySettings
						.GetHDLType())) {
			return false;
		}
		if (!AbstractHDLGeneratorFactory.WriteArchitecture(
				ProjectDir
						+ Worker.GetRelativeDirectory(MySettings.GetHDLType()),
				Worker.GetArchitecture(RootSheet.getNetList(), null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, MySettings.GetHDLType()), Worker
						.getComponentStringIdentifier(), MyReporter, MySettings
						.GetHDLType())) {
			return false;
		}

		return true;
	}

	private void clearDRCTrace() {
		if (DRCTraceActive) {
			ActiveDRCContainer.ClearMarks();
			DRCTraceActive = false;
			MyProject.repaintCanvas();
		}
	}
	
	private void GenerateDRCTrace(SimpleDRCContainer dc) {
		DRCTraceActive = true;
		ActiveDRCContainer = dc;
		dc.MarkComponents();
		if (dc.HasCircuit()) 
			if (!MyProject.getCurrentCircuit().equals(dc.GetCircuit()))
				MyProject.setCurrentCircuit(dc.GetCircuit());
		MyProject.repaintCanvas();
	}
}
