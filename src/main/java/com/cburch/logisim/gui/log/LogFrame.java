/**
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

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.chronogram.chronodata.TimelineParam;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class LogFrame extends LFrame implements KeyListener{
  private class MyListener
      implements 
          ProjectListener,
          LibraryListener,
          SimulatorListener,
          LocaleListener {

	public void libraryChanged(LibraryEvent event) {
      int action = event.getAction();
      if (action == LibraryEvent.SET_NAME) {
        setTitle(computeTitle(curModel, project));
      }
    }

    public void localeChanged() {
      setTitle(computeTitle(curModel, project));
      for (int i = 0; i < panels.length; i++) {
        tabbedPane.setTitleAt(i, panels[i].getTitle());
        tabbedPane.setToolTipTextAt(i, panels[i].getToolTipText());
        panels[i].localeChanged();
      }
      windowManager.localeChanged();
    }

    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_STATE) {
        setSimulator(event.getProject().getSimulator(), event.getProject().getCircuitState());
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        setTitle(computeTitle(curModel, project));
      }
    }

    public void propagationCompleted(SimulatorEvent e) {
      curModel.propagationCompleted();
    }

    public void simulatorStateChanged(SimulatorEvent e) {}

    public void tickCompleted(SimulatorEvent e) {}
  }

  // TODO should automatically repaint icons when component attr change
  // TODO ? moving a component using Select tool removes it from selection
  private class WindowMenuManager extends WindowMenuItemManager
      implements LocaleListener, ProjectListener, LibraryListener {
    WindowMenuManager() {
      super(S.get("logFrameMenuItem"), false);
      project.addProjectListener(this);
      project.addLibraryListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return LogFrame.this;
    }

    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        localeChanged();
      }
    }

    public void localeChanged() {
      String title = project.getLogisimFile().getDisplayName();
      setText(StringUtil.format(S.get("logFrameMenuItem"), title));
    }

    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
        localeChanged();
      }
    }
  }

  private static String computeTitle(Model data, Project proj) {
    String name = data == null ? "???" : data.getCircuitState().getCircuit().getName();
    return StringUtil.format(S.get("logFrameTitle"), name, proj.getLogisimFile().getDisplayName());
  }

  private static final long serialVersionUID = 1L;
  private Project project;
  private Simulator curSimulator = null;
  private Model curModel;
  private Map<CircuitState, Model> modelMap = new HashMap<CircuitState, Model>();
  private MyListener myListener = new MyListener();

  private WindowMenuManager windowManager;
  private LogPanel[] panels;
  private JTabbedPane tabbedPane;

  public LogFrame(Project project) {
    super(false,project);
    this.project = project;
    this.windowManager = new WindowMenuManager();
    project.addProjectListener(myListener);
    project.addLibraryListener(myListener);
    setSimulator(project.getSimulator(), project.getCircuitState());

    panels =
        new LogPanel[] {
          new SelectionPanel(this), new ScrollPanel(this), new FilePanel(this),
       // chrono goes here
        };
    tabbedPane = new JTabbedPane();
    tabbedPane.addKeyListener(this);
    for (int index = 0; index < panels.length; index++) {
      LogPanel panel = panels[index];
      panels[index].addKeyListener((this));
      tabbedPane.addTab(panel.getTitle(), null, panel, panel.getToolTipText());
    }

    Container contents = getContentPane();
    tabbedPane.setPreferredSize(new Dimension(550, 300));
    contents.add(tabbedPane, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
  }

  public Model getModel() {
    return curModel;
  }

  LogPanel[] getPrefPanels() {
    return panels;
  }

  public Project getProject() {
    return project;
  }

  public TimelineParam getTimelineParam() {
    SelectionPanel p = (SelectionPanel) panels[0];
    return p.getTimelineParam();
  }

  private void setSimulator(Simulator value, CircuitState state) {
    if ((value == null) == (curModel == null)) {
      if (value == null || value.getCircuitState() == curModel.getCircuitState()) 
    	  return;
    }

    if (curSimulator != null) curSimulator.removeSimulatorListener(myListener);
    if (curModel != null) curModel.setSelected(this, false);

    Model oldModel = curModel;
    Model data = null;
    if (value != null) {
      data = modelMap.get(value.getCircuitState());
      if (data == null) {
        data = new Model(value.getCircuitState());
        modelMap.put(data.getCircuitState(), data);
      }
    }
    curSimulator = value;
    curModel = data;

    if (curSimulator != null) curSimulator.addSimulatorListener(myListener);
    if (curModel != null) curModel.setSelected(this, true);
    setTitle(computeTitle(curModel, project));
    if (panels != null) {
      for (int i = 0; i < panels.length; i++) {
        panels[i].modelChanged(oldModel, curModel);
      }
    }
  }

  @Override
  public void setVisible(boolean value) {
    if (value) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }

  @Override
  public void keyPressed(KeyEvent ke) {
    int keyCode = ke.getKeyCode();
    if (keyCode == KeyEvent.VK_F2) {
      curSimulator.tick(2);
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
}
