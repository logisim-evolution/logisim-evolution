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
import javax.swing.JLabel;

import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.WarningIcon;

public class DialogNotification {

  public static void showDialogNotification(Frame parent, String type, String string) {
    JDialog dialog = new JDialog(parent, type, true);
    JLabel pic = new JLabel();
    if (type.equals("Warning")) {
      pic.setIcon(new WarningIcon());
    } else {
      pic.setIcon(new ErrorIcon());
    }
    GridBagLayout dialogLayout = new GridBagLayout();
    dialog.setLayout(dialogLayout);
    GridBagConstraints c = new GridBagConstraints();
    JLabel message = new JLabel(string);
    JButton close = new JButton(S.get("FpgaBoardClose"));
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dialog.dispose();
          }
        };
    close.addActionListener(actionListener);

    c.gridx = 0;
    c.gridy = 0;
    c.ipadx = 20;
    dialog.add(pic, c);

    c.gridx = 1;
    c.gridy = 0;
    dialog.add(message, c);

    c.gridx = 1;
    c.gridy = 1;
    dialog.add(close, c);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
  }

}
