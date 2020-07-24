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


/*
 * This file was originally written by Kevin Walsh <kwalsh@cs.cornell.edu> for
 * Cornell's CS 314 computer organization course. It was subsequently modified
 * Martin Dybdal <dybber@dybber.dk> and Anders Boesen Lindbo Larsen
 * <abll@diku.dk> for use in the computer architecture class at the Department
 * of Computer Science, University of Copenhagen.
 */
package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

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
import com.cburch.logisim.tools.ToolTipMaker;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;

// 128 x 128 pixel LCD display with 8bpp color (byte addressed)
class Video extends ManagedComponent implements ToolTipMaker, AttributeListener {
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
  static final String[] COLOR_OPTIONS = {
    COLOR_RGB,
    COLOR_555_RGB,
    COLOR_565_RGB,
    COLOR_111_RGB,
    COLOR_ATARI,
    COLOR_XTERM16,
    COLOR_XTERM256,
    COLOR_GRAY4
  };

  static final Integer[] SIZE_OPTIONS = {2, 4, 8, 16, 32, 64, 128, 256};

  public static final Attribute BLINK_OPTION =
      Attributes.forOption("cursor", S.getter("rgbVideoCursor"), BLINK_OPTIONS);
  public static final Attribute RESET_OPTION =
      Attributes.forOption("reset", S.getter("rgbVideoReset"), RESET_OPTIONS);
  public static final Attribute COLOR_OPTION =
      Attributes.forOption("color", S.getter("rgbVideoColor"), COLOR_OPTIONS);
  public static final Attribute<Integer> WIDTH_OPTION =
      Attributes.forOption("width", S.getter("rgbVideoWidth"), SIZE_OPTIONS);
  public static final Attribute<Integer> HEIGHT_OPTION =
      Attributes.forOption("height", S.getter("rgbVideoHeight"), SIZE_OPTIONS);
  public static final Attribute<Integer> SCALE_OPTION =
      Attributes.forIntegerRange("scale", S.getter("rgbVideoScale"), 1, 8);

  private static final Attribute[] ATTRIBUTES = {
    BLINK_OPTION, RESET_OPTION, COLOR_OPTION, WIDTH_OPTION, HEIGHT_OPTION, SCALE_OPTION
  };

  private static class Factory extends AbstractComponentFactory {
    private Factory() {}

    public String getName() {
      return "RGB Video";
    }

    public String getDisplayName() {
      return S.get("rgbVideoComponent");
    }

    public AttributeSet createAttributeSet() {
      return AttributeSets.fixedSet(
          ATTRIBUTES,
          new Object[] {
            BLINK_OPTIONS[0],
            RESET_OPTIONS[0],
            COLOR_OPTIONS[0],
            128,
            128,
            2
          });
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
      return new Video(loc, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
      int s = attrs.getValue(SCALE_OPTION);
      int w = attrs.getValue(WIDTH_OPTION);
      int h = attrs.getValue(HEIGHT_OPTION);
      int bw = (s * w + 14 < 100 ? 100 : s * w + 14);
      int bh = (s * h + 14 < 20 ? 20 : s * h + 14);
      return Bounds.create(-30, -bh, bw, bh);
    }

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

  public ComponentFactory getFactory() {
    return factory;
  }
  
  public void setFactory(ComponentFactory fact) {}

  Location loc(int pin) {
    return getEndLocation(pin);
  }

  Value val(CircuitState s, int pin) {
    return s.getValue(loc(pin));
  }

  int addr(CircuitState s, int pin) {
    return (int)val(s, pin).toLongValue();
  }

  public void propagate(CircuitState circuitState) {
    State state = getState(circuitState);
    AttributeSet attrs = getAttributeSet();
    int x = addr(circuitState, P_X);
    int y = addr(circuitState, P_Y);
    int color = addr(circuitState, P_DATA);
    state.last_x = x;
    state.last_y = y;
    state.color = color;

    Object reset_option = attrs.getValue(RESET_OPTION);
    if (reset_option == null) reset_option = RESET_OPTIONS[0];
    ColorModel cm = getColorModel(attrs.getValue(COLOR_OPTION));
    int w = attrs.getValue(WIDTH_OPTION);
    int h = attrs.getValue(HEIGHT_OPTION);

    if (state.tick(val(circuitState, P_CLK)) && val(circuitState, P_WE) == Value.TRUE) {
      Graphics g = state.img.getGraphics();
      g.setColor(new Color(cm.getRGB(color)));
      g.fillRect(x, y, 1, 1);
      if (RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
      }
    }

    if (!RESET_SYNC.equals(reset_option) && val(circuitState, P_RST) == Value.TRUE) {
      Graphics g = state.img.getGraphics();
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, w, h);
    }
  }

  public void draw(ComponentDrawContext context) {
    Location loc = getLocation();
    int size = getBounds().getWidth();
    State s = getState(context.getCircuitState());
    drawVideo(context, loc.getX(), loc.getY(), s);
  }

