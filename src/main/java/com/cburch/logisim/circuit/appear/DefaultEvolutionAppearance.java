/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.shapes.Text;
import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEvolutionAppearance {

  private static final int OFFS = 50;

  private DefaultEvolutionAppearance() {}

  public static List<CanvasObject> build(
      Collection<Instance> pins, String CircuitName, boolean FixedSize) {
    Map<Direction, List<Instance>> edge;
    edge = new HashMap<>();
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    int MaxLeftLabelLength = 0;
    int MaxRightLabelLength = 0;
    int TitleWidth =
        (CircuitName == null)
            ? 14 * DrawAttr.FIXED_FONT_CHAR_WIDTH
            : CircuitName.length() * DrawAttr.FIXED_FONT_CHAR_WIDTH;

    if (!pins.isEmpty()) {
      for (Instance pin : pins) {
        Direction pinEdge;
        Text label = new Text(0, 0, pin.getAttributeValue(StdAttr.LABEL));
        int LabelWidth = label.getText().length() * DrawAttr.FIXED_FONT_CHAR_WIDTH;
        if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
          pinEdge = Direction.EAST;
          if (LabelWidth > MaxRightLabelLength) MaxRightLabelLength = LabelWidth;
        } else {
          pinEdge = Direction.WEST;
          if (LabelWidth > MaxLeftLabelLength) MaxLeftLabelLength = LabelWidth;
        }
        List<Instance> e = edge.get(pinEdge);
        e.add(pin);
      }
    }

    for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    int numEast = edge.get(Direction.EAST).size();
    int numWest = edge.get(Direction.WEST).size();
    int maxVert = Math.max(numEast, numWest);

    int dy = ((DrawAttr.FIXED_FONT_HEIGHT + (DrawAttr.FIXED_FONT_HEIGHT >> 2) + 5) / 10) * 10;
    int textWidth =
        (FixedSize)
            ? 25 * DrawAttr.FIXED_FONT_CHAR_WIDTH
            : Math.max((MaxLeftLabelLength + MaxRightLabelLength + 35), (TitleWidth + 15));
    int Thight = ((DrawAttr.FIXED_FONT_HEIGHT + 10) / 10) * 10;
    int width = (textWidth / 10) * 10 + 20;
    int height = (maxVert > 0) ? maxVert * dy + Thight : 10 + Thight;
    int sdy = (DrawAttr.FIXED_FONT_ASCENT - DrawAttr.FIXED_FONT_DESCENT) >> 1;

    // compute position of anchor relative to top left corner of box
    int ax;
    int ay;
    if (numEast > 0) { // anchor is on east side
      ax = width;
      ay = 10;
    } else if (numWest > 0) { // anchor is on west side
      ax = 0;
      ay = 10;
    } else { // anchor is top left corner
      ax = 0;
      ay = 0;
    }

    // place rectangle so anchor is on the grid
    int rx = OFFS + (9 - (ax + 9) % 10);
    int ry = OFFS + (9 - (ay + 9) % 10);

    List<CanvasObject> ret = new ArrayList<>();
    placePins(ret, edge.get(Direction.WEST), rx, ry + 10, 0, dy, true, sdy, FixedSize);
    placePins(ret, edge.get(Direction.EAST), rx + width, ry + 10, 0, dy, false, sdy, FixedSize);
    Rectangle rect = new Rectangle(rx + 10, ry + height - Thight, width - 20, Thight);
    rect.setValue(DrawAttr.STROKE_WIDTH, 1);
    rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
    rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
    ret.add(rect);
    rect = new Rectangle(rx + 10, ry, width - 20, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);
    ret.add(rect);
    String Label = CircuitName == null ? "VHDL Component" : CircuitName;
    if (FixedSize) {
      if (Label.length() > 23) {
        Label = Label.substring(0, 20);
        Label = Label.concat("...");
      }
    }
    Text label = new Text(rx + (width >> 1), ry + (height - DrawAttr.FIXED_FONT_DESCENT - 5), Label);
    label.getLabel().setHorizontalAlignment(EditableLabel.CENTER);
    label.getLabel().setColor(Color.WHITE);
    label.getLabel().setFont(DrawAttr.DEFAULT_NAME_FONT);
    ret.add(label);
    ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
    return ret;
  }

  private static void placePins(
      List<CanvasObject> dest,
      List<Instance> pins,
      int x,
      int y,
      int dx,
      int dy,
      boolean LeftSide,
      int ldy,
      boolean FixedSize) {
    int halign;
    Color color = Color.DARK_GRAY;
    int ldx;
    for (Instance pin : pins) {
      int offset =
          (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1)
              ? Wire.WIDTH_BUS >> 1
              : Wire.WIDTH >> 1;
      int height =
          (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) ? Wire.WIDTH_BUS : Wire.WIDTH;
      Rectangle rect;
      if (LeftSide) {
        ldx = 15;
        halign = EditableLabel.LEFT;
        rect = new Rectangle(x, y - offset, 10, height);
      } else {
        ldx = -15;
        halign = EditableLabel.RIGHT;
        rect = new Rectangle(x - 10, y - offset, 10, height);
      }
      rect.setValue(DrawAttr.STROKE_WIDTH, 1);
      rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
      rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
      dest.add(rect);
      dest.add(new AppearancePort(Location.create(x, y), pin));
      if (pin.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
        String Label = pin.getAttributeValue(StdAttr.LABEL);
        if (FixedSize) {
          if (Label.length() > 12) {
            Label = Label.substring(0, 9);
            Label = Label.concat("..");
          }
        }
        Text label = new Text(x + ldx, y + ldy, Label);
        label.getLabel().setHorizontalAlignment(halign);
        label.getLabel().setColor(color);
        label.getLabel().setFont(DrawAttr.DEFAULT_FIXED_PICH_FONT);
        dest.add(label);
      }
      x += dx;
      y += dy;
    }
  }
}
