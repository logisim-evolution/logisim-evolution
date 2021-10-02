/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

@SuppressWarnings("serial")
public class FpgaCommanderTextWindow extends JFrame implements BaseKeyListenerContract, BaseWindowListenerContract {

  private int FontSize;
  private final String Title;
  private final JTextArea textArea;
  private boolean IsActive = false;
  private final boolean count;

  public FpgaCommanderTextWindow(String Title, Color fg, boolean count) {
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

  public boolean isActivated() {
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
  public void windowClosing(WindowEvent e) {
    IsActive = false;
    setVisible(false);
  }

  @Override
  public void windowActivated(WindowEvent e) {
    IsActive = true;
  }
}
