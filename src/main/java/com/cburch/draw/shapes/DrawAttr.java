/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public class DrawAttr {
  public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);
  public static final Font DEFAULT_FIXED_PICH_FONT = new Font("Courier 10 Pitch", Font.PLAIN, 12);
  public static final Font DEFAULT_NAME_FONT = new Font("Courier 10 Pitch", Font.BOLD, 14);
  public static final int FIXED_FONT_HEIGHT = 12;
  public static final int FIXED_FONT_CHAR_WIDTH = 8;
  public static final int FIXED_FONT_ASCENT = 9;
  public static final int FIXED_FONT_DESCENT = 1;
  public static final AttributeOption HALIGN_LEFT =
      new AttributeOption(EditableLabel.LEFT, S.getter("alignLeft"));
  public static final AttributeOption HALIGN_CENTER =
      new AttributeOption(EditableLabel.CENTER, S.getter("alignCenter"));
  public static final AttributeOption HALIGN_RIGHT =
      new AttributeOption(EditableLabel.RIGHT, S.getter("alignRight"));
  public static final AttributeOption VALIGN_TOP =
      new AttributeOption(EditableLabel.TOP, S.getter("alignTop"));
  public static final AttributeOption VALIGN_MIDDLE =
      new AttributeOption(EditableLabel.MIDDLE, S.getter("alignMiddle"));
  public static final AttributeOption VALIGN_BASELINE =
      new AttributeOption(EditableLabel.BASELINE, S.getter("alignBaseline"));
  public static final AttributeOption VALIGN_BOTTOM =
      new AttributeOption(EditableLabel.BOTTOM, S.getter("alignBottom"));
  public static final AttributeOption PAINT_STROKE =
      new AttributeOption("stroke", S.getter("paintStroke"));
  public static final AttributeOption PAINT_FILL =
      new AttributeOption("fill", S.getter("paintFill"));
  public static final AttributeOption PAINT_STROKE_FILL =
      new AttributeOption("both", S.getter("paintBoth"));
  public static final Attribute<Font> FONT = Attributes.forFont("font", S.getter("attrFont"));
  public static final Attribute<AttributeOption> HALIGNMENT =
      Attributes.forOption(
          "halign",
          S.getter("attrHAlign"),
          new AttributeOption[] {HALIGN_LEFT, HALIGN_CENTER, HALIGN_RIGHT});
  public static final Attribute<AttributeOption> VALIGNMENT =
      Attributes.forOption(
          "valign",
          S.getter("attrVAlign"),
          new AttributeOption[] {VALIGN_TOP, VALIGN_MIDDLE, VALIGN_BASELINE, VALIGN_BOTTOM});
  public static final Attribute<AttributeOption> PAINT_TYPE =
      Attributes.forOption(
          "paintType",
          S.getter("attrPaint"),
          new AttributeOption[] {PAINT_STROKE, PAINT_FILL, PAINT_STROKE_FILL});
  public static final Attribute<Integer> STROKE_WIDTH =
      Attributes.forIntegerRange("stroke-width", S.getter("attrStrokeWidth"), 1, 8);
  public static final Attribute<Color> STROKE_COLOR =
      Attributes.forColor("stroke", S.getter("attrStroke"));
  public static final Attribute<Color> FILL_COLOR =
      Attributes.forColor("fill", S.getter("attrFill"));
  public static final Attribute<Color> TEXT_DEFAULT_FILL =
      Attributes.forColor("fill", S.getter("attrFill"));
  public static final Attribute<Integer> CORNER_RADIUS =
      Attributes.forIntegerRange("rx", S.getter("attrRx"), 1, 1000);
  public static final List<Attribute<?>> ATTRS_TEXT // for text
      = createAttributes(new Attribute[] {FONT, HALIGNMENT, VALIGNMENT, FILL_COLOR});
  public static final List<Attribute<?>> ATTRS_TEXT_TOOL // for text tool
      = createAttributes(new Attribute[] {FONT, HALIGNMENT, VALIGNMENT, TEXT_DEFAULT_FILL});
  public static final List<Attribute<?>> ATTRS_STROKE // for line, polyline
      = createAttributes(new Attribute[] {STROKE_WIDTH, STROKE_COLOR});
  // attribute lists for rectangle, oval, polygon
  private static final List<Attribute<?>> ATTRS_FILL_STROKE =
      createAttributes(new Attribute[] {PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR});
  private static final List<Attribute<?>> ATTRS_FILL_FILL =
      createAttributes(new Attribute[] {PAINT_TYPE, FILL_COLOR});
  private static final List<Attribute<?>> ATTRS_FILL_BOTH =
      createAttributes(new Attribute[] {PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, FILL_COLOR});
  // attribute lists for rounded rectangle
  private static final List<Attribute<?>> ATTRS_RRECT_STROKE =
      createAttributes(new Attribute[] {PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, CORNER_RADIUS});
  private static final List<Attribute<?>> ATTRS_RRECT_FILL =
      createAttributes(new Attribute[] {PAINT_TYPE, FILL_COLOR, CORNER_RADIUS});
  private static final List<Attribute<?>> ATTRS_RRECT_BOTH =
      createAttributes(new Attribute[] {PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, FILL_COLOR, CORNER_RADIUS});

  private DrawAttr() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  private static List<Attribute<?>> createAttributes(Attribute<?>[] values) {
    return UnmodifiableList.create(values);
  }

  public static List<Attribute<?>> getFillAttributes(AttributeOption paint) {
    if (paint.equals(PAINT_STROKE)) {
      return ATTRS_FILL_STROKE;
    } else if (paint.equals(PAINT_FILL)) {
      return ATTRS_FILL_FILL;
    } else {
      return ATTRS_FILL_BOTH;
    }
  }

  public static List<Attribute<?>> getRoundRectAttributes(AttributeOption paint) {
    if (paint.equals(PAINT_STROKE)) {
      return ATTRS_RRECT_STROKE;
    } else if (paint.equals(PAINT_FILL)) {
      return ATTRS_RRECT_FILL;
    } else {
      return ATTRS_RRECT_BOTH;
    }
  }
}
