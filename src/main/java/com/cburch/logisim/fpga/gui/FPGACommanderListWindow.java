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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.cburch.logisim.fpga.data.FPGACommanderListModel;

@SuppressWarnings("serial")
public class FPGACommanderListWindow extends JFrame implements WindowListener, ListDataListener {

  private String Title;
  private JList<Object> textArea = new JList<Object>();
  private boolean IsActive = false;
  private boolean count;
  private FPGACommanderListModel model;
  private JScrollPane textMessages;

  public FPGACommanderListWindow(
      String Title, Color fg, boolean count, FPGACommanderListModel model) {
    super((count) ? Title + " (" + model.getCountNr() + ")" : Title);
    this.Title = Title;
    setResizable(true);
    setAlwaysOnTop(false);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    Color bg = Color.black;

    textArea.setBackground(bg);
    textArea.setForeground(fg);
    textArea.setSelectionBackground(fg);
    textArea.setSelectionForeground(bg);
    textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
    textArea.setModel(model);
    textArea.setCellRenderer(model.getMyRenderer());
    textArea.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    model.addListDataListener(this);

    textMessages = new JScrollPane(textArea);
    textMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(textMessages);
    setLocationRelativeTo(null);
    pack();
    addWindowListener(this);
    this.count = count;
    this.model = model;
  }

  public boolean IsActivated() {
    return IsActive;
  }

  public JList<Object> getListObject() {
    return textArea;
  }

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) {
    IsActive = false;
    setVisible(false);
  }

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowActivated(WindowEvent e) {
    IsActive = true;
  }

  @Override
  public void windowDeactivated(WindowEvent e) {}

  @Override
  public void intervalAdded(ListDataEvent e) {}

  @Override
  public void intervalRemoved(ListDataEvent e) {}

  @Override
  public void contentsChanged(ListDataEvent e) {
    setTitle((count) ? Title + " (" + model.getCountNr() + ")" : Title);
    this.revalidate();
    this.repaint();
  }
}
