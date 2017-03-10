package com.cburch.logisim.gui.menu;

import javax.swing.JMenu;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class ScaledJMenu extends JMenu {
    public ScaledJMenu() {
    	setFont(AppPreferences.getScaledFont(getFont()));
    }
}
