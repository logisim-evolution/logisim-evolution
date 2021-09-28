/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.appear.RevertAppearanceAction;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class MainMenuListener extends MenuListener {

  protected final Frame frame;
  protected final FileListener fileListener = new FileListener();
  protected final ProjectMenuListener projectListener = new ProjectMenuListener();
  protected final SimulateMenuListener simulateListener = new SimulateMenuListener();

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

  protected class FileListener implements ActionListener {
    @Override
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
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      final var proj = frame.getProject();
      final var cur = proj == null ? null : proj.getCurrentCircuit();
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
      } else if (src == LogisimMenuBar.ANALYZE_CIRCUIT) {
        ProjectCircuitActions.doAnalyze(proj, cur);
      } else if (src == LogisimMenuBar.CIRCUIT_STATS) {
        StatisticsDialog.show(frame, proj.getLogisimFile(), cur);
      }
    }

    public void computeEnabled() {
      final var proj = frame == null ? null : frame.getProject();
      final var file = proj == null ? null : proj.getLogisimFile();
      final var cur = proj == null ? null : proj.getCurrentCircuit();
      final var curIndex = file == null ? -1 : file.indexOfCircuit(cur);
      final var editorView = frame == null ? "" : frame.getEditorView();
      final var viewAppearance = editorView.equals(Frame.EDIT_APPEARANCE);
      final var viewLayout = editorView.equals(Frame.EDIT_LAYOUT);

      var canSetMain = false;
      var canMoveUp = false;
      var canMoveDown = false;
      var canRemove = false;
      var canRevert = false;

      // is project circuit?
      if (curIndex >= 0) {
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
      final var proj = frame.getProject();
      final var file = proj.getLogisimFile();
      final var cur = proj.getCurrentCircuit();
      final var isProjectCircuit = file.contains(cur);
      final var viewAppearance = frame.getEditorView().equals(Frame.EDIT_APPEARANCE);
      final var canRevert = isProjectCircuit && viewAppearance && !cur.getAppearance().isDefaultAppearance();
      final var oldValue = menubar.isEnabled(LogisimMenuBar.REVERT_APPEARANCE);
      if (canRevert != oldValue) {
        menubar.setEnabled(LogisimMenuBar.REVERT_APPEARANCE, canRevert);
        fireEnableChanged();
      }
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      computeEnabled();
    }

    @Override
    public void modelChanged(CanvasModelEvent event) {
      computeRevertEnabled();
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      final var action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getOldData() instanceof Circuit old) {
          old.getAppearance().removeCanvasModelListener(this);
        }
        if (event.getData() instanceof Circuit circ) {
          circ.getAppearance().addCanvasModelListener(this);
        }
        computeEnabled();
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        computeEnabled();
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
      computeEnabled();
    }

    void register() {
      final var proj = frame.getProject();
      if (proj == null) return;

      proj.addProjectListener(this);
      proj.addLibraryListener(this);
      frame.addPropertyChangeListener(Frame.EDITOR_VIEW, this);
      frame.addPropertyChangeListener(Frame.EXPLORER_VIEW, this);
      final var circ = proj.getCurrentCircuit();
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
    @Override
    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_STATE) {
        menubar.setCircuitState(
            frame.getProject().getSimulator(), frame.getProject().getCircuitState());
      }
    }

    void register() {
      final var proj = frame.getProject();
      proj.addProjectListener(this);
      menubar.setSimulateListener(this);
      menubar.setCircuitState(proj.getSimulator(), proj.getCircuitState());
    }

    @Override
    public void stateChangeRequested(Simulator sim, CircuitState state) {
      if (state != null) frame.getProject().setCircuitState(state);
    }
  }
}
