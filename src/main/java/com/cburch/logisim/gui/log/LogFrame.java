/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.chrono.ChronoPanel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.Getter;

public class LogFrame extends LFrame.SubWindowWithSimulation {
  @Getter private final LogMenuListener menuListener;

  private class MyListener
      implements ProjectListener, LibraryListener, Simulator.Listener, LocaleListener {

    @Override
    public void libraryChanged(LibraryEvent event) {
      final var action = event.getAction();
      if (action == LibraryEvent.SET_NAME) {
        setTitle(computeTitle(model, project));
      }
    }

    @Override
    public void localeChanged() {
      setTitle(computeTitle(model, project));
      for (var i = 0; i < prefPanels.length; i++) {
        tabbedPane.setTitleAt(i, prefPanels[i].getTitle());
        tabbedPane.setToolTipTextAt(i, prefPanels[i].getToolTipText());
        prefPanels[i].localeChanged();
      }
      windowManager.localeChanged();
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      final var action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_STATE) {
        setSimulator(event.getProject().getSimulator(), event.getProject().getCircuitState());
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        setTitle(computeTitle(model, project));
      }
    }

    @Override
    public void simulatorReset(Simulator.Event e) {
      model.simulatorReset();
    }

    @Override
    public void propagationCompleted(Simulator.Event e) {
      model.propagationCompleted(e.didTick(), e.didSingleStep(), e.didPropagate());
    }

    @Override
    public boolean wantProgressEvents() {
      return model.isFine();
    }

    @Override
    public void propagationInProgress(Simulator.Event e) {
      model.propagationCompleted(false, true, false); // treat as a single-step
    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {
      if (setSimulator(project.getSimulator(), project.getCircuitState())) return;
      if (model != null) model.checkForClocks();
    }
  }

  // TODO should automatically repaint icons when component attr change
  // TODO ? moving a component using Select tool removes it from selection
  private class WindowMenuManager extends WindowMenuItemManager implements LocaleListener, ProjectListener, LibraryListener {
    final Project proj;

    WindowMenuManager(Project p) {
      super(S.get("logFrameMenuItem"), false);
      proj = p;
      proj.addProjectListener(this);
      proj.addLibraryListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return LogFrame.this;
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        localeChanged();
      }
    }

