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

public class BoardRectangle {
  private int xPosition;
  private int yPosition;
  private int width;
  private int height;
  private boolean isActiveHigh = true;
  private int nrBits = 0;
  private Long value = null;
  private String label;

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
              && (rect.getXpos() == xPosition)
              && (rect.getYpos() == yPosition))
           : false;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public int getXpos() {
    return xPosition;
  }

  public int getYpos() {
    return yPosition;
  }

  public boolean isActiveOnHigh() {
    return isActiveHigh;
  }

  public String getLabel() {
    return label;
  }

  public int getNrBits() {
    return nrBits;
  }

  public void setNrBits(int nr) {
    nrBits = nr;
  }

  public void updateRectangle(BoardRectangle other) {
    xPosition = other.getXpos();
    yPosition = other.getYpos();
    width = other.getWidth();
    height = other.getHeight();
  }

  public void updateRectangle(Rectangle other) {
    xPosition = other.getX();
    yPosition = other.getY();
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
    xl = rect.getXpos();
    xr = xl + rect.getWidth();
    yt = rect.getYpos();
    yb = yt + rect.getHeight();

    /* first check for the other corner points inside myself */
    result = this.isPointInside(xl, yt);
    result |= this.isPointInside(xl, yb);
    result |= this.isPointInside(xr, yt);
    result |= this.isPointInside(xr, yb);

    /* check for my corner points inside him */
    result |= rect.isPointInside(xPosition, yPosition);
    result |= rect.isPointInside(xPosition + width, yPosition);
    result |= rect.isPointInside(xPosition, yPosition + height);
    result |= rect.isPointInside(xPosition + width, yPosition + height);

    /*
     * if result=false: for sure the corner points are not inside one of
     * each other
     */
    /* we now have to check for partial overlap */
    if (!result) {
      result = ((xl >= xPosition)
          && (xl <= (xPosition + width))
          && (yt <= yPosition)
          && (yb >= (yPosition + height)));
      result |=
          ((xr >= xPosition)
              && (xr <= (xPosition + width))
              && (yt <= yPosition)
              && (yb >= (yPosition + height)));
      result |=
          ((xl <= xPosition)
              && (xr >= (xPosition + width))
              && (yt >= yPosition)
              && (yt <= (yPosition + height)));
      result |=
          ((xl <= xPosition)
              && (xr >= (xPosition + width))
              && (yb >= yPosition)
              && (yb <= (yPosition + height)));
    }
    if (!result) {
      result = ((xPosition >= xl)
          && (xPosition <= xr)
          && (yPosition <= yt)
          && ((yPosition + height) >= yb));
      result |=
          (((xPosition + width) >= xl)
              && ((xPosition + width) <= xr)
              && (yPosition <= yt)
              && ((yPosition + height) >= yb));
      result |=
          ((xPosition <= xl)
              && ((xPosition + width) >= xr)
              && (yPosition >= yt)
              && (yPosition <= yb));
      result |=
          ((xPosition <= xl)
              && ((xPosition + width) >= xr)
              && ((yPosition + height) >= yt)
              && ((yPosition + height) <= yb));
    }

    return result;
  }

  public Boolean isPointInside(int x, int y) {
    return ((x >= xPosition)
        && (x <= (xPosition + width))
        && (y >= yPosition)
        && (y <= (yPosition + height)));
  }

  private void set(int x, int y, int w, int h) {
    if (w < 0) {
      xPosition = x + w;
      width = -w;
    } else {
      xPosition = x;
      width = w;
    }
    if (h < 0) {
      yPosition = y + h;
      height = -h;
    } else {
      yPosition = y;
      height = h;
    }
  }

  public void setActiveOnHigh(boolean isActiveHigh) {
    this.isActiveHigh = isActiveHigh;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setValue(Long val) {
    this.value = val;
  }

  public Long getValue() {
    return value;
  }
}
