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

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class ScaledLabel extends JLabel {

	public ScaledLabel() {
		super();
		Init();
	}
	
	public ScaledLabel(Icon Image) {
		super(Image);
		Init();
	}
	
	public ScaledLabel(Icon image, int horizontalAlignment) {
		super(image,horizontalAlignment);
		Init();
	}
	
	public ScaledLabel(String text) {
		super(text);
		Init();
	}
	
	public ScaledLabel(String text, Icon icon, int horizontalAlignment) {
		super(text,icon,horizontalAlignment);
		Init();
	}
	
	public ScaledLabel(String text, int horizontalAlignment) {
		super(text,horizontalAlignment);
		Init();
	}
	
	public void setFont(Font font) {
		super.setFont(AppPreferences.getScaledFont(font));
	}
	
	private void Init() {
		AppPreferences.setScaledFonts(getComponents());
		super.setFont(AppPreferences.getScaledFont(super.getFont()));
	}
	
}
