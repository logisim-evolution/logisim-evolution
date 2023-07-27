/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.gui.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.TableLayout;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.cburch.logisim.gui.Strings.S;

class HotkeyOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private static final PrefMonitor<KeyStroke>[] hotkeys=new PrefMonitor[]{
          AppPreferences.HOTKEY_SIM_AUTO_PROPAGATE,
          AppPreferences.HOTKEY_SIM_RESET,
          AppPreferences.HOTKEY_SIM_STEP,
          AppPreferences.HOTKEY_SIM_TICK_HALF,
          AppPreferences.HOTKEY_SIM_TICK_FULL,
          AppPreferences.HOTKEY_SIM_TICK_ENABLED,
          AppPreferences.HOTKEY_EDIT_UNDO,
          AppPreferences.HOTKEY_EDIT_REDO,
          AppPreferences.HOTKEY_EDIT_EXPORT,
          AppPreferences.HOTKEY_EDIT_PRINT,
          AppPreferences.HOTKEY_EDIT_QUIT,
  };

  private JLabel[] key_labels=new JLabel[hotkeys.length];
  private JButton[] key_buttons=new JButton[hotkeys.length];

  public HotkeyOptions(PreferencesFrame window) {
    super(window);
    this.setLayout(new TableLayout(1));
    final var listener = new SettingsChangeListener();
    for(int i=0;i<hotkeys.length;i++){
      final var panel = new JPanel(new TableLayout(2));
      key_labels[i] = new JLabel(((PrefMonitorKeyStroke)hotkeys[i]).getName());
      key_buttons[i]=new JButton(hotkeys[i].get().toString());
      key_buttons[i].addActionListener(listener);
      panel.add(key_labels[i]);
      panel.add(key_buttons[i]);
      add(panel);
    }

  }

  @Override
  public String getHelpText() {
    /* TODO: localize */
    return "This is the help of hotkey settings. Remember to localize it.";
  }

  @Override
  public String getTitle() {
    /* TODO: localize */
    return "Hotkey Settings";
  }

  @Override
  public void localeChanged() {
    /* TODO: localize */
  }

  private class SettingsChangeListener implements ChangeListener, ActionListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      /* TODO: Update Settings */
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//      if (e.getActionCommand().equals(cmdResetWindowLayout)) {
//        AppPreferences.resetWindow();
//        final var nowOpen = Projects.getOpenProjects();
//        for (final var proj : nowOpen) {
//          proj.getFrame().resetLayout();
//          proj.getFrame().revalidate();
//          proj.getFrame().repaint();
//        }
//      } else if (e.getActionCommand().equals(cmdResetGridColors)) {
//        //        AppPreferences.resetWindow();
//        final var nowOpen = Projects.getOpenProjects();
//        AppPreferences.setDefaultGridColors();
//        for (final var proj : nowOpen) {
//          proj.getFrame().repaint();
//        }
//      } else if (e.getActionCommand().equals(cmdSetAutoScaleFactor)) {
//        final var tmp = AppPreferences.getAutoScaleFactor();
//        AppPreferences.SCALE_FACTOR.set(tmp);
//        AppPreferences.getPrefs().remove(AppPreferences.SCALE_FACTOR.getIdentifier());
//        zoomValue.setValue((int) (tmp * 100));
//      }
    }
  }
}
