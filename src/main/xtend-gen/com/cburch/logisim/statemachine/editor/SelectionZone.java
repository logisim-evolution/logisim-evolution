package com.cburch.logisim.statemachine.editor;

import com.google.common.base.Objects;
import java.awt.Point;

@SuppressWarnings("all")
public class SelectionZone {
  private Point start;
  
  private Point end;
  
  public SelectionZone() {
  }
  
  public boolean isSinglePoint() {
    return (((!Objects.equal(this.start, null)) && Objects.equal(this.end, null)) || this.end.equals(this.start));
  }
  
  public Point start(final Point point) {
    Point _point = new Point(point);
    return this.start = _point;
  }
  
  public Point extend(final Point point) {
    Point _point = new Point(point);
    return this.end = _point;
  }
  
  public Point clear() {
    Point _xblockexpression = null;
    {
      this.start = null;
      _xblockexpression = this.end = null;
    }
    return _xblockexpression;
  }
}
