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

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == Xcopies) {
      MatrixInfo.setNrOfXCopies((int) Xcopies.getSelectedItem());
    } else if (e.getSource() == Xdistance) {
      MatrixInfo.setXDisplacement((int) Xdistance.getSelectedItem());
    } else if (e.getSource() == Ycopies) {
      MatrixInfo.setNrOfYCopies((int) Ycopies.getSelectedItem());
    } else if (e.getSource() == Ydistance) {
      MatrixInfo.setYisplacement((int) Ydistance.getSelectedItem());
    }
  }

  private final MatrixPlacerInfo MatrixInfo;
  private final JComboBox<Integer> Xcopies = new JComboBox<>();
  private final JComboBox<Integer> Ycopies = new JComboBox<>();
  private final JComboBox<Integer> Xdistance = new JComboBox<>();
  private final JComboBox<Integer> Ydistance = new JComboBox<>();
  private final JTextField Label = new JTextField();
  private final String compName;

  public MatrixPlacerDialog(MatrixPlacerInfo value, String name, boolean AutoLablerActive) {
    super();
    compName = name;
    MatrixInfo = value;

    final var thisLayout = new GridBagLayout();
    final var c = new GridBagConstraints();
    setLayout(thisLayout);

    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 1;
    c.weighty = 1;
    if ((MatrixInfo.GetLabel() != null) & AutoLablerActive) {
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 4;
      add(new JLabel("Base Label:"), c);
      c.gridx = 4;
      c.gridwidth = 7;
      Label.setText(MatrixInfo.GetLabel());
      Label.setEditable(true);
      add(Label, c);
      c.gridwidth = 1;
    }
    for (var x = 4; x < 11; x++)
      for (var y = 5; y < 12; y++) {
        c.gridx = x;
        c.gridy = y;
        if (((x == 4) | (x == 7) | (x == 10)) & ((y == 5) | (y == 8) | (y == 11))) {
          final var CompText = new JLabel("   O   ");
          this.add(CompText, c);
        } else {
          final var CompText = new JLabel("   .   ");
          this.add(CompText, c);
        }
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
      Xcopies.addItem(i);
    }
    Xcopies.setSelectedItem(1);
    Xcopies.addActionListener(this);
    add(Xcopies, c);
    c.gridy = 2;
    c.gridwidth = 1;
    c.gridx = 4;
    add(new JLabel("    <--"), c);
    c.gridx = 7;
    add(new JLabel("-->    "), c);
    c.gridx = 5;
    add(new JLabel("dx:"), c);
    c.gridx = 6;
    for (var i = MatrixInfo.getMinimalXDisplacement(); i < 100; i++) {
      Xdistance.addItem(i);
    }
    Xdistance.setSelectedIndex(0);
    Xdistance.addActionListener(this);
    add(Xdistance, c);

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
    for (var i = 1; i < 50; i++) Ycopies.addItem(i);
    Ycopies.setSelectedIndex(0);
    Ycopies.addActionListener(this);
    add(Ycopies, c);
    c.gridx = 0;
    c.gridwidth = 2;
    add(new JLabel("NrOfRows:"), c);
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 6;
    add(new JLabel("dy:"), c);
    c.gridx = 1;
    for (var i = MatrixInfo.getMinimalYDisplacement(); i < 100; i++) {
      Ydistance.addItem(i);
    }
    Ydistance.setSelectedIndex(0);
    Ydistance.addActionListener(this);
    add(Ydistance, c);
  }

  public boolean execute() {
    Label.setText(MatrixInfo.GetLabel());
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
    MatrixInfo.SetLabel(Label.getText());
    return ret;
  }
}
