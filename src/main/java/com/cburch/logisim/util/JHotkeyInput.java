package com.cburch.logisim.util;

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.concurrent.Flow;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class JHotkeyInput extends JPanel {
  private JButton resetButton = new JButton("❌");
  private JTextField hotkeyInputField;
  public JHotkeyInput(String text) {
    super(new FlowLayout(FlowLayout.CENTER,0,0));
    hotkeyInputField=new JTextField(text);
    hotkeyInputField.setHorizontalAlignment(SwingConstants.CENTER);

    setBackground(hotkeyInputField.getBackground());
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(10,10,10,10),
        hotkeyInputField.getBorder()
    ));
    resetButton.setBorder(BorderFactory.createEmptyBorder());
    hotkeyInputField.setBorder(BorderFactory.createEmptyBorder());
//    setBackground(hotkeyInputField.getBackground());
//    setBorder(BorderFactory.createEmptyBorder());
    add(hotkeyInputField);
    add(resetButton);
  }

  public void setText(String s){
    hotkeyInputField.setText(s);
  }
}
