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

package com.cburch.logisim.fpga.data;

import static com.cburch.logisim.fpga.Strings.S;

import java.awt.Graphics2D;

import javax.swing.JPanel;

import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.gui.generic.OptionPane;

public class ConstantButton extends FPGAIOInformationContainer {

  public static final int CONSTANT_ZERO = 0;
  public static final int CONSTANT_ONE = 1;
  public static final int CONSTANT_VALUE = 2;
  public static final int LEAVE_OPEN = 3;
	  
  public static final ConstantButton ZERO_BUTTON = new ConstantButton(CONSTANT_ZERO);
  public static final ConstantButton ONE_BUTTON = new ConstantButton(CONSTANT_ONE);
  public static final ConstantButton VALUE_BUTTON = new ConstantButton(CONSTANT_VALUE);
  public static final ConstantButton OPEN_BUTTON = new ConstantButton(LEAVE_OPEN);
   
  private int myType;
  
  public ConstantButton(int type) {
    super();
    myType = type;
    MyRectangle = new BoardRectangle(type*BoardManipulator.CONSTANT_BUTTON_WIDTH,
        BoardManipulator.IMAGE_HEIGHT, BoardManipulator.CONSTANT_BUTTON_WIDTH, BoardManipulator.CONSTANT_BAR_HEIGHT);
  }

  @Override
  public boolean tryMap(JPanel parent) {
    if (selComp == null) return false;
    MapComponent map = selComp.getMap();
    switch (myType) {
      case CONSTANT_ZERO : return map.tryConstantMap(selComp.getPin(), 0L);
      case CONSTANT_ONE  : return map.tryConstantMap(selComp.getPin(), -1L);
      case LEAVE_OPEN    : return map.tryOpenMap(selComp.getPin());
      case CONSTANT_VALUE: return getConstant(selComp.getPin(),map);
    }
    return false;
  }
  
  private boolean getConstant(int pin, MapComponent map) {
    Long v = 0L;
    boolean correct;
    do {
      correct = true;
      String Value = OptionPane.showInputDialog(S.get("FpgaMapSpecConst"));
      if (Value == null) return false;
      if (Value.startsWith("0x")) {
        try {
          v = Long.parseLong(Value.substring(2), 16);
        } catch (NumberFormatException e1) {
          correct = false;
        }
      } else {
        try {
          v = Long.parseLong(Value);
        } catch (NumberFormatException e) {
          correct = false;
        }
      }
      if (!correct) OptionPane.showMessageDialog(null, S.get("FpgaMapSpecErr"));
    } while (!correct);
    return map.tryConstantMap(pin, v);
  }
  
  @Override
  public void paint(Graphics2D g , float scale) {
    super.paintselected(g, scale);
  }
  
  @Override
  public boolean setSelectable(MapListModel.MapInfo comp) {
    selComp = comp;
    MapComponent map = comp.getMap();
    int connect = comp.getPin();
    if (connect < 0) {
      if (map.hasInputs()) {
        switch (myType) {
          case CONSTANT_ONE  :
          case CONSTANT_ZERO : selectable = true;
                               break;
          case CONSTANT_VALUE: selectable = map.nrInputs() > 1;
        }
      }
      if (map.hasOutputs() || map.hasIOs()) selectable = myType == LEAVE_OPEN;
    } else {
      if (map.isInput(connect)) 
        selectable = myType == CONSTANT_ZERO || myType == CONSTANT_ONE;
      if (map.isOutput(connect) || map.isIO(connect))
    	selectable = myType == LEAVE_OPEN;
    }
    return selectable;
  }
}
