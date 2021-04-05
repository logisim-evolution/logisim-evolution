/*
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

package com.cburch.logisim.gui.chrono;

import static com.cburch.logisim.gui.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.log.LogPanel;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import com.cburch.logisim.gui.main.SimulationToolbarModel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.EditHandler;


public class ChronoPanel extends LogPanel implements KeyListener, Model.Listener {

  private class MyListener implements ActionListener, AdjustmentListener {

    @Override
    public void actionPerformed(ActionEvent e) { }
    /**
     * rightScroll horizontal movement
     */
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      if (rightPanel != null) rightPanel.adjustmentValueChanged(e.getValue());
    }
  }

private static final long serialVersionUID = 1L;
public static final int HEADER_HEIGHT = 20;
public static final int SIGNAL_HEIGHT = 30;
public static final int GAP = 2;
public static final int INITIAL_SPLIT = 150;
private Simulator simulator;
private Model model;
// button bar
private JPanel buttonBar = new JPanel();
private JButton chooseFileButton = new JButton();
private JButton exportDataInFile = new JButton();
private JButton exportDataToImage = new JButton();
private RightPanel rightPanel;
private LeftPanel leftPanel;
private JScrollPane leftScroll, rightScroll;
private JSplitPane splitPane;
// listeners
private MyListener myListener = new MyListener();

public ChronoPanel(LogFrame logFrame) {
  super(logFrame);
  SELECT_BG = UIManager.getDefaults().getColor("List.selectionBackground");
  SELECT_HI = darker(SELECT_BG);
  SELECT = new Color[] { SELECT_BG, SELECT_HI, SELECT_LINE, SELECT_ERR, SELECT_ERRLINE, SELECT_UNK, SELECT_UNKLINE };
  simulator = getProject().getSimulator();
  setModel(logFrame.getModel());
  configure();
  resplit();
  editHandler.computeEnabled();
}

 
private void configure() {
  setLayout(new BorderLayout());
  setFocusable(true);
  requestFocusInWindow();
  addKeyListener(this);
  // button bar
  Dimension buttonSize = new Dimension(150, 25);
  buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));

  chooseFileButton.setActionCommand("load");
  chooseFileButton.addActionListener(myListener);
  chooseFileButton.setPreferredSize(buttonSize);
  chooseFileButton.setFocusable(false);

  exportDataInFile.setActionCommand("export");
  exportDataInFile.addActionListener(myListener);
  exportDataInFile.setPreferredSize(buttonSize);
  exportDataInFile.setFocusable(false);

  exportDataToImage.setActionCommand("exportImg");
  exportDataToImage.addActionListener(myListener);
  exportDataToImage.setPreferredSize(buttonSize);
  exportDataToImage.setFocusable(false);
  // menu and simulation toolbar
  LogFrame logFrame = getLogFrame();
  SimulationToolbarModel simTools;
  simTools = new SimulationToolbarModel(getProject(), logFrame.getMenuListener());
  Toolbar toolbar = new Toolbar(simTools);
  JPanel toolpanel = new JPanel();
  GridBagLayout gb = new GridBagLayout();
  GridBagConstraints gc = new GridBagConstraints();
  toolpanel.setLayout(gb);
  gc.fill = GridBagConstraints.NONE;
  gc.weightx = gc.weighty = 0.0;
  gc.gridx = gc.gridy = 0;
  gb.setConstraints(toolbar, gc);
  toolpanel.add(toolbar);

  JButton b = logFrame.makeSelectionButton();
  gc.gridx = 1;
  gb.setConstraints(b, gc);
  toolpanel.add(b);

  Component filler = Box.createHorizontalGlue();
  gc.fill = GridBagConstraints.HORIZONTAL;
  gc.weightx = 1.0;
  gc.gridx = 2;
  gb.setConstraints(filler, gc);
  toolpanel.add(filler);
  add(toolpanel, BorderLayout.NORTH);

  buttonBar.add(chooseFileButton);
  buttonBar.add(exportDataInFile);
  buttonBar.add(exportDataToImage);
  add(BorderLayout.SOUTH, buttonBar);

  // panels
  splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
  splitPane.setDividerSize(5);
  splitPane.setResizeWeight(0.0);
  add(BorderLayout.CENTER, splitPane);
}