  static void drawVideoIcon(ComponentDrawContext context, int x, int y) {
    Graphics g = context.getGraphics().create();
    g.translate(x, y);
    g.setColor(Color.WHITE);
    g.fillRoundRect(scale(2), scale(2), scale(16 - 1), scale(16 - 1), scale(3), scale(3));
    g.setColor(Color.BLACK);
    g.drawRoundRect(scale(2),scale(2), scale(16 - 1), scale(16 - 1), scale(3), scale(3));
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

  static DirectColorModel rgb111 = new DirectColorModel(3, 0x4, 0x2, 0x1);
  static DirectColorModel rgb555 = new DirectColorModel(15, 0x7C00, 0x03E0, 0x001F);
  static DirectColorModel rgb565 = new DirectColorModel(16, 0xF800, 0x07E0, 0x001F);
  static DirectColorModel rgb = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
  static IndexColorModel gray4 =
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
  static IndexColorModel atari =
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
  static IndexColorModel xterm16 =
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
  static IndexColorModel xterm256 =
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

  static ColorModel getColorModel(Object model) {
    if (model == COLOR_RGB) return rgb;
    else if (model == COLOR_555_RGB) return rgb555;
    else if (model == COLOR_565_RGB) return rgb565;
    else if (model == COLOR_111_RGB) return rgb111;
    else if (model == COLOR_ATARI) return atari;
    else if (model == COLOR_XTERM16) return xterm16;
    else if (model == COLOR_XTERM256) return xterm256;
    else if (model == COLOR_GRAY4) return gray4;
    else return rgb555;
  }

  void drawVideo(ComponentDrawContext context, int x, int y, State state) {
    Graphics g = context.getGraphics();

    AttributeSet attrs = getAttributeSet();
    Object blink_option = attrs.getValue(BLINK_OPTION);
    Object reset_option = attrs.getValue(RESET_OPTION);
    ColorModel cm = getColorModel(attrs.getValue(COLOR_OPTION));

    int s = attrs.getValue(SCALE_OPTION);
    int w = attrs.getValue(WIDTH_OPTION);
    int h = attrs.getValue(HEIGHT_OPTION);
    int bw = (s * w + 14 < 100 ? 100 : s * w + 14);
    int bh = (s * h + 14 < 20 ? 20 : s * h + 14);

    x += -30;
    y += -bh;

    g.drawRoundRect(x, y, bw, bh, 6, 6);
    for (int i = 0; i < 6; i++) {
      if (i != P_CLK) context.drawPin(this, i);
    }
    context.drawClock(this, P_CLK, Direction.NORTH);
    g.drawRect(x + 6, y + 6, s * w + 2, s * h + 2);
    g.drawImage(state.img, x + 7, y + 7, x + 7 + s * w, y + 7 + s * h, 0, 0, w, h, null);
    // draw a little cursor for sanity
    if (blink_option == null) blink_option = BLINK_OPTIONS[0];
    if (BLINK_YES.equals(blink_option)
        && blink()
        && state.last_x >= 0
        && state.last_x < w
        && state.last_y >= 0
        && state.last_y < h) {
      g.setColor(new Color(cm.getRGB(state.color)));
      g.fillRect(x + 7 + state.last_x * s, y + 7 + state.last_y * s, s, s);
    }
  }

  private State getState(CircuitState circuitState) {
    State state = (State) circuitState.getData(this);
    if (state == null) {
      state = new State(new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
      circuitState.setData(this, state);
    }
    return state;
  }

  private class State implements ComponentState, Cloneable {
    public Value lastClock = null;
    public BufferedImage img;
    public int last_x, last_y, color;

    State(BufferedImage img) {
      this.img = img;
      reset();
    }

    public void reset() {
      Graphics g = img.getGraphics();
      g.setColor(Color.YELLOW);
      g.fillRect(0, 0, img.getWidth(), img.getHeight());
    }

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

  public String getToolTip(ComponentUserEvent e) {
    int end = -1;
    for (int i = getEnds().size() - 1; i >= 0; i--) {
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
        return S.fmt("rgbVideoData", attrs.getValue(COLOR_OPTION).toString());
      case P_RST:
        return S.get("rgbVideoRST");
      default:
        return null;
    }
  }

  public void attributeListChanged(AttributeEvent e) {}

  public void attributeValueChanged(AttributeEvent e) {
    configureComponent();
  }

  void configureComponent() {
    AttributeSet attrs = getAttributeSet();
    int bpp = getColorModel(attrs.getValue(COLOR_OPTION)).getPixelSize();
    int xs = 31 - Integer.numberOfLeadingZeros(attrs.getValue(WIDTH_OPTION));
    int ys = 31 - Integer.numberOfLeadingZeros(attrs.getValue(HEIGHT_OPTION));
    setEnd(P_X, getLocation().translate(40, 0), BitWidth.create(xs), EndData.INPUT_ONLY);
    setEnd(P_Y, getLocation().translate(50, 0), BitWidth.create(ys), EndData.INPUT_ONLY);
    setEnd(P_DATA, getLocation().translate(60, 0), BitWidth.create(bpp), EndData.INPUT_ONLY);
    recomputeBounds();
    fireComponentInvalidated(new ComponentEvent(this));
  }
}
