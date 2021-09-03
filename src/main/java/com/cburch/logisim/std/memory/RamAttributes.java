/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class RamAttributes extends AbstractAttributeSet {

  /* here the rest is defined */
  static final AttributeOption VOLATILE = new AttributeOption("volatile", S.getter("ramTypeVolatile"));
  static final AttributeOption NONVOLATILE = new AttributeOption("nonvolatile", S.getter("ramTypeNonVolatile"));
  static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption("type", S.getter("ramTypeAttr"), new AttributeOption[] {VOLATILE, NONVOLATILE });
  static final AttributeOption BUS_BIDIR = new AttributeOption("bidir", S.getter("ramBidirDataBus"));
  static final AttributeOption BUS_SEP = new AttributeOption("bibus", S.getter("ramSeparateDataBus"));
  static final Attribute<AttributeOption> ATTR_DBUS = Attributes.forOption("databus", S.getter("ramDataAttr"), new AttributeOption[] {BUS_BIDIR, BUS_SEP});
  static final AttributeOption BUS_WITH_BYTEENABLES = new AttributeOption("byteEnables", S.getter("ramWithByteEnables"));
  static final AttributeOption BUS_WITHOUT_BYTEENABLES = new AttributeOption("NobyteEnables", S.getter("ramNoByteEnables"));
  static final Attribute<AttributeOption> ATTR_ByteEnables = Attributes.forOption("byteenables", S.getter("ramByteEnables"), new AttributeOption[] {BUS_WITH_BYTEENABLES, BUS_WITHOUT_BYTEENABLES});
  static final Attribute<Boolean> CLEAR_PIN = Attributes.forBoolean("clearpin", S.getter("RamClearPin"));
  private final ArrayList<Attribute<?>> myAttributes = new ArrayList<>();


  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private String label = "";
  private AttributeOption Trigger = StdAttr.TRIG_RISING;
  private AttributeOption BusStyle = BUS_SEP;
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;
  private AttributeOption ByteEnables = BUS_WITHOUT_BYTEENABLES;
  private Boolean asynchronousRead = false;
  private AttributeOption Appearance = AppPreferences.getDefaultAppearance();
  private AttributeOption RWBehavior = Mem.READAFTERWRITE;
  private Boolean ClearPin = false;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private AttributeOption typeOfEnables = Mem.USEBYTEENABLES;
  private AttributeOption ramType = VOLATILE;

  RamAttributes() {
    updateAttributes();
  }

  public boolean updateAttributes() {
    final var newList = new ArrayList<Attribute<?>>();
    var changes = false;
    newList.add(Mem.ADDR_ATTR);
    newList.add(Mem.DATA_ATTR);
    newList.add(Mem.ENABLES_ATTR);
    newList.add(ATTR_TYPE);
    newList.add(CLEAR_PIN);
    if (typeOfEnables.equals(Mem.USEBYTEENABLES)) {
      newList.add(StdAttr.TRIGGER);
      if (Trigger.equals(StdAttr.TRIG_RISING) || Trigger.equals(StdAttr.TRIG_FALLING)) {
        changes |= !myAttributes.contains(Mem.ASYNC_READ);
        newList.add(Mem.ASYNC_READ);
        if (!asynchronousRead) {
          changes |= !myAttributes.contains(Mem.READ_ATTR);
          newList.add(Mem.READ_ATTR);
        } else changes |= myAttributes.contains(Mem.READ_ATTR);
        if (dataBits.getWidth() > 8) {
          changes |= !myAttributes.contains(ATTR_ByteEnables);
          newList.add(ATTR_ByteEnables);
        } else changes |= myAttributes.contains(ATTR_ByteEnables);
      } else changes |= myAttributes.contains(Mem.ASYNC_READ);
      newList.add(ATTR_DBUS);
      changes |= myAttributes.contains(Mem.LINE_ATTR);
      changes |= myAttributes.contains(Mem.ALLOW_MISALIGNED);
    } else {
      newList.add(Mem.LINE_ATTR);
      newList.add(Mem.ALLOW_MISALIGNED);
      newList.add(StdAttr.TRIGGER);
      newList.add(ATTR_DBUS);
      changes |= !myAttributes.contains(Mem.LINE_ATTR);
      changes |= !myAttributes.contains(Mem.ALLOW_MISALIGNED);
    }
    newList.add(StdAttr.LABEL);
    newList.add(StdAttr.LABEL_FONT);
    newList.add(StdAttr.LABEL_VISIBILITY);
    newList.add(StdAttr.APPEARANCE);
    if (changes) {
      myAttributes.clear();
      myAttributes.addAll(newList);
    }
    return changes;
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var d = (RamAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.Trigger = Trigger;
    d.BusStyle = BusStyle;
    d.labelFont = labelFont;
    d.Appearance = Appearance;
    d.ByteEnables = ByteEnables;
    d.asynchronousRead = asynchronousRead;
    d.RWBehavior = RWBehavior;
    d.ClearPin = ClearPin;
    d.lineSize = lineSize;
    d.allowMisaligned = allowMisaligned;
    d.typeOfEnables = typeOfEnables;
    d.ramType = ramType;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return myAttributes;
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
    if (attr == ATTR_TYPE) {
      return (V) ramType;
    }
    if (attr == StdAttr.LABEL) {
      return (V) label;
    }
    if (attr == StdAttr.TRIGGER) {
      return (V) Trigger;
    }
    if (attr == Mem.ASYNC_READ) {
      return (V) asynchronousRead;
    }
    if (attr == Mem.READ_ATTR) {
      return (V) RWBehavior;
    }
    if (attr == ATTR_DBUS) {
      return (V) BusStyle;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisible;
    }
    if (attr == ATTR_ByteEnables) {
      return (V) ByteEnables;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) Appearance;
    }
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == Mem.ALLOW_MISALIGNED) {
      return (V) allowMisaligned;
    }
    if (attr == CLEAR_PIN) {
      return (V) ClearPin;
    }
    if (attr == Mem.ENABLES_ATTR) {
      return (V) typeOfEnables;
    }
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      final var newAddr = (BitWidth) value;
      if (addrBits == newAddr) {
        return;
      }
      addrBits = newAddr;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      final var newData = (BitWidth) value;
      if (dataBits == newData) {
        return;
      }
      dataBits = newData;
      if (typeOfEnables.equals(Mem.USEBYTEENABLES) && updateAttributes())
        fireAttributeListChanged();
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ENABLES_ATTR) {
      final var val = (AttributeOption) value;
      if (!typeOfEnables.equals(val)) {
        typeOfEnables = val;
        if (updateAttributes()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == ATTR_TYPE) {
      final var val = (AttributeOption) value;
      if (!ramType.equals(val)) {
        ramType = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == StdAttr.LABEL) {
      final var newLabel = (String) value;
      if (label.equals(newLabel)) {
        return;
      }
      @SuppressWarnings("unchecked")
      V Oldlabel = (V) label;
      label = newLabel;
      fireAttributeValueChanged(attr, value, Oldlabel);
    } else if (attr == StdAttr.TRIGGER) {
      final var newTrigger = (AttributeOption) value;
      if (Trigger.equals(newTrigger)) {
        return;
      }
      Trigger = newTrigger;
      if (typeOfEnables.equals(Mem.USEBYTEENABLES) && updateAttributes())
        fireAttributeListChanged();
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ASYNC_READ) {
      final var val = (Boolean) value;
      if (asynchronousRead != val) {
        asynchronousRead = val;
        if (updateAttributes()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.READ_ATTR) {
      final var val = (AttributeOption) value;
      if (!RWBehavior.equals(val)) {
        RWBehavior = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == ATTR_DBUS) {
      final var NewStyle = (AttributeOption) value;
      if (BusStyle.equals(NewStyle)) {
        return;
      }
      BusStyle = NewStyle;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_FONT) {
      final var newFont = (Font) value;
      if (labelFont.equals(newFont)) {
        return;
      }
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      final var newVis = (Boolean) value;
      if (labelVisible.equals(newVis)) return;
      labelVisible = newVis;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == ATTR_ByteEnables) {
      var newBE = (AttributeOption) value;
      if (ByteEnables.equals(newBE)) {
        return;
      }
      if (dataBits.getWidth() < 9) {
        newBE = BUS_WITHOUT_BYTEENABLES;
      }
      ByteEnables = newBE;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == CLEAR_PIN) {
      Boolean val = (Boolean) value;
      if (ClearPin != val) {
        ClearPin = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.LINE_ATTR) {
      final var val = (AttributeOption) value;
      if (!lineSize.equals(val)) {
        lineSize = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.ALLOW_MISALIGNED) {
      final var val = (Boolean) value;
      if (allowMisaligned != val) {
        allowMisaligned = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == StdAttr.APPEARANCE) {
      final var NewAppearance = (AttributeOption) value;
      if (Appearance.equals(NewAppearance)) return;
      Appearance = NewAppearance;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
