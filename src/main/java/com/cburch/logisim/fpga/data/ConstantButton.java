/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class ConstantButton extends FpgaIoInformationContainer {

  public static final int CONSTANT_ZERO = 0;
  public static final int CONSTANT_ONE = 1;
  public static final int CONSTANT_VALUE = 2;
  public static final int LEAVE_OPEN = 3;

  public static final ConstantButton ZERO_BUTTON = new ConstantButton(CONSTANT_ZERO);
  public static final ConstantButton ONE_BUTTON = new ConstantButton(CONSTANT_ONE);
  public static final ConstantButton VALUE_BUTTON = new ConstantButton(CONSTANT_VALUE);
  public static final ConstantButton OPEN_BUTTON = new ConstantButton(LEAVE_OPEN);

  private final int myType;

  public ConstantButton(int type) {
    super();
    myType = type;
    myRectangle =
        new BoardRectangle(
            type * BoardManipulator.CONSTANT_BUTTON_WIDTH,
            BoardManipulator.IMAGE_HEIGHT,
            BoardManipulator.CONSTANT_BUTTON_WIDTH,
            BoardManipulator.CONSTANT_BAR_HEIGHT);
  }

  @Override
  public boolean tryMap(JPanel parent) {
    if (selComp == null) return false;
    final var map = selComp.getMap();
    return switch (myType) {
      case CONSTANT_ZERO -> map.tryConstantMap(selComp.getPin(), 0L);
      case CONSTANT_ONE  -> map.tryConstantMap(selComp.getPin(), -1L);
      case LEAVE_OPEN    -> map.tryOpenMap(selComp.getPin());
      case CONSTANT_VALUE ->  getConstant(selComp.getPin(), map);
      default -> false;
    };
  }

  private boolean getConstant(int pin, MapComponent map) {
    var v = 0L;
    boolean correct;
    do {
      correct = true;
      final var value = OptionPane.showInputDialog(S.get("FpgaMapSpecConst"));
      if (value == null) return false;
      if (value.startsWith("0x")) {
        try {
          v = Long.parseLong(value.substring(2), 16);
        } catch (NumberFormatException e1) {
          correct = false;
        }
      } else {
        try {
          v = Long.parseLong(value);
        } catch (NumberFormatException e) {
          correct = false;
        }
      }
      if (!correct) OptionPane.showMessageDialog(null, S.get("FpgaMapSpecErr"));
    } while (!correct);
    return map.tryConstantMap(pin, v);
  }

  @Override
  public void paint(Graphics2D g, float scale) {
    super.paintSelected(g, scale);
  }

  @Override
  public boolean setSelectable(MapListModel.MapInfo comp) {
    selComp = comp;
    final var map = comp.getMap();
    int connect = comp.getPin();
    if (connect < 0) {
      if (map.hasInputs()) {
        selectable = switch (myType) {
          case CONSTANT_ONE -> true;
          case CONSTANT_ZERO -> true;
          case CONSTANT_VALUE -> map.nrInputs() > 1;
          case LEAVE_OPEN -> false;
          default -> throw new IllegalStateException("Unexpected value: " + myType);
        };
      }
      if (map.hasOutputs() || map.hasIos()) selectable = myType == LEAVE_OPEN;
    } else {
      if (map.isInput(connect))
        selectable = myType == CONSTANT_ZERO || myType == CONSTANT_ONE;
      if (map.isOutput(connect) || map.isIo(connect))
        selectable = myType == LEAVE_OPEN;
    }
    return selectable;
  }
}
