/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZonedDateTime;

import javax.swing.Timer;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import static com.cburch.logisim.std.Strings.S;
import com.cburch.logisim.util.GraphicsUtil;

public class RealTimeClock extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "RealTimeClock";

  private static final AttributeOption SIZE_MEDIUM =
      new AttributeOption(1, S.getter("porSizeMedium"));
  private static final AttributeOption SIZE_NARROW =
      new AttributeOption(2, S.getter("porSizeNarrow"));
  private static final Attribute<AttributeOption> TIME_SIZE =
      Attributes.forOption(
          "porsize", S.getter("PorSize"), new AttributeOption[] {SIZE_MEDIUM, SIZE_NARROW});

  private static final AttributeOption TYPE_EPOCH_MILLI =
      new AttributeOption(1, S.getter("realTimeClockEpochMilliseconds"));
  private static final AttributeOption TYPE_EPOCH_SEC =
      new AttributeOption(2, S.getter("realTimeClockEpochSeconds"));
  private static final Attribute<AttributeOption> TIME_TYPE =
          Attributes.forOption(
              "timeType", S.getter("realTimeClockTimeType"), new AttributeOption[] {TYPE_EPOCH_MILLI, TYPE_EPOCH_SEC});

  public static final RealTimeClock FACTORY = new RealTimeClock();

  public RealTimeClock() {
    super(_ID, S.getter("realTimeClockComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TIME_TYPE,
          TIME_SIZE,
        },
        new Object[] {
          Direction.EAST,
          TYPE_EPOCH_MILLI,
          SIZE_MEDIUM,
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("realtimeclock.gif");
  }

  private static class TimeState implements InstanceData, Cloneable, ActionListener {

    private final Timer timer;
    private InstanceComponent component;
    private Simulator simulator;

    public TimeState(InstanceState state) {
      component = state.getInstance().getComponent();;
      simulator = state.getProject().getSimulator();
      timer = new Timer(10, this);
      timer.start();
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      component.fireInvalidated();
      if (simulator != null) simulator.nudge();
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();

    var timeType = instance.getAttributeValue(TIME_TYPE);
    var port = new Port[] {new Port(0, 0, Port.OUTPUT, BitWidth.create(64))};
    port[0].setToolTip(timeType == TYPE_EPOCH_MILLI ?
      S.getter("realTimeClockMilliseconds"): S.getter("realTimeClockSeconds"));
    instance.setPorts(port);
    instance.recomputeBounds();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);

    final var psize = attrs.getValue(TIME_SIZE);
    if (psize == SIZE_MEDIUM) {
      return Bounds.create(0, -20, 40, 40).rotate(Direction.WEST, facing, 0, 0);
    }
    return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == TIME_SIZE) {
      instance.recomputeBounds();
    }
    else if (attr == TIME_TYPE || attr == StdAttr.FACING){
      var timeType = instance.getAttributeValue(TIME_TYPE);
      var port = new Port[] {new Port(0, 0, Port.OUTPUT, BitWidth.create(64))};
      port[0].setToolTip(timeType == TYPE_EPOCH_MILLI ?
        S.getter("realTimeClockMilliseconds"): S.getter("realTimeClockSeconds"));
      instance.setPorts(port);
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    java.awt.Graphics g = painter.getGraphics();
    Bounds bds = painter.getInstance().getBounds();
    int x = bds.getX();
    int y = bds.getY();
    int width =  bds.getWidth();
    int height = bds.getHeight();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.WHITE);
    g.fillRect(x, y, width, height);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRect(x, y, width, height);

    final var psize = painter.getAttributeValue(TIME_SIZE);

    int offset;

    Font old = g.getFont();
    if  (psize == SIZE_NARROW) {
      g.setFont(old.deriveFont(6.0f).deriveFont(Font.BOLD));
      offset = 7;
    } else {
      g.setFont(old.deriveFont(14.0f).deriveFont(Font.BOLD));
      offset = 15;
    }

    int circleDiameter = height - 4 - offset;
    int xCenter = x + (width - circleDiameter) / 2;
    int yCenter = y + offset + 1;

    Graphics2D g2 = (Graphics2D) g;
    var oldStroke = g2.getStroke();
    g.setColor(Color.darkGray);
    g2.setStroke(new BasicStroke(1.5f));
    g.drawArc(xCenter, yCenter, circleDiameter, circleDiameter, 0, 360);

    xCenter = xCenter + circleDiameter/2;
    yCenter = yCenter + circleDiameter/2;
    int x1 = xCenter - (int)(circleDiameter * 0.24f);
    int y1 = yCenter - (int)(circleDiameter * 0.24f);
    int x2 = xCenter + (int)(circleDiameter * 0.18f);
    int y2 = yCenter - (int)(circleDiameter * 0.18f);

    g2.setStroke(new BasicStroke(1f));
    g.drawLine(xCenter, yCenter, x1, y1);
    g.drawLine(xCenter, yCenter, x2, y2);

    g2.setStroke(oldStroke);
    g.setColor(Color.BLACK);
    String txt = S.get("Time");
    g.drawString(txt, x + 2, y + offset - 1);
    painter.drawPorts();
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    TimeState timeState = (TimeState) state.getData();
    if (timeState == null) {
      timeState = new TimeState(state);
      state.setData(timeState);
    }

    var timeType = state.getAttributeValue(TIME_TYPE);
    Value timeValue;

    if(timeType == TYPE_EPOCH_MILLI){
      timeValue = Value.createKnown(BitWidth.create(64), ZonedDateTime.now().toInstant().toEpochMilli());
      state.setPort(0, timeValue, 0);
    }
    else {
      timeValue = Value.createKnown(BitWidth.create(64), ZonedDateTime.now().toInstant().getEpochSecond());
      state.setPort(0, timeValue, 0);
    }
  }
}
