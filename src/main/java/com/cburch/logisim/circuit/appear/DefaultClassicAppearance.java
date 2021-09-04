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
import com.cburch.draw.shapes.Curve;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.val;

public class DefaultClassicAppearance {

  private static final int OFFS = 50;

  public static List<CanvasObject> build(Collection<Instance> pins) {
    val edge = new HashMap<Direction, List<Instance>>();
    edge.put(Direction.NORTH, new ArrayList<>());
    edge.put(Direction.SOUTH, new ArrayList<>());
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    for (val pin : pins) {
      val pinFacing = pin.getAttributeValue(StdAttr.FACING);
      val pinEdge = pinFacing.reverse();
      val e = edge.get(pinEdge);
      e.add(pin);
    }

    for (val entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    val numNorth = edge.get(Direction.NORTH).size();
    val numSouth = edge.get(Direction.SOUTH).size();
    val numEast = edge.get(Direction.EAST).size();
    val numWest = edge.get(Direction.WEST).size();
    val maxVert = Math.max(numNorth, numSouth);
    val maxHorz = Math.max(numEast, numWest);

    val offsNorth = computeOffset(numNorth, numSouth, maxHorz);
    val offsSouth = computeOffset(numSouth, numNorth, maxHorz);
    val offsEast = computeOffset(numEast, numWest, maxVert);
    val offsWest = computeOffset(numWest, numEast, maxVert);

    val width = computeDimension(maxVert, maxHorz);
    val height = computeDimension(maxHorz, maxVert);

    // compute position of anchor relative to top left corner of box
    int ax;
    int ay;
    if (numEast > 0) { // anchor is on east side
      ax = width;
      ay = offsEast;
    } else if (numNorth > 0) { // anchor is on north side
      ax = offsNorth;
      ay = 0;
    } else if (numWest > 0) { // anchor is on west side
      ax = 0;
      ay = offsWest;
    } else if (numSouth > 0) { // anchor is on south side
      ax = offsSouth;
      ay = height;
    } else { // anchor is top left corner
      ax = 0;
      ay = 0;
    }

    // place rectangle so anchor is on the grid
    val rx = OFFS + (9 - (ax + 9) % 10);
    val ry = OFFS + (9 - (ay + 9) % 10);

    val e0 = Location.create(rx + (width - 8) / 2, ry + 1);
    val e1 = Location.create(rx + (width + 8) / 2, ry + 1);
    val ct = Location.create(rx + width / 2, ry + 11);
    val notch = new Curve(e0, e1, ct);
    notch.setValue(DrawAttr.STROKE_WIDTH, 2);
    notch.setValue(DrawAttr.STROKE_COLOR, Color.GRAY);
    val rect = new Rectangle(rx, ry, width, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);

    val ret = new ArrayList<CanvasObject>();
    ret.add(notch);
    ret.add(rect);
    placePins(ret, edge.get(Direction.WEST), rx, ry + offsWest, 0, 10);
    placePins(ret, edge.get(Direction.EAST), rx + width, ry + offsEast, 0, 10);
    placePins(ret, edge.get(Direction.NORTH), rx + offsNorth, ry, 10, 0);
    placePins(ret, edge.get(Direction.SOUTH), rx + offsSouth, ry + height, 10, 0);
    ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
    return ret;
  }

  private static int computeDimension(int maxThis, int maxOthers) {
    if (maxThis < 3) {
      return 30;
    } else if (maxOthers == 0) {
      return 10 * maxThis;
    } else {
      return 10 * maxThis + 10;
    }
  }

  private static int computeOffset(int numFacing, int numOpposite, int maxOthers) {
    int maxThis = Math.max(numFacing, numOpposite);
    int maxOffs;
    switch (maxThis) {
      case 0:
      case 1:
        maxOffs = (maxOthers == 0 ? 15 : 10);
        break;
      case 2:
        maxOffs = 10;
        break;
      default:
        maxOffs = (maxOthers == 0 ? 5 : 10);
    }
    return maxOffs + 10 * ((maxThis - numFacing) / 2);
  }

  private static void placePins(List<CanvasObject> dest, List<Instance> pins, int x, int y, int dx, int dy) {
    for (Instance pin : pins) {
      dest.add(new AppearancePort(Location.create(x, y), pin));
      x += dx;
      y += dy;
    }
  }
}
