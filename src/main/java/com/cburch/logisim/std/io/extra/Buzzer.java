/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Buzzer extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Buzzer";

  private static final byte FREQ = 0;
  private static final byte ENABLE = 1;
  private static final byte VOL = 2;
  private static final byte PW = 3;
  private static final Attribute<BitWidth> VOLUME_WIDTH =
      Attributes.forBitWidth("vol_width", S.getter("buzzerVolumeBitWidth"));
  private static final AttributeOption Hz = new AttributeOption("Hz", S.getter("Hz"));
  private static final AttributeOption dHz = new AttributeOption("dHz", S.getter("buzzerUnitDhz"));
  private static final Attribute<AttributeOption> FREQUENCY_MEASURE =
      Attributes.forOption(
          "freq_measure", S.getter("buzzerFrequecy"), new AttributeOption[]{Hz, dHz});

  private static final AttributeOption Sine = new AttributeOption(BuzzerWaveform.Sine,
      S.getter("buzzerSine"));
  private static final AttributeOption Square = new AttributeOption(BuzzerWaveform.Square,
      S.getter("buzzerSquare"));
  private static final AttributeOption Triangle = new AttributeOption(BuzzerWaveform.Triangle,
      S.getter("buzzerTriangle"));
  private static final AttributeOption Sawtooth = new AttributeOption(BuzzerWaveform.Sawtooth,
      S.getter("buzzerSawtooth"));
  private static final AttributeOption Noise = new AttributeOption(BuzzerWaveform.Noise,
      S.getter("buzzerNoise"));
  private static final Attribute<AttributeOption> WAVEFORM =
      Attributes.forOption(
          "waveform", S.getter("buzzerWaveform"),
          new AttributeOption[]{Sine, Square, Triangle, Sawtooth, Noise});
  private static final AttributeOption C_BOTH = new AttributeOption(3,
      S.getter("buzzerChannelBoth"));
  private static final AttributeOption C_LEFT = new AttributeOption(1,
      S.getter("buzzerChannelLeft"));
  private static final AttributeOption C_RIGHT = new AttributeOption(2,
      S.getter("buzzerChannelRight"));
  private static final Attribute<AttributeOption> CHANNEL =
      Attributes.forOption(
          "channel", S.getter("buzzerChannel"), new AttributeOption[]{C_BOTH, C_LEFT, C_RIGHT});
  private static final Attribute<Integer> SMOOTH_LEVEL =
      Attributes.forIntegerRange("smooth_level", S.getter("buzzerSmoothLevel"), 0, 10);
  private static final Attribute<Integer> SMOOTH_WIDTH =
      Attributes.forIntegerRange("smooth_width", S.getter("buzzerSmoothWidth"), 1, 10);

  public Buzzer() {
    super(_ID, S.getter("buzzerComponent"));
    setAttributes(
        new Attribute[]{
            StdAttr.FACING,
            StdAttr.SELECT_LOC,
            FREQUENCY_MEASURE,
            VOLUME_WIDTH,
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            WAVEFORM,
            CHANNEL,
            SMOOTH_LEVEL,
            SMOOTH_WIDTH
        },
        new Object[]{
            Direction.WEST,
            StdAttr.SELECT_BOTTOM_LEFT,
            Hz,
            BitWidth.create(7),
            "",
            StdAttr.DEFAULT_LABEL_FONT,
            Sine,
            C_BOTH, 2, 2});
    setFacingAttribute(StdAttr.FACING);
    setIconName("buzzer.gif");
  }

  public static void StopBuzzerSound(Component comp, CircuitState circState) {
    // static method, have to check if the comp parameter is a Buzzer or contains it
    ComponentFactory compFact = comp.getFactory();
    // if it is a buzzer, stop its sound thread
    if (compFact instanceof Buzzer) {
      Data d = (Data) circState.getData(comp);
      if (d != null && d.thread.isAlive()) {
        d.is_on.set(false);
      }
    } else if (compFact instanceof SubcircuitFactory) {
      // if it's a subcircuit search other buzzer's instances inside it and stop all sound threads
      for (Component subComponent :
          ((SubcircuitFactory) comp.getFactory()).getSubcircuit().getComponents()) {
        // recursive if there are other subcircuits
        StopBuzzerSound(subComponent, ((SubcircuitFactory) compFact).getSubstate(circState, comp));
      }
    }
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
    if (dir == Direction.EAST || dir == Direction.WEST) {
      return Bounds.create(-40, -20, 40, 40).rotate(Direction.EAST, dir, 0, 0);
    } else {
      return Bounds.create(-20, 0, 40, 40).rotate(Direction.NORTH, dir, 0, 0);
    }
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updateports(instance);
    } else if (attr == VOLUME_WIDTH || attr == StdAttr.SELECT_LOC) {
      updateports(instance);
    } else if (attr == WAVEFORM || attr == CHANNEL || attr == SMOOTH_LEVEL || attr == SMOOTH_WIDTH) {
      instance.fireInvalidated();
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

  private Data getData(InstanceState state) {
    Data d = (Data) state.getData();
    if (d == null) {
      state.setData(d = new Data());
    }
    return d;
  }

  @Override
  public void propagate(InstanceState state) {
    Data d = getData(state);
    var active = state.getPortValue(ENABLE) == Value.TRUE;
    d.is_on.set(active);
    var freq = (int) state.getPortValue(FREQ).toLongValue();
    if (freq >= 0) {
      if (state.getAttributeValue(FREQUENCY_MEASURE) == dHz) {
        freq /= 10;
      }
      d.hz = freq;
    } else {
      d.hz = 440;
    }
    d.wf = (BuzzerWaveform) state.getAttributeValue(WAVEFORM).getValue();
    d.channels = (Integer) state.getAttributeValue(CHANNEL).getValue();
    if (state.getPortValue(PW).isFullyDefined())
      d.pw = (int) state.getPortValue(PW).toLongValue();
    else
      d.pw = 128;
    d.smoothLevel = state.getAttributeValue(SMOOTH_LEVEL);
    d.smoothWidth = state.getAttributeValue(SMOOTH_WIDTH);
    if (state.getPortValue(VOL).isFullyDefined()) {
      int vol = (int) state.getPortValue(VOL).toLongValue();
      byte VolumeWidth = (byte) state.getAttributeValue(VOLUME_WIDTH).getWidth();
      d.vol = ((vol & 0xffffffffL) * 32767) / (Math.pow(2, VolumeWidth) - 1);
    } else {
      d.vol = 0.5;
    }
    d.updateRequired = true;
    if (active && !d.thread.isAlive()) {
      d.StartThread();
    }
  }

  private void updateports(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    byte VolumeWidth = (byte) instance.getAttributeValue(VOLUME_WIDTH).getWidth();
    Port[] p = new Port[4];
    if (facing == Direction.EAST || facing == Direction.WEST) {
      p[FREQ] = new Port(0, -10, Port.INPUT, 14);
      p[VOL] = new Port(0, 10, Port.INPUT, VolumeWidth);
    } else {
      p[FREQ] = new Port(-10, 0, Port.INPUT, 14);
      p[VOL] = new Port(10, 0, Port.INPUT, VolumeWidth);
    }
    p[FREQ].setToolTip(S.getter("buzzerFrequecy"));
    p[VOL].setToolTip(S.getter("buzzerVolume"));
    p[ENABLE] = new Port(0, 0, Port.INPUT, 1);
    p[ENABLE].setToolTip(S.getter("enableSound"));
    Object selectLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    var xPw = 20;
    var yPw = 20;
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      xPw *= selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? -1 : 1;
      yPw *= facing == Direction.SOUTH ? -1 : 1;
    } else {
      xPw *= facing == Direction.EAST ? -1 : 1;
      yPw *= selectLoc == StdAttr.SELECT_TOP_RIGHT ? -1 : 1;
    }
    p[PW] = new Port(xPw, yPw, Port.INPUT, 8);
    p[PW].setToolTip(S.getter("buzzerDutyCycle"));
    instance.setPorts(p);
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    StopBuzzerSound(c, state);
  }

  private enum BuzzerWaveform {
    Sine((i, hz, pw) -> Math.sin(i * hz * 2 * Math.PI)),
    Square((i, hz, pw) -> (hz * i) % 1 < pw ? 1 : -1),
    Triangle((i, hz, pw) -> Math.asin(Sine.strategy.amplitude(i, hz, pw)) * 2 / Math.PI),
    Sawtooth((i, hz, pw) -> 2 * ((hz * i) % 1) - 1),
    Noise((i, hz, pw) -> Math.random() * 2 - 1);

    public final BuzzerWaveformStrategy strategy;

    BuzzerWaveform(BuzzerWaveformStrategy strategy) {
      this.strategy = strategy;
    }
  }

  private interface BuzzerWaveformStrategy {
    double amplitude(double i, double hz, double pw);
  }

  private static class Data implements InstanceData {

    private int sampleRate;
    private final AtomicBoolean is_on = new AtomicBoolean(false);
    public int pw;
    public int channels;
    private int hz;
    private double vol;
    private int smoothLevel = 0;
    private int smoothWidth = 0;
    private boolean updateRequired = true;
    private BuzzerWaveform wf = BuzzerWaveform.Sine;
    private Thread thread;

    public Data() {
      StartThread();
    }

    @Override
    public Object clone() {
      return new Data();
    }

    public void ThreadFunc() {
      AudioFormat af = null;
      Clip clip = null;
      AudioInputStream ais = null;
      var oldfreq = -1;
      var oldpw = -1;
      try {
        while (is_on.get()) {
          if (updateRequired) {
            updateRequired = false;

            if (!(hz >= 20 && hz <= 20000)) {
              return;
            }

            if (hz != oldfreq) {
              sampleRate = (int) Math.ceil(44100.0 / hz) * hz;
              af = new AudioFormat(sampleRate, 16, 2, true, false);
              oldfreq = hz;
            }

            // TODO: Computing all those values takes time; it may be interesting to replace this by a LUT
            var cycle = Math.max(1, sampleRate / hz);
            var values = new double[4 * cycle];
            for (var i = 0; i < values.length; i++) {
              values[i] = wf.strategy.amplitude(i / (double) sampleRate, hz, pw / 256.0);
            }

            if (wf != BuzzerWaveform.Sine && smoothLevel > 0 && smoothWidth > 0) {
              var nsig = new double[values.length];
              for (var k = 0; k < smoothLevel; k++) {
                var sum = 0;
                for (var i = 0; i < values.length; i++) {
                  if (i > 2 * smoothWidth) {
                    nsig[i - smoothWidth - 1] = (sum - values[i - smoothWidth - 1]) / (2 * smoothWidth);
                    sum -= values[i - 2 * smoothWidth - 1];
                  }
                  sum += values[i];
                }
                System.arraycopy(nsig, smoothWidth, values, smoothWidth, values.length - 2 * smoothWidth);
              }
            }

            var rvalues = new double[sampleRate];
            for (var i = 0; i < sampleRate; i += cycle) {
              System.arraycopy(values, 2 * cycle, rvalues, i, Math.min(cycle, sampleRate - i));
            }

            byte[] buf = new byte[4 * sampleRate];
            for (int i = 0, j = 0; i < buf.length; i += 4, j++) {
              var val = (short) Math.round(rvalues[j] * vol);
              if ((channels & 1) != 0) {
                buf[i] = (byte) (val & 0xff);
                buf[i + 1] = (byte) (val >> 8);
              }
              if ((channels & 2) != 0) {
                buf[i + 2] = (byte) (val & 0xff);
                buf[i + 3] = (byte) (val >> 8);
              }
            }

            var newAis = new AudioInputStream(
                new ByteArrayInputStream(buf),
                af,
                buf.length);

            Clip newClip = AudioSystem.getClip();
            newClip.open(newAis);

            if (clip != null) {
              newClip.loop(Clip.LOOP_CONTINUOUSLY);
              clip.close();
              ais.close();
            }

            clip = newClip;
            ais = newAis;
            clip.loop(Clip.LOOP_CONTINUOUSLY);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (clip != null) {
          clip.close();
        }
        if (ais != null) {
          try {
            ais.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void StartThread() {
      // avoid crash (for example if you connect a clock at 4KHz to the enable pin)
      if (Thread.activeCount() > 100) {
        return;
      }
      thread = new Thread(this::ThreadFunc);
      thread.start();
      thread.setName("Sound Thread");
    }
  }

}
