package com.cburch.logisim.gui.menu;

import javax.swing.JMenuItem;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class ScaledMenuItem extends JMenuItem {
	
	public ScaledMenuItem() {
		setFont(AppPreferences.getScaledFont(getFont()));
	}

}
