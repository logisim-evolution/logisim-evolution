/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.SevenSegmentIcon;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class SevenSegment extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7-Segment Display";

  static void drawBase(InstancePainter painter, boolean drawPoint) {
    ensureSegments();
    final var data = (InstanceDataSingleton) painter.getData();
    int summ = (data == null ? 0 : (Integer) data.getValue());
    final var active = painter.getAttributeValue(IoLibrary.ATTR_ACTIVE);
    int desired = active == null || active ? 1 : 0;

    final var bds = painter.getBounds();
    final var x = bds.getX() + 5;
    final var y = bds.getY();

    final var g = painter.getGraphics();
    final var onColor = painter.getAttributeValue(IoLibrary.ATTR_ON_COLOR);
    final var offColor = painter.getAttributeValue(IoLibrary.ATTR_OFF_COLOR);
    final var bgColor = painter.getAttributeValue(IoLibrary.ATTR_BACKGROUND);
    if (painter.shouldDrawColor() && bgColor.getAlpha() != 0) {
      g.setColor(bgColor);
      g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    for (int i = 0; i <= 7; i++) {
      if (painter.getShowState()) {
        g.setColor(((summ >> i) & 1) == desired ? onColor : offColor);
      }
      if (i < 7) {
        Bounds seg = SEGMENTS[i];
        g.fillRect(x + seg.getX(), y + seg.getY(), seg.getWidth(), seg.getHeight());
      } else {
        if (drawPoint) g.fillOval(x + 28, y + 48, 5, 5); // draw decimal point
      }
    }
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawLabel();
    painter.drawPorts();
  }

  static void ensureSegments() {
    if (SEGMENTS == null) {
      SEGMENTS =
          new Bounds[] {
            Bounds.create(3, 8, 19, 4),
            Bounds.create(23, 10, 4, 19),
            Bounds.create(23, 30, 4, 19),
            Bounds.create(3, 47, 19, 4),
            Bounds.create(-2, 30, 4, 19),
            Bounds.create(-2, 10, 4, 19),
            Bounds.create(3, 28, 19, 4)
          };
    }
  }

  public static List<String> getLabels() {
    final var labelNames = new ArrayList<String>();
    for (int i = 0; i < 8; i++) labelNames.add("");
    labelNames.set(Segment_A, "Segment_A");
    labelNames.set(Segment_B, "Segment_B");
    labelNames.set(Segment_C, "Segment_C");
    labelNames.set(Segment_D, "Segment_D");
    labelNames.set(Segment_E, "Segment_E");
    labelNames.set(Segment_F, "Segment_F");
    labelNames.set(Segment_G, "Segment_G");
    labelNames.set(DP, "DecimalPoint");
    return labelNames;
  }

  public static String getOutputLabel(int id) {
    if (id < 0 || id > getLabels().size()) return "Undefined";
    return getLabels().get(id);
  }

  public static final int Segment_A = 0;
  public static final int Segment_B = 1;
  public static final int Segment_C = 2;
  public static final int Segment_D = 3;
  public static final int Segment_E = 4;
  public static final int Segment_F = 5;
  public static final int Segment_G = 6;

  public static final int DP = 7;

  static Bounds[] SEGMENTS = null;

  static final Color DEFAULT_OFF = new Color(220, 220, 220);

  public static final Attribute<Boolean> ATTR_DP =
      Attributes.forBoolean("decimalPoint", S.getter("SevenSegDP"));

  /** Separate 1-bit pins. This is the default so existing circuits keep their wires. */
  public static final AttributeOption INPUT_SEPARATE =
      new AttributeOption("separate", S.getter("SevenSegInputSeparate"));

  /** One bus of width 7, or 8 when the decimal point is enabled. */
  public static final AttributeOption INPUT_BUS =
      new AttributeOption("bus", S.getter("SevenSegInputBus"));

  public static final Attribute<AttributeOption> ATTR_INPUT =
      Attributes.forOption(
          "inputmode",
          S.getter("SevenSegInput"),
          new AttributeOption[] {INPUT_SEPARATE, INPUT_BUS});

  /**
   * Side of the bus pin. The digit itself is not rotated. The value is stored in separate-pin mode
   * as well, and is applied when the input mode is switched to a bus.
   */
  public static final Attribute<Direction> ATTR_BUS_SIDE =
      Attributes.forDirection("busside", S.getter("SevenSegBusSide"));

  /** Wiring grid. A wire can end only on a multiple of this step. */
  static final int GRID = 10;

  /**
   * Separate-pin body. The left edge is half a cell left of the anchor so the existing pins at x =
   * 0, 10, 20 and 30 stay on the grid. Do not change these values: saved circuits depend on them.
   */
  static final int OFFSET_X = -5;
  static final int OFFSET_Y = 0;
  static final int WIDTH = 40;
  static final int HEIGHT = 60;

  public SevenSegment() {
    super(_ID, S.getter("sevenSegmentComponent"), new SevenSegmentHdlGeneratorFactory(), true);
    setAttributes(
        new Attribute[] {
          IoLibrary.ATTR_ON_COLOR,
          IoLibrary.ATTR_OFF_COLOR,
          IoLibrary.ATTR_BACKGROUND,
          IoLibrary.ATTR_ACTIVE,
          ATTR_DP,
          ATTR_INPUT,
          ATTR_BUS_SIDE,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          new Color(240, 0, 0),
          DEFAULT_OFF,
          IoLibrary.DEFAULT_BACKGROUND,
          Boolean.TRUE,
          Boolean.TRUE,
          INPUT_SEPARATE,
          Direction.WEST,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          false,
          new ComponentMapInformationContainer(0, 8, 0, null, getLabels(), null)
        });
    setOffsetBounds(Bounds.create(OFFSET_X, OFFSET_Y, WIDTH, HEIGHT));
    setIcon(new SevenSegmentIcon(false));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
  }

  static boolean isBusInput(AttributeSet attrs) {
    return INPUT_BUS.equals(attrs.getValue(ATTR_INPUT));
  }

  static int segmentCount(AttributeSet attrs) {
    return Boolean.TRUE.equals(attrs.getValue(ATTR_DP)) ? 8 : 7;
  }

  /**
   * Packs segment bits in port order: bit 0 is A, then B, C, D, E, F, G, and bit 7 is the decimal
   * point. A bit lights only when it is exactly {@link Value#TRUE}, matching the separate-pin mode.
   */
  static int segmentMask(Value[] bits) {
    var summary = 0;
    for (var i = 0; i < bits.length; i++) {
      if (bits[i] == Value.TRUE) summary |= 1 << i;
    }
    return summary;
  }

  /**
   * Horizontal grid point for the top and bottom bus pin. The body's middle is halfway between two
   * grid points, so {@code ceil(middle / 20) * 10} is used instead of the geometric center.
   */
  static int busHorizontalGridPoint() {
    final var middle = OFFSET_X + WIDTH / 2.0;
    return (int) (Math.ceil(middle / (GRID * 2.0)) * GRID);
  }

  /**
   * First grid point at or outside an edge. Positive {@code outward} points away from the interior.
   */
  static int gridPointOutside(int edge, int outward) {
    final var cells = edge / (double) GRID;
    final var snapped = outward > 0 ? Math.ceil(cells) : Math.floor(cells);
    return (int) (snapped * GRID);
  }

  /**
   * Connection point of the bus pin, in offset coordinates. The outline does not change. Top and
   * bottom use {@link #busHorizontalGridPoint()} and stay on the edge. Left and right are drawn
   * just outside the border, on the nearest grid point, because those edges are half a cell off the
   * wiring grid.
   */
  static int[] busPortPosition(Direction side) {
    final var x = busHorizontalGridPoint();
    final var y = OFFSET_Y + HEIGHT / 2;
    if (side == Direction.NORTH) return new int[] {x, OFFSET_Y};
    if (side == Direction.SOUTH) return new int[] {x, OFFSET_Y + HEIGHT};
    if (side == Direction.EAST) return new int[] {gridPointOutside(OFFSET_X + WIDTH, 1), y};
    return new int[] {gridPointOutside(OFFSET_X, -1), y};
  }

  private void updatePorts(Instance instance) {
    final var attrs = instance.getAttributeSet();
    if (isBusInput(attrs)) {
      updateBusPort(instance);
    } else {
      updateSeparatePorts(instance);
    }
    instance.getAttributeValue(StdAttr.MAPINFO).setNrOfOutports(segmentCount(attrs), getLabels());
  }

  private void updateSeparatePorts(Instance instance) {
    final var hasDp = Boolean.TRUE.equals(instance.getAttributeValue(ATTR_DP));
    final var ps = new Port[hasDp ? 8 : 7];
    ps[Segment_A] = new Port(20, 0, Port.INPUT, 1);
    ps[Segment_B] = new Port(30, 0, Port.INPUT, 1);
    ps[Segment_C] = new Port(20, 60, Port.INPUT, 1);
    ps[Segment_D] = new Port(10, 60, Port.INPUT, 1);
    ps[Segment_E] = new Port(0, 60, Port.INPUT, 1);
    ps[Segment_F] = new Port(10, 0, Port.INPUT, 1);
    ps[Segment_G] = new Port(0, 0, Port.INPUT, 1);
    ps[Segment_A].setToolTip(S.getter("Segment_A"));
    ps[Segment_B].setToolTip(S.getter("Segment_B"));
    ps[Segment_C].setToolTip(S.getter("Segment_C"));
    ps[Segment_D].setToolTip(S.getter("Segment_D"));
    ps[Segment_E].setToolTip(S.getter("Segment_E"));
    ps[Segment_F].setToolTip(S.getter("Segment_F"));
    ps[Segment_G].setToolTip(S.getter("Segment_G"));
    if (hasDp) {
      ps[DP] = new Port(30, 60, Port.INPUT, 1);
      ps[DP].setToolTip(S.getter("DecimalPoint"));
    }
    instance.setPorts(ps);
  }

  private void updateBusPort(Instance instance) {
    final var side = instance.getAttributeValue(ATTR_BUS_SIDE);
    final var at = busPortPosition(side == null ? Direction.WEST : side);
    final var width = segmentCount(instance.getAttributeSet());
    final var port = new Port(at[0], at[1], Port.INPUT, width);
    port.setToolTip(S.getter("SevenSegBusTip"));
    instance.setPorts(new Port[] {port});
  }

  @Override
  public boolean activeOnHigh(AttributeSet attrs) {
    return attrs.getValue(IoLibrary.ATTR_ACTIVE);
  }

  public static void computeTextField(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    Object labelLoc = instance.getAttributeValue(StdAttr.LABEL_LOC);

    final var bds = instance.getBounds();
    int x = bds.getX() + bds.getWidth() / 2;
    int y = bds.getY() + bds.getHeight() / 2;
    int halign = GraphicsUtil.H_CENTER;
    int valign = GraphicsUtil.V_CENTER;
    if (labelLoc == Direction.NORTH) {
      y = bds.getY() - 2;
      valign = GraphicsUtil.V_BOTTOM;
    } else if (labelLoc == Direction.SOUTH) {
      y = bds.getY() + bds.getHeight() + 2;
      valign = GraphicsUtil.V_TOP;
    } else if (labelLoc == Direction.EAST) {
      x = bds.getX() + bds.getWidth() + 2;
      halign = GraphicsUtil.H_LEFT;
    } else if (labelLoc == Direction.WEST) {
      x = bds.getX() - 2;
      halign = GraphicsUtil.H_RIGHT;
    }
    if (labelLoc == facing) {
      if (labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
        x += 2;
        halign = GraphicsUtil.H_LEFT;
      } else {
        y -= 2;
        valign = GraphicsUtil.V_BOTTOM;
      }
    }

    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, valign);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance
        .getAttributeSet()
        .setValue(
            StdAttr.MAPINFO,
            new ComponentMapInformationContainer(0, 8, 0, null, getLabels(), null));
    instance.addAttributeListener();
    updatePorts(instance);
    computeTextField(instance);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      computeTextField(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      computeTextField(instance);
    } else if (attr == ATTR_INPUT || attr == ATTR_DP) {
      updatePorts(instance);
    } else if (attr == ATTR_BUS_SIDE && isBusInput(instance.getAttributeSet())) {
      updatePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawBase(painter, painter.getAttributeValue(ATTR_DP));
  }

  private static Value[] inputBits(InstanceState state) {
    final var attrs = state.getAttributeSet();
    final var width = segmentCount(attrs);
    final var bits = new Value[width];
    if (isBusInput(attrs)) {
      final var bus = state.getPortValue(0);
      for (var i = 0; i < width; i++) {
        bits[i] = bus == null ? Value.UNKNOWN : bus.get(i);
      }
    } else {
      for (var i = 0; i < width; i++) {
        bits[i] = state.getPortValue(i);
      }
    }
    return bits;
  }

  @Override
  public void propagate(InstanceState state) {
    final Object value = segmentMask(inputBits(state));
    final var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(value));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new SevenSegmentShape(x, y, path);
  }
}
