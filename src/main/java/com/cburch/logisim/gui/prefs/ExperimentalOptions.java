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
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ExperimentalOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final JLabel accelRestart = new JLabel();
  private final PrefOptionList accel;

  public ExperimentalOptions(PreferencesFrame window) {
    super(window);

    accel =
        new PrefOptionList(
            AppPreferences.GRAPHICS_ACCELERATION,
            S.getter("accelLabel"),
            new PrefOption[] {
              new PrefOption(AppPreferences.ACCEL_DEFAULT, S.getter("accelDefault")),
              new PrefOption(AppPreferences.ACCEL_NONE, S.getter("accelNone")),
              new PrefOption(AppPreferences.ACCEL_OPENGL, S.getter("accelOpenGL")),
              new PrefOption(AppPreferences.ACCEL_D3D, S.getter("accelD3D")),
            });

    final var accelPanel = new JPanel(new BorderLayout());
    accelPanel.add(accel.getJLabel(), BorderLayout.LINE_START);
    accelPanel.add(accel.getJComboBox(), BorderLayout.CENTER);
    accelPanel.add(accelRestart, BorderLayout.PAGE_END);
    accelRestart.setFont(accelRestart.getFont().deriveFont(Font.ITALIC));
    final var accelPanel2 = new JPanel();
    accelPanel2.add(accelPanel);

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createGlue());
    add(accelPanel2);
    add(Box.createGlue());
  }

  @Override
  public String getHelpText() {
    return S.get("experimentHelp");
  }

  @Override
  public String getTitle() {
    return S.get("experimentTitle");
  }

  @Override
  public void localeChanged() {
    accel.localeChanged();
    accelRestart.setText(S.get("accelRestartLabel"));
  }
}
