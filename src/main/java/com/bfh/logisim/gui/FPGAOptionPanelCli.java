package com.bfh.logisim.gui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JOptionPane;

public class FPGAOptionPanelCli implements IFPGAOptionPanel {

	@Override
	public int doshowOptionDialog(IFPGAProgressBar parentComponent, Object message, String title, int optionType,
			int messageType, Icon icon, Object[] options, Object initialValue) {
		return JOptionPane.NO_OPTION;
	}

	@Override
	public void doshowMessageDialog(Component parentComponent, Object message) {

	}
}
