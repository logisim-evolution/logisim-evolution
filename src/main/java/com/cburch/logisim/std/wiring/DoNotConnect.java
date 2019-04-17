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

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.gates.AbstractGateHDLGenerator;
import com.cburch.logisim.tools.key.BitWidthConfigurator;

public class DoNotConnect extends InstanceFactory {
	
  private class DoNotConnectGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
	  @Override
	  public boolean IsOnlyInlined(String HDLType) {
	    return true;
	  }
  }

  public DoNotConnect() {
    super("NoConnect", S.getter("noConnectionComponent"));
    setIconName("noconnect.gif");
    setAttributes(new Attribute[] {StdAttr.WIDTH},new Object[] {BitWidth.ONE});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setPorts(new Port[] {new Port(0, 0, Port.INOUT, StdAttr.WIDTH)});
  }
  
  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    Location loc = painter.getLocation();
    g.setColor(isGhost ? Color.GRAY : Color.RED);
    g.drawLine(loc.getX()-5, loc.getY()-5, loc.getX()+5, loc.getY()+5);
    g.drawLine(loc.getX()-5, loc.getY()+5, loc.getX()+5, loc.getY()-5);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(-5, -5, 10, 10);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) { }
	
  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new DoNotConnectGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

}
