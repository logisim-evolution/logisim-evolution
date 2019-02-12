package com.cburch.logisim.fpga.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.cburch.logisim.proj.Projects;

public class DownloadProgressBar extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JLabel LocText;
	private JProgressBar Progress;
	
	private int ProgresSteps = 5;
	private static Dimension LoctextSize = new Dimension(600,30);

	public DownloadProgressBar(String title) {
		super(title);
		SetupGui();
	}
	
	public DownloadProgressBar(String title, int NrOfProgressSteps) {
		super(title);
		ProgresSteps = NrOfProgressSteps;
		SetupGui();
	}
	
	private void SetupGui() {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLayout(layout);
		LocText = new JLabel("...");
		LocText.setMaximumSize(LoctextSize);
		LocText.setPreferredSize(LoctextSize);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(LocText,gbc);
		Progress = new JProgressBar(0,ProgresSteps);
		Progress.setValue(0);
		Progress.setStringPainted(true);
		gbc.gridy = 1;
		add(Progress,gbc);
		pack();
		setLocation(Projects.getCenteredLoc(getWidth(), getHeight() * 4));
		setVisible(true);
	}
	
	public void SetStatus(String msg) {
		LocText.setText(msg);
		Rectangle bounds = LocText.getBounds();
		bounds.x = 0;
		bounds.y = 0;
		LocText.repaint(bounds);
	}
	
	public void SetProgress(int val) {
		Progress.setValue(val);
		Rectangle bounds = Progress.getBounds();
		bounds.x = 0;
		bounds.y = 0;
		Progress.repaint(bounds);
	}
	
}
