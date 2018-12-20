package com.cburch.logisim.fpga.gui;

import java.awt.Rectangle;

public interface IFPGAProgressBar {
	void setValue(int progress);
	void setStringPainted(boolean b);
	void paintImmediately(Rectangle r) ;
	Rectangle getBounds();
}
