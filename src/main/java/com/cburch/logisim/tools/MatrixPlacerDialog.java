/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MatrixPlacerDialog extends JPanel implements ActionListener {

  private static final long serialVersionUID = 1L;

  private final MatrixPlacerInfo matrixInfo;
  private final JComboBox<Integer> copiesX = new JComboBox<>();
  private final JComboBox<Integer> copiesY = new JComboBox<>();
  private final JComboBox<Integer> distanceX = new JComboBox<>();
  private final JComboBox<Integer> distanceY = new JComboBox<>();
  private final JTextField labelField = new JTextField();
  private final String compName;

  public MatrixPlacerDialog(MatrixPlacerInfo value, String name, boolean isAutoLabelerActive) {
    super();
    compName = name;
    matrixInfo = value;

    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.weightx = 1;
    gbc.weighty = 1;
    if ((matrixInfo.getLabel() != null) && isAutoLabelerActive) {
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 4;
      add(new JLabel("Base Label:"), gbc);
      gbc.gridx = 4;
      gbc.gridwidth = 7;
      labelField.setText(matrixInfo.getLabel());
      labelField.setEditable(true);
      add(labelField, gbc);
      gbc.gridwidth = 1;
    }
    for (var x = 4; x < 11; x++)
      for (var y = 5; y < 12; y++) {
        gbc.gridx = x;
        gbc.gridy = y;
        final var symbol =
            (((x == 4) || (x == 7) || (x == 10)) && ((y == 5) || (y == 8) || (y == 11)))
                ? "O"
                : ".";
        final var spacer = " ".repeat(3);
        final var compText = new JLabel(spacer + symbol + spacer);
        this.add(compText, gbc);
      }
    gbc.gridy = 1;
    gbc.gridx = 4;
    add(new JLabel("    <--"), gbc);
    gbc.gridx = 10;
    add(new JLabel("-->    "), gbc);
    gbc.gridx = 5;
    gbc.gridwidth = 3;
    add(new JLabel("NrOfColums:"), gbc);
    gbc.gridx = 8;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    for (var i = 1; i < 50; i++) {
      copiesX.addItem(i);
    }
    copiesX.setSelectedItem(1);
    copiesX.addActionListener(this);
    add(copiesX, gbc);
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.gridx = 4;
    add(new JLabel("    <--"), gbc);
    gbc.gridx = 7;
    add(new JLabel("-->    "), gbc);
    gbc.gridx = 5;
    add(new JLabel("dx:"), gbc);
    gbc.gridx = 6;
    for (var i = matrixInfo.getMinimalDisplacementX(); i < 100; i++) {
      distanceX.addItem(i);
    }
    distanceX.setSelectedIndex(0);
    distanceX.addActionListener(this);
    add(distanceX, gbc);

    for (var y = 5; y < 12; y++) {
      gbc.gridy = y;
      if (y == 5) {
        gbc.gridx = 3;
        add(new JLabel("   ^   "), gbc);
        gbc.gridx = 2;
        add(new JLabel("   ^   "), gbc);
      } else if (y == 11) {
        gbc.gridx = 3;
        add(new JLabel("   _   "), gbc);
        gbc.gridx = 2;
        gbc.gridy = 8;
        add(new JLabel("   _   "), gbc);
      } else {
        gbc.gridx = 3;
        add(new JLabel("   |   "), gbc);
        if (y < 8) {
          gbc.gridx = 2;
          add(new JLabel("   |   "), gbc);
        }
      }
    }
    gbc.gridx = 2;
    gbc.gridy = 10;
    for (var i = 1; i < 50; i++) copiesY.addItem(i);
    copiesY.setSelectedIndex(0);
    copiesY.addActionListener(this);
    add(copiesY, gbc);
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    add(new JLabel("NrOfRows:"), gbc);
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = 6;
    add(new JLabel("dy:"), gbc);
    gbc.gridx = 1;
    for (var i = matrixInfo.getMinimalDisplacementY(); i < 100; i++) {
      distanceY.addItem(i);
    }
    distanceY.setSelectedIndex(0);
    distanceY.addActionListener(this);
    add(distanceY, gbc);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (matrixInfo == null) return;

    if (e.getSource() == copiesX) {
      matrixInfo.setCopiesCountX((int) copiesX.getSelectedItem());
    } else if (e.getSource() == distanceX) {
      matrixInfo.setDisplacementX((int) distanceX.getSelectedItem());
    } else if (e.getSource() == copiesY) {
      matrixInfo.setCopiesCountY((int) copiesY.getSelectedItem());
    } else if (e.getSource() == distanceY) {
      matrixInfo.setDisplacementY((int) distanceY.getSelectedItem());
    }
  }

  public boolean execute() {
    labelField.setText(matrixInfo.getLabel());
    boolean ret =
        OptionPane.showOptionDialog(
                null,
                this,
                "Matrix Place component \"" + compName + "\"",
                OptionPane.OK_CANCEL_OPTION,
                OptionPane.PLAIN_MESSAGE,
                null,
                null,
                null)
            == OptionPane.OK_OPTION;
    matrixInfo.setLabel(labelField.getText());
    return ret;
  }
}
