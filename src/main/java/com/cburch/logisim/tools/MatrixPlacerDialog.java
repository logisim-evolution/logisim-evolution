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

    final var thisLayout = new GridBagLayout();
    final var c = new GridBagConstraints();
    setLayout(thisLayout);

    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 1;
    c.weighty = 1;
    if ((matrixInfo.getLabel() != null) && isAutoLabelerActive) {
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 4;
      add(new JLabel("Base Label:"), c);
      c.gridx = 4;
      c.gridwidth = 7;
      labelField.setText(matrixInfo.getLabel());
      labelField.setEditable(true);
      add(labelField, c);
      c.gridwidth = 1;
    }
    for (var x = 4; x < 11; x++)
      for (var y = 5; y < 12; y++) {
        c.gridx = x;
        c.gridy = y;
        final var symbol = (((x == 4) || (x == 7) || (x == 10)) && ((y == 5) || (y == 8) || (y == 11))) ? "O" : ".";
        final var spacer = " ".repeat(3);
        final var compText = new JLabel(spacer + symbol + spacer);
        this.add(compText, c);
      }
    c.gridy = 1;
    c.gridx = 4;
    add(new JLabel("    <--"), c);
    c.gridx = 10;
    add(new JLabel("-->    "), c);
    c.gridx = 5;
    c.gridwidth = 3;
    add(new JLabel("NrOfColums:"), c);
    c.gridx = 8;
    c.gridy = 1;
    c.gridwidth = 2;
    for (var i = 1; i < 50; i++) {
      copiesX.addItem(i);
    }
    copiesX.setSelectedItem(1);
    copiesX.addActionListener(this);
    add(copiesX, c);
    c.gridy = 2;
    c.gridwidth = 1;
    c.gridx = 4;
    add(new JLabel("    <--"), c);
    c.gridx = 7;
    add(new JLabel("-->    "), c);
    c.gridx = 5;
    add(new JLabel("dx:"), c);
    c.gridx = 6;
    for (var i = matrixInfo.getMinimalDisplacementX(); i < 100; i++) {
      distanceX.addItem(i);
    }
    distanceX.setSelectedIndex(0);
    distanceX.addActionListener(this);
    add(distanceX, c);

    for (var y = 5; y < 12; y++) {
      c.gridy = y;
      if (y == 5) {
        c.gridx = 3;
        add(new JLabel("   ^   "), c);
        c.gridx = 2;
        add(new JLabel("   ^   "), c);
      } else if (y == 11) {
        c.gridx = 3;
        add(new JLabel("   _   "), c);
        c.gridx = 2;
        c.gridy = 8;
        add(new JLabel("   _   "), c);
      } else {
        c.gridx = 3;
        add(new JLabel("   |   "), c);
        if (y < 8) {
          c.gridx = 2;
          add(new JLabel("   |   "), c);
        }
      }
    }
    c.gridx = 2;
    c.gridy = 10;
    for (var i = 1; i < 50; i++) copiesY.addItem(i);
    copiesY.setSelectedIndex(0);
    copiesY.addActionListener(this);
    add(copiesY, c);
    c.gridx = 0;
    c.gridwidth = 2;
    add(new JLabel("NrOfRows:"), c);
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 6;
    add(new JLabel("dy:"), c);
    c.gridx = 1;
    for (var i = matrixInfo.getMinimalDisplacementY(); i < 100; i++) {
      distanceY.addItem(i);
    }
    distanceY.setSelectedIndex(0);
    distanceY.addActionListener(this);
    add(distanceY, c);
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
