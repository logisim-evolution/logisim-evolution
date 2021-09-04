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
import lombok.val;

public class DefaultEvolutionAppearance {

  private static final int OFFS = 50;

  private DefaultEvolutionAppearance() {}

  public static List<CanvasObject> build(Collection<Instance> pins, String CircuitName, boolean FixedSize) {
    val edge = new HashMap<Direction, List<Instance>>();
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    var MaxLeftLabelLength = 0;
    var MaxRightLabelLength = 0;
    val TitleWidth =
        (CircuitName == null)
            ? 14 * DrawAttr.FIXED_FONT_CHAR_WIDTH
            : CircuitName.length() * DrawAttr.FIXED_FONT_CHAR_WIDTH;

    if (!pins.isEmpty()) {
      for (val pin : pins) {
        Direction pinEdge;
        val label = new Text(0, 0, pin.getAttributeValue(StdAttr.LABEL));
        val LabelWidth = label.getText().length() * DrawAttr.FIXED_FONT_CHAR_WIDTH;
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

    for (val entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    val numEast = edge.get(Direction.EAST).size();
    val numWest = edge.get(Direction.WEST).size();
    val maxVert = Math.max(numEast, numWest);

    val dy = ((DrawAttr.FIXED_FONT_HEIGHT + (DrawAttr.FIXED_FONT_HEIGHT >> 2) + 5) / 10) * 10;
    val textWidth =
        (FixedSize)
            ? 25 * DrawAttr.FIXED_FONT_CHAR_WIDTH
            : Math.max((MaxLeftLabelLength + MaxRightLabelLength + 35), (TitleWidth + 15));
    val Thight = ((DrawAttr.FIXED_FONT_HEIGHT + 10) / 10) * 10;
    val width = (textWidth / 10) * 10 + 20;
    val height = (maxVert > 0) ? maxVert * dy + Thight : 10 + Thight;
    val sdy = (DrawAttr.FIXED_FONT_ASCENT - DrawAttr.FIXED_FONT_DESCENT) >> 1;

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
    val rx = OFFS + (9 - (ax + 9) % 10);
    val ry = OFFS + (9 - (ay + 9) % 10);

    val ret = new ArrayList<CanvasObject>();
    placePins(ret, edge.get(Direction.WEST), rx, ry + 10, 0, dy, true, sdy, FixedSize);
    placePins(ret, edge.get(Direction.EAST), rx + width, ry + 10, 0, dy, false, sdy, FixedSize);
    var rect = new Rectangle(rx + 10, ry + height - Thight, width - 20, Thight);
    rect.setValue(DrawAttr.STROKE_WIDTH, 1);
    rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
    rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
    ret.add(rect);
    rect = new Rectangle(rx + 10, ry, width - 20, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);
    ret.add(rect);
    var labelA = CircuitName == null ? "VHDL Component" : CircuitName;
    if (FixedSize && labelA.length() > 23) {
      labelA = labelA.substring(0, 20);
      labelA = labelA.concat("...");
    }
    val labelB = new Text(rx + (width >> 1), ry + (height - DrawAttr.FIXED_FONT_DESCENT - 5), labelA);
    labelB.getLabel().setHorizontalAlignment(EditableLabel.CENTER);
    labelB.getLabel().setColor(Color.WHITE);
    labelB.getLabel().setFont(DrawAttr.DEFAULT_NAME_FONT);
    ret.add(labelB);
    ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
    return ret;
  }

  private static void placePins(List<CanvasObject> dest, List<Instance> pins,
                                int x, int y, int dx, int dy, boolean LeftSide, int ldy, boolean FixedSize) {
    int hAlign;
    val color = Color.DARK_GRAY;
    int ldx;
    for (val pin : pins) {
      int offset =
          (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1)
              ? Wire.WIDTH_BUS >> 1
              : Wire.WIDTH >> 1;
      val height =
          (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) ? Wire.WIDTH_BUS : Wire.WIDTH;
      Rectangle rect;
      if (LeftSide) {
        ldx = 15;
        hAlign = EditableLabel.LEFT;
        rect = new Rectangle(x, y - offset, 10, height);
      } else {
        ldx = -15;
        hAlign = EditableLabel.RIGHT;
        rect = new Rectangle(x - 10, y - offset, 10, height);
      }
      rect.setValue(DrawAttr.STROKE_WIDTH, 1);
      rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
      rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
      dest.add(rect);
      dest.add(new AppearancePort(Location.create(x, y), pin));
      if (pin.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
        var labelA = pin.getAttributeValue(StdAttr.LABEL);
        if (FixedSize) {
          if (labelA.length() > 12) {
            labelA = labelA.substring(0, 9);
            labelA = labelA.concat("..");
          }
        }
        val labelB = new Text(x + ldx, y + ldy, labelA);
        labelB.getLabel().setHorizontalAlignment(hAlign);
        labelB.getLabel().setColor(color);
        labelB.getLabel().setFont(DrawAttr.DEFAULT_FIXED_PICH_FONT);
        dest.add(labelB);
      }
      x += dx;
      y += dy;
    }
  }
}
