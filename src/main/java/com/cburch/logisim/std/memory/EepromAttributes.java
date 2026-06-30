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

public class EepromAttributes extends RamAttributes {

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
    final var l = new RomContentsListener(proj, S.get("eepromChangeAction"));
    value.addHexModelListener(l);
    listenerRegistry.put(value, l);
  }

  private static final WeakHashMap<MemContents, RomContentsListener> listenerRegistry =
      new WeakHashMap<>();

  private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();
  private MemContents contents;

  EepromAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth(), false);
    updateAttributes();
  }

  @Override
  public boolean updateAttributes() {
    boolean changes = super.updateAttributes();
    if (changes) {
      myAttributes.add(Eeprom.CONTENTS_ATTR);
    }
    return changes;
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    super.copyInto(dest);
    final var d = (EepromAttributes) dest;
    d.contents = contents.clone();
  }

  void setProject(Project proj) {
    register(contents, proj);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Eeprom.CONTENTS_ATTR) {
      return (V) contents;
    }
    return super.getValue(attr);
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    // must handle ADDR_ATTR and DATA_ATTR here to call contents.setDimensions
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
    } else if (attr == Eeprom.CONTENTS_ATTR) {
      final var newContents = (MemContents) value;
      if (!contents.equals(newContents)) {
        contents = newContents;
        fireAttributeValueChanged(attr, value, null);
      }
    } else {
      super.setValue(attr, value);
    }
  }
}
