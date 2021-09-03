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
import java.util.Map;

public class DefaultClassicAppearance {

  private static final int OFFS = 50;

  public static List<CanvasObject> build(Collection<Instance> pins) {
    Map<Direction, List<Instance>> edge;
    edge = new HashMap<>();
    edge.put(Direction.NORTH, new ArrayList<>());
    edge.put(Direction.SOUTH, new ArrayList<>());
    edge.put(Direction.EAST, new ArrayList<>());
    edge.put(Direction.WEST, new ArrayList<>());
    for (Instance pin : pins) {
      Direction pinFacing = pin.getAttributeValue(StdAttr.FACING);
      Direction pinEdge = pinFacing.reverse();
      List<Instance> e = edge.get(pinEdge);
      e.add(pin);
    }

    for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
      DefaultAppearance.sortPinList(entry.getValue(), entry.getKey());
    }

    int numNorth = edge.get(Direction.NORTH).size();
    int numSouth = edge.get(Direction.SOUTH).size();
    int numEast = edge.get(Direction.EAST).size();
    int numWest = edge.get(Direction.WEST).size();
    int maxVert = Math.max(numNorth, numSouth);
    int maxHorz = Math.max(numEast, numWest);

    int offsNorth = computeOffset(numNorth, numSouth, maxHorz);
    int offsSouth = computeOffset(numSouth, numNorth, maxHorz);
    int offsEast = computeOffset(numEast, numWest, maxVert);
    int offsWest = computeOffset(numWest, numEast, maxVert);

    int width = computeDimension(maxVert, maxHorz);
    int height = computeDimension(maxHorz, maxVert);

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
    int rx = OFFS + (9 - (ax + 9) % 10);
    int ry = OFFS + (9 - (ay + 9) % 10);

    Location e0 = Location.create(rx + (width - 8) / 2, ry + 1);
    Location e1 = Location.create(rx + (width + 8) / 2, ry + 1);
    Location ct = Location.create(rx + width / 2, ry + 11);
    Curve notch = new Curve(e0, e1, ct);
    notch.setValue(DrawAttr.STROKE_WIDTH, 2);
    notch.setValue(DrawAttr.STROKE_COLOR, Color.GRAY);
    Rectangle rect = new Rectangle(rx, ry, width, height);
    rect.setValue(DrawAttr.STROKE_WIDTH, 2);

    List<CanvasObject> ret = new ArrayList<>();
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
