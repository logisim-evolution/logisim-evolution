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
package com.hepia.logisim.chronogui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.util.Icons;
import com.hepia.logisim.chronodata.ChronoData;
import com.hepia.logisim.chronodata.ChronoDataWriter;
import com.hepia.logisim.chronodata.ChronoModelEventHandler;
import com.hepia.logisim.chronodata.NoSysclkException;
import com.hepia.logisim.chronodata.SignalDataBus;
import com.hepia.logisim.chronodata.TimelineParam;
import java.io.File;
/**
 * Main chronogram JFrame Creates the chronogram
 */
public class ChronoFrame extends LFrame implements KeyListener, ActionListener,
		WindowListener {
 		JFileChooser fc;

	/**
	 * Listener to the button, the scrollbars, splitPane divider
	 */
	private class MyListener implements ActionListener, AdjustmentListener {

		private ChronoFrame chronoFrame;

		public MyListener(ChronoFrame cf) {
			chronoFrame = cf;
		}

		/**
		 * Load or export button event handler
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			// load a chronogram from a file
			if ("load".equals(e.getActionCommand())) {
				final JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(chronoFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					chronoFrame
							.loadFile(fc.getSelectedFile().getAbsolutePath());
				}

				// export a chronogram to a file
			} else if ("export".equals(e.getActionCommand())) {
                final JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(chronoFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    chronoFrame.exportFile(fc.getSelectedFile().getAbsolutePath());
                }

            }
            else if ("exportImg".equals(e.getActionCommand())) {
                fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(ChronoFrame.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();

                    //add .png to the filename if the user forgot
                    if (!fc.getSelectedFile().getAbsolutePath().endsWith(".png")) {
                        file = new File(fc.getSelectedFile() + ".png");
                    }
                    exportImage(file);
				}

			} else if ("play".equals(e.getActionCommand())) {
				if (simulator.isRunning()) {
					((JButton) e.getSource()).setIcon(Icons
							.getIcon("simplay.png"));
				} else {
					((JButton) e.getSource()).setIcon(Icons
							.getIcon("simstop.png"));
				}
				simulator.setIsRunning(!simulator.isRunning());
			} else if ("step".equals(e.getActionCommand())) {
				simulator.step();
			} else if ("tplay".equals(e.getActionCommand())) {
				if (simulator.isTicking()) {
					((JButton) e.getSource()).setIcon(Icons
							.getIcon("simtplay.png"));
				} else {
					((JButton) e.getSource()).setIcon(Icons
							.getIcon("simtstop.png"));
				}
				simulator.setIsTicking(!simulator.isTicking());
			} else if ("tstep".equals(e.getActionCommand())) {
				simulator.tick();
			} else if ("tmainstep".equals(e.getActionCommand())) {
				tickMain();
			}
		}
	

		/**
		 * rightScroll horizontal movement
		 */
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			if (rightPanel != null) {
				rightPanel.adjustmentValueChanged(e.getValue());
			}
		}
	}

	private static final long serialVersionUID = 1L;
	private Simulator simulator;
	private Project project;
	private ChronoData chronogramData;
	private JPanel mainPanel;
	private Preferences prefs;
	private LogFrame logFrame;
	// top bar
	private JPanel topBar;
	private JButton chooseFileButton;
	private JButton exportDataInFile;
    private JButton exportDataToImage;
	private JLabel statusLabel;
	// split pane
	private RightPanel rightPanel;
	private LeftPanel leftPanel;
	private CommonPanelParam commonPanelParam;
	private JScrollPane leftScroll;
	private JScrollPane rightScroll;
	private JSplitPane mainSplitPane;
	private TimelineParam timelineParam;
	// event managers
	private MyListener myListener = new MyListener(this);
	private DrawAreaEventManager mDrawAreaEventManager;
	private DrawAreaManager mDrawAreaManager;
	// graphical
	private int dividerLocation = 353;
	// mode
	private boolean realTimeMode;
	private ChronoModelEventHandler chronoModelEventHandler;
	// menu
	private JMenuBar winMenuBar;
	private JCheckBoxMenuItem ontopItem;

	private JMenuItem close;

	/**
	 * Offline mode ChronoFrame constructor
	 */
	public ChronoFrame(Project prj) {
		realTimeMode = false;
		commonPanelParam = new CommonPanelParam(20, 38);
		createMainStructure();
	}

	/**
	 * Real time mode ChronoFrame constructor
	 */
	public ChronoFrame(Project prj, LogFrame logFrame) {
		realTimeMode = true;

		this.logFrame = logFrame;
		timelineParam = logFrame.getTimelineParam();
		commonPanelParam = new CommonPanelParam(20, 38);
		project = prj;
		simulator = prj.getSimulator();
		chronogramData = new ChronoData();
		try {
			chronoModelEventHandler = new ChronoModelEventHandler(this,
					logFrame.getModel(), prj);
			createMainStructure();
			fillMainSPlitPane();

			if (chronogramData.size() == 0) {
				statusLabel.setText(Strings.get("SimStatusNoSignal"));
			} else {
				statusLabel.setText(Strings.get("SimStatusCurrentScheme"));
			}

		} catch (NoSysclkException ex) {
			createMainStructure();
			statusLabel.setText(Strings.get("SimStatusNoSysclk"));
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		Object src = ae.getSource();
		if (src == ontopItem) {
			ae.paramString();
			setAlwaysOnTop(ontopItem.getState());
		} else if (src == close) {
			setVisible(false);
		}
	}

	/**
	 * Creates the main panels for the chronogram
	 */
	private void createMainStructure() {
		mDrawAreaEventManager = new DrawAreaEventManager();
		mDrawAreaManager = new DrawAreaManager(this);
		mDrawAreaEventManager.addDrawAreaListener(mDrawAreaManager);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setFocusable(true);
		mainPanel.requestFocus();
		mainPanel.addKeyListener(this);
		addWindowListener(this);

		// menu bar
		winMenuBar = new JMenuBar();
		JMenu windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');
		ontopItem = new JCheckBoxMenuItem("Set on top", true);
		ontopItem.addActionListener(this);
		close = new JMenuItem("Close");
		close.addActionListener(this);
		windowMenu.add(ontopItem);
		windowMenu.addSeparator();
		windowMenu.add(close);
		winMenuBar.add(windowMenu);
		winMenuBar.setFocusable(false);
		setJMenuBar(winMenuBar);
		//setAlwaysOnTop(true);

		// top bar
		Dimension buttonSize = new Dimension(150, 25);
		topBar = new JPanel();
		topBar.setLayout(new FlowLayout(FlowLayout.LEFT));

		// external file
		chooseFileButton = new JButton(Strings.get("ButtonLoad"));
		chooseFileButton.setActionCommand("load");
		chooseFileButton.addActionListener(myListener);
		chooseFileButton.setPreferredSize(buttonSize);
		chooseFileButton.setFocusable(false);

		// export
		exportDataInFile = new JButton(Strings.get("ButtonExport"));
		exportDataInFile.setActionCommand("export");
		exportDataInFile.addActionListener(myListener);
		exportDataInFile.setPreferredSize(buttonSize);
		exportDataInFile.setFocusable(false);

        exportDataToImage = new JButton(Strings.get("Export as image"));
        exportDataToImage.setActionCommand("exportImg");
        exportDataToImage.addActionListener(myListener);
        exportDataToImage.setPreferredSize(buttonSize);
        exportDataToImage.setFocusable(false);

		// Toolbar
		JToolBar bar = new JToolBar();
		bar.setFocusable(false);
		JButton playButton;
		if (simulator != null && simulator.isRunning()) {
			playButton = new JButton(Icons.getIcon("simstop.png"));
		} else {
			playButton = new JButton(Icons.getIcon("simplay.png"));
		}
		playButton.setActionCommand("play");
		playButton.addActionListener(myListener);
		playButton.setToolTipText("Start/Stop simulation");
		playButton.setFocusable(false);
		bar.add(playButton);
		JButton stepButton = new JButton(Icons.getIcon("simstep.png"));
		stepButton.setActionCommand("step");
		stepButton.addActionListener(myListener);
		stepButton.setToolTipText("Simulate one step");
		stepButton.setFocusable(false);
		bar.add(stepButton);
		JButton tplayButton;
		if (simulator != null && simulator.isTicking()) {
			tplayButton = new JButton(Icons.getIcon("simtstop.png"));
		} else {
			tplayButton = new JButton(Icons.getIcon("simtplay.png"));
		}
		tplayButton.setActionCommand("tplay");
		tplayButton.addActionListener(myListener);
		tplayButton.setToolTipText("Start/Stop 'sysclk' tick");
		tplayButton.setFocusable(false);
		bar.add(tplayButton);
		JButton tstepButton = new JButton(Icons.getIcon("simtstep.png"));
		tstepButton.setActionCommand("tstep");
		tstepButton.addActionListener(myListener);
		tstepButton.setToolTipText("Step one 'sysclk' tick");
		tstepButton.setFocusable(false);
		bar.add(tstepButton);
		JButton tmainstepButton = new JButton(Icons.getIcon("clock.gif"));
		tmainstepButton.setActionCommand("tmainstep");
		tmainstepButton.addActionListener(myListener);
		tmainstepButton.setToolTipText("Step one 'clk' tick");
		tmainstepButton.setFocusable(false);
		if (chronogramData.get("clk") == null) {
			tmainstepButton.setEnabled(false);
			tmainstepButton
					.setToolTipText("Please create a clock named 'clk' to enable this function");
		}
		bar.add(tmainstepButton);

		mainPanel.add(BorderLayout.NORTH, bar);

		statusLabel = new JLabel();
		topBar.add(chooseFileButton);
		topBar.add(exportDataInFile);
		topBar.add(exportDataToImage);
		topBar.add(new JLabel(Strings.get("SimStatusName")));
		topBar.add(statusLabel);
		mainPanel.add(BorderLayout.SOUTH, topBar);

		// split pane
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainPanel.add(BorderLayout.CENTER, mainSplitPane);

		setTitle(Strings.get("ChronoTitle") + ": "
				+ project.getLogisimFile().getDisplayName());
		setResizable(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(new Dimension(1024, 768));

		setContentPane(mainPanel);

		prefs = Preferences.userRoot().node(this.getClass().getName());
		setLocation(prefs.getInt("X", 0), prefs.getInt("Y", 0));
		setSize(prefs.getInt("W", getSize().width),
				prefs.getInt("H", getSize().height));

		setVisible(true);
	}

	/**
	 * Popup an error message
	 *
	 * @param err
	 *            Error message
	 */
	public void errorMessage(String err) {
		JOptionPane.showMessageDialog(mainPanel, err,"",JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Export the current chronogram to file
	 */
	public void exportFile(String file) {
		ChronoDataWriter.export(file, timelineParam, chronogramData);
	}
    public void exportImage(File file) {
        ImageExporter ie = new ImageExporter(this, chronogramData, this.getCommonPanelParam().getHeaderHeight());
        ie.createImage(file);
    }

	/**
	 * Fill the splitPane with the two panels (SignalName and SignalDraw)
	 */
	private void fillMainSPlitPane() {
		mainSplitPane.setDividerSize(5);

		// ===Left Side===
		leftPanel = new LeftPanel(this, mDrawAreaEventManager);
		leftScroll = new JScrollPane(leftPanel);

		// ===Right Side===
		// keep scroolbar position
		int scrollBarCursorPos = rightScroll == null ? 0 : rightScroll
				.getHorizontalScrollBar().getValue();
		if (rightPanel == null) {
			rightPanel = new RightPanel(this, mDrawAreaEventManager);
		} else {
			rightPanel = new RightPanel(rightPanel);
		}

		rightScroll = new JScrollPane(rightPanel);
		rightScroll.getHorizontalScrollBar().addAdjustmentListener(myListener);

		// Synchronize the two scrollbars
		leftScroll.getVerticalScrollBar().setModel(
				rightScroll.getVerticalScrollBar().getModel());

		mainSplitPane.setLeftComponent(leftScroll);
		mainSplitPane.setRightComponent(rightScroll);

		// right scrollbar position
		rightScroll.getHorizontalScrollBar().setValue(scrollBarCursorPos);
		mDrawAreaManager.drawVerticalMouseClicked();
		rightScroll.getHorizontalScrollBar().setValue(scrollBarCursorPos);

		// keep leftpanel signal value up to date
		mDrawAreaManager.refreshSignalsValues();

		mainSplitPane.setDividerLocation(dividerLocation);
	}

	// accessors
	public ChronoData getChronoData() {
		return chronogramData;
	}

	public CommonPanelParam getCommonPanelParam() {
		return commonPanelParam;
	}

	public int getDividerLocation() {
		return dividerLocation;
	}

	public LeftPanel getLeftPanel() {
		return leftPanel;
	}

	public int getNbrOfTick() {
		return chronogramData.get("sysclk").getSignalValues().size();
	}

	public Project getProject() {
		return project;
	}

	public RightPanel getRightPanel() {
		return rightPanel;
	}

	public TimelineParam getTimelineParam() {
		return timelineParam;
	}

	public int getVisibleSignalsWidth() {
		return mainSplitPane.getRightComponent().getWidth();
	}

	public boolean isRealTimeMode() {
		return realTimeMode;
	}

	@Override
	public void keyPressed(KeyEvent ke) {
		int keyCode = ke.getKeyCode();
		if (keyCode == KeyEvent.VK_F2) {
			tickMain();
			// if(ke.getSource() instanceof JTable){
			// ((JTable)(ke.getSource())).getInputMap()
			// }
		}
	}

	@Override
	public void keyReleased(KeyEvent ke) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void keyTyped(KeyEvent ke) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Load the chronogram from the log file
	 */
	public void loadFile(String logFile) {
		try {
			ChronoData tmp = new ChronoData(logFile, this);
			if (tmp != null) {
				realTimeMode = false;
				chronogramData = tmp;
				fillMainSPlitPane();
				statusLabel.setText(Strings.get("InputFileLoaded") + logFile);
			}
		} catch (NoSysclkException ex) {
			errorMessage(Strings.get("InputFileNoSysclk"));
		} catch (Exception ex) {
			errorMessage(ex.toString());
		}
	}

	/**
	 * Repaint all signals
	 *
	 * @param force
	 *            If true, calls SwingUtilities.updateComponentTreeUI(this);
	 */
	public void repaintAll(boolean force) {
		rightPanel.repaintAll();

		if (this.isRealTimeMode()) {
			Runnable refreshScroll = new Runnable() {
				@Override
				public void run() {
					// keeps the scrolling bar at the top right, to follow the
					// last added data
					rightScroll.getHorizontalScrollBar().setValue(
							rightPanel.getSignalWidth());
					SwingUtilities.updateComponentTreeUI(ChronoFrame.this);
				}
			};
			SwingUtilities.invokeLater(refreshScroll);
		}
		if (force) {
			SwingUtilities.updateComponentTreeUI(this);
		}
	}

	public void setScrollbarPosition(int pos) {
		rightScroll.getHorizontalScrollBar().setValue(pos);
	}

	public void setTimelineParam(TimelineParam timelineParam) {
		this.timelineParam = timelineParam;
	}

	private void tickMain() {
		int ticks = 0;
		for (com.cburch.logisim.comp.Component clock : project.getLogisimFile()
				.getMainCircuit().getClocks()) {
			if (clock.getAttributeSet().getValue(StdAttr.LABEL)
					.contentEquals("clk")) {
				if (project.getOptions().getAttributeSet()
						.getValue(Options.ATTR_TICK_MAIN)
						.equals(Options.TICK_MAIN_HALF_PERIOD)) {
					if (project.getCircuitState().getValue(clock.getLocation())
							.toIntValue() == 0) {
						ticks = clock.getAttributeSet()
								.getValue(Clock.ATTR_LOW);
					} else {
						ticks = clock.getAttributeSet().getValue(
								Clock.ATTR_HIGH);
					}
				} else {
					ticks = clock.getAttributeSet().getValue(Clock.ATTR_LOW)
							+ clock.getAttributeSet().getValue(Clock.ATTR_HIGH);
				}
				break;
			}
		}
		simulator.tickMain(ticks);
	}

	/**
	 * Switch bus between signle bus view or detailed signals view
	 *
	 * @param choosenBus
	 *            Bus to expand or contract
	 * @param expand
	 *            true: expand, false:contract
	 */
	public void toggleBusExpand(SignalDataBus choosenBus, boolean expand) {
		if (expand) {
			chronogramData.expandBus(choosenBus);
		} else {
			chronogramData.contractBus(choosenBus);
		}
		fillMainSPlitPane();
	}

	@Override
	public void windowActivated(WindowEvent we) {
	}

	@Override
	public void windowClosed(WindowEvent we) {
	}

	@Override
	public void windowClosing(WindowEvent we) {
		logFrame.getModel().removeModelListener(chronoModelEventHandler);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		prefs.putInt("X", getX());
		prefs.putInt("Y", getY());
		prefs.putInt("W", getWidth());
		prefs.putInt("H", getHeight());
	}

	@Override
	public void windowDeiconified(WindowEvent we) {
	}

	@Override
	public void windowIconified(WindowEvent we) {
	}

	@Override
	public void windowOpened(WindowEvent we) {
	}
}
