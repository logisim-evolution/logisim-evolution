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

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class Buzzer extends InstanceFactory {
  private static class Data implements InstanceData {
    private AtomicBoolean is_on = new AtomicBoolean(false);
    private final int SAMPLE_RATE = 44100;
    private double hz = 523;
    private double vol = 12;
    private Thread thread;

    public Data() {
      StartThread();
    }

    @Override
    public Object clone() {
      return new Data();
    }
    
    public void StartThread() {
      // avoid crash (for example if you connect a clock at 4KHz to the enable pin)
      if (Thread.activeCount() > 100) return;
      thread =
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  SourceDataLine line = null;
                  AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
                  try {
                    line = AudioSystem.getSourceDataLine(format);
                    line.open(format, SAMPLE_RATE / 10);
                  } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Could not initialise audio");
                    return;
                  }
                  line.start();
                  byte[] audioData = new byte[1];
                  while (is_on.get()) {
                    for (int i = 0; is_on.get() && i < SAMPLE_RATE * 2; i += 2) {
                      audioData[0] =
                          (byte) Math.round(Math.sin(Math.PI * i * hz / SAMPLE_RATE) * vol);
                      line.write(audioData, 0, 1);
                    }
                  }
                  line.stop();
                  line.drain();
                  line.close();
                }
              });
      thread.start();
      thread.setName("Sound Thread");
    }
  }

  private static final byte FREQ = 0;
  private static final byte ENABLE = 1;
  private static final byte VOL = 2;
  private static final Attribute<BitWidth> VOLUME_WIDTH =
      Attributes.forBitWidth("vol_width", S.getter("buzzerVolumeBitWidth"));

  private static final AttributeOption Hz = new AttributeOption("Hz", S.getter("Hz"));
  private static final AttributeOption dHz = new AttributeOption("dHz", S.getter("dHz (0.1Hz)"));
  private static final Attribute<AttributeOption> FREQUENCY_MEASURE =
      Attributes.forOption(
          "freq_measure", S.getter("buzzerFrequecy"), new AttributeOption[] {Hz, dHz});

  public static void StopBuzzerSound(Component comp, CircuitState circState) {
    // static method, have to check if the comp parameter is a Buzzer or contains it
    ComponentFactory compFact = comp.getFactory();
    // if it is a buzzer, stop its sound thread
    if (compFact instanceof Buzzer) {
      Data d = (Data) circState.getData(comp);
      if (d != null && d.thread.isAlive()) {
        d.is_on.set(false);
      }
    }
    // if it's a subcircuit search other buzzer's instances inside it and stop all
    // sound threads
    else if (compFact instanceof SubcircuitFactory) {
      for (Component subComponent :
          ((SubcircuitFactory) comp.getFactory()).getSubcircuit().getComponents()) {
        // recursive if there are other subcircuits
        StopBuzzerSound(subComponent, ((SubcircuitFactory) compFact).getSubstate(circState, comp));
      }
    }
  }

  public Buzzer() {
    super("Buzzer", S.getter("buzzerComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING, FREQUENCY_MEASURE, VOLUME_WIDTH, StdAttr.LABEL, StdAttr.LABEL_FONT
        },
        new Object[] {Direction.WEST, Hz, BitWidth.create(7), "", StdAttr.DEFAULT_LABEL_FONT});
    setFacingAttribute(StdAttr.FACING);
    setIconName("buzzer.gif");
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    Bounds b = instance.getBounds();
    updateports(instance);
    instance.addAttributeListener();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        b.getX() + b.getWidth() / 2,
        b.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction dir = attrs.getValue(StdAttr.FACING);
    if (dir == Direction.EAST || dir == Direction.WEST)
      return Bounds.create(-40, -20, 40, 40).rotate(Direction.EAST, dir, 0, 0);
    else return Bounds.create(-20, 0, 40, 40).rotate(Direction.NORTH, dir, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updateports(instance);
    } else if (attr == VOLUME_WIDTH) {
      updateports(instance);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Bounds b = painter.getBounds();
    Graphics g = painter.getGraphics();
    g.setColor(Color.GRAY);
    g.drawOval(b.getX(), b.getY(), 40, 40);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds b = painter.getBounds();
    int x = b.getX();
    int y = b.getY();
    byte height = (byte) b.getHeight();
    byte width = (byte) b.getWidth();
    g.setColor(Color.DARK_GRAY);
    g.fillOval(x, y, 40, 40);
    g.setColor(Color.GRAY);
    GraphicsUtil.switchToWidth(g, 2);
    for (byte k = 8; k <= 16; k += 4) {
      g.drawOval(x + 20 - k, y + 20 - k, k * 2, k * 2);
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.DARK_GRAY);
    g.drawLine(x + 4, y + height / 2, x + 36, y + height / 2);
    g.drawLine(x + width / 2, y + 4, x + width / 2, y + 36);
    g.setColor(Color.BLACK);
    g.fillOval(x + 15, y + 15, 10, 10);
    g.drawOval(x, y, 40, 40);
    painter.drawPorts();
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    Data d = (Data) state.getData();
    boolean active = state.getPortValue(ENABLE) == Value.TRUE;
    if (d == null) {
      state.setData(d = new Data());
    }
    d.is_on.set(active);

    int freq = (int)state.getPortValue(FREQ).toLongValue();
    if (freq >= 0) {
      if (state.getAttributeValue(FREQUENCY_MEASURE) == dHz) freq /= 10;
      d.hz = freq;
    }
    if (state.getPortValue(VOL).isFullyDefined()) {
      int vol = (int)state.getPortValue(VOL).toLongValue();
      byte VolumeWidth = (byte) state.getAttributeValue(VOLUME_WIDTH).getWidth();
      d.vol = ((vol & 0xffffffffL) * 127) / (Math.pow(2, VolumeWidth) - 1);
    }
    if (active && !d.thread.isAlive()) d.StartThread();
  }

  private void updateports(Instance instance) {
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    byte VolumeWidth = (byte) instance.getAttributeValue(VOLUME_WIDTH).getWidth();
    Port[] p = new Port[3];
    if (dir == Direction.EAST || dir == Direction.WEST) {
      p[FREQ] = new Port(0, -10, Port.INPUT, 14);
      p[VOL] = new Port(0, 10, Port.INPUT, VolumeWidth);
    } else {
      p[FREQ] = new Port(-10, 0, Port.INPUT, 14);
      p[VOL] = new Port(10, 0, Port.INPUT, VolumeWidth);
    }
    p[ENABLE] = new Port(0, 0, Port.INPUT, 1);
    p[FREQ].setToolTip(S.getter("buzzerFrequecy"));
    p[ENABLE].setToolTip(S.getter("enableSound"));
    p[VOL].setToolTip(S.getter("buzzerVolume"));
    instance.setPorts(p);
  }

  @Override
  public void removeComponent(Circuit circ, Component c , CircuitState state) {
    StopBuzzerSound(c, state);
  }

}