    @Override
    public void localeChanged() {
      final var title = proj.getLogisimFile().getDisplayName();
      setText(S.get("logFrameMenuItem", title));
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
        localeChanged();
      }
    }
  }

  private static String computeTitle(Model data, Project proj) {
    final var name = data == null ? "???" : data.getCircuitState().getCircuit().getName();
    return S.get("logFrameTitle", name, proj.getLogisimFile().getDisplayName());
  }

  private static final long serialVersionUID = 1L;
  private Simulator curSimulator = null;
  @Getter private Model model;
  private final Map<CircuitState, Model> modelMap = new HashMap<>();
  private final MyListener myListener = new MyListener();
  private final MyChangeListener myChangeListener = new MyChangeListener();

  private final WindowMenuManager windowManager;
  @Getter private final LogPanel[] prefPanels;
  // private SelectionPanel selPanel;
  private final JTabbedPane tabbedPane;

  static class SelectionDialog extends JDialogOk {
    private static final long serialVersionUID = 1L;
    final SelectionPanel selPanel;

    SelectionDialog(LogFrame logFrame) {
      super("Signal Selection", false);
      selPanel = new SelectionPanel(logFrame);
      selPanel.localeChanged();
      getContentPane().add(selPanel);
      setMinimumSize(new Dimension(AppPreferences.getScaled(350), AppPreferences.getScaled(300)));
      setSize(AppPreferences.getScaled(400), AppPreferences.getScaled(400));
      pack();
      setVisible(true);
    }

    @Override
    public void cancelClicked() {
      okClicked();
    }

    @Override
    public void okClicked() {}
  }

  public JButton makeSelectionButton() {
    final var button = new JButton(S.get("addRemoveSignals"));
    button.addActionListener(event -> SelectionPanel.doDialog(LogFrame.this));
    return button;
  }

  public LogFrame(Project project) {
    super(project);
    windowManager = new WindowMenuManager(project);
    menuListener = new LogMenuListener(logisimMenuBar);
    project.addProjectListener(myListener);
    project.addLibraryListener(myListener);
    setSimulator(project.getSimulator(), project.getCircuitState());

    prefPanels =
        new LogPanel[] {
          new OptionsPanel(this), new ChronoPanel(this),
        };
    tabbedPane = new JTabbedPane();
    // tabbedPane.setFont(new Font("Dialog", Font.BOLD, 9));
    for (LogPanel panel : prefPanels) {
      tabbedPane.addTab(panel.getTitle(), null, panel, panel.getToolTipText());
    }
    tabbedPane.addChangeListener(myChangeListener);
    myChangeListener.stateChanged(null);

    final var contents = getContentPane();
    final var w = Math.max(550, project.getFrame().getWidth());
    var h = 300;
    tabbedPane.setPreferredSize(new Dimension(w, h));
    contents.add(tabbedPane, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
    h = getSize().height;

    // Try to place below circuit window, or at least near bottom of screen,
    // using same width as circuit window.
    final var d = Toolkit.getDefaultToolkit().getScreenSize();
    final var r = project.getFrame().getBounds();
    int x = r.x;
    int y = r.y + r.height;
    if (y + h > d.height) { // too small below circuit
      if (r.y >= h) {
        // plenty of room above circuit
        y = r.y - h;
      } else if (r.y > d.height - h) {
        // circuit is near bottom of screen
        y = 0;
      } else {
        // circuit is near top of screen
        y = d.height - h;
      }
    }
    setLocation(x, y);
    setMinimumSize(new Dimension(300, 200));
    // set initial focus to first panel
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            e.getWindow().removeWindowListener(this);
            myChangeListener.stateChanged(null);
          }
        });
  }

  private boolean setSimulator(Simulator value, CircuitState state) {
    if ((value == null) == (model == null)) {
      if (value == null || value.getCircuitState() == model.getCircuitState()) return false;
    }
    logisimMenuBar.setCircuitState(value, state);

    if (curSimulator != null) curSimulator.removeSimulatorListener(myListener);
    if (model != null) model.setSelected(false);

    final var oldModel = model;
    Model data = null;
    if (value != null) {
      data = modelMap.get(value.getCircuitState());
      if (data == null) {
        data = new Model(value.getCircuitState());
        modelMap.put(data.getCircuitState(), data);
      }
    }
    curSimulator = value;
    model = data;

    if (curSimulator != null) curSimulator.addSimulatorListener(myListener);
    if (model != null) model.setSelected(true);
    setTitle(computeTitle(model, project));
    if (prefPanels != null) {
      for (LogPanel panel : prefPanels) {
        panel.modelChanged(oldModel, model);
      }
    }
    return true;
  }

  @Override
  public void setVisible(boolean value) {
    if (value) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }

  @Override
  public void requestClose() {
    super.requestClose();
    dispose();
  }

  private class MyChangeListener implements ChangeListener {
    @Override
    public void stateChanged(ChangeEvent e) {
      Object selected = tabbedPane.getSelectedComponent();
      if (selected instanceof JScrollPane scrollPane) {
        selected = scrollPane.getViewport().getView();
      }
      if (selected instanceof JPanel panel) {
        panel.requestFocus();
      }
      if (selected instanceof LogPanel tab) {
        menuListener.setEditHandler(tab.getEditHandler());
        menuListener.setPrintHandler(tab.getPrintHandler());
        // menuListener.setSimulationHandler(tab.getSimulationHandler());
        tab.updateTab();
      }
    }
  }
}
