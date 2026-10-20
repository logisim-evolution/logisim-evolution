/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BusTransactionInsertionGui extends JFrame
    implements BaseWindowListenerContract, ActionListener, LocaleListener {

  private static final long serialVersionUID = 1L;
  private final SocBusStateInfo myBus;
  private final String myBusId;
  private final JLabel address = new JLabel();
  private final JTextField addrValue = new JTextField(8);
  private final JLabel inputdata = new JLabel();
  private final JTextField inputDataValue = new JTextField(8);
  private final JCheckBox readAction = new JCheckBox();
  private final JCheckBox writeAction = new JCheckBox();
  private final JCheckBox atomicAction = new JCheckBox();
  private final JButton insertButton = new JButton();
  private final JLabel resultLabel = new JLabel();
  private final JTextArea result = new JTextArea(2, 1);
  private final JCheckBox wordTrans = new JCheckBox();
  private final JCheckBox halfTrans = new JCheckBox();
  private final JCheckBox byteTrans = new JCheckBox();
  private final CircuitState circuitState;

  public BusTransactionInsertionGui(SocBusStateInfo bus, String busId, CircuitState state) {
    myBus = bus;
    myBusId = busId;
    LocaleManager.addLocaleListener(this);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.BOTH;
    address.setHorizontalAlignment(JLabel.RIGHT);
    add(address, gbc);
    gbc.gridy++;
    inputdata.setHorizontalAlignment(JLabel.RIGHT);
    add(inputdata, gbc);
    gbc.gridx++;
    inputDataValue.setText("00000000");
    add(inputDataValue, gbc);
    gbc.gridy--;
    addrValue.setText("00000000");
    add(addrValue, gbc);
    gbc.gridx = 0;
    gbc.gridy = 2;
    JPanel pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.add(readAction, BorderLayout.NORTH);
    readAction.addActionListener(this);
    pan.add(writeAction, BorderLayout.CENTER);
    writeAction.addActionListener(this);
    pan.add(atomicAction, BorderLayout.SOUTH);
    atomicAction.addActionListener(this);
    add(pan, gbc);
    gbc.gridx++;
    pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.add(wordTrans, BorderLayout.NORTH);
    wordTrans.setSelected(true);
    wordTrans.addActionListener(this);
    pan.add(halfTrans, BorderLayout.CENTER);
    halfTrans.addActionListener(this);
    pan.add(byteTrans, BorderLayout.SOUTH);
    byteTrans.addActionListener(this);
    add(pan, gbc);
    gbc.gridy++;
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    insertButton.setEnabled(false);
    insertButton.addActionListener(this);
    add(insertButton, gbc);
    gbc.gridy++;
    resultLabel.setHorizontalAlignment(JLabel.CENTER);
    add(resultLabel, gbc);
    gbc.gridy++;
    add(result, gbc);
    localeChanged();
    circuitState = state;
    pack();
  }

  private void insertAction() {
    int addr;
    try {
      addr = Integer.parseUnsignedInt(addrValue.getText(), 16);
    } catch (NumberFormatException e) {
      addr = 0;
    }
    addrValue.setText(String.format("%08X", addr));
    int data;
    try {
      data = Integer.parseUnsignedInt(inputDataValue.getText(), 16);
    } catch (NumberFormatException e) {
      data = 0;
    }
    int action = SocBusTransaction.WORD_ACCESS;
    String format = "%08X";
    if (halfTrans.isSelected()) {
      action = SocBusTransaction.HALF_WORD_ACCESS;
      format = "%04X";
      data &= 0xFFFF;
    }
    if (byteTrans.isSelected()) {
      action = SocBusTransaction.BYTE_ACCESS;
      format = "%02X";
      data &= 0xFF;
    }
    inputDataValue.setText(String.format(format, data));
    int type = 0;
    if (readAction.isSelected()) type |= SocBusTransaction.READ_TRANSACTION;
    if (writeAction.isSelected()) type |= SocBusTransaction.WRITE_TRANSACTION;
    if (atomicAction.isSelected()) type |= SocBusTransaction.ATOMIC_TRANSACTION;
    SocBusTransaction trans =
        new SocBusTransaction(type, addr, data, action, S.get("SocTransInsManual"));
    myBus.getSocSimulationManager().initializeTransaction(trans, myBusId, circuitState);
    String line1 = trans.getShortErrorMessage() + "\n";
    String line2 =
        (trans.isReadTransaction() && !trans.hasError())
            ? S.get("SocTransInsReadData") + String.format("0x%08X", trans.getReadData())
            : " ";
    result.setText(line1 + line2);
  }

  @Override
  public void windowClosing(WindowEvent e) {
    this.dispose();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == readAction || source == writeAction) {
      insertButton.setEnabled(readAction.isSelected() || writeAction.isSelected());
    } else if (source == insertButton) insertAction();
    else if (source == wordTrans) {
      if (wordTrans.isSelected()) {
        halfTrans.setSelected(false);
        byteTrans.setSelected(false);
      } else if (!halfTrans.isSelected() && !byteTrans.isSelected()) wordTrans.setSelected(true);
    } else if (source == halfTrans) {
      if (halfTrans.isSelected()) {
        wordTrans.setSelected(false);
        byteTrans.setSelected(false);
      } else if (!wordTrans.isSelected() && !byteTrans.isSelected()) halfTrans.setSelected(true);
    } else if (source == byteTrans) {
      if (byteTrans.isSelected()) {
        wordTrans.setSelected(false);
        halfTrans.setSelected(false);
      } else if (!wordTrans.isSelected() && !halfTrans.isSelected()) byteTrans.setSelected(true);
    }
  }

  @Override
  public void localeChanged() {
    address.setText(S.get("SocTransInsAddress"));
    inputdata.setText(S.get("SocTransInsInputData"));
    readAction.setText(S.get("SocTransInsReadRequest"));
    writeAction.setText(S.get("SocTransInsWriteRequest"));
    atomicAction.setText(S.get("SocTransInsAtomicRequest"));
    insertButton.setText(S.get("SocTransInsInsertTransaction"));
    resultLabel.setText(S.get("SocTransInsTransResultTitle"));
    wordTrans.setText(S.get("SocTransInsWordAccess"));
    halfTrans.setText(S.get("SocTransInsHalfWordAccess"));
    byteTrans.setText(S.get("SocTransInsByteAccess"));
    pack();
  }
}
