/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.vga;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VgaAttributes extends AbstractAttributeSet {

  private static class VgaStateAttribute extends Attribute<VgaState> {
    @Override
    public VgaState parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static final int MODE_160_120 = 0;
  public static final int MODE_320_240 = 1;
  public static final int MODE_640_480 = 2;
  public static final int MODE_800_600 = 3;
  public static final int MODE_1024_768 = 4;

  public static final int MODE_160_120_MASK = 1;
  public static final int MODE_320_240_MASK = 2;
  public static final int MODE_640_480_MASK = 4;
  public static final int MODE_800_600_MASK = 8;
  public static final int MODE_1024_768_MASK = 16;

  public static final AttributeOption OPT_160_120 = new AttributeOption("160x120", S.fixedString("160x120"));
  public static final AttributeOption OPT_320_240 = new AttributeOption("320x240", S.fixedString("320x240"));
  public static final AttributeOption OPT_640_480 = new AttributeOption("640x480", S.fixedString("640x480"));
  public static final AttributeOption OPT_800_600 = new AttributeOption("800x600", S.fixedString("800x600"));
  public static final AttributeOption OPT_1024_768 = new AttributeOption("1024x768", S.fixedString("1024x768"));
  public static final AttributeOption[] MODE_ARRAY = new AttributeOption[] {OPT_160_120, OPT_320_240, OPT_640_480, OPT_800_600, OPT_1024_768};

  public static final Attribute<VgaState> VGA_STATE = new VgaStateAttribute();
  public static final Attribute<Integer> START_ADDRESS = Attributes.forHexInteger("StartAddress", S.getter("VgaStartAddress"));
  public static final Attribute<Integer> BUFFER_ADDRESS = Attributes.forHexInteger("BufferAddress", S.getter("VgaBufferAddress"));
  public static final Attribute<AttributeOption> MODE = Attributes.forOption("DisplayMode", S.getter("VgaInitialDisplayMode"), MODE_ARRAY);
  public static final Attribute<Boolean> SOFT_160_120 = Attributes.forBoolean("soft160x120", S.getter("VgaSoftMode", "160x120"));
  public static final Attribute<Boolean> SOFT_320_240 = Attributes.forBoolean("soft320x240", S.getter("VgaSoftMode", "320x240"));
  public static final Attribute<Boolean> SOFT_640_480 = Attributes.forBoolean("soft640x480", S.getter("VgaSoftMode", "640x480"));
  public static final Attribute<Boolean> SOFT_800_600 = Attributes.forBoolean("soft800x600", S.getter("VgaSoftMode", "800x600"));
  public static final Attribute<Boolean> SOFT_1024_768 = Attributes.forBoolean("soft1024x768", S.getter("VgaSoftMode", "1024x768"));

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private VgaState state = new VgaState();

  @SuppressWarnings("serial")
  public static final List<AttributeOption> MODES = new ArrayList<>() {{
      this.addAll(Arrays.asList(MODE_ARRAY));
    }
  };

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          START_ADDRESS,
          MODE,
          BUFFER_ADDRESS,
          SOFT_160_120,
          SOFT_320_240,
          SOFT_640_480,
          SOFT_800_600,
          SOFT_1024_768,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          SocSimulationManager.SOC_BUS_SELECT,
          VGA_STATE);

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var d = (VgaAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.state = new VgaState();
    state.copyInto(d.state);
  }

  public static int getModeIndex(AttributeOption mode) {
    if (!MODES.contains(mode))
      return 0;
    else
      return MODES.indexOf(mode);
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == VGA_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != VGA_STATE;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == START_ADDRESS) return (V) state.getStartAddress();
    if (attr == MODE) return (V) state.getInitialMode();
    if (attr == BUFFER_ADDRESS) return (V) state.getVgaBufferStartAddress();
    if (attr == StdAttr.LABEL) return (V) state.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) state.getBusInfo();
    if (attr == VGA_STATE) return (V) state;
    if (attr == SOFT_160_120) return (V) state.getSoft160x120();
    if (attr == SOFT_320_240) return (V) state.getSoft320x240();
    if (attr == SOFT_640_480) return (V) state.getSoft640x480();
    if (attr == SOFT_800_600) return (V) state.getSoft800x600();
    if (attr == SOFT_1024_768) return (V) state.getSoft1024x768();
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    final V oldValue = getValue(attr);
    if (attr == START_ADDRESS) {
      if (state.setStartAddress((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == MODE) {
      if (state.setInitialMode((AttributeOption) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == BUFFER_ADDRESS) {
      if (state.setVgaBufferStartAddress((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL) {
      if (state.setLabel((String) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL_FONT) {
      Font f = (Font) value;
      if (!f.equals(labelFont)) {
        labelFont = f;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      final var b = (Boolean) value;
      if (b != labelVisible) {
        labelVisible = b;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (state.setBusInfo((SocBusInfo) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SOFT_160_120) {
      if (state.setSoft160x120((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SOFT_320_240) {
      if (state.setSoft320x240((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SOFT_640_480) {
      if (state.setSoft640x480((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SOFT_800_600) {
      if (state.setSoft800x600((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SOFT_1024_768) {
      if (state.setSoft1024x768((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
  }
}
