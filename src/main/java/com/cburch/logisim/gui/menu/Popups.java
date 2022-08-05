/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class Popups {
  public static JPopupMenu forCircuit(Project proj, AddTool tool, Circuit circ) {
    return new CircuitPopup(proj, tool, circ);
  }

  public static JPopupMenu forVhdl(Project proj, AddTool tool, VhdlContent vhdl) {
    return new VhdlPopup(proj, tool, vhdl);
  }

  public static JPopupMenu forLibrary(Project proj, Library lib, boolean isTop) {
    return new LibraryPopup(proj, lib, isTop);
  }

  public static JPopupMenu forProject(Project proj) {
    return new ProjectPopup(proj);
  }

  public static JPopupMenu forTool(Project proj, Tool tool) {
    return null;
  }

  @SuppressWarnings("serial")
  private static class CircuitPopup extends JPopupMenu implements ActionListener {
    final Project proj;
    /* Tool tool; */
    final Circuit circuit;
    final JMenuItem analyze = new JMenuItem(S.get("projectAnalyzeCircuitItem"));
    final JMenuItem stats = new JMenuItem(S.get("projectGetCircuitStatisticsItem"));
    final JMenuItem main = new JMenuItem(S.get("projectSetAsMainItem"));
    final JMenuItem remove = new JMenuItem(S.get("projectRemoveCircuitItem"));
    final JMenuItem editLayout = new JMenuItem(S.get("projectEditCircuitLayoutItem"));
    final JMenuItem editAppearance = new JMenuItem(S.get("projectEditCircuitAppearanceItem"));

    CircuitPopup(Project proj, Tool tool, Circuit circuit) {
      super(S.get("circuitMenu"));
      this.proj = proj;
      /* this.tool = tool; */
      this.circuit = circuit;

      add(editLayout);
      editLayout.addActionListener(this);
      add(editAppearance);
      editAppearance.addActionListener(this);
      add(analyze);
      analyze.addActionListener(this);
      add(stats);
      stats.addActionListener(this);
      addSeparator();
      add(main);
      main.addActionListener(this);
      add(remove);
      remove.addActionListener(this);

      final var canChange = proj.getLogisimFile().contains(circuit);
      final var file = proj.getLogisimFile();
      if (circuit == proj.getCurrentCircuit()) {
        if (proj.getFrame().getEditorView().equals(Frame.EDIT_APPEARANCE)) {
          editAppearance.setEnabled(false);
        } else {
          editLayout.setEnabled(false);
        }
      }
      main.setEnabled(canChange && file.getMainCircuit() != circuit);
      remove.setEnabled(
          canChange && file.getCircuitCount() > 1 && proj.getDependencies().canRemove(circuit));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var source = e.getSource();
      if (source == editLayout) {
        proj.setCurrentCircuit(circuit);
        proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
      } else if (source == editAppearance) {
        proj.setCurrentCircuit(circuit);
        proj.getFrame().setEditorView(Frame.EDIT_APPEARANCE);
      } else if (source == analyze) {
        ProjectCircuitActions.doAnalyze(proj, circuit);
      } else if (source == stats) {
        JFrame frame = (JFrame) SwingUtilities.getRoot(this);
        StatisticsDialog.show(frame, proj.getLogisimFile(), circuit);
      } else if (source == main) {
        ProjectCircuitActions.doSetAsMainCircuit(proj, circuit);
      } else if (source == remove) {
        ProjectCircuitActions.doRemoveCircuit(proj, circuit);
      }
    }
  }

  private static class VhdlPopup extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 1L;

    final Project proj;
    final VhdlContent vhdl;
    final JMenuItem edit = new JMenuItem(S.get("projectEditVhdlItem"));
    final JMenuItem remove = new JMenuItem(S.get("projectRemoveVhdlItem"));

    VhdlPopup(Project proj, Tool tool, VhdlContent vhdl) {
      super(S.get("vhdlMenu"));
      this.proj = proj;
      this.vhdl = vhdl;
      add(edit);
      edit.addActionListener(this);
      add(remove);
      remove.addActionListener(this);
      edit.setEnabled(vhdl != proj.getFrame().getHdlEditorView());
      boolean canChange = proj.getLogisimFile().contains(vhdl);
      remove.setEnabled(canChange && proj.getDependencies().canRemove(vhdl));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var source = e.getSource();
      if (source == edit) {
        proj.setCurrentHdlModel(vhdl);
      } else if (source == remove) {
        ProjectCircuitActions.doRemoveVhdl(proj, vhdl);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class LibraryPopup extends JPopupMenu implements ActionListener {
    final Project proj;
    final Library lib;
    final JMenuItem unload = new JMenuItem(S.get("projectUnloadLibraryItem"));
    final JMenuItem reload = new JMenuItem(S.get("projectReloadLibraryItem"));

    LibraryPopup(Project proj, Library lib, boolean isTop) {
      super(S.get("libMenu"));
      this.proj = proj;
      this.lib = lib;

      add(unload);
      unload.addActionListener(this);
      add(reload);
      reload.addActionListener(this);
      unload.setEnabled(isTop);
      reload.setEnabled(isTop && lib instanceof LoadedLibrary);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      if (src == unload) {
        ProjectLibraryActions.doUnloadLibrary(proj, lib);
      } else if (src == reload) {
        proj.getLogisimFile().getLoader().reload((LoadedLibrary) lib);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class ProjectPopup extends JPopupMenu implements ActionListener {
    final Project proj;
    final JMenuItem add = new JMenuItem(S.get("projectAddCircuitItem"));
    final JMenuItem vhdl = new JMenuItem(S.get("projectAddVhdlItem"));
    final JMenu load = new JMenu(S.get("projectLoadLibraryItem"));
    final JMenuItem loadBuiltin = new JMenuItem(S.get("projectLoadBuiltinItem"));
    final JMenuItem loadLogisim = new JMenuItem(S.get("projectLoadLogisimItem"));
    final JMenuItem loadJar = new JMenuItem(S.get("projectLoadJarItem"));

    ProjectPopup(Project proj) {
      super(S.get("projMenu"));
      this.proj = proj;

      load.add(loadBuiltin);
      loadBuiltin.addActionListener(this);
      load.add(loadLogisim);
      loadLogisim.addActionListener(this);
      load.add(loadJar);
      loadJar.addActionListener(this);

      add(add);
      add.addActionListener(this);
      add(vhdl);
      vhdl.addActionListener(this);
      add(load);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      if (src == add) {
        ProjectCircuitActions.doAddCircuit(proj);
      } else if (src == vhdl) {
        ProjectCircuitActions.doAddVhdl(proj);
      } else if (src == loadBuiltin) {
        ProjectLibraryActions.doLoadBuiltinLibrary(proj);
      } else if (src == loadLogisim) {
        ProjectLibraryActions.doLoadLogisimLibrary(proj);
      } else if (src == loadJar) {
        ProjectLibraryActions.doLoadJarLibrary(proj);
      }
    }
  }
}
