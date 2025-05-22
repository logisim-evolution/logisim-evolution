/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class TikZWriter extends Graphics2D {

  private final TikZInfo MyInfo;

  public TikZWriter() {
    MyInfo = new TikZInfo();
  }

  public TikZWriter(TikZInfo info) {
    MyInfo = info;
  }

  @Override
  public void draw(Shape s) {
    MyInfo.addBezier(s, false);
  }

  @Override
  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, AffineTransform xform, ImageObserver obs)");
    return false;
  }

  @Override
  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    System.out.println(
        "TikZ not yet supported : drawImage(BufferedImage img, BufferedImageOp op, int x, int y)");
  }

  @Override
  public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    System.out.println(
        "TikZ not yet supported : drawRenderedImage(RenderedImage img, AffineTransform xform)");
  }

  @Override
  public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    System.out.println(
        "TikZ not yet supported : drawRenderableImage(RenderableImage img, AffineTransform xform)");
  }

  @Override
  public void drawString(String str, int x, int y) {
    MyInfo.addString(str, x, y);
  }

  @Override
  public void drawString(String str, float x, float y) {
    MyInfo.addString(str, (int) x, (int) y);
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    MyInfo.addString(iterator, x, y);
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    MyInfo.addString(iterator, (int) x, (int) y);
  }

  @Override
  public void drawGlyphVector(GlyphVector g, float x, float y) {
    MyInfo.drawGlyphVector(g, x, y);
  }

  @Override
  public void fill(Shape s) {
    MyInfo.addBezier(s, true);
  }

  @Override
  public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
    System.out.println("TikZ not yet supported : hit(Rectangle rect, Shape s, boolean onStroke)");
    return false;
  }

  @Override
  public GraphicsConfiguration getDeviceConfiguration() {
    System.out.println("TikZ not yet supported : GraphicsConfiguration getDeviceConfiguration()");
    return null;
  }

  @Override
  public void setRenderingHint(Key hintKey, Object hintValue) {
    System.out.println("TikZ not yet supported : setRenderingHint(Key hintKey, Object hintValue)");
  }

  @Override
  public Object getRenderingHint(Key hintKey) {
    System.out.println("TikZ not yet supported : getRenderingHint(Key hintKey)");
    return null;
  }

  @Override
  public void addRenderingHints(Map<?, ?> hints) {
    System.out.println("TikZ not yet supported : addRenderingHints(Map<?, ?> hints)");
  }

  @Override
  public RenderingHints getRenderingHints() {
    System.out.println("TikZ not yet supported : RenderingHints getRenderingHints()");
    return null;
  }

  @Override
  public void setRenderingHints(Map<?, ?> hints) {
    System.out.println("TikZ not yet supported : setRenderingHints(Map<?, ?> hints)");
  }

  @Override
  public void translate(int x, int y) {
    MyInfo.getAffineTransform().translate(x, y);
  }

  @Override
  public void translate(double tx, double ty) {
    MyInfo.getAffineTransform().translate(tx, ty);
  }

  @Override
  public void rotate(double theta) {
    MyInfo.rotate(theta);
  }

  @Override
  public void rotate(double theta, double x, double y) {
    MyInfo.rotate(theta, x, y);
  }

  @Override
  public void scale(double sx, double sy) {
    MyInfo.getAffineTransform().scale(sx, sy);
  }

  @Override
  public void shear(double shx, double shy) {
    MyInfo.getAffineTransform().shear(shx, shy);
  }

  @Override
  public void transform(AffineTransform tx) {
    MyInfo.getAffineTransform().concatenate(tx);
  }

  @Override
  public AffineTransform getTransform() {
    return (AffineTransform) MyInfo.getAffineTransform().clone();
  }

  @Override
  public void setTransform(AffineTransform tx) {
    MyInfo.setAffineTransform(tx);
  }

  @Override
  public Paint getPaint() {
    System.out.println("TikZ not yet supported : getPaint()");
    return null;
  }

  @Override
  public void setPaint(Paint paint) {
    System.out.println("TikZ not yet supported : setPaint(Paint paint)");
  }

  @Override
  public Composite getComposite() {
    System.out.println("TikZ not yet supported : getComposite()");
    return null;
  }

  @Override
  public void setComposite(Composite comp) {
    System.out.println("TikZ not yet supported : setComposite(Composite comp)");
  }

  @Override
  public Color getBackground() {
    return MyInfo.getBackground();
  }

  @Override
  public void setBackground(Color color) {
    MyInfo.setBackground(color);
  }

  @Override
  public Stroke getStroke() {
    return MyInfo.getStroke();
  }

  @Override
  public void setStroke(Stroke s) {
    MyInfo.setStroke(s);
  }

  @Override
  public void clip(Shape s) {
    System.out.println("TikZ not yet supported : clip(Shape s)");
  }

  @Override
  public FontRenderContext getFontRenderContext() {
    /* TODO: just stubs, not related to LaTeX */
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();
    GraphicsConfiguration config = gd.getDefaultConfiguration();
    Canvas c = new Canvas(config);
    return c.getFontMetrics(MyInfo.getFont()).getFontRenderContext();
  }

  @Override
  public Graphics create() {
    return new TikZWriter(MyInfo.clone());
  }

  @Override
  public Color getColor() {
    return MyInfo.getColor();
  }

  @Override
  public void setColor(Color c) {
    MyInfo.setColor(c);
  }

  @Override
  public void setPaintMode() {
    // default mode
  }

  @Override
  public void setXORMode(Color c1) {
    System.out.println("TikZWriter not yet supported : setXORMode!");
  }

  @Override
  public Font getFont() {
    return MyInfo.getFont();
  }

  @Override
  public void setFont(Font font) {
    MyInfo.setFont(font);
  }

  @Override
  public FontMetrics getFontMetrics(Font f) {
    /* TODO: just stubs, not related to LaTeX */
    final var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final var gd = ge.getDefaultScreenDevice();
    final var config = gd.getDefaultConfiguration();
    final var c = new Canvas(config);
    return c.getFontMetrics(f);
  }

  @Override
  public Rectangle getClipBounds() {
    return MyInfo.getClip();
  }

  @Override
  public void clipRect(int x, int y, int width, int height) {
    System.out.println("TikZ not yet supported : clipRect(int x, int y, int width, int height)");
  }

  @Override
  public void setClip(int x, int y, int width, int height) {
    MyInfo.setClip(x, y, width, height);
  }

  @Override
  public Shape getClip() {
    System.out.println("TikZ not yet supported : getClip()");
    return null;
  }

  @Override
  public void setClip(Shape clip) {
    System.out.println("TikZ not yet supported : setClip(Shape clip)");
  }

  @Override
  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    MyInfo.copyArea(x, y, width, height, dx, dy);
  }

  @Override
  public void drawLine(int x1, int y1, int x2, int y2) {
    MyInfo.addLine(x1, y1, x2, y2);
  }

  @Override
  public void fillRect(int x, int y, int width, int height) {
    MyInfo.addRectangle(x, y, x + width, y + height, true, false);
  }

  @Override
  public void drawRect(int x, int y, int width, int height) {
    MyInfo.addRectangle(x, y, x + width, y + height, false, false);
  }

  @Override
  public void clearRect(int x, int y, int width, int height) {
    MyInfo.addRectangle(x, y, x + width, y + height, true, true);
  }

  @Override
  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    MyInfo.addRoundedRectangle(x, y, x + width, y + height, arcWidth, arcHeight, false);
  }

  @Override
  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    MyInfo.addRoundedRectangle(x, y, x + width, y + height, arcWidth, arcHeight, true);
  }

  @Override
  public void drawOval(int x, int y, int width, int height) {
    MyInfo.addEllipse(x, y, width, height, false);
  }

  @Override
  public void fillOval(int x, int y, int width, int height) {
    MyInfo.addEllipse(x, y, width, height, true);
  }

  @Override
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    MyInfo.addArc(x, y, width, height, startAngle, arcAngle, false);
  }

  @Override
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    MyInfo.addArc(x, y, width, height, startAngle, arcAngle, true);
  }

  @Override
  public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    MyInfo.addPolyline(xPoints, yPoints, nPoints, false, false);
  }

  @Override
  public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    MyInfo.addPolyline(xPoints, yPoints, nPoints, false, true);
  }

  @Override
  public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    MyInfo.addPolyline(xPoints, yPoints, nPoints, true, true);
  }

  @Override
  public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int x, int y, ImageObserver observer)");
    return false;
  }

  @Override
  public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int x, int y, int width, int height, ImageObserver observer)");
    return false;
  }

  @Override
  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer)");
    return false;
  }

  @Override
  public boolean drawImage(
      Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer)");
    return false;
  }

  @Override
  public boolean drawImage(
      Image img,
      int dx1,
      int dy1,
      int dx2,
      int dy2,
      int sx1,
      int sy1,
      int sx2,
      int sy2,
      ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,\n"
            + "      ImageObserver observer)");
    return false;
  }

  @Override
  public boolean drawImage(
      Image img,
      int dx1,
      int dy1,
      int dx2,
      int dy2,
      int sx1,
      int sy1,
      int sx2,
      int sy2,
      Color bgcolor,
      ImageObserver observer) {
    System.out.println(
        "TikZ not yet supported : drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,\n"
            + "      Color bgcolor, ImageObserver observer)");
    return false;
  }

  @Override
  public void dispose() {}

  public void writeFile(File outfile) throws IOException {
    MyInfo.writeFile(outfile);
  }

  public void writeSvg(int width, int height, File outfile)
      throws ParserConfigurationException, TransformerException {
    MyInfo.writeSvg(width, height, outfile);
  }
}
