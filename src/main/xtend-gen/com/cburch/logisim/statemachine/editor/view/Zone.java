package com.cburch.logisim.statemachine.editor.view;

import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo;
import com.google.common.base.Objects;
import java.awt.Point;
import org.eclipse.xtend2.lib.StringConcatenation;

@SuppressWarnings("all")
public class Zone {
  private Point x0;
  
  private Point x1;
  
  public Zone(final Point a, final Point b) {
    int _min = Math.min(a.x, b.x);
    int _min_1 = Math.min(a.y, b.y);
    Point _point = new Point(_min, _min_1);
    this.x0 = _point;
    int _max = Math.max(a.x, b.x);
    int _max_1 = Math.max(a.y, b.y);
    Point _point_1 = new Point(_max, _max_1);
    this.x1 = _point_1;
  }
  
  public Zone(final Point a) {
    Point _point = new Point(a);
    this.x0 = _point;
    Point _point_1 = new Point(a);
    this.x1 = _point_1;
  }
  
  public Object resize(final Point b) {
    return null;
  }
  
  public boolean isSinglePoint() {
    return (Objects.equal(this.x1, this.x0) && (!Objects.equal(this.x0, null)));
  }
  
  public Zone(final int xa, final int ya, final int xb, final int yb) {
    Point _point = new Point(xa, ya);
    this.x0 = _point;
    Point _point_1 = new Point(xb, yb);
    this.x1 = _point_1;
  }
  
  public Zone(final LayoutInfo l) {
    int _x = l.getX();
    int _y = l.getY();
    Point _point = new Point(_x, _y);
    this.x0 = _point;
    int _x_1 = l.getX();
    int _width = l.getWidth();
    int _plus = (_x_1 + _width);
    int _y_1 = l.getY();
    int _height = l.getHeight();
    int _plus_1 = (_y_1 + _height);
    Point _point_1 = new Point(_plus, _plus_1);
    this.x1 = _point_1;
  }
  
  public boolean contains(final Point p) {
    boolean _xifexpression = false;
    boolean _isSinglePoint = this.isSinglePoint();
    if (_isSinglePoint) {
      _xifexpression = false;
    } else {
      _xifexpression = ((((p.x >= this.x0.x) && (p.x <= this.x1.x)) && (p.y >= this.x0.y)) && (p.y <= this.x1.y));
    }
    return _xifexpression;
  }
  
  public boolean contains(final Zone p) {
    boolean _xifexpression = false;
    boolean _isSinglePoint = this.isSinglePoint();
    if (_isSinglePoint) {
      _xifexpression = false;
    } else {
      _xifexpression = (this.contains(p.x0) && this.contains(p.x1));
    }
    return _xifexpression;
  }
  
  public Point getX0() {
    return this.x0;
  }
  
  public Point getX1() {
    return this.x1;
  }
  
  @Override
  public String toString() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("(");
    _builder.append(this.x0.x);
    _builder.append(",");
    _builder.append(this.x0.y);
    _builder.append(")->(");
    _builder.append(this.x1.x);
    _builder.append(",");
    _builder.append(this.x1.y);
    _builder.append("))");
    return _builder.toString();
  }
}
