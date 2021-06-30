/*
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

  public static List<CanvasObject> build(Collection<Instance> pins, String circuitName, boolean fixedSize) {
    Map<Direction, List<Instance>> edge;
    edge = new HashMap<>();
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    int maxLeftLabelLength = 0;
    int maxRightLabelLength = 0;
    int titleWidth =
        (circuitName == null)
            ? 14 * DrawAttr.FixedFontCharWidth
            : circuitName.length() * DrawAttr.FixedFontCharWidth;

    if (!pins.isEmpty()) {
      for (final var pin : pins) {
        Direction pinEdge;
        final var label = new Text(0, 0, pin.getAttributeValue(StdAttr.LABEL));
        final var labelWidth = label.getText().length() * DrawAttr.FixedFontCharWidth;
        if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
          pinEdge = Direction.EAST;
          if (labelWidth > maxRightLabelLength) maxRightLabelLength = labelWidth;
        } else {
          pinEdge = Direction.WEST;
          if (labelWidth > maxLeftLabelLength) maxLeftLabelLength = labelWidth;
        }
        final var e = edge.get(pinEdge);
        e.add(pin);
      }
    }

    for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    final var numEast = edge.get(Direction.EAST).size();
    final var numWest = edge.get(Direction.WEST).size();
    final var maxVert = Math.max(numEast, numWest);

    final var dy = ((DrawAttr.FixedFontHeight + (DrawAttr.FixedFontHeight >> 2) + 5) / 10) * 10;
    final var textWidth =
        (fixedSize)
            ? 25 * DrawAttr.FixedFontCharWidth
            : Math.max((maxLeftLabelLength + maxRightLabelLength + 35), (titleWidth + 15));
    final var thight = ((DrawAttr.FixedFontHeight + 10) / 10) * 10;
    final var width = (textWidth / 10) * 10 + 20;
    final var height = (maxVert > 0) ? maxVert * dy + thight : 10 + thight;
    final var  sdy = (DrawAttr.FixedFontAscent - DrawAttr.FixedFontDescent) >> 1;

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
    final var  rx = OFFS + (9 - (ax + 9) % 10);
    final var  ry = OFFS + (9 - (ay + 9) % 10);

    final var  ret = new ArrayList<CanvasObject>();
    placePins(ret, edge.get(Direction.WEST), rx, ry + 10, 0, dy, true, sdy, fixedSize);
    placePins(ret, edge.get(Direction.EAST), rx + width, ry + 10, 0, dy, false, sdy, fixedSize);
    var rect = new Rectangle(rx + 10, ry + height - thight, width - 20, thight);
    rect.setValue(DrawAttr.STROKE_WIDTH, 1);
    rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
    rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
    ret.add(rect);
    rect = new Rectangle(rx + 10, ry, width - 20, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);
    ret.add(rect);
    String labelStr = circuitName == null ? "VHDL Component" : circuitName;
    if (fixedSize) {
      if (labelStr.length() > 23) {
        labelStr = labelStr.substring(0, 20);
        labelStr = labelStr.concat("...");
      }
    }

    final var label = new Text(rx + (width >> 1), ry + (height - DrawAttr.FixedFontDescent - 5), labelStr);
    label.getLabel().setHorizontalAlignment(EditableLabel.CENTER);
    label.getLabel().setColor(Color.WHITE);
    label.getLabel().setFont(DrawAttr.DEFAULT_NAME_FONT);
    ret.add(label);
    ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
    return ret;
  }

  private static void placePins(List<CanvasObject> dest, List<Instance> pins, int x, int y, int dx, int dy, boolean leftSide, int ldy, boolean fixedSize) {
    int halign;
    final var color = Color.DARK_GRAY;
    int ldx;
    for (final var pin : pins) {
      final var  offset = (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1)
                                ? Wire.WIDTH_BUS >> 1
                                : Wire.WIDTH >> 1;
      final var  height = (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) ? Wire.WIDTH_BUS : Wire.WIDTH;
      Rectangle rect;
      if (leftSide) {
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
        var labelValue = pin.getAttributeValue(StdAttr.LABEL);
        if (fixedSize) {
          if (labelValue.length() > 12) {
            labelValue = labelValue.substring(0, 9);
            labelValue = labelValue.concat("..");
          }
        }
        final var label = new Text(x + ldx, y + ldy, labelValue);
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
