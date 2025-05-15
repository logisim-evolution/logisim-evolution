/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.jtaguart;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class JtagUart extends SocInstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "SocJtagUart";

  public static final int CLOCK_PIN = 0;
  public static final int RESET_PIN = 1;
  public static final int IRQ_PIN = 2;
  public static final int READ_ENABLE_PIN = 3;
  public static final int CLEAR_KEYBOARD_PIN = 4;
  public static final int AVAILABLE_PIN = 5;
  public static final int DATA_IN_PIN = 6;
  public static final int DATA_OUT_PIN = 7;
  public static final int WRITE_PIN = 8;
  public static final int CLEAR_TTY_PIN = 9;

  public JtagUart() {
    super(_ID, S.getter("SocJtagUartComponent"), SOC_SLAVE);
    setIcon(new ArithmeticIcon("JtagUart", 4));
    setOffsetBounds(Bounds.create(0, 0, 300, 60));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new JtagUartAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    Bounds bds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() + 2,
        bds.getY() + bds.getHeight() / 2,
        GraphicsUtil.H_LEFT,
        GraphicsUtil.V_CENTER);
    Port[] ps = new Port[10];
    ps[CLOCK_PIN] = new Port(0, 50, Port.INPUT, 1);
    ps[CLOCK_PIN].setToolTip(S.getter("Rv32imClockInput"));
    ps[RESET_PIN] = new Port(0, 30, Port.INPUT, 1);
    ps[RESET_PIN].setToolTip(S.getter("Rv32imResetInput"));
    ps[IRQ_PIN] = new Port(300, 50, Port.OUTPUT, 1);
    ps[IRQ_PIN].setToolTip(S.getter("SocPioIrqOutput"));
    ps[READ_ENABLE_PIN] = new Port(10, 0, Port.OUTPUT, 1);
    ps[READ_ENABLE_PIN].setToolTip(S.getter("JtagUartKeybReadEnable"));
    ps[CLEAR_KEYBOARD_PIN] = new Port(20, 0, Port.OUTPUT, 1);
    ps[CLEAR_KEYBOARD_PIN].setToolTip(S.getter("JtagUartClearKeyb"));
    ps[AVAILABLE_PIN] = new Port(130, 0, Port.INPUT, 1);
    ps[AVAILABLE_PIN].setToolTip(S.getter("JtagUartKeybAvailable"));
    ps[DATA_IN_PIN] = new Port(140, 0, Port.INPUT, 7);
    ps[DATA_IN_PIN].setToolTip(S.getter("JtagUartKeybData"));
    ps[DATA_OUT_PIN] = new Port(160, 0, Port.OUTPUT, 7);
    ps[DATA_OUT_PIN].setToolTip(S.getter("JtagUartTtyData"));
    ps[WRITE_PIN] = new Port(180, 0, Port.OUTPUT, 1);
    ps[WRITE_PIN].setToolTip(S.getter("JtagUartTtyWrite"));
    ps[CLEAR_TTY_PIN] = new Port(190, 0, Port.OUTPUT, 1);
    ps[CLEAR_TTY_PIN].setToolTip(S.getter("JtagUartTtyClear"));
    instance.setPorts(ps);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    } else super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    g2.drawLine(loc.getX(), loc.getY() + 20, loc.getX() + 200, loc.getY() + 20);
    g2.drawLine(loc.getX() + 150, loc.getY(), loc.getX() + 150, loc.getY() + 20);
    g2.drawLine(loc.getX() + 200, loc.getY(), loc.getX() + 200, loc.getY() + 20);
    painter.drawPort(RESET_PIN, "Reset", Direction.EAST);
    painter.drawClock(CLOCK_PIN, Direction.EAST);
    painter.drawPort(IRQ_PIN, "IRQ", Direction.WEST);
    for (int i = READ_ENABLE_PIN; i < 10; i++) painter.drawPort(i);
    GraphicsUtil.drawCenteredText(g2, "Keyboard", loc.getX() + 75, loc.getY() + 10);
    GraphicsUtil.drawCenteredText(g2, "TTY", loc.getX() + 175, loc.getY() + 10);
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "Jtag Uart", loc.getX() + 250, loc.getY() + 10);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter
        .getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(g2, Bounds.create(loc.getX() + 40, loc.getY() + 41, 220, 18));
  }

  @Override
  public void propagate(InstanceState state) {
    JtagUartState myState = state.getAttributeValue(JtagUartAttributes.JTAG_STATE);
    myState.handleOperations(state);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(JtagUartAttributes.JTAG_STATE);
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
