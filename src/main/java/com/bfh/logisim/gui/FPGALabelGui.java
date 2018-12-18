package com.bfh.logisim.gui;

import javax.swing.JLabel;

public class FPGALabelGui extends JLabel implements IFPGALabel {
	/**
	 * For the need of class serialization
	 */
	private static final long serialVersionUID = 1L;

	FPGALabelGui(String value) {
		super(value);
	}
}
