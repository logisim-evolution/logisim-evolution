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

package com.cburch.logisim.gui.scale;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class ScaledCheckBoxMenuItem extends JCheckBoxMenuItem {
	public ScaledCheckBoxMenuItem() {
		super();
		Init();
	}

	public ScaledCheckBoxMenuItem(Action a) {
		super(a);
		Init();
	}

	public ScaledCheckBoxMenuItem(Icon icon) {
		super(icon);
		Init();
	}

	public ScaledCheckBoxMenuItem(String text) {
		super(text);
		Init();
	}

	public ScaledCheckBoxMenuItem(String text, boolean b) {
		super(text,b);
		Init();
	}

	public ScaledCheckBoxMenuItem(String text, Icon icon) {
		super(text,icon);
		Init();
	}

	public ScaledCheckBoxMenuItem(String text, Icon icon, boolean b) {
		super(text,icon,b);
		Init();
	}

	private void Init() {
		AppPreferences.setScaledFonts(getComponents());
		super.setFont(AppPreferences.getScaledFont(getFont()));
	}
}
