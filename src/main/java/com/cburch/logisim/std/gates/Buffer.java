/*
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

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LineBuffer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

class Buffer extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Buffer";

  private static class BufferGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
    @Override
    public ArrayList<String> GetLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      return (new LineBuffer())
          .add(HDL.isVHDL() ? "Result <= Input_1;" : "assign Result = Input_1;")
          .empty()
          .getWithIndent();
    }
  }

  //
  // static methods - shared with other classes
  //
  static Value repair(InstanceState state, Value v) {
    final var opts = state.getProject().getOptions().getAttributeSet();
    Object onUndefined = opts.getValue(Options.ATTR_GATE_UNDEFINED);
    final var errorIfUndefined = onUndefined.equals(Options.GATE_UNDEFINED_ERROR);
    Value repaired;
    if (errorIfUndefined) {
      final var vw = v.getWidth();
      final var w = state.getAttributeValue(StdAttr.WIDTH);
      final var ww = w.getWidth();
      if (vw == ww && v.isFullyDefined()) return v;
      final var vs = new Value[w.getWidth()];
      for (var i = 0; i < vs.length; i++) {
        final var ini = i < vw ? v.get(i) : Value.ERROR;
        vs[i] = ini.isFullyDefined() ? ini : Value.ERROR;
      }
      repaired = Value.create(vs);
    } else {
      repaired = v;
    }

    Object outType = state.getAttributeValue(GateAttributes.ATTR_OUTPUT);
    return AbstractGate.pullOutput(repaired, outType);
  }

  public static final InstanceFactory FACTORY = new Buffer();

  private Buffer() {
    super(_ID, S.getter("bufferComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.WIDTH,
          GateAttributes.ATTR_OUTPUT,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT
        },
        new Object[] {
          Direction.EAST, BitWidth.ONE, GateAttributes.OUTPUT_01, "", StdAttr.DEFAULT_LABEL_FONT
        });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setPorts(
        new Port[] {
          new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH), new Port(0, -20, Port.INPUT, StdAttr.WIDTH),
        });
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
    NotGate.configureLabel(instance, false, null);
  }

  private void configurePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);

    final var ports = new Port[2];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    final var out = Location.create(0, 0).translate(facing, -20);
    ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, StdAttr.WIDTH);
    instance.setPorts(ports);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var CompleteName = new StringBuilder();
    CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()).toUpperCase());
    CompleteName.append("_COMPONENT");
    final var width = attrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() > 1) CompleteName.append("_BUS");
    return CompleteName.toString();
  }

  @Override
  public Object getInstanceFeature(final Instance instance, Object key) {
    if (key == ExpressionComputer.class) {
      return (ExpressionComputer) expressionMap -> {
        final var width = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
        for (var b = 0; b < width; b++) {
          final var e = expressionMap.get(instance.getPortLocation(1), b);
          if (e != null) {
            expressionMap.put(instance.getPortLocation(0), b, e);
          }
        }
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    if (facing == Direction.SOUTH) return Bounds.create(-9, -20, 18, 20);
    if (facing == Direction.NORTH) return Bounds.create(-9, 0, 18, 20);
    if (facing == Direction.WEST) return Bounds.create(0, -9, 20, 18);
    return Bounds.create(-20, -9, 20, 18);
  }

  @Override
  public boolean HasThreeStateDrivers(AttributeSet attrs) {
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      return !(attrs.getValue(GateAttributes.ATTR_OUTPUT) == GateAttributes.OUTPUT_01);
    else return false;
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new BufferGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      configurePorts(instance);
      NotGate.configureLabel(instance, false, null);
    }
  }

  private void paintBase(InstancePainter painter) {
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    final var g = painter.getGraphics();
    g.translate(x, y);
    double rotate = 0.0;
    if (facing != Direction.EAST && g instanceof Graphics2D) {
      rotate = -facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    GraphicsUtil.switchToWidth(g, 2);
    Object shape = painter.getGateShape();
    if (shape == AppPreferences.SHAPE_RECTANGULAR) {
      g.drawRect(-19, -9, 18, 18);
      GraphicsUtil.drawCenteredText(g, "1", -10, 0);
    } else {
      final var xp = new int[4];
      final var yp = new int[4];
      xp[0] = 0;
      yp[0] = 0;
      xp[1] = -19;
      yp[1] = -7;
      xp[2] = -19;
      yp[2] = 7;
      xp[3] = 0;
      yp[3] = 0;
      g.drawPolyline(xp, yp, 4);
    }
    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-x, -y);
  }

  //
  // painting methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    paintBase(painter);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(Color.BLACK);
    paintBase(painter);
    painter.drawPorts();
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    var in = state.getPortValue(1);
    in = Buffer.repair(state, in);
    state.setPort(0, in.not().not(), GateAttributes.DELAY);
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics();
    if (painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR)
      AbstractGate.paintIconIEC(g, "1", false, true);
    else
      AbstractGate.paintIconBufferANSI(g, false, false);
  }
}
