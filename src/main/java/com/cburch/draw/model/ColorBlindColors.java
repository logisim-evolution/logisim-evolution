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
	
	public static Color[] PALETTE14WithAlpha(int alpha) {
		Color[] res = new Color[PALETTE14.length];
		for (int i = 0 ; i < PALETTE14.length ; i++) {
			Color s = PALETTE14[i];
			res[i] = new Color(s.getRed(),s.getRed(),s.getBlue(),alpha);
		}
		return res;
	}
}
