package com.cburch.logisim.util;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.prefs.BackingStoreException;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class JHotkeyInput extends JPanel {
  private final JButton resetButton = new JButton();
  private final JButton applyButton = new JButton();
  public final JTextField hotkeyInputField;
  private String previousData = "";
  private transient PrefMonitorKeyStroke boundKeyStroke = null;
  private static boolean layoutOptimized = false;

  public JHotkeyInput(JFrame frame, String text) {
    Icon iconOK = IconsUtil.getIcon("ok.gif");
    Icon iconCancel = IconsUtil.getIcon("cancel.gif");
    applyButton.setIcon(iconOK);
    resetButton.setIcon(iconCancel);
    setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    hotkeyInputField = new JTextField(text.toUpperCase());
    setBorder(BorderFactory.createCompoundBorder(
        hotkeyInputField.getBorder(),
        BorderFactory.createEmptyBorder(2,4,2,4)
    ));
    ((AbstractDocument) hotkeyInputField.getDocument())
        .setDocumentFilter(new KeyboardInputFilter());
    hotkeyInputField.setBackground(Color.yellow);
    hotkeyInputField.setHorizontalAlignment(SwingConstants.CENTER);
//    hotkeyInputField.setBackground(getBackground());
    hotkeyInputField.setBorder(BorderFactory.createEmptyBorder());
    var hotkeyListener = new HotkeyInputKeyListener(this);
    var that=this;
    hotkeyInputField.addKeyListener(hotkeyListener);
    hotkeyInputField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        /* TODO: disable all menu items */
        previousData = hotkeyInputField.getText();
        hotkeyInputField.setText("");
        resetButton.setVisible(true);
        applyButton.setVisible(true);
        int height=hotkeyInputField.getHeight();
        int width=that.getWidth()-18-18-8-8;
        hotkeyInputField.setPreferredSize(new Dimension(width,height));
        applyButton.setEnabled(false);
      }

      @Override
      public void focusLost(FocusEvent e) {
        /* not-used */
      }
    });
    applyButton.setBorder(BorderFactory.createEmptyBorder());
    applyButton.setVisible(false);
    resetButton.setBorder(BorderFactory.createEmptyBorder());
    resetButton.setVisible(false);
    resetButton.addActionListener(e -> {
      System.out.println("114");
      applyButton.setVisible(false);
      resetButton.setVisible(false);
      frame.requestFocus();
      hotkeyInputField.setText(previousData);
      applyButton.setVisible(false);
      resetButton.setVisible(false);
      int height=hotkeyInputField.getHeight();
      int width=hotkeyInputField.getPreferredSize().width+18+18+8;
      hotkeyInputField.setPreferredSize(new Dimension(width,height));
      repaint();
      updateUI();
    });
    applyButton.addActionListener(e -> {
      if (hotkeyListener.code != 0) {
        boundKeyStroke.set(KeyStroke.getKeyStroke(hotkeyListener.code, hotkeyListener.modifier));
        previousData= hotkeyListener.keyStr;
        try {
          AppPreferences.getPrefs().flush();
          AppPreferences.hotkeySync();
        } catch (BackingStoreException ex) {
          throw new RuntimeException(ex);
        }
      }
      applyButton.setVisible(false);
      resetButton.setVisible(false);
      frame.requestFocus();
    });
    Font font = resetButton.getFont();
    applyButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
    applyButton.setPreferredSize(new Dimension(18, 18));
    resetButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
    resetButton.setPreferredSize(new Dimension(18, 18));
    setPreferredSize(new Dimension(120, 28));
    add(hotkeyInputField);
    add(applyButton);
    add(resetButton);
    new Timer(200,e->{
      int height=getHeight();
      int width=getWidth();
      if(!layoutOptimized&&width>0){
        setPreferredSize(new Dimension(width+18+18,height));
        layoutOptimized=true;
        repaint();
        updateUI();
      }
    }).start();
  }

  public void setText(String s) {
    hotkeyInputField.setText(s.toUpperCase());
  }

  public void setBoundKeyStroke(PrefMonitorKeyStroke keyStroke) {
    boundKeyStroke = keyStroke;
  }

  public PrefMonitorKeyStroke getBoundKeyStroke() {
    return boundKeyStroke;
  }

  @Override
  public void setEnabled(boolean enabled) {
    hotkeyInputField.setEnabled(enabled);
  }

  public void setApplyEnabled(boolean enabled){
    applyButton.setEnabled(enabled);
  }

  private class HotkeyInputKeyListener implements KeyListener {
    private final JHotkeyInput hotkeyInput;
    private int modifier = 0;
    private int code = 0;
    public String keyStr = "";

    public HotkeyInputKeyListener(JHotkeyInput hotkeyInput) {
      this.hotkeyInput = hotkeyInput;
    }

    @Override
    public void keyTyped(KeyEvent e) {
      /* not-used */
    }

    @Override
    public void keyPressed(KeyEvent e) {
      modifier = e.getModifiersEx();
      code = e.getKeyCode();
      if (code == 0
          || code == KeyEvent.VK_CONTROL
          || code == KeyEvent.VK_ALT
          || code == KeyEvent.VK_SHIFT
          || code == KeyEvent.VK_META) {
        code = 0;
        modifier = 0;
        return;
      }
      String modifierString = InputEvent.getModifiersExText(modifier);
      if (modifierString.isEmpty()) {
        keyStr = KeyEvent.getKeyText(code);
      } else {
        keyStr = InputEvent.getModifiersExText(modifier) + "+" + KeyEvent.getKeyText(code);
      }
      if (!(hotkeyInput.getBoundKeyStroke().metaCheckPass(modifier))) {
        JOptionPane.showMessageDialog(null, S.get("hotkeyErrMeta",
                InputEvent.getModifiersExText(AppPreferences.hotkeyMenuMask)),
            S.get("hotkeyOptTitle"),
            JOptionPane.ERROR_MESSAGE);
        code = 0;
        modifier = 0;
        return;
      }
      String checkPass = AppPreferences.hotkeyCheckConflict(code, modifier);
      if (!checkPass.isEmpty()) {
        JOptionPane.showMessageDialog(null,
            checkPass,
            S.get("hotkeyOptTitle"),
            JOptionPane.ERROR_MESSAGE);
        code = 0;
        modifier = 0;
        return;
      }
      hotkeyInput.setText("");
    }

    @Override
    public void keyReleased(KeyEvent e) {
      hotkeyInput.setText(keyStr);
      hotkeyInput.setApplyEnabled(!keyStr.isEmpty());
    }
  }

  private class KeyboardInputFilter extends DocumentFilter {
    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
                             String text, AttributeSet attr) throws BadLocationException {

      fb.insertString(offset, text.toUpperCase(), attr);
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
                        String text, AttributeSet attrs) throws BadLocationException {

      fb.replace(offset, length, text.toUpperCase(), attrs);
    }
  }
}


