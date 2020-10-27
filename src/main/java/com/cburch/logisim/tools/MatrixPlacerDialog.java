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

package com.cburch.logisim.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.logisim.gui.generic.OptionPane;

public class MatrixPlacerDialog extends JPanel implements ActionListener {

  /** */
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

  private MatrixPlacerInfo MatrixInfo;
  private JComboBox<Integer> Xcopies = new JComboBox<>();
  private JComboBox<Integer> Ycopies = new JComboBox<>();
  private JComboBox<Integer> Xdistance = new JComboBox<>();
  private JComboBox<Integer> Ydistance = new JComboBox<>();
  private JTextField Label = new JTextField();
  private String compName;

  public MatrixPlacerDialog(MatrixPlacerInfo value, String name, boolean AutoLablerActive) {
    super();
    compName = name;
    MatrixInfo = value;

    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
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
    for (int x = 4; x < 11; x++)
      for (int y = 5; y < 12; y++) {
        c.gridx = x;
        c.gridy = y;
        if (((x == 4) | (x == 7) | (x == 10)) & ((y == 5) | (y == 8) | (y == 11))) {
          JLabel CompText = new JLabel("   O   ");
          this.add(CompText, c);
        } else {
          JLabel CompText = new JLabel("   .   ");
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
    for (int i = 1; i < 50; i++) {
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
    for (int i = MatrixInfo.getMinimalXDisplacement(); i < 100; i++) {
      Xdistance.addItem(i);
    }
    Xdistance.setSelectedIndex(0);
    Xdistance.addActionListener(this);
    add(Xdistance, c);

    for (int y = 5; y < 12; y++) {
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
    for (int i = 1; i < 50; i++) Ycopies.addItem(i);
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
    for (int i = MatrixInfo.getMinimalYDisplacement(); i < 100; i++) {
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
