/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import com.cburch.contracts.BaseListDataListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.fpga.data.FpgaCommanderListModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import lombok.Getter;

@SuppressWarnings("serial")
public class FpgaCommanderListWindow extends JFrame implements BaseWindowListenerContract, BaseListDataListenerContract {

  private final String Title;
  /**
   * text area
   */
  @Getter private final JList<Object> listObject = new JList<>();
  @Getter private boolean activated = false;
  private final boolean shouldCount;
  private final FpgaCommanderListModel model;
  private final JScrollPane textMessages;

  public FpgaCommanderListWindow(String title, Color fg, boolean shouldCount, FpgaCommanderListModel model) {
    super((shouldCount) ? title + " (" + model.getCount() + ")" : title);
    this.Title = title;
    setResizable(true);
    setAlwaysOnTop(false);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    final var bg = Color.black;

    listObject.setBackground(bg);
    listObject.setForeground(fg);
    listObject.setSelectionBackground(fg);
    listObject.setSelectionForeground(bg);
    listObject.setFont(new Font("monospaced", Font.PLAIN, 14));
    listObject.setModel(model);
    listObject.setCellRenderer(model.getMyRenderer());
    listObject.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    model.addListDataListener(this);

    textMessages = new JScrollPane(listObject);
    textMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(textMessages);
    setLocationRelativeTo(null);
    pack();
    addWindowListener(this);
    this.shouldCount = shouldCount;
    this.model = model;
  }

  @Override
  public void windowClosing(WindowEvent e) {
    activated = false;
    setVisible(false);
  }

  @Override
  public void windowActivated(WindowEvent e) {
    activated = true;
  }

  @Override
  public void contentsChanged(ListDataEvent e) {
    setTitle((shouldCount) ? Title + " (" + model.getCount() + ")" : Title);
    this.revalidate();
    this.repaint();
  }
}
