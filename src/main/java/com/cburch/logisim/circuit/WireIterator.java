/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.Location;
import java.util.Iterator;

class WireIterator implements Iterator<Location> {
  private int curX;
  private int curY;
  private int destX;
  private int destY;
  private final int deltaX;
  private final int deltaY;
  private boolean destReturned;

  public WireIterator(Location e0, Location e1) {
    curX = e0.getX();
    curY = e0.getY();
    destX = e1.getX();
    destY = e1.getY();
    destReturned = false;
    if (curX < destX) deltaX = 10;
    else if (curX > destX) deltaX = -10;
    else deltaX = 0;
    if (curY < destY) deltaY = 10;
    else if (curY > destY) deltaY = -10;
    else deltaY = 0;

    final var offX = (destX - curX) % 10;
    if (offX != 0) { // should not happen, but in case it does...
      destX = curX + deltaX * ((destX - curX) / 10);
    }
    final var offY = (destY - curY) % 10;
    if (offY != 0) { // should not happen, but in case it does...
      destY = curY + deltaY * ((destY - curY) / 10);
    }
  }

  @Override
  public boolean hasNext() {
    return !destReturned;
  }

  @Override
  public Location next() {
    final var ret = Location.create(curX, curY, true);
    destReturned |= curX == destX && curY == destY;
    curX += deltaX;
    curY += deltaY;
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
