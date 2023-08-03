/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.util.TableLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class HotkeyOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  /*
   * Hotkey Options TAB
   *
   * Author: Hanyuan Zhao <2524395907@qq.com>
   *
   * Description:
   * This is the hotkey settings Tab in the preferences.
   * Allowing users to decide which hotkey to bind to the specific function.
   *
   * To implement this into your code
   * Firstly add your hotkey configurations to AppPreferences and set up their strings in resources
   * Fill the resetHotkeys in AppPreferences with your own code
   * Then add your AppPreferences.HOTKEY_ADD_BY_YOU to hotkeys array in HotkeyOptions.java
   * Setting up the hotkey in your code by accessing AppPreferences.HOTKEY_ADD_BY_YOU
   * Do not forget to sync with the user's settings.
   * You should go modifying hotkeySync in AppPreferences, adding your codes there.
   *
   * Now the hotkey options don't involve all the bindings in logisim.
   * The hotkeys chosen by the user might have conflict with
   * some build-in key bindings until all key bindings can be set in this tab.
   * TODO: If you are available, you can bind them in order to make logisim feel better
   *
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
      AppPreferences.HOTKEY_PROJ_MOVE_UP,
      AppPreferences.HOTKEY_PROJ_MOVE_DOWN,
      AppPreferences.HOTKEY_DIR_NORTH,
      AppPreferences.HOTKEY_DIR_SOUTH,
      AppPreferences.HOTKEY_DIR_EAST,
      AppPreferences.HOTKEY_DIR_WEST,
      AppPreferences.HOTKEY_EDIT_TOOL_DUPLICATE,
      AppPreferences.HOTKEY_AUTO_LABEL_OPEN,
      AppPreferences.HOTKEY_AUTO_LABEL_TOGGLE,
      AppPreferences.HOTKEY_AUTO_LABEL_VIEW,
      AppPreferences.HOTKEY_AUTO_LABEL_HIDE,
      AppPreferences.HOTKEY_AUTO_LABEL_SELF_NUMBERED_STOP,
      AppPreferences.HOTKEY_ADD_TOOL_ROTATE,
      AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_SMALL,
      AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_MEDIUM,
      AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_WIDE,
      AppPreferences.HOTKEY_GATE_MODIFIER_INPUT_ADD,
      AppPreferences.HOTKEY_GATE_MODIFIER_INPUT_SUB,
  };
  private final JButton[] keyButtons = new JButton[hotkeys.length];
  private final JLabel headerLabel;
  private JButton northBtn;
  private JButton southBtn;
  private JButton eastBtn;
  private JButton westBtn;
  private int preferredWidth = 800;
  private boolean preferredWidthSet = false;

  public HotkeyOptions(PreferencesFrame window) {
    super(window);
    this.setLayout(new TableLayout(1));
    final var listener = new SettingsChangeListener(this);
    headerLabel = new JLabel();
    add(headerLabel);
    add(new JLabel(" "));

    JPanel p = new JPanel();
    p.setMaximumSize(new Dimension(400, 400));
    p.setLayout(new TableLayout(2));
    final JLabel[] keyLabels = new JLabel[hotkeys.length];
    for (int i = 0; i < hotkeys.length; i++) {
      /* I do this chore because they have a different layout */
      if (hotkeys[i] == AppPreferences.HOTKEY_DIR_NORTH
          || hotkeys[i] == AppPreferences.HOTKEY_DIR_SOUTH
          || hotkeys[i] == AppPreferences.HOTKEY_DIR_EAST
          || hotkeys[i] == AppPreferences.HOTKEY_DIR_WEST) {
        if (hotkeys[i] == AppPreferences.HOTKEY_DIR_NORTH) {
          northBtn = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
          keyButtons[i] = northBtn;
        }
        if (hotkeys[i] == AppPreferences.HOTKEY_DIR_SOUTH) {
          southBtn = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
          keyButtons[i] = southBtn;
        }
        if (hotkeys[i] == AppPreferences.HOTKEY_DIR_EAST) {
          eastBtn = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
          keyButtons[i] = eastBtn;
        }
        if (hotkeys[i] == AppPreferences.HOTKEY_DIR_WEST) {
          westBtn = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
          keyButtons[i] = westBtn;
        }
        keyButtons[i].addActionListener(listener);
        keyButtons[i].setActionCommand(i + "");
        keyButtons[i].setEnabled(((PrefMonitorKeyStroke) hotkeys[i]).canModify());
        continue;
      }
      keyLabels[i] = new JLabel(S.get(((PrefMonitorKeyStroke) hotkeys[i]).getName()) + "  ");
      keyButtons[i] = new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
      keyButtons[i].addActionListener(listener);
      keyButtons[i].setActionCommand(i + "");
      keyButtons[i].setEnabled(((PrefMonitorKeyStroke) hotkeys[i]).canModify());
      p.add(keyLabels[i]);
      p.add(keyButtons[i]);
    }

    /* Layout for arrow hotkeys */
    p.add(new JLabel(" "));
    p.add(new JLabel(" "));
    p.add(new JLabel(S.get("hotkeyOptOrientDesc")));
    p.add(new JLabel(" "));
    JPanel dirPLeft = new JPanel();
    JPanel dirPRight = new JPanel();
    dirPLeft.setLayout(new TableLayout(3));
    dirPRight.setLayout(new TableLayout(3));
    dirPLeft.add(new JLabel(" "));
    dirPLeft.add(new JLabel(" " + S.get("hotkeyDirNorth") + " "));
    dirPLeft.add(new JLabel(" "));
    dirPLeft.add(new JLabel(" " + S.get("hotkeyDirWest") + " "));
    dirPLeft.add(new JLabel(" " + S.get("hotkeyDirSouth") + " "));
    dirPLeft.add(new JLabel(" " + S.get("hotkeyDirEast") + " "));
    p.add(dirPLeft);
    dirPRight.add(new JLabel(" "));
    dirPRight.add(northBtn);
    dirPRight.add(new JLabel(" "));
    dirPRight.add(westBtn);
    dirPRight.add(southBtn);
    dirPRight.add(eastBtn);
    p.add(dirPRight);
    JScrollPane scrollPane = new JScrollPane(p, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(preferredWidth, 500);
      }
    };
    add(scrollPane);
    var that = this;
    /* this timer deals with the preferred width */
    new Timer(200, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int width = p.getWidth();
        if (width > 0 && width < that.getWidth() * 0.8 && !preferredWidthSet) {
          preferredWidth = width + 40;
          p.setPreferredSize(p.getSize());
          preferredWidthSet = true;
        }
      }
    }).start();

    JButton resetBtn = new JButton(S.get("hotkeyOptResetBtn"));
    resetBtn.addActionListener(e -> {
      AppPreferences.resetHotkeys();
    });
    add(new JLabel(" "));
    add(resetBtn);
    AppPreferences.getPrefs().addPreferenceChangeListener(new PreferenceChangeListener() {
      @Override
      public void preferenceChange(PreferenceChangeEvent evt) {
        AppPreferences.hotkeySync();
        for (int i = 0; i < hotkeys.length; i++) {
          keyButtons[i].setText(((PrefMonitorKeyStroke) hotkeys[i]).getDisplayString());
        }
      }
    });
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

      JButton ok = new JButton("OK");
      JButton cancel = new JButton("Cancel");

      ok.setFocusable(false);
      ok.addActionListener(e1 -> {
        if (code != 0) {
          HotkeyOptions.hotkeys[index].set(KeyStroke.getKeyStroke(code, modifier));
          try {
            AppPreferences.getPrefs().flush();
            owner.keyButtons[index].setText(
                ((PrefMonitorKeyStroke) HotkeyOptions.hotkeys[index]).getDisplayString());
            AppPreferences.hotkeySync();
          } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
          }
        }
        dl.setVisible(false);
      });
      cancel.setFocusable(false);
      cancel.addActionListener(ev -> dl.setVisible(false));

      JPanel sub = new JPanel();
      sub.setLayout(new TableLayout(2));
      JPanel contentPanel = new JPanel();
      contentPanel.setLayout(new TableLayout(1));

      sub.add(ok);
      sub.add(cancel);

      JPanel top = new JPanel();
      top.setLayout(new TableLayout(3));
      JLabel waitingLabel = new JLabel("Receiving Your Input Key");
      top.add(new JLabel("  "));
      top.add(waitingLabel);
      top.add(new JLabel("  "));
      contentPanel.add(top);
      contentPanel.add(sub);

      dl.addKeyListener(new KeyCaptureListener(waitingLabel, this));
      dl.setContentPane(contentPanel);
      dl.setLocationRelativeTo(null);
      dl.setSize(500, 100);
      dl.setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      /* not-used */
    }
  }

  private class KeyCaptureListener implements KeyListener {
    private final JLabel label;
    private final SettingsChangeListener scl;

    public KeyCaptureListener(JLabel l, SettingsChangeListener se) {
      label = l;
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
        for (var item : hotkeys) {
          if ((InputEvent.getModifiersExText(modifier) + " + "
              + KeyEvent.getKeyText(code)).equals(
              ((PrefMonitorKeyStroke) item).getCompareString())) {
            label.setText(S.get("hotkeyErrConflict")
                + S.get(((PrefMonitorKeyStroke) item).getName()));
            scl.code = 0;
            scl.modifier = 0;
            return;
          }
        }
        scl.code = code;
        scl.modifier = modifier;
        String modifierString = InputEvent.getModifiersExText(modifier);
        if (modifierString.equals("")) {
          label.setText(KeyEvent.getKeyText(code));
        } else {
          label.setText(InputEvent.getModifiersExText(modifier) + "+" + KeyEvent.getKeyText(code));
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      /* not-used */
    }
  }
}
