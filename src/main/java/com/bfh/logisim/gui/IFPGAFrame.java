package com.bfh.logisim.gui;

import java.awt.Point;


public interface IFPGAFrame {

	public void dispose() ;

	public void setLayout(IFPGAGridLayout layout);

	public void setDefaultCloseOperation(int def);

	public void setResizable(boolean isResizable);

	public void setVisible(boolean isVisible);

	public void add(IFPGALabel label, IFPGAGrid grid);

	public void add(IFPGAProgressBar progress, IFPGAGrid grid);

	public int getHeight();

	public void setLocation(int x, int y);

	public void setLocation(Point p);

	public int getWidth();

	public void pack();

}
