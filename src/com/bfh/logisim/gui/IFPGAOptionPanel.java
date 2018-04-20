package com.bfh.logisim.gui;

import java.awt.Component;

import javax.swing.Icon;

public interface IFPGAOptionPanel {
	public int doshowOptionDialog(IFPGAProgressBar parentComponent,
			Object message,
			String title,
			int optionType,
			int messageType,
			Icon icon,
			Object[] options,
			Object initialValue);

	public void doshowMessageDialog(Component parentComponent, Object message);
}
