/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.util.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.prefs.BackingStoreException;

import static com.cburch.logisim.gui.Strings.S;

class HotkeyOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  /*============Read Me==============
   * This is the hotkey settings Tab in the preferences.
   * Allowing users to decide which hotkey to bind to the specific function.
   * To implement this into your code
   * Firstly add your hotkey configurations to AppPreferences
   * Then add your AppPreferences.HOTKEY_ADD_BY_YOU to hotkeys array in HotkeyOptions.java
   * Setting up the hotkey in your code by accessing AppPreferences.HOTKEY_ADD_BY_YOU
   * Do not forget to sync with the user's settings. You should go modifing hotkeySync in AppPreferences, adding your codes there.
   * */
  @SuppressWarnings("unchecked")
  protected static final PrefMonitor<KeyStroke>[] hotkeys = new PrefMonitor[]{
      AppPreferences.HOTKEY_SIM_AUTO_PROPAGATE,
      AppPreferences.HOTKEY_SIM_RESET,
      AppPreferences.HOTKEY_SIM_STEP,
      AppPreferences.HOTKEY_SIM_TICK_HALF,
      AppPreferences.HOTKEY_SIM_TICK_FULL,
      AppPreferences.HOTKEY_SIM_TICK_ENABLED,
      AppPreferences.HOTKEY_EDIT_UNDO,
      AppPreferences.HOTKEY_EDIT_REDO,
      AppPreferences.HOTKEY_FILE_EXPORT,
      AppPreferences.HOTKEY_FILE_PRINT,
      AppPreferences.HOTKEY_FILE_QUIT,
  };
  private final JButton[] keyButtons = new JButton[hotkeys.length];
  private final JLabel headerLabel;

  public HotkeyOptions(PreferencesFrame window) {
    super(window);
    this.setLayout(new TableLayout(1));
    final var listener = new SettingsChangeListener(this);
    headerLabel = new JLabel();
    add(headerLabel);
    add(new JLabel(" "));

    JPanel p = new JPanel();
    p.setLayout(new TableLayout(2));
    final JLabel[] keyLabels = new JLabel[hotkeys.length];
    for (int i = 0; i < hotkeys.length; i++) {
      keyLabels[i] = new JLabel(S.get(((PrefMonitorKeyStroke) hotkeys[i]).getName()) + "  ");
      keyButtons[i] = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getString());
      keyButtons[i].addActionListener(listener);
      keyButtons[i].setActionCommand(i + "");
      p.add(keyLabels[i]);
      p.add(keyButtons[i]);
    }
    add(p);

    JButton resetBtn = new JButton(S.get("hotkeyOptResetBtn"));
    resetBtn.addActionListener(e -> {
      AppPreferences.resetHotkeys();
      try {
        AppPreferences.getPrefs().flush();
        for (int i = 0; i < hotkeys.length; i++) {
          keyButtons[i].setText(((PrefMonitorKeyStroke) hotkeys[i]).getString());
        }
        AppPreferences.hotkeySync();
      } catch (BackingStoreException ex) {
        throw new RuntimeException(ex);
      }
    });
    add(new JLabel(" "));
    add(resetBtn);
    AppPreferences.addPropertyChangeListener(evt -> AppPreferences.hotkeySync());
  }

  @Override
  public String getHelpText() {
    return S.get("hotkeyOptHelp");
  }

  @Override
  public String getTitle() {
    return S.get("hotkeyOptTitle");
  }

  @Override
  public void localeChanged() {
    /* TODO: localize */
    headerLabel.setText(S.get("hotkeyOptHeader"));
  }

  private class SettingsChangeListener implements ChangeListener, ActionListener {
    HotkeyOptions owner;
    private int code;
    private int modifier;

    public SettingsChangeListener(HotkeyOptions ht) {
      owner = ht;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int index = Integer.parseInt(e.getActionCommand());
      JDialog dl = new JDialog(
          owner.getPreferencesFrame(),
          S.get(((PrefMonitorKeyStroke) hotkeys[index]).getName()),
          true);
      JPanel p = new JPanel();
      JPanel sub = new JPanel();
      JButton ok = new JButton("OK");
      JButton cancel = new JButton("Cancel");

      ok.setFocusable(false);
      ok.addActionListener(e1 -> {
        if (code != 0) {
          HotkeyOptions.hotkeys[index].set(KeyStroke.getKeyStroke(code, modifier));
          try {
            AppPreferences.getPrefs().flush();
            owner.keyButtons[index].setText(((PrefMonitorKeyStroke) HotkeyOptions.hotkeys[index]).getString());
            AppPreferences.hotkeySync();
          } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
          }
        }
        dl.setVisible(false);
      });
      cancel.setFocusable(false);
      cancel.addActionListener(ev -> dl.setVisible(false));

      sub.setLayout(new TableLayout(2));
      p.setLayout(new TableLayout(1));

      sub.add(ok);
      sub.add(cancel);

      JLabel waitingLabel = new JLabel("Receiving Your Input Key");
      p.add(waitingLabel);
      p.add(sub);

      dl.addKeyListener(new KeyCaptureListener(waitingLabel, ((PrefMonitorKeyStroke) hotkeys[index]).isMenuHotkey(), this));
      dl.setContentPane(p);
      dl.setLocationRelativeTo(null);
      dl.setSize(400, 200);
      dl.setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      /* not-used */
    }
  }

  private class KeyCaptureListener implements KeyListener {
    private final JLabel label;
    private final boolean isMenuKey;
    private final SettingsChangeListener scl;

    public KeyCaptureListener(JLabel l, boolean isMenuKey, SettingsChangeListener se) {
      label = l;
      this.isMenuKey = isMenuKey;
      scl = se;
    }

    @Override
    public void keyTyped(KeyEvent e) {
      /* not-used */
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() >= 32) {
        int modifier = e.getModifiersEx();
        int code = e.getKeyCode();
        /* TODO: be compatible with other scenes */
        if (isMenuKey && (modifier & InputEvent.CTRL_DOWN_MASK) != InputEvent.CTRL_DOWN_MASK) {
          label.setText(S.get("hotkeyErrCtrl"));
          scl.code = 0;
          scl.modifier = 0;
          return;
        }

        for (var item : hotkeys) {
          if ((InputEvent.getModifiersExText(modifier) + " + " + KeyEvent.getKeyText(code)).equals(((PrefMonitorKeyStroke) item).getString())) {
            label.setText(S.get("hotkeyErrConflict") + S.get(((PrefMonitorKeyStroke) item).getName()));
            scl.code = 0;
            scl.modifier = 0;
            return;
          }
        }
        scl.code = code;
        scl.modifier = modifier;
        label.setText(InputEvent.getModifiersExText(modifier) + " + " + KeyEvent.getKeyText(code));
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      /* not-used */
    }
  }
}
