/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class SocBus extends SocInstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "SocBus";

  public static final SocBusMenuProvider MENU_PROVIDER = new SocBusMenuProvider();

  public SocBus() {
    super(_ID, S.getter("SocBusComponent"), SOC_BUS);
    setIcon(new ArithmeticIcon("SOCBus", 3));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new SocBusAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(
        0,
        0,
        640,
        (attrs.getValue(SocBusAttributes.NrOfTracesAttr).getWidth() + 1)
            * SocBusStateInfo.TRACE_HEIGHT);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if (attr.equals(SocBusAttributes.NrOfTracesAttr)) {
      instance.recomputeBounds();
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    final var ps = new Port[1];
    ps[0] = new Port(0, 10, Port.INPUT, 1);
    ps[0].setToolTip(S.getter("Rv32imResetInput"));
    instance.setPorts(ps);
    final var bds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BASELINE);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g2 = (Graphics2D) painter.getGraphics();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    painter.drawPort(0, "Reset", Direction.EAST);
    final var loc = painter.getLocation();
    final var font = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "SOC Bus Interconnect", loc.getX() + 320, loc.getY() + 10);
    g2.setFont(font);
    if (painter.isPrintView()) return;
    var socBusInfo = painter.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    var socBusStateInfo = socBusInfo.getSocSimulationManager().getSocBusState(socBusInfo.getBusId());
    if (socBusStateInfo != null)
      socBusStateInfo.paint(
          loc.getX(),
          loc.getY(),
          g2,
          painter.getInstance(),
          painter.getAttributeValue(SocBusAttributes.SOC_TRACE_VISIBLE),
          painter.getData());
  }

  @Override
  public void propagate(InstanceState state) {
    SocBusInfo info = state.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    SocBusStateInfo data = info.getSocSimulationManager().getSocBusState(info.getBusId());
    SocBusStateInfo.SocBusState dat = (SocBusStateInfo.SocBusState) state.getData();
    if (dat == null) {
      state.setData(data.getNewState(state.getInstance()));
    } else {
      if (state.getPortValue(0) == Value.TRUE) {
        dat.clear();
      }
    }
  }

  @Override
  public boolean providesSubCircuitMenu() {
    return true;
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return MENU_PROVIDER.getMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) {
    return null;
  }
}