private void resplit() {
  leftPanel = new LeftPanel(this);
  leftScroll = new JScrollPane(leftPanel,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

  int p = rightScroll == null ? 0 : rightScroll.getHorizontalScrollBar().getValue();
  if (rightPanel == null)
    rightPanel = new RightPanel(this, leftPanel.getSelectionModel());
  rightScroll = new JScrollPane(rightPanel,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
  rightScroll.getHorizontalScrollBar().addAdjustmentListener(myListener);

  // Synchronize the two scrollbars
  leftScroll.getVerticalScrollBar().setModel(rightScroll.getVerticalScrollBar().getModel());

  splitPane.setLeftComponent(leftScroll);
  splitPane.setRightComponent(rightScroll);

  setSignalCursorX(Integer.MAX_VALUE);
  // put right scrollbar into same position
  rightScroll.getHorizontalScrollBar().setValue(p);
  rightScroll.getHorizontalScrollBar().setValue(p);
}

public LeftPanel getLeftPanel() {
  return leftPanel;
}

public RightPanel getRightPanel() {
  return rightPanel;
}

public JScrollBar getVerticalScrollBar() {
  return rightScroll == null ? null : rightScroll.getVerticalScrollBar();
}

public JScrollBar getHorizontalScrollBar() {
  return rightScroll == null ? null : rightScroll.getHorizontalScrollBar();
}

public JViewport getRightViewport() {
  return rightScroll == null ? null : rightScroll.getViewport();
}

public int getVisibleSignalsWidth() {
  return splitPane.getRightComponent().getWidth();
}

@Override
public void keyPressed(KeyEvent ke) {
  int keyCode = ke.getKeyCode();
  if (keyCode == KeyEvent.VK_F2) {
    simulator.tick(2);
  }
}

@Override
public void keyReleased(KeyEvent ke) {}

@Override
public void keyTyped(KeyEvent ke) { }

@Override
public String getTitle() {
  return S.get("ChronoTitle");
}

@Override
public String getHelpText() {
  return S.get("ChronoTitle");
}

@Override
public void localeChanged() {
  chooseFileButton.setText(S.get("ButtonLoad"));
  exportDataInFile.setText(S.get("ButtonExport"));
  exportDataToImage.setText(S.get("ButtonExportAsImage"));
}

@Override
public void modelChanged(Model oldModel, Model newModel) {
  setModel(newModel);
  setSignalCursorX(Integer.MAX_VALUE);
  leftPanel.updateSignals();
  rightPanel.updateSignals();
}

  public void changeSpotlight(Signal s) {
    Signal old = model.setSpotlight(s);
    if (old == s)
      return;
    rightPanel.changeSpotlight(old, s);
    leftPanel.changeSpotlight(old, s);
  }


  public void setSignalCursorX(int posX) {
    rightPanel.setSignalCursorX(posX);
    leftPanel.updateSignalValues();
  }

  @Override
  public void modeChanged(Model.Event event) {
    System.out.println("todo");
  }

  @Override
  public void signalsExtended(Model.Event event) {
    leftPanel.updateSignalValues();
    rightPanel.updateWaveforms();
  }

  @Override
  public void signalsReset(Model.Event event) {
    setSignalCursorX(Integer.MAX_VALUE);
    rightPanel.updateWaveforms();
  }

  @Override
  public void filePropertyChanged(Model.Event event) {}

  @Override
  public void historyLimitChanged(Model.Event event) {
     setSignalCursorX(Integer.MAX_VALUE);
     rightPanel.updateWaveforms();
  }
  
  @Override
  public void selectionChanged(Model.Event event) {
    leftPanel.updateSignals();
    rightPanel.updateSignals();
  }

  public void toggleBusExpand(Signal s, boolean expand) {}

  public Model getModel() {
    return model;
  }

  public void setModel(Model newModel) {
    if (model != null)
      model.removeModelListener(this);
    model = newModel;
    if (model == null)
      return;
    model.addModelListener(this);
  }

  private static final Color PLAIN_BG = new Color(0xbb, 0xbb, 0xbb);
  private static final Color PLAIN_HI = darker(PLAIN_BG);
  private static final Color PLAIN_LINE = Color.BLACK;
  private static final Color PLAIN_ERR = new Color(0xdb, 0x9d, 0x9d);
  private static final Color PLAIN_ERRLINE = Color.BLACK;
  private static final Color PLAIN_UNK = new Color(0xea, 0xaa, 0x6c);
  private static final Color PLAIN_UNKLINE = Color.BLACK;
  private static final Color SPOT_BG = new Color(0xaa, 0xff, 0xaa);
  private static final Color SPOT_HI = darker(SPOT_BG);
  private static final Color SPOT_LINE = Color.BLACK;
  private static final Color SPOT_ERR = new Color(0xf9, 0x76, 0x76);
  private static final Color SPOT_ERRLINE = Color.BLACK;
  private static final Color SPOT_UNK = new Color(0xea, 0x98, 0x49);
  private static final Color SPOT_UNKLINE = Color.BLACK;
  private final Color SELECT_BG; // set in constructor
  private final Color SELECT_HI; // set in constructor
  private static final Color SELECT_LINE = Color.BLACK;
  private static final Color SELECT_ERR = new Color(0xe5, 0x80, 0x80);
  private static final Color SELECT_ERRLINE = Color.BLACK;
  private static final Color SELECT_UNK = new Color(0xee, 0x99, 0x44);
  private static final Color SELECT_UNKLINE = Color.BLACK;
  private static final Color[] SPOT = { SPOT_BG, SPOT_HI, SPOT_LINE, SPOT_ERR, SPOT_ERRLINE, SPOT_UNK, SPOT_UNKLINE };
  private static final Color[] PLAIN = { PLAIN_BG, PLAIN_HI, PLAIN_LINE, PLAIN_ERR, PLAIN_ERRLINE, PLAIN_UNK, PLAIN_UNKLINE };
  private final Color[] SELECT; // set in constructor

  public Color[] rowColors(SignalInfo item, boolean isSelected) {
    if (isSelected)
      return SELECT;
    Signal spotlight = model.getSpotlight();
    if (spotlight != null && spotlight.info == item)
      return SPOT;
    return PLAIN;
  }

  private static Color darker(Color c) {
    float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null); 
    float s = 0.8f;
    if (hsb[1] == 0.0)
      return Color.getHSBColor(hsb[0], hsb[1] + hsb[1], hsb[2]*s);
    else
      return Color.getHSBColor(hsb[0], 1.0f - (1.0f - hsb[1])*s, hsb[2]);
  }
  @Override
  public EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    @Override
    public void computeEnabled() {
      boolean empty = model.getSignalCount() == 0;
      boolean sel = !empty && !leftPanel.getSelectionModel().isSelectionEmpty();
      setEnabled(LogisimMenuBar.CUT, sel);
      setEnabled(LogisimMenuBar.COPY, sel);
      setEnabled(LogisimMenuBar.PASTE, true);
      setEnabled(LogisimMenuBar.DELETE, sel);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, !empty);
      // todo: raise/lower handlers
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }
  };
}
