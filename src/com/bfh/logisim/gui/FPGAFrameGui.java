package com.bfh.logisim.gui;

import java.awt.LayoutManager;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class FPGAFrameGui extends JFrame  implements IFPGAFrame {
	/**
	 * For the need of class serialization
	 */
	private static final long serialVersionUID = 1L;

	public FPGAFrameGui(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void add(IFPGALabel label, IFPGAGrid grid) {
		// TODO Auto-generated method stub
		super.add((JLabel) label, grid);

	}

	@Override
	public void add(IFPGAProgressBar progress, IFPGAGrid grid) {
		// TODO Auto-generated method stub
		super.add((JProgressBar) progress, grid);
	}

	@Override
	public void setLayout(IFPGAGridLayout layout) {
		// TODO Auto-generated method stub
		super.setLayout((LayoutManager) layout);
	}
}
