/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

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

import static com.cburch.logisim.std.Strings.S;

public class DualPortRamAttributes extends AbstractAttributeSet {

  /* here the rest is defined */
  static final AttributeOption VOLATILE =
      new AttributeOption("volatile", S.getter("ramTypeVolatile"));
  static final AttributeOption NONVOLATILE =
      new AttributeOption("nonvolatile", S.getter("ramTypeNonVolatile"));
  static final Attribute<AttributeOption> ATTR_TYPE =
      Attributes.forOption(
          "type", S.getter("ramTypeAttr"), new AttributeOption[] {VOLATILE, NONVOLATILE});
  static final AttributeOption BUS_BIDIR =
      new AttributeOption("bidir", S.getter("ramBidirDataBus"));
  static final AttributeOption BUS_WITH_BYTEENABLES =
      new AttributeOption("byteEnables", S.getter("ramWithByteEnables"));
  static final AttributeOption BUS_WITHOUT_BYTE_ENABLES =
      new AttributeOption("NobyteEnables", S.getter("ramNoByteEnables"));
  static final Attribute<AttributeOption> ATTR_ByteEnables =
      Attributes.forOption(
          "byteenables",
          S.getter("ramByteEnables"),
          new AttributeOption[] {BUS_WITH_BYTEENABLES, BUS_WITHOUT_BYTE_ENABLES});
  static final Attribute<Boolean> CLEAR_PIN =
      Attributes.forBoolean("clearpin", S.getter("RamClearPin"));
  private final ArrayList<Attribute<?>> myAttributes = new ArrayList<>();

  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private String label = "";
  private AttributeOption trigger = StdAttr.TRIG_RISING;
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;
  private AttributeOption byteEnables = BUS_WITHOUT_BYTE_ENABLES;
  private Boolean asynchronousRead = false;
  private AttributeOption appearance = AppPreferences.getDefaultAppearance();
  private AttributeOption readWriteBehavior = Mem.READAFTERWRITE;
  private Boolean clearPin = false;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private AttributeOption typeOfEnables = Mem.USEBYTEENABLES;
  private AttributeOption ramType = VOLATILE;

  DualPortRamAttributes() {
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
      if (trigger.equals(StdAttr.TRIG_RISING) || trigger.equals(StdAttr.TRIG_FALLING)) {
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
      changes |= myAttributes.contains(Mem.ALLOW_MISALIGNED);
    } else {
      newList.add(Mem.ALLOW_MISALIGNED);
      newList.add(StdAttr.TRIGGER);
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
    final var d = (DualPortRamAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.trigger = trigger;
    d.labelFont = labelFont;
    d.appearance = appearance;
    d.byteEnables = byteEnables;
    d.asynchronousRead = asynchronousRead;
    d.readWriteBehavior = readWriteBehavior;
    d.clearPin = clearPin;
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
      return (V) trigger;
    }
    if (attr == Mem.ASYNC_READ) {
      return (V) asynchronousRead;
    }
    if (attr == Mem.READ_ATTR) {
      return (V) readWriteBehavior;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisible;
    }
    if (attr == ATTR_ByteEnables) {
      return (V) byteEnables;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) appearance;
    }
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == Mem.ALLOW_MISALIGNED) {
      return (V) allowMisaligned;
    }
    if (attr == CLEAR_PIN) {
      return (V) clearPin;
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
      if (addrBits == newAddr) return;
      addrBits = newAddr;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      final var newData = (BitWidth) value;
      if (dataBits == newData) return;
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
      if (label.equals(newLabel)) return;
      @SuppressWarnings("unchecked")
      V oldlabel = (V) label;
      label = newLabel;
      fireAttributeValueChanged(attr, value, oldlabel);
    } else if (attr == StdAttr.TRIGGER) {
      final var newTrigger = (AttributeOption) value;
      if (trigger.equals(newTrigger)) return;
      trigger = newTrigger;
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
      if (!readWriteBehavior.equals(val)) {
        readWriteBehavior = val;
        fireAttributeValueChanged(attr, value, null);
      }
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
      if (byteEnables.equals(newBE)) return;
      if (dataBits.getWidth() < 9) newBE = BUS_WITHOUT_BYTE_ENABLES;
      byteEnables = newBE;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == CLEAR_PIN) {
      final var val = (Boolean) value;
      if (clearPin != val) {
        clearPin = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == Mem.ALLOW_MISALIGNED) {
      final var val = (Boolean) value;
      if (allowMisaligned != val) {
        allowMisaligned = val;
        fireAttributeValueChanged(attr, value, null);
      }
    } else if (attr == StdAttr.APPEARANCE) {
      final var option = (AttributeOption) value;
      if (appearance.equals(option)) return;
      appearance = option;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
