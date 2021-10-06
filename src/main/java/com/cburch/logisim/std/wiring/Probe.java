/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorBooleanConvert;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class Probe extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Probe";

  public static class ProbeLogger extends InstanceLogger {
    public ProbeLogger() {}

    @Override
    public String getLogName(InstanceState state, Object option) {
      String ret = state.getAttributeValue(StdAttr.LABEL);
      return ret != null && !ret.equals("") ? ret : null;
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      ProbeAttributes attrs = (ProbeAttributes) state.getAttributeSet();
      return attrs.width;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      return getValue(state);
    }
  }

  private static class StateData implements InstanceData, Cloneable {
    Value curValue = Value.NIL;

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  public static Bounds getOffsetBounds(
      Direction dir, BitWidth width, RadixOption radix, boolean NewLayout, boolean IsPin) {
    int len =
        radix == null || radix == RadixOption.RADIX_2
            ? width.getWidth()
            : radix.getMaxLength(width);
    int bwidth, bheight, x, y;
    if (radix == RadixOption.RADIX_2) {
      int maxBitsPerRow = 8;
      int maxRows = 8;
      int rows = len / maxBitsPerRow;
      if (len > rows * maxBitsPerRow) rows++;
      bwidth = (len < 2) ? 20 : (len >= maxBitsPerRow) ? maxBitsPerRow * 10 : len * 10;
      bheight = (rows < 2) ? 20 : (rows >= maxRows) ? maxRows * 20 : rows * 20;
    } else {
      if (len < 2) bwidth = 20;
      else bwidth = len * Pin.DIGIT_WIDTH;
      bheight = 20;
    }
    if (NewLayout) bwidth += (len == 1) ? 20 : 25;
    bwidth = ((bwidth + 9) / 10) * 10;
    if (dir == Direction.EAST) {
      x = -bwidth;
      y = -(bheight / 2);
    } else if (dir == Direction.WEST) {
      x = 0;
      y = -(bheight / 2);
    } else if (dir == Direction.SOUTH) {
      if (NewLayout && IsPin) {
        x = bwidth;
        bwidth = bheight;
        bheight = x;
      }
      x = -(bwidth / 2);
      y = -bheight;
    } else {
      if (NewLayout && IsPin) {
        x = bwidth;
        bwidth = bheight;
        bheight = x;
      }
      x = -(bwidth / 2);
      y = 0;
    }
    return Bounds.create(x, y, bwidth, bheight);
  }

  private static Value getValue(InstanceState state) {
    StateData data = (StateData) state.getData();
    return data == null ? Value.NIL : data.curValue;
  }

  static void paintValue(InstancePainter painter, Value value) {
    if (painter.getAttributeValue(ProbeAttributes.PROBEAPPEARANCE)
        == ProbeAttributes.APPEAR_EVOLUTION_NEW) paintValue(painter, value, false);
    else paintOldStyleValue(painter, value);
  }

  static void paintOldStyleValue(InstancePainter painter, Value value) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds(); // intentionally with no graphics
    // object - we don't want label
    // included

    RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
    if (radix == null || radix == RadixOption.RADIX_2) {
      int x = bds.getX();
      int y = bds.getY();
      int wid = value.getWidth();
      if (wid == 0) {
        x += bds.getWidth() / 2;
        y += bds.getHeight() / 2;
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(x - 4, y, x + 4, y);
        return;
      }
      int x0 = bds.getX() + bds.getWidth() - 5;
      int compWidth = wid * 10;
      if (compWidth < bds.getWidth() - 3) {
        x0 = bds.getX() + (bds.getWidth() + compWidth) / 2 - 5;
      }
      int cx = x0;
      int cy = bds.getY() + bds.getHeight() - 10;
      int cur = 0;
      for (int k = 0; k < wid; k++) {
        GraphicsUtil.drawCenteredText(g, value.get(k).toDisplayString(), cx, cy);
        ++cur;
        if (cur == 8) {
          cur = 0;
          cx = x0;
          cy -= 14;
        } else {
          cx -= 10;
        }
      }
    } else {
      String text = radix.toString(value);
      GraphicsUtil.drawCenteredText(
          g, text, bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2 - 2);
    }
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return attr.equals(ProbeAttributes.PROBEAPPEARANCE)
        ? ProbeAttributes.getDefaultProbeAppearance()
        : super.getDefaultAttributeValue(attr, ver);
  }

  static void paintValue(InstancePainter painter, Value value, boolean colored) {
    Graphics g = painter.getGraphics();
    Graphics2D g2 = (Graphics2D) g;
    Bounds bds = painter.getBounds(); // intentionally with no graphics
    // object - we don't want label
    // included
    g.setFont(Pin.DEFAULT_FONT);
    boolean IsOutput =
        (painter.getAttributeSet().containsAttribute(Pin.ATTR_TYPE))
            ? painter.getAttributeValue(Pin.ATTR_TYPE)
            : false;
    if (painter.getAttributeValue(ProbeAttributes.PROBEAPPEARANCE)
        != ProbeAttributes.APPEAR_EVOLUTION_NEW) {
      if (colored) {
        int x = bds.getX();
        int y = bds.getY();
        if (!IsOutput) {
          g.setColor(value.get(0).getColor());
          g.fillOval(x + 5, y + 4, 11, 13);
        }
        if (!IsOutput) g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(g, value.get(0).toDisplayString(), x + 10, y + 9);
      } else paintOldStyleValue(painter, value);
      return;
    }
    RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
    int LabelValueXOffset = 15;
    if (radix != null && radix != RadixOption.RADIX_2) LabelValueXOffset += 3;
    g.setColor(Color.BLUE);
    g2.scale(0.7, 0.7);
    g2.drawString(
        radix.getIndexChar(),
        (int) ((bds.getX() + bds.getWidth() - LabelValueXOffset) / 0.7),
        (int) ((bds.getY() + bds.getHeight() - 2) / 0.7));
    g2.scale(1.0 / 0.7, 1.0 / 0.7);
    g.setColor(Color.BLACK);
    if (radix == null || radix == RadixOption.RADIX_2) {
      int x = bds.getX();
      int y = bds.getY();
      int wid = value.getWidth();
      if (wid == 0) {
        x += bds.getWidth() / 2;
        y += bds.getHeight() / 2;
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(x - 4, y, x + 4, y);
        return;
      }
      int yoffset = 12;
      int x0 = bds.getX() + bds.getWidth() - 20;
      int cx = x0;
      int cy = bds.getY() + bds.getHeight() - yoffset;
      int cur = 0;
      for (int k = 0; k < wid; k++) {
        GraphicsUtil.drawCenteredText(g, value.get(k).toDisplayString(), cx, cy);
        ++cur;
        if (cur == 8) {
          cur = 0;
          cx = x0;
          cy -= 20;
        } else {
          cx -= 10;
        }
      }
    } else {
      String text = radix.toString(value);
      int cx = bds.getX() + bds.getWidth() - LabelValueXOffset - 2;
      for (int k = text.length() - 1; k >= 0; k--) {
        GraphicsUtil.drawText(
            g, text.substring(k, k + 1), cx, bds.getY() + bds.getHeight() / 2 - 1, GraphicsUtil.H_RIGHT, GraphicsUtil.H_CENTER);
        cx -= Pin.DIGIT_WIDTH;
      }
    }
  }

  public static final Probe FACTORY = new Probe();

  public Probe() {
    super(_ID, S.getter("probeComponent"));
    setIconName("probe.gif");
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setFacingAttribute(StdAttr.FACING);
    setInstanceLogger(ProbeLogger.class);
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.setPorts(new Port[] {new Port(0, 0, Port.INPUT, BitWidth.UNKNOWN)});
    instance.addAttributeListener();
    ((PrefMonitorBooleanConvert) AppPreferences.NEW_INPUT_OUTPUT_SHAPES)
        .addConvertListener((ProbeAttributes) instance.getAttributeSet());
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  @Override
  public AttributeSet createAttributeSet() {
    AttributeSet attrs = new ProbeAttributes();
    attrs.setValue(ProbeAttributes.PROBEAPPEARANCE, ProbeAttributes.getDefaultProbeAppearance());
    return attrs;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    ProbeAttributes attrs = (ProbeAttributes) attrsBase;
    return getOffsetBounds(
        attrs.facing,
        attrs.width,
        attrs.radix,
        attrs.appearance == ProbeAttributes.APPEAR_EVOLUTION_NEW,
        false);
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return true;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.FACING
        || attr == RadixOption.ATTRIBUTE
        || attr == ProbeAttributes.PROBEAPPEARANCE) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  //
  // graphics methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getOffsetBounds();
    g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 1, bds.getHeight() - 1);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Value value = getValue(painter);

    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds(); // intentionally with no graphics
    // object - we don't want label
    // included
    int x = bds.getX();
    int y = bds.getY();
    Color back = new Color(0xff, 0xf0, 0x99);
    if (value.getWidth() <= 1) {
      g.setColor(back);
      g.fillOval(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2);
      g.setColor(Color.lightGray);
      g.drawOval(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2);
    } else {
      g.setColor(back);
      g.fillRoundRect(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2, 20, 20);
      g.setColor(Color.lightGray);
      g.drawRoundRect(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2, 20, 20);
    }

    g.setColor(Color.GRAY);
    painter.drawLabel();
    g.setColor(Color.DARK_GRAY);

    if (!painter.getShowState()) {
      if (value.getWidth() > 0) {
        GraphicsUtil.drawCenteredText(
            g,
            "x" + value.getWidth(),
            bds.getX() + bds.getWidth() / 2,
            bds.getY() + bds.getHeight() / 2);
      }
    } else {
      paintValue(painter, value);
    }

    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    StateData oldData = (StateData) state.getData();
    Value oldValue = oldData == null ? Value.NIL : oldData.curValue;
    Value newValue = state.getPortValue(0);
    boolean same = Objects.equals(oldValue, newValue);
    if (!same) {
      if (oldData == null) {
        oldData = new StateData();
        oldData.curValue = newValue;
        state.setData(oldData);
      } else {
        oldData.curValue = newValue;
      }
      int oldWidth = oldValue == null ? 1 : oldValue.getBitWidth().getWidth();
      int newWidth = newValue.getBitWidth().getWidth();
      if (oldWidth != newWidth) {
        ProbeAttributes attrs = (ProbeAttributes) state.getAttributeSet();
        attrs.width = newValue.getBitWidth();
        state.getInstance().recomputeBounds();
        state.getInstance().computeLabelTextField(Instance.AVOID_LEFT);
      }
    }
  }
}
