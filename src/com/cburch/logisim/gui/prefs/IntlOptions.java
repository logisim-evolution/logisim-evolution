/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.gui.prefs;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleManager;

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

		locale = Strings.createLocaleSelector();
		replAccents = new PrefBoolean(AppPreferences.ACCENTS_REPLACE,
				Strings.getter("intlReplaceAccents"));
		gateShape = new PrefOptionList(AppPreferences.GATE_SHAPE,
				Strings.getter("intlGateShape"), new PrefOption[] {
						new PrefOption(AppPreferences.SHAPE_SHAPED,
								Strings.getter("shapeShaped")),
						new PrefOption(AppPreferences.SHAPE_RECTANGULAR,
								Strings.getter("shapeRectangular")) });
//						new PrefOption(AppPreferences.SHAPE_DIN40700,
//								Strings.getter("shapeDIN40700")) 

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
		return Strings.get("intlHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("intlTitle");
	}

	@Override
	public void localeChanged() {
		gateShape.localeChanged();
		localeLabel.setText(Strings.get("intlLocale") + " ");
		replAccents.localeChanged();
		replAccents.setEnabled(LocaleManager.canReplaceAccents());
	}
}
