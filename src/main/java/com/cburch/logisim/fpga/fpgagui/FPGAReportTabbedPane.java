package com.cburch.logisim.fpga.fpgagui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.text.DefaultCaret;

import com.cburch.logisim.fpga.designrulecheck.SimpleDRCContainer;
import com.cburch.logisim.proj.Project;

public class FPGAReportTabbedPane  extends JTabbedPane implements MouseListener,WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int FONT_SIZE = 12;
	private static GridLayout consolesLayout = new GridLayout(1, 1);
	private static int InfoTabIndex = 0;
	private static int WarningsTabIndex = 1;
	private static int ErrorsTabIndex = 2;
	private static int ConsoleTabIndex = 3;

	private JTextArea textAreaInfo;
	private JComponent panelInfos;
	private ArrayList<String> InfoMessages;
	private FPGACommanderTextWindow InfoWindow;
	
	private JList<Object> Warnings;
	private JComponent panelWarnings;
	private FPGACommanderListModel WarningsList;
	private FPGACommanderListWindow WarningsWindow;
	
	private JList<Object> Errors;
	private JComponent panelErrors;
	private FPGACommanderListModel ErrorsList;
	private FPGACommanderListWindow ErrorsWindow;

	private FPGACommanderTextWindow ConsoleWindow;
	private JTextArea textAreaConsole;
	private JComponent panelConsole;
	private ArrayList<String> ConsoleMessages;

	private boolean DRCTraceActive = false;
	private SimpleDRCContainer ActiveDRCContainer;

	private Project MyProject;

	public FPGAReportTabbedPane(Project MyProject) {
		super();
		this.MyProject = MyProject;
		/* first we setup all info for the first tab, the Information window */
		InfoMessages = new ArrayList<String>();
		textAreaInfo = new JTextArea(10, 50);
		textAreaInfo.setForeground(Color.GRAY);
		textAreaInfo.setBackground(Color.BLACK);
		textAreaInfo.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		textAreaInfo.setEditable(false);
		textAreaInfo.setText(null);
		DefaultCaret caret = (DefaultCaret)textAreaInfo.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		JScrollPane textMessages = new JScrollPane(textAreaInfo);
		textMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelInfos = new JPanel();
		panelInfos.setLayout(consolesLayout);
		panelInfos.add(textMessages);
		panelInfos.setName("Infos (0)");
		add(panelInfos,InfoTabIndex);
		
		/* now we setup the Warning window */
		WarningsList = new FPGACommanderListModel();
		Warnings = new JList<Object>();
		Warnings.setBackground(Color.BLACK);
		Warnings.setForeground(Color.ORANGE);
		Warnings.setSelectionBackground(Color.ORANGE);
		Warnings.setSelectionForeground(Color.BLACK);
		Warnings.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		Warnings.setModel(WarningsList);
		Warnings.setCellRenderer(WarningsList.getMyRenderer(true));
		Warnings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Warnings.addMouseListener(this);
		JScrollPane textWarnings = new JScrollPane(Warnings);
		textWarnings.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textWarnings.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelWarnings = new JPanel();
		panelWarnings.setLayout(consolesLayout);
		panelWarnings.add(textWarnings);
		panelWarnings.setName("Warnings (0)");
		add(panelWarnings,WarningsTabIndex);
		WarningsWindow = new FPGACommanderListWindow("FPGACommander: Warnings",Color.ORANGE,true,WarningsList);
		WarningsWindow.setSize(new Dimension(740,400));
		WarningsWindow.addWindowListener(this);
		WarningsWindow.getListObject().addMouseListener(this);

		
		/* here we setup the Error window */
		ErrorsList = new FPGACommanderListModel();
		Errors = new JList<Object>();
		Errors.setBackground(Color.BLACK);
		Errors.setForeground(Color.RED);
		Errors.setSelectionBackground(Color.RED);
		Errors.setSelectionForeground(Color.BLACK);
		Errors.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		Errors.setModel(ErrorsList);
		Errors.setCellRenderer(ErrorsList.getMyRenderer(true));
		Errors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Errors.addMouseListener(this);
		JScrollPane textErrors = new JScrollPane(Errors);
		textErrors.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textErrors.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelErrors = new JPanel();
		panelErrors.setLayout(consolesLayout);
		panelErrors.add(textErrors);
		panelErrors.setName("Errors (0)");
		add(panelErrors,ErrorsTabIndex);
		ErrorsWindow  = new FPGACommanderListWindow("FPGACommander: Errors",Color.RED,true,ErrorsList);
		ErrorsWindow.addWindowListener(this);
		ErrorsWindow.setSize(new Dimension(740,400));
		ErrorsWindow.getListObject().addMouseListener(this);
		
		/* finally we define the console window */
		ConsoleMessages = new ArrayList<String>();
		textAreaConsole = new JTextArea(10, 50);
		textAreaConsole.setForeground(Color.LIGHT_GRAY);
		textAreaConsole.setBackground(Color.BLACK);
		textAreaConsole.setFont(new Font("monospaced", Font.PLAIN, FONT_SIZE));
		textAreaConsole.setEditable(false);
		textAreaConsole.setText(null);
		caret = (DefaultCaret)textAreaConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		JScrollPane textConsole = new JScrollPane(textAreaConsole);
		textConsole.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textConsole.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelConsole = new JPanel();
		panelConsole.setLayout(consolesLayout);
		panelConsole.add(textConsole);
		panelConsole.setName("Console");
		add(panelConsole,ConsoleTabIndex);
		
		addMouseListener(this);
		setPreferredSize(new Dimension(700, 20 * FONT_SIZE));
	}
	
	private String GetAllignmentSpaces(int index) {
		if (index < 9) {
			return "    "+index;
		} else if (index < 99) {
			return "   "+index;
		} else if (index < 999) {
			return "  "+index;
		} else if (index < 9999) {
			return " "+index;
		}
		return Integer.toString(index);
	}

	public void AddInfo(Object Message) {
		int NrOfInfos = InfoMessages.size()+1;
		InfoMessages.add(GetAllignmentSpaces(NrOfInfos)+"> "+Message.toString()+"\n");
		if (InfoWindow != null) {
			if (InfoWindow.isVisible()) {
				UpdateInfoWindow();
				return;
			}
		}
		UpdateInfoTab();
	}
	
	private void UpdateInfoWindow() {
		StringBuffer Line = new StringBuffer();
		for (String mes : InfoMessages) {
			Line.append(mes);
		}
		InfoWindow.set(Line.toString(), InfoMessages.size());
	}
	
	private void UpdateInfoTab() {
		StringBuffer Line = new StringBuffer();
		for (String mes : InfoMessages) {
			Line.append(mes);
		}
		textAreaInfo.setText(Line.toString());
		int idx = indexOfComponent(panelInfos);
		if (idx >= 0) {
			setSelectedIndex(idx);
			setTitleAt(idx, "Infos (" + InfoMessages.size() + ")");
			Rectangle rect = getBounds();
			rect.x = 0;
			rect.y = 0;
			if (EventQueue.isDispatchThread())
				paintImmediately(rect);
			else
				repaint(rect);
		}
	}

	public void AddWarning(Object Message) {
		WarningsList.add(Message);
		int idx = indexOfComponent(panelWarnings);
		if (idx >= 0) {
			setSelectedIndex(idx);
			setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
			Rectangle rect = getBounds();
			rect.x = 0;
			rect.y = 0;
			if (EventQueue.isDispatchThread())
				paintImmediately(rect);
			else
				repaint(rect);
		}
	}

	public void AddErrors(Object Message) {
		ErrorsList.add(Message);
		int idx = indexOfComponent(panelErrors);
		if (idx >= 0) {
			setSelectedIndex(idx);
			setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
			Rectangle rect = getBounds();
			rect.x = 0;
			rect.y = 0;
			if (EventQueue.isDispatchThread())
				paintImmediately(rect);
			else
				repaint(rect);
		}
	}

	public void AddConsole(String Message) {
		ConsoleMessages.add(Message + "\n");
		if (ConsoleWindow != null)
			if (ConsoleWindow.isVisible()) {
				UpdateConsoleWindow();
			}
		UpdateConsoleTab();
	}
	
	private void UpdateConsoleWindow() {
		StringBuffer Lines = new StringBuffer();
		for (String mes : ConsoleMessages) {
			Lines.append(mes);
		}
		ConsoleWindow.set(Lines.toString(), 0);
	}
	
	private void UpdateConsoleTab() {
		StringBuffer Lines = new StringBuffer();
		for (String mes : ConsoleMessages) {
			Lines.append(mes);
		}
		textAreaConsole.setText(Lines.toString());
		int idx = indexOfComponent(panelConsole);
		if (idx >= 0) {
			setSelectedIndex(idx);
			Rectangle rect = getBounds();
			rect.x = 0;
			rect.y = 0;
			if (EventQueue.isDispatchThread())
				paintImmediately(rect);
			else
				repaint(rect);
		}
	}

	public void ClearConsole() {
		ConsoleMessages.clear();
	}

	public void clearDRCTrace() {
		if (DRCTraceActive) {
			ActiveDRCContainer.ClearMarks();
			DRCTraceActive = false;
			MyProject.repaintCanvas();
		}
	}
	
	public void clearAllMessages() {
		clearDRCTrace();
		textAreaInfo.setText(null);
		InfoMessages.clear();
		int idx = indexOfComponent(panelInfos);
		if (idx >= 0) {
			setTitleAt(idx, "Infos (" + InfoMessages.size() + ")");
			setSelectedIndex(idx);
		}
		WarningsList.clear();
		idx = indexOfComponent(panelWarnings);
		if (idx >= 0)
			setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
		ErrorsList.clear();
		idx = indexOfComponent(panelErrors);
		if (idx >= 0)
			setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
		Rectangle rect = getBounds();
		rect.x = 0;
		rect.y = 0;
		if (EventQueue.isDispatchThread())
			paintImmediately(rect);
		else
			repaint(rect);
	}

	private void GenerateDRCTrace(SimpleDRCContainer dc) {
		DRCTraceActive = true;
		ActiveDRCContainer = dc;
		if (dc.HasCircuit())
			if (!MyProject.getCurrentCircuit().equals(dc.GetCircuit()))
				MyProject.setCurrentCircuit(dc.GetCircuit());
		dc.MarkComponents();
		MyProject.repaintCanvas();
	}

	/* Here the mouse events are handled */
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getClickCount()>1) {

			if (e.getSource().equals(this)) {
				if (getComponentCount()>0) {
					if (getSelectedComponent().equals(panelInfos)) {
						if (InfoWindow != null) {
							InfoWindow.setVisible(true);
							UpdateInfoWindow();
						} else {
							InfoWindow = new FPGACommanderTextWindow("FPGACommander: Infos",Color.GRAY,true);
							InfoWindow.setVisible(true);
							UpdateInfoWindow();
							InfoWindow.addWindowListener(this);
						}
						remove(getSelectedIndex());
					} else
					if (getSelectedComponent().equals(panelConsole)) {
						if (ConsoleWindow != null) {
							ConsoleWindow.setVisible(true);
							UpdateConsoleWindow();
						} else {
							ConsoleWindow = new FPGACommanderTextWindow("FPGACommander: Console",Color.LIGHT_GRAY,false);
							ConsoleWindow.setVisible(true);
							UpdateConsoleWindow();
							ConsoleWindow.addWindowListener(this);
						}
						remove(getSelectedIndex());
					} else 
					if (getSelectedComponent().equals(panelWarnings)) {
						if (WarningsWindow != null) {
							WarningsWindow.setVisible(true);
							remove(getSelectedIndex());
						}
					} else
					if (getSelectedComponent().equals(panelErrors)) {
						if (ErrorsWindow != null) {
							ErrorsWindow.setVisible(true);
							remove(getSelectedIndex());
						}
					}
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		boolean SourceIsWarningsWindow = (WarningsWindow == null) ? false : e.getSource().equals(WarningsWindow.getListObject());
		boolean SourceIsErrorsWindow = (ErrorsWindow == null) ? false : e.getSource().equals(ErrorsWindow.getListObject()); 
		if (e.getSource().equals(Errors) || SourceIsErrorsWindow) {
			clearDRCTrace();
			int idx = -1;
			if (e.getSource().equals(Errors))
				idx = Errors.getSelectedIndex();
			else if (SourceIsErrorsWindow)
				idx = ErrorsWindow.getListObject().getSelectedIndex();
			if (idx >= 0) {
				if (ErrorsList.getElementAt(idx) instanceof SimpleDRCContainer) {
					GenerateDRCTrace((SimpleDRCContainer)ErrorsList.getElementAt(idx));
				}
			}
		} else
			if (e.getSource().equals(Warnings) || SourceIsWarningsWindow ) {
				clearDRCTrace();
				int idx = -1;
				if (e.getSource().equals(Warnings))
					idx = Warnings.getSelectedIndex();
				else if (SourceIsWarningsWindow)
					idx = WarningsWindow.getListObject().getSelectedIndex();
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
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(InfoWindow)) {
			add(panelInfos,InfoTabIndex);
			UpdateInfoTab();
		}
		if (e.getSource().equals(ConsoleWindow)) {
			add(panelConsole, getComponentCount());
			UpdateConsoleTab();
		}
		if (e.getSource().equals(WarningsWindow)) {
			int idx = getComponentCount();
			HashSet<Component> comps = new HashSet<Component>(Arrays.asList(getComponents()));
			if (comps.contains(panelConsole))
				idx = indexOfComponent(panelConsole);
			if (comps.contains(panelErrors))
				idx = indexOfComponent(panelErrors);
			add(panelWarnings, idx);
			setTitleAt(idx, "Warnings (" + WarningsList.getCountNr() + ")");
			setSelectedIndex(idx);
			clearDRCTrace();
		}
		if (e.getSource().equals(ErrorsWindow)) {
			int idx = getComponentCount();
			HashSet<Component> comps = new HashSet<Component>(Arrays.asList(getComponents()));
			if (comps.contains(panelConsole))
				idx = indexOfComponent(panelConsole);
			add(panelErrors, idx);
			setTitleAt(idx, "Errors (" + ErrorsList.getCountNr() + ")");
			setSelectedIndex(idx);
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
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

}
