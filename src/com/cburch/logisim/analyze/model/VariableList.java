/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class VariableList {
	private ArrayList<VariableListListener> listeners = new ArrayList<VariableListListener>();
	private int maxSize;
	private ArrayList<String> data;
	private List<String> dataView;

	public VariableList(int maxSize) {
		this.maxSize = maxSize;
		data = maxSize > 16 ? new ArrayList<String>() : new ArrayList<String>(
				maxSize);
		dataView = Collections.unmodifiableList(data);
	}

	public void add(String name) {
		if (data.size() >= maxSize) {
			throw new IllegalArgumentException("maximum size is " + maxSize);
		}
		data.add(name);
		fireEvent(VariableListEvent.ADD, name);
	}

	//
	// listener methods
	//
	public void addVariableListListener(VariableListListener l) {
		listeners.add(l);
	}

	public boolean contains(String value) {
		return data.contains(value);
	}

	private void fireEvent(int type) {
		fireEvent(type, null, null);
	}

	private void fireEvent(int type, String variable) {
		fireEvent(type, variable, null);
	}

	private void fireEvent(int type, String variable, Object data) {
		if (listeners.size() == 0)
			return;
		VariableListEvent event = new VariableListEvent(this, type, variable,
				data);
		for (VariableListListener l : listeners) {
			l.listChanged(event);
		}
	}

	public String get(int index) {
		return data.get(index);
	}

	public List<String> getAll() {
		return dataView;
	}

	//
	// data methods
	//
	public int getMaximumSize() {
		return maxSize;
	}

	public int indexOf(String name) {
		return data.indexOf(name);
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public boolean isFull() {
		return data.size() >= maxSize;
	}

	public void move(String name, int delta) {
		int index = data.indexOf(name);
		if (index < 0)
			throw new NoSuchElementException(name);
		int newIndex = index + delta;
		if (newIndex < 0) {
			throw new IllegalArgumentException("cannot move index " + index
					+ " by " + delta);
		}
		if (newIndex > data.size() - 1) {
			throw new IllegalArgumentException("cannot move index " + index
					+ " by " + delta + ": size " + data.size());
		}
		if (index == newIndex)
			return;
		data.remove(index);
		data.add(newIndex, name);
		fireEvent(VariableListEvent.MOVE, name,
				Integer.valueOf(newIndex - index));
	}

	public void remove(String name) {
		int index = data.indexOf(name);
		if (index < 0)
			throw new NoSuchElementException("input " + name);
		data.remove(index);
		fireEvent(VariableListEvent.REMOVE, name, Integer.valueOf(index));
	}

	public void removeVariableListListener(VariableListListener l) {
		listeners.remove(l);
	}

	public void replace(String oldName, String newName) {
		int index = data.indexOf(oldName);
		if (index < 0)
			throw new NoSuchElementException(oldName);
		if (oldName.equals(newName))
			return;
		data.set(index, newName);
		fireEvent(VariableListEvent.REPLACE, oldName, Integer.valueOf(index));
	}

	public void setAll(List<String> values) {
		if (values.size() > maxSize) {
			throw new IllegalArgumentException("maximum size is " + maxSize);
		}
		data.clear();
		data.addAll(values);
		fireEvent(VariableListEvent.ALL_REPLACED);
	}

	public int size() {
		return data.size();
	}

	public String[] toArray(String[] dest) {
		return data.toArray(dest);
	}

}
