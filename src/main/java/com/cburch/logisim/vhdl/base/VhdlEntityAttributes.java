package com.cburch.logisim.vhdl.base;

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
import com.cburch.logisim.vhdl.gui.HdlContentEditor;

public class VhdlEntityAttributes  extends AbstractAttributeSet {

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

	private static List<Attribute<?>> attributes = Arrays.asList(
			VhdlEntity.NAME_ATTR, VhdlEntity.CONTENT_ATTR, StdAttr.LABEL, 
			StdAttr.LABEL_FONT, StdAttr.LABEL_VISIBILITY);

	private final static WeakHashMap<HdlContent, HdlContentEditor> windowRegistry = new WeakHashMap<HdlContent, HdlContentEditor>();

	private VhdlContent content;
	private String label = "";
	private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
	private Boolean labelVisable = false;

	VhdlEntityAttributes(VhdlContent content) {
		this.content = content;
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		VhdlEntityAttributes attr = (VhdlEntityAttributes) dest;
		attr.labelFont = labelFont;
		attr.content = content;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return attributes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == VhdlEntity.CONTENT_ATTR) {
			return (V) content;
		}
        if (attr == VhdlEntity.NAME_ATTR) {
            return (V) content.getName();
        }
		if (attr == StdAttr.LABEL) {
			return (V) label;
		}
		if (attr == StdAttr.LABEL_FONT) {
			return (V) labelFont;
		}
		if (attr == StdAttr.LABEL_VISIBILITY) {
			return (V) labelVisable;
		}
		return null;
	}

	@Override
	public boolean isToSave(Attribute<?> attr) {
            return (attr != VhdlEntity.CONTENT_ATTR && attr != VhdlEntity.NAME_ATTR);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == VhdlEntity.CONTENT_ATTR) {
			VhdlContent newContent = (VhdlContent) value;
			if (content.equals(newContent))
			    return;
			content = newContent;
            fireAttributeValueChanged(attr, value, null);
            fireAttributeValueChanged(VhdlEntity.NAME_ATTR, content.getName(),null);
		}
		 else if (attr == VhdlEntity.NAME_ATTR) {
             String newValue = (String)value;
             if (content.getName().equals(newValue))
                 return;
             if (!content.setName(newValue))
                 return;
             fireAttributeValueChanged(attr, value,null);
             fireAttributeValueChanged(VhdlEntity.CONTENT_ATTR, content,null);
		 }
		 else if (attr == StdAttr.LABEL && value instanceof String) {
			String newLabel = (String) value;
			String oldlabel = label;
			if (label.equals(newLabel))
				return;
			label = newLabel;
			fireAttributeValueChanged(attr, value, (V) oldlabel);
		}
		 else if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
			Font newFont = (Font) value;
			if (labelFont.equals(newFont))
				return;
			labelFont = newFont;
			fireAttributeValueChanged(attr, value,null);
		}
		if (attr == StdAttr.LABEL_VISIBILITY) {
			Boolean newvis = (Boolean) value;
			if (labelVisable.equals(newvis))
				return;
			labelVisable=newvis;
			fireAttributeValueChanged(attr, value,null);
		}
	}

}
