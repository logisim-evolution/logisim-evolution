/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

abstract class AbstractGate extends InstanceFactory {
  static Value pullOutput(Value value, Object outType) {
    if (outType == GateAttributes.OUTPUT_01) {
      return value;
    } else {
      final var v = value.getAll();
      if (outType == GateAttributes.OUTPUT_0Z) {
        for (var i = 0; i < v.length; i++) {
          if (v[i] == Value.TRUE) v[i] = Value.UNKNOWN;
        }
      } else if (outType == GateAttributes.OUTPUT_Z1) {
        for (var i = 0; i < v.length; i++) {
          if (v[i] == Value.FALSE) v[i] = Value.UNKNOWN;
        }
      }
      return Value.create(v);
    }
  }

  private int bonusWidth = 0;
  private boolean negateOutput = false;
  private boolean isXor = false;
  private String rectLabel = "";

  private boolean paintInputLines;

  protected AbstractGate(String name, StringGetter desc, HdlGeneratorFactory generator) {
    this(name, desc, false, generator);
  }

  protected AbstractGate(String name, StringGetter desc, boolean isXor, HdlGeneratorFactory generator) {
    super(name, desc, generator);
    this.isXor = isXor;
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new IntegerConfigurator(GateAttributes.ATTR_INPUTS, 2, GateAttributes.MAX_INPUTS, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));
  }

  protected abstract Expression computeExpression(Expression[] inputs, int numInputs);

  private void computeLabel(Instance instance) {
    final var attrs = (GateAttributes) instance.getAttributeSet();
    final var facing = attrs.facing;
    final var baseWidth = (Integer) attrs.size.getValue();

    final var axis = baseWidth / 2 + (negateOutput ? 10 : 0);
    var perp = 0;
    if (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) {
      perp += 6;
    }
    final var loc = instance.getLocation();
    int cx;
    int cy;
    if (facing == Direction.NORTH) {
      cx = loc.getX() + perp;
      cy = loc.getY() + axis;
    } else if (facing == Direction.SOUTH) {
      cx = loc.getX() - perp;
      cy = loc.getY() - axis;
    } else if (facing == Direction.WEST) {
      cx = loc.getX() + axis;
      cy = loc.getY() - perp;
    } else {
      cx = loc.getX() - axis;
      cy = loc.getY() + perp;
    }
    instance.setTextField(
        StdAttr.LABEL, StdAttr.LABEL_FONT, cx, cy, TextField.H_CENTER, TextField.V_CENTER);
  }

  protected abstract Value computeOutput(Value[] inputs, int numInputs, InstanceState state);

  void computePorts(Instance instance) {
    final var attrs = (GateAttributes) instance.getAttributeSet();
    int inputs = attrs.inputs;

    final var ports = new Port[inputs + 1];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    for (var i = 0; i < inputs; i++) {
      final var offs = getInputOffset(attrs, i);
      ports[i + 1] = new Port(offs.getX(), offs.getY(), Port.INPUT, StdAttr.WIDTH);
    }
    instance.setPorts(ports);
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    computePorts(instance);
    computeLabel(instance);
  }

  @Override
  public boolean contains(Location loc, AttributeSet attrsBase) {
    final var attrs = (GateAttributes) attrsBase;
    if (super.contains(loc, attrs)) {
      if (attrs.negated == 0) {
        return true;
      } else {
        final var facing = attrs.facing;
        final var bds = getOffsetBounds(attrsBase);
        int delt;
        if (facing == Direction.NORTH) {
          delt = loc.getY() - (bds.getY() + bds.getHeight());
        } else if (facing == Direction.SOUTH) {
          delt = loc.getY() - bds.getY();
        } else if (facing == Direction.WEST) {
          delt = loc.getX() - (bds.getX() + bds.getHeight());
        } else {
          delt = loc.getX() - bds.getX();
        }
        if (Math.abs(delt) > 5) {
          return true;
        } else {
          int inputs = attrs.inputs;
          for (var i = 1; i <= inputs; i++) {
            Location offs = getInputOffset(attrs, i);
            if (loc.manhattanDistanceTo(offs) <= 5) return true;
          }
          return false;
        }
      }
    } else {
      return false;
    }
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new GateAttributes(isXor);
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return (attr instanceof NegateAttribute)
        ? Boolean.FALSE
        : super.getDefaultAttributeValue(attr, ver);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var myAttrs = (GateAttributes) attrs;
    final var completeName = new StringBuilder();
    completeName.append(CorrectLabel.getCorrectLabel(this.getName()).toUpperCase());
    final var width = myAttrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() > 1) completeName.append("_BUS");
    final var inputCount = myAttrs.getValue(GateAttributes.ATTR_INPUTS);
    if (inputCount > 2) {
      completeName.append("_").append(inputCount).append("_INPUTS");
    }
    if (myAttrs.containsAttribute(GateAttributes.ATTR_XOR)) {
      if (myAttrs.getValue(GateAttributes.ATTR_XOR).equals(GateAttributes.XOR_ONE)) {
        completeName.append("_ONEHOT");
      }
    }
    return completeName.toString();
  }

  //
  // protected methods intended to be overridden
  //
  protected abstract Value getIdentity();

  Location getInputOffset(GateAttributes attrs, int index) {
    final var inputs = attrs.inputs;
    final var facing = attrs.facing;
    final var size = (Integer) attrs.size.getValue();
    final var axisLength = size + bonusWidth + (negateOutput ? 10 : 0);
    final var negated = attrs.negated;

    int skipStart;
    int skipDist;
    int skipLowerEven;
    if (inputs <= 3) {
      if (size < 40) {
        skipStart = -5;
        skipDist = 10;
        skipLowerEven = 10;
      } else if (size < 60 || inputs <= 2) {
        skipStart = -10;
        skipDist = 20;
        skipLowerEven = 20;
      } else {
        skipStart = -15;
        skipDist = 30;
        skipLowerEven = 30;
      }
    } else if (inputs == 4 && size >= 60) {
      skipStart = -5;
      skipDist = 20;
      skipLowerEven = 0;
    } else {
      skipStart = -5;
      skipDist = 10;
      skipLowerEven = 10;
    }

    int dy;
    if ((inputs & 1) == 1) {
      dy = skipStart * (inputs - 1) + skipDist * index;
    } else {
      dy = skipStart * inputs + skipDist * index;
      if (index >= inputs / 2) dy += skipLowerEven;
      if (inputs == 4 && size >= 60) dy -= 10;
    }

    int dx = axisLength;
    int negatedBit = (int) (negated >> index) & 1;
    if (negatedBit == 1) {
      dx += 10;
    }

    if (facing == Direction.NORTH) {
      return Location.create(dy, dx, true);
    } else if (facing == Direction.SOUTH) {
      return Location.create(dy, -dx, true);
    } else if (facing == Direction.WEST) {
      return Location.create(dx, dy, true);
    } else {
      return Location.create(-dx, dy, true);
    }
  }

  @Override
  protected Object getInstanceFeature(final Instance instance, Object key) {
    if (key == WireRepair.class) {
      return (WireRepair) data -> AbstractGate.this.shouldRepairWire(instance, data);
    }
    if (key == ExpressionComputer.class) {
      return (ExpressionComputer)
          expressionMap -> {
            final var attrs = (GateAttributes) instance.getAttributeSet();
            final var inputCount = attrs.inputs;
            final var negated = attrs.negated;
            final var width = attrs.width.getWidth();

            for (var b = 0; b < width; b++) {
              final var inputs = new Expression[inputCount];
              var numInputs = 0;
              for (var i = 1; i <= inputCount; i++) {
                Expression e = expressionMap.get(instance.getPortLocation(i), b);
                if (e != null) {
                  final var negatedBit = (int) (negated >> (i - 1)) & 1;
                  if (negatedBit == 1) {
                    e = Expressions.not(e);
                  }
                  inputs[numInputs] = e;
                  ++numInputs;
                }
              }
              if (numInputs > 0) {
                Expression out = AbstractGate.this.computeExpression(inputs, numInputs);
                expressionMap.put(instance.getPortLocation(0), b, out);
              }
            }
          };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    final var attrs = (GateAttributes) attrsBase;
    final var facing = attrs.facing;
    final var size = (Integer) attrs.size.getValue();
    var inputs = attrs.inputs;
    if (inputs % 2 == 0) {
      inputs++;
    }
    final var negated = attrs.negated;

    var width = size + bonusWidth + (negateOutput ? 10 : 0);
    if (negated != 0) {
      width += 10;
    }
    final var height = Math.max(10 * inputs, size);
    if (facing == Direction.SOUTH) {
      return Bounds.create(-height / 2, -width, height, width);
    } else if (facing == Direction.NORTH) {
      return Bounds.create(-height / 2, 0, height, width);
    } else if (facing == Direction.WEST) {
      return Bounds.create(0, -height / 2, width, height);
    } else {
      return Bounds.create(-width, -height / 2, width, height);
    }
  }

  protected String getRectangularLabel(AttributeSet attrs) {
    return rectLabel;
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    return (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
        ? (attrs.getValue(GateAttributes.ATTR_OUTPUT) != GateAttributes.OUTPUT_01)
        : false;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == GateAttributes.ATTR_SIZE || attr == StdAttr.FACING) {
      instance.recomputeBounds();
      computePorts(instance);
      computeLabel(instance);
    } else if (attr == GateAttributes.ATTR_INPUTS || attr instanceof NegateAttribute) {
      instance.recomputeBounds();
      computePorts(instance);
    } else if (attr == GateAttributes.ATTR_XOR) {
      instance.fireInvalidated();
    }
  }

  private void paintBase(InstancePainter painter) {
    final var attrs = (GateAttributes) painter.getAttributeSet();
    final var facing = attrs.facing;
    final var inputs = attrs.inputs;
    final var negated = attrs.negated;

    Object shape = painter.getGateShape();
    final var loc = painter.getLocation();
    final var bds = painter.getOffsetBounds();
    var width = bds.getWidth();
    var height = bds.getHeight();
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      int t = width;
      width = height;
      height = t;
    }
    if (negated != 0) {
      width -= 10;
    }

    final var g = painter.getGraphics();
    final var baseColor = new Color(AppPreferences.COMPONENT_COLOR.get());
    if (shape == AppPreferences.SHAPE_SHAPED && paintInputLines) {
      PainterShaped.paintInputLines(painter, this);
    } else if (negated != 0) {
      for (int i = 0; i < inputs; i++) {
        int negatedBit = (int) (negated >> i) & 1;
        if (negatedBit == 1) {
          Location in = getInputOffset(attrs, i);
          Location cen = in.translate(facing, 5);
          painter.drawDongle(loc.getX() + cen.getX(), loc.getY() + cen.getY());
        }
      }
    }

    g.setColor(baseColor);
    g.translate(loc.getX(), loc.getY());
    var rotate = 0.0;
    if (facing != Direction.EAST && g instanceof Graphics2D g2) {
      rotate = -facing.toRadians();
      g2.rotate(rotate);
    }

    if (shape == AppPreferences.SHAPE_RECTANGULAR) {
      paintRectangular(painter, width, height);
      //    } else if (shape == AppPreferences.SHAPE_DIN40700) {
      //      paintDinShape(painter, width, height, inputs);
    } else { // SHAPE_SHAPED
      if (negateOutput) {
        g.translate(-10, 0);
        paintShape(painter, width - 10, height);
        painter.drawDongle(5, 0);
        g.translate(10, 0);
      } else {
        paintShape(painter, width, height);
      }
    }

    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-loc.getX(), -loc.getY());

    painter.drawLabel();
  }

  protected abstract void paintDinShape(InstancePainter painter, int width, int height, int inputs);

  //
  // painting methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    paintBase(painter);
  }

  @Override
  public final void paintIcon(InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics().create();
    GraphicsUtil.switchToWidth(g, AppPreferences.getScaled(1));
    final var border = AppPreferences.getIconBorder();
    if (painter.getGateShape().equals(AppPreferences.SHAPE_RECTANGULAR))
      paintIconIEC(g, getRectangularLabel(painter.getAttributeSet()), negateOutput, false);
    else
      paintIconANSI(
          g, AppPreferences.getIconSize() - (border << 1), border, AppPreferences.getScaled(4));
    g.dispose();
  }

  protected static void paintIconIEC(Graphics2D g, String label, boolean negateOutput, boolean singleInput) {
    GraphicsUtil.switchToWidth(g, AppPreferences.getScaled(1));
    final var iconBorder = AppPreferences.getIconBorder();
    final var iconSize = AppPreferences.getIconSize() - (iconBorder << 1);
    final var negateDiameter = AppPreferences.getScaled(4);
    final var yoffset = singleInput ? (int) (iconSize / 6.0) : 0;
    final var ysize = singleInput ? iconSize - (yoffset << 1) : iconSize;
    final var af = g.getTransform();
    g.translate(iconBorder, iconBorder);
    g.drawRect(0, yoffset, iconSize - negateDiameter, ysize);
    final var iconFont = g.getFont().deriveFont(((float) iconSize) / 2).deriveFont(Font.BOLD);
    g.setFont(iconFont);
    if (label.length() < 3) {
      final var txt = new TextLayout(label, iconFont, g.getFontRenderContext());
      float xpos =
          ((float) iconSize - (float) negateDiameter) / 2 - (float) txt.getBounds().getCenterX();
      float ypos = ((float) iconSize) / 2 - (float) txt.getBounds().getCenterY();
      txt.draw(g, xpos, ypos);
    } else {
      var txt = new TextLayout(label.substring(0, 2), iconFont, g.getFontRenderContext());
      float xpos =
          ((float) iconSize - (float) negateDiameter) / 2 - (float) txt.getBounds().getCenterX();
      float ypos = ((float) iconSize) / 4 - (float) txt.getBounds().getCenterY();
      txt.draw(g, xpos, ypos);
      txt =
          new TextLayout(label.substring(2, label.length() < 5 ? label.length() : 4),
              iconFont, g.getFontRenderContext());
      xpos = ((float) iconSize - (float) negateDiameter) / 2 - (float) txt.getBounds().getCenterX();
      ypos = (3 * (float) iconSize) / 4 - (float) txt.getBounds().getCenterY();
      txt.draw(g, xpos, ypos);
    }
    paintIconPins(g, iconSize, iconBorder, negateDiameter, negateOutput, singleInput);
    g.setTransform(af);
  }

  protected static void paintIconPins(Graphics2D g, int iconSize, int iconBorder, int negateDiameter, boolean negateOutput, boolean singleInput) {
    if (negateOutput)
      g.drawOval(iconSize - negateDiameter, (iconSize - negateDiameter) >> 1, negateDiameter, negateDiameter);
    g.drawLine(
        iconSize - (negateOutput ? 0 : negateDiameter),
        iconSize >> 1,
        iconSize - (negateOutput ? 0 : negateDiameter) + iconBorder,
        iconSize >> 1);
    if (singleInput)
      g.drawLine(-iconBorder, iconSize >> 1, 0, iconSize >> 1);
    else {
      g.drawLine(-iconBorder, iconSize >> 2, 0, iconSize >> 2);
      g.drawLine(-iconBorder, (3 * iconSize) >> 2, 0, (3 * iconSize) >> 2);
    }
  }

  protected static void paintIconBufferAnsi(Graphics2D gfx, boolean negate, boolean controlled) {
    GraphicsUtil.switchToWidth(gfx, AppPreferences.getScaled(1));
    final var borderSize = AppPreferences.getIconBorder();
    final var iconSize = AppPreferences.getIconSize() - (borderSize << 1);
    final var negateSize = AppPreferences.getScaled(4);
    final var af = gfx.getTransform();
    gfx.translate(borderSize, borderSize);
    final var ystart = negateSize >> 1;
    final var yend = iconSize - ystart;
    final var xstart = 0;
    final var xend = iconSize - negateSize;
    final var xpos = new int[] {xstart, xend, xstart, xstart};
    final var ypos = new int[] {ystart, iconSize >> 1, yend, ystart};
    gfx.drawPolygon(xpos, ypos, 4);
    paintIconPins(gfx, iconSize, borderSize, negateSize, negate, true);
    if (controlled) gfx.drawLine(xend >> 1, ((3 * (yend - ystart)) >> 2) + ystart, xend >> 1, yend);
    gfx.setTransform(af);
  }

  protected abstract void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize);

  @Override
  public void paintInstance(InstancePainter painter) {
    paintBase(painter);
    if (!painter.isPrintView() || painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR) {
      painter.drawPorts();
    }
  }

  protected void paintRectangular(InstancePainter painter, int width, int height) {
    final var don = negateOutput ? 10 : 0;
    final var attrs = painter.getAttributeSet();
    painter.drawRectangle(-width, -height / 2, width - don, height, getRectangularLabel(attrs));
    if (negateOutput) {
      painter.drawDongle(-5, 0);
    }
  }

  protected abstract void paintShape(InstancePainter painter, int width, int height);

  @Override
  public void propagate(InstanceState state) {
    final var attrs = (GateAttributes) state.getAttributeSet();
    final var inputCount = attrs.inputs;
    final var negated = attrs.negated;
    final var opts = state.getProject().getOptions().getAttributeSet();
    final var errorIfUndefined =
        opts.getValue(Options.ATTR_GATE_UNDEFINED).equals(Options.GATE_UNDEFINED_ERROR);

    final var inputs = new Value[inputCount];
    var numInputs = 0;
    var error = false;
    for (var i = 1; i <= inputCount; i++) {
      if (state.isPortConnected(i)) {
        final var negatedBit = (int) (negated >> (i - 1)) & 1;
        if (negatedBit == 1) {
          inputs[numInputs] = state.getPortValue(i).not();
        } else {
          inputs[numInputs] = state.getPortValue(i);
        }
        numInputs++;
      } else {
        if (errorIfUndefined) {
          error = true;
        }
      }
    }

    final var out = (numInputs == 0 || error)
            ? Value.createError(attrs.width)
            : pullOutput(computeOutput(inputs, numInputs, state), attrs.out);
    state.setPort(0, out, GateAttributes.DELAY);
  }

  protected void setAdditionalWidth(int value) {
    bonusWidth = value;
  }

  protected void setNegateOutput(boolean value) {
    negateOutput = value;
  }

  protected void setPaintInputLines(boolean value) {
    paintInputLines = value;
  }

  protected void setRectangularLabel(String value) {
    rectLabel = value;
  }

  protected boolean shouldRepairWire(Instance instance, WireRepairData data) {
    return false;
  }
}
