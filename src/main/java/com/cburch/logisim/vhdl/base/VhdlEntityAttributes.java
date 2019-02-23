package com.cburch.logisim.vhdl.base;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class VhdlEntityAttributes  extends AbstractAttributeSet {
	
	public static class VhdlGenericAttribute extends Attribute<Integer> {
		int start, end;
		VhdlContent.Generic g;

		private VhdlGenericAttribute(String name, StringGetter disp, int start, int end, VhdlContent.Generic g) {
			super(name, disp);
			this.start = start;
			this.end = end;
            this.g = g;
		}
		
		 public VhdlContent.Generic getGeneric() {
             return g;
         }

		@Override
		public java.awt.Component getCellEditor(Integer value) {
                    return super.getCellEditor(value != null ? value : g.getDefaultValue());
		}

		@Override
		public Integer parse(String value) {
                        if (value == null)
                            return null;
                        value = value.trim();
                        if (value.length() == 0 || value.equals("default") || value.equals("(default)") || value.equals(toDisplayString(null)))
                            return null;
			long v = (long) Long.parseLong(value);
			if (v < start)
				throw new NumberFormatException("integer too small");
			if (v > end)
				throw new NumberFormatException("integer too large");
			return Integer.valueOf((int)v);
		}

		@Override
		public String toDisplayString(Integer value) {
			return value == null ? "(default) " + g.getDefaultValue() : value.toString();
		}
	}

    public static Attribute<Integer> forGeneric(VhdlContent.Generic g) {
        String name = g.getName();
        StringGetter disp = StringUtil.constantGetter(name);
        if (g.getType().equals("positive"))
            return new VhdlGenericAttribute("vhdl_" + name, disp, 1, Integer.MAX_VALUE, g);
        else if (g.getType().equals("natural"))
            return new VhdlGenericAttribute("vhdl_" + name, disp, 0, Integer.MAX_VALUE, g);
        else
            return new VhdlGenericAttribute("vhdl_" + name, disp, Integer.MIN_VALUE, Integer.MAX_VALUE, g);
    }

	private static List<Attribute<?>> attributes = Arrays.asList(
			VhdlEntity.NAME_ATTR, VhdlEntity.CONTENT_ATTR, StdAttr.LABEL, 
			StdAttr.LABEL_FONT, StdAttr.LABEL_VISIBILITY);

    static AttributeSet createBaseAttrs(VhdlContent content) {
        VhdlContent.Generic[] g = content.getGenerics();
        List<Attribute<Integer>> a = content.getGenericAttributes();
        Attribute<?>[] attrs = new Attribute<?>[4 + g.length];
        Object[] value = new Object[4 + g.length];
        attrs[0] = VhdlEntity.NAME_ATTR;
        value[0] = content.getName();
        attrs[1] = VhdlEntity.CONTENT_ATTR;
        value[1] = content;
        attrs[2] = StdAttr.LABEL;
        value[2] = "";
        attrs[3] = StdAttr.LABEL_FONT;
        value[3] = StdAttr.DEFAULT_LABEL_FONT;
        for (int i = 0; i < g.length; i++) {
            attrs[4+i] = a.get(i);
            value[4+i] = new Integer(g[i].getDefaultValue());
        }
        AttributeSet ret = AttributeSets.fixedSet(attrs, value);
        ret.addAttributeListener(new StaticListener(content));
        	return ret;
    }

    private static class StaticListener implements AttributeListener {
    	private VhdlContent content;
    	private StaticListener(VhdlContent vhdl) { content = vhdl; }
    	public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) {
            if (e.getAttribute() == VhdlEntity.NAME_ATTR) {
                String newValue = (String)e.getValue();
                if (!content.getName().equals(newValue)
                        && !content.setName(newValue))
                    e.getSource().setValue(VhdlEntity.NAME_ATTR, content.getName());
            } 
        }
    }

	private VhdlContent content;
	private Instance vhdlInstance;
	private String label = "";
	private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
	private Boolean labelVisable = false;
    private HashMap<Attribute<Integer>, Integer> genericValues;
    private List<Attribute<?>> instanceAttrs;
    private VhdlEntityListener listener;

	VhdlEntityAttributes(VhdlContent content) {
		this.content = content;
		genericValues = null;
        vhdlInstance = null;
        listener = null;
        updateGenerics();
	}


    void setInstance(Instance value) {
        vhdlInstance = value;
        if (vhdlInstance != null && listener != null) {
            listener = new VhdlEntityListener(this);
	content.addHdlModelListener(listener);
        }
    }

    void updateGenerics() {
        List<Attribute<Integer>> genericAttrs = content.getGenericAttributes();
        instanceAttrs = new ArrayList<Attribute<?>>(4 + genericAttrs.size());
        instanceAttrs.add(VhdlEntity.NAME_ATTR);
        instanceAttrs.add(VhdlEntity.CONTENT_ATTR);
        instanceAttrs.add(StdAttr.LABEL);
        instanceAttrs.add(StdAttr.LABEL_FONT);
        for (Attribute<Integer> a : genericAttrs) {
            instanceAttrs.add(a);
        }
        if (genericValues == null)
            genericValues = new HashMap<Attribute<Integer>, Integer>();
        ArrayList<Attribute<Integer>> toRemove = new ArrayList<Attribute<Integer>>();
        for (Attribute<Integer> a : genericValues.keySet()) {
            if (!genericAttrs.contains(a))
                toRemove.add(a);
        }
        for (Attribute<Integer> a : toRemove) {
            genericValues.remove(a);
        }
        fireAttributeListChanged();
    }

