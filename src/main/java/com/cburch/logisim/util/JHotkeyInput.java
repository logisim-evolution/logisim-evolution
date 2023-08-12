package com.cburch.logisim.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.Flow;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;

public class JHotkeyInput extends JPanel {
  private final JButton resetButton = new JButton("❌");
  private final JButton applyButton = new JButton("✓");
  private final JTextField hotkeyInputField;
  private boolean preferredWidthSet = false;
  private String previousData = "";

  public JHotkeyInput(JFrame frame, String text) {
    setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    hotkeyInputField = new JTextField(text);
    setBorder(hotkeyInputField.getBorder());
    hotkeyInputField.setHorizontalAlignment(SwingConstants.CENTER);
    hotkeyInputField.setBackground(getBackground());
    hotkeyInputField.setBorder(BorderFactory.createEmptyBorder());
    hotkeyInputField.addKeyListener(new HotkeyInputKeyListener(this));
    hotkeyInputField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        previousData = hotkeyInputField.getText();
        hotkeyInputField.setText("");
        applyButton.setVisible(true);
        resetButton.setVisible(true);
      }

      @Override
      public void focusLost(FocusEvent e) {
        /* TODO: decide whether the data changes*/
        hotkeyInputField.setText(previousData);
        applyButton.setVisible(false);
        resetButton.setVisible(false);
      }
    });
    applyButton.setBorder(BorderFactory.createEmptyBorder());
    applyButton.setVisible(false);
    resetButton.setBorder(BorderFactory.createEmptyBorder());
    resetButton.setVisible(false);
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        applyButton.setVisible(false);
        resetButton.setVisible(false);
        frame.requestFocus();
      }
    });
    new Timer(200, e -> {
      if (!preferredWidthSet) {
        Font font = resetButton.getFont();
        applyButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
        applyButton.setPreferredSize(new Dimension(20, 20));
        resetButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
        resetButton.setPreferredSize(new Dimension(20, 20));
        hotkeyInputField.setPreferredSize(new Dimension(getWidth()+20, 30));
        preferredWidthSet = true;
      }
    }).start();
    add(hotkeyInputField);
    add(applyButton);
    add(resetButton);
  }

  /* TODO: use when user inputs a valid key binding */
  public void updateLayout() {
    preferredWidthSet = false;
  }

  public void setText(String s) {
    hotkeyInputField.setText(s);
  }
}

class HotkeyInputKeyListener implements KeyListener {
  private final JHotkeyInput hotkeyInput;
  private int modifier = 0;
  private int code = 0;

  public HotkeyInputKeyListener(JHotkeyInput hotkeyInput) {
    this.hotkeyInput = hotkeyInput;
  }

  @Override
  public void keyTyped(KeyEvent e) {
    /* GET KEYCHAR */
  }

  @Override
  public void keyPressed(KeyEvent e) {
    modifier = e.getModifiersEx();
    code = e.getKeyCode();
//    hotkeyInput.updateLayout();
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (code == 0
        || code == KeyEvent.VK_CONTROL
        || code == KeyEvent.VK_ALT
        || code == KeyEvent.VK_SHIFT
        || code == KeyEvent.VK_META) {
      return;
    }
    String modifierString = InputEvent.getModifiersExText(modifier);
    String keyStr="";
    if (modifierString.isEmpty()) {
      keyStr=KeyEvent.getKeyText(code);
    } else {
      keyStr=InputEvent.getModifiersExText(modifier) + "+" + KeyEvent.getKeyText(code);
    }
    hotkeyInput.setText(keyStr);
  }
}
