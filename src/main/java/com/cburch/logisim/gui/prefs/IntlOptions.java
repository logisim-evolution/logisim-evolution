/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class IntlOptions extends OptionsPanel {
  private static class RestrictedLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }

  private static final long serialVersionUID = 1L;

  private JLabel localeLabel = new RestrictedLabel();
  private JComponent locale;
  private PrefBoolean replAccents;
  private PrefOptionList gateShape;

  public IntlOptions(PreferencesFrame window) {
    super(window);

    locale = S.createLocaleSelector();
    replAccents = new PrefBoolean(AppPreferences.ACCENTS_REPLACE, S.getter("intlReplaceAccents"));
    gateShape =
        new PrefOptionList(
            AppPreferences.GATE_SHAPE,
            S.getter("intlGateShape"),
            new PrefOption[] {
              new PrefOption(AppPreferences.SHAPE_SHAPED, S.getter("shapeShaped")),
              new PrefOption(AppPreferences.SHAPE_RECTANGULAR, S.getter("shapeRectangular"))
            });
    //						new PrefOption(AppPreferences.SHAPE_DIN40700,
    //								S.getter("shapeDIN40700"))

    Box localePanel = new Box(BoxLayout.X_AXIS);
    localePanel.add(Box.createGlue());
    localePanel.add(localeLabel);
    localeLabel.setMaximumSize(localeLabel.getPreferredSize());
    localeLabel.setAlignmentY(Component.TOP_ALIGNMENT);
    localePanel.add(locale);
    locale.setAlignmentY(Component.TOP_ALIGNMENT);
    localePanel.add(Box.createGlue());

    JPanel shapePanel = new JPanel();
    shapePanel.add(gateShape.getJLabel());
    shapePanel.add(gateShape.getJComboBox());

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createGlue());
    add(shapePanel);
    add(localePanel);
    add(replAccents);
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
    replAccents.localeChanged();
    replAccents.setEnabled(LocaleManager.canReplaceAccents());
  }
}
