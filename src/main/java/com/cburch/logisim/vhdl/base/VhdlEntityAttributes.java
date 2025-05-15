/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class VhdlEntityAttributes extends AbstractAttributeSet {

  public static class VhdlGenericAttribute extends Attribute<Integer> {
    final int start;
    final int end;
    final VhdlContent.Generic g;

    private VhdlGenericAttribute(String name, StringGetter disp, int start, int end, VhdlContent.Generic generic) {
      super(name, disp);
      this.start = start;
      this.end = end;
      this.g = generic;
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
      if (value == null) return null;
      value = value.trim();
      if (value.length() == 0
          || value.equals("default")
          || value.equals("(default)")
          || value.equals(toDisplayString(null))) return null;
      final var v = Long.parseLong(value);
      if (v < start) throw new NumberFormatException("integer must be at least " + start);
      if (v > end) throw new NumberFormatException("integer must be at most " + end);
      return (int) v;
    }

    @Override
    public String toDisplayString(Integer value) {
      return value == null ? "(default) " + g.getDefaultValue() : value.toString();
    }
  }

  public static Attribute<Integer> forGeneric(VhdlContent.Generic generic) {
    final var name = generic.getName();
    final var disp = StringUtil.constantGetter(name);
    if (generic.getType().equals("positive"))
      return new VhdlGenericAttribute("vhdl_" + name, disp, 1, Integer.MAX_VALUE, generic);
    else if (generic.getType().equals("natural"))
      return new VhdlGenericAttribute("vhdl_" + name, disp, 0, Integer.MAX_VALUE, generic);
    else
      return new VhdlGenericAttribute("vhdl_" + name, disp, Integer.MIN_VALUE, Integer.MAX_VALUE, generic);
  }

  private static final List<Attribute<?>> static_attributes =
      Arrays.asList(
          VhdlEntity.nameAttr,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.FACING,
          StdAttr.APPEARANCE,
          VhdlSimConstants.SIM_NAME_ATTR);

  static AttributeSet createBaseAttrs(VhdlContent content) {
    final var generic = content.getGenerics();
    final var genericAttr = content.getGenericAttributes();
    final var attrs = new Attribute<?>[7 + generic.length];
    final var value = new Object[7 + generic.length];
    attrs[0] = VhdlEntity.nameAttr;
    value[0] = content.getName();
    attrs[1] = StdAttr.LABEL;
    value[1] = "";
    attrs[2] = StdAttr.LABEL_FONT;
    value[2] = StdAttr.DEFAULT_LABEL_FONT;
    attrs[3] = StdAttr.LABEL_VISIBILITY;
    value[3] = false;
    attrs[4] = StdAttr.FACING;
    value[4] = Direction.EAST;
    attrs[5] = StdAttr.APPEARANCE;
    value[5] = StdAttr.APPEAR_EVOLUTION;
    attrs[6] = VhdlSimConstants.SIM_NAME_ATTR;
    value[6] = "";
    for (var i = 0; i < generic.length; i++) {
      attrs[6 + i] = genericAttr.get(i);
      value[6 + i] = generic[i].getDefaultValue();
    }
    return AttributeSets.fixedSet(attrs, value);
  }

  private VhdlContent content;
  private Instance vhdlInstance;
  private String label = "";
  private String simName = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Direction facing = Direction.EAST;
  private Boolean labelVisible = false;
  private HashMap<Attribute<Integer>, Integer> genericValues;
  private List<Attribute<?>> instanceAttrs;
  private VhdlEntityListener listener;

  public VhdlEntityAttributes(VhdlContent content) {
    this.content = content;
    genericValues = null;
    vhdlInstance = null;
    listener = null;
    updateGenerics();
  }

  public VhdlContent getContent() {
    return content;
  }

  public Direction getFacing() {
    return facing;
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
    instanceAttrs = new ArrayList<>(6 + genericAttrs.size());
    instanceAttrs.add(VhdlEntity.nameAttr);
    instanceAttrs.add(StdAttr.LABEL);
    instanceAttrs.add(StdAttr.LABEL_FONT);
    instanceAttrs.add(StdAttr.LABEL_VISIBILITY);
    instanceAttrs.add(StdAttr.FACING);
    instanceAttrs.add(StdAttr.APPEARANCE);
    instanceAttrs.add(VhdlSimConstants.SIM_NAME_ATTR);
    instanceAttrs.addAll(genericAttrs);
    if (genericValues == null) genericValues = new HashMap<>();
    ArrayList<Attribute<Integer>> toRemove = new ArrayList<>();
    for (Attribute<Integer> a : genericValues.keySet()) {
      if (!genericAttrs.contains(a)) toRemove.add(a);
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
    // attr.label = unchanged;
    attr.labelFont = labelFont;
    attr.labelVisible = labelVisible;
    attr.facing = facing;
    attr.instanceAttrs = instanceAttrs;
    attr.genericValues = new HashMap<>();
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
    if (attr == VhdlEntity.nameAttr) {
      return (V) content.getName();
    }
    if (attr == StdAttr.LABEL) {
      return (V) label;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisible;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) content.getAppearance();
    }
    if (attr == StdAttr.FACING) {
      return (V) facing;
    }
    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      return (V) simName;
    }
    if (genericValues.containsKey(attr)) {
      return (V) genericValues.get(attr);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == VhdlEntity.nameAttr) {
      final var newValue = (String) value;
      if (content.getName().equals(newValue)) return;
      if (!content.setName(newValue)) return;
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (attr == StdAttr.LABEL && value instanceof String newLabel) {
      final var oldLabel = label;
      if (label.equals(newLabel)) return;
      label = newLabel;
      fireAttributeValueChanged(attr, value, (V) oldLabel);
      return;
    }

    if (attr == StdAttr.LABEL_FONT && value instanceof Font newFont) {
      if (labelFont.equals(newFont)) return;
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (attr == StdAttr.LABEL_VISIBILITY) {
      final var newVisibility = (Boolean) value;
      if (labelVisible.equals(newVisibility)) return;
      labelVisible = newVisibility;
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (attr == StdAttr.FACING) {
      final var direction = (Direction) value;
      if (facing.equals(direction)) return;
      facing = direction;
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (attr == VhdlSimConstants.SIM_NAME_ATTR) {
      final var name = (String) value;
      if (name.equals(simName)) return;
      simName = name;
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (attr == StdAttr.APPEARANCE
        && (value == StdAttr.APPEAR_FPGA
            || value == StdAttr.APPEAR_CLASSIC
            || value == StdAttr.APPEAR_EVOLUTION)) {
      final var attrOpt = (AttributeOption) value;
      if (content.getAppearance().equals(attrOpt)) return;
      content.setAppearance(attrOpt);
      fireAttributeValueChanged(attr, value, null);
      return;
    }

    if (genericValues != null) {
      genericValues.put((Attribute<Integer>) attr, (Integer) value);
      fireAttributeValueChanged(attr, value, null);
    }
  }

  static class VhdlEntityListener implements HdlModelListener {
    final VhdlEntityAttributes attrs;

    VhdlEntityListener(VhdlEntityAttributes attrs) {
      this.attrs = attrs;
    }

    @Override
    public void contentSet(HdlModel source) {
      attrs.updateGenerics();
      attrs.vhdlInstance.fireInvalidated();
      attrs.vhdlInstance.recomputeBounds();
      attrs.fireAttributeValueChanged(VhdlEntity.nameAttr, source.getName(), null);
    }

    @Override
    public void appearanceChanged(HdlModel source) {
      attrs.vhdlInstance.recomputeBounds();
      attrs.fireAttributeValueChanged(StdAttr.APPEARANCE, ((VhdlContent) source).getAppearance(), null);
    }
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != VhdlSimConstants.SIM_NAME_ATTR;
  }
}
