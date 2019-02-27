package com.cburch.logisim.vhdl.base;

import java.util.Arrays;

import com.cburch.logisim.util.EventSourceWeakSupport;

public abstract class HdlContent implements HdlModel, Cloneable  {
	protected static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	protected EventSourceWeakSupport<HdlModelListener> listeners;

	protected HdlContent() {
		this.listeners = null;
	}

	@Override
	public void addHdlModelListener(HdlModelListener l) {
		if (listeners == null) {
			listeners = new EventSourceWeakSupport<HdlModelListener>();
		}
		listeners.add(l);
	}

	@Override
	public HdlContent clone() throws CloneNotSupportedException {
		HdlContent ret = (HdlContent) super.clone();
		ret.listeners = null;
		return ret;
	}

	@Override
	public abstract boolean compare(HdlModel model);

	@Override
	public abstract boolean compare(String value);

	protected void fireContentSet() {
		if (listeners == null) {
			return;
		}

		boolean found = false;
		for (HdlModelListener l : listeners) {
			found = true;
			l.contentSet(this);
		}

		if (!found) {
			listeners = null;
		}
	}

	protected void fireAboutToSave() {
		if (listeners == null) {
			return;
		}

		boolean found = false;
		for (HdlModelListener l : listeners) {
			found = true;
			l.aboutToSave(this);
		}

		if (!found) {
			listeners = null;
		}
	}

	protected void fireAppearanceChanged() {
		if (listeners == null) {
			return;
		}

		boolean found = false;
		for (HdlModelListener l : listeners) {
			found = true;
			l.appearanceChanged(this);
		}

		if (!found) {
			listeners = null;
		}
	}

	public void displayChanged() {
		if (listeners == null) {
			return;
		}

		boolean found = false;
		for (HdlModelListener l : listeners) {
			found = true;
			l.displayChanged(this);
		}

		if (!found) {
			listeners = null;
		}
	}
	
	@Override
	public abstract String getContent();

	@Override
	public abstract String getName();

	@Override
	public void removeHdlModelListener(HdlModelListener l) {
		if (listeners == null) {
			return;
		}
		listeners.remove(l);
		if (listeners.isEmpty()) {
			listeners = null;
		}
	}

	@Override
	public abstract boolean setContent(String content);

}
