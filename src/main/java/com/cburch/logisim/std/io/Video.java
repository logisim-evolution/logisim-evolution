/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

/*
 * This file was originally written by Kevin Walsh <kwalsh@cs.cornell.edu> for
 * Cornell's CS 314 computer organization course. It was subsequently modified
 * Martin Dybdal <dybber@dybber.dk> and Anders Boesen Lindbo Larsen
 * <abll@diku.dk> for use in the computer architecture class at the Department
 * of Computer Science, University of Copenhagen.
 */
package com.cburch.logisim.std.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import static com.cburch.logisim.std.Strings.S;
import com.cburch.logisim.tools.ToolTipMaker;

// 128 x 128 pixel LCD display with 8bpp color (byte addressed)
class Video extends ManagedComponent implements ToolTipMaker, AttributeListener {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "RGB Video";

  public static final ComponentFactory factory = new Factory();

  static final String BLINK_YES = "Blinking Dot";
  static final String BLINK_NO = "No Cursor";
  static final String[] BLINK_OPTIONS = {BLINK_YES, BLINK_NO};
  static final String RESET_ASYNC = "Asynchronous";
  static final String RESET_SYNC = "Synchronous";
  static final String[] RESET_OPTIONS = {RESET_ASYNC, RESET_SYNC};

  static final String COLOR_RGB = "888 RGB (24 bit)";
  static final String COLOR_555_RGB = "555 RGB (15 bit)";
  static final String COLOR_565_RGB = "565 RGB (16 bit)";
  static final String COLOR_111_RGB = "8-Color RGB (3 bit)";
  static final String COLOR_ATARI = "Atari 2600 (7 bit)";
  static final String COLOR_XTERM16 = "XTerm16 (4 bit)";
  static final String COLOR_XTERM256 = "XTerm256 (8 bit)";
  static final String COLOR_GRAY4 = "Grayscale (4 bit)";
  static final String COLOR_VGA256 = "VGA256 (8 bit)";

  static final String[] COLOR_OPTIONS = {
    COLOR_RGB,
    COLOR_555_RGB,
    COLOR_565_RGB,
    COLOR_111_RGB,
    COLOR_ATARI,
    COLOR_XTERM16,
    COLOR_XTERM256,
    COLOR_GRAY4,
    COLOR_VGA256
  };

  static final Integer[] SIZE_OPTIONS = {2, 4, 8, 16, 32, 64, 128, 256};

  public static final Attribute<String> BLINK_OPTION =
      Attributes.forOption("cursor", S.getter("rgbVideoCursor"), BLINK_OPTIONS);
  public static final Attribute<String> RESET_OPTION =
      Attributes.forOption("reset", S.getter("rgbVideoReset"), RESET_OPTIONS);
  public static final Attribute<String> COLOR_OPTION =
      Attributes.forOption("color", S.getter("rgbVideoColor"), COLOR_OPTIONS);
  public static final Attribute<Integer> WIDTH_OPTION =
      Attributes.forOption("width", S.getter("rgbVideoWidth"), SIZE_OPTIONS);
  public static final Attribute<Integer> HEIGHT_OPTION =
      Attributes.forOption("height", S.getter("rgbVideoHeight"), SIZE_OPTIONS);
  public static final Attribute<Integer> SCALE_OPTION =
      Attributes.forIntegerRange("scale", S.getter("rgbVideoScale"), 1, 8);

  private static final Attribute<?>[] ATTRIBUTES = {
    BLINK_OPTION, RESET_OPTION, COLOR_OPTION, WIDTH_OPTION, HEIGHT_OPTION, SCALE_OPTION
  };

  private static class Factory extends AbstractComponentFactory {
    private Factory() {}

    @Override
    public String getName() {
      return _ID;
    }

    @Override
    public String getDisplayName() {
      return S.get("rgbVideoComponent");
    }

    @Override
    public AttributeSet createAttributeSet() {
      return AttributeSets.fixedSet(
          ATTRIBUTES,
          new Object[] {BLINK_OPTIONS[0], RESET_OPTIONS[0], COLOR_OPTIONS[0], 128, 128, 2});
    }

