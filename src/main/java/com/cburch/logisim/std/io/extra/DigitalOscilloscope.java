/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class DigitalOscilloscope extends InstanceFactory {

  private static final Attribute<Integer> ATTR_INPUTS =
      Attributes.forIntegerRange("inputs", S.getter("gateInputsAttr"), 1, 32);

  private static final Attribute<Integer> ATTR_NSTATE =
      Attributes.forIntegerRange("nState", S.getter("NStateAttr"), 4, 35);

  private static final AttributeOption NO = new AttributeOption("no", S.getter("noOption"));
  private static final AttributeOption TRIG_RISING =
      new AttributeOption("rising", S.getter("stdTriggerRising"));
  private static final AttributeOption TRIG_FALLING =
      new AttributeOption("falling", S.getter("stdTriggerFalling"));
  private static final AttributeOption BOTH = new AttributeOption("both", S.getter("bothOption"));

  private static final Attribute<AttributeOption> VERT_LINE =
      Attributes.forOption(
          "frontlines",
          S.getter("DrawClockFrontLine"),
          new AttributeOption[] {NO, TRIG_RISING, TRIG_FALLING, BOTH});

  private static final Attribute<Boolean> SHOW_CLOCK =
      Attributes.forBoolean("showclock", S.getter("ShowClockAttribute"));

  private static final Attribute<Color> ATTR_COLOR =
      Attributes.forColor("color", S.getter("BorderColor"));

  private final int border = 10;

  public DigitalOscilloscope() {
    super("Digital Oscilloscope", S.getter("DigitalOscilloscopeComponent"));
    setAttributes(
        new Attribute<?>[] {
          ATTR_INPUTS,
          ATTR_NSTATE,
          VERT_LINE,
          SHOW_CLOCK,
          ATTR_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT
        },
        new Object[] {
          3,
          10,
          TRIG_RISING,
          true,
          new Color(0, 208, 208),
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT
        });
    setIconName("digitaloscilloscope.gif");
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_LEFT);
    updateports(instance);
  }

  private DiagramState getDiagramState(InstanceState state) {
    byte inputs = (byte) (state.getAttributeValue(ATTR_INPUTS).byteValue() + 1);
    byte length = (byte) (state.getAttributeValue(ATTR_NSTATE).byteValue() * 2);
    DiagramState ret = (DiagramState) state.getData();
    if (ret == null) {
      ret = new DiagramState(inputs, length);
      state.setData(ret);
    } else {
      ret.updateSize(inputs, length);
    }
    return ret;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    int width = attrs.getValue(ATTR_NSTATE).intValue() * 30 + 2 * border + 15;
    int height =
        attrs.getValue(SHOW_CLOCK)
            ? (attrs.getValue(ATTR_INPUTS).intValue() + 1) * 30 + 3 * border + 2
            : attrs.getValue(ATTR_INPUTS).intValue() * 30 + 3 * border;
    byte showclock = (byte) (attrs.getValue(SHOW_CLOCK) ? 32 : 0);
    return Bounds.create(0, -border - showclock, width, height);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_NSTATE || attr == ATTR_INPUTS || attr == SHOW_CLOCK) {
      instance.recomputeBounds();
      updateports(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Bounds bds = painter.getBounds();
    Graphics g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), border, border);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Bounds bds = painter.getBounds();
    // if showclock = true all diagram lines are moved down
    byte showclock = (byte) (painter.getAttributeValue(SHOW_CLOCK) ? 1 : 0);
    int x = bds.getX();
    int y = bds.getY();
    int width = bds.getWidth();
    int height = bds.getHeight();
    byte inputs = (byte) (painter.getAttributeValue(ATTR_INPUTS).byteValue() + showclock);
    byte length = (byte) (painter.getAttributeValue(ATTR_NSTATE).byteValue() * 2);
    DiagramState diagramstate = getDiagramState(painter);
    Graphics2D g = (Graphics2D) painter.getGraphics();
    // draw border
    painter.drawRoundBounds(painter.getAttributeValue(ATTR_COLOR));
    // draw white space
    g.setColor(new Color(250, 250, 250));
    g.fillRoundRect(
        x + border, y + border, width - 2 * border, height - 2 * border, border / 2, border / 2);
    // draw clock edge lines if not disabled
    if (painter.getAttributeValue(VERT_LINE) != NO) {
      g.setColor(painter.getAttributeValue(ATTR_COLOR).darker());
      g.setStroke(
          new BasicStroke(
              0.5f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              0,
              new float[] {6.0f, 4.0f},
              8.0f));
      for (byte j = 1; j < length; j++) {
        // rising or both || falling or both
        if (((painter.getAttributeValue(VERT_LINE) == TRIG_RISING
                    || painter.getAttributeValue(VERT_LINE) == BOTH)
                && diagramstate.getState(0, j) == Boolean.TRUE
                && diagramstate.getState(0, j - 1) == Boolean.FALSE)
            || ((painter.getAttributeValue(VERT_LINE) == TRIG_FALLING
                    || painter.getAttributeValue(VERT_LINE) == BOTH)
                && diagramstate.getState(0, j) == Boolean.FALSE
                && diagramstate.getState(0, j - 1) == Boolean.TRUE))
          g.drawLine(x + border + 15 * j, y + border, x + border + 15 * j, y + height - border);
      }
    }
    byte nck = (byte) (length / 2);
    g.setFont(new Font("sans serif", Font.PLAIN, 8));
    for (byte i = 0; i < inputs; i++) {
      g.setColor(painter.getAttributeValue(ATTR_COLOR).darker().darker().darker());
      // horizontal line
      GraphicsUtil.switchToWidth(g, 1);
      g.drawLine(
          x + border,
          y + border + i * 30 + 30 + showclock * 2,
          x + border + 15 * length + 4,
          y + border + i * 30 + 30 + showclock * 2);

      GraphicsUtil.switchToWidth(g, 2);
      if (diagramstate.getmoveback() && diagramstate.getState(i, length - 1) != null) {
        g.setColor(Color.BLACK);
        g.drawLine(
            x + border + 15 * length,
            y + border + i * 30 + 30 + showclock * 2,
            x + border + 15 * length + 4,
            y + border + i * 30 + 30 + showclock * 2);
      }
      // arrow
      g.fillPolygon(
          new int[] {
            x + border + 15 * length + 4,
            x + border + 15 * length + 13,
            x + border + 15 * length + 4
          },
          new int[] {
            y + border + i * 30 + 27 + showclock * 2,
            y + border + i * 30 + 30 + showclock * 2,
            y + border + i * 30 + 33 + showclock * 2
          },
          3);
      if (showclock == 1 && i == 0) // clock diagram color
      g.setColor(painter.getAttributeValue(ATTR_COLOR).darker().darker());
      else // input diagrams color
      g.setColor(Color.BLACK);
      // draw diagram
      for (byte j = 0; j < length; j++) {
        // vertical line
        if (j != 0
            && diagramstate.getState(i + (showclock == 0 ? 1 : 0), j)
                != diagramstate.getState(i + (showclock == 0 ? 1 : 0), j - 1)
            && diagramstate.getState(i + (showclock == 0 ? 1 : 0), j) != null
            && diagramstate.getState(i + (showclock == 0 ? 1 : 0), j - 1) != null)
          g.drawLine(
              x + border + 15 * j,
              y + 2 * border + 30 * i + showclock * 2,
              x + border + 15 * j,
              y + border + 30 * (i + 1) + showclock * 2);
        // 1 line
        if (diagramstate.getState(i + (showclock == 0 ? 1 : 0), j) == Boolean.TRUE) {
          g.drawLine(
              x + border + 15 * j,
              y + 2 * border + 30 * i + showclock * 2,
              x + border + 15 * (j + 1),
              y + 2 * border + 30 * i + showclock * 2);
          // vertical ending line if 1
          if (j == length - 1) {
            g.drawLine(
                x + border + 15 * (j + 1),
                y + 2 * border + 30 * i + showclock * 2,
                x + border + 15 * (j + 1),
                y + border + 30 * (i + 1) + showclock * 2);
          }
          if (i == 0
              && painter.getAttributeValue(VERT_LINE) != NO
              && showclock == 1) { // drawclocknumber
            nck--;
            Integer cknum =
                ((diagramstate.getclocknumber() - nck) > 0)
                    ? diagramstate.getclocknumber() - nck
                    : 100 + (diagramstate.getclocknumber() - nck - 1);
            g.setColor(painter.getAttributeValue(ATTR_COLOR).darker());
            GraphicsUtil.drawCenteredText(
                g, cknum.toString(), x + border + 15 * j + 7, y + border + 5);
            if (showclock == 1 && i == 0)
              g.setColor(painter.getAttributeValue(ATTR_COLOR).darker().darker());
            else g.setColor(Color.BLACK);
          }
        }
        // 0 line
        else if (diagramstate.getState(i + (showclock == 0 ? 1 : 0), j) == Boolean.FALSE)
          g.drawLine(
              x + border + 15 * j,
              y + border + 30 * (i + 1) + showclock * 2,
              x + border + 15 * (j + 1),
              y + border + 30 * (i + 1) + showclock * 2);
      }
    }
    g.drawRoundRect(
        x + border, y + border, width - 2 * border, height - 2 * border, border / 2, border / 2);

    // draw ports
    for (byte i = 1; i < inputs + 2; i++) {
      painter.drawPort(i);
    }
    painter.drawClock(0, Direction.EAST);
    // draw label
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    byte inputs = (byte) (state.getAttributeValue(ATTR_INPUTS).byteValue() + 1);
    byte length = (byte) (state.getAttributeValue(ATTR_NSTATE).byteValue() * 2);
    Value clock = state.getPortValue(0);
    Value enable = state.getPortValue(inputs);
    Value clear = state.getPortValue(inputs + 1);
    DiagramState diagramstate = getDiagramState(state);

    // not disabled, not clear an clock connected
    if (clock != Value.UNKNOWN && clear != Value.TRUE && enable != Value.FALSE) {
      // get old value and set new value
      Value lastclock = diagramstate.setLastClock(clock);
      if (lastclock != Value.UNKNOWN) {
        // for each front
        if (lastclock != clock) {
          if (diagramstate.getusedcell() < length - 1)
            diagramstate.setusedcell((byte) (diagramstate.getusedcell() + 1));
          // move back all old values
          if (diagramstate.getmoveback() == true) {
            diagramstate.moveback();
            if (clock == Value.TRUE)
              diagramstate.setclocknumber((byte) (diagramstate.getclocknumber() + 1));
          }
          if (diagramstate.getusedcell() == length - 1) diagramstate.hastomoveback(true);
          // input values
          for (byte i = 0; i < inputs; i++)
            // set new value at the end
            diagramstate.setState(
                i,
                diagramstate.getusedcell(),
                (state.getPortValue(i) == Value.TRUE)
                    ? Boolean.TRUE
                    : (state.getPortValue(i) == Value.FALSE) ? Boolean.FALSE : null);

        } else if (diagramstate.getusedcell() != -1) {
          // input's values can change also after clock front because
          // of output's delays (Flip Flop, gates etc..)
          for (byte i = 1; i < inputs; i++)
            diagramstate.setState(
                i,
                diagramstate.getusedcell(),
                (state.getPortValue(i) == Value.TRUE)
                    ? Boolean.TRUE
                    : (state.getPortValue(i) == Value.FALSE) ? Boolean.FALSE : null);
        }
      }
    }
    // clear
    else if (clear == Value.TRUE) {
      diagramstate.clear();
      diagramstate.setusedcell((byte) -1);
      diagramstate.setLastClock(Value.UNKNOWN);
      diagramstate.hastomoveback(false);
      diagramstate.setclocknumber((byte) (length / 2));
    }
  }

  private void updateports(Instance instance) {
    byte inputs = instance.getAttributeValue(ATTR_INPUTS).byteValue();
    Port[] port = new Port[inputs + 3];
    for (byte i = 0; i <= inputs; i++) {
      port[i] = new Port(0, 30 * i, Port.INPUT, 1);
    }
    // enable
    port[inputs + 1] = new Port(20, 30 * inputs + 2 * border, Port.INPUT, 1);
    port[inputs + 1].setToolTip(S.getter("priorityEncoderEnableInTip"));
    // clear
    port[inputs + 2] = new Port(30, 30 * inputs + 2 * border, Port.INPUT, 1);
    port[inputs + 2].setToolTip(S.getter("ClearDiagram"));
    // clock
    port[0].setToolTip(S.getter("DigitalOscilloscopeClock"));

    instance.setPorts(port);
  }
}
