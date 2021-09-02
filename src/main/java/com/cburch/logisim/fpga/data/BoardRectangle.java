/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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
  private int Width;
  private int Height;
  private boolean IsActiveHigh = true;
  private int NrBits = 0;
  private Long value = null;
  private String Label;

  public BoardRectangle(int x, int y, int w, int h) {
    this.set(x, y, w, h);
  }

  public BoardRectangle(Rectangle rect) {
    set(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
  }

  @Override
  public boolean equals(Object rect) {
    if (!(rect instanceof BoardRectangle)) return false;
    BoardRectangle Rect = (BoardRectangle) rect;
    return ((Rect.getHeight() == Height)
        && (Rect.getWidth() == Width)
        && (Rect.getXpos() == xPosition)
        && (Rect.getYpos() == yPosition));
  }

  public int getHeight() {
    return Height;
  }

  public int getWidth() {
    return Width;
  }

  public int getXpos() {
    return xPosition;
  }

  public int getYpos() {
    return yPosition;
  }

  public boolean IsActiveOnHigh() {
    return IsActiveHigh;
  }

  public String GetLabel() {
    return Label;
  }

  public int getNrBits() {
    return NrBits;
  }

  public void setNrBits(int nr) {
    NrBits = nr;
  }

  public void updateRectangle(BoardRectangle other) {
    xPosition = other.getXpos();
    yPosition = other.getYpos();
    Width = other.getWidth();
    Height = other.getHeight();
  }

  public void updateRectangle(Rectangle other) {
    xPosition = other.getX();
    yPosition = other.getY();
    Width = other.getWidth();
    Height = other.getHeight();
  }

  public Boolean Overlap(Rectangle rect) {
    return Overlap(new BoardRectangle(rect));
  }

  public Boolean Overlap(BoardRectangle rect) {
    Boolean result;
    int xl, xr, yt, yb;
    xl = rect.getXpos();
    xr = xl + rect.getWidth();
    yt = rect.getYpos();
    yb = yt + rect.getHeight();

    /* first check for the other corner points inside myself */
    result = this.PointInside(xl, yt);
    result |= this.PointInside(xl, yb);
    result |= this.PointInside(xr, yt);
    result |= this.PointInside(xr, yb);

    /* check for my corner points inside him */
    result |= rect.PointInside(xPosition, yPosition);
    result |= rect.PointInside(xPosition + Width, yPosition);
    result |= rect.PointInside(xPosition, yPosition + Height);
    result |= rect.PointInside(xPosition + Width, yPosition + Height);

    /*
     * if result=false: for sure the corner points are not inside one of
     * each other
     */
    /* we now have to check for partial overlap */
    if (!result) {
      result = ((xl >= xPosition)
          && (xl <= (xPosition + Width))
          && (yt <= yPosition)
          && (yb >= (yPosition + Height)));
      result |=
          ((xr >= xPosition)
              && (xr <= (xPosition + Width))
              && (yt <= yPosition)
              && (yb >= (yPosition + Height)));
      result |=
          ((xl <= xPosition)
              && (xr >= (xPosition + Width))
              && (yt >= yPosition)
              && (yt <= (yPosition + Height)));
      result |=
          ((xl <= xPosition)
              && (xr >= (xPosition + Width))
              && (yb >= yPosition)
              && (yb <= (yPosition + Height)));
    }
    if (!result) {
      result = ((xPosition >= xl)
          && (xPosition <= xr)
          && (yPosition <= yt)
          && ((yPosition + Height) >= yb));
      result |=
          (((xPosition + Width) >= xl)
              && ((xPosition + Width) <= xr)
              && (yPosition <= yt)
              && ((yPosition + Height) >= yb));
      result |=
          ((xPosition <= xl)
              && ((xPosition + Width) >= xr)
              && (yPosition >= yt)
              && (yPosition <= yb));
      result |=
          ((xPosition <= xl)
              && ((xPosition + Width) >= xr)
              && ((yPosition + Height) >= yt)
              && ((yPosition + Height) <= yb));
    }

    return result;
  }

  public Boolean PointInside(int x, int y) {
    return ((x >= xPosition)
        && (x <= (xPosition + Width))
        && (y >= yPosition)
        && (y <= (yPosition + Height)));
  }

  private void set(int x, int y, int w, int h) {
    if (w < 0) {
      xPosition = x + w;
      Width = -w;
    } else {
      xPosition = x;
      Width = w;
    }
    if (h < 0) {
      yPosition = y + h;
      Height = -h;
    } else {
      yPosition = y;
      Height = h;
    }
  }

  public void SetActiveOnHigh(boolean IsActiveHigh) {
    this.IsActiveHigh = IsActiveHigh;
  }

  public void SetLabel(String Label) {
    this.Label = Label;
  }

  public void setValue(Long val) {
    this.value = val;
  }

  public Long getValue() {
    return value;
  }
}
