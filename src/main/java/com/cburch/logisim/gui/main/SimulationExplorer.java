/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class SimulationExplorer extends JPanel implements ProjectListener, BaseMouseListenerContract {

  private static final long serialVersionUID = 1L;
  private final Project project;
  private final SimulationTreeModel model;
  private final JTree tree;

  SimulationExplorer(Project proj, MenuListener menu) {
    super(new BorderLayout());
    this.project = proj;

    SimulationToolbarModel toolbarModel = new SimulationToolbarModel(proj, menu);
    Toolbar toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    model = new SimulationTreeModel(proj.getRootCircuitStates());
    model.setCurrentView(project.getCircuitState());
    tree = new ScaledTree(model);
    tree.setCellRenderer(new SimulationTreeRenderer());
    tree.addMouseListener(this);
    tree.setToggleClickCount(3);
    add(new JScrollPane(tree), BorderLayout.CENTER);
    proj.addProjectListener(this);
  }

  private void checkForPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      // do nothing
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2) {
      TreePath path = tree.getPathForLocation(e.getX(), e.getY());
      if (path != null) {
        Object last = path.getLastPathComponent();
        if (last instanceof SimulationTreeCircuitNode node) {
          project.setCircuitState(node.getCircuitState());
        }
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    requestFocus();
    checkForPopup(e);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    checkForPopup(e);
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    int action = event.getAction();
    if (action == ProjectEvent.ACTION_SET_STATE) {
      model.updateSimulationList(project.getRootCircuitStates());
      model.setCurrentView(project.getCircuitState());
      TreePath path = model.mapToPath(project.getCircuitState());
      if (path != null) {
        tree.scrollPathToVisible(path);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class ScaledTree extends JTree {
    public ScaledTree(TreeModel model) {
      super(model);
    }
  }
}
