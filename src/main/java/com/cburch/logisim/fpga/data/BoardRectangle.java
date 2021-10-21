/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import com.cburch.draw.shapes.Rectangle;
import lombok.Getter;
import lombok.Setter;

public class BoardRectangle {
  @Getter private int positionX;
  @Getter private int positionY;
  @Getter private int width;
  @Getter private int height;
  @Getter @Setter private boolean activeOnHigh = true;
  @Getter @Setter private int nrBits = 0;
  @Getter @Setter private Long value = null;
  @Getter @Setter private String label;

  public BoardRectangle(int x, int y, int w, int h) {
    this.set(x, y, w, h);
  }

  public BoardRectangle(Rectangle rect) {
    set(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
  }

  @Override
  public boolean equals(Object rectangle) {
    return (rectangle instanceof BoardRectangle rect)
           ? ((rect.getHeight() == height)
              && (rect.getWidth() == width)
              && (rect.getPositionX() == positionX)
              && (rect.getPositionY() == positionY))
           : false;
  }

  public void updateRectangle(BoardRectangle other) {
    positionX = other.getPositionX();
    positionY = other.getPositionY();
    width = other.getWidth();
    height = other.getHeight();
  }

  public void updateRectangle(Rectangle other) {
    positionX = other.getX();
    positionY = other.getY();
    width = other.getWidth();
    height = other.getHeight();
  }

  public Boolean overlap(Rectangle rect) {
    return overlap(new BoardRectangle(rect));
  }

  public Boolean overlap(BoardRectangle rect) {
    Boolean result;
    int xl;
    int xr;
    int yt;
    int yb;
    xl = rect.getPositionX();
    xr = xl + rect.getWidth();
    yt = rect.getPositionY();
    yb = yt + rect.getHeight();

    /* first check for the other corner points inside myself */
    result = this.isPointInside(xl, yt);
    result |= this.isPointInside(xl, yb);
    result |= this.isPointInside(xr, yt);
    result |= this.isPointInside(xr, yb);

    /* check for my corner points inside him */
    result |= rect.isPointInside(positionX, positionY);
    result |= rect.isPointInside(positionX + width, positionY);
    result |= rect.isPointInside(positionX, positionY + height);
    result |= rect.isPointInside(positionX + width, positionY + height);

    /*
     * if result=false: for sure the corner points are not inside one of
     * each other
     */
    /* we now have to check for partial overlap */
    if (!result) {
      result = ((xl >= positionX)
          && (xl <= (positionX + width))
          && (yt <= positionY)
          && (yb >= (positionY + height)));
      result |=
          ((xr >= positionX)
              && (xr <= (positionX + width))
              && (yt <= positionY)
              && (yb >= (positionY + height)));
      result |=
          ((xl <= positionX)
              && (xr >= (positionX + width))
              && (yt >= positionY)
              && (yt <= (positionY + height)));
      result |=
          ((xl <= positionX)
              && (xr >= (positionX + width))
              && (yb >= positionY)
              && (yb <= (positionY + height)));
    }
    if (!result) {
      result = ((positionX >= xl)
          && (positionX <= xr)
          && (positionY <= yt)
          && ((positionY + height) >= yb));
      result |=
          (((positionX + width) >= xl)
              && ((positionX + width) <= xr)
              && (positionY <= yt)
              && ((positionY + height) >= yb));
      result |=
          ((positionX <= xl)
              && ((positionX + width) >= xr)
              && (positionY >= yt)
              && (positionY <= yb));
      result |=
          ((positionX <= xl)
              && ((positionX + width) >= xr)
              && ((positionY + height) >= yt)
              && ((positionY + height) <= yb));
    }

    return result;
  }

  public Boolean isPointInside(int x, int y) {
    return ((x >= positionX)
        && (x <= (positionX + width))
        && (y >= positionY)
        && (y <= (positionY + height)));
  }

  private void set(int x, int y, int w, int h) {
    if (w < 0) {
      positionX = x + w;
      width = -w;
    } else {
      positionX = x;
      width = w;
    }
    if (h < 0) {
      positionY = y + h;
      height = -h;
    } else {
      positionY = y;
      height = h;
    }
  }

}
