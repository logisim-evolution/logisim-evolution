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


  public HotkeyOptions(PreferencesFrame window) {
    super(window);

    final var listener = new SettingsChangeListener();
    final var panel = new JPanel(new TableLayout(2));



    final var gridColorsResetButton = new JButton();
    gridColorsResetButton.addActionListener(listener);
    gridColorsResetButton.setText(S.get("windowGridColorsReset"));
    panel.add(new JLabel());
    panel.add(gridColorsResetButton);

    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));



    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));


    setLayout(new TableLayout(1));
    final var but = new JButton();
    but.addActionListener(listener);
    but.setText(S.get("windowToolbarReset"));
    add(but);
    add(panel);
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
