package com.cburch.logisim.vhdl.base;

public interface HdlModel {

	/**
	 * Registers a listener for changes to the values.
	 */
	public void addHdlModelListener(HdlModelListener l);

	/**
	 * Compares the model's content with another model.
	 */
	public boolean compare(HdlModel model);

	/**
	 * Compares the model's content with a string.
	 */
	public boolean compare(String value);

	/**
	 * Gets the content of the HDL-IP component.
	 */
	public String getContent();

	/**
	 * Get the component's name
	 */
	public String getName();

	/**
	 * Unregisters a listener for changes to the values.
	 */
	public void removeHdlModelListener(HdlModelListener l);

	/**
	 * Sets the content of the component.
	 */
	public boolean setContent(String content);

	/**
	 * Checks whether the content of the component is valid.
	 */
	public boolean isValid();

	/**
	 * Displays errors, if any.
	 */
	public void showErrors();

	/**
	 * Fire notification that the display has changed.
	 */
	public void displayChanged();

}
