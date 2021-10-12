/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.analyze.data.ExpressionRenderData;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JPanel;

class ExpressionView extends JPanel {

  private class MyListener extends ComponentAdapter {

    @Override
    public void componentResized(ComponentEvent arg0) {
      final var width = getWidth();
      if (renderData != null && Math.abs(renderData.getParentWidth() - width) > 2) {
        renderData = new ExpressionRenderData(expr, width, notation);
        setPreferredSize(renderData.getPreferredSize());
        revalidate();
        repaint();
      }
    }

  }

  public static class NamedExpression {
    public final String name;
    public Expression expr; // can be null
    public String exprString;
    public String err;

    NamedExpression(String n) {
      name = n;
    }

    NamedExpression(String n, Expression e, String s) {
      name = n;
      expr = e;
      exprString = s;
    }
  }


  private static final long serialVersionUID = 1L;

  private final MyListener myListener = new MyListener();
  private Notation notation = Notation.MATHEMATICAL;
  private ExpressionRenderData renderData;
  private Expression expr;
  private int width;
  private boolean selected;

  boolean isSelected() {
    return selected;
  }

  public ExpressionView() {
    addComponentListener(myListener);
    setExpression((Expression) null);
    width = -1;
    final var f =
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            if (e.isTemporary()) return;
            selected = true;
          }

          @Override
          public void focusLost(FocusEvent e) {
            if (e.isTemporary()) return;
            selected = false;
          }
        };
    addFocusListener(f);
  }

  public void setWidth(int w) {
    removeComponentListener(myListener);
    width = w;
  }

  public ExpressionRenderData getRenderData() {
    return renderData;
  }

  public void setNotation(Notation notation) {
    if (this.notation == notation || expr == null) return;
    this.notation = notation;
    setExpression(expr);
  }

  @Override
  public void paintComponent(Graphics g) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    super.paintComponent(g);
    if (renderData != null) {
      final var bds = renderData.getPreferredSize();
      int x = Math.max(0, (width < 0) ? (getWidth() - (int) bds.getWidth()) / 2 : 0);
      int y = Math.max(0, (width < 0) ? (getHeight() - (int) bds.getHeight()) / 2 : 0);
      renderData.paint(g, x, y);
    }
  }

  public int getExpressionHeight() {
    final var defaultHeight = 25;
    if (renderData == null) return defaultHeight;
    return (int) renderData.getPreferredSize().getHeight();
  }

  public void setExpression(NamedExpression e) {
    if (e.expr != null) {
      setExpression(e.name, e.expr);
    } else {
      setError(e.name, e.err != null ? e.err : "unspecified");
    }
  }

  public void setExpression(Expression expr) {
    this.expr = expr;
    renderData = new ExpressionRenderData(expr, (width < 0) ? getWidth() : width, notation);
    setPreferredSize(renderData.getPreferredSize());
    revalidate();
    repaint();
  }

  public void setExpression(String name, Expression expr) {
    setExpression(Expressions.eq(Expressions.variable(name), expr));
  }

  public void setError(String name, String msg) {
    setExpression(Expressions.eq(Expressions.variable(name), Expressions.variable(msg)));
  }
}