    @Override
    public Component createComponent(Location loc, AttributeSet attrs) {
      return new Video(loc, attrs);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
      final var s = attrs.getValue(SCALE_OPTION);
      final var w = attrs.getValue(WIDTH_OPTION);
      final var h = attrs.getValue(HEIGHT_OPTION);
      final var bw = (Math.max(s * w + 14, 100));
      final var bh = (Math.max(s * h + 14, 20));
      return Bounds.create(-30, -bh, bw, bh);
    }

    @Override
    public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
      drawVideoIcon(context, x, y);
    }
  }

  static final int P_RST = 0;
  static final int P_CLK = 1;
  static final int P_WE = 2;
  static final int P_X = 3;
  static final int P_Y = 4;
  static final int P_DATA = 5;

  private Video(Location loc, AttributeSet attrs) {
    super(loc, attrs, 6);
    setEnd(P_RST, getLocation().translate(0, 0), BitWidth.ONE, EndData.INPUT_ONLY);
    setEnd(P_CLK, getLocation().translate(10, 0), BitWidth.ONE, EndData.INPUT_ONLY);
    setEnd(P_WE, getLocation().translate(20, 0), BitWidth.ONE, EndData.INPUT_ONLY);
    configureComponent();
    attrs.addAttributeListener(this);
  }

  @Override
  public ComponentFactory getFactory() {
    return factory;
  }

  @Override
  public void setFactory(ComponentFactory fact) {}

  Location loc(int pin) {
    return getEndLocation(pin);
  }

  Value val(CircuitState s, int pin) {
    return s.getValue(loc(pin));
  }

  int addr(CircuitState s, int pin) {
    return (int) val(s, pin).toLongValue();
  }

  @Override
  public void propagate(CircuitState circuitState) {
    final var state = getState(circuitState);
    final var attrs = getAttributeSet();
    final var x = addr(circuitState, P_X);
    final var y = addr(circuitState, P_Y);
    final var color = addr(circuitState, P_DATA);
    state.lastX = x;
    state.lastY = y;
    state.color = color;

    Object resetOption = attrs.getValue(RESET_OPTION);
    if (resetOption == null) resetOption = RESET_OPTIONS[0];
    final var cm = getColorModel(attrs.getValue(COLOR_OPTION));
    final var w = attrs.getValue(WIDTH_OPTION);
    final var h = attrs.getValue(HEIGHT_OPTION);

    if (state.tick(val(circuitState, P_CLK)) && val(circuitState, P_WE) == Value.TRUE) {
      final var g = state.img.getGraphics();
      g.setColor(new Color(cm.getRGB(color)));
      g.fillRect(x, y, 1, 1);
      if (RESET_SYNC.equals(resetOption) && val(circuitState, P_RST) == Value.TRUE) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
      }
    }

    if (!RESET_SYNC.equals(resetOption) && val(circuitState, P_RST) == Value.TRUE) {
      final var g = state.img.getGraphics();
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, w, h);
    }
  }

  @Override
  public void draw(ComponentDrawContext context) {
    final var loc = getLocation();
    final var s = getState(context.getCircuitState());
    drawVideo(context, loc.getX(), loc.getY(), s);
  }

  static void drawVideoIcon(ComponentDrawContext context, int x, int y) {
    final var g = context.getGraphics().create();
    g.translate(x, y);
    g.setColor(Color.WHITE);
    g.fillRoundRect(scale(2), scale(2), scale(16 - 1), scale(16 - 1), scale(3), scale(3));
    g.setColor(Color.BLACK);
    g.drawRoundRect(scale(2), scale(2), scale(16 - 1), scale(16 - 1), scale(3), scale(3));
    int five = scale(5);
    int ten = scale(10);
    g.setColor(Color.RED);
    g.fillRect(five, five, five, five);
    g.setColor(Color.BLUE);
    g.fillRect(ten, five, five, five);
    g.setColor(Color.GREEN);
    g.fillRect(five, ten, five, five);
    g.setColor(Color.MAGENTA);
    g.fillRect(ten, ten, five, five);
    g.dispose();
  }

  private static int scale(int v) {
    return AppPreferences.getScaled(v);
  }

  boolean blink() {
    long now = System.currentTimeMillis();
    return (now / 1000 % 2 == 0);
  }

  static final DirectColorModel rgb111 = new DirectColorModel(3, 0x4, 0x2, 0x1);
  static final DirectColorModel rgb555 = new DirectColorModel(15, 0x7C00, 0x03E0, 0x001F);
  static final DirectColorModel rgb565 = new DirectColorModel(16, 0xF800, 0x07E0, 0x001F);
  static final DirectColorModel rgb = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
  static final IndexColorModel gray4 =
      new IndexColorModel(
          4,
          16,
          new int[] {
            0x000000, 0x111111, 0x222222, 0x333333, 0x444444, 0x555555, 0x666666, 0x777777,
            0x888888, 0x999999, 0xaaaaaa, 0xbbbbbb, 0xcccccc, 0xdddddd, 0xeeeeee, 0xffffff,
          },
          0,
          0,
          null);
  static final IndexColorModel atari =
      new IndexColorModel(
          7,
          128,
          new int[] {
            0x000000, 0x0a0a0a, 0x373737, 0x5f5f5f, 0x7a7a7a, 0xa1a1a1, 0xc5c5c5, 0xededed,
            0x000000, 0x352100, 0x5a4500, 0x816c00, 0x9c8700, 0xc3af01, 0xe8d326, 0xfffa4d,
            0x310000, 0x590700, 0x7d2b00, 0xa45200, 0xbf6d04, 0xe7952b, 0xffb950, 0xffe077,
            0x470000, 0x6e0000, 0x931302, 0xba3b2a, 0xd55545, 0xfc7d6c, 0xffa190, 0xffc9b8,
            0x4b0002, 0x720029, 0x96034e, 0xbe2a75, 0xd94590, 0xff6cb7, 0xff91dc, 0xffb8ff,
            0x3c0049, 0x640070, 0x880094, 0xaf24bc, 0xca3fd7, 0xf266fe, 0xff8aff, 0xffb2ff,
            0x1e007d, 0x4500a5, 0x6902c9, 0x9129f1, 0xac44ff, 0xd36bff, 0xf790ff, 0xffb7ff,
            0x000096, 0x1d00bd, 0x4111e1, 0x6939ff, 0x8453ff, 0xab7bff, 0xcf9fff, 0xf7c7ff,
            0x00008d, 0x0004b4, 0x1728d9, 0x3f50ff, 0x5a6bff, 0x8192ff, 0xa5b6ff, 0xcddeff,
            0x000065, 0x001e8c, 0x0042b0, 0x1b6ad8, 0x3685f3, 0x5dacff, 0x82d0ff, 0xa9f8ff,
            0x000f25, 0x00364c, 0x005a70, 0x048298, 0x1f9db3, 0x47c4da, 0x6be8fe, 0x92ffff,
            0x002000, 0x004701, 0x006b25, 0x00934d, 0x1aae68, 0x42d58f, 0x66f9b4, 0x8dffdb,
            0x002700, 0x004e00, 0x007200, 0x0d9a06, 0x28b520, 0x4fdc48, 0x74ff6c, 0x9bff94,
            0x002200, 0x004a00, 0x036e00, 0x2b9500, 0x45b000, 0x6dd812, 0x91fc36, 0xb9ff5d,
            0x000a00, 0x073a00, 0x2b5f00, 0x528600, 0x6da100, 0x95c800, 0xb9ed1c, 0xe0ff43,
            0x000000, 0x352100, 0x5a4500, 0x816c00, 0x9c8700, 0xc3af01, 0xe8d326, 0xfffa4d,
          },
          0,
          0,
          null);
  static final IndexColorModel xterm16 =
      new IndexColorModel(
          4,
          16,
          new int[] {
            0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
            0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff,
          },
          0,
          0,
          null);
  static final IndexColorModel xterm256 =
      new IndexColorModel(
          8,
          256,
          new int[] {
            0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
            0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff,
            0x000000, 0x00005f, 0x000087, 0x0000af, 0x0000d7, 0x0000ff, 0x005f00, 0x005f5f,
            0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f, 0x008787, 0x0087af,
            0x0087d7, 0x0087ff, 0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff,
            0x00d700, 0x00d75f, 0x00d787, 0x00d7af, 0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f,
            0x00ff87, 0x00ffaf, 0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f, 0x5f0087, 0x5f00af,
            0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f, 0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff,
            0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff, 0x5faf00, 0x5faf5f,
            0x5faf87, 0x5fafaf, 0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af,
            0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f, 0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff,
            0x870000, 0x87005f, 0x870087, 0x8700af, 0x8700d7, 0x8700ff, 0x875f00, 0x875f5f,
            0x875f87, 0x875faf, 0x875fd7, 0x875fff, 0x878700, 0x87875f, 0x878787, 0x8787af,
            0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf, 0x87afd7, 0x87afff,
            0x87d700, 0x87d75f, 0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f,
            0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff, 0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af,
            0xaf00d7, 0xaf00ff, 0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf, 0xaf5fd7, 0xaf5fff,
            0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af, 0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f,
            0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f, 0xafd787, 0xafd7af,
            0xafd7d7, 0xafd7ff, 0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff,
            0xd70000, 0xd7005f, 0xd70087, 0xd700af, 0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f,
            0xd75f87, 0xd75faf, 0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f, 0xd78787, 0xd787af,
            0xd787d7, 0xd787ff, 0xdfaf00, 0xdfaf5f, 0xdfaf87, 0xdfafaf, 0xdfafdf, 0xdfafff,
            0xdfdf00, 0xdfdf5f, 0xdfdf87, 0xdfdfaf, 0xdfdfdf, 0xdfdfff, 0xdfff00, 0xdfff5f,
            0xdfff87, 0xdfffaf, 0xdfffdf, 0xdfffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
            0xff00df, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fdf, 0xff5fff,
            0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87df, 0xff87ff, 0xffaf00, 0xffaf5f,
            0xffaf87, 0xffafaf, 0xffafdf, 0xffafff, 0xffdf00, 0xffdf5f, 0xffdf87, 0xffdfaf,
            0xffdfdf, 0xffdfff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffdf, 0xffffff,
            0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
            0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e,
            0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee,
          },
          0,
          0,
          null);
   static final IndexColorModel vga256 = 
          new IndexColorModel(
          8,
          256,
          new int[] {
            0x000000, 0x0000aa, 0x00aa00, 0x00aaaa, 0xaa0000, 0xaa00aa, 0xaa5500, 0xaaaaaa, 
            0x555555, 0x5555ff, 0x55ff55, 0x55ffff, 0xff5555, 0xff55ff, 0xffff55, 0xffffff, 
            0x000000, 0x141414, 0x202020, 0x2c2c2c, 0x383838, 0x454545, 0x515151, 0x616161, 
            0x717171, 0x828282, 0x929292, 0xa2a2a2, 0xb6b6b6, 0xcbcbcb, 0xe3e3e3, 0xffffff, 
            0x0000ff, 0x4100ff, 0x7d00ff, 0xbe00ff, 0xff00ff, 0xff00be, 0xff007d, 0xff0041, 
            0xff0000, 0xff4100, 0xff7d00, 0xffbe00, 0xffff00, 0xbeff00, 0x7dff00, 0x41ff00, 
            0x00ff00, 0x00ff41, 0x00ff7d, 0x00ffbe, 0x00ffff, 0x00beff, 0x007dff, 0x0041ff, 
            0x7d7dff, 0x9e7dff, 0xbe7dff, 0xdf7dff, 0xff7dff, 0xff7ddf, 0xff7dbe, 0xff7d9e, 
            0xff7d7d, 0xff9e7d, 0xffbe7d, 0xffdf7d, 0xffff7d, 0xdfff7d, 0xbeff7d, 0x9eff7d, 
            0x7dff7d, 0x7dff9e, 0x7dffbe, 0x7dffdf, 0x7dffff, 0x7ddfff, 0x7dbeff, 0x7d9eff, 
            0xb6b6ff, 0xc7b6ff, 0xdbb6ff, 0xebb6ff, 0xffb6ff, 0xffb6eb, 0xffb6db, 0xffb6c7, 
            0xffb6b6, 0xffc7b6, 0xffdbb6, 0xffebb6, 0xffffb6, 0xebffb6, 0xdbffb6, 0xc7ffb6, 
            0xb6ffb6, 0xb6ffc7, 0xb6ffdb, 0xb6ffeb, 0xb6ffff, 0xb6ebff, 0xb6dbff, 0xb6c7ff, 
            0x000071, 0x1c0071, 0x380071, 0x550071, 0x710071, 0x710055, 0x710038, 0x71001c, 
            0x710000, 0x711c00, 0x713800, 0x715500, 0x717100, 0x557100, 0x387100, 0x1c7100, 
            0x007100, 0x00711c, 0x007138, 0x007155, 0x007171, 0x005571, 0x003871, 0x001c71, 
            0x383871, 0x453871, 0x553871, 0x613871, 0x713871, 0x713861, 0x713855, 0x713845, 
            0x713838, 0x714538, 0x715538, 0x716138, 0x717138, 0x617138, 0x557138, 0x457138, 
            0x387138, 0x387145, 0x387155, 0x387161, 0x387171, 0x386171, 0x385571, 0x384571, 
            0x515171, 0x595171, 0x615171, 0x695171, 0x715171, 0x715169, 0x715161, 0x715159, 
            0x715151, 0x715951, 0x716151, 0x716951, 0x717151, 0x697151, 0x617151, 0x597151, 
            0x517151, 0x517159, 0x517161, 0x517169, 0x517171, 0x516971, 0x516171, 0x515971, 
            0x000041, 0x100041, 0x200041, 0x300041, 0x410041, 0x410030, 0x410020, 0x410010, 
            0x410000, 0x411000, 0x412000, 0x413000, 0x414100, 0x304100, 0x204100, 0x104100, 
            0x004100, 0x004110, 0x004120, 0x004130, 0x004141, 0x003041, 0x002041, 0x001041, 
            0x202041, 0x282041, 0x302041, 0x382041, 0x412041, 0x412038, 0x412030, 0x412028, 
            0x412020, 0x412820, 0x413020, 0x413820, 0x414120, 0x384120, 0x304120, 0x284120, 
            0x204120, 0x204128, 0x204130, 0x204138, 0x204141, 0x203841, 0x203041, 0x202841, 
            0x2c2c41, 0x302c41, 0x342c41, 0x3c2c41, 0x412c41, 0x412c3c, 0x412c34, 0x412c30, 
            0x412c2c, 0x41302c, 0x41342c, 0x413c2c, 0x41412c, 0x3c412c, 0x34412c, 0x30412c, 
            0x2c412c, 0x2c4130, 0x2c4134, 0x2c413c, 0x2c4141, 0x2c3c41, 0x2c3441, 0x2c3041, 
            0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000
          },
          0,
          0,
          null);

  static ColorModel getColorModel(Object model) {
    if (model == COLOR_RGB) return rgb;
    else if (model == COLOR_555_RGB) return rgb555;
    else if (model == COLOR_565_RGB) return rgb565;
    else if (model == COLOR_111_RGB) return rgb111;
    else if (model == COLOR_ATARI) return atari;
    else if (model == COLOR_XTERM16) return xterm16;
    else if (model == COLOR_XTERM256) return xterm256;
    else if (model == COLOR_GRAY4) return gray4;
    else if (model == COLOR_VGA256) return vga256;    
    else return rgb555;
  }

  void drawVideo(ComponentDrawContext context, int x, int y, State state) {
    final var g = context.getGraphics();

    final var attrs = getAttributeSet();
    Object blinkOption = attrs.getValue(BLINK_OPTION);
    final var cm = getColorModel(attrs.getValue(COLOR_OPTION));

    final var s = attrs.getValue(SCALE_OPTION);
    final var w = attrs.getValue(WIDTH_OPTION);
    final var h = attrs.getValue(HEIGHT_OPTION);
    final var bw = (Math.max(s * w + 14, 100));
    final var bh = (Math.max(s * h + 14, 20));

    x += (-30);
    y += (-bh);

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    g.drawRoundRect(x, y, bw, bh, 6, 6);
    for (var i = 0; i < 6; i++) {
      if (i != P_CLK) context.drawPin(this, i);
    }
    context.drawClock(this, P_CLK, Direction.NORTH);
    g.drawRect(x + 6, y + 6, s * w + 2, s * h + 2);
    g.drawImage(state.img, x + 7, y + 7, x + 7 + s * w, y + 7 + s * h, 0, 0, w, h, null);
    // draw a little cursor for sanity
    if (blinkOption == null) blinkOption = BLINK_OPTIONS[0];
    if (BLINK_YES.equals(blinkOption)
        && blink()
        && state.lastX >= 0
        && state.lastX < w
        && state.lastY >= 0
        && state.lastY < h) {
      g.setColor(new Color(cm.getRGB(state.color)));
      g.fillRect(x + 7 + state.lastX * s, y + 7 + state.lastY * s, s, s);
    }
  }

  private State getState(CircuitState circuitState) {
    var state = (State) circuitState.getData(this);
    if (state == null) {
      state = new State(new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
      circuitState.setData(this, state);
    }
    return state;
  }

  private static class State implements ComponentState, Cloneable {
    public Value lastClock = null;
    public final BufferedImage img;
    public int lastX;
    public int lastY;
    public int color;

    State(BufferedImage img) {
      this.img = img;
      reset();
    }

    public void reset() {
      final var g = img.getGraphics();
      g.setColor(Color.YELLOW);
      g.fillRect(0, 0, img.getWidth(), img.getHeight());
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public boolean tick(Value clk) {
      boolean rising = (lastClock == null || (lastClock == Value.FALSE && clk == Value.TRUE));
      lastClock = clk;
      return rising;
    }
  }

  @Override
  public Object getFeature(Object key) {
    if (key == ToolTipMaker.class) return this;
    else return super.getFeature(key);
  }

  @Override
  public String getToolTip(ComponentUserEvent e) {
    int end = -1;
    for (var i = getEnds().size() - 1; i >= 0; i--) {
      if (getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
        end = i;
        break;
      }
    }
    switch (end) {
      case P_CLK:
        return S.get("rgbVideoCLK");
      case P_WE:
        return S.get("rgbVideoWE");
      case P_X:
        return S.get("rgbVideoX");
      case P_Y:
        return S.get("rgbVideoY");
      case P_DATA:
        AttributeSet attrs = getAttributeSet();
        return S.get("rgbVideoData", attrs.getValue(COLOR_OPTION));
      case P_RST:
        return S.get("rgbVideoRST");
      default:
        return null;
    }
  }

  @Override
  public void attributeListChanged(AttributeEvent e) {}

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    configureComponent();
  }

  void configureComponent() {
    final var attrs = getAttributeSet();
    final var bpp = getColorModel(attrs.getValue(COLOR_OPTION)).getPixelSize();
    final var xs = 31 - Integer.numberOfLeadingZeros(attrs.getValue(WIDTH_OPTION));
    final var ys = 31 - Integer.numberOfLeadingZeros(attrs.getValue(HEIGHT_OPTION));
    setEnd(P_X, getLocation().translate(40, 0), BitWidth.create(xs), EndData.INPUT_ONLY);
    setEnd(P_Y, getLocation().translate(50, 0), BitWidth.create(ys), EndData.INPUT_ONLY);
    setEnd(P_DATA, getLocation().translate(60, 0), BitWidth.create(bpp), EndData.INPUT_ONLY);
    recomputeBounds();
    fireComponentInvalidated(new ComponentEvent(this));
  }
}
