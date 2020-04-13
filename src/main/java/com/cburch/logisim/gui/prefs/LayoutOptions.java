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

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.TableLayout;
import javax.swing.JPanel;

class LayoutOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private PrefBoolean[] checks;
  private PrefOptionList afterAdd;
  private PrefOptionList DefaultAppear;
  private PrefOptionList radix1;
  private PrefOptionList radix2;

  public LayoutOptions(PreferencesFrame window) {
    super(window);

    checks =
        new PrefBoolean[] {
          new PrefBoolean(AppPreferences.AntiAliassing, S.getter("layoutAntiAliasing")),
          new PrefBoolean(AppPreferences.PRINTER_VIEW, S.getter("layoutPrinterView")),
          new PrefBoolean(AppPreferences.ATTRIBUTE_HALO, S.getter("layoutAttributeHalo")),
          new PrefBoolean(AppPreferences.ANIMATED_ICONS, S.getter("layoutAnimatedIcons")),
          new PrefBoolean(AppPreferences.COMPONENT_TIPS, S.getter("layoutShowTips")),
          new PrefBoolean(AppPreferences.MOVE_KEEP_CONNECT, S.getter("layoutMoveKeepConnect")),
          new PrefBoolean(AppPreferences.ADD_SHOW_GHOSTS, S.getter("layoutAddShowGhosts")),
          new PrefBoolean(AppPreferences.NAMED_CIRCUIT_BOXES_FIXED_SIZE,S.getter("layoutNamedCircuitBoxesFixedSize")),
          new PrefBoolean(AppPreferences.NEW_INPUT_OUTPUT_SHAPES, S.getter("layoutUseNewInputOutputSymbols")),
        };

    for (int i = 0; i < 2; i++) {
      RadixOption[] opts = RadixOption.OPTIONS;
      PrefOption[] items = new PrefOption[opts.length];
      for (int j = 0; j < RadixOption.OPTIONS.length; j++) {
        items[j] = new PrefOption(opts[j].getSaveString(), opts[j].getDisplayGetter());
      }
      if (i == 0) {
        radix1 =
            new PrefOptionList(AppPreferences.POKE_WIRE_RADIX1, S.getter("layoutRadix1"), items);
      } else {
        radix2 =
            new PrefOptionList(AppPreferences.POKE_WIRE_RADIX2, S.getter("layoutRadix2"), items);
      }
    }
    afterAdd =
        new PrefOptionList(
            AppPreferences.ADD_AFTER,
            S.getter("layoutAddAfter"),
            new PrefOption[] {
              new PrefOption(
                  AppPreferences.ADD_AFTER_UNCHANGED, S.getter("layoutAddAfterUnchanged")),
              new PrefOption(AppPreferences.ADD_AFTER_EDIT, S.getter("layoutAddAfterEdit"))
            });
    DefaultAppear =
        new PrefOptionList(
            AppPreferences.DefaultAppearance,
            S.getter("layoutDefaultApearance"),
            new PrefOption[] {
              new PrefOption(
                  StdAttr.APPEAR_CLASSIC.toString(), StdAttr.APPEAR_CLASSIC.getDisplayGetter()),
              new PrefOption(
                  StdAttr.APPEAR_FPGA.toString(), StdAttr.APPEAR_FPGA.getDisplayGetter()),
              new PrefOption(
                  StdAttr.APPEAR_EVOLUTION.toString(), StdAttr.APPEAR_EVOLUTION.getDisplayGetter())
            });

    JPanel panel = new JPanel(new TableLayout(2));
    panel.add(DefaultAppear.getJLabel());
    panel.add(DefaultAppear.getJComboBox());
    panel.add(afterAdd.getJLabel());
    panel.add(afterAdd.getJComboBox());
    panel.add(radix1.getJLabel());
    panel.add(radix1.getJComboBox());
    panel.add(radix2.getJLabel());
    panel.add(radix2.getJComboBox());

    setLayout(new TableLayout(1));
    for (int i = 0; i < checks.length; i++) {
      add(checks[i]);
    }
    add(panel);
  }

  @Override
  public String getHelpText() {
    return S.get("layoutHelp");
  }

  @Override
  public String getTitle() {
    return S.get("layoutTitle");
  }

  @Override
  public void localeChanged() {
    for (int i = 0; i < checks.length; i++) {
      checks[i].localeChanged();
    }
    radix1.localeChanged();
    radix2.localeChanged();
    afterAdd.localeChanged();
    DefaultAppear.localeChanged();
  }
}
