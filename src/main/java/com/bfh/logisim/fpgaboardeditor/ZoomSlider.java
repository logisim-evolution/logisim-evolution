package com.bfh.logisim.fpgaboardeditor;

import java.awt.Dimension;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JSlider;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class ZoomSlider extends JSlider {

	 public ZoomSlider() {
		 JLabel label;
		 super.setOrientation(JSlider.HORIZONTAL);
		 super.setMinimum(100);
		 super.setMaximum(200);
		 super.setValue(100);
		 Dimension orig=super.getSize();
		 orig.height=AppPreferences.getScaled(orig.height);
		 orig.width=AppPreferences.getScaled(orig.width);
		 super.setSize(orig);
		 setMajorTickSpacing(50);
		 setMinorTickSpacing(10);
		 setPaintTicks(true);
		 Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		 label = new JLabel("1.0x");
		 label.setFont(AppPreferences.getScaledFont(label.getFont()));
		 labelTable.put(new Integer(100), label);
		 label = new JLabel("1.5x");
		 label.setFont(AppPreferences.getScaledFont(label.getFont()));
		 labelTable.put(new Integer(150), label);
		 label = new JLabel("2.0x");
		 label.setFont(AppPreferences.getScaledFont(label.getFont()));
		 labelTable.put(new Integer(200), label);
		 setLabelTable(labelTable);
		 setPaintLabels(true);
	 }
}
