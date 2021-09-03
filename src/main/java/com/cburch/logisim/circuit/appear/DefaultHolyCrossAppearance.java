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
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultHolyCrossAppearance {
  // Precise font dimensions vary based on the platform. We need component
  // widths to be stable, so we approximate the widths when sizing
  // components.

  public static final int OFFS = 50; // ?
  public static final int LABEL_OUTSIDE = 5; // space from edge of box to label
  public static final int LABEL_GAP = 15; // minimum gap between left and right labels
  public static final int PORT_GAP = 10; // gap between ports
  public static final int TOP_MARGIN = 30; // extra space above ports
  public static final int TOP_TEXT_MARGIN = 5; // space above text label
  public static final int BOTTOM_MARGIN = 5; // extra space below ports

  public static final int MIN_WIDTH = 100; // minimum width (e.g. if no port labels)
  public static final int MIN_HEIGHT = 40; // minimum height (e.g. if no ports)

  // private static int[] asciiWidths = { // 12 point font
  //   4,  5,  5, 10,  8, 11, 10,  3,  // ' ', '!', '"', '#', '$', '%', '&', ''',
  //   5,  5,  6, 10,  4,  4,  4,  4,  // '(', ')', '*', '+', ',', '-', '.', '/',
  //   8,  8,  8,  8,  8,  8,  8,  8,  // '0', '1', '2', '3', '4', '5', '6', '7',
  //   8,  8,  4,  4, 10, 10, 10,  6,  // '8', '9', ':', ';', '<', '=', '>', '?',
  //  13,  8,  8,  8,  9,  8,  7,  9,  // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
  //   9,  3,  3,  7,  6, 10,  9,  9,  // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
  //   8,  9,  8,  8,  7,  9,  8, 11,  // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
  //   7,  7,  9,  5,  4,  5, 10,  6,  // 'X', 'Y', 'Z', '[', '\', ']', '^', '_',
  //   6,  8,  8,  7,  8,  8,  4,  8,  // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
  //   8,  3,  3,  7,  3, 11,  8,  8,  // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
  //   8,  8,  5,  7,  5,  8,  6,  9,  // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
  //   6,  6,  5,  8,  4,  8, 10,      // 'x', 'y', 'z', '{', '|', '}', '~',
  // };

  private static final int[] asciiWidths = { // 10 point font
    3, 4, 5, 8, 6, 10, 9, 3, // ' ', '!', '"', '#', '$', '%', '&', ''',
    4, 4, 5, 8, 3, 4, 3, 3, // '(', ')', '*', '+', ',', '-', '.', '/',
    6, 6, 6, 6, 6, 6, 6, 6, // '0', '1', '2', '3', '4', '5', '6', '7',
    6, 6, 3, 3, 8, 8, 8, 5, // '8', '9', ':', ';', '<', '=', '>', '?',
    11, 7, 7, 8, 8, 7, 6, 8, // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
    8, 3, 3, 7, 6, 9, 8, 8, // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
    7, 8, 7, 7, 5, 8, 7, 9, // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
    6, 7, 6, 4, 3, 4, 8, 5, // 'X', 'Y', 'Z', '[', '\', ']', '^', '_',
    5, 6, 6, 5, 6, 6, 4, 6, // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
    6, 2, 2, 5, 2, 10, 6, 6, // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
    6, 6, 4, 5, 4, 6, 6, 8, // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
    6, 6, 5, 6, 3, 6, // 'x', 'y', 'z', '{', '|', '}',
  };

  public static void calculateTextDimensions(float fontsize) {
    Text label = new Text(0, 0, "a");
    Font f = label.getLabel().getFont().deriveFont(fontsize);
    Canvas canvas = new Canvas();
    FontMetrics fm = canvas.getFontMetrics(f);
    System.out.println("private static int[] asciiWidths = {");
    for (char row = ' '; row <= '~'; row += 8) {
      StringBuilder comment = new StringBuilder("//");
      StringBuilder chars = new StringBuilder("    ");
      for (char c = row; c < row + 8; c++) {
        if (c >= '~') {
          chars.append("    ");
        } else {
          comment.append(String.format(" '%c',", c));
          // label = new Text(0, 0, "" + c);
          // label.getLabel().setFont(f);
          // int w = label.getLabel().getWidth();
          int w = fm.stringWidth("" + c);
          chars.append(String.format(" %2d,", w));
        }
      }
      System.out.println(chars + "  " + comment);
    }
    System.out.println("};");
    System.exit(0);
  }

  private static int textWidth(String s) {
    int w = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= ' ' && c <= '~') {
        w += asciiWidths[c - ' '];
      } else {
        w += 8;
      }
    }
    return w;
  }

  public static List<CanvasObject> build(Collection<Instance> pins, String name) {
    Map<Direction, List<Instance>> edge;
    edge = new HashMap<>();
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    var maxLeftLabelLength = 0;
    var maxRightLabelLength = 0;
    for (Instance pin : pins) {
      Direction pinEdge;
      final var labelString = pin.getAttributeValue(StdAttr.LABEL);
      final var LabelWidth = textWidth(labelString);
      if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
        pinEdge = Direction.EAST;
        if (LabelWidth > maxRightLabelLength) {
          maxRightLabelLength = LabelWidth;
        }
      } else {
        pinEdge = Direction.WEST;
        if (LabelWidth > maxLeftLabelLength) {
          maxLeftLabelLength = LabelWidth;
        }
      }
      List<Instance> e = edge.get(pinEdge);
      e.add(pin);
    }
    for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    int numEast = edge.get(Direction.EAST).size();
    int numWest = edge.get(Direction.WEST).size();
    int maxHorz = Math.max(numEast, numWest);

    int offsEast = computeOffset(numEast, numWest);
    int offsWest = computeOffset(numWest, numEast);

    int width = 2 * LABEL_OUTSIDE + maxLeftLabelLength + maxRightLabelLength + LABEL_GAP;
    width = Math.max(MIN_WIDTH, (width + 9) / 10 * 10);

    int height = PORT_GAP * maxHorz + TOP_MARGIN + BOTTOM_MARGIN;
    height = Math.max(MIN_HEIGHT, height);

    // compute position of anchor relative to top left corner of box
    int ax;
    int ay;
    if (numEast > 0) { // anchor is on east side
      ax = width;
      ay = offsEast;
    } else if (numWest > 0) { // anchor is on west side
      ax = 0;
      ay = offsWest;
    } else { // anchor is top left corner
      ax = 0;
      ay = 0;
    }

    // place rectangle so anchor is on the grid
    int rx = OFFS + (9 - (ax + 9) % 10);
    int ry = OFFS + (9 - (ay + 9) % 10);

    final var rect = new Rectangle(rx, ry, width, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);
    List<CanvasObject> ret = new ArrayList<>();
    ret.add(rect);

    placePins(ret, edge.get(Direction.WEST), rx, ry + offsWest, 0, PORT_GAP, true);
    placePins(ret, edge.get(Direction.EAST), rx + width, ry + offsEast, 0, PORT_GAP, false);

    if (name != null && name.length() > 0) {
      final var label = new Text(rx + width / 2, ry + TOP_TEXT_MARGIN, name);
      label.getLabel().setHorizontalAlignment(EditableLabel.CENTER);
      label.getLabel().setVerticalAlignment(EditableLabel.TOP);
      label.getLabel().setColor(Color.BLACK);
      ret.add(label);
    }

    ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
    return ret;
  }

  private static int computeOffset(int numFacing, int numOpposite) {
    return TOP_MARGIN;
  }

  private static void placePins(
      List<CanvasObject> dest,
      List<Instance> pins,
      int x,
      int y,
      int dx,
      int dy,
      boolean leftSide) {
    int halign;
    final var color = Color.DARK_GRAY; // maybe GRAY instead?
    int ldx;
    for (Instance pin : pins) {
      dest.add(new AppearancePort(Location.create(x, y), pin));
      if (leftSide) {
        ldx = LABEL_OUTSIDE;
        halign = EditableLabel.LEFT;
      } else {
        ldx = -LABEL_OUTSIDE;
        halign = EditableLabel.RIGHT;
      }
      Font pinFont = null;
      if (pin.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
        final var text = pin.getAttributeValue(StdAttr.LABEL);
        if (text != null && text.length() > 0) {
          final var label = new Text(x + ldx, y, text);
          label.getLabel().setHorizontalAlignment(halign);
          label.getLabel().setVerticalAlignment(EditableLabel.MIDDLE);
          label.getLabel().setColor(color);
          if (pinFont == null) {
            pinFont = label.getLabel().getFont().deriveFont((float) 10);
          }
          label.getLabel().setFont(pinFont);
          dest.add(label);
        }
      }
      x += dx;
      y += dy;
    }
  }

  private DefaultHolyCrossAppearance() {
    // dummy, private
  }
}
