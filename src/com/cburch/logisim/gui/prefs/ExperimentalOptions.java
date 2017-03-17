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

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.prefs.AppPreferences;

class ExperimentalOptions extends OptionsPanel {
	private static final long serialVersionUID = 1L;
	private JLabel accelRestart = new JLabel();
	private PrefOptionList accel;

	public ExperimentalOptions(PreferencesFrame window) {
		super(window);

		accel = new PrefOptionList(AppPreferences.GRAPHICS_ACCELERATION,
				Strings.getter("accelLabel"), new PrefOption[] {
						new PrefOption(AppPreferences.ACCEL_DEFAULT,
								Strings.getter("accelDefault")),
						new PrefOption(AppPreferences.ACCEL_NONE,
								Strings.getter("accelNone")),
						new PrefOption(AppPreferences.ACCEL_OPENGL,
								Strings.getter("accelOpenGL")),
						new PrefOption(AppPreferences.ACCEL_D3D,
								Strings.getter("accelD3D")), });

		JPanel accelPanel = new JPanel(new BorderLayout());
		accelPanel.add(accel.getJLabel(), BorderLayout.LINE_START);
		accelPanel.add(accel.getJComboBox(), BorderLayout.CENTER);
		accelPanel.add(accelRestart, BorderLayout.PAGE_END);
		accelRestart.setFont(accelRestart.getFont().deriveFont(Font.ITALIC));
		JPanel accelPanel2 = new JPanel();
		accelPanel2.add(accelPanel);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(Box.createGlue());
		add(accelPanel2);
		add(Box.createGlue());
	}

	@Override
	public String getHelpText() {
		return Strings.get("experimentHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("experimentTitle");
	}

	@Override
	public void localeChanged() {
		accel.localeChanged();
		accelRestart.setText(Strings.get("accelRestartLabel"));
	}
}
