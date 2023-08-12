package com.cburch.logisim.util;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class JHotkeyInput extends JTextField {
  private JButton cancelBtn=new JButton("u274c");
  public JHotkeyInput(String text) {
    super(text);
    setHorizontalAlignment(SwingConstants.CENTER);
    add(cancelBtn);
  }
}
