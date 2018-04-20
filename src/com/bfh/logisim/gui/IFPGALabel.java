package com.bfh.logisim.gui;

import java.awt.Rectangle;

public interface IFPGALabel {
	void paintImmediately(Rectangle rect);
	Rectangle getBounds();
	void setText(String text);

}
