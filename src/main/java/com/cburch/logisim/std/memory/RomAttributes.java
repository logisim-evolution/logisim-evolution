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
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

class RomAttributes extends AbstractAttributeSet {

	static HexFrame getHexFrame(MemContents value, Project proj) {
		synchronized (windowRegistry) {
			HexFrame ret = windowRegistry.get(value);
			if (ret == null) {
				ret = new HexFrame(proj, value);
				windowRegistry.put(value, ret);
			}
			return ret;
		}
	}

	static void register(MemContents value, Project proj) {
		if (proj == null || listenerRegistry.containsKey(value)) {
			return;
		}
		RomContentsListener l = new RomContentsListener(proj);
		value.addHexModelListener(l);
		listenerRegistry.put(value, l);
	}

	private static List<Attribute<?>> ATTRIBUTES = Arrays
			.asList(new Attribute<?>[] { Mem.ADDR_ATTR, Mem.DATA_ATTR,
					Rom.CONTENTS_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_VISIBILITY });

	private static WeakHashMap<MemContents, RomContentsListener> listenerRegistry = new WeakHashMap<MemContents, RomContentsListener>();

	private static WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<MemContents, HexFrame>();
	private BitWidth addrBits = BitWidth.create(8);
	private BitWidth dataBits = BitWidth.create(8);
	private MemContents contents;
	private String Label = "";
	private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
	private Boolean LabelVisable = false;

	RomAttributes() {
		contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth(),true);
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		RomAttributes d = (RomAttributes) dest;
		d.addrBits = addrBits;
		d.dataBits = dataBits;
		d.contents = contents.clone();
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == Mem.ADDR_ATTR) {
			return (V) addrBits;
		}
		if (attr == Mem.DATA_ATTR) {
			return (V) dataBits;
		}
		if (attr == Rom.CONTENTS_ATTR) {
			return (V) contents;
		}
		if (attr == StdAttr.LABEL) {
			return (V) Label;
		}
		if (attr == StdAttr.LABEL_FONT) {
			return (V) LabelFont;
		}
		if (attr == StdAttr.LABEL_VISIBILITY) {
			return (V) LabelVisable;
		}
		return null;
	}

	void setProject(Project proj) {
		register(contents, proj);
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == Mem.ADDR_ATTR) {
			BitWidth newAddr = (BitWidth) value;
			if (newAddr == addrBits)
				return;
			addrBits = newAddr;
			contents.setDimensions(addrBits.getWidth(), dataBits.getWidth(),true);
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == Mem.DATA_ATTR) {
			BitWidth newData = (BitWidth) value;
			if (newData == dataBits)
				return;
			dataBits = newData;
			contents.setDimensions(addrBits.getWidth(), dataBits.getWidth(),true);
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == Rom.CONTENTS_ATTR) {
			MemContents newContents = (MemContents) value;
			if (contents.equals(newContents))
				return;
			contents = newContents;
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == StdAttr.LABEL) {
			String NewLabel = (String) value;
			if (Label.equals(NewLabel))
				return;
			@SuppressWarnings("unchecked")
			V Oldlabel = (V) Label;
			Label = NewLabel;
			fireAttributeValueChanged(attr, value,Oldlabel);
		} else if (attr == StdAttr.LABEL_FONT) {
			Font NewFont = (Font) value;
			if (LabelFont.equals(NewFont))
				return;
			LabelFont = NewFont;
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == StdAttr.LABEL_VISIBILITY) {
			Boolean newVis = (Boolean) value;
			if (LabelVisable.equals(newVis))
				return;
			LabelVisable = newVis;
			fireAttributeValueChanged(attr, value,null);
		}
	}
}
