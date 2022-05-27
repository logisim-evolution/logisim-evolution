/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.TextIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Text extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Text";

  public static final Attribute<String> ATTR_TEXT =
      Attributes.forString("text", S.getter("textTextAttr"));
  public static final Attribute<Font> ATTR_FONT =
      Attributes.forFont("font", S.getter("textFontAttr"));
  public static final Attribute<Color> ATTR_COLOR =
      Attributes.forColor("color", S.getter("textColorAttr"));
  public static final Attribute<AttributeOption> ATTR_HALIGN =
      Attributes.forOption(
          "halign",
          S.getter("textHorzAlignAttr"),
          new AttributeOption[] {
            new AttributeOption(TextField.H_LEFT, "left", S.getter("textHorzAlignLeftOpt")),
            new AttributeOption(TextField.H_RIGHT, "right", S.getter("textHorzAlignRightOpt")),
            new AttributeOption(TextField.H_CENTER, "center", S.getter("textHorzAlignCenterOpt")),
          });
  public static final Attribute<AttributeOption> ATTR_VALIGN =
      Attributes.forOption(
          "valign",
          S.getter("textVertAlignAttr"),
          new AttributeOption[] {
            new AttributeOption(TextField.V_TOP, "top", S.getter("textVertAlignTopOpt")),
            new AttributeOption(TextField.V_BASELINE, "base", S.getter("textVertAlignBaseOpt")),
            new AttributeOption(TextField.V_BOTTOM, "bottom", S.getter("textVertAlignBottomOpt")),
            new AttributeOption(TextField.H_CENTER, "center", S.getter("textVertAlignCenterOpt")),
          });

  public static final Text FACTORY = new Text();

  private Text() {
    super(_ID, S.getter("textComponent"));
    setShouldSnap(false);
  }

  private void configureLabel(Instance instance) {
    TextAttributes attrs = (TextAttributes) instance.getAttributeSet();
    Location loc = instance.getLocation();
    instance.setTextField(
        ATTR_TEXT,
        ATTR_FONT,
        loc.getX(),
        loc.getY(),
        attrs.getHorizontalAlign(),
        attrs.getVerticalAlign());
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configureLabel(instance);
    instance.addAttributeListener();
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new TextAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    TextAttributes attrs = (TextAttributes) attrsBase;
    String text = attrs.getText();
    if (text == null || text.equals("")) {
      return Bounds.EMPTY_BOUNDS;
    } else {
      Bounds bds = attrs.getOffsetBounds();
      if (bds == null) {
        bds =
            StringUtil.estimateBounds(
                attrs.getText(),
                attrs.getFont(),
                attrs.getHorizontalAlign(),
                attrs.getVerticalAlign());
        attrs.setOffsetBounds(bds);
      }
      return bds == null ? Bounds.EMPTY_BOUNDS : bds;
    }
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return true;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_HALIGN || attr == ATTR_VALIGN) {
      configureLabel(instance);
    }
  }

  //
  // graphics methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    TextAttributes attrs = (TextAttributes) painter.getAttributeSet();
    String text = attrs.getText();
    if (text == null || text.equals("")) return;

    int halign = attrs.getHorizontalAlign();
    int valign = attrs.getVerticalAlign();
    Graphics g = painter.getGraphics();
    Font old = g.getFont();
    g.setFont(attrs.getFont());
    GraphicsUtil.drawText(g, text, 0, 0, halign, valign);

    String textTrim = text.endsWith(" ") ? text.substring(0, text.length() - 1) : text;
    Bounds newBds;
    if (textTrim.equals("")) {
      newBds = Bounds.EMPTY_BOUNDS;
    } else {
      Rectangle bdsOut = GraphicsUtil.getTextBounds(g, textTrim, 0, 0, halign, valign);
      newBds = Bounds.create(bdsOut).expand(4);
    }
    if (attrs.setOffsetBounds(newBds)) {
      Instance instance = painter.getInstance();
      if (instance != null) instance.recomputeBounds();
    }

    g.setFont(old);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    final var gfx = painter.getGraphics();
    gfx.translate(x, y);
    gfx.setColor(painter.getAttributeValue(ATTR_COLOR));
    paintGhost(painter);
    gfx.translate(-x, -y);
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics().create();
    TextIcon t = new TextIcon();
    t.paintIcon(null, g2, 0, 0);
    g2.dispose();
  }

  @Override
  public void propagate(InstanceState state) {}
}
