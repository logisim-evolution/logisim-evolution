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

package com.cburch.logisim.gui.menu;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.appear.RevertAppearanceAction;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.SimulateListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class MainMenuListener extends MenuListener {

  protected class FileListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      Project proj = frame.getProject();
      if (src == LogisimMenuBar.EXPORT_IMAGE) {
        ExportImage.doExport(proj);
      } else if (src == LogisimMenuBar.PRINT) {
        Print.doPrint(proj);
      }
    }

    public void register() {
      menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
      menubar.addActionListener(LogisimMenuBar.PRINT, this);
    }
  }

  protected class ProjectMenuListener
      implements ProjectListener,
          LibraryListener,
          ActionListener,
          PropertyChangeListener,
          CanvasModelListener {
    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      Project proj = frame.getProject();
      Circuit cur = proj == null ? null : proj.getCurrentCircuit();
      if (src == LogisimMenuBar.ADD_CIRCUIT) {
        ProjectCircuitActions.doAddCircuit(proj);
      } else if (src == LogisimMenuBar.ADD_VHDL) {
        ProjectCircuitActions.doAddVhdl(proj);
      } else if (src == LogisimMenuBar.IMPORT_VHDL) {
        ProjectCircuitActions.doImportVhdl(proj);
      } else if (src == LogisimMenuBar.MOVE_CIRCUIT_UP) {
        ProjectCircuitActions.doMoveCircuit(proj, cur, -1);
      } else if (src == LogisimMenuBar.MOVE_CIRCUIT_DOWN) {
        ProjectCircuitActions.doMoveCircuit(proj, cur, 1);
      } else if (src == LogisimMenuBar.SET_MAIN_CIRCUIT) {
        ProjectCircuitActions.doSetAsMainCircuit(proj, cur);
      } else if (src == LogisimMenuBar.REMOVE_CIRCUIT) {
        ProjectCircuitActions.doRemoveCircuit(proj, cur);
      } else if (src == LogisimMenuBar.EDIT_LAYOUT) {
        frame.setEditorView(Frame.EDIT_LAYOUT);
      } else if (src == LogisimMenuBar.EDIT_APPEARANCE) {
        frame.setEditorView(Frame.EDIT_APPEARANCE);
      } else if (src == LogisimMenuBar.TOGGLE_APPEARANCE) {
          boolean viewAppearance = frame.getEditorView().equals(Frame.EDIT_APPEARANCE);
          frame.setEditorView(viewAppearance ? Frame.EDIT_LAYOUT : Frame.EDIT_APPEARANCE);
      } else if (src == LogisimMenuBar.REVERT_APPEARANCE) {
        proj.doAction(new RevertAppearanceAction(cur));
      } else if (src == LogisimMenuBar.ANALYZE_CIRCUIT && Main.ANALYZE) {
        ProjectCircuitActions.doAnalyze(proj, cur);
      } else if (src == LogisimMenuBar.CIRCUIT_STATS) {
        StatisticsDialog.show(frame, proj.getLogisimFile(), cur);
      }
    }

    public void computeEnabled() {
      Project proj = frame == null ? null : frame.getProject();
      LogisimFile file = proj == null ? null : proj.getLogisimFile();
      Circuit cur = proj == null ? null : proj.getCurrentCircuit();
      int curIndex = file == null ? -1 : file.indexOfCircuit(cur);
      boolean isProjectCircuit = curIndex >= 0;
      String editorView = frame == null ? "" : frame.getEditorView();
      boolean canSetMain = false;
      boolean canMoveUp = false;
      boolean canMoveDown = false;
      boolean canRemove = false;
      boolean canRevert = false;
      boolean viewAppearance = editorView.equals(Frame.EDIT_APPEARANCE);
      boolean viewLayout = editorView.equals(Frame.EDIT_LAYOUT);
      if (isProjectCircuit) {
        List<?> tools = proj.getLogisimFile().getTools();

        canSetMain = proj.getLogisimFile().getMainCircuit() != cur;
        canMoveUp = curIndex > 0;
        canMoveDown = curIndex < tools.size() - 1;
        canRemove = tools.size() > 1;
        canRevert = viewAppearance && !cur.getAppearance().isDefaultAppearance();
      }

      menubar.setEnabled(LogisimMenuBar.ADD_CIRCUIT, true);
      menubar.setEnabled(LogisimMenuBar.ADD_VHDL, true);
      menubar.setEnabled(LogisimMenuBar.IMPORT_VHDL, true);
      menubar.setEnabled(LogisimMenuBar.MOVE_CIRCUIT_UP, canMoveUp);
      menubar.setEnabled(LogisimMenuBar.MOVE_CIRCUIT_DOWN, canMoveDown);
      menubar.setEnabled(LogisimMenuBar.SET_MAIN_CIRCUIT, canSetMain);
      menubar.setEnabled(LogisimMenuBar.REMOVE_CIRCUIT, canRemove);
      menubar.setEnabled(LogisimMenuBar.EDIT_LAYOUT, viewAppearance);
      menubar.setEnabled(LogisimMenuBar.EDIT_APPEARANCE, viewLayout);
      menubar.setEnabled(LogisimMenuBar.TOGGLE_APPEARANCE, true);
      menubar.setEnabled(LogisimMenuBar.REVERT_APPEARANCE, canRevert);
      menubar.setEnabled(LogisimMenuBar.ANALYZE_CIRCUIT, true);
      menubar.setEnabled(LogisimMenuBar.CIRCUIT_STATS, true);
      fireEnableChanged();
    }

    protected void computeRevertEnabled() {
      // do this separately since it can happen rather often
      Project proj = frame.getProject();
      LogisimFile file = proj.getLogisimFile();
      Circuit cur = proj.getCurrentCircuit();
      boolean isProjectCircuit = file.contains(cur);
      boolean viewAppearance = frame.getEditorView().equals(Frame.EDIT_APPEARANCE);
      boolean canRevert =
          isProjectCircuit && viewAppearance && !cur.getAppearance().isDefaultAppearance();
      boolean oldValue = menubar.isEnabled(LogisimMenuBar.REVERT_APPEARANCE);
      if (canRevert != oldValue) {
        menubar.setEnabled(LogisimMenuBar.REVERT_APPEARANCE, canRevert);
        fireEnableChanged();
      }
    }

    public void libraryChanged(LibraryEvent event) {
      computeEnabled();
    }

    public void modelChanged(CanvasModelEvent event) {
      computeRevertEnabled();
    }

    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getOldData() instanceof Circuit) {
          Circuit old = (Circuit) event.getOldData();
          old.getAppearance().removeCanvasModelListener(this);
        }
        if (event.getData() instanceof Circuit) {
          Circuit circ = (Circuit) event.getData();
          circ.getAppearance().addCanvasModelListener(this);
        }
        computeEnabled();
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        computeEnabled();
      }
    }

    public void propertyChange(PropertyChangeEvent e) {
      computeEnabled();
    }

    void register() {
      Project proj = frame.getProject();
      if (proj == null) {
        return;
      }

      proj.addProjectListener(this);
      proj.addLibraryListener(this);
      frame.addPropertyChangeListener(Frame.EDITOR_VIEW, this);
      frame.addPropertyChangeListener(Frame.EXPLORER_VIEW, this);
      Circuit circ = proj.getCurrentCircuit();
      if (circ != null) {
        circ.getAppearance().addCanvasModelListener(this);
      }

      menubar.addActionListener(LogisimMenuBar.ADD_CIRCUIT, this);
      menubar.addActionListener(LogisimMenuBar.ADD_VHDL, this);
      menubar.addActionListener(LogisimMenuBar.IMPORT_VHDL, this);
      menubar.addActionListener(LogisimMenuBar.MOVE_CIRCUIT_UP, this);
      menubar.addActionListener(LogisimMenuBar.MOVE_CIRCUIT_DOWN, this);
      menubar.addActionListener(LogisimMenuBar.SET_MAIN_CIRCUIT, this);
      menubar.addActionListener(LogisimMenuBar.REMOVE_CIRCUIT, this);
      menubar.addActionListener(LogisimMenuBar.EDIT_LAYOUT, this);
      menubar.addActionListener(LogisimMenuBar.EDIT_APPEARANCE, this);
      menubar.addActionListener(LogisimMenuBar.TOGGLE_APPEARANCE, this);
      menubar.addActionListener(LogisimMenuBar.REVERT_APPEARANCE, this);
      menubar.addActionListener(LogisimMenuBar.ANALYZE_CIRCUIT, this);
      menubar.addActionListener(LogisimMenuBar.CIRCUIT_STATS, this);

      computeEnabled();
    }
  }

  protected class SimulateMenuListener implements ProjectListener, SimulateListener {
    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_STATE) {
        menubar.setCircuitState(
            frame.getProject().getSimulator(), frame.getProject().getCircuitState());
      }
    }

    void register() {
      Project proj = frame.getProject();
      proj.addProjectListener(this);
      menubar.setSimulateListener(this);
      menubar.setCircuitState(proj.getSimulator(), proj.getCircuitState());
    }

    public void stateChangeRequested(Simulator sim, CircuitState state) {
      if (state != null) frame.getProject().setCircuitState(state);
    }
  }

  protected Frame frame;
  protected FileListener fileListener = new FileListener();
  protected ProjectMenuListener projectListener = new ProjectMenuListener();
  protected SimulateMenuListener simulateListener = new SimulateMenuListener();

  public MainMenuListener(Frame frame, LogisimMenuBar menubar) {
    super(menubar);
    this.frame = frame;
  }

  public void register(CardPanel mainPanel) {
    fileListener.register();
    editListener.register();
    projectListener.register();
    simulateListener.register();
  }

}
