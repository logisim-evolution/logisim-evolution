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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.util.ArrayList;

public class ReptarLocalBus extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "ReptarLB";

  public static String getInputLabel(int id) {
    if (id < 5)
      switch (id) {
        case 0: return "SP6_LB_nCS3_i";
        case 1: return "SP6_LB_nADV_ALE_i";
        case 2: return "SP6_LB_RE_nOE_i";
        case 3: return "SP6_LB_nWE_i";
      }
    if (id < 13) return "Addr_LB_i_" + (id + 11);
    return "Undefined";
  }

  public static String getOutputLabel(int id) {
    return switch (id) {
      case 0 -> "SP6_LB_WAIT3_o";
      case 1 -> "IRQ_o";
      default -> "Undefined";
    };
  }

  public static String getIoLabel(int id) {
    if (id < 16) return "Addr_Data_LB_io_" + id;
    return "Undefined";
  }

  // FIXME: these names do not conform to const namich scheme. Maybe instead of "n" we just can add verbose "ACTIVE_LOW"?
  public static final int SP6_LB_nCS3_O = 0;
  public static final int SP6_LB_nADV_ALE_O = 1;
  public static final int SP6_LB_RE_nOE_O = 2;
  public static final int SP6_LB_nWE_O = 3;
  public static final int SP_6_LB_WAIT_3_I = 4;
  public static final int ADDR_DATA_LB_O = 5;
  public static final int ADDR_DATA_LB_I = 6;
  public static final int ADDR_DATA_LB_TRIS_I = 7;
  public static final int ADDR_LB_O = 8;
  public static final int IRQ_I = 9;

  /* Default Name. Very important for the genration of the VDHL Code */
  private static final String defaultLocalBusName = "LocalBus";

  public ReptarLocalBus() {
    super(_ID, S.getter("repLBComponent"), new ReptarLocalBusHdlGeneratorFactory(), false, true);

    final var inpLabels = new ArrayList<String>();
    final var outpLabels = new ArrayList<String>();
    final var ioLabels = new ArrayList<String>();
    for (var i = 0; i < 16; i++) {
      if (i < 13) inpLabels.add(getInputLabel(i));
      if (i < 2) outpLabels.add(getOutputLabel(i));
      ioLabels.add(getIoLabel(i));
    }

    setAttributes(
        new Attribute[] {StdAttr.LABEL, StdAttr.MAPINFO},
        new Object[] {
          defaultLocalBusName,
          new ComponentMapInformationContainer(13, 2, 16, inpLabels, outpLabels, ioLabels)
        });

    // setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-110, -10, 110, 110));
    setIconName("localbus.gif");

    final var ps = new Port[10];
    ps[SP6_LB_nCS3_O] = new Port(0, 0, Port.OUTPUT, 1);
    ps[SP6_LB_nADV_ALE_O] = new Port(0, 10, Port.OUTPUT, 1);
    ps[SP6_LB_RE_nOE_O] = new Port(0, 20, Port.OUTPUT, 1);
    ps[SP6_LB_nWE_O] = new Port(0, 30, Port.OUTPUT, 1);
    ps[SP_6_LB_WAIT_3_I] = new Port(0, 40, Port.INPUT, 1);
    ps[ADDR_DATA_LB_O] = new Port(0, 50, Port.OUTPUT, 16);
    ps[ADDR_DATA_LB_I] = new Port(0, 60, Port.INPUT, 16);
    ps[ADDR_DATA_LB_TRIS_I] = new Port(0, 70, Port.INPUT, 1);
    ps[ADDR_LB_O] = new Port(0, 80, Port.OUTPUT, 9);
    ps[IRQ_I] = new Port(0, 90, Port.INPUT, 1);
    // ps[Addr_Data_LB_io ] = new Port(0,80, Port.INOUT,16);
    ps[SP6_LB_nCS3_O].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_nADV_ALE_O].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_RE_nOE_O].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_nWE_O].setToolTip(S.getter("repLBTip"));
    ps[SP_6_LB_WAIT_3_I].setToolTip(S.getter("repLBTip"));
    ps[ADDR_DATA_LB_O].setToolTip(S.getter("repLBTip"));
    ps[ADDR_DATA_LB_I].setToolTip(S.getter("repLBTip"));
    ps[ADDR_DATA_LB_TRIS_I].setToolTip(S.getter("repLBTip"));
    ps[ADDR_LB_O].setToolTip(S.getter("repLBTip"));
    ps[IRQ_I].setToolTip(S.getter("repLBTip"));
    // ps[Addr_Data_LB_io ].setToolTip(S.getter("repLBTip"));
    setPorts(ps);

  }

  @Override
  public Component createComponent(Location loc, AttributeSet attrs) {
    attrs.setReadOnly(StdAttr.LABEL, true);
    return super.createComponent(loc, attrs);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    /* Force the name of the localBus*/
    return attrs.getValue(StdAttr.LABEL);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() - 2));

    painter.drawBounds();
    painter.drawPort(SP6_LB_nCS3_O, "SP6_LB_nCS3_o", Direction.WEST);
    painter.drawPort(SP6_LB_nADV_ALE_O, "SP6_LB_nADV_ALE_o", Direction.WEST);
    painter.drawPort(SP6_LB_RE_nOE_O, "SP6_LB_RE_nOE_o", Direction.WEST);
    painter.drawPort(SP6_LB_nWE_O, "SP6_LB_nWE_o", Direction.WEST);
    painter.drawPort(SP_6_LB_WAIT_3_I, "SP6_LB_WAIT3_i", Direction.WEST);
    painter.drawPort(ADDR_DATA_LB_O, "Addr_Data_LB_o", Direction.WEST);
    painter.drawPort(ADDR_DATA_LB_I, "Addr_Data_LB_i", Direction.WEST);
    painter.drawPort(ADDR_DATA_LB_TRIS_I, "Addr_Data_LB_tris_i", Direction.WEST);
    painter.drawPort(ADDR_LB_O, "Addr_LB_o", Direction.WEST);
    painter.drawPort(IRQ_I, "IRQ_i", Direction.WEST);
    // painter.drawPort(Addr_Data_LB_io ,"Addr_Data_LB_io",Direction.WEST);

    // Location loc = painter.getLocation();
    // int x = loc.getX();
    // int y = loc.getY();
    // GraphicsUtil.switchToWidth(g, 2);
    // g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    // g.drawLine(x - 15, y, x - 5, y);
    // g.drawLine(x - 10, y - 5, x - 10, y + 5);
    // GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    throw new UnsupportedOperationException("Reptar Local Bus simulation not implemented");
    // // get attributes
    // BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    //
    // // compute outputs
    // Value a = state.getPortValue(IN0);
    // Value b = state.getPortValue(IN1);
    // Value c_in = state.getPortValue(C_IN);
    // Value[] outs = ReptarLocalBus.computeSum(dataWidth, a, b, c_in);
    //
    // // propagate them
    // int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    // state.setPort(OUT, outs[0], delay);
    // state.setPort(C_OUT, outs[1], delay);
  }
}
