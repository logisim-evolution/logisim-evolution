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

package com.bfh.logisim.designrulecheck;

import java.util.HashSet;
import java.util.Set;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.instance.InstanceComponent;

public class SimpleDRCContainer {
	
	public static final int LEVEL_NORMAL = 1;
	public static final int LEVEL_SEVERE = 2;
	public static final int LEVEL_FATAL = 3;
	public static final int MARK_NONE = 0;
	public static final int MARK_INSTANCE = 1;
	public static final int MARK_LABEL = 2;
	public static final int MARK_WIRE = 4;
	
	private String Message;
	private int SeverityLevel;
	private Set<Object> DRCComponents;
	private Circuit MyCircuit;
	private int MarkType;
	private int ListNumber;
	private boolean SuppressCount;
	
	public SimpleDRCContainer(String Message, int level) {
		this.Message = Message;
		this.SeverityLevel = level;
		MarkType = MARK_NONE;
		ListNumber = 0;
		SuppressCount = false;
	}

	public SimpleDRCContainer(String Message, int level, boolean SupressCount) {
		this.Message = Message;
		this.SeverityLevel = level;
		MarkType = MARK_NONE;
		ListNumber = 0;
		this.SuppressCount = SupressCount;
	}

	public SimpleDRCContainer(Object Message, int level) {
		this.Message = Message.toString();
		this.SeverityLevel = level;
		MarkType = MARK_NONE;
		ListNumber = 0;
		SuppressCount = false;
	}

	public SimpleDRCContainer(Object Message, int level, boolean SupressCount) {
		this.Message = Message.toString();
		this.SeverityLevel = level;
		MarkType = MARK_NONE;
		ListNumber = 0;
		this.SuppressCount = SupressCount;
	}

	public SimpleDRCContainer(Circuit circ, Object Message, int level, int MarkMask) {
		this.Message = Message.toString();
		this.SeverityLevel = level;
		MyCircuit=circ;
		MarkType = MarkMask;
		ListNumber = 0;
		SuppressCount = false;
	}

	public SimpleDRCContainer(Circuit circ, Object Message, int level, int MarkMask, boolean SupressCount) {
		this.Message = Message.toString();
		this.SeverityLevel = level;
		MyCircuit=circ;
		MarkType = MarkMask;
		ListNumber = 0;
		this.SuppressCount = SupressCount;
	}

	@Override
	public String toString() {
		return Message;
	}
	
	public int Severity() {
		return SeverityLevel;
	}
	
	public boolean DRCInfoPresent() {
		if (DRCComponents == null||
			MyCircuit==null)
			return false;
		return !DRCComponents.isEmpty();
	}
	
	public Circuit GetCircuit() {
		return MyCircuit;
	}
	
	public boolean HasCircuit() {
		return (MyCircuit!=null);
	}
	
	public void AddMarkComponent(Object comp) {
		if (DRCComponents==null) 
			DRCComponents = new HashSet<Object>();
		DRCComponents.add(comp);
	}
	
	public void AddMarkComponents(Set<?> set) {
		if (DRCComponents==null) 
			DRCComponents = new HashSet<Object>();
		DRCComponents.addAll(set);
	}
	
	public void SetListNumber(int number) {
		ListNumber = number;
	}
	
	public boolean SupressCount() {
		return this.SuppressCount;
	}
	
	public int GetListNumber() {
		return ListNumber;
	}
	
	public void MarkComponents() {
		if (!DRCInfoPresent())
			return;
		for (Object obj : DRCComponents) {
			if (obj instanceof Wire) {
				Wire wire = (Wire)obj;
				if ((MarkType&MARK_WIRE)!=0) {
					wire.SetDRCHighlight(true);
				}
			} else
			if (obj instanceof Splitter) {
				Splitter split = (Splitter)obj;
				if ((MarkType&MARK_INSTANCE)!=0) {
					split.SetMarked(true);
				}
			} else
			if (obj instanceof InstanceComponent) {
				InstanceComponent comp = (InstanceComponent)obj;
				if ((MarkType&MARK_INSTANCE)!=0)
					comp.MarkInstance();
				if ((MarkType&MARK_LABEL)!=0)
					comp.MarkLabel();
			} else {
			}
		}
	}
	
	public void ClearMarks() {
		if (!DRCInfoPresent())
			return;
		for (Object obj : DRCComponents) {
			if (obj instanceof Wire) {
				Wire wire = (Wire)obj;
				if ((MarkType&MARK_WIRE)!=0) {
					wire.SetDRCHighlight(false);
				}
			} else
			if (obj instanceof Splitter) {
				Splitter split = (Splitter)obj;
				if ((MarkType&MARK_INSTANCE)!=0) {
					split.SetMarked(false);
				}
			} else
			if (obj instanceof InstanceComponent) {
				InstanceComponent comp = (InstanceComponent)obj;
				comp.clearMarks();
			}
		}
	}
}
