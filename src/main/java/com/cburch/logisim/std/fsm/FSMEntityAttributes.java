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

package com.cburch.logisim.std.fsm;

import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

public class FSMEntityAttributes extends AbstractAttributeSet {

	private static final boolean GUI = true	;

	public static java.awt.Component getContentEditor(Window source, FSMContent value, Project proj) {
		synchronized (windowRegistry) {
			java.awt.Component ret = windowRegistry.get(value);
			if (ret == null) {
				if (!GUI) {
					if (source instanceof Frame)
						ret = new FSMContentTextEditor((Frame) source, proj, value);
					else
						ret = new FSMContentTextEditor((Dialog) source, proj, value);
					
				} else {
					if (source instanceof Frame)
						ret = new FSMContentVisualEditor((Frame) source, proj, value);
						//ret = new FSMContentEditor((Frame) source, proj, value);
					else
						ret = new FSMContentVisualEditor((Frame) source, proj, value);
					
				}
				windowRegistry.put(value, ret);
			}
			return ret;
		}
	}

	private static List<Attribute<?>> attributes = Arrays.asList(
			FSMEntity.CONTENT_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT);

	private final static WeakHashMap<FSMContent, java.awt.Component> windowRegistry = new WeakHashMap<FSMContent, java.awt.Component>();

	private FSMContent content;
	private String label = "";
	private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;

	FSMEntityAttributes() {
		content = new FSMContent(FSMContent.TEMPLATE);
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		FSMEntityAttributes attr = (FSMEntityAttributes) dest;
		attr.labelFont = labelFont;
		attr.content = content.clone();
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return attributes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == FSMEntity.CONTENT_ATTR) {
			return (V) content;
		}
		if (attr == StdAttr.LABEL) {
			return (V) label;
		}
		if (attr == StdAttr.LABEL_FONT) {
			return (V) labelFont;
		}
		return null;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == FSMEntity.CONTENT_ATTR) {
			FSMContent newContent = (FSMContent) value;
			if (!content.equals(newContent)) {
				fireAttributeValueChanged(attr, (V) newContent,(V)content);
				content = newContent;
			}
			return;
		}
		if (attr == StdAttr.LABEL && value instanceof String) {
			String newLabel = (String) value;
			if (!label.equals(newLabel)) {
				fireAttributeValueChanged(attr, (V) newLabel,(V)label);
				fireAttributeValueChanged(attr, value, (V) newLabel);
				label = newLabel;
			}
			return;
		}
		if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
			Font newFont = (Font) value;
			if (labelFont.equals(newFont)) {
				labelFont = newFont;
				fireAttributeValueChanged(attr, (V) newFont,(V)labelFont);
			}
			return;
		}
	}

}
