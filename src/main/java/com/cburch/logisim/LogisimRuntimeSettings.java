package com.cburch.logisim;

import java.awt.GraphicsEnvironment;

public class LogisimRuntimeSettings {

	static private boolean isGui = true;
	static public final boolean CLI = false;
	static public final boolean GUI = false;



	static public boolean isRunTimeIsGui() {
		if (!GraphicsEnvironment.isHeadless()) {
			return !GraphicsEnvironment.isHeadless();
		}

		return isGui;
	}

	static public void setIsGui(boolean value) {
		isGui = value;
	}
}
