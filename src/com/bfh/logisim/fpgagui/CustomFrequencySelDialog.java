package com.bfh.logisim.fpgagui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class CustomFrequencySelDialog implements ActionListener {

	private JDialog panel;
	private double TickFrequency = 1.0;
	private double ClockFreq;
	private JButton DoneButton = new JButton();
	private JButton CancelButton = new JButton();
	private JLabel FreqText = new JLabel();
	private JTextField DividerText = new JTextField();
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getActionCommand().equals("Done")) {
			TickFrequency = ClockFreq / GetDivider();
			panel.setVisible(false);
		} else if (e.getActionCommand().equals("Cancel")) {
			DividerText.setText(Long.toString((long) (ClockFreq/TickFrequency)));
			panel.setVisible(false);
		} else if (e.getActionCommand().equals("DivText")) {
			UpdateFreqText();
		}
	}
	
	private double GetDivider() {
		String str = DividerText.getText();
		double ret = 0.0;
		boolean ok = true;
        int i = 0;
        while (i < str.length()) {
        	if ((str.charAt(i) >= '0')&&(str.charAt(i)<= '9')) {
        		ret *= 10.0;
        		ret += (double) (str.charAt(i)-'0');
        	} else {
        		ok = false;
        	}
        	i++;
        }
        if (ret < 4.0 || !ok) {
        	long value = (long) (ClockFreq/TickFrequency);
			if (value < 4)
               value = 4;
			DividerText.setText(Long.toString(value));
    		return GetDivider();
        }
        return ret;
	}
	
	private double round2(double val) {
		double ret = val*100;
		long tmp = Math.round(ret);
		ret = (double) tmp/100;
		return ret;
	}
	
	private void UpdateFreqText() {
		double div = ClockFreq / GetDivider();
		if (div < 1000) {
			FreqText.setText(Double.toString(round2(div))+" Hz");
		} else if (div < 1000000) {
			FreqText.setText(Double.toString(round2(div/1000.0))+" kHz");
		} else {
			FreqText.setText(Double.toString(round2(div/1000000.0))+" MHz");
		}
	}
	
	public void Reset(double ClockFrequency) {
		TickFrequency = 1.0;
		ClockFreq = ClockFrequency;
	}

	public CustomFrequencySelDialog(JFrame parrentFrame, double ClockFrequency) {
		ClockFreq = ClockFrequency;
		panel = new JDialog(parrentFrame, ModalityType.APPLICATION_MODAL);
		panel.setTitle("Custom Tick Frequency selection");
		panel.setResizable(false);
		panel.setAlwaysOnTop(true);
		panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		GridBagLayout thisLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(thisLayout);

		JLabel Text = new JLabel();
		Text.setText("TickFrequency:");
		Text.setHorizontalAlignment(JLabel.CENTER);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		panel.add(Text, c);
		
		FreqText.setHorizontalAlignment(JLabel.CENTER);
		c.gridx = 1;
		c.gridy = 0;
		panel.add(FreqText, c);
		
		Text = new JLabel();
		Text.setText("Divider value (min. 4):");
		Text.setHorizontalAlignment(JLabel.CENTER);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		panel.add(Text, c);
		
		DividerText.setText(Long.toString((long) ClockFreq));
		DividerText.addActionListener(this);
		DividerText.setActionCommand("DivText");
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		panel.add(DividerText, c);

		/* Add the Cancel button */
		CancelButton.setText("Cancel");
		CancelButton.setActionCommand("Cancel");
		CancelButton.addActionListener(this);
		CancelButton.setEnabled(true);
		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 1;
		panel.add(CancelButton, c);

		/* Add the Done button */
		DoneButton.setText("Done");
		DoneButton.setActionCommand("Done");
		DoneButton.addActionListener(this);
		DoneButton.setEnabled(true);
		c.gridy = 2;
		c.gridx = 1;
		panel.add(DoneButton, c);

		
		panel.pack();
		panel.setLocationRelativeTo(null);
		panel.setVisible(false);
		DoneButton.setPreferredSize(Text.getPreferredSize());
		CancelButton.setPreferredSize(Text.getPreferredSize());
		panel.pack();
		UpdateFreqText();
	}
	
	public double GetFrequency() {
		return TickFrequency;
	}
	
	public void setVisible( boolean value ) {
		panel.setVisible(value);
	}

}
