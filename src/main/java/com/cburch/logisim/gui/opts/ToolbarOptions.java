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

import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.generic.ProjectExplorerToolNode;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.TableLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

class ToolbarOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final Listener listener = new Listener();
  private final ProjectExplorer explorer;
  private final JButton addTool;
  private final JButton addSeparator;
  private final JButton moveUp;
  private final JButton moveDown;
  private final JButton remove;
  private final ToolbarList list;

  public ToolbarOptions(OptionsFrame window) {
    super(window);
    explorer = new ProjectExplorer(getProject(), true);
    addTool = new JButton();
    addSeparator = new JButton();
    moveUp = new JButton();
    moveDown = new JButton();
    remove = new JButton();

    list = new ToolbarList(getOptions().getToolbarData());

    final var middleLayout = new TableLayout(1);
    final var middle = new JPanel(middleLayout);
    middle.add(addTool);
    middle.add(addSeparator);
    middle.add(moveUp);
    middle.add(moveDown);
    middle.add(remove);
    middleLayout.setRowWeight(4, 1.0);

    explorer.setListener(listener);
    addTool.addActionListener(listener);
    addSeparator.addActionListener(listener);
    moveUp.addActionListener(listener);
    moveDown.addActionListener(listener);
    remove.addActionListener(listener);
    list.addListSelectionListener(listener);
    listener.computeEnabled();

    final var gridbag = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gridbag);
    final var explorerPane =
        new JScrollPane(
            explorer,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    final var listPane =
        new JScrollPane(
            list,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gridbag.setConstraints(explorerPane, gbc);
    add(explorerPane);
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.weightx = 0.0;
    gridbag.setConstraints(middle, gbc);
    add(middle);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gridbag.setConstraints(listPane, gbc);
    add(listPane);
  }

  @Override
  public String getHelpText() {
    return S.get("toolbarHelp");
  }

  @Override
  public String getTitle() {
    return S.get("toolbarTitle");
  }

  @Override
  public void localeChanged() {
    addTool.setText(S.get("toolbarAddTool"));
    addSeparator.setText(S.get("toolbarAddSeparator"));
    moveUp.setText(S.get("toolbarMoveUp"));
    moveDown.setText(S.get("toolbarMoveDown"));
    remove.setText(S.get("toolbarRemove"));
    list.localeChanged();
  }

  private class Listener implements ProjectExplorer.Listener, ActionListener, ListSelectionListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      if (src == addTool) {
        doAddTool(explorer.getSelectedTool().cloneTool());
      } else if (src == addSeparator) {
        getOptions().getToolbarData().addSeparator();
      } else if (src == moveUp) {
        doMove(-1);
      } else if (src == moveDown) {
        doMove(1);
      } else if (src == remove) {
        int index = list.getSelectedIndex();
        if (index >= 0) {
          getProject().doAction(ToolbarActions.removeTool(getOptions().getToolbarData(), index));
          list.clearSelection();
        }
      }
    }

    private void computeEnabled() {
      final var index = list.getSelectedIndex();
      addTool.setEnabled(explorer.getSelectedTool() != null);
      moveUp.setEnabled(index > 0);
      moveDown.setEnabled(index >= 0 && index < list.getModel().getSize() - 1);
      remove.setEnabled(index >= 0);
    }

    private void doAddTool(Tool tool) {
      if (tool != null) {
        getProject().doAction(ToolbarActions.addTool(getOptions().getToolbarData(), tool));
      }
    }

    private void doMove(int delta) {
      final var oldIndex = list.getSelectedIndex();
      final var newIndex = oldIndex + delta;
      final var data = getOptions().getToolbarData();
      if (oldIndex >= 0 && newIndex >= 0 && newIndex < data.size()) {
        getProject().doAction(ToolbarActions.moveTool(data, oldIndex, newIndex));
        list.setSelectedIndex(newIndex);
      }
    }

    @Override
    public void doubleClicked(ProjectExplorer.Event event) {
      final var target = event.getTarget();
      if (target instanceof ProjectExplorerToolNode toolNode) {
        doAddTool(toolNode.getValue());
      }
    }

    @Override
    public JPopupMenu menuRequested(ProjectExplorer.Event event) {
      return null;
    }

    @Override
    public void selectionChanged(ProjectExplorer.Event event) {
      computeEnabled();
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
      computeEnabled();
    }
  }
}
