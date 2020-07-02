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
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class ReptarLocalBus extends InstanceFactory {

  public static final String getInputLabel(int id) {
    if (id < 5)
      switch (id) {
        case 0 : return "SP6_LB_nCS3_i";
        case 1 : return "SP6_LB_nADV_ALE_i";
        case 2 : return "SP6_LB_RE_nOE_i";
        case 3 : return "SP6_LB_nWE_i";
      }
    if (id < 13) return "Addr_LB_i_" + (id + 11);
    return "Undefined";
  }
  
  public static final String getOutputLabel(int id) {
    switch(id) {
      case 0  : return "SP6_LB_WAIT3_o";
      case 1  : return "IRQ_o";
      default : return "Undefined";
    }
  }
  
  public static final String getIOLabel(int id) {
    if (id < 16) return "Addr_Data_LB_io_" + id;
    return "Undefined";
  }

  public static final int SP6_LB_nCS3_o = 0;
  public static final int SP6_LB_nADV_ALE_o = 1;
  public static final int SP6_LB_RE_nOE_o = 2;
  public static final int SP6_LB_nWE_o = 3;
  public static final int SP6_LB_WAIT3_i = 4;
  public static final int Addr_Data_LB_o = 5;
  public static final int Addr_Data_LB_i = 6;
  public static final int Addr_Data_LB_tris_i = 7;
  public static final int Addr_LB_o = 8;
  public static final int IRQ_i = 9;

  /* Default Name. Very important for the genration of the VDHL Code */
  private String defaultLocalBusName = "LocalBus";

  public ReptarLocalBus() {
    super("ReptarLB", S.getter("repLBComponent"));

    ArrayList<String> inpLabels = new ArrayList<String>();
    ArrayList<String> outpLabels = new ArrayList<String>();
    ArrayList<String> ioLabels = new ArrayList<String>();
    for (int i = 0 ; i < 16 ; i++) {
      if (i < 13) inpLabels.add(getInputLabel(i));
      if (i < 2) outpLabels.add(getOutputLabel(i));
      ioLabels.add(getIOLabel(i));
    }

    setAttributes(new Attribute[] {StdAttr.LABEL,StdAttr.MAPINFO}, 
        new Object[] {defaultLocalBusName,
          new ComponentMapInformationContainer( 13, 2, 16, inpLabels, outpLabels, ioLabels)});

    // setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-110, -10, 110, 110));
    setIconName("localbus.gif");

    Port[] ps = new Port[10];
    ps[SP6_LB_nCS3_o] = new Port(0, 0, Port.OUTPUT, 1);
    ps[SP6_LB_nADV_ALE_o] = new Port(0, 10, Port.OUTPUT, 1);
    ps[SP6_LB_RE_nOE_o] = new Port(0, 20, Port.OUTPUT, 1);
    ps[SP6_LB_nWE_o] = new Port(0, 30, Port.OUTPUT, 1);
    ps[SP6_LB_WAIT3_i] = new Port(0, 40, Port.INPUT, 1);
    ps[Addr_Data_LB_o] = new Port(0, 50, Port.OUTPUT, 16);
    ps[Addr_Data_LB_i] = new Port(0, 60, Port.INPUT, 16);
    ps[Addr_Data_LB_tris_i] = new Port(0, 70, Port.INPUT, 1);
    ps[Addr_LB_o] = new Port(0, 80, Port.OUTPUT, 9);
    ps[IRQ_i] = new Port(0, 90, Port.INPUT, 1);
    // ps[Addr_Data_LB_io ] = new Port(0,80, Port.INOUT,16);
    ps[SP6_LB_nCS3_o].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_nADV_ALE_o].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_RE_nOE_o].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_nWE_o].setToolTip(S.getter("repLBTip"));
    ps[SP6_LB_WAIT3_i].setToolTip(S.getter("repLBTip"));
    ps[Addr_Data_LB_o].setToolTip(S.getter("repLBTip"));
    ps[Addr_Data_LB_i].setToolTip(S.getter("repLBTip"));
    ps[Addr_Data_LB_tris_i].setToolTip(S.getter("repLBTip"));
    ps[Addr_LB_o].setToolTip(S.getter("repLBTip"));
    ps[IRQ_i].setToolTip(S.getter("repLBTip"));
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
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    // return false;
    if (MyHDLGenerator == null) {
      MyHDLGenerator = new ReptarLocalBusHDLGeneratorFactory();
    }
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    painter.drawBounds();

    g.setColor(Color.BLACK);
    g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() - 2));
    painter.drawPort(SP6_LB_nCS3_o, "SP6_LB_nCS3_o", Direction.WEST);
    painter.drawPort(SP6_LB_nADV_ALE_o, "SP6_LB_nADV_ALE_o", Direction.WEST);
    painter.drawPort(SP6_LB_RE_nOE_o, "SP6_LB_RE_nOE_o", Direction.WEST);
    painter.drawPort(SP6_LB_nWE_o, "SP6_LB_nWE_o", Direction.WEST);
    painter.drawPort(SP6_LB_WAIT3_i, "SP6_LB_WAIT3_i", Direction.WEST);
    painter.drawPort(Addr_Data_LB_o, "Addr_Data_LB_o", Direction.WEST);
    painter.drawPort(Addr_Data_LB_i, "Addr_Data_LB_i", Direction.WEST);
    painter.drawPort(Addr_Data_LB_tris_i, "Addr_Data_LB_tris_i", Direction.WEST);
    painter.drawPort(Addr_LB_o, "Addr_LB_o", Direction.WEST);
    painter.drawPort(IRQ_i, "IRQ_i", Direction.WEST);
    // painter.drawPort(Addr_Data_LB_io ,"Addr_Data_LB_io",Direction.WEST);

    // Location loc = painter.getLocation();
    // int x = loc.getX();
    // int y = loc.getY();
    // GraphicsUtil.switchToWidth(g, 2);
    // g.setColor(Color.BLACK);
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

  @Override
  public boolean RequiresGlobalClock() {
    return true;
  }

}
