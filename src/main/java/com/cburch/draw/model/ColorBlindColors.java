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
 *******************************************************************************/

package com.cburch.draw.model;

import java.awt.Color;

public class ColorBlindColors {
	/* Based on: http://http://mkweb.bcgsc.ca/colorblind/ */
	public final static Color COMMON_BLACK = new Color(0,0,0);
	public final static Color COMMON_ORANGE = new Color(230,159,0);
	public final static Color COMMON_SKYBLUE = new Color(86,180,233);
	public final static Color COMMON_BLUISHGREEN = new Color(0,158,115);
	public final static Color COMMON_Yellow = new Color(240,228,66);
	public final static Color COMMON_BLUE = new Color(0,114,178);
	public final static Color COMMON_VERMILLION = new Color(213,94,0);
	public final static Color COMMON_REDDISHPURPLE = new Color(204,121,167);
	
	/* see: https://ux.stackexchange.com/questions/94696/color-palette-for-all-types-of-color-blindness */
	public final static Color[] PALETTE14 = {
			new Color(73,0,146),  new Color(146,0,0),
			new Color(0,73,73) , new Color(0,109,219), new Color(146,73,0),
			new Color(0,146,146), new Color(182,109,255), new Color(219,209,0),
			new Color(255,109,182), new Color(109,182,255), new Color(36,255,36),
			new Color(255,182,119), new Color(182,219,255), new Color(255,255,109)
	};
	
	public final static String[] PALETTE14NAMES = {
			"MyPurple", "MyRedBrown" ,
			"MyNavyGreen", "MyNavyBlue", "MyBrown",
			"MyGrayGreen", "MyLightPurple", "MyOrange",
			"MyMagenta", "MyLightBlue", "MyGreen",
			"MySkin", "MyGrayBlue", "MyYellow"
	};
	
	public static Color[] PALETTE14WithAlpha(int alpha) {
		Color[] res = new Color[PALETTE14.length];
		for (int i = 0 ; i < PALETTE14.length ; i++) {
			Color s = PALETTE14[i];
			res[i] = new Color(s.getRed(),s.getRed(),s.getBlue(),alpha);
		}
		return res;
	}
}
