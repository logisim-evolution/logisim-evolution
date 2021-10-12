/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class BoardPainter {

  public static void errorBoardPainter(BoardManipulator bm, Graphics2D g) {
    g.setColor(Color.gray);
    g.fillRect(0, 0, bm.getWidth(), bm.getHeight());
    Graphics g1 = g.create();
    Font curfont =
        AppPreferences.getScaledFont(new Font(g1.getFont().getFontName(), Font.BOLD, 20));
    g1.setFont(curfont);
    g1.setColor(Color.red);
    GraphicsUtil.drawCenteredText(
        g1, S.get("BoardPainterError"), bm.getWidth() / 2, bm.getHeight() / 2);
  }

  public static void newBoardpainter(BoardManipulator bm, Graphics2D g) {
    g.setColor(Color.gray);
    g.fillRect(0, 0, bm.getWidth(), bm.getHeight());
    String message;
    int xpos;
    Font curfont = AppPreferences.getScaledFont(new Font(g.getFont().getFontName(), Font.BOLD, 20));
    g.setColor(Color.black);
    g.setFont(curfont);
    FontMetrics fm = g.getFontMetrics();
    message = S.get("BoardPainterMsg1");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, 100);
    message = S.get("BoardPainterMsg2");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, 200);
    message =
        S.get("BoardPainterMsg3", BoardManipulator.IMAGE_WIDTH, BoardManipulator.IMAGE_HEIGHT);
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (1.5 * fm.getAscent())));
    message = S.get("BoardPainterMsg4");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (3 * fm.getAscent())));
    message = S.get("BoardPainterMsg5");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (4.5 * fm.getAscent())));
    message = S.get("BoardPainterMsg6", bm.getWidth(), bm.getHeight());
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (6 * fm.getAscent())));
  }

  public static void paintConstantOpenBar(Graphics g, float scale) {
    Graphics2D g2 = (Graphics2D) g.create();
    int yoffset = AppPreferences.getScaled(BoardManipulator.IMAGE_HEIGHT + 2, scale);
    int skip = AppPreferences.getScaled(BoardManipulator.CONSTANT_BUTTON_WIDTH, scale);
    int xoffset = AppPreferences.getScaled(1, scale);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
    for (int i = 0; i < 3; i++)
      paintConstantButton(g2, xoffset + i * skip, yoffset, i == 2, i, scale);
    paintOpenButton(g2, xoffset + 3 * skip, yoffset, scale);
    g2.dispose();
  }

  private static void paintConstantButton(
      Graphics2D g, int xpos, int ypos, boolean constant, int value, float scale) {
    int width = AppPreferences.getScaled(BoardManipulator.CONSTANT_BUTTON_WIDTH - 2, scale);
    int height = AppPreferences.getScaled(BoardManipulator.CONSTANT_BAR_HEIGHT - 2, scale);
    int ydif2 = height - (height >> 2);
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
    g.drawRect(xpos, ypos, width, height);
    String val = constant ? S.get("BoardMapValue") : Integer.toString(value);
    String txt = S.get("BoardMapConstant", val);
    g.setFont(AppPreferences.getScaledFont(g.getFont().deriveFont(Font.BOLD), scale));
    g.setColor(Color.BLUE);
    g.drawString(txt, xpos + height + (height >> 2), ypos + ydif2);
    g.setColor(
            value == 0 ? Value.falseColor : value == 1 ? Value.trueColor : Value.unknownColor);
    g.fillOval(
        xpos + (height >> 3), ypos + (height >> 3), height - (height >> 2), height - (height >> 2));
    g.setColor(Color.WHITE);
    if (!constant)
      GraphicsUtil.drawCenteredText(
          g, Integer.toString(value), xpos + (height >> 1), ypos + (height >> 1));
    else GraphicsUtil.drawCenteredText(g, "C", xpos + (height >> 1), ypos + (height >> 1));
  }

  private static void paintOpenButton(Graphics2D g, int xpos, int ypos, float scale) {
    int width = AppPreferences.getScaled(BoardManipulator.CONSTANT_BUTTON_WIDTH - 2, scale);
    int height = AppPreferences.getScaled(BoardManipulator.CONSTANT_BAR_HEIGHT - 2, scale);
    int ydif2 = height - (height >> 2);
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
    g.drawRect(xpos, ypos, width, height);
    g.setFont(AppPreferences.getScaledFont(g.getFont().deriveFont(Font.BOLD), scale));
    g.setColor(Color.BLUE);
    g.drawString(S.get("BoardMapOpen"), xpos + height + (height >> 2), ypos + ydif2);
    g.setColor(Color.RED);
    g.setStroke(new BasicStroke(AppPreferences.getScaled(3, scale)));
    g.drawLine(
        xpos + (height >> 2),
        ypos + (height >> 2),
        xpos + height - (height >> 2),
        ypos + height - (height >> 2));
    g.drawLine(
        xpos + height - (height >> 2),
        ypos + (height >> 2),
        xpos + (height >> 2),
        ypos + height - (height >> 2));
  }


}
