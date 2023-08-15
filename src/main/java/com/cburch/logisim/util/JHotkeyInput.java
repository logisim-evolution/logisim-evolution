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
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class JHotkeyInput extends JPanel {
  private static JFrame topFrame = null;
  private final JButton resetButton = new JButton();
  private final JButton applyButton = new JButton();
  public final JTextField hotkeyInputField;
  private transient PrefMonitorKeyStroke boundKeyStroke = null;
  private final transient HotkeyInputKeyListener hotkeyListener;
  private boolean focusableEnabled = false;
  private static int layoutOptimizedDelay = 4;
  private static boolean globalLayoutOptimized = false;
  private boolean needUpdate = false;
  private static boolean activeHotkeyInputUpdated = false;
  private static String activeHotkeyInputName = "";
  private String previousData = "";
  private static final List<JHotkeyInput> JHotkeyInputList = new ArrayList<>();
  private static final Timer optimizeTimer = new Timer(100, e -> {
    if (topFrame == null) {
      return;
    }
    for (var com : JHotkeyInputList) {
      int height = com.getHeight();
      int width = com.getWidth();
      if (!globalLayoutOptimized && width > 0 && layoutOptimizedDelay-- > 0) {
        /* run only once */
        com.setPreferredSize(new Dimension(width + 18 + 18, height));
        globalLayoutOptimized = true;
        com.repaint();
        com.updateUI();
      }
      if (!com.focusableEnabled && globalLayoutOptimized) {
        /* run on every component's load */
        com.exitEditModeWithoutRefresh();
        com.repaint();
        com.updateUI();
        topFrame.requestFocus();
        com.hotkeyInputField.setFocusable(true);
        com.focusableEnabled = true;
      }
      if (com.needUpdate && com.boundKeyStroke != null
          && activeHotkeyInputUpdated
          && !activeHotkeyInputName.equals(com.boundKeyStroke.getName())) {
        com.needUpdate = false;
        com.exitEditModeWithoutRefresh();
      }
    }
  });


  public JHotkeyInput(JFrame frame, String text) {
    if (topFrame == null) {
      topFrame = frame;
    }
    hotkeyListener = new HotkeyInputKeyListener(this);
    hotkeyInputField = new JTextField(text.toUpperCase());
    previousData = text;

    setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    setBorder(BorderFactory.createCompoundBorder(
        hotkeyInputField.getBorder(),
        BorderFactory.createEmptyBorder(2, 4, 2, 4)
    ));
    setPreferredSize(new Dimension(140, 28));

    ((AbstractDocument) hotkeyInputField.getDocument())
        .setDocumentFilter(new KeyboardInputFilter());
    hotkeyInputField.setHorizontalAlignment(SwingConstants.CENTER);
    /* Sometimes the look and feel will override our settings
     *  Then it will look strange
     *  Especially for the theme Nimbus
     *  So we have to do the belows to make Nimbus happy
     *  */
    if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
      hotkeyInputField.setBackground(new Color(0, 0, 0, 0));
    } else {
      hotkeyInputField.setBackground(topFrame.getBackground());
    }
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

    hotkeyInputField.setFocusable(false);
    add(hotkeyInputField);
    add(applyButton);
    add(resetButton);

    JHotkeyInputList.add(this);

    if (!optimizeTimer.isRunning()) {
      optimizeTimer.start();
    }
  }

  private void enterEditMode() {
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
    int width = getWidth() - 18 - 18 - 8 - 8 - 6;
    hotkeyInputField.setPreferredSize(new Dimension(width, height));
    applyButton.setEnabled(false);
  }

  public void exitEditModeWithoutRefresh() {
    activeHotkeyInputUpdated = false;
    applyButton.setVisible(false);
    resetButton.setVisible(false);
    hotkeyInputField.setText(previousData);
  }

  public void exitEditMode() {
    exitEditModeWithoutRefresh();
    int height = hotkeyInputField.getHeight();
    //int width = hotkeyInputField.getPreferredSize().width + 18 + 18 + 8;
    int width = getWidth() - 12;
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
    int height = hotkeyInputField.getHeight();
    //int width = hotkeyInputField.getPreferredSize().width + 18 + 18 + 8;
    int width = getWidth() - 12;
    hotkeyInputField.setPreferredSize(new Dimension(width, height));
    repaint();
    updateUI();
    topFrame.requestFocus();
  }

  public void setText(String s) {
    hotkeyInputField.setText(s.toUpperCase());
  }

  public void resetText(String s) {
    hotkeyInputField.setText(s.toUpperCase());
    previousData = s.toUpperCase();
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
      String checkPass = AppPreferences.hotkeyCheckConflict(hotkeyInput.boundKeyStroke.getName(),
          code, modifier);
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


