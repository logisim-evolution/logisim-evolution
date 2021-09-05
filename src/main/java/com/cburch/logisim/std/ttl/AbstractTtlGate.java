/*
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

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;

public abstract class AbstractTtlGate extends InstanceFactory {
  protected static final int PIN_WIDTH = 10;
  protected static final int PIN_HEIGHT = 7;
  private int height = 60;
  protected final byte pinnumber;
  private final String name;
  private byte ngatestodraw = 0;
  protected String[] portnames = null;
  private final HashSet<Byte> outputports = new HashSet<>();
  private final HashSet<Byte> unusedpins = new HashSet<>();

  /**
   * @param name = name to display in the center of the TTl
   * @param pins = the total number of pins (GND and VCC included)
   * @param outputports = an array with the indexes of the output ports (indexes are the same you
   *     can find on Google searching the TTL you want to add)
   */
  protected AbstractTtlGate(String name, byte pins, byte[] outputports) {
    super(name);
    setIconName("ttl.gif");
    setAttributes(
        new Attribute[] {StdAttr.FACING, TtlLibrary.VCC_GND, TtlLibrary.DRAW_INTERNAL_STRUCTURE, StdAttr.LABEL},
        new Object[] {Direction.EAST, false, false, ""});
    setFacingAttribute(StdAttr.FACING);
    this.name = name;
    this.pinnumber = pins;
    for (byte outputport : outputports)
      this.outputports.add(outputport);
  }

  protected AbstractTtlGate(String name, byte pins, byte[] outputports, byte[] NotUsedPins) {
    this(name, pins, outputports);
    if (NotUsedPins == null) return;
    for (byte notUsedPin : NotUsedPins)
      unusedpins.add(notUsedPin);
  }

  /**
   * @param name = name to display in the center of the TTl
   * @param pins = the total number of pins (GND and VCC included)
   * @param outputports = an array with the indexes of the output ports (indexes are the same you
   *     can find on Google searching the TTL you want to add)
   * @param drawgates = if true, it calls the paintInternal method many times as the number of
   *     output ports passing the coordinates
   */
  protected AbstractTtlGate(String name, byte pins, byte[] outputports, boolean drawgates) {
    this(name, pins, outputports);
    this.ngatestodraw = (byte) (drawgates ? outputports.length : 0);
  }

  /**
   * @param name = name to display in the center of the TTl
   * @param pins = the total number of pins (GND and VCC included)
   * @param outputports = an array with the indexes of the output ports (indexes are the same you
   *     can find on Google searching the TTL you want to add)
   * @param Ttlportnames = an array of strings which will be tooltips of the corresponding port in
   *     the order you pass
   */
  protected AbstractTtlGate(String name, byte pins, byte[] outputports, String[] Ttlportnames) {
    // the ttl name, the total number of pins and an array with the indexes of
    // output ports (indexes are the one you can find on Google), an array of
    // strings which will be tooltips of the corresponding port in order
    this(name, pins, outputports);
    this.portnames = Ttlportnames;
  }

  protected AbstractTtlGate(
      String name, byte pins, byte[] outputports, byte[] NotUsedPins, String[] Ttlportnames) {
    this(name, pins, outputports);
    portnames = Ttlportnames;
    if (NotUsedPins == null) return;
    for (final var notUsedPin : NotUsedPins) unusedpins.add(notUsedPin);
  }

  protected AbstractTtlGate(
      String name, byte pins, byte[] outputports, String[] Ttlportnames, int height) {
    // the ttl name, the total number of pins and an array with the indexes of
    // output ports (indexes are the one you can find on Google), an array of
    // strings which will be tooltips of the corresponding port in order
    this(name, pins, outputports);
    this.height = height;
    this.portnames = Ttlportnames;
  }

  private void computeTextField(Instance instance) {
    final var bds = instance.getBounds();
    final var dir = instance.getAttributeValue(StdAttr.FACING);
    if (dir == Direction.EAST || dir == Direction.WEST)
      instance.setTextField(
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          bds.getX() + bds.getWidth() + 3,
          bds.getY() + bds.getHeight() / 2,
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER_OVERALL);
    else
      instance.setTextField(
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          bds.getX() + bds.getWidth() / 2,
          bds.getY() - 3,
          GraphicsUtil.H_CENTER,
          GraphicsUtil.V_CENTER_OVERALL);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updateports(instance);
    computeTextField(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var dir = attrs.getValue(StdAttr.FACING);
    return Bounds.create(0, -30, this.pinnumber * 10, height).rotate(Direction.EAST, dir, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updateports(instance);
      computeTextField(instance);
    } else if (attr == TtlLibrary.VCC_GND) {
      updateports(instance);
    }
  }

  static Point TTLGetTranslatedXY(InstanceState state, MouseEvent e) {
    int x = 0, y = 0;
    final var loc = state.getInstance().getLocation();
    final var height = state.getInstance().getBounds().getHeight();
    final var width = state.getInstance().getBounds().getWidth();
    final var dir = state.getAttributeValue(StdAttr.FACING);
    if (dir.equals(Direction.EAST)) {
      x = e.getX() - loc.getX();
      y = e.getY() + 30 - loc.getY();
    } else if (dir.equals(Direction.WEST)) {
      x = loc.getX() - e.getX();
      y = height - (e.getY() + (height - 30) - loc.getY());
    } else if (dir.equals(Direction.NORTH)) {
      x = loc.getY() - e.getY();
      y = width - (loc.getX() + (width - 30) - e.getX());
    } else {
      x = e.getY() - loc.getY();
      y = (loc.getX() + 30 - e.getX());
    }
    return new Point(x, y);
  }

  protected void paintBase(InstancePainter painter, boolean drawname, boolean ghost) {
    final var dir = painter.getAttributeValue(StdAttr.FACING);
    final var g = (Graphics2D) painter.getGraphics();
    final var bds = painter.getBounds();
    final var x = bds.getX();
    final var y = bds.getY();
    var xp = x;
    var yp = y;
    var width = bds.getWidth();
    var height = bds.getHeight();
    for (byte i = 0; i < this.pinnumber; i++) {
      if (i < this.pinnumber / 2) {
        if (dir == Direction.WEST || dir == Direction.EAST) xp = i * 20 + (10 - PIN_WIDTH / 2) + x;
        else yp = i * 20 + (10 - PIN_WIDTH / 2) + y;
      } else {
        if (dir == Direction.WEST || dir == Direction.EAST) {
          xp = (i - this.pinnumber / 2) * 20 + (10 - PIN_WIDTH / 2) + x;
          yp = height + y - PIN_HEIGHT;
        } else {
          yp = (i - this.pinnumber / 2) * 20 + (10 - PIN_WIDTH / 2) + y;
          xp = width + x - PIN_HEIGHT;
        }
      }
      if (dir == Direction.WEST || dir == Direction.EAST) {
        // fill the background of white if selected from preferences
        g.drawRect(xp, yp, PIN_WIDTH, PIN_HEIGHT);
      } else {
        // fill the background of white if selected from preferences
        g.drawRect(xp, yp, PIN_HEIGHT, PIN_WIDTH);
      }
    }
    if (dir == Direction.SOUTH) {
      // fill the background of white if selected from preferences
      g.drawRoundRect(x + PIN_HEIGHT, y, bds.getWidth() - PIN_HEIGHT * 2, bds.getHeight(), 10, 10);
      g.drawArc(x + width / 2 - 7, y - 7, 14, 14, 180, 180);
    } else if (dir == Direction.WEST) {
      // fill the background of white if selected from preferences
      g.drawRoundRect(x, y + PIN_HEIGHT, bds.getWidth(), bds.getHeight() - PIN_HEIGHT * 2, 10, 10);
      g.drawArc(x + width - 7, y + height / 2 - 7, 14, 14, 90, 180);
    } else if (dir == Direction.NORTH) {
      // fill the background of white if selected from preferences
      g.drawRoundRect(x + PIN_HEIGHT, y, bds.getWidth() - PIN_HEIGHT * 2, bds.getHeight(), 10, 10);
      g.drawArc(x + width / 2 - 7, y + height - 7, 14, 14, 0, 180);
    } else { // east
      // fill the background of white if selected from preferences
      g.drawRoundRect(x, y + PIN_HEIGHT, bds.getWidth(), bds.getHeight() - PIN_HEIGHT * 2, 10, 10);
      g.drawArc(x - 7, y + height / 2 - 7, 14, 14, 270, 180);
    }
    g.rotate(Math.toRadians(-dir.toDegrees()), x + width / 2, y + height / 2);
    if (drawname) {
      g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 14));
      GraphicsUtil.drawCenteredText(
          g, this.name, x + bds.getWidth() / 2, y + bds.getHeight() / 2 - 4);
    }
    if (dir == Direction.WEST || dir == Direction.EAST) {
      xp = x;
      yp = y;
    } else {
      xp = x + (width - height) / 2;
      yp = y + (height - width) / 2;
      width = bds.getHeight();
      height = bds.getWidth();
    }
    g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 7));
    GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + PIN_HEIGHT + 4);
    GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - PIN_HEIGHT - 7);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paintBase(painter, true, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.drawPorts();
    final var g = (Graphics2D) painter.getGraphics();
    painter.drawLabel();
    if (!painter.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) {
      final var dir = painter.getAttributeValue(StdAttr.FACING);
      final var bds = painter.getBounds();
      final var x = bds.getX();
      final var y = bds.getY();
      var xp = x;
      var yp = y;
      final var width = bds.getWidth();
      final var height = bds.getHeight();
      for (byte i = 0; i < this.pinnumber; i++) {
        if (i == this.pinnumber / 2) {
          xp = x;
          yp = y;
          if (dir == Direction.WEST || dir == Direction.EAST) {
            g.setColor(Color.DARK_GRAY.darker());
            g.fillRoundRect(xp, yp + PIN_HEIGHT, width, height - PIN_HEIGHT * 2 + 2, 10, 10);
            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect(xp, yp + PIN_HEIGHT, width, height - PIN_HEIGHT * 2 - 2, 10, 10);
            g.setColor(Color.BLACK);
            g.drawRoundRect(xp, yp + PIN_HEIGHT, width, height - PIN_HEIGHT * 2 - 2, 10, 10);
            g.drawRoundRect(xp, yp + PIN_HEIGHT, width, height - PIN_HEIGHT * 2 + 2, 10, 10);
          } else {
            g.setColor(Color.DARK_GRAY.darker());
            g.fillRoundRect(xp + PIN_HEIGHT, yp, width - PIN_HEIGHT * 2, height, 10, 10);
            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect(xp + PIN_HEIGHT, yp, width - PIN_HEIGHT * 2, height - 4, 10, 10);
            g.setColor(Color.BLACK);
            g.drawRoundRect(xp + PIN_HEIGHT, yp, width - PIN_HEIGHT * 2, height - 4, 10, 10);
            g.drawRoundRect(xp + PIN_HEIGHT, yp, width - PIN_HEIGHT * 2, height, 10, 10);
          }
          if (dir == Direction.SOUTH) g.fillArc(xp + width / 2 - 7, yp - 7, 14, 14, 180, 180);
          else if (dir == Direction.WEST)
            g.fillArc(xp + width - 7, yp + height / 2 - 7, 14, 14, 90, 180);
          else if (dir == Direction.NORTH)
            g.fillArc(xp + width / 2 - 7, yp + height - 11, 14, 14, 0, 180);
          else // east
            g.fillArc(xp - 7, yp + height / 2 - 7, 14, 14, 270, 180);
        }
        if (i < this.pinnumber / 2) {
          if (dir == Direction.WEST || dir == Direction.EAST) xp = i * 20 + (10 - PIN_WIDTH / 2) + x;
          else yp = i * 20 + (10 - PIN_WIDTH / 2) + y;
        } else {
          if (dir == Direction.WEST || dir == Direction.EAST) {
            xp = (i - this.pinnumber / 2) * 20 + (10 - PIN_WIDTH / 2) + x;
            yp = height + y - PIN_HEIGHT;
          } else {
            yp = (i - this.pinnumber / 2) * 20 + (10 - PIN_WIDTH / 2) + y;
            xp = width + x - PIN_HEIGHT;
          }
        }
        if (dir == Direction.WEST || dir == Direction.EAST) {
          g.setColor(Color.LIGHT_GRAY);
          g.fillRect(xp, yp, PIN_WIDTH, PIN_HEIGHT);
          g.setColor(Color.BLACK);
          g.drawRect(xp, yp, PIN_WIDTH, PIN_HEIGHT);
        } else {
          g.setColor(Color.LIGHT_GRAY);
          g.fillRect(xp, yp, PIN_HEIGHT, PIN_WIDTH);
          g.setColor(Color.BLACK);
          g.drawRect(xp, yp, PIN_HEIGHT, PIN_WIDTH);
        }
      }

      g.setColor(Color.LIGHT_GRAY.brighter());
      g.rotate(Math.toRadians(-dir.toDegrees()), x + width / 2, y + height / 2);
      g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 14));
      GraphicsUtil.drawCenteredText(g, this.name, x + width / 2, y + height / 2 - 4);
      g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 7));
      if (dir == Direction.WEST || dir == Direction.EAST) {
        xp = x;
        yp = y;
      } else {
        xp = x + (width - height) / 2;
        yp = y + (height - width) / 2;
      }
      if (dir == Direction.SOUTH) {
        GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + PIN_HEIGHT + 4);
        GraphicsUtil.drawCenteredText(g, "GND", xp + height - 14, yp + width - PIN_HEIGHT - 8);
      } else if (dir == Direction.WEST) {
        GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + PIN_HEIGHT + 6);
        GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - PIN_HEIGHT - 8);
      } else if (dir == Direction.NORTH) {
        GraphicsUtil.drawCenteredText(g, "Vcc", xp + 14, yp + PIN_HEIGHT + 4);
        GraphicsUtil.drawCenteredText(g, "GND", xp + height - 10, yp + width - PIN_HEIGHT - 8);
      } else { // east
        GraphicsUtil.drawCenteredText(g, "Vcc", xp + 10, yp + PIN_HEIGHT + 4);
        GraphicsUtil.drawCenteredText(g, "GND", xp + width - 10, yp + height - PIN_HEIGHT - 10);
      }
    } else paintInternalBase(painter);
  }

  /**
   * @param painter = the instance painter you have to use to create Graphics (Graphics g =
   *     painter.getGraphics())
   * @param x = if drawgates is false or not used, the component's left side; if drawgates is true
   *     it gets the component's width, subtracts 20 (for GND or Vcc) and divides for the number of
   *     outputs for each side, you'll get the x coordinate of the leftmost input -10 before the
   *     last output
   * @param y = the component's upper side
   * @param height = the component's height
   * @param up = true if drawgates is true when drawing the gates in the upper side (introduced this
   *     because can't draw upside down so you have to write what to draw if down and up)
   */
  public abstract void paintInternal(InstancePainter painter, int x, int y, int height, boolean up);

  private void paintInternalBase(InstancePainter painter) {
    final var dir = painter.getAttributeValue(StdAttr.FACING);
    final var bds = painter.getBounds();
    var x = bds.getX();
    var y = bds.getY();
    var width = bds.getWidth();
    var height = bds.getHeight();
    if (dir == Direction.SOUTH || dir == Direction.NORTH) {
      x += (width - height) / 2;
      y += (height - width) / 2;
      width = bds.getHeight();
      height = bds.getWidth();
    }

    if (this.ngatestodraw == 0)
      paintInternal(painter, x, y, height, false);
    else {
      paintBase(painter, false, false);
      for (byte i = 0; i < this.ngatestodraw; i++) {
        paintInternal(
            painter,
            x
                + (i < this.ngatestodraw / 2 ? i : i - this.ngatestodraw / 2)
                    * ((width - 20) / (this.ngatestodraw / 2))
                + (i < this.ngatestodraw / 2 ? 0 : 20),
            y,
            height,
            i >= this.ngatestodraw / 2);
      }
    }
  }

  /** Here you have to write the logic of your component */
  @Override
  public void propagate(InstanceState state) {
    final var NrOfUnusedPins = unusedpins.size();
    if (state.getAttributeValue(TtlLibrary.VCC_GND)
        && (state.getPortValue(this.pinnumber - 2 - NrOfUnusedPins) != Value.FALSE
            || state.getPortValue(this.pinnumber - 1 - NrOfUnusedPins) != Value.TRUE)) {
      var port = 0;
      for (byte i = 1; i <= pinnumber; i++) {
        if (!unusedpins.contains(i) && (i != (pinnumber / 2))) {
          if (outputports.contains(i)) state.setPort(port, Value.UNKNOWN, 1);
          port++;
        }
      }
    } else ttlpropagate(state);
  }

  public abstract void ttlpropagate(InstanceState state);

  private void updateports(Instance instance) {
    final var bds = instance.getBounds();
    final var dir = instance.getAttributeValue(StdAttr.FACING);
    var dx = 0;
    var dy = 0;
    final var width = bds.getWidth();
    final var height = bds.getHeight();
    byte portindex = 0;
    var isoutput = false;
    var hasvccgnd = instance.getAttributeValue(TtlLibrary.VCC_GND);
    var skip = false;
    final var NrOfUnusedPins = unusedpins.size();
    /*
     * array port is composed in this order: lower ports less GND, upper ports less
     * Vcc, GND, Vcc
     */
    final var ps = new Port[hasvccgnd ? this.pinnumber - NrOfUnusedPins : this.pinnumber - 2 - NrOfUnusedPins];

    for (byte i = 0; i < this.pinnumber; i++) {
      isoutput = outputports.contains((byte) (i + 1));
      skip = unusedpins.contains((byte) (i + 1));
      // set the position
      if (i < this.pinnumber / 2) {
        if (dir == Direction.EAST) {
          dx = i * 20 + 10;
          dy = height - 30;
        } else if (dir == Direction.WEST) {
          dx = -10 - 20 * i;
          dy = 30 - height;
        } else if (dir == Direction.NORTH) {
          dx = width - 30;
          dy = -10 - 20 * i;
        } else { // SOUTH
          dx = 30 - width;
          dy = i * 20 + 10;
        }
      } else {
        if (dir == Direction.EAST) {
          dx = width - (i - this.pinnumber / 2) * 20 - 10;
          dy = -30;
        } else if (dir == Direction.WEST) {
          dx = -width + (i - this.pinnumber / 2) * 20 + 10;
          dy = 30;
        } else if (dir == Direction.NORTH) {
          dx = -30;
          dy = -height + (i - this.pinnumber / 2) * 20 + 10;
        } else { // SOUTH
          dx = 30;
          dy = height - (i - this.pinnumber / 2) * 20 - 10;
        }
      }
      // Set the port (output/input)
      if (skip) {
        portindex--;
      } else if (isoutput) { // output port
        ps[portindex] = new Port(dx, dy, Port.OUTPUT, 1);
        if (this.portnames == null || this.portnames.length <= portindex)
          ps[portindex].setToolTip(S.getter("demultiplexerOutTip", ": " + (i + 1)));
        else
          ps[portindex].setToolTip(S.getter("demultiplexerOutTip", (i + 1) + ": " + this.portnames[portindex]));
      } else { // input port
        if (hasvccgnd && i == this.pinnumber - 1) { // Vcc
          ps[ps.length - 1] = new Port(dx, dy, Port.INPUT, 1);
          ps[ps.length - 1].setToolTip(S.getter("VCCPin", Integer.toString(this.pinnumber)));
        } else if (i == this.pinnumber / 2 - 1) { // GND
          if (hasvccgnd) {
            ps[ps.length - 2] = new Port(dx, dy, Port.INPUT, 1);
            ps[ps.length - 2].setToolTip(S.getter("GNDPin", Integer.toString(this.pinnumber / 2)));
          }
          portindex--;
        } else if (i != this.pinnumber - 1 && i != this.pinnumber / 2 - 1) { // normal output
          ps[portindex] = new Port(dx, dy, Port.INPUT, 1);
          if (this.portnames == null || this.portnames.length <= portindex)
            ps[portindex].setToolTip(S.getter("multiplexerInTip", ": " + (i + 1)));
          else
            ps[portindex].setToolTip(S.getter("multiplexerInTip", (i + 1) + ": " + this.portnames[portindex]));
        }
      }
      portindex++;
    }
    instance.setPorts(ps);
  }

  @Override
  public final void paintIcon(InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics().create();
    g.setColor(Color.DARK_GRAY.brighter());
    GraphicsUtil.switchToWidth(g, AppPreferences.getScaled(1));
    g.fillRoundRect(AppPreferences.getScaled(4), 0, AppPreferences.getScaled(8), AppPreferences.getScaled(16),
            AppPreferences.getScaled(3), AppPreferences.getScaled(3));
    g.setColor(Color.black);
    g.drawRoundRect(AppPreferences.getScaled(4), 0, AppPreferences.getScaled(8), AppPreferences.getScaled(16),
            AppPreferences.getScaled(3), AppPreferences.getScaled(3));
    final var wh1 = AppPreferences.getScaled(3);
    final var wh2 = AppPreferences.getScaled(2);
    for (int y = 0; y < 3; y++) {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(wh2, AppPreferences.getScaled(y * 5 + 1), wh1, wh1);
      g.fillRect(AppPreferences.getScaled(12), AppPreferences.getScaled(y * 5 + 1), wh1, wh1);
      g.setColor(Color.BLACK);
      g.drawRect(wh2, AppPreferences.getScaled(y * 5 + 1), wh1, wh1);
      g.drawRect(AppPreferences.getScaled(12), AppPreferences.getScaled(y * 5 + 1), wh1, wh1);
    }
    g.drawRoundRect(AppPreferences.getScaled(6), 0, AppPreferences.getScaled(6), AppPreferences.getScaled(16),
            AppPreferences.getScaled(3), AppPreferences.getScaled(3));
    g.dispose();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel("TTL" + getName()).toUpperCase();
  }

}
