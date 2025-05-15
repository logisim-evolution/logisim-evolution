/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

/* This file is adopted from the MIPS.jar library by
 * Martin Dybdal <dybber@dybber.dk> and
 * Anders Boesen Lindbo Larsen <abll@diku.dk>.
 * It was developed for the computer architecture class at the Department of
 * Computer Science, University of Copenhagen.
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class PlaTable {
  private final ArrayList<Row> rows = new ArrayList<>();
  private int inSize, outSize;
  private String label = "";

  public PlaTable(int inSz, int outSz, String l) {
    inSize = inSz;
    outSize = outSz;
    label = l;
  }

  public PlaTable(PlaTable other) {
    this(other.inSize, other.outSize, other.label);
    this.copyFrom(other);
  }

  public void setLabel(String l) {
    label = l;
  }

  public List<Row> rows() {
    return rows;
  }

  public void copyFrom(PlaTable other) {
    rows.clear();
    inSize = other.inSize;
    outSize = other.outSize;
    for (Row otherRow : other.rows) {
      final var r = addTableRow();
      r.copyFrom(otherRow);
    }
  }

  public void resize(int newInSize, int newOutSize) {
    inSize = newInSize;
    outSize = newOutSize;
    for (Row r : rows) r.truncate(inSize, outSize);
  }

  public int inSize() {
    return inSize;
  }

  public int outSize() {
    return outSize;
  }

  public void setInSize(int sz) {
    resize(sz, outSize);
  }

  public void setOutSize(int sz) {
    resize(inSize, sz);
  }

  public Row addTableRow() {
    final var r = new Row(inSize, outSize);
    rows.add(r);
    return r;
  }

  public void deleteTableRow(Row row) {
    rows.remove(row);
  }

  @Override
  public String toString() {
    return toStandardString();
  }

  public String toStandardString() {
    final var ret = new StringBuilder();
    for (final var r : rows) ret.append(r.toStandardString()).append("\n");
    return ret.toString();
  }

  public static PlaTable parse(String str) {
    PlaTable tt = null;
    for (final var line : str.split("\n")) {
      try {
        tt = parseOneLine(tt, line);
      } catch (IOException e) {
        OptionPane.showMessageDialog(
            null, e.getMessage(), S.get("plaTableError"), OptionPane.ERROR_MESSAGE);
      }
    }
    if (tt == null) tt = new PlaTable(2, 2, "PLA");
    return tt;
  }

  private static PlaTable parseOneLine(PlaTable tt, String line) throws IOException {
    line = line.trim();
    final var jj = line.indexOf("#");
    String andBits, orBits, comment = "";
    if (jj >= 0) {
      comment = line.substring(jj + 1).trim();
      line = line.substring(0, jj).trim();
    }
    if (line.equals("")) return tt;
    final var ii = line.indexOf(" ");
    if (ii <= 0) throw new IOException(S.get("plaRowMissingOutputError", "" + line));
    andBits = line.substring(0, ii).trim();
    orBits = line.substring(ii + 1).trim();
    if (tt == null) tt = new PlaTable(andBits.length(), orBits.length(), "PLA");
    else if (andBits.length() != tt.inSize)
      throw new IOException(S.get("plaRowExactInBitError", "" + line, "" +  tt.inSize));
    else if (orBits.length() != tt.outSize)
      throw new IOException(S.get("plaRowExactOutBitError", "" + line, "" + tt.outSize));
    final var r = tt.addTableRow();
    for (var i = 0; i < andBits.length(); i++) {
      final var s = andBits.charAt(i);
      if (s != ONE && s != ZERO && s != DONTCARE)
        throw new IOException(S.get("plaInvalideInputBitError", "" + line, "" + s));
      r.inBits[andBits.length() - i - 1] = s;
    }
    for (var i = 0; i < orBits.length(); i++) {
      final var s = orBits.charAt(i);
      if (s != ONE && s != ZERO)
        throw new IOException(S.get("plaInvalideOutputBitError", "" + line, "" + s));
      r.outBits[orBits.length() - i - 1] = s;
    }
    r.comment = comment;
    return tt;
  }

  public static PlaTable parse(File src) throws IOException {
    BufferedReader in;
    try {
      in = new BufferedReader(new FileReader(src));
    } catch (IOException e) {
      throw new IOException(S.get("plaFileOpenError"));
    }
    PlaTable tt = null;
    try {
      var line = in.readLine();
      while (line != null) {
        tt = parseOneLine(tt, line);
        line = in.readLine();
      }
    } finally {
      try {
        in.close();
      } catch (IOException ignored) {
      }
    }
    if (tt == null) throw new IOException(S.get("plaFileIoException"));
    return tt;
  }

  public void save(File dst) throws IOException {
    FileWriter out;
    try {
      out = new FileWriter(dst);
    } catch (IOException e) {
      throw new IOException(S.get("plaFileCreateError"));
    }
    try {
      out.write("# Logisim PLA program table\n");
      out.write(toStandardString());
    } finally {
      try {
        out.close();
      } catch (IOException ignored) {
      }
    }
  }

  private static final char ONE = '1';
  private static final char ZERO = '0';
  private static final char DONTCARE = 'x';

  public static class Row {
    public char[] inBits;
    public char[] outBits;
    private String comment = "";

    public Row(int inSize, int outSize) {
      inBits = new char[inSize];
      outBits = new char[outSize];
      for (var i = 0; i < inSize; i++) inBits[i] = ZERO;
      for (var i = 0; i < outSize; i++) outBits[i] = ZERO;
    }

    public void copyFrom(Row other) {
      System.arraycopy(other.inBits, 0, inBits, 0, inBits.length);
      System.arraycopy(other.outBits, 0, outBits, 0, outBits.length);
      comment = other.comment;
    }

    public char changeInBit(int i) {
      if (inBits[i] == ZERO) inBits[i] = ONE;
      else if (inBits[i] == ONE) inBits[i] = DONTCARE;
      else inBits[i] = ZERO;
      return inBits[i];
    }

    public char changeOutBit(int i) {
      if (outBits[i] == ZERO) outBits[i] = ONE;
      else if (outBits[i] == ONE) outBits[i] = ZERO;
      return outBits[i];
    }

    void truncate(int newInSize, int newOutSize) {
      inBits = truncate(inBits, newInSize);
      outBits = truncate(outBits, newOutSize);
    }

    static char[] truncate(char[] b, int n) {
      if (b.length == n) return b;
      final var a = new char[n];
      for (var i = 0; i < n && i < b.length; i++) a[i] = b[i];
      for (var i = b.length; i < n; i++) a[i] = ZERO;
      return a;
    }

    @Override
    public String toString() {
      return toStandardString();
    }

    public String toStandardString() {
      final var i = new StringBuilder();
      for (char inBit : inBits) i.insert(0, inBit);
      final var o = new StringBuilder();
      for (char outBit : outBits) o.insert(0, outBit);
      var ret = i + " " + o;
      if (!comment.trim().equals("")) ret += " # " + comment.trim();
      return ret;
    }

    boolean matches(long input) {
      for (final var bit : inBits) {
        long b = input & 1;
        if ((bit == ONE && b != 1) || (bit == ZERO && b != 0)) return false;
        input = (input >> 1);
      }
      return true;
    }

    long getOutput() {
      long out = 0;
      long bit = 1;
      for (char c : outBits) {
        if (c == ONE) out |= bit;
        bit = bit << 1;
      }
      return out;
    }
  }

  public long valueFor(long input) {
    for (final var row : rows) if (row.matches(input)) return row.getOutput();
    return 0;
  }

  public String commentFor(long input) {
    for (final var row : rows) if (row.matches(input)) return row.comment;
    return "n/a";
  }

  public static class EditorDialog extends JDialog implements JInputDialog {
    private static final long serialVersionUID = 1L;

    private static final float smallFont = 9.5f;
    private static final float tinyFont = 8.8f;
    private final HeaderPanel hdrPanel;
    private final TablePanel ttPanel;
    private final JPanel ttScrollPanel;
    private PlaTable oldTable, newTable;
    private final BoundedRangeModel vScrollModel;

    public EditorDialog(Frame parent) {
      super(parent, S.get("plaEditorTitle"), true);
      setResizable(true);
      final var cPane = super.getContentPane();
      cPane.setLayout(new BorderLayout(5, 5));

      hdrPanel = new HeaderPanel();
      // Give header a vertical (but invisible) vertical scroll bar, to help
      // align with the lower panel.
      final var header =
          new JScrollPane(
              hdrPanel,
              JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      header
          .getVerticalScrollBar()
          .setUI(
              new BasicScrollBarUI() {
                @Override
                protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}

                @Override
                protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {}

                @Override
                protected JButton createIncreaseButton(int orientation) {
                  return createZeroButton();
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                  return createZeroButton();
                }

                private JButton createZeroButton() {
                  final var jbutton = new JButton();
                  jbutton.setPreferredSize(new Dimension(0, 0));
                  jbutton.setMinimumSize(new Dimension(0, 0));
                  jbutton.setMaximumSize(new Dimension(0, 0));
                  return jbutton;
                }
              });

      ttPanel = new TablePanel();
      ttScrollPanel = new JPanel();
      ttScrollPanel.add(ttPanel, BorderLayout.CENTER);
      final var table =
          new JScrollPane(
              ttScrollPanel,
              JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      header.getHorizontalScrollBar().setModel(table.getHorizontalScrollBar().getModel());
      vScrollModel = table.getVerticalScrollBar().getModel();

      cPane.add(header, "North");
      cPane.add(table, "Center");
      cPane.add(new ButtonPanel(this), "South");
      cPane.setPreferredSize(
          new Dimension(AppPreferences.getScaled(440), AppPreferences.getScaled(360)));
      setMinimumSize(new Dimension(AppPreferences.getScaled(300), AppPreferences.getScaled(200)));
      setLocationRelativeTo(parent);
    }

    @Override
    public void setValue(Object o) {
      if (!(o instanceof PlaTable)) return;
      oldTable = (PlaTable) o;
      newTable = new PlaTable(oldTable);
      reset(true);
    }

    @Override
    public Object getValue() {
      return oldTable;
    }

    void reset(boolean resize) {
      hdrPanel.reset();
      ttPanel.reset();
      if (resize) {
        final var d = hdrPanel.getPreferredSize();
        final var w = (int) d.getWidth() + 50;
        final var h = (int) d.getHeight() + 20 * newTable.rows.size() + 140;
        final var ww = Math.max(Math.min(w, 800), 300);
        final var hh = Math.max(Math.min(h, 500), 200);
        repack(new Dimension(AppPreferences.getScaled(ww), AppPreferences.getScaled(hh)));
      }
    }

    void repack(Dimension prefSize) {
      Dimension d = (prefSize != null ? prefSize : getSize());
      setMinimumSize(d);
      setMaximumSize(d);
      setPreferredSize(d);
      pack();
      // setSize(d);
      setMinimumSize(new Dimension(AppPreferences.getScaled(300), AppPreferences.getScaled(200)));
      setMaximumSize(null);
    }

    void close(boolean ok) {
      if (ok) oldTable.copyFrom(newTable);
      setVisible(false);
    }

    static String normalizeName(String s) {
      if (s == null) return "pla.txt";
      s = s.trim();
      s = s.replace("[^a-zA-Z0-9().-]", " ");
      s = s.replace("\\s+", "_");
      if (s.equals("") || s.equals("_")) return "pla.txt";
      return s + ".txt";
    }

    void read() {
      final var chooser = JFileChoosers.create();
      chooser.setSelectedFile(new File(normalizeName(oldTable.label)));
      chooser.setDialogTitle(S.get("plaLoadDialogTitle"));
      chooser.setFileFilter(Loader.TXT_FILTER);
      final var choice = chooser.showOpenDialog(null);
      if (choice == JFileChooser.APPROVE_OPTION) {
        final var f = chooser.getSelectedFile();
        try {
          final var loaded = parse(f);
          newTable.copyFrom(loaded);
          reset(false);
        } catch (IOException e) {
          OptionPane.showMessageDialog(
              null, e.getMessage(), S.get("plaLoadErrorTitle"), OptionPane.ERROR_MESSAGE);
        }
      }
    }

    void write() {
      final var chooser = JFileChoosers.create();
      chooser.setSelectedFile(new File(normalizeName(oldTable.label)));
      chooser.setDialogTitle(S.get("plaSaveDialogTitle"));
      chooser.setFileFilter(Loader.TXT_FILTER);
      final var choice = chooser.showSaveDialog(null);
      if (choice == JFileChooser.APPROVE_OPTION) {
        final var f = chooser.getSelectedFile();
        try {
          newTable.save(f);
        } catch (IOException e) {
          OptionPane.showMessageDialog(
              null, e.getMessage(), S.get("plaSaveErrorTitle"), OptionPane.ERROR_MESSAGE);
        }
      }
    }

    class ButtonPanel extends JPanel {
      private static final long serialVersionUID = 1L;

      public ButtonPanel(JDialog parent) {
        final var write = new JButton(S.get("plaExportButton"));
        write.addActionListener(e -> write());
        add(write);

        final var read = new JButton(S.get("plaImportButton"));
        read.addActionListener(e -> read());
        add(read);

        final var ok = new JButton(S.get("plaOKButton"));
        ok.addActionListener(e -> close(true));
        parent.getRootPane().setDefaultButton(ok);
        add(ok);

        final var cancel = new JButton(S.get("plaCancelButton"));
        cancel.addActionListener(e -> close(false));
        add(cancel);

        parent
            .getRootPane()
            .registerKeyboardAction(
                e -> close(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
      }
    }

    class HeaderPanel extends JPanel {
      private static final long serialVersionUID = 1L;

      HeaderPanel() {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      }

      void reset() {
        removeAll();
        add(new TopLabelPanel(newTable.inSize, newTable.outSize));
        add(new TopNumberPanel(newTable.inSize, newTable.outSize));
        pack();
      }
    }

    class TablePanel extends JPanel {
      private static final long serialVersionUID = 1L;

      TablePanel() {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      }

      void reset() {
        removeAll();
        for (final var r : newTable.rows) add(new RowPanel(r));
        add(new InsertRowPanel());
        add(Box.createVerticalGlue());
      }

      void addRow() {
        final var prevSize = EditorDialog.this.getSize();
        add(new RowPanel(newTable.addTableRow()), getComponentCount() - 2);
        repack(prevSize);
        vScrollModel.setValue(vScrollModel.getMaximum());
      }

      void deleteRow(RowPanel rp) {
        newTable.deleteTableRow(rp.row);
        this.remove(rp);
        repack(null);
      }

      class RowPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Row row;

        RowPanel(Row r) {
          super(new FlowLayout(FlowLayout.CENTER, 0, 0));
          this.row = r;

          final var rm = new JButton(S.get("plaRemoveButton"));
          rm.setFont(AppPreferences.getScaledFont(rm.getFont().deriveFont(smallFont)));
          rm.addActionListener(e -> deleteRow(RowPanel.this));
          rm.setMargin(new Insets(0, 0, 0, 0));
          rm.setPreferredSize(
              new Dimension(AppPreferences.getScaled(75), AppPreferences.getScaled(17)));
          add(rm);

          int inSz = row.inBits.length;
          int outSz = row.outBits.length;

          final var layout = new GridLayout(1, 1 + inSz + 1 + Math.max(outSz, 2) + 1);
          layout.setHgap(BUTTON_HGAP);
          final var bitPanel = new JPanel(layout);

          bitPanel.add(new Box(BoxLayout.X_AXIS));

          for (var i = inSz - 1; i >= 0; i--) {
            final var ii = i;
            bitPanel.add(
                new BitStateButton(row.inBits[ii]) {
                  private static final long serialVersionUID = 1L;

                  @Override
                  public char clicked() {
                    return row.changeInBit(ii);
                  }
                });
          }

          bitPanel.add(new Box(BoxLayout.X_AXIS));

          for (var i = outSz; i < 2; i++) bitPanel.add(new Box(BoxLayout.X_AXIS));
          for (var i = outSz - 1; i >= 0; i--) {
            final var ii = i;
            bitPanel.add(
                new BitStateButton(row.outBits[ii]) {
                  private static final long serialVersionUID = 1L;

                  @Override
                  public char clicked() {
                    return row.changeOutBit(ii);
                  }
                });
          }

          bitPanel.add(new Box(BoxLayout.X_AXIS));

          add(bitPanel);

          final var txt = new JTextField(null, row.comment, 10);
          txt.addKeyListener(
              new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                  row.comment = txt.getText();
                }
              });
          txt.setPreferredSize(
              new Dimension(AppPreferences.getScaled(125), AppPreferences.getScaled(20)));
          add(txt);
          pack();
          setMaximumSize(getPreferredSize());
          add(
              Box.createRigidArea(
                  new Dimension(
                      AppPreferences.getScaled(4),
                      AppPreferences.getScaled(
                          20)))); // prevents txt from disappearing when window is narrow
        }
      }

      class InsertRowPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public InsertRowPanel() {
          super(new FlowLayout(FlowLayout.CENTER));
          final var more = new JButton(S.get("plaAddRowButton"));
          more.setFont(AppPreferences.getScaledFont(more.getFont().deriveFont(smallFont)));
          more.addActionListener(e -> addRow());
          more.setMargin(new Insets(1, 20, 1, 20));
          add(more);
        }
      }
    }

    class TopNumberPanel extends JPanel {
      private static final long serialVersionUID = 1L;

      TopNumberPanel(int inSz, int outSz) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        add(
            Box.createRigidArea(
                new Dimension(
                    AppPreferences.getScaled(75 + BS),
                    AppPreferences.getScaled(15)))); // space for remove button
        final var dim = new Dimension(AppPreferences.getScaled(BS), AppPreferences.getScaled(15));

        for (var i = inSz - 1; i >= 0; i--) {
          final var l = new JLabel("" + i, SwingConstants.CENTER);
          l.setFont(AppPreferences.getScaledFont(l.getFont().deriveFont(tinyFont)));
          l.setPreferredSize(dim);
          add(l);
        }

        add(Box.createRigidArea(dim));

        for (var i = outSz; i < 2; i++) add(Box.createRigidArea(dim));
        for (var i = outSz - 1; i >= 0; i--) {
          final var l = new JLabel("" + i, SwingConstants.CENTER);
          l.setFont(AppPreferences.getScaledFont(l.getFont().deriveFont(tinyFont)));
          l.setPreferredSize(dim);
          add(l);
        }

        add(Box.createRigidArea(dim));

        final var c = new JLabel(S.get("plaCommentsLabel"));
        c.setFont(AppPreferences.getScaledFont(c.getFont().deriveFont(smallFont)));
        c.setPreferredSize(
            new Dimension(
                AppPreferences.getScaled(125 - 8), AppPreferences.getScaled(15))); // mystery 8
        add(c);

        pack();
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
      }
    }

    class TopLabelPanel extends JPanel {
      private static final long serialVersionUID = 1L;

      TopLabelPanel(int inSz, int outSz) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        add(
            Box.createRigidArea(
                new Dimension(
                    AppPreferences.getScaled(75 + BS - Math.max(3 - inSz, 0) * BS),
                    AppPreferences.getScaled(15)))); // space for remove button

        final var i = new JLabel(S.get("plaInputLabel"), SwingConstants.RIGHT);
        i.setFont(AppPreferences.getScaledFont(i.getFont().deriveFont(smallFont)));
        i.setPreferredSize(
            new Dimension(
                AppPreferences.getScaled(Math.max(inSz, 3) * BS), AppPreferences.getScaled(15)));
        add(i);

        final var o = new JLabel(S.get("plaOutputLabel"), SwingConstants.RIGHT);
        o.setFont(AppPreferences.getScaledFont(o.getFont().deriveFont(smallFont)));
        o.setPreferredSize(
            new Dimension(
                AppPreferences.getScaled(Math.max(outSz + 1, 3) * BS),
                AppPreferences.getScaled(15)));
        add(o);

        add(
            Box.createRigidArea(
                new Dimension(
                    AppPreferences.getScaled(BS + 125 - 8),
                    AppPreferences.getScaled(15)))); // space for comment, mystery 8

        pack();
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
      }
    }

    private static final int BS = 18;
    private static final int BUTTON_HGAP = 2;
    private static final int EDGE_THICKNESS = 2;
    private static final Border STD_BORDER = BorderFactory.createEtchedBorder();
    private static final Border CLICK_BORDER =
        BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    private static final Dimension BUTTON_SIZE =
        new Dimension(
            AppPreferences.getScaled(BS - 2 * EDGE_THICKNESS - BUTTON_HGAP),
            AppPreferences.getScaled(BS - 2 * EDGE_THICKNESS));

    abstract static class BitStateButton extends JPanel {
      private static final long serialVersionUID = 1L;
      private final JLabel text;

      public abstract char clicked();

      BitStateButton(char s) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        setBorder(STD_BORDER);
        text = new JLabel("" + s, SwingConstants.CENTER);
        text.setPreferredSize(BUTTON_SIZE);
        add(text);
        addMouseListener(
            new MouseAdapter() {
              @Override
              public void mousePressed(MouseEvent e) {
                setBorder(CLICK_BORDER);
              }

              @Override
              public void mouseReleased(MouseEvent e) {
                setBorder(STD_BORDER);
              }

              @Override
              public void mouseClicked(MouseEvent e) {
                text.setText("" + clicked());
              }
            });
      }
    }
  }
}
