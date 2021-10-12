/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.generic.ProjectExplorerToolNode;
import com.cburch.logisim.gui.main.AttrTableToolModel;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

class MouseOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final MyListener listener = new MyListener();
  private final MappingsModel model;
  private final ProjectExplorer explorer;
  private final JPanel addArea = new AddArea();
  private final JTable mappings = new JTable();
  private final AttrTable attrTable;
  private final JButton remove = new JButton();
  private Tool curTool = null;

  public MouseOptions(OptionsFrame window) {
    super(window, new GridLayout(1, 3));

    explorer = new ProjectExplorer(getProject(), true);
    explorer.setListener(listener);

    // Area for adding mappings
    addArea.addMouseListener(listener);

    // Area for viewing current mappings
    model = new MappingsModel();
    mappings.setTableHeader(null);
    mappings.setModel(model);
    mappings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mappings.getSelectionModel().addListSelectionListener(listener);
    mappings.clearSelection();
    final var mapPane = new JScrollPane(mappings);

    // Button for removing current mapping
    final var removeArea = new JPanel();
    remove.addActionListener(listener);
    remove.setEnabled(false);
    removeArea.add(remove);

    // Area for viewing/changing attributes
    attrTable = new AttrTable(getOptionsFrame());

    final var gridbag = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gridbag);
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridheight = 4;
    gbc.fill = GridBagConstraints.BOTH;
    final var explorerPane = new JScrollPane(explorer, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    gridbag.setConstraints(explorerPane, gbc);
    add(explorerPane);
    gbc.weightx = 0.0;
    final var gap = new JPanel();
    gap.setPreferredSize(new Dimension(10, 10));
    gridbag.setConstraints(gap, gbc);
    add(gap);
    gbc.weightx = 1.0;
    gbc.gridheight = 1;
    gbc.gridx = 2;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.weighty = 0.0;
    gridbag.setConstraints(addArea, gbc);
    add(addArea);
    gbc.weighty = 1.0;
    gridbag.setConstraints(mapPane, gbc);
    add(mapPane);
    gbc.weighty = 0.0;
    gridbag.setConstraints(removeArea, gbc);
    add(removeArea);
    gbc.weighty = 1.0;
    gridbag.setConstraints(attrTable, gbc);
    add(attrTable);

    getOptions().getMouseMappings().addMouseMappingsListener(listener);
    setCurrentTool(null);
  }

  @Override
  public String getHelpText() {
    return S.get("mouseHelp");
  }

  @Override
  public String getTitle() {
    return S.get("mouseTitle");
  }

  @Override
  public void localeChanged() {
    remove.setText(S.get("mouseRemoveButton"));
    addArea.repaint();
  }

  private void setCurrentTool(Tool t) {
    curTool = t;
    localeChanged();
  }

  private void setSelectedRow(int row) {
    if (row < 0) row = 0;
    if (row >= model.getRowCount()) row = model.getRowCount() - 1;
    if (row >= 0) {
      mappings.getSelectionModel().setSelectionInterval(row, row);
    }
  }

  private class AddArea extends JPanel {
    private static final long serialVersionUID = 1L;

    public AddArea() {
      setPreferredSize(new Dimension(75, 60));
      setMinimumSize(new Dimension(75, 60));
      setBorder(
          BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderFactory.createEtchedBorder()));
    }

    @Override
    public void paintComponent(Graphics g) {
      if (AppPreferences.AntiAliassing.getBoolean()) {
        final var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      super.paintComponent(g);
      final var sz = getSize();
      g.setFont(remove.getFont());
      String label1;
      String label2;
      if (curTool == null) {
        g.setColor(Color.GRAY);
        label1 = S.get("mouseMapNone");
        label2 = null;
      } else {
        g.setColor(Color.BLACK);
        label1 = S.get("mouseMapText");
        label2 = S.get("mouseMapText2", curTool.getDisplayName());
      }
      final var fm = g.getFontMetrics();
      final var x1 = (sz.width - fm.stringWidth(label1)) / 2;
      if (label2 == null) {
        final var y = Math.max(0, (sz.height - fm.getHeight()) / 2 + fm.getAscent() - 2);
        g.drawString(label1, x1, y);
      } else {
        final var x2 = (sz.width - fm.stringWidth(label2)) / 2;
        var y = Math.max(0, (sz.height - 2 * fm.getHeight()) / 2 + fm.getAscent() - 2);
        g.drawString(label1, x1, y);
        y += fm.getHeight();
        g.drawString(label2, x2, y);
      }
    }
  }

  private class MappingsModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    ArrayList<Integer> curKeys;

    MappingsModel() {
      fireTableStructureChanged();
    }

    // AbstractTableModel methods
    @Override
    public void fireTableStructureChanged() {
      curKeys = new ArrayList<>(getOptions().getMouseMappings().getMappedModifiers());
      Collections.sort(curKeys);
      super.fireTableStructureChanged();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    // other methods
    Integer getKey(int row) {
      return curKeys.get(row);
    }

    int getRow(Integer mods) {
      var row = Collections.binarySearch(curKeys, mods);
      if (row < 0) row = -(row + 1);
      return row;
    }

    @Override
    public int getRowCount() {
      return curKeys.size();
    }

    Tool getTool(int row) {
      if (row < 0 || row >= curKeys.size()) return null;
      return getOptions().getMouseMappings().getToolFor(curKeys.get(row).intValue());
    }

    @Override
    public Object getValueAt(int row, int column) {
      final var key = curKeys.get(row);
      if (column == 0) {
        return InputEventUtil.toDisplayString(key);
      } else {
        return getOptions().getMouseMappings().getToolFor(key).getDisplayName();
      }
    }
  }

  private class MyListener
      implements ActionListener,
          BaseMouseListenerContract,
          ListSelectionListener,
          MouseMappings.MouseMappingsListener,
          ProjectExplorer.Listener {
    //
    // ActionListener method
    //
    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      if (src == remove) {
        var row = mappings.getSelectedRow();
        getProject().doAction(OptionsActions.removeMapping(getOptions().getMouseMappings(), model.getKey(row)));
        row = Math.min(row, model.getRowCount() - 1);
        if (row >= 0) setSelectedRow(row);
      }
    }

    @Override
    public JPopupMenu menuRequested(ProjectExplorer.Event event) {
      return null;
    }


    //
    // MouseMappingsListener method
    //
    @Override
    public void mouseMappingsChanged() {
      model.fireTableStructureChanged();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      // do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (e.getSource() == addArea && curTool != null) {
        final var t = curTool.cloneTool();
        Integer mods = e.getModifiersEx();
        getProject().doAction(OptionsActions.setMapping(getOptions().getMouseMappings(), mods, t));
        setSelectedRow(model.getRow(mods));
      }
    }

    //
    // Explorer.Listener methods
    //
    @Override
    public void selectionChanged(ProjectExplorer.Event event) {
      final var target = event.getTarget();
      if (target instanceof ProjectExplorerToolNode toolNode) {
        setCurrentTool(toolNode.getValue());
      } else {
        setCurrentTool(null);
      }
    }

    //
    // ListSelectionListener method
    //
    @Override
    public void valueChanged(ListSelectionEvent e) {
      int row = mappings.getSelectedRow();
      if (row < 0) {
        remove.setEnabled(false);
        attrTable.setAttrTableModel(null);
      } else {
        remove.setEnabled(true);
        final var tool = model.getTool(row);
        final var proj = getProject();
        final var model = (tool.getAttributeSet() != null) ? new AttrTableToolModel(proj, tool) : null;
        attrTable.setAttrTableModel(model);
      }
    }
  }
}
