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
package com.cburch.logisim.std.tcl;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.HdlContent;
import com.cburch.logisim.std.hdl.HdlContentEditor;
import com.cburch.logisim.std.hdl.VhdlContent;

/**
 * This attribute set is the same as the one for the TclComponent but it adds an
 * attribute to specify the interface VHDL entity definition. It calls the
 * parent class as often as possible to avoid code duplication.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclGenericAttributes extends TclComponentAttributes {

	public static HdlContentEditor getContentEditor(Window source,
			HdlContent value, Project proj) {
		synchronized (windowRegistry) {
			HdlContentEditor ret = windowRegistry.get(value);
			if (ret == null) {
				if (source instanceof Frame)
					ret = new HdlContentEditor((Frame) source, proj, value);
				else
					ret = new HdlContentEditor((Dialog) source, proj, value);
				windowRegistry.put(value, ret);
			}
			return ret;
		}
	}

	private static List<Attribute<?>> attributes = Arrays
			.asList(new Attribute<?>[] { CONTENT_FILE_ATTR,
					TclGeneric.CONTENT_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT });

	private final static WeakHashMap<HdlContent, HdlContentEditor> windowRegistry = new WeakHashMap<HdlContent, HdlContentEditor>();

	private VhdlContent vhdlEntitiy;

	TclGenericAttributes() {
		super();

		/*
		 * The editor is the same as for the VhdlContent, only the base template
		 * changes
		 */
		vhdlEntitiy = TclVhdlEntityContent.create();
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		TclGenericAttributes attr = (TclGenericAttributes) dest;
		attr.vhdlEntitiy = vhdlEntitiy;

		super.copyInto(dest);
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return attributes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {

		if (attr == TclGeneric.CONTENT_ATTR) {
			return (V) vhdlEntitiy;
		} else {
			return super.getValue(attr);
		}
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == TclGeneric.CONTENT_ATTR) {
			VhdlContent newContent = (VhdlContent) value;
			if (!vhdlEntitiy.equals(newContent))
				vhdlEntitiy = newContent;
			fireAttributeValueChanged(attr, value,null);
		} else {
			super.setValue(attr, value);
		}
	}
}
