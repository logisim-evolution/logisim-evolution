/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.contracts.BaseDocumentListenerContract;
import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.icons.ClearIcon;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;

class Toolbox extends JPanel {
  private static final long serialVersionUID = 1L;
  private final ProjectExplorer toolbox;

  Toolbox(Project proj, Frame frame, MenuListener menu) {
    super(new BorderLayout());

    final var toolbarModel = new ToolboxToolbarModel(frame, menu);
    final var toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    toolbox = new ProjectExplorer(proj, false);
    toolbox.setListener(new ToolboxManip(proj, toolbox));
    add(new JScrollPane(toolbox), BorderLayout.CENTER);
    add(new FilterPanel(), BorderLayout.SOUTH);

    toolbarModel.menuEnableChanged(menu);
  }

  void setHaloedTool(Tool value) {
    toolbox.setHaloedTool(value);
  }

  public void updateStructure() {
    toolbox.updateStructure();
  }

  /** Tree filter input */
  private class FilterPanel extends JPanel implements BaseDocumentListenerContract, LocaleListener {
    private static final long serialVersionUID = 1L;
    private static final int GAP = 3;
    private static final String CLEAR_FILTER_ACTION = "clearFilter";

    private final JLabel label = new JLabel();
    private final JTextField field = new JTextField();
    private final JButton clearButton = new JButton(new ClearIcon());

    FilterPanel() {
      super(new BorderLayout(GAP, 0));
      setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

      label.setFont(AppPreferences.getScaledFont(label.getFont()));
      field.setFont(AppPreferences.getScaledFont(field.getFont()));
      field.getDocument().addDocumentListener(this);

      // Pressing ECS whike input has focus clears the content of the input filter.
      final var clearAction = new ClearFilterAction();
      field.getInputMap(JComponent.WHEN_FOCUSED)
          .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLEAR_FILTER_ACTION);
      field.getActionMap().put(CLEAR_FILTER_ACTION, clearAction);
      clearButton.addActionListener(clearAction);
      clearButton.setBorderPainted(false);
      clearButton.setContentAreaFilled(false);
      clearButton.setFocusable(false);
      clearButton.setMargin(new Insets(0, 0, 0, 0));
      clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

      add(label, BorderLayout.WEST);
      add(field, BorderLayout.CENTER);
      add(clearButton, BorderLayout.EAST);

      LocaleManager.addLocaleListener(this);
      localeChanged();
    }

    private void filterChanged() {
      toolbox.setFilterText(field.getText());
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
      filterChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
      filterChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
      filterChanged();
    }

    @Override
    public void localeChanged() {
      label.setText(S.get("toolboxFilterLabel"));
      final var tip = S.get("toolboxFilterTip");
      label.setToolTipText(tip);
      field.setToolTipText(tip);
      clearButton.setToolTipText(S.get("toolboxFilterClearTip"));
    }

    private class ClearFilterAction extends AbstractAction {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent event) {
        field.setText("");
      }
    }
  }
}
