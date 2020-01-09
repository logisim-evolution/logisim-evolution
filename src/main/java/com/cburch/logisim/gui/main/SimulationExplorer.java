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

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class SimulationExplorer extends JPanel implements ProjectListener, MouseListener {

  @SuppressWarnings("serial")
  private class ScaledTree extends JTree {
    public ScaledTree(TreeModel model) {
      super(model);
    }
  }

  private static final long serialVersionUID = 1L;
  private Project project;
  private SimulationTreeModel model;
  private JTree tree;

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
    if (e.isPopupTrigger()) {; // do nothing
    }
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2) {
      TreePath path = tree.getPathForLocation(e.getX(), e.getY());
      if (path != null) {
        Object last = path.getLastPathComponent();
        if (last instanceof SimulationTreeCircuitNode) {
          SimulationTreeCircuitNode node;
          node = (SimulationTreeCircuitNode) last;
          project.setCircuitState(node.getCircuitState());
        }
      }
    }
  }

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {}

  public void mousePressed(MouseEvent e) {
    requestFocus();
    checkForPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    checkForPopup(e);
  }

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
}
