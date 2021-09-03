/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.gui.icons.TreeIcon;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class ProjectExplorer extends JTree implements LocaleListener {
  public static final Color MAGNIFYING_INTERIOR = new Color(200, 200, 255, 64);
  private static final long serialVersionUID = 1L;

  private final Project proj;
  private final MyListener myListener = new MyListener();
  private final MyCellRenderer renderer = new MyCellRenderer();
  private final DeleteAction deleteAction = new DeleteAction();
  private Listener listener = null;
  private Tool haloedTool = null;

  public ProjectExplorer(Project proj, boolean showMouseTools) {
    super();
    this.proj = proj;

    setModel(new ProjectExplorerModel(proj, this, showMouseTools));
    setRootVisible(true);
    addMouseListener(myListener);
    ToolTipManager.sharedInstance().registerComponent(this);

    MySelectionModel selector = new MySelectionModel();
    selector.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    setSelectionModel(selector);
    setCellRenderer(renderer);
    addTreeSelectionListener(myListener);

    InputMap imap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), deleteAction);
    ActionMap amap = getActionMap();
    amap.put(deleteAction, deleteAction);

    proj.addProjectListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    LocaleManager.addLocaleListener(this);
    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
    renderer.setClosedIcon(new TreeIcon(true));
    renderer.setOpenIcon(new TreeIcon(false));
  }

  public Tool getSelectedTool() {
    TreePath path = getSelectionPath();
    if (path == null) return null;
    Object last = path.getLastPathComponent();

    if (last instanceof ProjectExplorerToolNode) {
      return ((ProjectExplorerToolNode) last).getValue();
    } else {
      return null;
    }
  }

  public void updateStructure() {
    ProjectExplorerModel model = (ProjectExplorerModel) getModel();
    model.updateStructure();
  }

  @Override
  public void localeChanged() {
    // repaint() would work, except that names that get longer will be
    // abbreviated with an ellipsis, even when they fit into the window.
    final ProjectExplorerModel model = (ProjectExplorerModel) getModel();
    model.fireStructureChanged();
  }

  public void setHaloedTool(Tool t) {
    haloedTool = t;
  }

  public void setListener(Listener value) {
    listener = value;
  }

  private class DeleteAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent event) {
      TreePath path = getSelectionPath();
      if (listener != null && path != null && path.getPathCount() == 2) {
        listener.deleteRequested(new Event(path));
      }

      ProjectExplorer.this.requestFocus();
    }
  }

  private class MyCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public java.awt.Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {
      java.awt.Component ret = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
      Font plainFont = AppPreferences.getScaledFont(ret.getFont());
      Font boldFont = new Font(plainFont.getFontName(), Font.BOLD, plainFont.getSize());
      ret.setFont(plainFont);
      if (ret instanceof JComponent) {
        JComponent comp = (JComponent) ret;
        comp.setToolTipText(null);
      }
      if (value instanceof ProjectExplorerToolNode) {
        ProjectExplorerToolNode toolNode = (ProjectExplorerToolNode) value;
        var tool = toolNode.getValue();
        if (ret instanceof JLabel) {
          JLabel label = (JLabel) ret;
          boolean viewed = false;
          if (tool instanceof AddTool && proj != null && proj.getFrame() != null) {
            Circuit circ = null;
            VhdlContent vhdl = null;
            ComponentFactory fact = ((AddTool) tool).getFactory(false);
            if (fact instanceof SubcircuitFactory) {
              circ = ((SubcircuitFactory) fact).getSubcircuit();
            } else if (fact instanceof VhdlEntity) {
              vhdl = ((VhdlEntity) fact).getContent();
            }
            if (proj.getFrame().getHdlEditorView() == null)
              viewed = (circ != null && circ == proj.getCurrentCircuit());
            else viewed = (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
          }
          label.setFont(viewed ? boldFont : plainFont);
          label.setText(tool.getDisplayName());
          label.setIcon(new ToolIcon(tool));
          label.setToolTipText(tool.getDescription());
        }
      } else if (value instanceof ProjectExplorerLibraryNode) {
        final var libNode = (ProjectExplorerLibraryNode) value;
        final var lib = libNode.getValue();

        if (ret instanceof JLabel) {
          final var baseName = lib.getDisplayName();
          var text = baseName;
          if (lib.isDirty()) {
            // TODO: Would be nice to use DIRTY_MARKER here instead of "*" but it does not render
            // corectly in project tree, font seem to have the character as frame title is fine.
            // Needs to figure out what is different (java fonts?). Keep "*" unless bug is resolved.
            final var DIRTY_MARKER_LOCAL = "*"; // useless var for easy DIRTY_MARKER hunt in future.
            text = DIRTY_MARKER_LOCAL + baseName;
          }

          ((JLabel) ret).setText(text);
        }
      }
      return ret;
    }
  }

  private class MyListener implements BaseMouseListenerContract, TreeSelectionListener, ProjectListener, PropertyChangeListener {
    private void checkForPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null && listener != null) {
          JPopupMenu menu = listener.menuRequested(new Event(path));
          if (menu != null) {
            menu.show(ProjectExplorer.this, e.getX(), e.getY());
          }
        }
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null && listener != null) {
          listener.doubleClicked(new Event(path));
        }
      }
    }

    //
    // MouseListener methods
    //
    @Override
    public void mousePressed(MouseEvent e) {
      ProjectExplorer.this.requestFocus();
      checkForPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      checkForPopup(e);
    }

    void changedNode(Object o) {
      ProjectExplorerModel model = (ProjectExplorerModel) getModel();
      if (model != null && o instanceof Tool) {
        ProjectExplorerModel.Node<Tool> node = model.findTool((Tool) o);
        if (node != null) node.fireNodeChanged();
      }
    }

    //
    // project/library file/circuit listener methods
    //
    @Override
    public void projectChanged(ProjectEvent event) {
      int act = event.getAction();
      if (act == ProjectEvent.ACTION_SET_CURRENT || act == ProjectEvent.ACTION_SET_TOOL) {
        changedNode(event.getOldData());
        changedNode(event.getData());
      }
    }

    //
    // PropertyChangeListener methods
    //
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)) {
        ProjectExplorer.this.repaint();
      }
    }

    //
    // TreeSelectionListener methods
    //
    @Override
    public void valueChanged(TreeSelectionEvent e) {
      TreePath path = e.getNewLeadSelectionPath();
      if (listener != null) {
        listener.selectionChanged(new Event(path));
      }
    }
  }

  private static class MySelectionModel extends DefaultTreeSelectionModel {

    private static final long serialVersionUID = 1L;

    @Override
    public void addSelectionPath(TreePath path) {
      if (isPathValid(path)) super.addSelectionPath(path);
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
      paths = getValidPaths(paths);

      if (paths != null) super.addSelectionPaths(paths);
    }

    private TreePath[] getValidPaths(TreePath[] paths) {
      int count = 0;
      for (TreePath treePath : paths) {
        if (isPathValid(treePath)) ++count;
      }

      if (count == 0) {
        return null;
      } else if (count == paths.length) {
        return paths;
      } else {
        TreePath[] ret = new TreePath[count];
        int j = 0;

        for (TreePath path : paths) {
          if (isPathValid(path)) ret[j++] = path;
        }

        return ret;
      }
    }

    private boolean isPathValid(TreePath path) {
      if (path == null || path.getPathCount() > 3) return false;
      Object last = path.getLastPathComponent();

      return last instanceof ProjectExplorerToolNode;
    }

    @Override
    public void setSelectionPath(TreePath path) {
      if (isPathValid(path)) {
        clearSelection();
        super.setSelectionPath(path);
      }
    }

    @Override
    public void setSelectionPaths(TreePath[] paths) {
      paths = getValidPaths(paths);
      if (paths != null) {
        clearSelection();
        super.setSelectionPaths(paths);
      }
    }
  }

  private class ToolIcon implements Icon {

    final Tool tool;
    Circuit circ = null;
    VhdlContent vhdl = null;

    ToolIcon(Tool tool) {
      this.tool = tool;
      if (tool instanceof AddTool) {
        ComponentFactory fact = ((AddTool) tool).getFactory(false);
        if (fact instanceof SubcircuitFactory) {
          circ = ((SubcircuitFactory) fact).getSubcircuit();
        } else if (fact instanceof VhdlEntity) {
          vhdl = ((VhdlEntity) fact).getContent();
        }
      }
    }

    @Override
    public int getIconHeight() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public int getIconWidth() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
      boolean viewed;
      if (proj.getFrame().getHdlEditorView() == null) {
        viewed = (circ != null && circ == proj.getCurrentCircuit());
      } else {
        viewed = (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
      }
      boolean haloed =
          !viewed && (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean());
      // draw halo if appropriate
      if (haloed) {
        Shape s = g.getClip();
        g.clipRect(
            x,
            y,
            AppPreferences.getScaled(AppPreferences.BOX_SIZE),
            AppPreferences.getScaled(AppPreferences.BOX_SIZE));
        g.setColor(Canvas.HALO_COLOR);
        g.setColor(Color.BLACK);
        g.setClip(s);
      }

      // draw tool icon
      var gfxIcon = g.create();
      ComponentDrawContext context =
          new ComponentDrawContext(ProjectExplorer.this, null, null, g, gfxIcon);
      tool.paintIcon(
          context,
          x + AppPreferences.getScaled(AppPreferences.ICON_BORDER),
          y + AppPreferences.getScaled(AppPreferences.ICON_BORDER));
      gfxIcon.dispose();

      // draw magnifying glass if appropriate
      if (viewed) {
        int tx = x + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 7);
        int ty = y + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 7);
        int[] xp = {
          tx - 1,
          x + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 2),
          x + AppPreferences.getScaled(AppPreferences.BOX_SIZE),
          tx + 1
        };
        int[] yp = {
          ty + 1,
          y + AppPreferences.getScaled(AppPreferences.BOX_SIZE),
          y + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 2),
          ty - 1
        };
        g.setColor(MAGNIFYING_INTERIOR);
        g.fillOval(
            x + AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 2),
            y + AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 2),
            AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 1),
            AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 1));
        g.setColor(new Color(139, 69, 19));
        g.drawOval(
            x + AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 2),
            y + AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 2),
            AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 1),
            AppPreferences.getScaled(AppPreferences.BOX_SIZE >> 1));
        g.fillPolygon(xp, yp, xp.length);
      }
    }
  }

  public interface Listener {
    default void deleteRequested(Event event) {
      // no-op implementation
    }

    default void doubleClicked(Event event) {
      // no-op implementation
    }

    JPopupMenu menuRequested(Event event);

    default void moveRequested(Event event, AddTool dragged, AddTool target) {
      // no-op implementation
    }

    default void selectionChanged(Event event) {
      // no-op implementation
    }
  }

  public static class Event {
    private final TreePath path;

    public Event(TreePath p) {
      path = p;
    }

    public TreePath getTreePath() {
      return path;
    }

    public Object getTarget() {
      return path == null ? null : path.getLastPathComponent();
    }
  }
}
