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
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.WeakHashMap;

public class EepromAttributes extends AbstractAttributeSet {

  static HexFrame getHexFrame(MemContents value, Project proj, Instance instance) {
    synchronized (windowRegistry) {
      HexFrame ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, instance, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  static void closeHexFrame(MemContents value) {
    HexFrame ret;
    synchronized (windowRegistry) {
      ret = windowRegistry.remove(value);
    }
    if (ret != null) ret.closeAndDispose();
  }

  static void register(MemContents value, Project proj) {
    if (proj == null || listenerRegistry.containsKey(value)) {
      return;
    }
    final var l = new EepromContentsListener(proj);
    value.addHexModelListener(l);
    listenerRegistry.put(value, l);
  }

  private final ArrayList<Attribute<?>> myAttributes = new ArrayList<>();

  private static final WeakHashMap<MemContents, EepromContentsListener> listenerRegistry =
      new WeakHashMap<>();

  private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();

  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private MemContents contents;
  private String label = "";
  private AttributeOption trigger = StdAttr.TRIG_RISING;
  private AttributeOption busStyle = RamAttributes.BUS_SEP;
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;
  private AttributeOption byteEnables = RamAttributes.BUS_WITHOUT_BYTE_ENABLES;
  private Boolean asynchronousRead = false;
  private AttributeOption appearance = AppPreferences.getDefaultAppearance();
  private AttributeOption readWriteBehavior = Mem.READAFTERWRITE;
  private Boolean clearPin = false;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private AttributeOption typeOfEnables = Mem.USEBYTEENABLES;

  EepromAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth(), false);
    updateAttributes();
  }

  public boolean updateAttributes() {
    final var newList = new ArrayList<Attribute<?>>();
    var changes = false;
    newList.add(Mem.ADDR_ATTR);
    newList.add(Mem.DATA_ATTR);
    newList.add(Mem.ENABLES_ATTR);
    newList.add(RamAttributes.CLEAR_PIN);
    newList.add(Eeprom.CONTENTS_ATTR);

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
          changes |= !myAttributes.contains(RamAttributes.ATTR_ByteEnables);
          newList.add(RamAttributes.ATTR_ByteEnables);
        } else changes |= myAttributes.contains(RamAttributes.ATTR_ByteEnables);
      } else changes |= myAttributes.contains(Mem.ASYNC_READ);
      newList.add(RamAttributes.ATTR_DBUS);
      changes |= myAttributes.contains(Mem.LINE_ATTR);
      changes |= myAttributes.contains(Mem.ALLOW_MISALIGNED);
    } else {
      newList.add(Mem.LINE_ATTR);
      newList.add(Mem.ALLOW_MISALIGNED);
      newList.add(StdAttr.TRIGGER);
      newList.add(RamAttributes.ATTR_DBUS);
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
    final var d = (EepromAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.contents = contents.clone();
    d.trigger = trigger;
    d.busStyle = busStyle;
    d.labelFont = labelFont;
    d.appearance = appearance;
    d.byteEnables = byteEnables;
    d.asynchronousRead = asynchronousRead;
    d.readWriteBehavior = readWriteBehavior;
    d.clearPin = clearPin;
    d.lineSize = lineSize;
    d.allowMisaligned = allowMisaligned;
    d.typeOfEnables = typeOfEnables;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return myAttributes;
  }

  void setProject(Project proj) {
    register(contents, proj);
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
    if (attr == Eeprom.CONTENTS_ATTR) {
      return (V) contents;
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
    if (attr == RamAttributes.ATTR_DBUS) {
      return (V) busStyle;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      return (V) labelVisible;
    }
    if (attr == RamAttributes.ATTR_ByteEnables) {
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
    if (attr == RamAttributes.CLEAR_PIN) {
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
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      final var newData = (BitWidth) value;
      if (dataBits == newData) return;
      dataBits = newData;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
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
    } else if (attr == Eeprom.CONTENTS_ATTR) {
      final var newContents = (MemContents) value;
      if (!contents.equals(newContents)) {
        contents = newContents;
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
    } else if (attr == RamAttributes.ATTR_DBUS) {
      final var newStyle = (AttributeOption) value;
      if (busStyle.equals(newStyle)) return;
      busStyle = newStyle;
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
    } else if (attr == RamAttributes.ATTR_ByteEnables) {
      var newBE = (AttributeOption) value;
      if (byteEnables.equals(newBE)) return;
      if (dataBits.getWidth() < 9) newBE = RamAttributes.BUS_WITHOUT_BYTE_ENABLES;
      byteEnables = newBE;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == RamAttributes.CLEAR_PIN) {
      final var val = (Boolean) value;
      if (clearPin != val) {
        clearPin = val;
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
      final var option = (AttributeOption) value;
      if (appearance.equals(option)) return;
      appearance = option;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
