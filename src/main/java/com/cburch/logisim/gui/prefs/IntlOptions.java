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
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class IntlOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final JLabel localeLabel = new RestrictedLabel();
  private final JComponent locale;
  private final PrefOptionList gateShape;

  public IntlOptions(PreferencesFrame window) {
    super(window);

    locale = S.createLocaleSelector();
    gateShape =
        new PrefOptionList(
            AppPreferences.GATE_SHAPE,
            S.getter("intlGateShape"),
            new PrefOption[] {
              new PrefOption(AppPreferences.SHAPE_SHAPED, S.getter("shapeShaped")),
              new PrefOption(AppPreferences.SHAPE_RECTANGULAR, S.getter("shapeRectangular"))
            });
    // new PrefOption(AppPreferences.SHAPE_DIN40700, S.getter("shapeDIN40700"))

    final var localePanel = new Box(BoxLayout.X_AXIS);
    localePanel.add(Box.createGlue());
    localePanel.add(localeLabel);
    localeLabel.setMaximumSize(localeLabel.getPreferredSize());
    localeLabel.setAlignmentY(Component.TOP_ALIGNMENT);
    localePanel.add(locale);
    locale.setAlignmentY(Component.TOP_ALIGNMENT);
    localePanel.add(Box.createGlue());

    final var shapePanel = new JPanel();
    shapePanel.add(gateShape.getJLabel());
    shapePanel.add(gateShape.getJComboBox());

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createGlue());
    add(shapePanel);
    add(localePanel);
    add(Box.createGlue());
  }

  @Override
  public String getHelpText() {
    return S.get("intlHelp");
  }

  @Override
  public String getTitle() {
    return S.get("intlTitle");
  }

  @Override
  public void localeChanged() {
    gateShape.localeChanged();
    localeLabel.setText(S.get("intlLocale") + " ");
  }

  private static class RestrictedLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }
}