@Override
	protected void copyInto(AbstractAttributeSet dest) {
    	VhdlEntityAttributes attr = (VhdlEntityAttributes) dest;
    	attr.content = content; // .clone();
    	// 	attr.label = unchanged;
    	attr.labelFont = labelFont;
    	attr.instanceAttrs = instanceAttrs;
    	attr.genericValues = new HashMap<Attribute<Integer>, Integer>();
    	for (Attribute<Integer> a : genericValues.keySet())
    		attr.genericValues.put(a, genericValues.get(a));
    	attr.listener = null;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return instanceAttrs;
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
		if (genericValues.containsKey((Attribute<Integer>)attr)) {
            V v = (V) genericValues.get((Attribute<Integer>)attr);
            return v;
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
			updateGenerics();
            fireAttributeValueChanged(attr, value, null);
            fireAttributeValueChanged(VhdlEntity.NAME_ATTR, content.getName(),null);
            return;
		}
		if (attr == VhdlEntity.NAME_ATTR) {
             String newValue = (String)value;
             if (content.getName().equals(newValue))
                 return;
             if (!content.setName(newValue))
                 return;
             fireAttributeValueChanged(attr, value,null);
             fireAttributeValueChanged(VhdlEntity.CONTENT_ATTR, content,null);
             return;
		 }
		 if (attr == StdAttr.LABEL && value instanceof String) {
			String newLabel = (String) value;
			String oldlabel = label;
			if (label.equals(newLabel))
				return;
			label = newLabel;
			fireAttributeValueChanged(attr, value, (V) oldlabel);
			return;
		}
		if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
			Font newFont = (Font) value;
			if (labelFont.equals(newFont))
				return;
			labelFont = newFont;
			fireAttributeValueChanged(attr, value,null);
			return;
		}
		if (attr == StdAttr.LABEL_VISIBILITY) {
			Boolean newvis = (Boolean) value;
			if (labelVisable.equals(newvis))
				return;
			labelVisable=newvis;
			fireAttributeValueChanged(attr, value,null);
			return;
		}
		 if (genericValues != null) {
             genericValues.put((Attribute<Integer>)attr, (Integer)value);
             fireAttributeValueChanged(attr, value,null);
         }
	}

    static class VhdlEntityListener implements HdlModelListener {
        VhdlEntityAttributes attrs;
        VhdlEntityListener(VhdlEntityAttributes attrs) {
            this.attrs = attrs;
        }
        @Override
        public void contentSet(HdlModel source) {
            attrs.updateGenerics();
            attrs.vhdlInstance.fireInvalidated();
            attrs.vhdlInstance.recomputeBounds();
        }
    }

}
