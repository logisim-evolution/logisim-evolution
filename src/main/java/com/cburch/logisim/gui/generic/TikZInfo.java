/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.util.XmlUtil;

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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TikZInfo implements Cloneable {

  private static final double BASIC_STROKE_WIDTH = 1;

  private AffineTransform myTransformer = new AffineTransform();
  private Color drawColor;
  private Color backColor;
  private ArrayList<DrawObject> contents = new ArrayList<>();
  private HashMap<String, String> customColors = new HashMap<>();
  private ArrayList<String> usedFonts = new ArrayList<>();
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

  public TikZInfo() {
    setFont(DrawAttr.DEFAULT_FONT);
    setBackground(Color.WHITE);
    setColor(Color.BLACK);
  }

  public static double rounded(double v) {
    return ((double) Math.round(v * 1000.0)) / 1000.0;
  }

  public static String getPoint(Point2D p) {
    return " (" + rounded(p.getX()) + "," + rounded(p.getY()) + ") ";
  }

  @Override
  public TikZInfo clone() {
    var newInst = new TikZInfo();
    newInst.myTransformer = (AffineTransform) myTransformer.clone();
    newInst.drawColor = drawColor;
    newInst.backColor = backColor;
    newInst.contents = contents;
    newInst.customColors = customColors;
    newInst.currentDrawColor = currentDrawColor;
    newInst.currentBackColor = currentBackColor;
    newInst.curFont = curFont;
    newInst.curStroke = curStroke;
    newInst.myRotation = myRotation;
    newInst.fontIndex = fontIndex;
    newInst.fontSize = fontSize;
    newInst.usedFonts = usedFonts;
    newInst.fontBold = fontBold;
    newInst.fontItalic = fontItalic;
    if (clip != null) newInst.clip = (Rectangle) clip.clone();
    return newInst;
  }

  private String getColorName(Color c) {
    String custName =
        "custcol_"
            + Integer.toString(c.getRed(), 16)
            + "_"
            + Integer.toString(c.getGreen(), 16)
            + "_"
            + Integer.toString(c.getBlue(), 16);
    String rgbCol =
        c.getRed()
            + ", "
            + c.getGreen()
            + ", "
            + c.getBlue();
    customColors.put(custName, rgbCol);
    return custName;
  }

  public void transform(Point2D src, Point2D dest) {
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
    return (myRotation / Math.PI) * 180;
  }

  public Color getColor() {
    return drawColor;
  }

  public void setColor(Color c) {
    currentDrawColor = getColorName(c);
    drawColor = c;
  }

  public Color getBackground() {
    return backColor;
  }

  public void setBackground(Color color) {
    backColor = color;
    currentBackColor = getColorName(color);
  }

  public Stroke getStroke() {
    return curStroke;
  }

  public void setStroke(Stroke stroke) {
    if (stroke instanceof BasicStroke s) curStroke = s;
    else System.out.println("TikZWriter: Unsupported Stroke set");
  }

  public AffineTransform getAffineTransform() {
    return myTransformer;
  }

  public void setAffineTransform(AffineTransform tx) {
    myTransformer = tx;
  }

  private double toDegree(double angle) {
    return (angle / Math.PI) * 180.0;
  }

  public void addLine(int x1, int y1, int x2, int y2) {
    contents.add(new TikZLine(x1, y1, x2, y2));
  }

  public void addBezier(Shape s, boolean filled) {
    contents.add(new TikZBezier(s, filled));
  }

  public void addRectangle(int x1, int y1, int x2, int y2, boolean filled, boolean backcolor) {
    TikZRectangle obj = new TikZRectangle(x1, y1, x2, y2, filled);
    if (backcolor) obj.setBackColor();
    contents.add(obj);
  }

  public void addRoundedRectangle(int x1, int y1, int x2, int y2, int arcWidth, int arcHeight, boolean filled) {
    int width = Math.abs(x1 - x2);
    int height = Math.abs(y1 - y2);
    int xDiameter = Math.max(0, Math.min(arcWidth, width));
    int yDiameter = Math.max(0, Math.min(arcHeight, height));
    //The previous two lines implement a just-in-case data normalization.
    //The SVG standard asserts this behavior, so I've replicated it here.
    if ((xDiameter == 0) || (yDiameter == 0)) {
      addRectangle(x1, y1, x2, y2, filled, false);
      return;
    }
    if ((xDiameter == width) && (yDiameter == height)) {
      addEllipse(x1, y1, width, height, filled);
      return;
    }
    contents.add(new TikZRectangle(x1, y1, x2, y2, xDiameter, yDiameter, filled));
  }

  public void addEllipse(int x, int y, int width, int height, boolean filled) {
    contents.add(new TikZEllipse(x, y, width, height, filled));
  }

  public void addArc(
      int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled) {
    contents.add(new TikZArc(x, y, width, height, startAngle, arcAngle, filled));
  }

  public void addPolyline(
      int[] xPoints, int[] yPoints, int nPoints, boolean filled, boolean closed) {
    contents.add(new TikZLine(xPoints, yPoints, nPoints, filled, closed));
  }

  public void addString(String str, int x, int y) {
    contents.add(new TikZString(str, x, y));
  }

  public void addString(AttributedCharacterIterator str, int x, int y) {
    contents.add(new TikZString(str, x, y));
  }

  public void rotate(double theta) {
    getAffineTransform().rotate(theta);
    myRotation += theta;
  }

  public void rotate(double theta, double x, double y) {
    getAffineTransform().rotate(theta, x, y);
    myRotation += theta;
  }

  public Font getFont() {
    return curFont;
  }

  public void setFont(Font f) {
    curFont = f;
    final var fontName = f.getFamily();
    fontSize = f.getSize();
    fontBold = f.isBold();
    fontItalic = f.isItalic();
    if (usedFonts.contains(fontName)) fontIndex = usedFonts.indexOf(fontName);
    else {
      usedFonts.add(fontName);
      fontIndex = usedFonts.size() - 1;
    }
  }

  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    final var copyList = new ArrayList<DrawObject>();
    for (final var obj : contents) {
      if (obj.insideArea(x, y, width, height)) {
        final var objClone = obj.clone();
        objClone.move(dx, dy);
        copyList.add(objClone);
      }
    }
    contents.addAll(copyList);
  }

  public void setClip(int x, int y, int width, int height) {
    clip = new Rectangle(x, y, width, height);
  }

  public Rectangle getClip() {
    return clip;
  }

  public void drawGlyphVector(GlyphVector g, float x, float y) {
    for (int i = 0; i < g.getNumGlyphs(); i++) {
      final var at = AffineTransform.getTranslateInstance(x, y);
      final var p = g.getGlyphPosition(i);
      at.transform(p, p);
      final var shape = g.getGlyphOutline(i);
      contents.add(new TikZBezier(p, shape, true));
    }
  }

  private void optimize() {
    final var l = contents.listIterator();
    while (l.hasNext()) {
      final var obj = l.next();
      if (obj instanceof TikZLine lineA) {
        var merged = false;
        for (var i = contents.indexOf(obj) + 1; i < contents.size(); i++) {
          final var n = contents.get(i);
          if (n instanceof TikZLine lineB) {
            if (lineB.canMerge(lineA)) {
              merged = lineB.merge(lineA);
              if (merged) break;
            }
          }
        }
        if (merged) l.remove();
      } else if (obj.getClass() == TikZEllipse.class) {
        //This non-instanceof check must be used so that we DON'T match with classes that extend TikZEllipse.
        final var ovalA = (TikZEllipse) obj;
        final var circular = ovalA.radX == ovalA.radY;
        var redundant = false;
        for (var i = contents.indexOf(obj) + 1; i < contents.size(); i++) {
          final var n = contents.get(i);
          if (n.getClass() == TikZEllipse.class) {
            final var ovalB = (TikZEllipse) n;
            final var centerMatch = ovalA.start.getX() == ovalB.start.getX() && ovalA.start.getY() == ovalB.start.getY();
            final var radiusMatch = ovalA.radX == ovalB.radX && ovalA.radY == ovalB.radY;
            final var rotaryMatch = ovalA.rotation == ovalB.rotation;
            if (centerMatch && radiusMatch && (circular || rotaryMatch)) {
              if (ovalA.filled && ovalB.filled) {
                redundant = true;
                break;
              } else if (!ovalA.filled && !ovalB.filled && (ovalA.strokeWidth == ovalB.strokeWidth)) {
                redundant = true;
                break;
              }
            }
          }
        }
        if (redundant) l.remove();
      }
    }
  }

  private String getCharRepresentation(int i) {
    final var repeat = i / 26;
    final var charId = i % 26;
    return String.valueOf((char) (charId + 'A')).repeat(repeat + 1);
  }

  private String getFontDefinition(int i) {
    final var content = new StringBuilder();
    var replaced = false;
    content
        .append("\\def\\logisimfont")
        .append(getCharRepresentation(i))
        .append("#1{\\fontfamily{");
    var fontName = usedFonts.get(i);
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
      content
          .append(" % Replaced by logisim, original font was \"")
          .append(usedFonts.get(i))
          .append("\"");
    content.append("\n");
    return content.toString();
  }

  private String getColorDefinitions() {
    final var content = new StringBuilder();
    for (final var key : customColors.keySet()) {
      content
          .append("\\definecolor{")
          .append(key)
          .append("}{RGB}{")
          .append(customColors.get(key))
          .append("}\n");
    }
    return content.toString();
  }

  public void writeFile(File outfile) throws IOException {
    optimize();
    final var writer = new FileWriter(outfile);
    writer.write("% Important: If latex complains about unicode characters,\n");
    writer.write("% please use \"\\usepackage[utf8x]{inputenc}\" in your preamble\n");
    writer.write("% You can change the size of the picture by putting it into the construct:\n");
    writer.write("% 1) \\resizebox{10cm}{!}{\"below picture\"} to scale horizontally to 10 cm\n");
    writer.write("% 2) \\resizebox{!}{15cm}{\"below picture\"} to scale vertically to 15 cm\n");
    writer.write("% 3) \\resizebox{10cm}{15cm}{\"below picture\"} a combination of above two\n");
    writer.write("% It is not recomended to use the scale option of the tikzpicture environment.\n");
    writer.write("\\begin{tikzpicture}[x=1pt,y=-1pt,line cap=rect]\n");
    for (int i = 0; i < usedFonts.size(); i++) writer.write(getFontDefinition(i));
    writer.write(getColorDefinitions());
    for (final var obj : contents) writer.write(obj.getTikZCommand() + "\n");
    writer.write("\\end{tikzpicture}\n");
    writer.close();
  }

  public void writeSvg(int width, int height, File outfile) throws ParserConfigurationException, TransformerException {
    optimize();
    final var factory = XmlUtil.getHardenedBuilderFactory();
    final var parser = factory.newDocumentBuilder();
    final var svgInfo = parser.newDocument();
    final var svg = svgInfo.createElement("svg");
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("version", "1.1");
    svg.setAttribute("viewBox", "0 0 " + width + " " + height);
    svgInfo.appendChild(svg);
    for (final var obj : contents) obj.getSvgCommand(svgInfo, svg);
    final var tranFactory = TransformerFactory.newInstance();
    tranFactory.setAttribute("indent-number", 3);
    final var transformer = tranFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    final var src = new DOMSource(svgInfo);
    final var dest = new StreamResult(outfile);
    transformer.transform(src, dest);
  }

  public interface DrawObject {
    String getTikZCommand();

    void getSvgCommand(Document root, Element e);

    boolean insideArea(int x, int y, int width, int height);

    DrawObject clone();

    void move(int dx, int dy);
  }

  private class AbstratctTikZ implements DrawObject {
    protected Point start;
    protected Point end;
    protected ArrayList<Point> points = new ArrayList<>();
    protected float strokeWidth;
    protected String color;
    protected double alpha;
    protected boolean filled;
    protected boolean close;

    public AbstratctTikZ() {}

    public AbstratctTikZ(int x1, int y1, int x2, int y2) {
      start = new Point(x1, y1);
      end = new Point(x2, y2);
      transform(start, start);
      transform(end, end);
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      alpha = (double) drawColor.getAlpha() / 255.0;
      points.clear();
      filled = false;
      close = false;
    }

    @Override
    public String getTikZCommand() {
      return "";
    }

    @Override
    public boolean insideArea(int x, int y, int width, int height) {
      final var left = new Point(x, y);
      final var right = new Point(x + width, y + height);
      transform(left, left);
      transform(right, right);
      final var x1 = Math.min(left.x, right.x);
      final var x2 = Math.max(left.x, right.x);
      final var y1 = Math.min(left.y, right.y);
      final var y2 = Math.max(left.y, right.y);
      var inside = true;
      if (points.isEmpty())
        return (start.x >= x1 && start.x <= x2)
            && (start.y >= y1 && start.y <= y2)
            && (end.x >= x1 && end.x <= x2)
            && (end.y >= y1 && end.y <= y2);
      else {
        for (final var point : points) {
          inside &= (point.x >= x1 && point.x <= x2) && (point.y >= y1 && point.y <= y2);
        }
      }
      return inside;
    }

    @Override
    public DrawObject clone() {
      return null;
    }

    @Override
    public void move(int dx, int dy) {
      final var move = new Point(dx, dy);
      transform(move, move);
      if (points.isEmpty()) {
        start = new Point(start.x + move.x, start.y + move.y);
        end = new Point(end.x + move.x, start.y + move.y);
      } else {
        for (final var point : points) {
          point.x += move.x;
          point.y += move.y;
        }
      }
    }

    @Override
    public void getSvgCommand(Document root, Element e) {}
  }

  private class TikZLine extends AbstratctTikZ {

    public TikZLine() {}

    public TikZLine(int x1, int y1, int x2, int y2) {
      super(x1, y1, x2, y2);
    }

    public TikZLine(int[] pointsX, int[] pointsY, int pointsCnt, boolean fill, boolean isPolygon) {
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      start = null;
      end = null;
      for (int i = 0; i < pointsCnt; i++) {
        final var p = new Point(pointsX[i], pointsY[i]);
        transform(p, p);
        points.add(p);
      }
      filled = fill;
      close = isPolygon;
    }

    public Point getStartPoint() {
      return points.isEmpty() ? start : points.get(0);
    }

    public Point getEndPoint() {
      return points.isEmpty() ? end : points.get(points.size() - 1);
    }

    public boolean canMerge(TikZLine l) {
      if (close || l.close) return false;
      if (!color.equals(l.color)) return false;
      if (strokeWidth != l.strokeWidth) return false;
      if (filled || l.filled) return false;
      if (getStartPoint().equals(l.getEndPoint())) return true;
      if (getEndPoint().equals(l.getStartPoint())) return true;
      if (getStartPoint().equals(l.getStartPoint())) return true;
      return getEndPoint().equals(l.getEndPoint());
    }

    private void addPoints(ArrayList<Point> p, int start, int end, boolean atEnd, boolean reverseOrder) {
      if (atEnd) {
        if (reverseOrder) for (int i = end - 1; i >= start; i--) points.add(p.get(i));
        else for (int i = start; i < end; i++) points.add(p.get(i));
      } else {
        if (reverseOrder) for (int i = start; i < end; i++) points.add(0, p.get(i));
        else for (int i = end - 1; i >= start; i--) points.add(0, p.get(i));
      }
    }

    public boolean merge(TikZLine l) {
      if (!canMerge(l)) return false;
      if (points.isEmpty()) {
        points.add(start);
        points.add(end);
      }
      if (l.points.isEmpty()) {
        l.points.add(l.start);
        l.points.add(l.end);
      }
      if (getStartPoint().equals(l.getEndPoint())) {
        addPoints(l.points, 0, l.points.size() - 1, false, false);
      } else if (getEndPoint().equals(l.getStartPoint())) {
        addPoints(l.points, 1, l.points.size(), true, false);
      } else if (getStartPoint().equals(l.getStartPoint())) {
        addPoints(l.points, 1, l.points.size(), false, true);
      } else if (getEndPoint().equals(l.getEndPoint())) {
        addPoints(l.points, 0, l.points.size() - 1, true, true);
      } else return false;
      return true;
    }

    @Override
    public String getTikZCommand() {
      final var contents = new StringBuilder();
      if (filled) contents.append("\\fill ");
      else contents.append("\\draw ");
      contents.append("[line width=");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      contents.append(rounded(width)).append("pt, ").append(color).append("]");
      if (points.isEmpty()) {
        contents.append(getPoint(start));
        contents.append("--");
        contents.append(getPoint(end));
      } else {
        var first = true;
        for (final var point : points) {
          if (first) first = false;
          else contents.append("--");
          contents.append(getPoint(point));
        }
      }
      if (close) {
        contents.append("-- cycle;");
      } else {
        //This is necessary to eliminate space between last point and final semicolon.
        contents.setCharAt(contents.length() - 1, ';');
      }
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var content = new StringBuilder();
      final var ne = root.createElement(close ? "polygon" : "polyline");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb(" + customColors.get(color) + ")" : "none");
      ne.setAttribute("stroke", filled ? "none" : "rgb(" + customColors.get(color) + ")");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      ne.setAttribute("stroke-linecap", "square");
      if (points.isEmpty()) {
        content.append(start.x).append(",").append(start.y).append(" ").append(end.x).append(",")
            .append(end.y);
      } else {
        var first = true;
        for (final var point : points) {
          if (first) first = false;
          else content.append(" ");
          content.append(point.x).append(",").append(point.y);
        }
      }
      ne.setAttribute("points", content.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public DrawObject clone() {
      final var newIns = new TikZLine();
      newIns.start = (Point) start.clone();
      newIns.end = (Point) end.clone();
      newIns.strokeWidth = strokeWidth;
      newIns.color = color;
      newIns.points = (ArrayList<Point>) points.clone();
      newIns.filled = filled;
      newIns.close = close;
      return newIns;
    }
  }

  private class TikZBezier extends AbstratctTikZ {
    private final ArrayList<BezierInfo> myPath = new ArrayList<>();

    public TikZBezier() {}

    public TikZBezier(Shape s, boolean filled) {
      final var p = new Point2D.Double();
      p.setLocation(0, 0);
      create(p, s, filled);
    }

    public TikZBezier(Point2D orig, Shape s, boolean filled) {
      create(orig, s, filled);
    }

    private void create(Point2D origin, Shape s, boolean filled) {
      this.filled = filled;
      this.color = getDrawColorString();
      this.alpha = (double) drawColor.getAlpha() / 255.0;
      this.strokeWidth = getStrokeWidth();
      final var at = AffineTransform.getTranslateInstance(origin.getX(), origin.getY());
      final var p = s.getPathIterator(at);
      while (!p.isDone()) {
        final var coords = new double[6];
        final var type = p.currentSegment(coords);
        if (type == PathIterator.SEG_MOVETO) {
          final var current = new Point2D.Double();
          current.setLocation(coords[0], coords[1]);
          myPath.add(new BezierInfo(current, true, filled));
        } else if (type == PathIterator.SEG_LINETO) {
          final var next = new Point2D.Double();
          next.setLocation(coords[0], coords[1]);
          myPath.add(new BezierInfo(next, false, false));
        } else if (type == PathIterator.SEG_CLOSE) {
          myPath.add(new BezierInfo());
        } else if (type == PathIterator.SEG_QUADTO) {
          final var next = new Point2D.Double();
          final var control = new Point2D.Double();
          control.setLocation(coords[0], coords[1]);
          next.setLocation(coords[2], coords[3]);
          myPath.add(new BezierInfo(control, next));
        } else if (type == PathIterator.SEG_CUBICTO) {
          final var next = new Point2D.Double();
          final var control1 = new Point2D.Double();
          final var control2 = new Point2D.Double();
          control1.setLocation(coords[0], coords[1]);
          control2.setLocation(coords[2], coords[3]);
          next.setLocation(coords[4], coords[5]);
          myPath.add(new BezierInfo(control1, control2, next));
        }
        p.next();
      }
    }

    @Override
    public String getTikZCommand() {
      final var contents = new StringBuilder();
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      contents.append(filled ? "\\fill " : "\\draw ").append("[line width=").append(rounded(width)).append("pt, ").append(color);
      if (filled && alpha != 1.0) contents.append(", fill opacity=").append(rounded(alpha));
      contents.append("]");
      for (final var point : myPath) {
        contents.append(point.getTikZCommand());
      }
      //The output from BezierInfo::getTikZCommand() will always end in a space, so:
      contents.setCharAt(contents.length() - 1, ';');
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var ne = root.createElement("path");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb(" + customColors.get(color) + ")" : "none");
      if (filled && alpha != 1.0) ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb(" + customColors.get(color) + ")");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      ne.setAttribute("stroke-linecap", "square");
      final var content = new StringBuilder();
      for (final var point : myPath) {
        content.append(point.getSvgPath());
      }
      ne.setAttribute("d", content.toString());
    }

    @Override
    public boolean insideArea(int x, int y, int width, int height) {
      var inside = true;
      for (final var point : myPath) inside &= point.insideArea(x, y, width, height);
      return inside;
    }

    @Override
    public DrawObject clone() {
      final var newInst = new TikZBezier();
      newInst.filled = filled;
      newInst.color = color;
      newInst.alpha = alpha;
      newInst.strokeWidth = strokeWidth;
      for (final var point : myPath) newInst.myPath.add(point.clone());
      return newInst;
    }

    @Override
    public void move(int dx, int dy) {
      for (final var point : myPath) point.move(dx, dy);
    }

    private class BezierInfo implements Cloneable {
      private Point2D startPoint;
      private Point2D controlPoint1;
      private Point2D controlPoint2;
      private Point2D endPoint;
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
        closePath = false;
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

      @Override
      public BezierInfo clone() {
        final var newInst = new BezierInfo();
        newInst.startPoint = startPoint;
        newInst.controlPoint1 = controlPoint1;
        newInst.controlPoint2 = controlPoint2;
        newInst.endPoint = endPoint;
        newInst.closePath = closePath;
        return newInst;
      }

      public void move(int dx, int dy) {
        final var at = AffineTransform.getTranslateInstance(dx, dy);
        if (startPoint != null) at.transform(startPoint, startPoint);
        if (controlPoint1 != null) at.transform(controlPoint1, controlPoint1);
        if (controlPoint2 != null) at.transform(controlPoint2, controlPoint2);
        if (endPoint != null) at.transform(endPoint, endPoint);
      }

      public String getTikZCommand() {
        final var contents = new StringBuilder();
        if (closePath) {
          contents.append("-- cycle ");
        } else if (startPoint != null) {
          contents.append(getPoint(startPoint));
        } else {
          if (controlPoint1 == null && controlPoint2 == null) {
            contents.append("--").append(getPoint(endPoint));
          } else {
            contents.append(".. controls").append(getPoint(controlPoint1));
            if (controlPoint2 != null) contents.append("and").append(getPoint(controlPoint2));
            contents.append("..").append(getPoint(endPoint));
          }
        }
        return contents.toString();
      }

      public String getSvgPath() {
        final var contents = new StringBuilder();
        if (closePath) {
          contents.append(" Z");
        } else if (startPoint != null) {
          contents.append(" M").append(startPoint.getX()).append(",").append(startPoint.getY());
        } else {
          if (controlPoint1 == null && controlPoint2 == null) {
            contents.append(" L").append(endPoint.getX()).append(",").append(endPoint.getY());
          } else {
            final var singlePoint = (controlPoint2 == null) ? controlPoint1 : controlPoint2;
            contents.append(" C").append(controlPoint1.getX()).append(",")
                .append(controlPoint1.getY());
            contents.append(" ").append(singlePoint.getX()).append(",").append(singlePoint.getY());
            contents.append(" ").append(endPoint.getX()).append(",").append(endPoint.getY());
          }
        }
        return contents.toString();
      }

      public boolean insideArea(int x, int y, int width, int height) {
        if (closePath) return true;
        var inside = true;
        final var x2 = x + width;
        final var y2 = y + height;
        if (startPoint != null)
          inside &=
              (startPoint.getX() >= (double) x && startPoint.getX() <= x2)
                  && (startPoint.getY() >= (double) y && startPoint.getY() <= y2);
        if (endPoint != null)
          inside &=
              (endPoint.getX() >= (double) x && endPoint.getX() <= x2)
                  && (endPoint.getY() >= (double) y && endPoint.getY() <= y2);
        return inside;
      }
    }
  }

  private class TikZRectangle extends AbstratctTikZ {
    Point2D rad;

    public TikZRectangle() {}

    public TikZRectangle(
        int x1, int y1, int x2, int y2, int arcwidth, int archeight, boolean filled) {
      super(x1, y1, x2, y2);
      rad = new Point2D.Double();
      rad.setLocation(((double) arcwidth) / 2.0, ((double) archeight) / 2.0);
      this.filled = filled;
    }

    public TikZRectangle(int x1, int y1, int x2, int y2, boolean filled) {
      super(x1, y1, x2, y2);
      this.filled = filled;
      rad = null;
    }

    public void setBackColor() {
      this.color = getBackColorString();
    }

    @Override
    public DrawObject clone() {
      final var newIns = new TikZRectangle();
      newIns.start = (Point) start.clone();
      newIns.end = (Point) end.clone();
      newIns.strokeWidth = strokeWidth;
      newIns.color = color;
      newIns.filled = filled;
      newIns.rad = (Point2D) rad.clone();
      newIns.alpha = alpha;
      return newIns;
    }

    private static String quickPointTrim(double x, double y) {
      return "(" + rounded(x) + "," + rounded(y) + ")";
    }

    private static String quickPoint(double x, double y) {
      return " " + quickPointTrim(x, y) + " ";
    }

    @Override
    public String getTikZCommand() {
      final var contents = new StringBuilder();
      contents.append(filled ? "\\fill " : "\\draw ");
      contents.append("[line width=");
      final var ptWidth = strokeWidth * BASIC_STROKE_WIDTH;
      contents.append(rounded(ptWidth)).append("pt, ").append(color);
      if (filled && alpha != 1.0) contents.append(", fill opacity=").append(rounded(alpha));
      if (rad == null) {
        contents.append("]");
        contents.append(getPoint(start));
        contents.append("rectangle");
        contents.append(getPoint(end));
        contents.setCharAt(contents.length() - 1, ';');
      } else {
        final double width = Math.abs(start.getX() - end.getX());
        final double height = Math.abs(start.getY() - end.getY());
        final double xRadius = rad.getX();
        final double yRadius = rad.getY();
        final double xHalf = width / 2.0;
        final double yHalf = height / 2.0;
        final double xGap = xHalf - xRadius;
        final double yGap = yHalf - yRadius;
        final double xMid = Math.min(start.getX(), end.getX()) + (width / 2.0);
        final double yMid = Math.min(start.getY(), end.getY()) + (height / 2.0);
        contents.append(", shift={").append(quickPointTrim(xMid, yMid)).append("}, ");
        contents.append("x radius=").append(xRadius).append(", ");
        contents.append("y radius=").append(yRadius).append("]");
        if (xRadius >= xHalf) {
          //This if check could be a "==" due to the normalization in addRoundedRectangle(),
          //but I'm using a ">=" here just in case of floating-point funny business.
          contents.append(quickPoint(-xHalf, -yGap));
          contents.append("arc[start angle=180, delta angle=180] --");
          contents.append(quickPoint(xHalf, yGap));
          contents.append("arc[start angle=0, delta angle=180] -- cycle;");
        } else if (yRadius >= yHalf) {
          //This if check could be a "==" due to the normalization in addRoundedRectangle(),
          //but I'm using a ">=" here just in case of floating-point funny business.
          contents.append(quickPoint(xGap, -yHalf));
          contents.append("arc[start angle=270, delta angle=180] --");
          contents.append(quickPoint(-xGap, yHalf));
          contents.append("arc[start angle=90, delta angle=180] -- cycle;");
        } else {
          contents.append(quickPoint(xGap, -yHalf));
          contents.append("arc[start angle=270, delta angle=90] --");
          contents.append(quickPoint(xHalf, yGap));
          contents.append("arc[start angle=0, delta angle=90] --");
          contents.append(quickPoint(-xGap, yHalf));
          contents.append("arc[start angle=90, delta angle=90] --");
          contents.append(quickPoint(-xHalf, -yGap));
          contents.append("arc[start angle=180, delta angle=90] -- cycle;");
        }
      }
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var ne = root.createElement("rect");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb(" + customColors.get(color) + ")" : "none");
      if (filled && alpha != 1.0) ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb(" + customColors.get(color) + ")");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      ne.setAttribute("stroke-linecap", "square");
      if (rad != null) {
        ne.setAttribute("rx", Double.toString(rad.getX()));
        ne.setAttribute("ry", Double.toString(rad.getY()));
      }
      final var xpos = Math.min(end.x, start.x);
      final var bwidth = Math.abs(end.x - start.x);
      final var ypos = Math.min(end.y, start.y);
      final var bheight = Math.abs(end.y - start.y);
      ne.setAttribute("x", Integer.toString(xpos));
      ne.setAttribute("y", Integer.toString(ypos));
      ne.setAttribute("width", Integer.toString(bwidth));
      ne.setAttribute("height", Integer.toString(bheight));
    }
  }

  private class TikZEllipse extends AbstratctTikZ {

    protected double radX;
    protected double radY;
    protected int rotation;

    public TikZEllipse() {}

    public TikZEllipse(int x, int y, int width, int height, boolean filled) {
      super(x + (width >> 1), y + (height >> 1), 0, 0);
      init(width, height, filled);
    }

    private void init(int width, int height, boolean filled) {
      this.filled = filled;
      radX = ((double) width) / 2.0;
      radY = ((double) height) / 2.0;
      rotation = (int) getRotationDegrees();
    }

    @Override
    public DrawObject clone() {
      final var newIns = new TikZEllipse();
      newIns.start = (Point) start.clone();
      newIns.end = (Point) end.clone();
      newIns.strokeWidth = strokeWidth;
      newIns.color = color;
      newIns.filled = filled;
      newIns.radX = radX;
      newIns.radY = radY;
      newIns.rotation = rotation;
      newIns.alpha = alpha;
      return newIns;
    }

    @Override
    public String getTikZCommand() {
      final var circular = radX == radY;
      final var contents = new StringBuilder();
      contents.append(filled ? "\\fill " : "\\draw ");
      contents.append("[line width=");
      double width = strokeWidth * BASIC_STROKE_WIDTH;
      contents.append(rounded(width)).append("pt, ").append(color);
      if (!circular && rotation != 0) {
        //Circles look the same when rotated in any orientation.
        //Therefore, only apply rotation handling for non-circular ellipses.
        contents.append(", rotate around={").append(this.rotation).append(":")
            .append(getPoint(start)).append("}");
      }
      if (filled && alpha != 1.0) contents.append(", fill opacity=").append(rounded(alpha));
      contents.append("]");
      contents.append(getPoint(start));
      if (circular) {
        contents.append("circle (").append(radX).append(");");
      } else {
        contents.append("ellipse (").append(radX).append(" and ").append(radY).append(");");
      }
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var circular = radX == radY;
      final var ne = circular ? root.createElement("circle") : root.createElement("ellipse");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb(" + customColors.get(color) + ")" : "none");
      if (filled && alpha != 1.0) ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb(" + customColors.get(color) + ")");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      if (!circular && rotation != 0) {
        //Circles look the same when rotated in any orientation.
        //Therefore, only apply rotation handling for non-circular ellipses.
        ne.setAttribute(
            "transform",
            "translate(" + start.getX() + " " + start.getY() + ") rotate(" + this.rotation + ")");
      } else {
        ne.setAttribute("cx", Double.toString(start.getX()));
        ne.setAttribute("cy", Double.toString(start.getY()));
      }
      if (circular) {
        ne.setAttribute("r", Double.toString(Math.abs(radX)));
      } else {
        ne.setAttribute("rx", Double.toString(Math.abs(radX)));
        ne.setAttribute("ry", Double.toString(Math.abs(radY)));
      }
    }
  }

  private class TikZArc extends TikZEllipse {
    private double startAngle;
    private double stopAngle;
    private Point2D startPos = new Point2D.Double();
    private Point2D stopPos = new Point2D.Double();

    public TikZArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
      filled = fill;
      strokeWidth = getStrokeWidth();
      color = getDrawColorString();
      points.clear();
      close = false;
      final var radius = new Point2D.Double();
      final var center = new Point2D.Double();
      radius.setLocation(((double) width) / 2.0, ((double) height) / 2.0);
      center.setLocation(((double) x) + radius.getX(), ((double) y) + radius.getY());
      final double startAnglePi = ((double) startAngle * Math.PI) / 180.0;
      final double startX = center.getX() + radius.getX() * Math.cos(startAnglePi);
      final double startY = center.getY() - radius.getY() * Math.sin(startAnglePi);
      final double stopAnglePi = ((double) (startAngle + arcAngle) * Math.PI) / 180.0;
      final double stopX = center.getX() + radius.getX() * Math.cos(stopAnglePi);
      final double stopY = center.getY() - radius.getY() * Math.sin(stopAnglePi);
      radX = radius.getX();
      radY = radius.getY();
      this.startAngle = -toDegree(startAnglePi);
      stopAngle = -toDegree(stopAnglePi);
      rotation = (int) getRotationDegrees();
      this.startAngle += rotation;
      stopAngle += rotation;
      startPos.setLocation(startX, startY);
      transform(startPos, startPos);
      stopPos.setLocation(stopX, stopY);
      transform(stopPos, stopPos);
    }

    public TikZArc() {}

    @Override
    public DrawObject clone() {
      final var newIns = new TikZArc();
      newIns.strokeWidth = strokeWidth;
      newIns.color = color;
      newIns.filled = filled;
      newIns.radX = radX;
      newIns.radY = radY;
      newIns.rotation = rotation;
      newIns.startAngle = startAngle;
      newIns.stopAngle = stopAngle;
      newIns.alpha = alpha;
      newIns.startPos = (Point2D) startPos.clone();
      newIns.stopPos = (Point2D) stopPos.clone();
      return newIns;
    }

    @Override
    public String getTikZCommand() {
      final var contents = new StringBuilder();
      contents.append(filled ? "\\fill " : "\\draw ");
      contents.append("[line width=");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      contents.append(rounded(width)).append("pt, ").append(color);
      if (filled && alpha != 1.0) contents.append(", fill opacity=").append(rounded(alpha));
      contents.append("] ");
      contents.append("(").append(rounded(startPos.getX())).append(",")
          .append(rounded(startPos.getY())).append(")");
      contents.append(" arc (").append(startAngle).append(":").append(stopAngle).append(":")
          .append(radX).append(" and ").append(radY).append(" );");
      return contents.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var ne = root.createElement("path");
      e.appendChild(ne);
      ne.setAttribute("fill", filled ? "rgb(" + customColors.get(color) + ")" : "none");
      if (filled && alpha != 1.0) ne.setAttribute("fill-opacity", Double.toString(rounded(alpha)));
      ne.setAttribute("stroke", filled ? "none" : "rgb(" + customColors.get(color) + ")");
      final var width = strokeWidth * BASIC_STROKE_WIDTH;
      ne.setAttribute("stroke-width", Double.toString(rounded(width)));
      String info = startAngle > stopAngle ? " 0,0 " : " 0,1 ";
      String content = "M" + startPos.getX() + "," + startPos.getY()
          + " A" + radX + "," + radY + " " + this.startAngle
          + info + stopPos.getX() + "," + stopPos.getY();
      ne.setAttribute("d", content);
    }
  }

  private class TikZString implements DrawObject {
    private Point location;
    private String name;
    private AttributedCharacterIterator strIter;
    private String color;
    private double rotation;
    private int fontIndex;
    private int fontSize;
    private boolean isFontBold;
    private boolean isFontItalic;

    public TikZString() {}

    public TikZString(String str, int x, int y) {
      name = str;
      strIter = null;
      init(x, y);
      fontIndex = TikZInfo.this.fontIndex;
      fontSize = TikZInfo.this.fontSize;
      isFontBold = fontBold;
      isFontItalic = fontItalic;
    }

    public TikZString(AttributedCharacterIterator str, int x, int y) {
      name = null;
      strIter = str;
      if (str.getAttribute(TextAttribute.FAMILY) == null || str.getAttribute(TextAttribute.FAMILY).equals("Default")) {
        fontIndex = TikZInfo.this.fontIndex;
        fontSize = TikZInfo.this.fontSize;
        isFontBold = fontBold;
        isFontItalic = fontItalic;
      } else {
        isFontBold = str.getAttribute(TextAttribute.WEIGHT) == TextAttribute.WEIGHT_BOLD;
        isFontItalic = false;
        fontSize = (int) str.getAttribute(TextAttribute.SIZE);
        final var fontName = (String) str.getAttribute(TextAttribute.FAMILY);
        if (!usedFonts.contains(fontName)) usedFonts.add(fontName);
        fontIndex = usedFonts.indexOf(fontName);
      }
      init(x, y);
    }

    private void init(int x, int y) {
      rotation = -getRotationDegrees();
      location = new Point(x, y);
      transform(location, location);
      color = getDrawColorString();
    }

    private String getAttrString(boolean svg, Document root, Element e) {
      strIter.first();
      var content = new StringBuilder();
      Element tspan = null;
      if (!svg) content.append("$\\text{");
      else tspan = root.createElement("tspan");
      while (strIter.getIndex() < strIter.getEndIndex()) {
        if (strIter.getAttribute(TextAttribute.SUPERSCRIPT) == TextAttribute.SUPERSCRIPT_SUB) {
          if (svg) {
            if (content.length() > 0) {
              e.appendChild(tspan);
              tspan.setTextContent(content.toString());
              content = new StringBuilder();
            }
            tspan = root.createElement("tspan");
            tspan.setAttribute("dy", "3");
            tspan.setAttribute("font-size", ".7em");
          } else content.append("}_{\\text{");
          while (strIter.getIndex() < strIter.getEndIndex()
              && strIter.getAttribute(TextAttribute.SUPERSCRIPT) == TextAttribute.SUPERSCRIPT_SUB) {
            final var kar = strIter.current();
            if (kar == '_' && !svg) content.append("\\_");
            if (kar == '&' && !svg) content.append("\\&");
            else content.append(kar);
            strIter.next();
          }
          if (svg) {
            if (content.length() > 0) {
              e.appendChild(tspan);
              tspan.setTextContent(content.toString());
              content = new StringBuilder();
              tspan = root.createElement("tspan");
              tspan.setAttribute("dy", "-3");
            } else tspan = root.createElement("tspan");
          } else content.append("}}\\text{");
        } else {
          char kar = strIter.current();
          if (kar == '\u22C5' && !svg) {
            content.append("}\\cdot\\text{");
          } else if (kar == '_' && !svg) {
            content.append("\\_");
          } else if (kar == '&' && !svg) {
            content.append("\\&");
          } else {
            content.append(kar);
          }
          strIter.next();
        }
      }
      if (!svg) content.append("}$");
      else if (content.length() > 0) {
        e.appendChild(tspan);
        tspan.setTextContent(content.toString());
        content = new StringBuilder();
      }
      return content.toString();
    }

    @Override
    public String getTikZCommand() {
      final var content = new StringBuilder();
      content.append("\\logisimfont").append(getCharRepresentation(fontIndex)).append("{");
      content.append("\\fontsize{").append(fontSize).append("pt}{").append(fontSize).append("pt}");
      if (isFontBold) content.append("\\fontseries{bx}");
      if (isFontItalic) content.append("\\fontshape{it}");
      content.append("\\selectfont\\node[inner sep=0, outer sep=0, ").append(color)
          .append(", anchor=base west");
      if (rotation != 0) content.append(", rotate=").append(this.rotation);
      content.append("] at ").append(getPoint(location)).append(" {");
      if (name != null)
        if (name.isEmpty()) return "";
        else
          for (var i = 0; i < name.length(); i++) {
            char kar = name.charAt(i);
            if (kar == '_' || kar == '&') content.append("\\");
            content.append(kar);
          }
      else content.append(getAttrString(false, null, null));
      content.append("};}");
      return content.toString();
    }

    @Override
    public void getSvgCommand(Document root, Element e) {
      final var ne = root.createElement("text");
      ne.setAttribute("font-family", usedFonts.get(TikZInfo.this.fontIndex));
      ne.setAttribute("font-size", Integer.toString(fontSize));
      if (isFontBold) ne.setAttribute("font-weight", "bold");
      if (isFontItalic) ne.setAttribute("font-style", "italic");
      if (this.rotation != 0)
        ne.setAttribute(
            "transform",
            "rotate("
                + -this.rotation
                + ","
                + location.getX()
                + ","
                + location.getY()
                + ")");
      ne.setAttribute("x", Double.toString(location.getX()));
      ne.setAttribute("y", Double.toString(location.getY()));
      ne.setAttribute("fill", "rgb(" + customColors.get(color) + ")");
      if (name != null) {
        ne.setTextContent(name);
        if (!name.isEmpty()) e.appendChild(ne);
      } else {
        getAttrString(true, root, ne);
        e.appendChild(ne);
      }
    }

    @Override
    public boolean insideArea(int x, int y, int width, int height) {
      return (location.x >= x && location.x <= (x + width))
          && (location.y >= y && location.y <= (y + height));
    }

    @Override
    public DrawObject clone() {
      final var newInst = new TikZString();
      newInst.fontIndex = fontIndex;
      newInst.fontSize = fontSize;
      newInst.isFontBold = isFontBold;
      newInst.isFontItalic = isFontItalic;
      newInst.location = (Point) location.clone();
      newInst.name = name;
      newInst.color = color;
      newInst.rotation = rotation;
      return newInst;
    }

    @Override
    public void move(int dx, int dy) {
      location = new Point(location.x + dx, location.y + dy);
    }
  }
}
