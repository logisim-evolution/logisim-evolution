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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;

import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;

public class IOComponentSelector implements ActionListener{
	
  private String action_id;
  private JDialog diag;
  private static final String CancelStr = "cancel";
  
  public IOComponentSelector(Frame parrent) {
    action_id = CancelStr;
    diag = new JDialog(parrent, S.get("FpgaBoardIOResources"));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    diag.setLayout(new GridBagLayout());
    JButton button;
    for (String comp : FPGAIOInformationContainer.GetComponentTypes()) {
      button = new JButton(S.fmt("FpgaBoardDefine", comp));
      button.setActionCommand(comp);
      button.addActionListener(this);
      c.gridy++;
      diag.add(button, c);
    }
    JButton cancel = new JButton(S.get("FpgaBoardCancel"));
    cancel.setActionCommand(CancelStr);
    cancel.addActionListener(this);
    c.gridy++;
    diag.add(cancel, c);
    diag.pack();
    diag.setLocationRelativeTo(parrent);
    diag.setModal(true);
    diag.setResizable(false);
    diag.setAlwaysOnTop(true);
  }
  
  public String run() {
    diag.setVisible(true);
    diag.dispose();
    if (action_id.equals(CancelStr)) return null;
    return action_id;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    action_id = e.getActionCommand();
    diag.setVisible(false);
  }

}
