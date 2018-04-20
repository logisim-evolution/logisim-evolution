package com.bfh.logisim.gui;

import javax.swing.JProgressBar;

public class FPGAProgressBarGui extends JProgressBar implements IFPGAProgressBar {

	/**
	 * For the need of class serialization
	 */
	private static final long serialVersionUID = 1L;

	public FPGAProgressBarGui() {
		super();
	}

	public FPGAProgressBarGui(int min, int max) {
		super(min, max);
	}
}
