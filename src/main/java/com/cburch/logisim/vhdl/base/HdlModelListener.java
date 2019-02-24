package com.cburch.logisim.vhdl.base;

public interface HdlModelListener {


	/**
	 * Called when the content of the given model has been set.
	 */
	public void contentSet(HdlModel source);

	/**
	 * Called when the content of the given model is about to be saved.
	 */
	public void aboutToSave(HdlModel source);

	/**
	 * Called when the vhdl icon or name has changed.
	 */
	public void displayChanged(HdlModel source);
}
