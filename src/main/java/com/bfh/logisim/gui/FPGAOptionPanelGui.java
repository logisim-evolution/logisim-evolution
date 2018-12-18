package com.bfh.logisim.gui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JOptionPane;

public class FPGAOptionPanelGui extends JOptionPane implements IFPGAOptionPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int doshowOptionDialog(IFPGAProgressBar parentComponent, Object message, String title, int optionType,
			int messageType, Icon icon, Object[] options, Object initialValue) {
		return super.showOptionDialog((Component) parentComponent, message,
				title,  optionType, messageType, icon, options,
				initialValue);
	}

	@Override
	public void doshowMessageDialog(Component parentComponent, Object message) {
		super.showMessageDialog(parentComponent, message);
	}
}
