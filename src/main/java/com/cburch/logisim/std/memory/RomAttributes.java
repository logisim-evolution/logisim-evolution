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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class RomAttributes extends AbstractAttributeSet {

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
    final var l = new RomContentsListener(proj);
    value.addHexModelListener(l);
    listenerRegistry.put(value, l);
  }

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          Mem.ADDR_ATTR,
          Mem.DATA_ATTR,
          Mem.LINE_ATTR,
          Mem.ALLOW_MISALIGNED,
          Rom.CONTENTS_ATTR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.APPEARANCE);

  private static final WeakHashMap<MemContents, RomContentsListener> listenerRegistry =
      new WeakHashMap<>();

  private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();
  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private MemContents contents;
  private AttributeOption lineSize = Mem.SINGLE;
  private Boolean allowMisaligned = false;
  private String label = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = false;
  private AttributeOption appearance = AppPreferences.getDefaultAppearance();

  RomAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth(), false);
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RomAttributes d = (RomAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.lineSize = lineSize;
    d.allowMisaligned = allowMisaligned;
    d.contents = contents.clone();
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.appearance = appearance;
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
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == Mem.ALLOW_MISALIGNED) {
      return (V) allowMisaligned;
    }
    if (attr == Rom.CONTENTS_ATTR) {
      return (V) contents;
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
      return (V) appearance;
    }
    return null;
  }

  void setProject(Project proj) {
    register(contents, proj);
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      final var newAddr = (BitWidth) value;
      if (newAddr == addrBits) return;
      addrBits = newAddr;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.DATA_ATTR) {
      final var newData = (BitWidth) value;
      if (newData == dataBits) return;
      dataBits = newData;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.LINE_ATTR) {
      final var val = (AttributeOption) value;
      if (lineSize.equals(val)) return;
      lineSize = val;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Mem.ALLOW_MISALIGNED) {
      final var val = (Boolean) value;
      if (allowMisaligned.equals(val)) return;
      allowMisaligned = val;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == Rom.CONTENTS_ATTR) {
      final var newContents = (MemContents) value;
      if (contents.equals(newContents)) return;
      contents = newContents;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL) {
      final var newLabel = (String) value;
      if (label.equals(newLabel)) return;
      @SuppressWarnings("unchecked")
      V oldLabel = (V) label;
      label = newLabel;
      fireAttributeValueChanged(attr, value, oldLabel);
    } else if (attr == StdAttr.LABEL_FONT) {
      final var newFont = (Font) value;
      if (labelFont.equals(newFont)) return;
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      final var newVis = (Boolean) value;
      if (labelVisible.equals(newVis)) return;
      labelVisible = newVis;
      fireAttributeValueChanged(attr, value, null);
    } else if (attr == StdAttr.APPEARANCE) {
      final var newAppearance = (AttributeOption) value;
      if (appearance.equals(newAppearance)) return;
      appearance = newAppearance;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
