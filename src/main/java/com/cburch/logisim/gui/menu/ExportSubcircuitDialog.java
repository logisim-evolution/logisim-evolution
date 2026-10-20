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
import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class ExportSubcircuitDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;

  private final Circuit mainCircuit;
  private final List<Circuit> dependentCircuits;
  private final String[] currentExportNames;
  private final JCheckBox autoPrefixCheckBox;
  private final JTable table;
  private final DependentsTableModel tableModel;
  private boolean isApproved = false;

  public ExportSubcircuitDialog(Window parent, Circuit mainCircuit, List<Circuit> dependentCircuits) {
    super(parent, S.get("exportSubcircuitTitle", mainCircuit.getName()), ModalityType.APPLICATION_MODAL);
    this.mainCircuit = mainCircuit;
    this.dependentCircuits = dependentCircuits;
    this.currentExportNames = new String[dependentCircuits.size()];

    for (int i = 0; i < dependentCircuits.size(); i++) {
      currentExportNames[i] = mainCircuit.getName() + "_" + dependentCircuits.get(i).getName();
    }

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    final var topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    final var headerLabel = new JLabel(S.get("exportSubcircuitDependenciesHeader"));
    topPanel.add(headerLabel);

    tableModel = new DependentsTableModel();
    table = new JTable(tableModel);
    table.setRowHeight(24);

    final var tableHeader = table.getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setFont(table.getFont());
    final var baseRenderer = tableHeader.getDefaultRenderer();
    tableHeader.setDefaultRenderer(
        (tbl, val, isSel, hasFocus, row, col) -> {
          final var comp = baseRenderer.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
          comp.setFont(tbl.getFont());
          return comp;
        });

    final int rowCount = dependentCircuits.size();
    final int visibleRows = Math.min(Math.max(rowCount, 2), 8);
    final int headerH = table.getTableHeader().getPreferredSize().height;
    final int tableH = (visibleRows * table.getRowHeight()) + (headerH > 0 ? headerH : 24);
    table.setPreferredScrollableViewportSize(new Dimension(520, tableH));

    final var scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

    autoPrefixCheckBox = new JCheckBox(S.get("exportSubcircuitAutoPrefix"), true);
    autoPrefixCheckBox.addActionListener(this);

    final var infoLabel =
        new JLabel(
            "<html><body style='width: 480px;'>"
                + S.get("exportSubcircuitExplanation")
                + "</body></html>");
    infoLabel.setForeground(java.awt.Color.DARK_GRAY);

    final var okButton = new JButton(S.get("exportSubcircuitFileSelect"));
    okButton.setActionCommand("OK");
    okButton.addActionListener(this);
    okButton.setMargin(new java.awt.Insets(4, 16, 4, 16));

    final var cancelButton = new JButton(S.get("showStateDialogCancelButton"));
    cancelButton.setActionCommand("CANCEL");
    cancelButton.addActionListener(this);
    cancelButton.setMargin(new java.awt.Insets(4, 16, 4, 16));

    final var buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    final var bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

    final var cbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    cbPanel.add(autoPrefixCheckBox);
    bottomPanel.add(cbPanel);
    bottomPanel.add(Box.createVerticalStrut(6));

    final var infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    infoPanel.add(infoLabel);
    bottomPanel.add(infoPanel);
    bottomPanel.add(Box.createVerticalStrut(10));

    bottomPanel.add(buttonPanel);

    add(topPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);

    pack();
    setMinimumSize(new Dimension(540, getPreferredSize().height));
    setLocationRelativeTo(parent);
  }

  public boolean isApproved() {
    return isApproved;
  }

  public Map<Circuit, String> getRenamedCircuits() {
    if (!isApproved) return null;
    final var map = new HashMap<Circuit, String>();
    for (int i = 0; i < dependentCircuits.size(); i++) {
      map.put(dependentCircuits.get(i), currentExportNames[i].trim());
    }
    return map;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == autoPrefixCheckBox) {
      final var isAuto = autoPrefixCheckBox.isSelected();
      for (int i = 0; i < dependentCircuits.size(); i++) {
        if (isAuto) {
          currentExportNames[i] = mainCircuit.getName() + "_" + dependentCircuits.get(i).getName();
        } else {
          currentExportNames[i] = dependentCircuits.get(i).getName();
        }
      }
      tableModel.fireTableDataChanged();
    } else if ("OK".equals(e.getActionCommand())) {
      if (table.isEditing()) {
        table.getCellEditor().stopCellEditing();
      }
      if (validateNames()) {
        isApproved = true;
        dispose();
      }
    } else if ("CANCEL".equals(e.getActionCommand())) {
      isApproved = false;
      dispose();
    }
  }

  private boolean validateNames() {
    final var usedNames = new HashSet<String>();
    usedNames.add(mainCircuit.getName().trim().toLowerCase());

    for (int i = 0; i < currentExportNames.length; i++) {
      final var name = currentExportNames[i] == null ? "" : currentExportNames[i].trim();
      if (name.isEmpty()) {
        showError(S.get("circuitNameMissingError"));
        return false;
      }
      final var lowerName = name.toLowerCase();
      if (usedNames.contains(lowerName)) {
        showError(name + ": " + S.get("circuitNameExists"));
        return false;
      }
      usedNames.add(lowerName);
    }
    return true;
  }

  private void showError(String msg) {
    OptionPane.showMessageDialog(this, msg, S.get("exportSubcircuitTitle", mainCircuit.getName()), OptionPane.ERROR_MESSAGE);
  }

  private class DependentsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final String[] columnNames = {
      S.get("exportSubcircuitOriginalName"),
      S.get("exportSubcircuitExportedName")
    };

    @Override
    public int getRowCount() {
      return dependentCircuits.size();
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
      return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        return dependentCircuits.get(rowIndex).getName();
      } else {
        return currentExportNames[rowIndex];
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == 1 && aValue != null) {
        currentExportNames[rowIndex] = aValue.toString();
        fireTableCellUpdated(rowIndex, columnIndex);
      }
    }
  }
}
