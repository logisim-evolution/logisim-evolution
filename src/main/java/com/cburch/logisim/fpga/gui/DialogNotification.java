/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

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
    GridBagConstraints gbc = new GridBagConstraints();
    JLabel message = new JLabel(string);
    JButton close = new JButton(S.get("FpgaBoardClose"));
    ActionListener actionListener = e -> dialog.dispose();
    close.addActionListener(actionListener);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.ipadx = 20;
    dialog.add(pic, gbc);

    gbc.gridx = 1;
    gbc.gridy = 0;
    dialog.add(message, gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    dialog.add(close, gbc);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
  }
}
