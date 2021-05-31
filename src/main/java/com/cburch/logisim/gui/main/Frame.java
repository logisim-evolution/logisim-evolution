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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.appear.AppearanceView;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.RegTabContent;
import com.cburch.logisim.gui.generic.ZoomControl;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.icons.AnnimationTimer;
import com.cburch.logisim.gui.menu.MainMenuListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.HorizontalSplitPane;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.VerticalSplitPane;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.gui.HdlContentView;
import com.cburch.logisim.vhdl.gui.VhdlSimState;
import com.cburch.logisim.vhdl.gui.VhdlSimulatorConsole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Frame extends LFrame.MainWindow implements LocaleListener {

  public static final AnnimationTimer ANNIMATIONICONTIMER = new AnnimationTimer();
  public static final String EDITOR_VIEW = "editorView";
  public static final String EXPLORER_VIEW = "explorerView";
  public static final String EDIT_LAYOUT = "layout";
  public static final String EDIT_APPEARANCE = "appearance";
  public static final String EDIT_HDL = "hdl";
  private static final long serialVersionUID = 1L;
  private final Timer timer = new Timer();
  private final Project project;
  private final MyProjectListener myProjectListener = new MyProjectListener();
  // GUI elements shared between views
  private final MainMenuListener menuListener;
  private final Toolbar toolbar;
  private final HorizontalSplitPane leftRegion;
  private final HorizontalSplitPane rightRegion;
  private final HorizontalSplitPane editRegion;
  private final VerticalSplitPane mainRegion;
  private final JPanel rightPanel;
  private final JPanel mainPanelSuper;
  private final CardPanel mainPanel;
  // left-side elements
  private final JTabbedPane topTab;
  private final JTabbedPane bottomTab;
  private final Toolbox toolbox;
  private final SimulationExplorer simExplorer;
  private final AttrTable attrTable;
  private final ZoomControl zoom;
  // for the Layout view
  private final LayoutToolbarModel layoutToolbarModel;
  private final Canvas layoutCanvas;
  private final VhdlSimulatorConsole vhdlSimulatorConsole;
  private final HdlContentView hdlEditor;
  private final ZoomModel layoutZoomModel;
  private final LayoutEditHandler layoutEditHandler;
  private final AttrTableSelectionModel attrTableSelectionModel;
  // for the Appearance view
  private AppearanceView appearance;
  private Double lastFraction = AppPreferences.WINDOW_RIGHT_SPLIT.get();

  public Frame(Project project) {
    super(project);
    this.project = project;

    setBackground(Color.white);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new MyWindowListener());

    project.addProjectListener(myProjectListener);
    project.addLibraryListener(myProjectListener);
    project.addCircuitListener(myProjectListener);

    // set up elements for the Layout view
    layoutToolbarModel = new LayoutToolbarModel(this, project);
    layoutCanvas = new Canvas(project);
    CanvasPane canvasPane = new CanvasPane(layoutCanvas);
    double[] Options = new double[49];
    for (int i = 0; i < 49; i++) {
      Options[i] = (i + 1) * 20;
    }
    layoutZoomModel =
        new BasicZoomModel(
            AppPreferences.LAYOUT_SHOW_GRID,
            AppPreferences.LAYOUT_ZOOM,
            Options,
            canvasPane); // ZOOM_OPTIONS);

    layoutCanvas.getGridPainter().setZoomModel(layoutZoomModel);
    layoutEditHandler = new LayoutEditHandler(this);
    attrTableSelectionModel = new AttrTableSelectionModel(project, this);

    // set up menu bar and toolbar
    menuListener = new MainMenuListener(this, menubar);
    menuListener.setEditHandler(layoutEditHandler);
    toolbar = new Toolbar(layoutToolbarModel);

    // set up the left-side components
    toolbox = new Toolbox(project, this, menuListener);
    simExplorer = new SimulationExplorer(project, menuListener);
    bottomTab = new JTabbedPane();
    bottomTab.setFont(AppPreferences.getScaledFont(new Font("Dialog", Font.BOLD, 9)));
    bottomTab.add(attrTable = new AttrTable(this));
    bottomTab.add(new RegTabContent(this));

    zoom = new ZoomControl(layoutZoomModel, layoutCanvas);

    // set up the central area
    mainPanelSuper = new JPanel(new BorderLayout());
    canvasPane.setZoomModel(layoutZoomModel);
    mainPanel = new CardPanel();
    mainPanel.addView(EDIT_LAYOUT, canvasPane);
    mainPanel.setView(EDIT_LAYOUT);
    mainPanelSuper.add(mainPanel, BorderLayout.CENTER);

    // set up the contents, split down the middle, with the canvas
    // on the right and a split pane on the left containing the
    // explorer and attribute values.
    JPanel explPanel = new JPanel(new BorderLayout());
    explPanel.add(toolbox, BorderLayout.CENTER);

    JPanel simPanel = new JPanel(new BorderLayout());
    // simPanel.add(new JButton("stuff"), BorderLayout.NORTH);
    simPanel.add(simExplorer, BorderLayout.CENTER);

    topTab = new JTabbedPane();
    topTab.setFont(new Font("Dialog", Font.BOLD, 9));
    topTab.add(explPanel);
    topTab.add(simPanel);

    JPanel attrFooter = new JPanel(new BorderLayout());
    attrFooter.add(zoom);

    JPanel bottomTabAndZoom = new JPanel(new BorderLayout());
    bottomTabAndZoom.add(bottomTab, BorderLayout.CENTER);
    bottomTabAndZoom.add(attrFooter, BorderLayout.SOUTH);
    leftRegion =
        new HorizontalSplitPane(
            topTab, bottomTabAndZoom, AppPreferences.WINDOW_LEFT_SPLIT.get());

    hdlEditor = new HdlContentView(project);
    vhdlSimulatorConsole = new VhdlSimulatorConsole(project);
    editRegion = new HorizontalSplitPane(mainPanelSuper, hdlEditor, 1.0);
    rightRegion = new HorizontalSplitPane(editRegion, vhdlSimulatorConsole, 1.0);

    rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(rightRegion, BorderLayout.CENTER);

    VhdlSimState state = new VhdlSimState(project);
    state.stateChanged();
    project.getVhdlSimulator().addVhdlSimStateListener(state);

    mainRegion =
        new VerticalSplitPane(
            leftRegion, rightPanel, AppPreferences.WINDOW_MAIN_SPLIT.get());

    getContentPane().add(mainRegion, BorderLayout.CENTER);

    localeChanged();

    this.setSize(
        AppPreferences.WINDOW_WIDTH.get(),
        AppPreferences.WINDOW_HEIGHT.get());
    Point prefPoint = getInitialLocation();
    if (prefPoint != null) {
      this.setLocation(prefPoint);
    }
    this.setExtendedState(AppPreferences.WINDOW_STATE.get());

    menuListener.register(mainPanel);
    KeyboardToolSelection.register(toolbar);

    project.setFrame(this);
    if (project.getTool() == null) {
      project.setTool(project.getOptions().getToolbarData().getFirstTool());
    }
    mainPanel.addChangeListener(myProjectListener);
    AppPreferences.TOOLBAR_PLACEMENT.addPropertyChangeListener(myProjectListener);
    placeToolbar();

    LocaleManager.addLocaleListener(this);
    toolbox.updateStructure();
    try {
      timer.schedule(ANNIMATIONICONTIMER, 1000, 500);
    } catch (IllegalStateException e) {
      /* just continue, the timer was already running */
    }
  }

  private static Point getInitialLocation() {
    String s = AppPreferences.WINDOW_LOCATION.get();
    if (s == null) {
      return null;
    }
    int comma = s.indexOf(',');
    if (comma < 0) {
      return null;
    }
    try {
      int x = Integer.parseInt(s.substring(0, comma));
      int y = Integer.parseInt(s.substring(comma + 1));
      while (isProjectFrameAt(x, y)) {
        x += 20;
        y += 20;
      }
      Rectangle desired = new Rectangle(x, y, 50, 50);

      int gcBestSize = 0;
      Point gcBestPoint = null;
      GraphicsEnvironment ge;
      ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      for (GraphicsDevice gd : ge.getScreenDevices()) {
        for (GraphicsConfiguration gc : gd.getConfigurations()) {
          Rectangle gcBounds = gc.getBounds();
          if (gcBounds.intersects(desired)) {
            Rectangle inter = gcBounds.intersection(desired);
            int size = inter.width * inter.height;
            if (size > gcBestSize) {
              gcBestSize = size;
              int x2 = Math.max(gcBounds.x, Math.min(inter.x, inter.x + inter.width - 50));
              int y2 = Math.max(gcBounds.y, Math.min(inter.y, inter.y + inter.height - 50));
              gcBestPoint = new Point(x2, y2);
            }
          }
        }
      }
      if (gcBestPoint != null) {
        if (isProjectFrameAt(gcBestPoint.x, gcBestPoint.y)) {
          gcBestPoint = null;
        }
      }
      return gcBestPoint;
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean isProjectFrameAt(int x, int y) {
    for (Project current : Projects.getOpenProjects()) {
      Frame frame = current.getFrame();
      if (frame != null) {
        Point loc = frame.getLocationOnScreen();
        int d = Math.abs(loc.x - x) + Math.abs(loc.y - y);
        if (d <= 3) {
          return true;
        }
      }
    }
    return false;
  }

  public void resetLayout() {
    mainRegion.setFraction(0.25);
    leftRegion.setFraction(0.5);
    rightRegion.setFraction(1.0);
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  private void computeTitle() {
    String s;
    Circuit circuit = project.getCurrentCircuit();
    String name = project.getLogisimFile().getName();
    if (circuit != null) {
      s = StringUtil.format(S.get("titleCircFileKnown"), circuit.getName(), name);
    } else {
      s = StringUtil.format(S.get("titleFileKnown"), name);
    }
    this.setTitle(s + " (v " + Main.VERSION_NAME + ")");
    myProjectListener.enableSave();
  }

  public boolean confirmClose() {
    return confirmClose(S.get("confirmCloseTitle"));
  }

  // returns true if user is OK with proceeding
  public boolean confirmClose(String title) {
    String message =
        StringUtil.format(S.get("confirmDiscardMessage"), project.getLogisimFile().getName());

    if (!project.isFileDirty()) {
      return true;
    }
    toFront();
    String[] options = {S.get("saveOption"), S.get("discardOption"), S.get("cancelOption")};
    int result =
        OptionPane.showOptionDialog(
            this, message, title, 0, OptionPane.QUESTION_MESSAGE, null, options, options[0]);
    boolean ret;
    if (result == 0) {
      ret = ProjectActions.doSave(project);
    } else if (result == 1) {
      // Close the current project
      dispose();
      ret = true;
    } else {
      ret = false;
    }

    return ret;
  }

  public Canvas getCanvas() {
    return layoutCanvas;
  }

  public String getEditorView() {
    return (getHdlEditorView() != null ? EDIT_HDL : mainPanel.getView());
  }

  public void setEditorView(String view) {
    String curView = mainPanel.getView();
    if (hdlEditor.getHdlModel() == null && curView.equals(view)) {
      return;
    }
    editRegion.setFraction(1.0);
    hdlEditor.setHdlModel(null);

    if (view.equals(EDIT_APPEARANCE)) { // appearance view
      AppearanceView app = appearance;
      if (app == null) {
        app = new AppearanceView();
        app.setCircuit(project, project.getCircuitState());
        mainPanel.addView(EDIT_APPEARANCE, app.getCanvasPane());
        appearance = app;
        ANNIMATIONICONTIMER.addParrent(toolbar);
      }
      toolbar.setToolbarModel(app.getToolbarModel());
      app.getAttrTableDrawManager(attrTable).attributesSelected();
      zoom.setZoomModel(app.getZoomModel());
      zoom.setAutoZoomButtonEnabled(false);
      menuListener.setEditHandler(app.getEditHandler());
      mainPanel.setView(view);
      app.getCanvas().requestFocus();
    } else { // layout view
      toolbar.setToolbarModel(layoutToolbarModel);
      zoom.setZoomModel(layoutZoomModel);
      zoom.setAutoZoomButtonEnabled(true);
      menuListener.setEditHandler(layoutEditHandler);
      viewAttributes(project.getTool(), true);
      mainPanel.setView(view);
      layoutCanvas.requestFocus();
    }
  }

  public ZoomControl getZoomControl() {
    return this.zoom;
  }

  public VhdlSimulatorConsole getVhdlSimulatorConsole() {
    return vhdlSimulatorConsole;
  }

  public ZoomModel getZoomModel() {
    return layoutZoomModel;
  }

  @Override
  public void localeChanged() {
    computeTitle();
    topTab.setTitleAt(0, S.get("designTab"));
    topTab.setTitleAt(1, S.get("simulateTab"));
    bottomTab.setTitleAt(0, S.get("propertiesTab"));
    bottomTab.setTitleAt(1, S.get("stateTab"));
  }

  private void placeToolbar() {
    String loc = AppPreferences.TOOLBAR_PLACEMENT.get();
    rightPanel.remove(toolbar);
    if (!AppPreferences.TOOLBAR_HIDDEN.equals(loc)) {
      Object value = BorderLayout.NORTH;
      for (Direction dir : Direction.cardinals) {
        if (dir.toString().equals(loc)) {
          if (dir == Direction.EAST) {
            value = BorderLayout.EAST;
          } else if (dir == Direction.SOUTH) {
            value = BorderLayout.SOUTH;
          } else if (dir == Direction.WEST) {
            value = BorderLayout.WEST;
          } else {
            value = BorderLayout.NORTH;
          }
        }
      }
      rightPanel.add(toolbar, value);
      boolean vertical = value == BorderLayout.WEST || value == BorderLayout.EAST;
      toolbar.setOrientation(vertical ? Toolbar.VERTICAL : Toolbar.HORIZONTAL);
    }
    getContentPane().validate();
  }

  public void savePreferences() {
    AppPreferences.TICK_FREQUENCY.set(project.getSimulator().getTickFrequency());
    AppPreferences.LAYOUT_SHOW_GRID.setBoolean(layoutZoomModel.getShowGrid());
    AppPreferences.LAYOUT_ZOOM.set(layoutZoomModel.getZoomFactor());
    if (appearance != null) {
      ZoomModel aZoom = appearance.getZoomModel();
      AppPreferences.APPEARANCE_SHOW_GRID.setBoolean(aZoom.getShowGrid());
      AppPreferences.APPEARANCE_ZOOM.set(aZoom.getZoomFactor());
    }
    int state = getExtendedState() & ~JFrame.ICONIFIED;
    AppPreferences.WINDOW_STATE.set(state);
    Dimension dim = getSize();
    AppPreferences.WINDOW_WIDTH.set(dim.width);
    AppPreferences.WINDOW_HEIGHT.set(dim.height);
    Point loc;
    try {
      loc = getLocationOnScreen();
    } catch (IllegalComponentStateException e) {
      loc = Projects.getLocation(this);
    }
    if (loc != null) {
      AppPreferences.WINDOW_LOCATION.set(loc.x + "," + loc.y);
    }
    if (leftRegion.getFraction() > 0)
      AppPreferences.WINDOW_LEFT_SPLIT.set(leftRegion.getFraction());
    if (rightRegion.getFraction() < 1.0)
      AppPreferences.WINDOW_RIGHT_SPLIT.set(rightRegion.getFraction());
    if (mainRegion.getFraction() > 0)
      AppPreferences.WINDOW_MAIN_SPLIT.set(mainRegion.getFraction());
    AppPreferences.DIALOG_DIRECTORY.set(JFileChoosers.getCurrentDirectory());
  }

  void setAttrTableModel(AttrTableModel value) {
    attrTable.setAttrTableModel(value);
    if (value instanceof AttrTableToolModel) {
      Tool tool = ((AttrTableToolModel) value).getTool();
      toolbox.setHaloedTool(tool);
      layoutToolbarModel.setHaloedTool(tool);
    } else {
      toolbox.setHaloedTool(null);
      layoutToolbarModel.setHaloedTool(null);
    }
    if (value instanceof AttrTableComponentModel) {
      Circuit circ = ((AttrTableComponentModel) value).getCircuit();
      Component comp = ((AttrTableComponentModel) value).getComponent();
      layoutCanvas.setHaloedComponent(circ, comp);
    } else {
      layoutCanvas.setHaloedComponent(null, null);
    }
  }

  public void setVhdlSimulatorConsoleStatus(boolean visible) {
    if (visible) {
      rightRegion.setFraction(lastFraction);
    } else {
      lastFraction = rightRegion.getFraction();
      rightRegion.setFraction(1);
    }
  }

  public HdlModel getHdlEditorView() {
    return hdlEditor.getHdlModel();
  }

  private void setHdlEditorView(HdlModel hdl) {
    hdlEditor.setHdlModel(hdl);
    zoom.setZoomModel(null);
    editRegion.setFraction(0.0);
    toolbar.setToolbarModel(hdlEditor.getToolbarModel());
  }

  void viewAttributes(Tool newTool) {
    viewAttributes(null, newTool, false);
  }

  private void viewAttributes(Tool newTool, boolean force) {
    viewAttributes(null, newTool, force);
  }

  private void viewAttributes(Tool oldTool, Tool newTool, boolean force) {
    AttributeSet newAttrs;
    if (newTool == null) {
      newAttrs = null;
      if (!force) {
        return;
      }
    } else {
      newAttrs = newTool.getAttributeSet(layoutCanvas);
    }
    if (newAttrs == null) {
      AttrTableModel oldModel = attrTable.getAttrTableModel();
      boolean same =
          oldModel instanceof AttrTableToolModel
              && ((AttrTableToolModel) oldModel).getTool() == oldTool;
      if (!force && !same && !(oldModel instanceof AttrTableCircuitModel)) {
        return;
      }
    }
    if (newAttrs == null) {
      Circuit circ = project.getCurrentCircuit();
      if (circ != null) {
        setAttrTableModel(new AttrTableCircuitModel(project, circ));
      } else if (force) {
        setAttrTableModel(null);
      }
    } else if (newAttrs instanceof SelectionAttributes) {
      setAttrTableModel(attrTableSelectionModel);
    } else {
      setAttrTableModel(new AttrTableToolModel(project, newTool));
    }
  }

  public void viewComponentAttributes(Circuit circ, Component comp) {
    if (comp == null) {
      setAttrTableModel(null);
    } else {
      setAttrTableModel(new AttrTableComponentModel(project, circ, comp));
    }
  }

  class MyProjectListener
      implements ProjectListener,
          LibraryListener,
          CircuitListener,
          PropertyChangeListener,
          ChangeListener {

    public void attributeListChanged(AttributeEvent e) {}

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
        computeTitle();
      }
    }

    private void enableSave() {
      boolean ok = getProject().isFileDirty();
      getRootPane().putClientProperty("windowModified", ok);
    }

    @Override
    public void libraryChanged(LibraryEvent e) {
      if (e.getAction() == LibraryEvent.SET_NAME) {
        computeTitle();
      } else if (e.getAction() == LibraryEvent.DIRTY_STATE) {
        enableSave();
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();

      if (action == ProjectEvent.ACTION_SET_FILE) {
        computeTitle();
        project.setTool(project.getOptions().getToolbarData().getFirstTool());
        placeToolbar();
      } else if (action == ProjectEvent.ACTION_SET_STATE) {
        if (event.getData() instanceof CircuitState) {
          CircuitState state = (CircuitState) event.getData();
          if (state.getParentState() != null) topTab.setSelectedIndex(1); // sim explorer view
        }
      } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getData() instanceof Circuit) {
          setEditorView(EDIT_LAYOUT);
          if (appearance != null) {
            appearance.setCircuit(project, project.getCircuitState());
          }
        } else if (event.getData() instanceof HdlModel) {
          setHdlEditorView((HdlModel) event.getData());
        }
        viewAttributes(project.getTool());
        computeTitle();
      } else if (action == ProjectEvent.ACTION_SET_TOOL) {
        if (attrTable == null) {
          return; // for startup
        }
        Tool oldTool = (Tool) event.getOldData();
        Tool newTool = (Tool) event.getData();
        if (!getEditorView().equals(EDIT_APPEARANCE)) {
          viewAttributes(oldTool, newTool, false);
        }
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.TOOLBAR_PLACEMENT.isSource(event)) {
        placeToolbar();
      }
    }

    @Override
    public void stateChanged(ChangeEvent event) {
      Object source = event.getSource();
      if (source == mainPanel) {
        firePropertyChange(EDITOR_VIEW, "???", getEditorView());
      }
    }
  }

  class MyWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      if (confirmClose(S.get("confirmCloseTitle"))) {
        layoutCanvas.closeCanvas();
        timer.cancel();
        Frame.this.dispose();
      }
    }

    @Override
    public void windowOpened(WindowEvent e) {
      layoutCanvas.computeSize(true);
    }
  }
}
