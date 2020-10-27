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
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

@SuppressWarnings("serial")
public class FPGACommanderTextWindow extends JFrame implements KeyListener, WindowListener {

  private int FontSize;
  private String Title;
  private JTextArea textArea;
  private boolean IsActive = false;
  private boolean count;

  public FPGACommanderTextWindow(String Title, Color fg, boolean count) {
    super((count) ? Title + " (0)" : Title);
    this.Title = Title;
    setResizable(true);
    setAlwaysOnTop(false);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    Color bg = Color.black;
    textArea = new JTextArea(25, 80);
    ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    textArea.setForeground(fg);
    textArea.setBackground(bg);
    textArea.setFont(new Font("monospaced", Font.PLAIN, FontSize));
    textArea.setEditable(false);

    clear();

    JScrollPane textMessages = new JScrollPane(textArea);
    textMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(textMessages);
    setLocationRelativeTo(null);
    textArea.addKeyListener(this);
    pack();
    addWindowListener(this);
    this.count = count;
    FontSize = textMessages.getFont().getSize();
  }

  public boolean IsActivated() {
    return IsActive;
  }

  public void clear() {
    textArea.setText(null);
    if (count) setTitle(Title + " (0)");
  }

  public void set(String line, int LineCount) {
    textArea.setText(line);
    if (count) setTitle(Title + " (" + LineCount + ")");
    Rectangle rect = textArea.getBounds();
    rect.x = 0;
    rect.y = 0;
    if (EventQueue.isDispatchThread()) textArea.paintImmediately(rect);
    else textArea.repaint(rect);
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void keyPressed(KeyEvent e) {
    Rectangle rect;
    switch (e.getKeyCode()) {
      case KeyEvent.VK_EQUALS:
      case KeyEvent.VK_PLUS:
      case KeyEvent.VK_ADD:
        FontSize++;
        textArea.setFont(textArea.getFont().deriveFont((float) FontSize));
        rect = textArea.getBounds();
        rect.x = 0;
        rect.y = 0;
        textArea.paintImmediately(rect);
        break;
      case KeyEvent.VK_MINUS:
      case KeyEvent.VK_SUBTRACT:
        if (FontSize > 8) {
          FontSize--;
          textArea.setFont(textArea.getFont().deriveFont((float) FontSize));
          rect = textArea.getBounds();
          rect.x = 0;
          rect.y = 0;
          textArea.paintImmediately(rect);
        }
        break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {}

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
}
