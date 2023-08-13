package com.cburch.logisim.util;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
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
  private final JFrame topFrame;
  private final JButton resetButton = new JButton();
  private final JButton applyButton = new JButton();
  public final JTextField hotkeyInputField;
  private transient PrefMonitorKeyStroke boundKeyStroke = null;
  private final transient HotkeyInputKeyListener hotkeyListener;
  private static boolean layoutOptimized = false;
  private boolean needUpdate = false;
  private static boolean activeHotkeyInputUpdated = false;
  private static String activeHotkeyInputName = "";
  private String previousData = "";


  public JHotkeyInput(JFrame frame, String text) {
    topFrame = frame;
    hotkeyListener = new HotkeyInputKeyListener(this);
    hotkeyInputField = new JTextField(text.toUpperCase());

    setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    setBorder(BorderFactory.createCompoundBorder(
        hotkeyInputField.getBorder(),
        BorderFactory.createEmptyBorder(2, 4, 2, 4)
    ));
    setPreferredSize(new Dimension(120, 28));

    ((AbstractDocument) hotkeyInputField.getDocument())
        .setDocumentFilter(new KeyboardInputFilter());
    hotkeyInputField.setHorizontalAlignment(SwingConstants.CENTER);
    hotkeyInputField.setBackground(getBackground());
    hotkeyInputField.setBorder(BorderFactory.createEmptyBorder());
    hotkeyInputField.addKeyListener(hotkeyListener);
    hotkeyInputField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        enterEditMode();
      }

      @Override
      public void focusLost(FocusEvent e) {
        needUpdate = true;
      }
    });

    Icon iconOK = IconsUtil.getIcon("ok.gif");
    Icon iconCancel = IconsUtil.getIcon("cancel.gif");
    applyButton.setIcon(iconOK);
    resetButton.setIcon(iconCancel);
    applyButton.setBorder(BorderFactory.createEmptyBorder());
    applyButton.setVisible(false);
    resetButton.setBorder(BorderFactory.createEmptyBorder());
    resetButton.setVisible(false);
    resetButton.addActionListener(e -> exitEditMode());
    applyButton.addActionListener(e -> applyChanges());
    Font font = resetButton.getFont();
    applyButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
    applyButton.setPreferredSize(new Dimension(18, 18));
    resetButton.setFont(new Font(font.getFontName(), Font.PLAIN, 8));
    resetButton.setPreferredSize(new Dimension(18, 18));

    add(hotkeyInputField);
    add(applyButton);
    add(resetButton);
    new Timer(100, e -> {
      int height = getHeight();
      int width = getWidth();
      if (!layoutOptimized && width > 0) {
        setPreferredSize(new Dimension(width + 18 + 18, height));
        layoutOptimized = true;
        repaint();
        updateUI();
      }
      if (needUpdate && boundKeyStroke != null
          && activeHotkeyInputUpdated
          && !activeHotkeyInputName.equals(boundKeyStroke.getName())) {
        needUpdate = false;
        exitEditModeWithoutRefresh();
      }
    }).start();
  }

  private void enterEditMode() {
    /* TODO: disable all menu items */
    activeHotkeyInputName = boundKeyStroke.getName();
    activeHotkeyInputUpdated = true;
    if (hotkeyListener.rewritable()) {
      previousData = hotkeyInputField.getText();
    }
    hotkeyListener.clearStatus();
    hotkeyInputField.setText("");
    resetButton.setVisible(true);
    applyButton.setVisible(true);
    int height = hotkeyInputField.getHeight();
    int width = getWidth() - 18 - 18 - 8 - 8;
    hotkeyInputField.setPreferredSize(new Dimension(width, height));
    applyButton.setEnabled(false);
  }

  private void exitEditModeWithoutRefresh() {
    activeHotkeyInputUpdated = false;
    applyButton.setVisible(false);
    resetButton.setVisible(false);
    hotkeyInputField.setText(previousData);
  }

  private void exitEditMode() {
    exitEditModeWithoutRefresh();
    int height = hotkeyInputField.getHeight();
    int width = hotkeyInputField.getPreferredSize().width + 18 + 18 + 8;
    hotkeyInputField.setPreferredSize(new Dimension(width, height));
    repaint();
    updateUI();
    topFrame.requestFocus();
  }

  private void applyChanges() {
    if (hotkeyListener.code != 0) {
      boundKeyStroke.set(KeyStroke.getKeyStroke(hotkeyListener.code, hotkeyListener.modifier));
      previousData = hotkeyListener.getHotkeyString();
      try {
        AppPreferences.getPrefs().flush();
        AppPreferences.hotkeySync();
      } catch (BackingStoreException ex) {
        throw new RuntimeException(ex);
      }
    }
    applyButton.setVisible(false);
    resetButton.setVisible(false);
    topFrame.requestFocus();
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

  public void setApplyEnabled(boolean enabled) {
    applyButton.setEnabled(enabled);
  }

  private class HotkeyInputKeyListener implements KeyListener {
    private final JHotkeyInput hotkeyInput;
    private int modifier = 0;
    private int code = 0;
    private boolean rewriteFlag = true;
    private String hotkeyString = "";

    public HotkeyInputKeyListener(JHotkeyInput hotkeyInput) {
      this.hotkeyInput = hotkeyInput;
    }

    public void clearStatus() {
      code = 0;
      modifier = 0;
      hotkeyString = "";
      rewriteFlag = false;
    }

    public boolean rewritable() {
      return rewriteFlag;
    }

    public String getHotkeyString() {
      return hotkeyString;
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
        clearStatus();
        return;
      }
      String modifierString = InputEvent.getModifiersExText(modifier);
      if (modifierString.isEmpty()) {
        hotkeyString = KeyEvent.getKeyText(code);
      } else {
        hotkeyString = InputEvent.getModifiersExText(modifier) + "+" + KeyEvent.getKeyText(code);
      }
      if (!(hotkeyInput.getBoundKeyStroke().metaCheckPass(modifier))) {
        JOptionPane.showMessageDialog(null, S.get("hotkeyErrMeta",
                InputEvent.getModifiersExText(AppPreferences.hotkeyMenuMask)),
            S.get("hotkeyOptTitle"),
            JOptionPane.ERROR_MESSAGE);
        clearStatus();
        return;
      }
      String checkPass = AppPreferences.hotkeyCheckConflict(code, modifier);
      if (!checkPass.isEmpty()) {
        JOptionPane.showMessageDialog(null,
            checkPass,
            S.get("hotkeyOptTitle"),
            JOptionPane.ERROR_MESSAGE);
        clearStatus();
        return;
      }
      hotkeyInput.setText("");
    }

    @Override
    public void keyReleased(KeyEvent e) {
      hotkeyInput.setText(hotkeyString);
      hotkeyInput.setApplyEnabled(!hotkeyString.isEmpty());
      if (!hotkeyString.isEmpty()) {
        rewriteFlag = true;
      }
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


