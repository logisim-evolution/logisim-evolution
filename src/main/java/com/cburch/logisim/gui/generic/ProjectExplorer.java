/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.gui.icons.symbolic.VectorBaseIcon;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;

import com.cburch.logisim.gui.icons.symbolic.CircuitGlyphIcon;
import com.cburch.logisim.gui.main.ComponentGlyphCache;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
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
  private int hoverRow = -1;

  public ProjectExplorer(Project proj, boolean showMouseTools) {
    this(proj, showMouseTools, true);
  }

  public ProjectExplorer(Project proj, boolean showMouseTools, boolean includeCircuits) {
    super();
    this.proj = proj;

    setModel(new ProjectExplorerModel(proj, this, showMouseTools, includeCircuits));
    setRootVisible(false);
    setShowsRootHandles(true);
    setOpaque(false);
    addMouseListener(myListener);
    ToolTipManager.sharedInstance().registerComponent(this);

    MySelectionModel selector = new MySelectionModel();
    selector.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    setSelectionModel(selector);
    setCellRenderer(renderer);
    addTreeSelectionListener(myListener);

    InputMap imap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), deleteAction);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), deleteAction);
    ActionMap amap = getActionMap();
    amap.put(deleteAction, deleteAction);

    // Enter key toggles group expansion or activates tool/circuit
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggleOrActivate");
    amap.put("toggleOrActivate", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final var path = getSelectionPath();
        if (path == null) return;
        final var node = path.getLastPathComponent();
        if (!getModel().isLeaf(node)) {
          if (isExpanded(path)) {
            collapsePath(path);
          } else {
            expandPath(path);
          }
        } else if (listener != null) {
          listener.doubleClicked(new Event(path));
        }
      }
    });

    // Arrow keys expand and collapse
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "expandNode");
    amap.put("expandNode", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final var path = getSelectionPath();
        if (path != null && !isExpanded(path)) {
          expandPath(path);
        }
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "collapseNode");
    amap.put("collapseNode", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final var path = getSelectionPath();
        if (path != null && isExpanded(path)) {
          collapsePath(path);
        }
      }
    });

    proj.addProjectListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    LocaleManager.addLocaleListener(this);

    setToggleClickCount(0);
    setRowHeight(AppPreferences.getScaled(24));
    setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
    putClientProperty(FlatClientProperties.STYLE, "showCellFocusIndicator: false; rowHeight: 24; wideSelection: true;");

    configureTreeUi();

    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int row = getRowForLocation(e.getX(), e.getY());
        if (row != hoverRow) {
          hoverRow = row;
          repaint();
        }
      }
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (hoverRow != -1) {
          hoverRow = -1;
          repaint();
        }
      }
    });
  }

  private void configureTreeUi() {
    setUI(new javax.swing.plaf.basic.BasicTreeUI() {
      @Override
      protected void installDefaults() {
        super.installDefaults();
        setLeftChildIndent(AppPreferences.getScaled(16));
        setRightChildIndent(0);
        if (tree != null) {
          tree.setOpaque(false);
        }
      }

      @Override
      protected boolean isLocationInExpandControl(TreePath path, int mouseX, int mouseY) {
        if (path == null || treeModel.isLeaf(path.getLastPathComponent())) {
          return false;
        }
        final var b = getPathBounds(tree, path);
        if (b == null) return false;
        final var cx = b.x - AppPreferences.getScaled(10);
        final var cy = b.y + b.height / 2;
        final var hitRadius = AppPreferences.getScaled(12);
        return Math.abs(mouseX - cx) <= hitRadius && Math.abs(mouseY - cy) <= hitRadius;
      }

      @Override
      protected void paintExpandControl(
          Graphics g,
          Rectangle clip,
          Insets ins,
          Rectangle b,
          TreePath path,
          int row,
          boolean expanded,
          boolean hasBeen,
          boolean leaf) {
        if (leaf) return;
        final var g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(AppPreferences.getScaled(1.6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean isSel = tree != null && tree.isRowSelected(row);
        Color chevronColor = isSel
            ? Color.WHITE
            : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(184, 191, 198) : new Color(74, 74, 74));
        g2.setColor(chevronColor);

        int cx = b.x - AppPreferences.getScaled(10);
        int cy = b.y + b.height / 2;
        final var p = new Path2D.Float();
        float arm = AppPreferences.getScaled(3.0f);
        if (expanded) {
          p.moveTo(cx - arm, cy - arm * 0.5f);
          p.lineTo(cx, cy + arm * 0.7f);
          p.lineTo(cx + arm, cy - arm * 0.5f);
        } else {
          p.moveTo(cx - arm * 0.5f, cy - arm);
          p.lineTo(cx + arm * 0.7f, cy);
          p.lineTo(cx - arm * 0.5f, cy + arm);
        }
        g2.draw(p);
        g2.dispose();
      }
    });
    setOpaque(false);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    configureTreeUi();
    if (renderer != null) {
      SwingUtilities.updateComponentTreeUI(renderer);
    }
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    final var g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    final var isDarkTree = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    Color accent = isDarkTree ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32);
    Color hoverFill = isDarkTree
        ? new Color(255, 255, 255, 14)
        : new Color(0, 0, 0, 12);
    int treeW = getWidth();

    for (int r = 0; r < getRowCount(); r++) {
      Rectangle b = getRowBounds(r);
      if (b == null) continue;
      boolean isArmed = isRowSelected(r);
      boolean isHover = (r == hoverRow);

      if (isArmed) {
        g2.setColor(accent);
        g2.fill(new RoundRectangle2D.Float(6, b.y + 1, treeW - 12, b.height - 2, 6, 6));
      } else if (isHover) {
        g2.setColor(hoverFill);
        g2.fill(new RoundRectangle2D.Float(6, b.y + 1, treeW - 12, b.height - 2, 6, 6));
      }
    }

    // Vertical Tree Indent Guide Lines
    int rowCount = getRowCount();
    if (rowCount > 0) {
      final Map<TreePath, int[]> spans = new LinkedHashMap<>();
      for (int r = 0; r < rowCount; r++) {
        TreePath path = getPathForRow(r);
        if (path == null) continue;
        for (TreePath anc = path.getParentPath(); anc != null && anc.getPathCount() >= 2; anc = anc.getParentPath()) {
          int ancRow = getRowForPath(anc);
          if (ancRow >= 0 && isExpanded(anc)) {
            int[] span = spans.get(anc);
            if (span == null) {
              Rectangle pb = getRowBounds(ancRow);
              int cx = (pb != null) ? (pb.x - AppPreferences.getScaled(10)) : -1;
              spans.put(anc, new int[]{ancRow, r, cx});
            } else {
              span[1] = r;
            }
          }
        }
      }

      Color guideColor = isDarkTree ? new Color(255, 255, 255, 42) : new Color(0, 0, 0, 36);
      g2.setColor(guideColor);
      g2.setStroke(new BasicStroke(1.0f));

      for (int[] span : spans.values()) {
        int ancRow = span[0];
        int lastRow = span[1];
        int cx = span[2];
        if (cx < 0 || lastRow <= ancRow) continue;

        Rectangle startB = getRowBounds(ancRow);
        Rectangle endB = getRowBounds(lastRow);
        if (startB == null || endB == null) continue;

        int startY = startB.y + startB.height;
        int endY = endB.y + endB.height / 2;
        if (endY > startY) {
          g2.drawLine(cx, startY, cx, endY);
        }
      }
    }

    g2.dispose();
    super.paintComponent(g);
  }

  public void setFilterText(String text) {
    setFilter(text);
  }

  public void setFilter(String filter) {
    if (getModel() instanceof ProjectExplorerModel m) {
      m.setFilter(filter);
      if (filter != null && !filter.isBlank()) {
        expandAllNodes();
      }
    }
  }

  public void expandAllNodes() {
    for (int i = 0; i < getRowCount(); i++) {
      expandRow(i);
    }
  }

  private static String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  public Tool getSelectedTool() {
    final var path = getSelectionPath();
    if (path == null) return null;
    final var last = path.getLastPathComponent();

    return (last instanceof ProjectExplorerToolNode)
        ? ((ProjectExplorerToolNode) last).getValue()
        : null;
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
      final var path = getSelectionPath();
      if (listener != null && path != null && path.getPathCount() == 2) {
        listener.deleteRequested(new Event(path));
      }
      ProjectExplorer.this.requestFocus();
    }
  }

  private class MyCellRenderer extends JPanel implements javax.swing.tree.TreeCellRenderer {

    private static final long serialVersionUID = 1L;
    private final JLabel label = new JLabel();
    private boolean isCurrentCircuit = false;
    private boolean isHovered = false;
    private boolean isRowSelected = false;

    MyCellRenderer() {
      super(new BorderLayout());
      setOpaque(false);
      setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 10));
      label.setIconTextGap(AppPreferences.getScaled(8));
      add(label, BorderLayout.CENTER);
    }

    @Override
    public java.awt.Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {

      final var plainFont = AppPreferences.getScaledFont(tree.getFont()).deriveFont(Font.PLAIN);
      final var boldFont = new Font(plainFont.getFontName(), Font.BOLD, plainFont.getSize());

      isRowSelected = selected;
      isHovered = (row == hoverRow);
      isCurrentCircuit = false;

      Color fgMain = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(235, 235, 235) : new Color(30, 30, 30); // #EBEBEB in Dark, #1E1E1E in Light
      final var isDarkRow = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
      Color accent = isDarkRow ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32);

      label.setFont(plainFont);

      if (value instanceof ProjectExplorerToolNode toolNode) {
        final var tool = toolNode.getValue();
        boolean isCircuit = false;
        boolean viewed = false;

        if (tool instanceof AddTool addTool && proj != null && proj.getFrame() != null) {
          final var fact = addTool.getFactory(false);
          if (fact instanceof SubcircuitFactory sub) {
            isCircuit = true;
            Circuit circ = sub.getSubcircuit();
            viewed = (circ != null && circ == proj.getCurrentCircuit());
          } else if (fact instanceof VhdlEntity vhdlEntity) {
            isCircuit = true;
            VhdlContent vhdl = vhdlEntity.getContent();
            viewed = (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
          }
        }

        if (isCircuit) {
          isCurrentCircuit = viewed;
          final var fact = ((AddTool) tool).getFactory(false);
          final boolean isVhdl = fact instanceof VhdlEntity;
          CircuitGlyphIcon circIcon = new CircuitGlyphIcon(
              viewed, isVhdl, tool.getDisplayName(), AppPreferences.getScaled(20));
          if (viewed) {
            circIcon.setColor(accent);
            label.setForeground(selected ? Color.WHITE : accent);
            label.setFont(boldFont);
          } else {
            circIcon.setColor(selected ? Color.WHITE : fgMain);
            label.setForeground(selected ? Color.WHITE : fgMain);
            label.setFont(plainFont);
          }
          label.setIcon(circIcon);
        } else {
          final var icon = ComponentGlyphCache.getIcon(tool, AppPreferences.getScaled(20));
          label.setIcon(icon != null ? icon : new ToolIcon(tool));
          label.setForeground(selected ? Color.WHITE : fgMain);
          label.setFont(plainFont);
        }

        final var rawName = tool.getDisplayName();
        final var filter = (getModel() instanceof ProjectExplorerModel pem) ? pem.getFilter() : null;
        if (filter != null && !filter.isBlank() && rawName.toLowerCase().contains(filter)) {
          int idx = rawName.toLowerCase().indexOf(filter);
          String b = escapeHtml(rawName.substring(0, idx));
          String m = escapeHtml(rawName.substring(idx, idx + filter.length()));
          String a = escapeHtml(rawName.substring(idx + filter.length()));
          label.setText("<html>" + b + "<span style='color: #3584e4; font-weight: bold;'>" + m + "</span>" + a + "</html>");
        } else {
          label.setText(rawName);
        }
        setToolTipText(tool.getDescription());
      } else if (value instanceof ProjectExplorerLibraryNode libNode) {
        final var lib = libNode.getValue();
        final var baseName = lib.getDisplayName();
        var text = baseName;

        if (lib.isDirty()) {
          text = baseName + " \u25cf";
        }

        int count = libNode.getChildCount();
        if (count > 0) {
          String badgeColor = selected ? "#ffffff" : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? "#8B949E" : "#6E7781");
          text = "<html>" + escapeHtml(text) + "&nbsp;&nbsp;<span style='color: " + badgeColor + "; font-size: 9pt;'>(" + count + ")</span></html>";
        }

        label.setText(text);
        Icon catIcon = getCategoryIcon(baseName, expanded, selected ? Color.WHITE : fgMain);
        label.setIcon(catIcon);
        label.setForeground(selected ? Color.WHITE : fgMain);
        label.setFont(plainFont);
        setToolTipText(null);
      } else {
        label.setText(value != null ? value.toString() : "");
        label.setIcon(null);
      }

      return this;
    }

    private final VectorBaseIcon moduleLibraryIcon = new com.cburch.logisim.gui.icons.symbolic.CategoryModuleIcon();

    private Icon getCategoryIcon(String name, boolean expanded, Color color) {
      if (name == null) return null;
      return moduleLibraryIcon;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
    }
  }

  private class MyListener implements BaseMouseListenerContract, TreeSelectionListener, ProjectListener, PropertyChangeListener {
    private void checkForPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        final var path = getPathForLocation(e.getX(), e.getY());
        if (path != null && listener != null) {
          final var menu = listener.menuRequested(new Event(path));
          if (menu != null) {
            menu.show(ProjectExplorer.this, e.getX(), e.getY());
          }
        }
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        final var path = getPathForLocation(e.getX(), e.getY());
        if (path != null) {
          final var node = path.getLastPathComponent();
          if (node instanceof ProjectExplorerToolNode && listener != null) {
            listener.doubleClicked(new Event(path));
          } else if (node instanceof ProjectExplorerLibraryNode) {
            if (isExpanded(path)) {
              collapsePath(path);
            } else {
              expandPath(path);
            }
          }
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
      final var model = (ProjectExplorerModel) getModel();
      if (model != null && o instanceof Tool) {
        final var node = model.findTool((Tool) o);
        if (node != null) node.fireNodeChanged();
      }
    }

    //
    // project/library file/circuit listener methods
    //
    @Override
    public void projectChanged(ProjectEvent event) {
      final var act = event.getAction();
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
      final var path = e.getNewLeadSelectionPath();
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
      var count = 0;
      for (final var treePath : paths) {
        if (isPathValid(treePath)) ++count;
      }

      if (count == 0) {
        return null;
      } else if (count == paths.length) {
        return paths;
      } else {
        final var ret = new TreePath[count];
        int j = 0;

        for (final var path : paths) {
          if (isPathValid(path)) ret[j++] = path;
        }

        return ret;
      }
    }

    private boolean isPathValid(TreePath path) {
      return isTool(path);
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

  private static boolean isTool(TreePath path) {
    return path != null && path.getLastPathComponent() instanceof ProjectExplorerToolNode;
  }

  private class ToolIcon implements Icon {
    final Tool tool;
    Circuit circ = null;
    VhdlContent vhdl = null;

    ToolIcon(Tool tool) {
      this.tool = tool;
      if (tool instanceof AddTool addTool) {
        final var fact = addTool.getFactory(false);
        if (fact instanceof SubcircuitFactory sub) {
          circ = sub.getSubcircuit();
        } else if (fact instanceof VhdlEntity vhdlEntity) {
          vhdl = vhdlEntity.getContent();
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
      boolean viewed =
          (proj.getFrame().getHdlEditorView() == null)
              ? (circ != null && circ == proj.getCurrentCircuit())
              : (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
      final var haloed = !viewed && (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean());
      // draw halo if appropriate
      if (haloed) {
        final var s = g.getClip();
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
      g.setColor((AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(220, 220, 220) : new Color(40, 40, 40)));
      final var gfxIcon = g.create();
      final var context = new ComponentDrawContext(ProjectExplorer.this, null, null, g, gfxIcon);
      tool.paintIcon(
          context,
          x + AppPreferences.getScaled(AppPreferences.ICON_BORDER),
          y + AppPreferences.getScaled(AppPreferences.ICON_BORDER));
      gfxIcon.dispose();

      // draw magnifying glass if appropriate
      if (viewed) {
        final var tx = x + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 7);
        final var ty = y + AppPreferences.getScaled(AppPreferences.BOX_SIZE - 7);
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
