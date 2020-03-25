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

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;


public class RamAppearance {

  public static int getNrAddrPorts(AttributeSet attrs) { 
    return 1 ; 
  }
  public static int getNrDataInPorts(AttributeSet attrs) {
    if (!seperatedBus(attrs)) return 0;
    return getNrDataOutPorts(attrs);
  }
  public static int getNrDataOutPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR) ||
         attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL)) return 2;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD)) return 4;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO)) return 8;
    }
    return 1;  
  }
  public static int getNrDataPorts(AttributeSet attrs) {
    return getNrDataInPorts(attrs)+getNrDataOutPorts(attrs);
  }
  public static int getNrOEPorts(AttributeSet attrs) {
	if (!attrs.containsAttribute(Mem.ENABLES_ATTR)) return 0;
    if (!seperatedBus(attrs)||
        (seperatedBus(attrs) && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES))) return 1;
    else return 0;
  }
  public static int getNrWEPorts(AttributeSet attrs) {
	if (!attrs.containsAttribute(Mem.ENABLES_ATTR)) return 0;
    return 1;
  }
  public static int getNrClkPorts(AttributeSet attrs) {
	if (!attrs.containsAttribute(Mem.ENABLES_ATTR)) return 0;
    boolean async = !synchronous(attrs);
    if (async && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES)) return 0;
    else return 1;
  }
  public static int getNrLEPorts(AttributeSet attrs) {
	if (!attrs.containsAttribute(Mem.ENABLES_ATTR)) return 0;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL)) return 2;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD)) return 4;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO)) return 8;
    }
    return 0;
  }
  public static int getNrBEPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR)) return 0;
    boolean async = !synchronous(attrs);
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES) &&
        attrs.getValue(RamAttributes.ATTR_ByteEnables).equals(RamAttributes.BUS_WITH_BYTEENABLES)&&
        !async) {
      int nrBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      if (nrBits < 9) return 0;
      return (nrBits+7)>>3;
    }
    return 0;
  }
  public static int getNrClrPorts(AttributeSet attrs) {
    if (attrs.containsAttribute(RamAttributes.CLEAR_PIN) && attrs.getValue(RamAttributes.CLEAR_PIN)) return 1;
    return 0;
  }
  public static int getNrOfPorts(AttributeSet attrs) {
    return getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs)+getNrWEPorts(attrs)+
           getNrClkPorts(attrs)+getNrLEPorts(attrs)+getNrBEPorts(attrs)+getNrClrPorts(attrs);
  }
  
  public static int getAddrIndex(int portIndex, AttributeSet attrs) { 
    switch (portIndex) {
      case 0  : return 0;
      default : return -1;
    }
  }
  public static int getDataInIndex(int portIndex, AttributeSet attrs) {
    if (!seperatedBus(attrs)) 
      return getDataOutIndex(portIndex,attrs);
    int portOffset = getNrAddrPorts(attrs)+getNrDataOutPorts(attrs);
    return getDataOffset(portOffset, portIndex, attrs);
  }
  public static int getDataOutIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs);
    return getDataOffset(portOffset, portIndex, attrs);
  }
  public static int getOEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs);
    int nrOEs = getNrOEPorts(attrs);
    if (nrOEs == 0 || portIndex < 0) return -1;
    if (portIndex < nrOEs) return portOffset+portIndex;
    return -1;
  }
  public static int getWEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs);
    int nrWEs = getNrWEPorts(attrs);
    if (nrWEs == 0 || portIndex < 0) return -1;
    if (portIndex < nrWEs) return portOffset+portIndex;
    return -1;
  }
  public static int getClkIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs)+getNrWEPorts(attrs);
    if (getNrClkPorts(attrs) == 0 || portIndex != 0) return -1;
    return portOffset;
  }
  public static int getLEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs)+getNrWEPorts(attrs)+getNrClkPorts(attrs);
    int nrLEs = getNrLEPorts(attrs);
    if (nrLEs == 0 || portIndex < 0) return -1;
    if (portIndex < nrLEs) return portOffset+portIndex;
    return -1;
  }
  public static int getBEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs)+getNrWEPorts(attrs)+
                     getNrClkPorts(attrs)+getNrLEPorts(attrs);
    int nrBEs = getNrBEPorts(attrs);
    if (nrBEs == 0 || portIndex < 0) return -1;
    if (portIndex < nrBEs) return portOffset+portIndex;
    return -1;
  }
  public static int getClrIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs)+getNrDataPorts(attrs)+getNrOEPorts(attrs)+getNrWEPorts(attrs)+
                     getNrClkPorts(attrs)+getNrLEPorts(attrs)+getNrBEPorts(attrs);
    int nrClrs = getNrClrPorts(attrs);
    if (nrClrs == 0 || portIndex < 0) return -1;
    if (portIndex < nrClrs) return portOffset+portIndex;
    return -1;
  }

  public static void configurePorts(Instance instance) {
    AttributeSet attrs = instance.getAttributeSet();
    Port[] ps = new Port[getNrOfPorts(attrs)];
    for (int i = 0 ; i < getNrAddrPorts(attrs); i++)
      ps[getAddrIndex(i,attrs)] = getAddrPort(i,attrs);
    for (int i = 0 ; i < getNrDataInPorts(attrs) ; i++)
      ps[getDataInIndex(i,attrs)] = getDataInPort(i,attrs);
    for (int i = 0 ; i < getNrDataOutPorts(attrs) ; i++)
      ps[getDataOutIndex(i,attrs)] = getDataOutPort(i,attrs);
    for (int i = 0 ; i < getNrOEPorts(attrs); i++)
      ps[getOEIndex(i,attrs)] = getOEPort(i,attrs);
    for (int i = 0 ; i < getNrWEPorts(attrs) ; i++)
      ps[getWEIndex(i,attrs)] = getWEPort(i,attrs);
    for (int i = 0 ; i < getNrClkPorts(attrs) ; i++)
      ps[getClkIndex(i,attrs)] = getClkPort(i,attrs);
    for (int i = 0 ; i < getNrLEPorts(attrs) ; i++)
      ps[getLEIndex(i,attrs)] = getLEPort(i,attrs);
    for (int i = 0 ; i < getNrBEPorts(attrs) ; i++)
      ps[getBEIndex(i,attrs)] = getBEPort(i,attrs);
    for (int i = 0 ; i < getNrClrPorts(attrs); i++)
      ps[getClrIndex(i,attrs)] = getClrPort(i,attrs);
    instance.setPorts(ps);
  }
  
  public static Bounds getBounds(AttributeSet attrs) {
    int xoffset = (seperatedBus(attrs)) ? 40 : 50;
    if (classicAppearance(attrs)) {
      return Bounds.create(0, 0, Mem.SymbolWidth + xoffset, 140);
    } else {
      int len = Math.max(attrs.getValue(Mem.DATA_ATTR).getWidth()*20,(getNrLEPorts(attrs)+1)*10);
      return Bounds.create(0, 0, Mem.SymbolWidth + xoffset, getControlHeight(attrs) + len);
    }
  }

  public static boolean classicAppearance(AttributeSet attrs) {
    return attrs.getValue(StdAttr.APPEARANCE).equals(StdAttr.APPEAR_CLASSIC);
  }

  public static void DrawRamClassic(InstancePainter painter) {
    AttributeSet attrs = painter.getAttributeSet();
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    Instance inst = painter.getInstance();
    /* draw label */
    String Label = painter.getAttributeValue(StdAttr.LABEL);
    if (Label != null && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      Font font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, Label, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }
    /* draw body */
    painter.drawBounds();
    /* draw connections */
    DrawConnections(inst,attrs,painter);
    /* draw the size */
    String type = inst.getFactory() instanceof Ram ? "RAM " : "ROM ";
    GraphicsUtil.drawCenteredText( g,
            type+ Mem.GetSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR).getWidth())
                + " x "
                + Integer.toString(painter.getAttributeValue(Mem.DATA_ATTR).getWidth()),
            bds.getX() + (Mem.SymbolWidth / 2) + 20,
            bds.getY() + 6);
    /* draw the contents */
    if (painter.getShowState()) {
      MemState state = (MemState) inst.getData(painter.getCircuitState());
      if (state != null)
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            30,
            15,
            bds.getWidth() - 60,
            bds.getHeight() - 20,
            getNrToHighlight(attrs));
    }
  }

  public static void DrawRamEvolution(InstancePainter painter) {
    AttributeSet attrs = painter.getAttributeSet();
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    Instance inst = painter.getInstance();
    /* draw label */
    String Label = painter.getAttributeValue(StdAttr.LABEL);
    if (Label != null && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      Font font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, Label, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }
    /* draw shape */
    DrawControlBlock(inst,attrs,painter);
    DrawDataBlocks(inst,attrs,painter);
    /* draw connections */
    DrawConnections(inst,attrs,painter);
    /* draw the size */
    String type = inst.getFactory() instanceof Ram ? "RAM " : "ROM ";
   GraphicsUtil.drawCenteredText( g,
            type+ Mem.GetSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR).getWidth())
                + " x "
                + Integer.toString(painter.getAttributeValue(Mem.DATA_ATTR).getWidth()),
            bds.getX() + (Mem.SymbolWidth / 2) + 20,
            bds.getY() + 6);
    /* draw the contents */
    if (painter.getShowState()) {
      MemState state = (MemState) inst.getData(painter.getCircuitState());
      if (state != null)
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            50,
            getControlHeight(attrs)+5,
            bds.getWidth() - 100,
            bds.getHeight() - 10 - getControlHeight(attrs),
            getNrToHighlight(attrs));
    }
  }
  
  public static int getControlHeight(AttributeSet attrs) {
    int result = 60;
    if (attrs.containsAttribute(Mem.ENABLES_ATTR) &&
        attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (!classicAppearance(attrs)) result += 30;
      result += getNrLEPorts(attrs)*10;
    } else if (attrs.containsAttribute(StdAttr.TRIGGER)){
      boolean async = !synchronous(attrs);
      result += 20;
      if (!async) result += 10;
      result += getNrBEPorts(attrs) * 10;
    }
    return result;
  }

  /* here all private handles are defined */
  private static int getNrToHighlight(AttributeSet attrs) {
    if (attrs.containsAttribute(Mem.ENABLES_ATTR) && 
    	attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES))
      return 1;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL)) return 2;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD)) return 4;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO)) return 8;
    return 1;
  }
  private static int getDataOffset(int portOffset, int portIndex, AttributeSet attrs) {
    switch (portIndex) {
      case 0  : return portOffset;
      case 1  : if ((!attrs.containsAttribute(Mem.ENABLES_ATTR) ||
    		         attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES))&&
                    !attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE)) return portOffset+1;
                else return -1;
      case 2  : 
      case 3  : if ((!attrs.containsAttribute(Mem.ENABLES_ATTR) ||
		             attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES))&&
                    (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD) ||
                     attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO))) return portOffset+portIndex;
                else return -1;
      case 4  :
      case 5  :
      case 6  :
      case 7  :  if ((!attrs.containsAttribute(Mem.ENABLES_ATTR) ||
		             attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES))&&
		             attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO)) return portOffset+portIndex;
                else return -1;
      default : return -1;
    }
  }
  
  private static Port getAddrPort(int portIndex, AttributeSet attrs) {
    Port result = new Port(0,10,Port.INPUT,attrs.getValue(Mem.ADDR_ATTR));
    result.setToolTip(S.getter("memAddrTip"));
    return result;
  }
  private static Port getDataInPort(int portIndex, AttributeSet attrs) {
    int nrDins = getNrDataInPorts(attrs);
    if (nrDins == 0 || portIndex < 0) return null;
    if (portIndex >= nrDins) return null;
    int ypos = getControlHeight(attrs);
    boolean classic = classicAppearance(attrs);
    BitWidth bits = attrs.getValue(Mem.DATA_ATTR);
    if (!classic && bits.getWidth() == 1) ypos += 10;
    ypos += portIndex*10;
    Port result = new Port(0,ypos,Port.INPUT,bits);
    switch (portIndex) {
      case 0 : if (nrDins == 1) result.setToolTip(S.getter("ramInTip"));
               else result.setToolTip(S.getter("ramInTip0"));
               break;
      case 1 : result.setToolTip(S.getter("ramInTip1"));
               break;
      case 2 : result.setToolTip(S.getter("ramInTip2"));
               break;
      case 3 : result.setToolTip(S.getter("ramInTip3"));
               break;
    }
    return result;
  }
  private static Port getDataOutPort(int portIndex, AttributeSet attrs) {
    int nrDouts = getNrDataOutPorts(attrs);
    if (nrDouts == 0 || portIndex < 0) return null;
    if (portIndex >= nrDouts) return null;
    int ypos = getControlHeight(attrs);
    int xpos = Mem.SymbolWidth+40;
    String portType = Port.OUTPUT;
    if (!seperatedBus(attrs)&&attrs.containsAttribute(Mem.ENABLES_ATTR)) {
      xpos += 10;
      portType = Port.INOUT;
    }
    boolean classic = classicAppearance(attrs);
    BitWidth bits = attrs.getValue(Mem.DATA_ATTR);
    if (!classic && bits.getWidth() == 1) ypos += 10;
    ypos += portIndex*10;
    Port result = new Port(xpos,ypos,portType,bits);
    switch (portIndex) {
      case 0 : if (nrDouts == 1) result.setToolTip(S.getter("memDataTip"));
               else result.setToolTip(S.getter("memDataTip0"));
               break;
      case 1 : result.setToolTip(S.getter("memDataTip1"));
               break;
      case 2 : result.setToolTip(S.getter("memDataTip2"));
               break;
      case 3 : result.setToolTip(S.getter("memDataTip3"));
               break;
    }
    return result;
  }
  private static Port getOEPort(int portIndex, AttributeSet attrs) {
    int nrOEs = getNrOEPorts(attrs);
    if (nrOEs == 0 || portIndex < 0) return null;
    if (portIndex >= nrOEs) return null;
    int ypos = 60;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES) && classicAppearance(attrs))
      ypos = 20;
    Port result = new Port(0,ypos,Port.INPUT,1);
    result.setToolTip(S.getter("ramOETip"));
    return result;
  }
  private static Port getWEPort(int portIndex, AttributeSet attrs) {
    int nrWEs = getNrWEPorts(attrs);
    if (nrWEs == 0 || portIndex < 0) return null;
    if (portIndex >= nrWEs) return null;
    int ypos = 50;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES) && classicAppearance(attrs))
      ypos = 30;
    Port result = new Port(0,ypos,Port.INPUT,1);
    result.setToolTip(S.getter("ramWETip"));
    return result;
  }
  private static Port getClkPort(int portIndex, AttributeSet attrs) {
    int nrClks = getNrClkPorts(attrs);
    if (nrClks == 0 || portIndex < 0) return null;
    if (portIndex >= nrClks) return null;
    int ypos = 70;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES) && classicAppearance(attrs))
      ypos = 40;
    ypos += getNrLEPorts(attrs)*10;
    ypos += getNrBEPorts(attrs)*10;
    Port result = new Port(0,ypos,Port.INPUT,1);
    result.setToolTip(S.getter("ramClkTip"));
    return result;
  }
  private static Port getLEPort(int portIndex, AttributeSet attrs) {
    int nrLEs = getNrLEPorts(attrs);
    if (nrLEs == 0 || portIndex < 0) return null;
    if (portIndex >= nrLEs) return null;
    int ypos = 70;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES) && classicAppearance(attrs))
      ypos = 40;
    ypos += portIndex*10;
    Port result = new Port(0,ypos,Port.INPUT,1);
    switch (portIndex) {
      case 0 : result.setToolTip(S.getter("ramLETip0"));
               break;
      case 1 : result.setToolTip(S.getter("ramLETip1"));
               break;
      case 2 : result.setToolTip(S.getter("ramLETip2"));
               break;
      case 3 : result.setToolTip(S.getter("ramLETip3"));
               break;
    }
    return result;
  }
  private static Port getBEPort(int portIndex, AttributeSet attrs) {
    int nrBEs = getNrBEPorts(attrs);
    if (nrBEs == 0 || portIndex < 0) return null;
    if (portIndex >= nrBEs) return null;
    int ypos = 70 + (nrBEs-portIndex-1)*10;
    Port result = new Port(0,ypos,Port.INPUT,1);
    switch (portIndex) {
      case 0 : result.setToolTip(S.getter("ramByteEnableTip0"));
               break;
      case 1 : result.setToolTip(S.getter("ramByteEnableTip1"));
               break;
      case 2 : result.setToolTip(S.getter("ramByteEnableTip2"));
               break;
      case 3 : result.setToolTip(S.getter("ramByteEnableTip3"));
               break;
    }
    return result;
  }
  private static Port getClrPort(int portIndex, AttributeSet attrs) {
    if (getNrClrPorts(attrs) == 0 || portIndex != 0) return null;
    Port result = new Port(40,0,Port.INPUT,1);
    result.setToolTip(S.getter("ramClrPin"));
    return result;
  }

  private static boolean seperatedBus(AttributeSet attrs) {
    Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
    return (bus != null && bus.equals(RamAttributes.BUS_SEP));
  }
  private static boolean synchronous(AttributeSet attrs) {
    return attrs.containsAttribute(StdAttr.TRIGGER) && 
    	   (attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_RISING) ||
            attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_FALLING));
  }
  
  private static void DrawConnections(Instance inst, AttributeSet attrs, InstancePainter painter) {
    boolean classic = classicAppearance(attrs);
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    Font font = g.getFont();
    g.setStroke(new BasicStroke(4));
    String label;
    int nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    int nrOfDataPorts = Math.max(getNrDataInPorts(attrs), getNrDataOutPorts(attrs));
    for (int i = 0 ; i < getNrDataInPorts(attrs) ; i++) {
      label = !classic ? "" : getNrDataInPorts(attrs) == 1 ? "D" : "D"+i;
      int idx = getDataInIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        int x = loc.getX();
        int y = loc.getY();
        if (nrOfBits == 1) {
          g.setStroke(new BasicStroke(2));
       if (nrOfDataPorts > 1) {
            int[] xpos = new int[4];
            int[] ypos = new int[4];
            xpos[0] = x;
            xpos[1] = xpos[2] = x+4+i*4;
            xpos[3] = x+20;
            ypos[0] = ypos[1] = y;
            ypos[2] = ypos[3] = y-(i+1)*6;
            g.drawPolyline(xpos, ypos, 4);
          } else {
            g.drawLine(x, y, x+20, y);
          }
       g.setStroke(new BasicStroke(4));
        } else {
          if (i != 0) {
            if (i==3 && nrOfBits == 2)
              g.drawLine(loc.getX(), loc.getY(), loc.getX()+4, loc.getY()-4);
            else
              g.drawLine(loc.getX(), loc.getY(), loc.getX()+4, loc.getY()+4);
          } else {
            int[] xpos = new int[3];
            int[] ypos = new int[3];
            xpos[0] = x;
            xpos[1] = xpos[2] = x+5;
            ypos[0] = y;
            ypos[1] = y+5;
            ypos[2] = y+5+(nrOfBits-1)*20;
            g.drawPolyline(xpos, ypos, 3);
            g.setStroke(new BasicStroke(2));
            xpos[0] = x+5;
            xpos[1] = x+10;
            xpos[2] = x+20;
            ypos[0] = y+5;
            ypos[1] = ypos[2] = y+10;
            g.setFont(font.deriveFont(7.0f));
            for (int j = 0 ; j < nrOfBits; j++) {
              g.drawPolyline(xpos, ypos, 3);
              GraphicsUtil.drawText(
                      g,
                      Integer.toString(j),
                      xpos[2] - 3,
                      ypos[2] - 3,
                      GraphicsUtil.H_RIGHT,
                      GraphicsUtil.V_BASELINE);
              ypos[0] += 20;
              ypos[1] += 20;
              ypos[2] += 20;
            }
           g.setStroke(new BasicStroke(4));
          }
        }
      }
      painter.drawPort(idx, label, Direction.EAST);
   }
    for (int i = 0 ; i < getNrDataOutPorts(attrs) ; i++) {
      label = !classic ? "" : getNrDataOutPorts(attrs) == 1 ? "D" : "D"+i;
      int idx = getDataOutIndex(i,attrs);
      if (!classic) {
        boolean seperate = seperatedBus(attrs) || !attrs.containsAttribute(RamAttributes.ATTR_DBUS);
        Location loc = inst.getPortLocation(idx);
        int x = loc.getX();
        int y = loc.getY();
        if (nrOfBits == 1) {
          g.setStroke(new BasicStroke(2));
         if (nrOfDataPorts > 1) {
            int[] xpos = new int[4];
            int[] ypos = new int[4];
            xpos[0] = x;
            xpos[1] = xpos[2] = x-(i+1)*4;
            xpos[3] = x-20;
            ypos[0] = ypos[1] = y;
            ypos[2] = ypos[3] = y-(i+1)*6;
            g.drawPolyline(xpos, ypos, 4);
          } else {
            g.drawLine(x, y, x-20, y);
          }
         if (!seperate && i==0) DrawBidir(g,x-20,y);
         g.setStroke(new BasicStroke(4));
        } else {
          if (i != 0) {
            if (i==3 && nrOfBits == 2)
              g.drawLine(x-4, y-4, x, y);
            else
              g.drawLine(x-4, y+4, x, y);
          } else {
            int[] xpos = new int[3];
            int[] ypos = new int[3];
            xpos[0] = x;
            xpos[1] = xpos[2] = x-5;
            ypos[0] = y;
            ypos[1] = y+5;
            ypos[2] = y+5+(nrOfBits-1)*20;
            g.drawPolyline(xpos, ypos, 3);
            g.setStroke(new BasicStroke(2));
            xpos[0] = x-5;
            xpos[1] = x-10;
            xpos[2] = x-20;
            ypos[0] = y+5;
            ypos[1] = ypos[2] = y+10;
            g.setFont(font.deriveFont(7.0f));
            for (int j = 0 ; j < nrOfBits; j++) {
              g.drawPolyline(xpos, ypos, 3);
              GraphicsUtil.drawText(
                      g,
                      Integer.toString(j),
                      xpos[2] + 3,
                      ypos[2] - 3,
                      GraphicsUtil.H_LEFT,
                      GraphicsUtil.V_BASELINE);
              if (!seperate) DrawBidir(g,xpos[2],ypos[2]);
              ypos[0] += 20;
              ypos[1] += 20;
              ypos[2] += 20;
            }
           g.setStroke(new BasicStroke(4));
          }
        }
      }
      painter.drawPort(idx, label, Direction.WEST);
    }
    for (int i = 0 ; i < getNrAddrPorts(attrs); i++) {
      label = !classic ? "" : getNrAddrPorts(attrs) == 1 ? "A" : "A"+i;
      int idx = getAddrIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        int x = loc.getX();
        int y = loc.getY();
        int[] xpos = new int[3];
        int[] ypos = new int[3];
        xpos[0] = x;
        xpos[1] = xpos[2] = x+5;
        ypos[0] = y;
        ypos[1] = y+5;
        ypos[2] = y+25;
        g.drawPolyline(xpos, ypos, 3);
        g.setStroke(new BasicStroke(2));
        xpos[0] = x+5;
        xpos[1] = x+10;
        xpos[2] = x+20;
        ypos[0] = y+5;
        ypos[1] = ypos[2] = y+10;
        g.drawPolyline(xpos, ypos, 3);
        for (int j = 0 ; j < 3 ; j++) {
          ypos[j] += 20;
          if (attrs.getValue(Mem.ADDR_ATTR).getWidth() > 2) g.drawLine(x+15, y+13+j*6, x+15, y+15+j*6);
        }
        g.drawPolyline(xpos, ypos, 3);
      }
      painter.drawPort(idx, label, Direction.EAST);
    }
    g.setStroke(new BasicStroke(2));
    for (int i = 0 ; i < getNrOEPorts(attrs); i++) {
      label = !classic ? "" : getNrOEPorts(attrs) == 1 ? "OE" : "OE"+i;
      int idx = getOEIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX()+20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }
    for (int i = 0 ; i < getNrWEPorts(attrs) ; i++) {
      label = !classic ? "" : getNrWEPorts(attrs) == 1 ? "WE" : "WE"+i;
      int idx = getWEIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX()+20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }
    for (int i = 0 ; i < getNrClkPorts(attrs) ; i++) {
      int idx = getClkIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        int xend = 20;
        if (attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_FALLING) ||
        attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_LOW)) {
          xend -= 8;
          g.drawOval(loc.getX()+12, loc.getY()-4, 8, 8);
        }
        g.drawLine(loc.getX(), loc.getY(), loc.getX()+xend, loc.getY());
        if (synchronous(attrs)) painter.drawClockSymbol(loc.getX()+20, loc.getY());
        painter.drawPort(idx);
      } else {
        if (synchronous(attrs)) 
          painter.drawClock(idx, Direction.EAST);
        else
          painter.drawPort(idx, getNrClkPorts(attrs) == 1 ? "E" : "E"+i, Direction.EAST);
      }
    }
    for (int i = 0 ; i < getNrLEPorts(attrs) ; i++) {
      label = !classic ? "" : getNrLEPorts(attrs) == 1 ? "LE" : "LE"+i;
      int idx = getLEIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX()+20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }
    for (int i = 0 ; i < getNrBEPorts(attrs) ; i++){
      label = !classic ? "" : getNrBEPorts(attrs) == 1 ? "BE" : "BE"+i;
      int idx = getBEIndex(i,attrs);
      if (!classic) {
        Location loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX()+20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }
    for (int i = 0 ; i < getNrClrPorts(attrs); i++) {
      int idx = getClrIndex(i,attrs);
      painter.drawPort(idx);
    }
    g.dispose();
  }
  
  private static void DrawControlBlock(Instance inst, AttributeSet attrs, InstancePainter painter) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    int[] xpos = new int[8];
    int[] ypos = new int[8];
    int x = painter.getBounds().getX();
    int y = painter.getBounds().getY();
    xpos[0] = xpos[1] = x+30;
    xpos[2] = xpos[3] = x+20;
    xpos[4] = xpos[5] = x+Mem.SymbolWidth+20;
    xpos[6] = xpos[7] = x+Mem.SymbolWidth+10;
    ypos[0] = ypos[7] = y+getControlHeight(attrs);
    ypos[1] = ypos[2] = ypos[5] = ypos[6] = ypos[0]-10;
    ypos[3] = ypos[4] = y;
    g.setStroke(new BasicStroke(2));
    g.drawPolyline(xpos, ypos, 8);
    for (int i = 0 ; i < getNrAddrPorts(attrs) ; i++) {
      int idx = getAddrIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      DrawAddress(g,loc.getX(),loc.getY(),attrs.getValue(Mem.ADDR_ATTR).getWidth());
    }
    int cidx = 1;
    for (int i = 0; i < getNrClkPorts(attrs) ; i++) {
      int idx = getClkIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      String label = synchronous(attrs) ? "C"+cidx : "E"+cidx;
      cidx++;
      g.drawString(label, loc.getX()+33, loc.getY()+5);
    }
    for (int i = 0 ; i < getNrOEPorts(attrs); i++) {
      int idx = getOEIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      String label = "M"+cidx+" [Output enable]";
      cidx++;
      g.drawString(label, loc.getX()+33, loc.getY()+5);
    }
    for (int i = 0 ; i < getNrWEPorts(attrs); i++) {
      int idx = getWEIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      String label = "M"+cidx+" [Write enable]";
      cidx++;
      g.drawString(label, loc.getX()+33, loc.getY()+5);
    }
    for (int i = 0 ; i < getNrLEPorts(attrs); i++) {
      int idx = getLEIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      String label = "M"+cidx+" [Line enable "+i+"]";
      cidx++;
      g.drawString(label, loc.getX()+33, loc.getY()+5);
    }
    for (int i = 0 ; i < getNrBEPorts(attrs); i++) {
      int idx = getBEIndex(i,attrs);
      Location loc = inst.getPortLocation(idx);
      String label = "M"+cidx+" [Byte enable "+i+"]";
      cidx++;
      g.drawString(label, loc.getX()+33, loc.getY()+5);
    }
    g.dispose();
  }
  
  private static void DrawDataBlocks(Instance inst, AttributeSet attrs, InstancePainter painter) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    int x = painter.getBounds().getX()+20;
    int y = painter.getBounds().getY()+getControlHeight(attrs);
    int width = Mem.SymbolWidth;
    int height = 20;
    g.setFont(g.getFont().deriveFont(9.0f));
    int nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    StringBuffer doutLabel = new StringBuffer();
    StringBuffer dinLabel = new StringBuffer();
    doutLabel.append("A");
    dinLabel.append("A");
    int cidx = 1;
    boolean async = !synchronous(attrs) || 
    		        (attrs.containsAttribute(Mem.ASYNC_READ) && attrs.getValue(Mem.ASYNC_READ));
    boolean drawDin = attrs.containsAttribute(RamAttributes.ATTR_DBUS);
    boolean seperate = seperatedBus(attrs) || !drawDin;
    for (int i = 0 ; i < getNrClkPorts(attrs) ; i++) {
      if (!async) doutLabel.append(","+cidx);
      dinLabel.append(","+cidx);
      cidx++;
    }
    for (int i = 0 ; i < getNrOEPorts(attrs); i++) {
      doutLabel.append(","+cidx);
      cidx++;
    }
    for (int i = 0 ; i < getNrWEPorts(attrs); i++) {
      dinLabel.append(","+cidx);
      cidx++;
    }
    for (int i = 0 ; i < getNrLEPorts(attrs); i++) {
      dinLabel.append(","+cidx);
      cidx++;
    }
    boolean appendBE = getNrBEPorts(attrs) > 0;
    String DLabel = seperate ? "" : "D";
    for (int i = 0 ; i < nrOfBits ; i++) {
      g.setStroke(new BasicStroke(2));
      g.drawRect(x, y, width, height);
      g.setStroke(new BasicStroke(1));
      GraphicsUtil.drawText(
              g,
              doutLabel.toString(),
              x - (seperate ? 3 : 10) + Mem.SymbolWidth,
              y + (seperate ? 10 : 5),
              GraphicsUtil.H_RIGHT,
              GraphicsUtil.V_CENTER);
      if (!seperate) {
        int[] xpos = new int[3];
        int[] ypos = new int[3];
        xpos[0] = x-8+Mem.SymbolWidth;
        xpos[1] = x-5+Mem.SymbolWidth;
        xpos[2] = x-2+Mem.SymbolWidth;
        ypos[0] = ypos[2] = y+5;
        ypos[1] = y+8;
        g.drawPolygon(xpos, ypos, 3);
      }
      String BEIndex = "";
      if (appendBE) {
        int beIdx = cidx+(i>>3);
        BEIndex = ","+beIdx;
      }
      if (drawDin)
        GraphicsUtil.drawText(
              g,
              dinLabel.toString()+BEIndex+DLabel,
              x + (seperate ? 3 :  Mem.SymbolWidth-3),
              y + (seperate ? 10 : 13),
              seperate ? GraphicsUtil.H_LEFT : GraphicsUtil.H_RIGHT,
              GraphicsUtil.V_CENTER);
      y += 20;
    }
    g.dispose();
  }
  
  private static void DrawBidir(Graphics2D g, int x , int y) {
    int[] xpos = new int[4];
    int[] ypos = new int[4];
    xpos[0] = xpos[3] = x-10;
    xpos[1] = xpos[2] = x;
    ypos[0] = ypos[1] = y-5;
    ypos[2] = ypos[3] = y+5;
    g.drawPolyline(xpos, ypos, 4);
    xpos[0] = xpos[2] = x-4;
    xpos[1] = x-8;
    ypos[0] = y+2;
    ypos[1] = y+5;
    ypos[2] = y+8;
    g.drawPolyline(xpos, ypos, 3);
    xpos[0] = xpos[2] = x-6;
    xpos[1] = x-2;
    ypos[0] = y-8;
    ypos[1] = y-5;
    ypos[2] = y-2;
    g.drawPolyline(xpos, ypos, 3);
  }
  
  private static void DrawAddress(Graphics2D g, int xpos, int ypos, int NrAddressBits) {
    GraphicsUtil.switchToWidth(g, 1);
    GraphicsUtil.drawText(g, "0", xpos + 22, ypos + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(
        g,
        Integer.toString(NrAddressBits - 1),
        xpos + 22,
        ypos + 30,
        GraphicsUtil.H_LEFT,
        GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, "A", xpos + 50, ypos + 20, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    g.drawLine(xpos + 40, ypos + 5, xpos + 45, ypos + 10);
    g.drawLine(xpos + 45, ypos + 10, xpos + 45, ypos + 17);
    g.drawLine(xpos + 45, ypos + 17, xpos + 48, ypos + 20);
    g.drawLine(xpos + 48, ypos + 20, xpos + 45, ypos + 23);
    g.drawLine(xpos + 45, ypos + 23, xpos + 45, ypos + 30);
    g.drawLine(xpos + 40, ypos + 35, xpos + 45, ypos + 30);
    String size = Long.toString((1 << NrAddressBits) - 1);
    Font font = g.getFont();
    FontMetrics fm = g.getFontMetrics(font);
    int StrSize = fm.stringWidth(size);
    g.drawLine(xpos + 60, ypos + 20, xpos + 60 + StrSize, ypos + 20);
    GraphicsUtil.drawText(
        g, "0", xpos + 60 + (StrSize / 2), ypos + 19, GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
    GraphicsUtil.drawText(
        g, size, xpos + 60 + (StrSize / 2), ypos + 21, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
  }

}
