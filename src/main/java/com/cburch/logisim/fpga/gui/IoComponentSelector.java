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

import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;

public class IoComponentSelector implements ActionListener {

  private String action_id;
  private final JDialog diag;
  private static final String CancelStr = "cancel";

  public IoComponentSelector(Frame parent) {
    action_id = CancelStr;
    diag = new JDialog(parent, S.get("FpgaBoardIOResources"));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    diag.setLayout(new GridBagLayout());
    JButton button;
    for (String comp : FpgaIoInformationContainer.getComponentTypes()) {
      button = new JButton(S.get("FpgaBoardDefine", comp));
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
    diag.setLocationRelativeTo(parent);
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
