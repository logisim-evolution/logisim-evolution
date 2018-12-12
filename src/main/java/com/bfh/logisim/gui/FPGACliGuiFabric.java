package com.bfh.logisim.gui;

import com.cburch.logisim.LogisimRuntimeSettings;

public class  FPGACliGuiFabric {

	static public IFPGAFrame getFPGAFrame(String title) {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAFrameGui(title);
		else
			return new FPGAFrameCli(title);
	}

	static public IFPGALabel getFPGALabel(String value) {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGALabelGui(value);
		else
			return new FPGALabelCli(value);
	}

	static public IFPGAProgressBar getFPGAProgressBar() {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAProgressBarGui();
		else
			return new FPGAProgressBarCli();
	}

	static public IFPGAProgressBar getFPGAProgressBar(int min, int max) {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAProgressBarGui(min, max);
		else
			return new FPGAProgressBarCli(min, max);
	}

	static public IFPGAGrid getFPGAGrid() {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAGridGui();
		else
			return new FPGAGridCli();
	}

	static public IFPGAGridLayout getFPGAGridLayout() {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAGridLayoutGui();
		else
			return new FPGAGridLayoutCli();
	}


	static public IFPGAOptionPanel getFPGAOptionPanel() {
		if (LogisimRuntimeSettings.isRunTimeIsGui())
			return new FPGAOptionPanelGui();
		else
			return new FPGAOptionPanelCli();
	}
}
