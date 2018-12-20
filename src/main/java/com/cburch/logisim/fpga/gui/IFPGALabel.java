package com.cburch.logisim.fpga.gui;

import java.awt.Rectangle;

public interface IFPGALabel {
	void paintImmediately(Rectangle rect);
	Rectangle getBounds();
	void setText(String text);

}
