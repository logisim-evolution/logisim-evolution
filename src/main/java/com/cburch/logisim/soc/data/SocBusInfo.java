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

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.GraphicsUtil;

public class SocBusInfo {
  private String busId;
  private SocSimulationManager socManager;
  private Component myComp;
    
  public SocBusInfo( String id) {
    busId = id;
    socManager = null;
  }
    
  public void setBusId(String value) {
    busId = value;
  }
    
  public String getBusId() {
    return busId;
  }
    
  public void setSocSimulationManager( SocSimulationManager man , Component comp ) {
    socManager = man;
    myComp = comp;
  }
    
  public SocSimulationManager getSocSimulationManager() {
    return socManager;
  }
  
  public Component getComponent() {
    return myComp;
  }

  public void paint(Graphics g, Bounds b) {
    String Ident = socManager == null ? null : socManager.getSocBusDisplayString(busId);
	Color c = Ident == null ? Color.RED : Color.GREEN;
    g.setColor(c);
    g.fillRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
    g.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(g, Ident == null ? S.get("SocBusNotConnected") : Ident, b.getCenterX(), b.getCenterY());
  }

}
