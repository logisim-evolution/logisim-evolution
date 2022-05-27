/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import com.cburch.logisim.generated.BuildInfo;
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
import com.cburch.logisim.util.VerticalSplitPane;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.gui.HdlContentView;
import com.cburch.logisim.vhdl.gui.VhdlSimState;
import com.cburch.logisim.vhdl.gui.VhdlSimulatorConsole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Timer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Frame extends LFrame.MainWindow implements LocaleListener {
  private static final long serialVersionUID = 1L;

  public static final String EDITOR_VIEW = "editorView";
  public static final String EXPLORER_VIEW = "explorerView";
  public static final String EDIT_LAYOUT = "layout";
  public static final String EDIT_APPEARANCE = "appearance";
  public static final String EDIT_HDL = "hdl";
  private final Timer timer = new Timer();
  private final Project project;
  private final MyProjectListener myProjectListener = new MyProjectListener();
  // GUI elements shared between views
  private final MainMenuListener menuListener;
  private final Toolbar toolbar;
  private final HorizontalSplitPane leftRegion;
  private final HorizontalSplitPane rightRegion;
  private final HorizontalSplitPane editRegion;
  private final MainRegionVerticalSplitPane mainRegion;
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
    final var canvasPane = new CanvasPane(layoutCanvas);

    layoutZoomModel =
        new BasicZoomModel(
            AppPreferences.LAYOUT_SHOW_GRID,
            AppPreferences.LAYOUT_ZOOM,
            buildZoomSteps(),
            canvasPane);

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
    final var explPanel = new JPanel(new BorderLayout());
    explPanel.add(toolbox, BorderLayout.CENTER);

    final var simPanel = new JPanel(new BorderLayout());
    simPanel.add(simExplorer, BorderLayout.CENTER);

    topTab = new JTabbedPane();
    topTab.setFont(new Font("Dialog", Font.BOLD, 9));
    topTab.add(explPanel);
    topTab.add(simPanel);

    final var attrFooter = new JPanel(new BorderLayout());
    attrFooter.add(zoom);

    final var bottomTabAndZoom = new JPanel(new BorderLayout());
    bottomTabAndZoom.add(bottomTab, BorderLayout.CENTER);
    bottomTabAndZoom.add(attrFooter, BorderLayout.SOUTH);
    leftRegion = new HorizontalSplitPane(topTab, bottomTabAndZoom, AppPreferences.WINDOW_LEFT_SPLIT.get());

    hdlEditor = new HdlContentView(project);
    vhdlSimulatorConsole = new VhdlSimulatorConsole(project);
    editRegion = new HorizontalSplitPane(mainPanelSuper, hdlEditor, 1.0);
    rightRegion = new HorizontalSplitPane(editRegion, vhdlSimulatorConsole, 1.0);

    rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(rightRegion, BorderLayout.CENTER);

    final var state = new VhdlSimState(project);
    state.stateChanged();
    project.getVhdlSimulator().addVhdlSimStateListener(state);

    mainRegion = new MainRegionVerticalSplitPane(leftRegion, rightPanel);
    getContentPane().add(mainRegion, BorderLayout.CENTER);

    localeChanged();

    this.setSize(AppPreferences.WINDOW_WIDTH.get(), AppPreferences.WINDOW_HEIGHT.get());
    final var prefPoint = getInitialLocation();
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
  }

  /**
   * Content aware VerticalSplitPane that handles main layout changes with bit of extra logic.
   */
  private static class MainRegionVerticalSplitPane extends VerticalSplitPane implements PropertyChangeListener {
    private final JComponent componentTree;
    private final JComponent mainCanvas;
    private Direction orientation;

    public MainRegionVerticalSplitPane(JComponent componentTree, JComponent mainCanvas) {
      this(componentTree, mainCanvas, AppPreferences.WINDOW_MAIN_SPLIT.get(),
              Direction.parse(AppPreferences.CANVAS_PLACEMENT.get()));
    }

    public MainRegionVerticalSplitPane(JComponent componentTree, JComponent mainCanvas, double fraction,
                                       Direction orientation) {
      super();

      this.componentTree = componentTree;
      this.mainCanvas = mainCanvas;
      this.orientation = orientation;
      this.realFraction = fraction;

      if (orientation == Direction.EAST) {
        init(componentTree, mainCanvas, fraction);
      } else {
        init(mainCanvas, componentTree, fraction);
      }

      AppPreferences.CANVAS_PLACEMENT.addPropertyChangeListener(this);
    }

    public void setOrientation(Direction newOrientation) {
      if (newOrientation != orientation) {
        orientation = newOrientation;
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.CANVAS_PLACEMENT.isSource(event)) {
        swapComponents();
      }
    }

    /**
     * As pane swapping should be fully transparent to the outer world, we must facade real
     * fraction value here, as it must stay unaltered regardles of the real content order.
     */
    private double realFraction;

    @Override
    public double getFraction() {
      return realFraction;
    }

    @Override
    public void setFraction(double value) {
      realFraction = value;
      super.setFraction(orientation == Direction.EAST ? value : 1 - value);
    }

    protected void swapComponents() {
      final var tmpOrient = Direction.parse(AppPreferences.CANVAS_PLACEMENT.get());
      if (orientation != tmpOrient) {
        orientation = tmpOrient;

        final var isSwapped = (orientation == Direction.WEST);
        compLeft = isSwapped ? mainCanvas : componentTree;
        compRight = isSwapped ? componentTree : mainCanvas;

        setFraction(realFraction);

        // setFraction() calls revalidate() if fraction changed, so for value of 0.5
        // the swap needs manual trigger
        if (super.getFraction() == realFraction) {
          revalidate();
        }
      }
    }
  }

  /**
   * Computes allowed zoom steps.
   *
   * @return
   */
  private ArrayList<Double> buildZoomSteps() {
    // Pairs must be in acending order (sorted by maxZoom value).
    final var config = new ZoomStepPair[] {new ZoomStepPair(50, 5), new ZoomStepPair(200, 10), new ZoomStepPair(1000, 20)};

    // Result zoomsteps.
    final var steps = new ArrayList<Double>();
    var zoom = 0D;
    for (final var pair : config) {
      while (zoom < pair.maxZoom()) {
        zoom += pair.step();
        steps.add(zoom);
      }
    }
    return steps;
  }

  private record ZoomStepPair(int maxZoom, int step) {}


  private static Point getInitialLocation() {
    final var s = AppPreferences.WINDOW_LOCATION.get();
    if (s == null) return null;
    final var comma = s.indexOf(',');
    if (comma < 0) return null;
    try {
      var x = Integer.parseInt(s.substring(0, comma));
      var y = Integer.parseInt(s.substring(comma + 1));
      while (isProjectFrameAt(x, y)) {
        x += 20;
        y += 20;
      }
      final var desired = new Rectangle(x, y, 50, 50);

      var gcBestSize = 0;
      Point gcBestPoint = null;
      final var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      for (final var gd : ge.getScreenDevices()) {
        for (final var gc : gd.getConfigurations()) {
          final var gcBounds = gc.getBounds();
          if (gcBounds.intersects(desired)) {
            final var inter = gcBounds.intersection(desired);
            final var size = inter.width * inter.height;
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
    for (final var current : Projects.getOpenProjects()) {
      final var frame = current.getFrame();
      if (frame != null) {
        final var loc = frame.getLocationOnScreen();
        final var d = Math.abs(loc.x - x) + Math.abs(loc.y - y);
        if (d <= 3) {
          return true;
        }
      }
    }
    return false;
  }

  public void resetLayout() {
    mainRegion.setFraction(0.25);
    mainRegion.setOrientation(Direction.EAST);
    leftRegion.setFraction(0.5);
    rightRegion.setFraction(1.0);
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  /**
   * Generates String to be used as generic Frame title, taking
   * names of circuits or app version or type.
   */
  private void buildTitleString() {
    final var circuit = project.getCurrentCircuit();
    final var name = project.getLogisimFile().getName();
    final var title = new StringBuilder();

    title
        .append(project.isFileDirty() ? (Main.DIRTY_MARKER + "\u0020") : "")
        .append(
            (circuit != null)
                ? S.get("titleCircFileKnown", circuit.getName(), name)
                : S.get("titleFileKnown", name))
        .append(" · ")
        .append(BuildInfo.displayName);

    // The icon alone may sometimes be missed so we add additional "[UNSAVED]" to the title too.
    if (project.isFileDirty()) {
      title.append(String.format("\u0020[%s]", S.get("titleUnsavedProjectState").toUpperCase()));
    }

    if (!BuildInfo.version.isStable()) {
      title.append(String.format("\u0020(ID:%s, BUILT:%s)", BuildInfo.buildId, BuildInfo.dateIso8601));
    }

    this.setTitle(title.toString().trim());
    myProjectListener.enableSave();
  }

  public boolean confirmClose() {
    return confirmClose(S.get("confirmCloseTitle"));
  }

  // returns true if user is OK with proceeding
  public boolean confirmClose(String title) {
    if (!project.isFileDirty()) return true;

    final var message = S.get("confirmDiscardMessage", project.getLogisimFile().getName());

    toFront();
    final String[] options = {S.get("saveOption"), S.get("discardOption"), S.get("cancelOption")};
    final var result = OptionPane.showOptionDialog(this, message, title, 0, OptionPane.QUESTION_MESSAGE, null, options, options[0]);
    var ret = false;
    if (result == 0) {
      ret = ProjectActions.doSave(project);
    } else if (result == 1) {
      // Close the current project
      dispose();
      ret = true;
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
    final var curView = mainPanel.getView();
    if (hdlEditor.getHdlModel() == null && curView.equals(view)) return;
    editRegion.setFraction(1.0);
    hdlEditor.setHdlModel(null);

    if (view.equals(EDIT_APPEARANCE)) {
      // appearance view
      var app = appearance;
      if (app == null) {
        app = new AppearanceView();
        app.setCircuit(project, project.getCircuitState());
        mainPanel.addView(EDIT_APPEARANCE, app.getCanvasPane());
        appearance = app;
      }
      toolbar.setToolbarModel(app.getToolbarModel());
      app.getAttrTableDrawManager(attrTable).attributesSelected();
      zoom.setZoomModel(app.getZoomModel());
      zoom.setAutoZoomButtonEnabled(false);
      menuListener.setEditHandler(app.getEditHandler());
      mainPanel.setView(view);
      app.getCanvas().requestFocus();
    } else {
      // layout view
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
    buildTitleString();
    topTab.setTitleAt(0, S.get("designTab"));
    topTab.setTitleAt(1, S.get("simulateTab"));
    bottomTab.setTitleAt(0, S.get("propertiesTab"));
    bottomTab.setTitleAt(1, S.get("stateTab"));
  }

  private void placeToolbar() {
    final var loc = AppPreferences.TOOLBAR_PLACEMENT.get();
    rightPanel.remove(toolbar);
    if (!AppPreferences.TOOLBAR_HIDDEN.equals(loc)) {
      var value = BorderLayout.NORTH;
      for (final var dir : Direction.cardinals) {
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
      final var vertical = BorderLayout.WEST.equals(value) || BorderLayout.EAST.equals(value);
      toolbar.setOrientation(vertical ? Toolbar.VERTICAL : Toolbar.HORIZONTAL);
    }
    getContentPane().validate();
  }

  public void savePreferences() {
    AppPreferences.TICK_FREQUENCY.set(project.getSimulator().getTickFrequency());
    AppPreferences.LAYOUT_SHOW_GRID.setBoolean(layoutZoomModel.getShowGrid());
    AppPreferences.LAYOUT_ZOOM.set(layoutZoomModel.getZoomFactor());
    if (appearance != null) {
      final var appearanceZoom = appearance.getZoomModel();
      AppPreferences.APPEARANCE_SHOW_GRID.setBoolean(appearanceZoom.getShowGrid());
      AppPreferences.APPEARANCE_ZOOM.set(appearanceZoom.getZoomFactor());
    }
    final var state = getExtendedState() & ~JFrame.ICONIFIED;
    AppPreferences.WINDOW_STATE.set(state);
    final var dim = getSize();
    AppPreferences.WINDOW_WIDTH.set(dim.width);
    AppPreferences.WINDOW_HEIGHT.set(dim.height);
    Point loc;
    try {
      loc = getLocationOnScreen();
    } catch (IllegalComponentStateException e) {
      loc = Projects.getLocation(this);
    }
    if (loc != null) AppPreferences.WINDOW_LOCATION.set(loc.x + "," + loc.y);
    if (leftRegion.getFraction() > 0) AppPreferences.WINDOW_LEFT_SPLIT.set(leftRegion.getFraction());
    if (rightRegion.getFraction() < 1.0) AppPreferences.WINDOW_RIGHT_SPLIT.set(rightRegion.getFraction());
    if (mainRegion.getFraction() > 0) AppPreferences.WINDOW_MAIN_SPLIT.set(mainRegion.getFraction());
    AppPreferences.DIALOG_DIRECTORY.set(JFileChoosers.getCurrentDirectory());
  }

  void setAttrTableModel(AttrTableModel value) {
    attrTable.setAttrTableModel(value);
    if (value instanceof AttrTableToolModel model) {
      final var tool = model.getTool();
      toolbox.setHaloedTool(tool);
      layoutToolbarModel.setHaloedTool(tool);
    } else {
      toolbox.setHaloedTool(null);
      layoutToolbarModel.setHaloedTool(null);
    }
    if (value instanceof AttrTableComponentModel model) {
      final var circ = model.getCircuit();
      final var comp = model.getComponent();
      layoutCanvas.setHaloedComponent(circ, comp);
    } else {
      layoutCanvas.setHaloedComponent(null, null);
    }
  }

  public void setVhdlSimulatorConsoleStatusVisible() {
    rightRegion.setFraction(lastFraction);
  }

  public void setVhdlSimulatorConsoleStatusInvisible() {
    lastFraction = rightRegion.getFraction();
    rightRegion.setFraction(1);
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
      final var oldModel = attrTable.getAttrTableModel();
      final var same = (oldModel instanceof AttrTableToolModel model) && model.getTool() == oldTool;
      if (!force && !same && !(oldModel instanceof AttrTableCircuitModel)) return;
    }
    if (newAttrs == null) {
      final var circ = project.getCurrentCircuit();
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

  class MyProjectListener implements ProjectListener, LibraryListener, CircuitListener, PropertyChangeListener, ChangeListener {
    public void attributeListChanged(AttributeEvent e) {
      // Do nothing.
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
        buildTitleString();
      }
    }

    private void enableSave() {
      final var ok = getProject().isFileDirty();
      getRootPane().putClientProperty("windowModified", ok);
    }

    @Override
    public void libraryChanged(LibraryEvent e) {
      if (e.getAction() == LibraryEvent.SET_NAME) {
        buildTitleString();
      } else if (e.getAction() == LibraryEvent.DIRTY_STATE) {
        buildTitleString();
        enableSave();
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      final var action = event.getAction();

      if (action == ProjectEvent.ACTION_SET_FILE) {
        buildTitleString();
        project.setTool(project.getOptions().getToolbarData().getFirstTool());
        placeToolbar();
      } else if (action == ProjectEvent.ACTION_SET_STATE) {
        if (event.getData() instanceof CircuitState state) {
          if (state.getParentState() != null) {
            // sim explorer view
            topTab.setSelectedIndex(1);
          }
        }
      } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getData() instanceof Circuit) {
          setEditorView(EDIT_LAYOUT);
          if (appearance != null) {
            appearance.setCircuit(project, project.getCircuitState());
          }
        } else if (event.getData() instanceof HdlModel model) {
          setHdlEditorView(model);
        }
        viewAttributes(project.getTool());
        buildTitleString();
      } else if (action == ProjectEvent.ACTION_SET_TOOL) {
        if (attrTable == null) {
          // for startup
          return;
        }
        final var oldTool = (Tool) event.getOldData();
        final var newTool = (Tool) event.getData();
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
      final var source = event.getSource();
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
