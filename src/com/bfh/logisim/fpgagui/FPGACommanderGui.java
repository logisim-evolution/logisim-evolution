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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.BorderFactory;
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

import com.bfh.logisim.designrulecheck.SimpleDRCContainer;
import com.bfh.logisim.fpgaboardeditor.BoardReaderClass;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public class FPGACommanderGui extends FPGACommanderBase implements ActionListener,LibraryListener,ProjectListener,SimulatorListener,CircuitListener,WindowListener,
MouseListener,PreferenceChangeListener {

	@Override
	public void preferenceChange(PreferenceChangeEvent pce) {
		String property = pce.getKey();
		if (property.equals(AppPreferences.HDL_Type.getIdentifier()))
			HDLType.setText(AppPreferences.HDL_Type.get());
		HandleHDLOnly();
		if (property.equals(AppPreferences.SelectedBoard.getIdentifier())) {
			MyBoardInformation = new BoardReaderClass(
					AppPreferences.Boards.GetSelectedBoardFileName())
					.GetBoardInformation();
			MyBoardInformation.setBoardName(AppPreferences.SelectedBoard.get());
			MapPannel.SetBoardInformation(MyBoardInformation);
			boardIcon = new BoardIcon(MyBoardInformation.GetImage());
			boardPic.setIcon(boardIcon);
			boardPic.repaint();
			UpdateFrequencies();
			CustFreqPannel.Reset(MyBoardInformation.fpga.getClockFrequency());
			HandleHDLOnly();
			writeToFlash.setSelected(false);
			writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
		}
	}

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


	@Override
	public void libraryChanged(LibraryEvent event) {
		if (event.getAction() == LibraryEvent.ADD_TOOL
				|| event.getAction() == LibraryEvent.REMOVE_TOOL) {
			RebuildCircuitSelection();
		}
	}

	@Override
	public void projectChanged(ProjectEvent event) {
		if (event.getAction() == ProjectEvent.ACTION_SET_CURRENT) {
			SetCurrentSheet(event.getCircuit().getName());
		} else if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
			RebuildCircuitSelection();
		}
	}

	@Override
	public void propagationCompleted(SimulatorEvent e) {
	}

	@Override
	public void simulatorStateChanged(SimulatorEvent e) {
		ChangeTickFrequency();
	}

	@Override
	public void tickCompleted(SimulatorEvent e) {
	}

	@Override
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
	private CustomFrequencySelDialog CustFreqPannel;
	private JLabel textMainCircuit = new JLabel("Choose main circuit ");
	private JLabel textTargetBoard = new JLabel("Choose target board ");
	private JLabel textTargetFreq = new JLabel("Choose tick frequency ");
	private JLabel textAnnotation = new JLabel("Annotation method");
	private JLabel boardPic = new JLabel();
	private BoardIcon boardIcon = null;
	private JButton annotateButton = new JButton();
	private JButton validateButton = new JButton();
	private JCheckBox writeToFlash = new JCheckBox("Write to flash?");
	private JComboBox<String> circuitsList = new JComboBox<>();
	private JComboBox<String> frequenciesList = new JComboBox<>();
	private JComboBox<String> annotationList = new JComboBox<>();
	private JLabel HDLType = new JLabel();
	private JLabel HDLOnly = new JLabel();
	private JButton ToolPath = new JButton();
	private JButton Workspace = new JButton();
	private JCheckBox skipHDL = new JCheckBox("Skip VHDL generation?");
	private JTextArea textAreaInfo = new JTextArea(10, 50);
	private JTextArea textAreaConsole = new JTextArea(10, 50);
	private JComponent panelInfos = new JPanel();
	private JComponent panelWarnings = new JPanel();
	private JComponent panelErrors = new JPanel();
	private JComponent panelConsole = new JPanel();
	private static final String SelectToolPathMessage = "Select Toolpath to Download";
	private static final String OnlyHDLMessage = "Generate HDL only";
	private static final String HDLandDownloadMessage = "Download to board";
	private JTabbedPane tabbedPane = new JTabbedPane();
	private LinkedList<String> consoleInfos = new LinkedList<String>();
	private LinkedList<String> consoleConsole = new LinkedList<String>();
	@SuppressWarnings("unused")
	private static final Integer VerilogSourcePath = 0;

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
	
	private void UpdateFrequencies() {
		long Clockfreq = MyBoardInformation.fpga.getClockFrequency();
		long Freq = 512000000;
		int added = 1;
		frequenciesList.setSelectedIndex(0);
		for (int i = frequenciesList.getItemCount()-1 ; i > 0 ; i-- )
			frequenciesList.removeItemAt(i);
		while (Freq > 4100) {
			if ((Clockfreq/Freq)>=4) {
				if (Freq >= 1000000) {
					frequenciesList.addItem(Integer.toString((int) (Freq/1000000))+" MHz");
					added++;
				} else {
					frequenciesList.addItem(Integer.toString((int) (Freq/1000))+" KHz");
					added++;
				}
			}
			if (Freq == 1000000)
				Freq = 512000;
			else
				Freq /=2 ;
		}
		for (String freq : MenuSimulate.getTickFrequencyStrings()) {
			frequenciesList.addItem(freq);
		}
		for (int i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
			if (MenuSimulate.SupportedTickFrequencies[i].equals(MyProject
					.getSimulator().getTickFrequency())) {
				frequenciesList.setSelectedIndex(i+added);
			}
		}
	}

	public FPGACommanderGui(Project Main) {
		MyReporter = new FPGAReportGui(this);
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
		RebuildCircuitSelection();
		MyProject.addProjectListener(this);
		MyProject.getLogisimFile().addLibraryListener(this);
		circuitsList.setActionCommand("Circuit");
		circuitsList.addActionListener(this);
		panel.add(circuitsList, c);

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		textTargetBoard.setEnabled(true);
		panel.add(textTargetBoard, c);
		c.gridx = 1;
		c.gridwidth = 2;
		panel.add(AppPreferences.Boards.BoardSelector(), c);

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
				AppPreferences.Boards.GetSelectedBoardFileName()).GetBoardInformation();
		MyBoardInformation
		.setBoardName(AppPreferences.SelectedBoard.get());
		boardIcon = new BoardIcon(MyBoardInformation.GetImage());
		// set board image on panel creation
		boardPic.setIcon(boardIcon);
		c.gridx = 3;
		c.gridy = 2;
		c.gridheight = 5;
		panel.add(boardPic, c);

		c.gridheight = 1;

		// select clock frequency
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 4;
		textTargetFreq.setEnabled(true);
		panel.add(textTargetFreq, c);
		frequenciesList.addActionListener(this);
		frequenciesList.setEnabled(true);
		frequenciesList.addItem(Strings.get("Custom"));
		UpdateFrequencies();
		frequenciesList.setActionCommand("Frequency");
		c.gridx = 1;
		panel.add(frequenciesList, c);
		MyProject.getSimulator().addSimulatorListener(this);

		c.gridx = 2;
		skipHDL.setVisible(true);
		panel.add(skipHDL, c);

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
		HDLType.setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		HDLType.setHorizontalAlignment(JLabel.CENTER);
		HDLType.setForeground(Color.BLUE);
		HDLType.setText(AppPreferences.HDL_Type.get());
		c.gridx = 0;
		c.gridy = 0;
		panel.add(HDLType, c);

		// HDL Only Radio
		HandleHDLOnly();
		HDLOnly.setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		HDLOnly.setHorizontalAlignment(JLabel.CENTER);
		HDLOnly.setForeground(Color.BLUE);
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
		CustFreqPannel = new CustomFrequencySelDialog(panel,MyBoardInformation.fpga.getClockFrequency());
		AppPreferences.getPrefs().addPreferenceChangeListener(this);
	}

	private void HandleHDLOnly() {
		if (!VendorSoftware.toolsPresent(MyBoardInformation.fpga.getVendor(),
				VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor()))) {
			HDLOnly.setText(SelectToolPathMessage);
		} else if (!AppPreferences.DownloadToBoard.get()) {
			HDLOnly.setText(OnlyHDLMessage);
		} else {
			HDLOnly.setText(HDLandDownloadMessage);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("annotate")) {
			Annotate(annotationList.getSelectedIndex() == 0);
		} else if (e.getActionCommand().equals("Workspace")) {
			selectWorkSpace(panel);
		} else if (e.getActionCommand().equals("ToolPath")) {
			selectToolPath(MyBoardInformation.fpga.getVendor());
			HandleHDLOnly();
		} else if (e.getActionCommand().equals("Download")) {
			DownLoad(skipHDL.isSelected(), circuitsList.getSelectedItem().toString());
		}
	}

	private boolean guiDRC() {
		clearAllMessages();
		String CircuitName = circuitsList.getSelectedItem().toString();

		clearAllMessages();
		return  performDRC(CircuitName, HDLType.getText());
	}

	private boolean guiMapDesign(String CircuitName) {

		if (!MapDesign(CircuitName)) {
			return false;
		}

		MapPannel.SetBoardInformation(MyBoardInformation);
		MapPannel.SetMappebleComponents(MyMappableResources);
		panel.setVisible(false);
		MapPannel.SetVisible(true);
		panel.setVisible(true);

		if (MapDesignCheckIOs()) {
			return true;
		}

		MyReporter
		.AddError("Not all IO components have been mapped to the board "
				+ MyBoardInformation.getBoardName()
				+ " please map all components to continue!");

		return false;
	}
	
	private double GetTickfrequency() {
		double ret = 0.0;
		if (frequenciesList.getSelectedIndex()==0) {
			panel.setVisible(false);
			CustFreqPannel.setVisible(true);
//			panel.setVisible(true);
			// Here a custom index is specified
			return CustFreqPannel.GetFrequency();
		} else {
			String TickIndex = frequenciesList.getSelectedItem().toString();
			int i = 0;
			boolean divide = false;
			while (i<TickIndex.length() && TickIndex.charAt(i) != ' ') {
				if (TickIndex.charAt(i) == '.') {
					divide = true;
				} else {
					if (!divide)
						ret *= 10.0;
					ret += (double) (TickIndex.charAt(i)-'0');
					if (divide)
						ret /= 10.0;
				}
				i++;
			}
			while (i<TickIndex.length() && TickIndex.charAt(i) == ' ') {
				i++;
			}
			if (i==TickIndex.length())
				return ret;
			if (TickIndex.charAt(i)=='M')
				ret *= 1000000.0;
			if (TickIndex.charAt(i)=='K')
				ret *= 1000.0;
		}
		return ret;
	}

	@Override
	protected boolean DownLoad(boolean skipVHDL, String CircuitName) {
		if (!canDownload() || !skipVHDL ) {
			if (!guiDRC()) {
				return false;
			}
			double frequency = GetTickfrequency();
			if (!guiMapDesign(CircuitName)) {
				return false;
			}
			if (!writeHDL(circuitsList.getSelectedItem().toString(),frequency)) {
				return false;
			}


			if (!MapPannel.isDoneAssignment()) {
				MyReporter.AddError("Download to board canceled");
				return false;
			}

			if (canDownload() || skipHDL.isSelected()) {
				return DownLoadDesign(!canDownload(), skipHDL.isSelected(),
						circuitsList.getSelectedItem().toString(), writeToFlash.isSelected(), true);
			}
		}

		return false;
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
			root.Annotate(ClearExistingLabels, MyReporter,false);
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

	public static void selectToolPath(char vendor) {
		String ToolPath = VendorSoftware.GetToolPath(vendor);
		if (ToolPath == null)
			return;
		JFileChooser fc = new JFileChooser(ToolPath);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File test = new File(ToolPath);
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle(VendorSoftware.Vendors[vendor]
				+ " Design Suite Path Selection");
		int retval;
		boolean ok = false;
		do {
			retval = fc.showOpenDialog(null);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				ToolPath = file.getPath();
				if (!ToolPath.endsWith(File.separator)) {
					ToolPath += File.separator;
				}
				if (VendorSoftware.setToolPath(vendor, ToolPath)) {
					ok = true;
				} else {
					JOptionPane.showMessageDialog(null,"Required tools not found in Directory \""+ToolPath+"\"!",
							"Toolpath Selection",JOptionPane.ERROR_MESSAGE);
				}
			} else ok=true;
		} while (!ok);
	}

	public static void selectWorkSpace(Component parentComponent) {
		JFileChooser fc = new JFileChooser(AppPreferences.FPGA_Workspace.get());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File test = new File(AppPreferences.FPGA_Workspace.get());
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
				JOptionPane.showMessageDialog(parentComponent,"Workspace directory may not contain spaces!","Workspace Directory Selection",JOptionPane.ERROR_MESSAGE);
			} else {
				ValidWorkpath = true;
			}
		}
		File file = fc.getSelectedFile();
		if (file.getPath().endsWith(File.separator)) {
			AppPreferences.FPGA_Workspace.set(file.getPath());
		} else {
			AppPreferences.FPGA_Workspace.set(file.getPath() + File.separator);
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
