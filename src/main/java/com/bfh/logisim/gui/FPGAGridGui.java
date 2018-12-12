package com.bfh.logisim.gui;

import java.awt.GridBagConstraints;

public class FPGAGridGui extends GridBagConstraints implements IFPGAGrid {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setGridx(int x) {
		// TODO Auto-generated method stub
		super.gridx = x;
	}

	@Override
	public void setGridy(int y) {
		// TODO Auto-generated method stub
		super.gridy = y;
	}

	@Override
	public void setFill(int fill) {
		// TODO Auto-generated method stub
		super.fill = fill;
	}

}
