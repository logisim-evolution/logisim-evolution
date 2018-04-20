package com.bfh.logisim.gui;

import java.awt.Rectangle;

public interface IFPGAProgressBar {
	void setValue(int progress);
	void setStringPainted(boolean b);
	void paintImmediately(Rectangle r) ;
	Rectangle getBounds();
}
