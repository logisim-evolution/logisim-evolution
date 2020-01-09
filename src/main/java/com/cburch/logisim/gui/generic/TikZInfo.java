/**
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

package com.cburch.logisim.gui.generic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.shapes.DrawAttr;

public class TikZInfo implements Cloneable {

  private static double BASIC_STROKE_WIDTH = 1;

  private AffineTransform myTransformer = new AffineTransform();
  private Color drawColor;
  private Color backColor;
  private ArrayList<DrawObject> Contents = new ArrayList<DrawObject>();
  private HashMap<String,String> customColors = new HashMap<String,String>();
  private ArrayList<String> usedFonts = new ArrayList<String>();
  private int fontIndex;
  private int fontSize;
  private boolean fontBold;
  private boolean fontItalic;
  private String currentDrawColor;
  private String currentBackColor;
  private Font curFont;
  private BasicStroke curStroke = new BasicStroke(1);
  private double myRotation = 0;
  private Rectangle clip;
  
  public interface DrawObject {
    public String getTikZCommand();
    public void getSvgCommand(Document root, Element e);
    public boolean insideArea(int x, int y, int width, int height);
    public DrawObject clone();
    public void move(int dx , int dy);
  }
  
  private class AbstratctTikZ implements DrawObject {
    protected Point start;
    protected Point end;
    protected ArrayList<Point> points = new ArrayList<Point>();
    protected float strokeWidth;
    protected String color;
    protected double alpha;
    protected boolean filled;
    protected boolean close;

    public AbstratctTikZ() {};
    
    public AbstratctTikZ(int x1, int y1, int x2, int y2) {
      start = new Point(x1,y1);
      end = new Point(x2,y2);
      transform(start, start);
      transform(end, end);
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      alpha = (double)drawColor.getAlpha()/255.0;
      points.clear();
      filled = false;
      close = false;
    }
    
    public String getTikZCommand() {
      return "";
    }

    public boolean insideArea(int x, int y, int width, int height) {
      Point left = new Point(x,y);
      Point right = new Point(x+width,y+height);
      transform(left,left);
      transform(right,right);
      int x1 = left.x < right.x ? left.x : right.x;
      int x2 = left.x < right.x ? right.x : left.x;
      int y1 = left.y < right.y ? left.y : right.y;
      int y2 = left.y < right.y ? right.y : left.y;
      boolean inside = true;
      if (points.isEmpty())
        return (start.x >= x1 && start.x <= x2) &&
                 (start.y >= y1 && start.y <= y2) &&
                 (end.x >= x1 && end.x <= x2) &&
                 (end.y >= y1 && end.y <= y2);
      else {
        for (Point point : points) {
          inside &= (point.x >= x1 && point.x <= x2) && (point.y >= y1 && point.y <= y2);
        }
      }
      return inside;
    }

    public DrawObject clone() {
      return null;
    }

    @Override
    public void move(int dx, int dy) {
      Point move = new Point(dx,dy);
      transform(move, move);
      if (points.isEmpty()) {
        start = new Point(start.x+move.x,start.y+move.y);
        end = new Point(end.x+move.x,start.y+move.y);
      } else {
        for (Point point : points) {
          point.x += move.x;
          point.y += move.y;
        }
      }
    }

	@Override
	public void getSvgCommand(Document root, Element e) {
	}
  }
  
  private class TikZLine extends AbstratctTikZ {
    
    public TikZLine() {}

    public TikZLine(int x1, int y1, int x2, int y2) {
      super(x1,y1,x2,y2);
    }
    
    public TikZLine(int[] xPoints, int[] yPoints, int nPoints, boolean fill, boolean isPolygon) {
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      start = null;
      end = null;
      for (int i = 0 ; i < nPoints ; i++) {
        Point p = new Point(xPoints[i],yPoints[i]);
        transform(p, p);
        points.add(p);
      }
      filled = fill;
      close = isPolygon;
    }
    
    public Point getStartPoint() {
      if (points.isEmpty())
        return start;
      else
        return points.get(0);  
    }
    
    public Point getEndPoint() {
      if (points.isEmpty())
        return end;
      else
        return points.get(points.size()-1);
    }
    
    public boolean canMerge(TikZLine l) {
      if (close || l.close)
        return false;
      if (!color.equals(l.color))
        return false;
      if (strokeWidth != l.strokeWidth)
        return false;
      if (filled || l.filled)
        return false;
      if (getStartPoint().equals(l.getEndPoint()))
        return true;
      if (getEndPoint().equals(l.getStartPoint()))
        return true;
      if (getStartPoint().equals(l.getStartPoint()))
        return true;
      if (getEndPoint().equals(l.getEndPoint()))
        return true;  
      return false;
    }
    
    public void closeIfPossible() {
      if (points.isEmpty())
        return;
      if (getStartPoint().equals(getEndPoint())) {
        points.remove(points.size()-1);
        close = true;
      }
    }
    
    private void addPoints(ArrayList<Point> p , int start , int end, boolean atEnd, boolean reverseOrder) {
      if (atEnd) {
        if (reverseOrder)
          for (int i = end-1 ; i >= start ; i--)
            points.add(p.get(i));  
        else
          for (int i = start ; i < end ; i++)
            points.add(p.get(i));
      } else {
        if (reverseOrder)
          for (int i = start ; i < end ; i++)
            points.add(0,p.get(i));
        else
          for (int i = end - 1 ; i >= start ; i--)
            points.add(0,p.get(i));
      }
    }
    
    public boolean merge(TikZLine l) {
      if (!canMerge(l))
        return false;
      if (points.isEmpty()) {
        points.add(start);
        points.add(end);
      }
      if (l.points.isEmpty()) {
        l.points.add(l.start);
        l.points.add(l.end);
      }
      if (getStartPoint().equals(l.getEndPoint())) {
        addPoints(l.points,0,l.points.size()-1,false,false);  
      } else if (getEndPoint().equals(l.getStartPoint())) {
        addPoints(l.points,1,l.points.size(),true,false);
      } else if (getStartPoint().equals(l.getStartPoint())) {
        addPoints(l.points,1,l.points.size(),false,true);
      } else if (getEndPoint().equals(l.getEndPoint())) {
        addPoints(l.points,0,l.points.size()-1,true,true);
      } else return false;
      return true;
    }
    
    @Override
    public String getTikZCommand() {
      StringBuffer contents = new StringBuffer();
      if (filled)
        contents.append("\\fill ");
      else
        contents.append("\\draw ");
      contents.append("[line width=");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      contents.append(rounded(width)+"pt, "+color+" ] ");
      if (points.isEmpty()) {
        contents.append(getPoint(start));
        contents.append("--");
        contents.append(getPoint(end));
      } else {
        boolean first = true;
        for (Point point : points) {
          if (first)
            first = false;
          else
            contents.append("--");
          contents.append(getPoint(point));
        }
      }
      if (close)
        contents.append("-- cycle");
      contents.append(";");
      return contents.toString();
    }
    
    @Override
    public void getSvgCommand(Document root, Element e) {
      StringBuffer content = new StringBuffer();
      Element ne = root.createElement(close ? "polygon" : "polyline");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb("+customColors.get(color)+")" : "none");
      ne.setAttribute("stroke", filled ? "none" : "rgb("+customColors.get(color)+")");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      ne.setAttribute("stroke-linecap", "square");
      if (points.isEmpty()) {
        content.append(start.x+","+start.y+" "+end.x+","+end.y);
      } else {
        boolean first = true;
        for (Point point : points) {
          if (first)
            first = false;
          else
            content.append(" ");
          content.append(point.x+","+point.y);
        }
      }
      ne.setAttribute("points", content.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public DrawObject clone() {
      TikZLine NewIns = new TikZLine();
      NewIns.start = (Point) start.clone();
      NewIns.end = (Point) end.clone();
      NewIns.strokeWidth = strokeWidth;
      NewIns.color = color;
      NewIns.points = (ArrayList<Point>) points.clone();
      NewIns.filled = filled;
      NewIns.close = close;
      return NewIns;
    }

  }
  
  private class TikZBezier extends AbstratctTikZ {
    private class BezierInfo implements Cloneable {
      private Point2D startPoint,controlPoint1,controlPoint2,endPoint;
      private boolean closePath;
      
      public BezierInfo() {
        startPoint = controlPoint1 = controlPoint2 = endPoint = null;
        closePath = true;
      }
      
      public BezierInfo(Point2D nextPoint, boolean startpoint, boolean filled) {
        controlPoint1 = controlPoint2 = null;
        if (startpoint) {
          startPoint = nextPoint;
          endPoint = null;
        } else {
          startPoint = null;
          endPoint = nextPoint;
        }   
        closePath = false;
        scale();
      }
      
      public BezierInfo(Point2D controlPoint, Point2D nextPoint) {
        startPoint = controlPoint2 = null;
        controlPoint1 = controlPoint;
        endPoint = nextPoint;
        closePath=false;
        scale();
      }
      
      public BezierInfo(Point2D controlPointa, Point2D controlPointb, Point2D nextPoint) {
        startPoint = null;
        controlPoint1 = controlPointa;
        controlPoint2 = controlPointb;
        endPoint = nextPoint;
        closePath = false;
        scale();
      }
      
      private void scale() {
        if (startPoint != null) transform(startPoint, startPoint);
        if (controlPoint1 != null) transform(controlPoint1, controlPoint1);
        if (controlPoint2 != null) transform(controlPoint2, controlPoint2);
        if (endPoint != null) transform(endPoint, endPoint);
      }
      
      public BezierInfo clone() {
        BezierInfo newInst = new BezierInfo();
        newInst.startPoint = startPoint;
        newInst.controlPoint1 = controlPoint1;
        newInst.controlPoint2 = controlPoint2;
        newInst.endPoint = endPoint;
        newInst.closePath = closePath;
        return newInst;
      }
      
      public void move(int dx , int dy) {
    	AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
        if (startPoint!= null)
          at.transform(startPoint, startPoint);
        if (controlPoint1 != null)
          at.transform(controlPoint1, controlPoint1);
        if (controlPoint2 != null)
          at.transform(controlPoint2, controlPoint2);
        if (endPoint != null)
          at.transform(endPoint, endPoint);
      }
      
      public String getTikZCommand() {
        StringBuffer contents = new StringBuffer();
        if (closePath) {
          contents.append("-- cycle ");
        } else if (startPoint != null) {
          contents.append(getPoint(startPoint));
        } else {
          if (controlPoint1 == null && controlPoint2 == null) {
        	contents.append("-- "+getPoint(endPoint));
          } else {
        	contents.append(".. controls "+getPoint(controlPoint1)+" ");
        	if (controlPoint2 != null)
        	  contents.append(" and "+getPoint(controlPoint2)+" ");
        	contents.append(".. "+getPoint(endPoint));
          }
        }
        return contents.toString();
      }
      
      public String getSvgPath() {
          StringBuffer contents = new StringBuffer();
          if (closePath) {
            contents.append(" Z");
          } else if (startPoint != null) {
            contents.append(" M"+startPoint.getX()+","+startPoint.getY());
          } else {
            if (controlPoint1 == null && controlPoint2 == null) {
              contents.append(" L"+endPoint.getX()+","+endPoint.getY());
            } else {
              Point2D sPoint = (controlPoint2 == null) ? controlPoint1 : controlPoint2;
              contents.append(" C"+controlPoint1.getX()+","+controlPoint1.getY());
              contents.append(" "+sPoint.getX()+","+sPoint.getY());
              contents.append(" "+endPoint.getX()+","+endPoint.getY());
            }
          }
          return contents.toString();
      }
      
      public boolean insideArea(int x, int y, int width, int height) {
      if (closePath)
        return true;
        boolean inside = true;
        double x1 = x;
        double x2 = x+width;
        double y1 = y;
        double y2 = y+height;
        if (startPoint != null)
          inside &= (startPoint.getX() >= x1 && startPoint.getX() <= x2) && (startPoint.getY() >= y1 && startPoint.getY() <= y2);
        if (endPoint != null)
          inside &= (endPoint.getX() >= x1 && endPoint.getX() <= x2) && (endPoint.getY() >= y1 && endPoint.getY() <= y2);
        return inside;
      }
    }
    
    private ArrayList<BezierInfo> myPath = new ArrayList<BezierInfo>();
    
    public TikZBezier() {};
    
    public TikZBezier(Shape s, boolean filled) {
      Point2D p = new Point2D.Double();
      p.setLocation(0, 0);
      create(p,s,filled);
    }

    public TikZBezier(Point2D orig, Shape s, boolean filled) {
      create(orig,s,filled);
    }
    
    private void create(Point2D origin, Shape s, boolean filled) {
      this.filled = filled;
      this.color = getDrawColorString();
      this.alpha = (double)drawColor.getAlpha()/255.0;
      this.strokeWidth = getStrokeWidth();
      AffineTransform at = AffineTransform.getTranslateInstance(origin.getX(), origin.getY());
      PathIterator p = s.getPathIterator(at);
      while (!p.isDone()) {
        double[] coords = new double[6];
        int type = p.currentSegment(coords);        
        if (type == PathIterator.SEG_MOVETO) {
          Point2D current = new Point2D.Double();
          current.setLocation(coords[0],coords[1]);
          myPath.add(new BezierInfo(current,true,filled));
        } else if (type == PathIterator.SEG_LINETO) {
          Point2D next = new Point2D.Double();
          next.setLocation(coords[0],coords[1]);
          myPath.add(new BezierInfo(next,false,false));
        } else if (type == PathIterator.SEG_CLOSE) {
          myPath.add(new BezierInfo());
        } else if (type == PathIterator.SEG_QUADTO) {
          Point2D next = new Point2D.Double();
          Point2D control = new Point2D.Double();
          control.setLocation(coords[0],coords[1]);
          next.setLocation(coords[2],coords[3]);
          myPath.add(new BezierInfo(control,next));
        } else if (type == PathIterator.SEG_CUBICTO) {
          Point2D next = new Point2D.Double();
          Point2D control1 = new Point2D.Double();
          Point2D control2 = new Point2D.Double();
          control1.setLocation(coords[0],coords[1]);
          control2.setLocation(coords[2],coords[3]);
          next.setLocation(coords[4],coords[5]);
          myPath.add(new BezierInfo(control1,control2,next));
        }
        p.next();
      }
    };
    
  @Override
  public String getTikZCommand() {
    StringBuffer contents = new StringBuffer();
    contents.append(filled ? "\\fill " : "\\draw ");
    contents.append("[line width=");
    double width = strokeWidth*BASIC_STROKE_WIDTH;
    contents.append(rounded(width)+"pt, "+color);
    if (filled && alpha != 1.0)
      contents.append(", fill opacity="+rounded(alpha));
    contents.append(" ] ");
    for (BezierInfo point : myPath) {
      contents.append(point.getTikZCommand());
    }
    contents.append(";");
    return contents.toString();
  }

  @Override
  public void getSvgCommand(Document root, Element e) {
    Element ne = root.createElement("path");
    e.appendChild(ne);
    ne.setAttribute("fill", filled ? "rgb("+customColors.get(color)+")" : "none");
    if (filled && alpha != 1.0)
      ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
    ne.setAttribute("stroke", filled ? "none" : "rgb("+customColors.get(color)+")");
    double width = strokeWidth*BASIC_STROKE_WIDTH;
    ne.setAttribute("stroke-width", Double.toString(rounded(width)));
    ne.setAttribute("stroke-linecap", "square");
    StringBuffer content = new StringBuffer();
    for (BezierInfo point : myPath) {
      content.append(point.getSvgPath());
    }
    ne.setAttribute("d", content.toString());
  }

  @Override
  public boolean insideArea(int x, int y, int width, int height) {
    boolean inside = true;
    for (BezierInfo point : myPath)
      inside &= point.insideArea(x, y, width, height);
    return inside;
  }

  @Override
  public DrawObject clone() {
      TikZBezier newInst = new TikZBezier();
      newInst.filled = filled;
      newInst.color = color;
      newInst.alpha = alpha;
      newInst.strokeWidth = strokeWidth;
      for (BezierInfo point : myPath)
        newInst.myPath.add(point.clone());  
    return newInst;
  }

  @Override
  public void move(int dx, int dy) {
    for (BezierInfo point : myPath)
      point.move(dx, dy);
  }
    
  }
  
  private class TikZRectangle extends AbstratctTikZ {
    Point2D rad;
  
    public TikZRectangle() {};

    public TikZRectangle(int x1, int y1, int x2, int y2, int arcwidth, int archeight, boolean filled) {
      super(x1,y1,x2,y2);
      rad = new Point2D.Double();
      rad.setLocation(((double)arcwidth)/2.0, ((double)archeight)/2.0);
      this.filled = filled;
    }

    public TikZRectangle(int x1, int y1, int x2, int y2, boolean filled) {
      super(x1,y1,x2,y2);
      this.filled = filled;
      rad = null;
    }
    
    public void setBackColor() {
      this.color = getBackColorString();
    }
      
    @Override
    public DrawObject clone() {
      TikZRectangle NewIns = new TikZRectangle();
      NewIns.start = (Point) start.clone();
      NewIns.end = (Point) end.clone();
      NewIns.strokeWidth = strokeWidth;
      NewIns.color = color;
      NewIns.filled = filled;
      NewIns.rad = (Point2D) rad.clone();
      NewIns.alpha = alpha;
      return NewIns;
    }
    
    @Override
    public String getTikZCommand() {
      StringBuffer contents = new StringBuffer();
      if (rad == null) {
        contents.append(filled ? "\\fill " : "\\draw ");
        contents.append("[line width=");
        double width = strokeWidth*BASIC_STROKE_WIDTH;
        contents.append(rounded(width)+"pt, "+color);
        if (filled && alpha != 1.0)
          contents.append(", fill opacity="+rounded(alpha));
        contents.append(" ] ");
        contents.append(getPoint(start));
        contents.append("rectangle");
        contents.append(getPoint(end));
        contents.append(";");
      } else {
    	/* TODO : change to tikz command as pgfpicture causes sometimes troubles */
        contents.append("\\begin{pgfpicture}\n");
        contents.append("   \\begin{pgfmagnify}{1pt}{-1pt}\n");
        contents.append("      \\pgfsetrectcap\n"); 
        contents.append("      \\pgfsetcornersarced{"+getPgfPoint(rad)+"}\n"); 
        contents.append("      \\pgfsetlinewidth{"+strokeWidth+"}\n"); 
        contents.append("      \\color{"+color+"}\n"); 
        contents.append("      \\pgfsetfillopacity{"+alpha+"}\n");
        contents.append("      \\pgfpathrectanglecorners{"+getPgfPoint(start)+"}{"+getPgfPoint(end)+"}\n");
        contents.append("      \\pgfusepath{"+(filled?"fill":"stroke")+"}\n");
        contents.append("   \\end{pgfmagnify}\n");
        contents.append("\\end{pgfpicture}");
      }
      return contents.toString();
    }
    
    @Override
    public void getSvgCommand(Document root, Element e) {
      Element ne = root.createElement("rect");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb("+customColors.get(color)+")" : "none");
      if (filled && alpha != 1.0)
        ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb("+customColors.get(color)+")");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      ne.setAttribute("stroke-linecap", "square");
      if (rad != null) {
        ne.setAttribute("rx", Double.toString(rad.getX()));
        ne.setAttribute("ry", Double.toString(rad.getY()));
      }
      int xpos = (end.x < start.x) ? end.x : start.x;
      int bwidth = Math.abs(end.x-start.x);
      int ypos = (end.y < start.y) ? end.y : start.y;
      int bheight = Math.abs(end.y-start.y);
      ne.setAttribute("x", Integer.toString(xpos));
      ne.setAttribute("y", Integer.toString(ypos));
      ne.setAttribute("width", Integer.toString(bwidth));
      ne.setAttribute("height", Integer.toString(bheight));
    }
  }

  private class TikZElipse extends AbstratctTikZ {

    protected double xRad,yRad;
    protected int rotation;

    public TikZElipse() {};
    
    public TikZElipse(int x , int y , int width , int height, boolean filled) {
      super(x+(width>>1), y+(height>>1), 0 , 0);
      init(width,height,filled);
    }
    
    private void init(int width , int height, boolean filled) {
      this.filled = filled;
      xRad = ((double)width)/2.0;
      yRad = ((double)height)/2.0;
      rotation = (int) getRotationDegrees();
    }

    @Override
    public DrawObject clone() {
      TikZElipse NewIns = new TikZElipse();
      NewIns.start = (Point) start.clone();
      NewIns.end = (Point) end.clone();
      NewIns.strokeWidth = strokeWidth;
      NewIns.color = color;
      NewIns.filled = filled;
      NewIns.xRad = xRad;
      NewIns.yRad = yRad;
      NewIns.rotation = rotation;
      NewIns.alpha = alpha;
      return NewIns;
    }
    
    @Override
    public String getTikZCommand() {
      StringBuffer contents = new StringBuffer();
      contents.append(filled ? "\\fill " : "\\draw ");
      contents.append("[line width=");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      contents.append(rounded(width)+"pt, "+color);
      if (rotation != 0)
        contents.append(", rotate around={"+this.rotation+":"+getPoint(start)+"}");
      if (filled && alpha != 1.0)
        contents.append(", fill opacity="+rounded(alpha));
      contents.append("] ");
      contents.append(getPoint(start));
      contents.append("ellipse ("+xRad+" and "+yRad+" );");
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      Element ne = root.createElement("ellipse");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb("+customColors.get(color)+")" : "none");
      if (filled && alpha != 1.0)
        ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb("+customColors.get(color)+")");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      if (rotation !=0)
        ne.setAttribute("transform", "translate("+start.getX()+" "+start.getY()+") rotate("+this.rotation+")");
      else {
    	ne.setAttribute("cx", Double.toString(start.getX()));
    	ne.setAttribute("cy", Double.toString(start.getY()));
      }
      ne.setAttribute("rx", Double.toString(Math.abs(xRad)));
      ne.setAttribute("ry", Double.toString(Math.abs(yRad)));
    }
  }
  
  private class TikZArc extends TikZElipse {

    private double startAngle,stopAngle;
    private Point2D startPos = new Point2D.Double();
    private Point2D stopPos = new Point2D.Double();

    public TikZArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
      filled = fill;
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      points.clear();
      close = false;
      Point2D Radius = new Point2D.Double();
      Point2D center = new Point2D.Double();
      Radius.setLocation(((double)width)/2.0, ((double)height)/2.0);
      center.setLocation(((double)x)+Radius.getX(), ((double)y)+Radius.getY());
      double startAnglePi = ((double) startAngle * Math.PI)/180.0;
      double startX = center.getX()+Radius.getX()*Math.cos(startAnglePi);
      double startY = center.getY()-Radius.getY()*Math.sin(startAnglePi);
      double stopAnglePi = ((double) (startAngle+arcAngle) * Math.PI)/180.0;
      double stopX = center.getX()+Radius.getX()*Math.cos(stopAnglePi);
      double stopY = center.getY()-Radius.getY()*Math.sin(stopAnglePi);
      xRad = Radius.getX();
      yRad = Radius.getY();
      this.startAngle = -toDegree(startAnglePi);
      stopAngle = -toDegree(stopAnglePi);
      rotation = (int) getRotationDegrees();
      this.startAngle += rotation;
      stopAngle += rotation;
      startPos.setLocation(startX, startY);
      transform(startPos,startPos);
      stopPos.setLocation(stopX,stopY);
      transform(stopPos,stopPos);
    }
    
    public TikZArc() {};
    
    @Override
    public DrawObject clone() {
      TikZArc NewIns = new TikZArc();
      NewIns.strokeWidth = strokeWidth;
      NewIns.color = color;
      NewIns.filled = filled;
      NewIns.xRad = xRad;
      NewIns.yRad = yRad;
      NewIns.rotation = rotation;
      NewIns.startAngle = startAngle;
      NewIns.stopAngle = stopAngle;
      NewIns.alpha = alpha;
      NewIns.startPos = (Point2D) startPos.clone();
      NewIns.stopPos = (Point2D) stopPos.clone();
      return NewIns;
    }

    @Override
    public String getTikZCommand() {
      StringBuffer contents = new StringBuffer();
      contents.append(filled ? "\\fill " : "\\draw ");
      contents.append("[line width=");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      contents.append(rounded(width)+"pt, "+color);
      if (filled && alpha != 1.0)
        contents.append(", fill opacity="+rounded(alpha));
      contents.append("] ");
      contents.append("("+rounded(startPos.getX())+","+rounded(startPos.getY())+")");
      contents.append(" arc ("+startAngle+":"+stopAngle+":"+xRad+" and "+yRad+" );");
      return contents.toString();
    }
    
    @Override
    public void getSvgCommand(Document root, Element e) {
      Element ne = root.createElement("path");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb("+customColors.get(color)+")" : "none");
      if (filled && alpha != 1.0)
        ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb("+customColors.get(color)+")");
      double width = strokeWidth*BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      StringBuffer content = new StringBuffer();
      String info = startAngle > stopAngle ? " 0,0 " : " 0,1 ";
      content.append("M"+startPos.getX()+","+startPos.getY());
      content.append(" A"+xRad+","+yRad+" "+this.startAngle+info+stopPos.getX()+","+stopPos.getY());
      ne.setAttribute("d", content.toString());
    }
  }
  
  private class TikZString implements DrawObject {

    private Point location;
    private String name;
    private AttributedCharacterIterator sIter;
    private String color;
    private double rotation;
    private int fIndex;
    private int fSize;
    private boolean fBold;
    private boolean fItalic;
    
    public TikZString() {};
    
    public TikZString(String str, int x, int y) {
      name = str;
      sIter = null;
      init(x,y);
      fIndex = fontIndex;
      fSize = fontSize;
      fBold = fontBold;
      fItalic = fontItalic;
    }
    
    public TikZString(AttributedCharacterIterator str, int x , int y) {
      name = null;
      sIter = str;
      if (str.getAttribute(TextAttribute.FAMILY)==null || str.getAttribute(TextAttribute.FAMILY).equals("Default")) {
    	fIndex = fontIndex;
        fSize = fontSize;
        fBold = fontBold;
        fItalic = fontItalic;
      } else {
        fBold = str.getAttribute(TextAttribute.WEIGHT)==TextAttribute.WEIGHT_BOLD;
        fItalic = false;
        fSize = (int) str.getAttribute(TextAttribute.SIZE);
        String fontName = (String) str.getAttribute(TextAttribute.FAMILY);
        if (!usedFonts.contains(fontName))
          usedFonts.add(fontName);
        fIndex = usedFonts.indexOf(fontName);
      }
      init(x,y);
    }
    
    private void init(int x, int y) {
      rotation = -getRotationDegrees();
      location = new Point(x,y);
      transform(location,location);
      color = getDrawColorString();
    }
    
    private String getAttrString(boolean svg, Document root, Element e) {
      /* this is a very simplified implementation that should suffice for logisim evolution */
      sIter.first();
      StringBuffer content = new StringBuffer();
      Element tspan = null;
      if (!svg)
        content.append("$\\text{");
      else
        tspan = root.createElement("tspan");
      while (sIter.getIndex() < sIter.getEndIndex()) {
        if (sIter.getAttribute(TextAttribute.SUPERSCRIPT) == TextAttribute.SUPERSCRIPT_SUB) {
          if (svg) {
            if (content.length()>0) {
              e.appendChild(tspan);
              tspan.setTextContent(content.toString());
              content = new StringBuffer();
            }
            tspan = root.createElement("tspan");
            tspan.setAttribute("dy", "3");
            tspan.setAttribute("font-size", ".7em");
          } else
            content.append("}_{\\text{");
          while (sIter.getIndex() < sIter.getEndIndex() && sIter.getAttribute(TextAttribute.SUPERSCRIPT) == TextAttribute.SUPERSCRIPT_SUB) {
            char kar = sIter.current();
            if (kar == '_' && !svg)
              content.append("\\_");
            if (kar == '&' && !svg)
                content.append("\\&");
            else
              content.append(kar);
            sIter.next();
          }
          if (svg) {
            if (content.length()>0) {
              e.appendChild(tspan);
              tspan.setTextContent(content.toString());
              content = new StringBuffer();
              tspan = root.createElement("tspan");
              tspan.setAttribute("dy", "-3");
            } else
                tspan = root.createElement("tspan");
          } else
            content.append("}}\\text{");
        } else {
          char kar = sIter.current();
          if (kar == '\u22C5' && !svg) {
            content.append("}\\cdot\\text{");
          } else if (kar == '_' && !svg) {
            content.append("\\_");
          } else if (kar == '&' && !svg) {
              content.append("\\&");
          } else {
            content.append(kar);
          }
          sIter.next();
        }
      }
      if (!svg)
        content.append("}$");
      else if (content.length()>0) {
        e.appendChild(tspan);
        tspan.setTextContent(content.toString());
        content = new StringBuffer();
      }
      return content.toString();
    }
    
    @Override
    public String getTikZCommand() {
      StringBuffer content = new StringBuffer();
      content.append("\\logisimfont"+getCharRepresentation(fIndex)+"{");
      content.append("\\fontsize{"+fSize+"pt}{"+fSize+"pt}");
      if (fBold)
        content.append("\\fontseries{bx}");
      if (fItalic)
        content.append("\\fontshape{it}");
      content.append("\\selectfont\\node[inner sep=0, outer sep=0, "+color+", anchor=base west");
      if (rotation != 0)
        content.append(", rotate="+this.rotation);
      content.append("] at "+getPoint(location)+" {");
      if (name != null)
        if (name.isEmpty())
          return "";
        else
          for (int i = 0 ; i < name.length() ; i++) {  
            char kar = name.charAt(i);
            if (kar == '_' || kar == '&')
              content.append("\\");
            content.append(kar);
          }
      else
        content.append(getAttrString(false,null,null));
      content.append("};}");
      return content.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      Element ne = root.createElement("text");
      ne.setAttribute("font-family",usedFonts.get(fontIndex) );
      ne.setAttribute("font-size", Integer.toString(fSize));
      if (fBold) 
        ne.setAttribute("font-weight", "bold");
      if (fItalic)
    	ne.setAttribute("font-style", "italic");
      if (this.rotation != 0)
        ne.setAttribute("transform","rotate("+Double.toString(-this.rotation)+","+location.getX()+","+location.getY()+")");
      ne.setAttribute("x", Double.toString(location.getX()));
      ne.setAttribute("y", Double.toString(location.getY()));
      ne.setAttribute("fill", "rgb("+customColors.get(color)+")");
      if (name != null) {
        ne.setTextContent(name);
        if (!name.isEmpty())
          e.appendChild(ne);
      } else {
        getAttrString(true,root,ne);
        e.appendChild(ne);
      }
    }

    @Override
    public boolean insideArea(int x, int y, int width, int height) {
      return (location.x >= x && location.x <= (x+width))&&
             (location.y >= y && location.y <= (y+height));
    }

    @Override
    public DrawObject clone() {
      TikZString newInst = new TikZString();
      newInst.fIndex = fIndex;
      newInst.fSize = fSize;
      newInst.fBold = fBold;
      newInst.fItalic = fItalic;
      newInst.location = (Point) location.clone();
      newInst.name = name;
      newInst.color = color;
      newInst.rotation=rotation;
      return newInst;
    }

    @Override
    public void move(int dx, int dy) {
      location = new Point(location.x+dx,location.y+dy);
    }

  }
  
  public static double rounded( double v ) {
    return ((double)Math.round(v*1000.0))/1000.0;
  }
  
  public static String getPoint(Point2D p) {
    return " ("+rounded(p.getX())+","+rounded(p.getY())+") ";
  }
  
  public static String getPgfPoint(Point2D p) {
    return "\\pgfpoint{"+p.getX()+"}{"+p.getY()+"}";
  }
    
  public TikZInfo() {
    setFont(DrawAttr.DEFAULT_FONT);
    setBackground(Color.WHITE);
    setColor(Color.BLACK);
  }
     
  public TikZInfo clone() {
    TikZInfo NewInst = new TikZInfo();
    NewInst.myTransformer = (AffineTransform) myTransformer.clone();
    NewInst.drawColor = drawColor;
    NewInst.backColor = backColor;
    NewInst.Contents = Contents;
    NewInst.customColors = customColors;
    NewInst.currentDrawColor = currentDrawColor;
    NewInst.currentBackColor = currentBackColor;
    NewInst.curFont = curFont;
    NewInst.curStroke = curStroke;
    NewInst.myRotation = myRotation;
    NewInst.fontIndex = fontIndex;
    NewInst.fontSize = fontSize;
    NewInst.usedFonts = usedFonts;
    NewInst.fontBold = fontBold;
    NewInst.fontItalic = fontItalic;
    if (clip != null)
      NewInst.clip = (Rectangle) clip.clone();
    return NewInst;
  }
  
  private String getColorName(Color c) {
    String custname = "custcol_"+Integer.toString(c.getRed(), 16)+"_"+
                   Integer.toString(c.getGreen(), 16)+"_"+Integer.toString(c.getBlue(),16);
    String RGBCol = Integer.toString(c.getRed())+", "+Integer.toString(c.getGreen())+", "+Integer.toString(c.getBlue());
    customColors.put(custname, RGBCol);
    return custname;
  }

  public void transform(Point2D src , Point2D dest) {
    myTransformer.transform(src, dest);
  }
    
  public float getStrokeWidth() {
    return curStroke.getLineWidth();
  }
    
  public String getDrawColorString() {
    return currentDrawColor;
  }
    
  public String getBackColorString() {
    return currentBackColor;
  }
      
  public double getRotationDegrees() {
    return (myRotation/Math.PI)*180;
  }

  public Color getColor() {
    return drawColor;
  }
    
  public void setColor(Color c) {
    currentDrawColor = getColorName(c);
    drawColor = c;
  }

  public void setBackground(Color color) {
    backColor = color;
    currentBackColor = getColorName(color);
  }

  public Color getBackground() {
    return backColor;
  }

  public void setStroke(Stroke s) {
    if (s instanceof BasicStroke)
      curStroke = (BasicStroke)s;
    else
      System.out.println("TikZWriter: Unsupported Stroke set");
  }
    
  public Stroke getStroke() {
    return curStroke;
  }
    
  public AffineTransform getAffineTransform() {
    return myTransformer;
  }
    
  public void setAffineTransform( AffineTransform Tx ) {
    myTransformer = Tx;  
  }
  
  private double toDegree( double angle ) {
    return (angle/Math.PI)*180.0;
  }

  public void addLine(int x1, int y1, int x2, int y2) {
    Contents.add(new TikZLine(x1,y1,x2,y2));
  }
  
  public void addBezier(Shape s, boolean filled) {
    Contents.add(new TikZBezier(s,filled));
  }
  
  public void addRectangle(int x, int y, int width, int height, boolean filled, boolean backcolor) {
    TikZRectangle obj = new TikZRectangle(x,y,width,height,filled);
    if (backcolor)
      obj.setBackColor();
    Contents.add(obj);
  }
  
  public void addRoundedRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean filled) {
    Contents.add(new TikZRectangle(x,y,width,height,arcWidth,arcHeight,filled));
  }
  
  public void addElipse(int x, int y, int width, int height, boolean filled) {
    Contents.add(new TikZElipse(x,y,width,height,filled));  
  }
  
  public void addArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled) {
    Contents.add(new TikZArc(x,y,width,height,startAngle,arcAngle,filled));
  }
  
  public void addPolyline(int[] xPoints, int[] yPoints, int nPoints, boolean filled, boolean closed) {
    Contents.add(new TikZLine(xPoints,yPoints,nPoints,filled,closed));  
  }
  
  public void addString(String str, int x, int y) {
    Contents.add(new TikZString(str,x,y));  
  }

  public void addString(AttributedCharacterIterator str, int x, int y) {
    Contents.add(new TikZString(str,x,y));  
  }

  public void rotate(double theta) {
    getAffineTransform().rotate(theta);
    myRotation += theta;
  }
  
  public void rotate(double theta, double x, double y) {
    getAffineTransform().rotate(theta,x,y);
    myRotation += theta;
  }
  
  public Font getFont() {
    return curFont;   
  }
  
  public void setFont(Font f) {
    curFont = f;
    String fontName = f.getFamily();
    fontSize = f.getSize();
    fontBold = f.isBold();
    fontItalic = f.isItalic();
    if (usedFonts.contains(fontName))
      fontIndex = usedFonts.indexOf(fontName);
    else {
      usedFonts.add(fontName);
      fontIndex = usedFonts.size()-1;
    }
  }
  
  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    ArrayList<DrawObject> copyList = new ArrayList<DrawObject>();
    for (DrawObject obj : Contents) {
      if (obj.insideArea(x, y, width, height)) {
        DrawObject Clone = obj.clone();
        Clone.move(dx, dy);
        copyList.add(Clone);
      }
    }
    Contents.addAll(copyList);
  }
  
  public void setClip(int x, int y, int width, int height) {
    clip = new Rectangle(x,y,width,height);
  }
  
  public Rectangle getClip() {
    return clip;
  }
  
  public void drawGlyphVector(GlyphVector g, float x, float y) {
    for (int i=0 ; i < g.getNumGlyphs(); i++) {
      AffineTransform at = AffineTransform.getTranslateInstance(x, y);
      Point2D p = g.getGlyphPosition(i);
      at.transform(p, p);
      Shape shape = g.getGlyphOutline(i);
      Contents.add(new TikZBezier(p,shape,true));
    }
  }

  private void optimize() {
    ListIterator<DrawObject> l = Contents.listIterator();
    while (l.hasNext()) {
      DrawObject obj = l.next();
      if (obj instanceof TikZLine) {
        boolean merged = false;
        TikZLine line = (TikZLine) obj;
        for (int i = Contents.indexOf(obj)+1 ; i < Contents.size() ; i++) {
          DrawObject n = Contents.get(i);
          if (n instanceof TikZLine) {
            TikZLine mLine = (TikZLine) n;
            if (mLine.canMerge(line)) {
              merged = mLine.merge(line);
              if (merged) break;
            }
          }
        }
        if (merged)
          l.remove();
        else
          ((TikZLine) obj).closeIfPossible();
      }
    }
  }
  
  private String getCharRepresentation(int i) {
    StringBuffer chars = new StringBuffer();
    int repeat = i/26;
    int charId = i%26;
    for (int j = 0 ; j <= repeat ; j++)
      chars.append(String.valueOf((char)(charId+'A')));
    return chars.toString();
  }
  
  private String getFontDefinition(int i) {
    StringBuffer content = new StringBuffer();
    boolean replaced = false; 
    content.append("\\def\\logisimfont"+getCharRepresentation(i)+"#1{\\fontfamily{");
    String fontName = usedFonts.get(i); 
    if (fontName.contains("SansSerif")) {
      replaced = true;
      fontName = "cmr";
    } else if (fontName.contains("Monospaced")) {
      replaced = true;
      fontName = "cmtt";
    } else if (fontName.contains("Courier")) {
      replaced = true;
      fontName = "pcr";
    }
    content.append(fontName);
    content.append("}{#1}}");
    if (replaced)
      content.append(" % Replaced by logisim, original font was \""+usedFonts.get(i)+"\"");
    content.append("\n");
    return content.toString();
  }
  
  private String getColorDefinitions() {
    StringBuffer content = new StringBuffer();
    for (String key : customColors.keySet())
      content.append("\\definecolor{"+key+"}{RGB}{"+customColors.get(key)+"}\n");
    return content.toString();
  }
  
  public void WriteFile(File outfile) throws IOException {
    optimize();
    FileWriter writer = new FileWriter(outfile);
    writer.write("% Important: If latex complains about unicode characters, please use \"\\usepackage[utf8x]{inputenc}\" in your preamble\n");
    writer.write("% You can change the size of the picture by putting it into the construct:\n");
    writer.write("% 1) \\resizebox{10cm}{!}{\"below picture\"} to scale horizontally to 10 cm\n");
    writer.write("% 2) \\resizebox{!}{15cm}{\"below picture\"} to scale vertically to 15 cm\n");
    writer.write("% 3) \\resizebox{10cm}{15cm}{\"below picture\"} a combination of above two\n");
    writer.write("% It is not recomended to use the scale option of the tikzpicture environment.\n");
    writer.write("\\begin{tikzpicture}[x=1pt,y=-1pt,line cap=rect]\n");
    for (int i = 0 ; i < usedFonts.size() ; i++)
      writer.write(getFontDefinition(i));
    writer.write(getColorDefinitions());
    for (DrawObject obj : Contents)
      writer.write(obj.getTikZCommand()+"\n");
    writer.write("\\end{tikzpicture}\n\n");
    writer.close();
  }
  
  public void WriteSvg(int width,int height,File outfile) throws IOException,ParserConfigurationException,TransformerException {
    optimize();
    DocumentBuilderFactory factory;
    DocumentBuilder parser;
    Document svgInfo;
    // Create instance of DocumentBuilderFactory
    factory = DocumentBuilderFactory.newInstance();
    // Get the DocumentBuilder
    parser = factory.newDocumentBuilder();
    // Create blank DOM Document
    svgInfo = parser.newDocument();
    Element svg = svgInfo.createElement("svg");
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("version", "1.1");
    svg.setAttribute("viewBox", "0 0 "+width+" "+height);
    svgInfo.appendChild(svg);
    for (DrawObject obj : Contents)
      obj.getSvgCommand(svgInfo,svg);
    TransformerFactory tranFactory = TransformerFactory.newInstance();
    tranFactory.setAttribute("indent-number", 3);
    Transformer aTransformer = tranFactory.newTransformer();
    aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
    Source src = new DOMSource(svgInfo);
    Result dest = new StreamResult(outfile);
    aTransformer.transform(src, dest);
  }

  
}


