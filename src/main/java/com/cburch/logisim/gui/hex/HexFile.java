/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.hex;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.Main;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.OutputStreamBinarySanitizer;
import com.cburch.logisim.util.OutputStreamEscaper;
import com.cburch.logisim.util.TextLineNumber;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexFile {
  // "v2.0 raw" -- A sequence of space-sparated hex numbers (without "0x"),
  // each optionally prefixed with a decimal count and a "*", spread out on as
  // many lines as desired. Anything on a line after a "#" is ignored. The parser
  // is a bit forgiving, and actually accepts any text that can get past java's
  // Long.parseUnsignedLong(text, 16) function. Each number corresponds to one location in
  // the resulting memory. For example, if the memory uses words that are 10 bits
  // wide, each number should be in the range 0-1023, and if the memory words are
  // only 2 bits wide, each number should be in the range 0-3.
  // The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex bytes plain [big-endian|little-endian]" -- The same format as used
  // by "xxd --plain". That is, a sequence of hex digits, spread out with as much
  // whitespace desired. Anything on a line after a "#" is ignored. Actually, the
  // parser is a bit forgiving and will ignore "0x" prefixes. The hex digits are
  // assembled into a stream of bits and these are then broken up into word-sized
  // chunks and used to fill the locations of memory. For example, if the memory
  // uses words that are 10 bits wide, the first two hex digits, and part of the
  // third, are used to fill the first memory location, but if the memory words
  // are only 2 bits wide, then the first hex digit will fill up two locations in
  // the memory. In some oddly-sized memory cases, a few leftover zero bits in the
  // file, if needed, will be silently ignored. If memory words are larger than 8
  // bits, then by default the ordering will be "big-endian", so the first byte in
  // the file ends up in the most significant 8 bits of the memory location. The
  // optional "little-endian" tag would instead cause the first byte in the file
  // to end up in the least significant 8 bits of the memory location. For memory
  // words of size 8 bits or less, the endianness is irrelevant.
  // The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex bytes addressed [big-endian|little-endian]" -- The same format as
  // used by "xxd" (but preferably without the ascii duplication). That is, a
  // sequence of lines starting with an address in hex (without "0x"), followed by
  // an optional ":", then a sequence of hex digits with optional
  // single-whitespace separators. Note that the addresses here are *byte*
  // addresses. The addresses need not be in increasing order, and you can leave
  // gaps, which will be filled with zeros. Anything on a line after a "#" is
  // ignored. Anything after two consecutive whitespaces will also be ignored,
  // which makes this format compatible with the default output format of "xxd".
  // Additionally, the parser is a bit forgiving and will ignore 0x prefixes on
  // both addresses and data. As with "v3.0 hex plain", the bits specified in the
  // file are considered a single large stream of bits, and taken in word-sized
  // chunks to fill memory. The meaning of the "little-endian" and "big-endian"
  // tags is the same as above, with the default ordering again being "big-endian"
  // The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex words plain" -- Similar to "v3.0 hex bytes plain ...", except that
  // whitespace is significant. Each whitespace-separated group of hex digits is
  // taken as a single word and put into a single location in memory. Endianness
  // is irrelevant here. As before, "0x" prefixes are ignored, "#" can be used for
  // comments. As with "v2.0 raw", if memory words are 10 bits wide, then each
  // group of hex digits should range from 000 to 3ff, and if memory words are 2
  // bits wide, then each group should range from 0 to 3.
  // The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex words addressed" -- Similar to "v3.0 hex bytes addressed ...",
  // except that whitespace is significant, and the addresses are *word*
  // addresses. Each group of hex digits corresponds to one word of memory. Again,
  // comments using "#" are allowed, anything after two consecutive whitespaces
  // will be ignored, and "0x" prefixes are ignored. As with with "v2.0 raw", if
  // memory words are 10 bits wide, then each group of hex digits should range
  // from 000 to 3ff, and if memory words are 2 bits wide, then each group should
  // range from 0 to 3. The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex bytes" -- Either of the "v3.0 bytes..." styles above. If the file
  // contains ":", then "v3.0 hex bytes addressed" will be used, otherwise "v3.0
  // hex bytes plain" will be used.
  // The file must be encoded using utf-8 or plain ascii.

  // "v3.0 hex words" -- Either of the "v3.0 words..." styles above. If the file
  // contains ":", then "v3.0 hex bytes addressed" will be used, otherwise "v3.0
  // hex bytes plain" will be used.
  // The file must be encoded using utf-8 or plain ascii.

  // Binary [big-endian|little-endian] -- Raw binary format. One byte per byte. No
  // header, no comments, no endoding/decoding. The bytes are packed into the
  // memory words in either big-endian or little-endian order. For example, if
  // memory words are 20 bits each, for big-endian the first byte of the file will
  // be put into the most signficant 8 bits of the memory word, the next byte will
  // go into the middle 8 bits of the memory word, and the most significant 4 bits
  // of the third byte of the file will go into the bottom 4 bits of the memory
  // word.

  // Escaped Ascii -- One byte per byte, except that bytes that are not regular
  // printable ascii must be escaped using simple or hex escape sequences. Any
  // non-printable ascii found in the file will be silently ignored. This means
  // that newlines, tabs, unusual unicode characters, control characters, and any
  // other bytes outside the range 0x20 - 0x7E, will be ignored. If you want a
  // newline or tab, use "\n" or "\t", etc. If you want a zero byte, use "\0" or
  // "\x00". Otherwise, no header, no comments, no encoding/decoding, no fuss. I
  // guess you could make weird emoji comments if you want, since they will be
  // ignored like all other non-ascii printable bytes.

  // Big-endian conversion works like this.
  // If we have 3-bit words, then the first 7 bytes
  // corresponds to around 7*8/3 = 18.6 words like so:
  // bytes: |1......|2......|3......|4......|5......|6......|7......|
  // words: |1.|2.|3.|4.|5.|6.|7.|8.|9.|A.|B.|C.|D.|E.|F.|0.|1.|2.|
  // If instead have 9-bit words, then the first 7 bytes
  // corresponds to around 7*8/9 = 6.2 words like so:
  // bytes: |1......|2......|3......|4......|5......|6......|7......|
  // words: |1.......|2.......|3.......|4.......|5.......|6.......|
  // And if we have 16-bit words, then the first 7 bytes
  // corresponds to around 7*8/16 = 3.5 words like so:
  // bytes: |1......|2......|3......|4......|5......|6......|7......|
  // words: |1..............|2..............|3..............|

  // Little-endian conversion works like this.
  // If we have 3-bit words, then the first 7 bytes
  // corresponds to around 7*8/3 = 18.6 words like so:
  // bytes: |7......|6......|5......|4......|3......|2......|1......|
  // words:   |2.|1.|0.|F.|E.|D.|C.|B.|A.|9.|8.|7.|6.|5.|4.|3.|2.|1.|
  // If instead have 9-bit words, then the first 7 bytes
  // corresponds to around 7*8/9 = 6.2 words like so:
  // bytes: |7......|6......|5......|4......|3......|2......|1......|
  // words:   |6.......|5.......|4.......|3.......|2.......|1.......|
  // And if we have 16-bit words, then the first 7 bytes
  // corresponds to around 7*8/16 = 3.5 words like so:
  // bytes: |7......|6......|5......|4......|3......|2......|1......|
  // words:         |3..............|2..............|1..............|

  // Error: Format tag 'nibbles' not recognized.
  //
  // Please select an appropriate file format to load this file
  // into memory:
  //
  //   [ ] v2.0 raw
  //   [x] v3.0 hex
  //           [x] words       [ ] bytes            (size)
  //           [x] addressed   [ ] plain            (style)
  //           [x] big-endian  [ ] little-endian    (endian)
  //   [ ] Binary
  //           [x] big-endian  [ ] little-endian    (endian)
  //   [ ] ASCII with C-style escapes
  //           [x] big-endian  [ ] little-endian    (endian)
  //
  //  Note: There were 7 errors encountered while decoding in
  //    this format, starting with:
  //       msg
  //       msg
  //       msg ...
  //
  //  Original file:                             Memory preview:
  //  580 characters                             4096 words, 12 bits each
  //  +---------------------------------------+  +-------------------+
  //  | v3.0 hex nibbles addressed big-endian |  | 001 002 003 004   |
  //  | 0x0000: 001 002 003 # some data       |  | 005 006 006 007   |
  //  | 0x0003: 004 005 006 # more data       |  | 008 009 00a 00b   |
  //  |  ...                                  |  |  ...              |
  //  +---------------------------------------+  +-------------------+
  static final int MAX_PREVIEW_SIZE = 10 * 1024; // 10KB max size for displaying files
  private static final Logger logger = LoggerFactory.getLogger(HexFile.class);
  private static final String autoFormat = "Any data file (auto-detects format)";
  private static final String[] formatDescriptions = {
    "v3.0 hex words addressed", // header = desc
    "v3.0 hex words plain", // header = desc
    "v3.0 hex bytes addressed big-endian", // header = desc
    "v3.0 hex bytes addressed little-endian", // header = desc
    "v3.0 hex bytes plain big-endian", // header = desc
    "v3.0 hex bytes plain little-endian", // header = desc
    "v2.0 raw (run-length-endcoded hex words)", // header = "v2.0 raw"
    "Binary data big-endian", // no header
    "Binary data little-endian", // no header
    "ASCII bytes, with escapes, big-endian", // no header
    "ASCII bytes, with escapes, little-endian" // no header
  };

  private HexFile() {}

  public static void open(
      MemContents dst,
      Frame parent, // for window positioning
      Project proj,
      Instance instance) { // for recent file access
    final var mem = (instance == null) ? null : (Mem) instance.getFactory();
    final var recent = getRecent(proj, mem, instance);

    final var chooser = createFileOpenChooser(recent);
    chooser.setDialogTitle(S.get("ramLoadDialogTitle"));
    final var choice = chooser.showOpenDialog(parent);
    if (choice == JFileChooser.APPROVE_OPTION) {
      final var f = chooser.getSelectedFile();
      try {
        open(dst, f);
        mem.setCurrentImage(instance, f);
      } catch (IOException e) {
        OptionPane.showMessageDialog(parent, e.getMessage(), S.get("ramLoadErrorTitle"), OptionPane.ERROR_MESSAGE);
      }
    }
  }

  public static boolean open(MemContents dst, File src) throws IOException {
    return open(dst, src, null);
  }

  private static boolean open(MemContents dst, File src, String desc) throws IOException {
    final var in = BufferedLineReader.forFile(src);
    try {
      final var r = new HexReader(in, dst.getLogLength(), dst.getValueWidth());
      MemContents loaded;
      if (desc == null) {
        loaded = r.detectFormatAndDecode();
      } else {
        r.parseFormat(desc);
        loaded = r.decodeOrWarn();
      }
      if (loaded == null) return false;
      dst.copyFrom(0, loaded, 0, (int) (loaded.getLastOffset() + 1));
      return true;
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {
        // do nothing
      }
    }
  }

  public static ParseResult parseFromClipboard(String src, int addrSize, int wordSize) throws IOException {
    return parse(true, src, "v3.0 hex plain words", addrSize, wordSize);
  }

  public static MemContents parseFromCircFile(String src, int addrSize, int wordSize) throws IOException {
    return parse(false, src, "v2.0 raw", addrSize, wordSize).model;
  }

  private static ParseResult parse(boolean interactive, String src, String desc, int addrSize, int wordSize) throws IOException {
    final var in = BufferedLineReader.forString(src);
    try {
      final var r = new HexReader(in, addrSize, wordSize);
      r.parseFormat(desc);
      final var loaded = interactive ? r.decodeOrWarn() : r.decode();
      if (loaded == null) throw new IOException("Could not parse memory image data.");
      return new ParseResult(loaded, (int) (r.memMaxAddr + 1));
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {
        // do nothing
      }
    }
  }

  public static void save(
      MemContents src,
      Frame parent, // for window positioning
      Project proj,
      Instance instance) { // for recent file access
    final var S = com.cburch.logisim.std.Strings.S;
    final var mem = instance == null ? null : (Mem) instance.getFactory();
    final var recent = getRecent(proj, mem, instance);

    final var chooser = createFileSaveChooser(recent, src);
    chooser.setDialogTitle(S.get("ramSaveDialogTitle"));
    final var choice = chooser.showSaveDialog(parent);
    if (choice == JFileChooser.APPROVE_OPTION) {
      final var f = chooser.getSelectedFile();
      if (f.exists()) {
        final var confirm =
            OptionPane.showConfirmDialog(
                parent,
                S.get("confirmOverwriteMessage", f.getName()),
                S.get("confirmOverwriteTitle"),
                OptionPane.YES_NO_OPTION);
        if (confirm != OptionPane.YES_OPTION) return;
      }
      try {
        save(f, src, chooser.getFileFilter().getDescription());
        if (mem != null) mem.setCurrentImage(instance, f);
      } catch (IOException e) {
        OptionPane.showMessageDialog(parent, e.getMessage(), S.get("ramSaveErrorTitle"), OptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static void save(File f, MemContents src, String desc) throws IOException {
    OutputStream out;
    try {
      out = new FileOutputStream(f);
    } catch (IOException e) {
      throw new IOException(S.get("hexFileOpenError", e.getMessage()));
    }
    out.write(headerForFormat(desc).getBytes(StandardCharsets.UTF_8));
    new HexWriter(out, src, desc).save();
  }

  public static String saveToString(MemContents src) {
    return saveToString(src, null, -1);
  }

  // No header is output. As a special, if desc is null, v2.0 raw will be used.
  // For binary format, this uses binary sanitizer (non-ascii printable bytes
  // will appear as unicode error placeholder chars), otherwise only plain
  // ascii will be output, with whitespace preserved.
  private static String saveToString(MemContents src, String desc, int limit) {
    try {
      final var out = new StringWriter();
      OutputStream stream;
      if (desc == null) desc = "v2.0 raw";
      if (desc.startsWith("Binary")) stream = new OutputStreamBinarySanitizer(out);
      else stream = new OutputStreamEscaper(out, true, 0);
      final var w = new HexWriter(stream, src, desc);
      if (limit > 0 && limit - 1 < w.memEnd) w.memEnd = limit - 1;
      w.save();
      return out.toString();
    } catch (IOException e) {
      // should never happen
      logger.error("HexFile.saveToString: {}", e.getMessage());
      throw new IllegalStateException("HexFile.saveToString: " + e.getMessage());
    }
  }

  private static FileFilter getFilter(String desc) {
    return new FileFilter() {
      @Override
      public String getDescription() {
        return desc;
      }

      @Override
      public boolean accept(File f) {
        return true;
      }
    };
  }

  private static String headerForFormat(String desc) {
    if (desc.startsWith("Binary") || desc.startsWith("ASCII")) return "";
    else if (desc.startsWith("v2.0 raw")) return "v2.0 raw\n";
    else return desc + "\n";
  }

  private static File getRecent(Project proj, Mem mem, Instance instance) {
    var recent = mem == null ? null : mem.getCurrentImage(instance);
    if (recent == null) {
      final var lf = (proj == null ? null : proj.getLogisimFile());
      final var ld = (lf == null ? null : lf.getLoader());
      recent = (ld == null ? null : ld.getCurrentDirectory());
    }
    return recent;
  }

  private static JFileChooser createFileSaveChooser(File lastFile, MemContents preview) {
    final var chooser = createFileChooser(lastFile, false);
    chooser.setAccessory(new Preview(chooser, preview));
    return chooser;
  }

  private static JFileChooser createFileOpenChooser(File lastFile) {
    return createFileChooser(lastFile, true);
  }

  private static JFileChooser createFileChooser(File lastFile, boolean auto) {
    final var chooser = JFileChoosers.createSelected(lastFile);
    if (auto) {
      chooser.addChoosableFileFilter(getFilter(autoFormat));
    } else {
      for (final var desc : formatDescriptions) {
        chooser.addChoosableFileFilter(getFilter(desc));
      }
    }
    chooser.setAcceptAllFileFilterUsed(false);
    return chooser;
  }

  private static MemContents compare(
      boolean autodetect,
      String desc,
      File tmp,
      int addrSize,
      int wordSize,
      HashMap<Long, Long> vals)
      throws Exception {
    final var dst = MemContents.create(addrSize, wordSize);
    if (desc.startsWith("Binary") || desc.startsWith("ASCII") || !autodetect) {
      // these can't be auto-detected
      if (!open(dst, tmp, desc)) {
        System.out.printf("Failed to load: %s\n", tmp);
        System.exit(0);
        return null;
      }
    } else {
      // auto-detect should figure out the correct format
      if (!open(dst, tmp)) {
        System.out.printf("Failed to load: %s\n", tmp);
        System.exit(1);
        return null;
      }
    }

    var errs = 0;
    var memEnd = dst.getLastOffset();
    for (long a = 0; a < memEnd; a++) {
      var v = vals.getOrDefault(a, 0L);
      var v2 = dst.get(a);
      if (v2 != v) {
        if (errs == 0) System.out.printf("  Decoding: %s\n", tmp);
        errs++;
        if (errs < 10) System.out.printf("  mem[0x%x] = 0x%x (but should be 0x%x)\n", a, v2, v);
      }
    }
    if (errs > 0) {
      System.out.printf("-- Found %d errors in: %s (%s)\n", errs, tmp, desc);
      System.exit(1);
      return null;
    }
    return dst;
  }

  private static void randomTests(java.util.Random rng) throws Exception {
    Main.headless = true;
    var addrSize = rng.nextInt(14) + 1;
    var wordSize = rng.nextInt(64) + 1;
    System.out.printf("Testing addrSize = %d, wordSize = %d\n", addrSize, wordSize);

    var m = MemContents.create(addrSize, wordSize);

    final var vals = new HashMap<Long, Long>();
    final var count = rng.nextInt(1 << addrSize);
    final var mask = (1L << wordSize) - 1;
    for (var i = 0; i < count; i++) {
      final long a = rng.nextInt(1 << addrSize);
      final var v = (rng.nextLong() & mask);
      vals.put(a, v);
      m.set(a, v);
    }
    final var orig = File.createTempFile("hexfile-orig-", ".dat");
    save(orig, m, formatDescriptions[0]);
    for (var i = 0; i < 30; i++) {
      final var desc = formatDescriptions[rng.nextInt(formatDescriptions.length)];
      final var tmp = File.createTempFile("hexfile-" + i + "-", ".dat");
      save(tmp, m, desc);

      final var dst = compare(true, desc, tmp, addrSize, wordSize, vals);

      if (desc.startsWith("Binary")) {
        final var endian = desc.endsWith("big-endian") ? "big-endian" : "little-endian";

        final var other = new File(tmp + ".xxd");
        Runtime.getRuntime().exec(String.format("xxd %s %s", tmp, other)).waitFor();
        compare(false, "v3.0 hex bytes addressed " + endian, other, addrSize, wordSize, vals);

        final var plain = new File(tmp + ".xxd-plain");
        Runtime.getRuntime().exec(String.format("xxd -p %s %s", tmp, plain)).waitFor();
        compare(false, "v3.0 hex bytes plain " + endian, plain, addrSize, wordSize, vals);
      }

      if (i % 3 == 0 && dst != null) m = dst;
    }
  }

  public static void main(String[] args) {
    try {
      java.util.Random rng = new java.util.Random(1234L);
      if (args.length == 0) {
        randomTests(rng);
      } else if (args.length == 1) {
        final var n = Integer.parseInt(args[0]);
        for (var i = 0; i < n; i++) randomTests(rng);
      } else {
        final var addrSize = Integer.parseInt(args[0]);
        final var wordSize = Integer.parseInt(args[1]);

        final var m = MemContents.create(addrSize, wordSize);

        // open file
        File f;
        if (args.length >= 3) {
          f = new File(args[2]);
        } else {
          final var chooser = createFileOpenChooser(null);
          chooser.setDialogTitle("Open Data File");
          final var choice = chooser.showSaveDialog(null);
          if (choice != JFileChooser.APPROVE_OPTION) {
            System.out.println("cancelled");
            return;
          }
          f = chooser.getSelectedFile();
        }
        final var b = open(m, f);
        if (!b) {
          System.out.println("cancelled");
          return;
        }

        // save file
        final var chooser = createFileSaveChooser(null, m);
        chooser.setDialogTitle("Save Data File");
        final var choice = chooser.showOpenDialog(null);
        if (choice != JFileChooser.APPROVE_OPTION) {
          System.out.println("cancelled");
          return;
        }
        f = chooser.getSelectedFile();
        save(f, m, chooser.getFileFilter().getDescription());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static int scaled(int i) {
    return AppPreferences.getScaled(i);
  }

  static class HexFormatDialog extends JDialogOk {
    private static final long serialVersionUID = 1L;
    JRadioButton raw;
    JRadioButton hex;
    JRadioButton bin;
    JRadioButton asc;

    JCheckBox hexWords;
    JCheckBox hexBytes;
    JCheckBox hexAddr;
    JCheckBox hexPlain;
    JCheckBox hexAuto;
    JCheckBox hexBig;
    JCheckBox hexLittle;
    JCheckBox binBig;
    JCheckBox binLittle;
    JCheckBox ascBig;
    JCheckBox ascLittle;

    JTextArea warnings;
    JTextArea previewMem;
    JTextArea originalTxt;

    JLabel previewHdr;
    JLabel originalHdr;
    JTabbedPane tabs;
    HexReader reader;
    boolean value = false;

    public HexFormatDialog(String msg, HexReader reader) {
      super(S.get("hexFormatTitle"));
      configure(msg, reader);
    }

    private void configure(String msg, HexReader reader) {
      this.reader = reader;

      final var p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
      final var scaledTen = scaled(10);
      p.setBorder(BorderFactory.createEmptyBorder(scaledTen, scaledTen, 0, scaledTen));
      var m =
          new JLabel(
              "<html>"
                  + msg
                  + "<br><br>"
                  + "Please select an appropriate file format to load"
                  + " this file into memory:</html>");
      final var f = m.getFont();
      m.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
      m.setAlignmentX(CENTER_ALIGNMENT);
      m.setBorder(BorderFactory.createEmptyBorder(0, 0, scaledTen, 0));
      p.add(m);

      final var grid = new GridBagLayout();
      final var pos = new GridBagConstraints();
      final var opts = new JPanel(grid);

      pos.gridheight = 1;
      pos.weighty = 0.0;
      pos.weightx = 0.0;
      pos.anchor = GridBagConstraints.WEST;

      pos.gridy = 0;
      pos.gridx = 0;
      pos.gridwidth = 4;
      raw = new JRadioButton("v2.0 raw", this.reader.tagged("radix", "raw"));
      grid.setConstraints(raw, pos);
      opts.add(raw);

      pos.gridy = 1;
      pos.gridx = 0;
      pos.gridwidth = 4;
      hex = new JRadioButton("v3.0 hex", this.reader.tagged("radix", "hex"));
      grid.setConstraints(hex, pos);
      opts.add(hex);

      pos.gridy = 2;
      pos.gridx = 0;
      pos.gridwidth = 1;
      Component strut = Box.createHorizontalStrut(scaled(20));
      grid.setConstraints(strut, pos);
      opts.add(strut);

      pos.gridx = 1;
      hexWords = new JCheckBox("words", this.reader.taggedOrUnset("size", "words"));
      grid.setConstraints(hexWords, pos);
      opts.add(hexWords);

      pos.gridx = 2;
      hexBytes = new JCheckBox("bytes", this.reader.tagged("size", "bytes"));
      grid.setConstraints(hexBytes, pos);
      opts.add(hexBytes);

      pos.gridy = 3;
      pos.gridx = 1;
      hexAuto = new JCheckBox("auto", !this.reader.tags.containsKey("style"));
      grid.setConstraints(hexAuto, pos);
      opts.add(hexAuto);
      pos.gridx = 2;
      hexAddr = new JCheckBox("addressed", this.reader.tagged("style", "addressed"));
      grid.setConstraints(hexAddr, pos);
      opts.add(hexAddr);
      pos.gridx = 3;
      hexPlain = new JCheckBox("plain", this.reader.tagged("style", "plain"));
      grid.setConstraints(hexPlain, pos);
      opts.add(hexPlain);

      pos.gridy = 4;
      pos.gridx = 1;
      hexBig = new JCheckBox("big-endian", this.reader.bigEndian());
      grid.setConstraints(hexBig, pos);
      opts.add(hexBig);
      pos.gridx = 2;
      hexLittle = new JCheckBox("little-endian", !this.reader.bigEndian());
      grid.setConstraints(hexLittle, pos);
      opts.add(hexLittle);

      pos.gridy = 5;
      pos.gridx = 0;
      pos.gridwidth = 4;
      bin = new JRadioButton("Binary", this.reader.taggedOrUnset("radix", "binary"));
      grid.setConstraints(bin, pos);
      opts.add(bin);

      pos.gridy = 6;
      pos.gridx = 1;
      pos.gridwidth = 1;
      binBig = new JCheckBox("big-endian", this.reader.bigEndian());
      grid.setConstraints(binBig, pos);
      opts.add(binBig);
      pos.gridx = 2;
      binLittle = new JCheckBox("little-endian", !this.reader.bigEndian());
      grid.setConstraints(binLittle, pos);
      opts.add(binLittle);

      pos.gridy = 7;
      pos.gridx = 0;
      pos.gridwidth = 4;
      asc = new JRadioButton("ASCII with C-style escapes", this.reader.tagged("radix", "ascii"));
      grid.setConstraints(asc, pos);
      opts.add(asc);

      pos.gridy = 8;
      pos.gridx = 1;
      pos.gridwidth = 1;
      ascBig = new JCheckBox("big-endian", this.reader.bigEndian());
      grid.setConstraints(ascBig, pos);
      opts.add(ascBig);
      pos.gridx = 2;
      ascLittle = new JCheckBox("little-endian", !this.reader.bigEndian());
      grid.setConstraints(ascLittle, pos);
      opts.add(ascLittle);

      final var radix = new ButtonGroup();
      radix.add(raw);
      radix.add(hex);
      radix.add(bin);
      radix.add(asc);

      final var hs = new ButtonGroup();
      hs.add(hexWords);
      hs.add(hexBytes);

      final var hy = new ButtonGroup();
      hy.add(hexPlain);
      hy.add(hexAddr);
      hy.add(hexAuto);

      final var he = new ButtonGroup();
      he.add(hexBig);
      he.add(hexLittle);

      final var be = new ButtonGroup();
      be.add(binBig);
      be.add(binLittle);

      final var ae = new ButtonGroup();
      ae.add(ascBig);
      ae.add(ascLittle);

      previewHdr = new JLabel("words...");
      previewMem = new JTextArea();
      previewMem.setEditable(false);
      previewMem.setFont(new Font("monospaced", Font.PLAIN, scaledTen));
      final var preview = new JPanel();
      preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
      preview.add(previewHdr);
      preview.add(new JScrollPane(previewMem));

      originalHdr = new JLabel(this.reader.in.byteLength() + " bytes");
      originalTxt = new JTextArea();
      originalTxt.setEditable(false);
      originalTxt.setFont(new Font("monospaced", Font.PLAIN, scaledTen));
      final var original = new JPanel();
      original.setLayout(new BoxLayout(original, BoxLayout.Y_AXIS));
      original.add(originalHdr);
      final var scroller = new JScrollPane(originalTxt);
      scroller.setRowHeaderView(new TextLineNumber(originalTxt));
      original.add(scroller);

      try {
        final var buf = new char[1024];
        this.reader.in.reset();
        var n = this.reader.in.readUtf8(buf, 0, 1024);
        var count = 0;
        if (n < 0) {
          originalTxt.setText("(error reading data)");
        } else {
          final var b = new StringWriter();
          do {
            b.write(buf, 0, n);
            count += n;
            if (count >= MAX_PREVIEW_SIZE) {
              b.write("..\n(rest of fole omitted)\n");
              break;
            }
            n = this.reader.in.readUtf8(buf, 0, 1024);
          } while (n > 0);
          originalTxt.setText(b.toString());
        }
      } catch (IOException e) {
        try {
          final var buf = new byte[1024];
          this.reader.in.reset();
          var n = this.reader.in.readBytes(buf, 0, 1024);
          var count = 0;
          if (n < 0) {
            originalTxt.setText("(error reading data)");
          } else {
            final var b = new StringWriter();
            OutputStreamBinarySanitizer sanitizer = new OutputStreamBinarySanitizer(b);
            do {
              sanitizer.write(buf, 0, n);
              count += n;
              if (count >= MAX_PREVIEW_SIZE) {
                b.write("..\n(rest of fole omitted)\n");
                break;
              }
              n = this.reader.in.readBytes(buf, 0, 1024);
            } while (n > 0);
            sanitizer.flush();
            sanitizer.close();
            originalTxt.setText(b.toString());
          }
        } catch (Exception e2) {
          originalTxt.setText("(error reading data)");
        }
      }
      originalTxt.setCaretPosition(0);

      tabs = new JTabbedPane();
      tabs.setBorder(BorderFactory.createEmptyBorder(0, scaledTen, 0, 0));
      tabs.setFont(new Font("Dialog", Font.BOLD, scaled(9)));
      tabs.addTab("Decoded", preview);
      tabs.addTab("Original", original);

      final var split = new JPanel(new BorderLayout());
      final var optp = new JPanel();
      optp.setLayout(new BoxLayout(optp, BoxLayout.X_AXIS));
      opts.setAlignmentY(TOP_ALIGNMENT);
      optp.add(opts);
      split.add(optp, BorderLayout.WEST);
      split.add(tabs, BorderLayout.CENTER);
      split.setBorder(BorderFactory.createEmptyBorder(0, 0, scaledTen, 0));
      p.add(split);

      warnings = new JTextArea();
      warnings.setEditable(false);
      p.add(
          new JScrollPane(warnings) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMinimumSize() {
              Dimension d = super.getMaximumSize();
              d.height = scaled(60);
              return d;
            }

            @Override
            public Dimension getPreferredSize() {
              Dimension d = super.getPreferredSize();
              d.height = scaled(80);
              return d;
            }

            @Override
            public Dimension getMaximumSize() {
              Dimension d = super.getMaximumSize();
              d.height = scaled(120);
              return d;
            }
          });

      final var listener = new MyListener();
      raw.addActionListener(listener);
      hex.addActionListener(listener);
      hexWords.addActionListener(listener);
      hexBytes.addActionListener(listener);
      hexAuto.addActionListener(listener);
      hexAddr.addActionListener(listener);
      hexPlain.addActionListener(listener);
      hexBig.addActionListener(listener);
      hexLittle.addActionListener(listener);
      bin.addActionListener(listener);
      binBig.addActionListener(listener);
      binLittle.addActionListener(listener);
      asc.addActionListener(listener);
      ascBig.addActionListener(listener);
      ascLittle.addActionListener(listener);

      listener.actionPerformed(null); // initialize preview, in case needed

      getContentPane().add(p, BorderLayout.CENTER);
      setMinimumSize(new Dimension(scaled(600), scaled(400)));
      opts.setMaximumSize(opts.getMinimumSize());
      pack();
    }

    void setWarnings() {
      final var s = new StringWriter();
      if (reader.numWarnings == 0) {
        s.write("No errors encountered decoding with this format.");
      } else if (reader.numWarnings == 1) {
        s.write("There was one error encountered decoding with this format:\n");
      } else {
        s.write(
            "There were "
                + reader.numWarnings
                + " errors encountered decoding with this format:\n");
      }
      s.write(reader.warnings.toString());
      warnings.setText(s.toString());
      warnings.setCaretPosition(0);
    }

    void setEnables() {
      hexWords.setEnabled(hex.isSelected());
      hexBytes.setEnabled(hex.isSelected());
      hexAuto.setEnabled(hex.isSelected());
      hexAddr.setEnabled(hex.isSelected());
      hexPlain.setEnabled(hex.isSelected());
      hexBig.setEnabled(hex.isSelected() && hexBytes.isSelected());
      hexLittle.setEnabled(hex.isSelected() && hexBytes.isSelected());
      binBig.setEnabled(bin.isSelected());
      binLittle.setEnabled(bin.isSelected());
      ascBig.setEnabled(asc.isSelected());
      ascLittle.setEnabled(asc.isSelected());
    }

    void setPreview() {
      final var n = reader.decodedWordCount;
      previewHdr.setText(String.format("decoded %d of %d words, %d bits each", n, reader.memEnd + 1, reader.memWidth));
      if (n > 0) previewMem.setText(saveToString(reader.dst, "v3.0 hex words addressed", n));
      else previewMem.setText("");
      previewMem.setCaretPosition(0);
    }

    public boolean ok() {
      return value;
    }

    @Override
    public void okClicked() {
      value = true;
    }

    @Override
    public void cancelClicked() {
      value = false;
    }

    private class MyListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent event) {
        setEnables();
        reader.tags.clear();
        if (raw.isSelected()) {
          reader.tags.put("version", "v2.0");
          reader.tags.put("radix", "raw");
        } else if (hex.isSelected()) {
          reader.tags.put("version", "v3.0");
          reader.tags.put("radix", "hex");
          reader.tags.put("size", hexWords.isSelected() ? "words" : "bytes");
          if (hexPlain.isSelected()) reader.tags.put("style", "plain");
          else if (hexAddr.isSelected()) reader.tags.put("style", "addressed");
          if (hexBytes.isSelected())
            reader.tags.put("endian", hexBig.isSelected() ? "big-endian" : "little-endian");
        } else if (asc.isSelected()) {
          reader.tags.put("version", "v3.0");
          reader.tags.put("radix", "ascii");
          reader.tags.put("endian", ascBig.isSelected() ? "big-endian" : "little-endian");
        } else {
          reader.tags.put("version", "v3.0");
          reader.tags.put("radix", "binary");
          reader.tags.put("endian", binBig.isSelected() ? "big-endian" : "little-endian");
        }
        try {
          reader.decode();
        } catch (IOException e) {
          reader.warn(e.getMessage());
        }
        setPreview();
        setWarnings();
      }
    }
  }

  static class FormatOptions {
    final HashMap<String, String> tags = new HashMap<>();
    // "version" -->  "v2.0", "v3.0",
    // "radix" --> "hex", "raw", "binary" (or null), or "ascii"
    // "size" --> "bytes", "words"
    // "style" --> "plain", "addressed"
    // "endian" --> "little-endian", "big-endian"

    FormatOptions() {}

    FormatOptions(String desc) {
      parseFormat(desc);
    }

    void parseFormat(String desc) {
      tags.clear();
      if (desc.startsWith("Binary")) {
        boolean le = desc.endsWith("little-endian");
        tags.put("version", "v3.0");
        tags.put("radix", "binary");
        tags.put("endian", le ? "little-endian" : "big-endian");
        tags.put("size", "bytes");
        tags.put("style", "plain");
      } else if (desc.startsWith("ASCII")) {
        boolean le = desc.endsWith("little-endian");
        tags.put("version", "v3.0");
        tags.put("radix", "ascii");
        tags.put("endian", le ? "little-endian" : "big-endian");
        tags.put("size", "bytes");
        tags.put("style", "plain");
      } else if (desc.startsWith("v2.0 raw")) {
        tags.put("version", "v2.0");
        tags.put("radix", "raw");
        tags.put("size", "words");
        tags.put("style", "rle");
      } else {
        String msg = parseHeader(desc);
        if (msg != null) throw new IllegalArgumentException(msg + ": " + desc);
      }
    }

    String parseHeader(String hdr) {
      tags.clear();
      final var t = hdr.split("\\s+");
      if (t.length < 1) return "File does not contain any header, and appears to contain only whitespace.";
      if (!t[0].equalsIgnoreCase("v2.0") && !t[0].equalsIgnoreCase("v3.0")) return "Hex file header not recognized";

      // plausible header line with version number
      tags.put("version", t[0]);

      String err = null;
      for (var i = 1; i < t.length; i++) {
        final var tag = t[i];

        final var key = switch (tag.toLowerCase()) {
          case "hex", "raw" -> "radix";
          case "bytes", "words" -> "size";
          case "plain", "addressed" -> "style";
          case "little-endian", "big-endian" -> "endian";
          default -> null;
        };

        if (key == null)
          err = (err != null) ? (err) : ("File header tag '" + tag + "' not recognized.");
        else if (tags.containsKey(key) && tags.get(key).equalsIgnoreCase(tag))
          err = (err != null) ? (err) : ("File header tag '" + tag + "' appears more than once.");
        else if (tags.containsKey(key))
          err =
              err != null
                  ? err
                  : ("File header tag '" + tag + "' conflicts with '" + tags.get(key) + "'.");
        else tags.put(key, tag);
      }
      return err;
    }

    boolean tagged(String key, String val) {
      return tags.containsKey(key) && tags.get(key).equalsIgnoreCase(val);
    }

    boolean taggedOrUnset(String key, String val) {
      return !tags.containsKey(key) || tags.get(key).equalsIgnoreCase(val);
    }

    boolean bigEndian() {
      return taggedOrUnset("endian", "big-endian");
    }

    protected String endian() {
      return bigEndian() ? "big-endian" : "little-endian";
    }

    protected String headerToString() {
      if (tagged("radix", "raw")) return "v2.0 raw";
      else if (taggedOrUnset("radix", "binary")) return "v3.0 binary " + endian();
      else if (tagged("radix", "ascii")) return "v3.0 ascii " + endian();
      else if (tagged("size", "words"))
        return "v3.0 hex words" + (tags.containsKey("style") ? (" " + tags.get("style")) : "");
      else
        return "v3.0 hex bytes"
            + (tags.containsKey("style") ? (" " + tags.get("style")) : "")
            + " "
            + endian();
    }
  }

  private static class HexReader extends FormatOptions {

    private final long[] data = new long[4096];
    final BufferedLineReader in;
    final MemContents dst;
    int decodedWordCount;
    final StringWriter warnings = new StringWriter();
    int numWarnings = 0;
    final byte[] bytes = new byte[4096];
    int bLen;
    long memAddr;
    long memMaxAddr;
    long memAddrFrac; // portion of the next address already set
    long memEnd;
    int memWidth;
    boolean bigEndian;
    private int curLineNo;
    private String curLine;
    private String[] curWords;
    private int curWordIdx;
    private boolean skipDoubleSpaces;
    private long rleCount;
    private long rleValue;

    protected HexReader(BufferedLineReader in, int addrBits, int width) {
      this.in = in;
      this.dst = MemContents.create(addrBits, width);
    }

    ////////////////////////////////////////////////////////
    // old "v2.0 raw" run-length-endoded hex nonsense format

    static int hex2int(int c) throws NumberFormatException { // byte, char
      if (c >= '0' && c <= '9') return c - '0';
      else if (c >= 'a' && c <= 'f') return 0xa + (c - 'a');
      else if (c >= 'A' && c <= 'F') return 0xA + (c - 'A');
      else throw new NumberFormatException("Invalid hex digit: " + (char) c);
    }

    static long hex2ulong(String s) {
      long val = 0;
      var n = s.length();
      for (var i = 0; i < n; i++) {
        final var d = hex2int(s.charAt(i));
        val = (val << 4) + d;
      }
      return val;
    }

    void warn(String msg, Object... args) {
      if (numWarnings > 0) warnings.write("\n");
      if (curLineNo > 0) warnings.write("Line " + curLineNo + ": ");
      warnings.write(String.format(msg, args));
      numWarnings++;
    }

    protected MemContents warnAndAsk(String errmsg) {
      if (Main.headless) {
        System.out.println(errmsg);
        System.out.println("Warnings:\n" + warnings.toString());
        return null;
      }
      final var d = new HexFormatDialog(errmsg, this);
      d.setVisible(true);
      if (!d.ok()) return null;
      return dst;
    }

    protected MemContents detectFormatAndDecode() throws IOException {
      if (in.byteLength() == 0) throw new IOException("File contains no data.");

      var hdr = in.readLine();
      while (hdr != null && (hdr = hdr.trim()).length() == 0) hdr = in.readLine();

      if (hdr == null) return warnAndAsk("File does not contain any header, and appears to contain only whitespace.");

      final var err = parseHeader(hdr);
      if (err != null) return warnAndAsk(err);

      if (!tags.containsKey("radix")) return warnAndAsk("Incomplete file header.");

      if (tagged("radix", "hex") && !tags.containsKey("size"))
        return warnAndAsk("File header should specify either 'bytes' or 'words'.");

      return decodeOrWarn();
    }

    protected MemContents decode() throws IOException {
      reset();
      if (taggedOrUnset("radix", "binary")) decodeBinary();
      else if (tagged("radix", "ascii")) decodeEscapedAscii();
      else if (tagged("radix", "raw")) decodeRaw();
      else if (tagged("style", "plain")) decodeHexPlain();
      else if (tagged("style", "addressed")) decodeHexAddressed();
      else decodeHexAuto();
      return dst;
    }

    ////////////////////////////////////////////////////////
    // new "v3.0 hex" styles

    protected MemContents decodeOrWarn() throws IOException {
      decode();
      if (tagged("size", "bytes") && (memMaxAddr - memEnd) * memWidth >= 8)
        warn("File contained %f extra bytes.", (memMaxAddr - memEnd) * memWidth / 8.0);
      else if (!tagged("size", "bytes") && (memMaxAddr - memEnd) > 0)
        warn("File contained %d extra words.", memMaxAddr - memEnd);
      if (numWarnings > 0) {
        return warnAndAsk(
            "Decoding with format '"
                + headerToString()
                + "'"
                + " produced "
                + numWarnings
                + " warnings.");
      }
      return dst;
    }

    protected void reset() throws IOException {
      in.reset();
      dst.clear();
      curLineNo = 0;
      decodedWordCount = 0;
      warnings.getBuffer().setLength(0);
      numWarnings = 0;
      curLine = null;
      curWords = null;
      curWordIdx = 0;
      skipDoubleSpaces = false;
      bLen = 0;
      memAddr = 0;
      memAddrFrac = 0;
      memMaxAddr = 0;
      memEnd = dst.getLastOffset();
      memWidth = dst.getWidth();
      bigEndian = bigEndian();
    }

    private void findNonemptyLine(boolean skipHeader) throws IOException {
      curLine = null;
      curWords = null;
      curWordIdx = 0;
      for (String line = in.readLine(); line != null; line = in.readLine()) {
        curLineNo++;
        int index = line.indexOf("#");
        if (index >= 0) line = line.substring(0, index);
        if (skipHeader) {
          line = line.trim();
          if (line.length() == 0) continue;
          skipHeader = false;
          if (line.charAt(0) == 'v') continue;
        }
        int idx;
        if (skipDoubleSpaces && (idx = line.indexOf("  ")) >= 0) line = line.substring(0, idx);
        line = line.trim();
        if (line.length() == 0) continue;
        String[] t = line.split("\\s+");
        if (t.length > 0) {
          curLine = line;
          curWords = t;
          return;
        }
      }
    }

    private String nextWord() throws IOException {
      return hasNextWord() ? curWords[curWordIdx++] : null;
    }

    public boolean hasNextWord() throws IOException {
      if (curWords == null || curWordIdx >= curWords.length) findNonemptyLine(false);
      return curWords != null;
    }

    private long[] subarray(long[] a, int n) {
      if (n >= a.length) return a;
      long[] s = new long[n];
      System.arraycopy(a, 0, s, 0, n);
      return s;
    }

    void decodeRaw() throws IOException {
      rleCount = 0;
      rleValue = 0;
      long offs = 0;
      findNonemptyLine(true);
      while (rleHasNextVals()) {
        long[] v = rleNextVals();
        long end = offs + v.length - 1;
        if (end > memMaxAddr) memMaxAddr = end;
        if (end > memEnd) {
          if (offs <= memEnd) {
            int n = (int) (memEnd - offs + 1);
            dst.set(offs, subarray(data, n));
          }
        } else {
          dst.set(offs, v);
        }
        offs += v.length;
        decodedWordCount += v.length;
      }
    }

    public boolean rleHasNextVals() throws IOException {
      return rleCount > 0 || hasNextWord();
    }

    public long[] rleNextVals() throws IOException {
      int pos = 0;
      if (rleCount > 0) {
        int n = (int) Math.min(data.length - pos, rleCount);
        if (n == 1) {
          data[pos] = (int) rleValue;
          pos++;
          rleCount--;
        } else {
          Arrays.fill(data, pos, pos + n, rleValue);
          pos += n;
          rleCount -= n;
        }
      }
      if (pos >= data.length) return data;
      for (String word = nextWord(); word != null; word = nextWord()) {
        int star = word.indexOf("*");
        if (star == 0) {
          warn("Run-length encoded token \"%s\" missing count, use \"count*data\" instead.", word);
          continue;
        } else if (star == word.length() - 1) {
          warn(
              "Run-length encoded token \"%s\" missing hex data, use \"count*data\" instead.",
              word);
          continue;
        }
        final var hexWord = star < 0 ? word : word.substring(star + 1);
        try {
          rleValue = Long.parseUnsignedLong(hexWord, 16);
        } catch (NumberFormatException e) {
          try {
            rleValue = Long.parseLong(hexWord, 16);
          } catch (NumberFormatException f) {
            warn("\"%s\" is not valid hex data.", hexWord);
            continue;
          }
        }
        if (star < 0) {
          rleCount = 1;
        } else {
          try {
            rleCount = Long.parseUnsignedLong(word.substring(0, star));
          } catch (NumberFormatException e) {
            warn("\"%s\" is not valid (base-10 decimal) count.", word.substring(0, star));
            continue;
          }
        }
        int n = (int) Math.min(data.length - pos, rleCount);
        Arrays.fill(data, pos, pos + n, rleValue);
        pos += n;
        rleCount -= n;
        if (pos >= data.length) return data;
      }
      return subarray(data, pos);
    }

    long get(long addr) {
      return addr > memEnd ? 0L : (dst.get(memAddr) & 0xffffffffL);
    }

    void set(long addr, long val) {
      decodedWordCount++;
      if (addr > memMaxAddr) memMaxAddr = addr;
      if (addr <= memEnd) dst.set(addr, val);
      // else
      //   System.out.printf("warn: overflow addr = %x\n", addr);
    }

    boolean deliver() {
      if (bigEndian) {
        long val = get(memAddr) >>> (memWidth - memAddrFrac);
        long nbits = memAddrFrac;
        for (int i = 0; i < bLen; i++) {
          val = (val << 8) | (0xffL & bytes[i]);
          nbits += 8;
          while (nbits >= memWidth) {
            // dst.set() will mask off all but the desired lower bits
            set(memAddr++, (val >>> (nbits - memWidth)));
            nbits -= memWidth;
          }
        }
        // there may be a few (nbits) bits of a partial word leftover
        if (nbits > 0) {
          set(memAddr, dst.get(memAddr) | (val << (memWidth - nbits)));
          memAddrFrac = nbits;
          decodedWordCount--;
        } else {
          memAddrFrac = 0;
        }
      } else {
        long val = get(memAddr);
        long nbits = memAddrFrac;
        for (int i = 0; i < bLen; i++) {
          val = val | ((0xffL & bytes[i]) << nbits);
          nbits += 8;
          while (nbits >= memWidth) {
            // dst.set() will mask off all but the desired lower bits
            set(memAddr++, val);
            nbits -= memWidth;
            val = val >>> memWidth;
          }
        }
        // there may be a few (nbits) bits of a partial word leftover
        if (nbits > 0) {
          set(memAddr, get(memAddr) | val);
          memAddrFrac = nbits;
          decodedWordCount--;
        } else {
          memAddrFrac = 0;
        }
      }
      bLen = 0; // all bytes consumed and put into dst
      if (memAddr > memEnd + 100) {
        warn("Halting decoding early, since plenty of words have been decoded.");
        return false;
      }
      return true;
    }

    void decodeHexAuto() throws IOException {
      findNonemptyLine(true);
      if (curLine == null) {
        // File appears to contain no data, only maybe some whitespace,
        // comments, and/or a header line.
        return;
      } else if (curLine.contains(":")) {
        reset();
        decodeHexAddressed();
      } else {
        reset();
        decodeHexPlain();
      }
    }

    void decodeHexPlain() throws IOException {
      if (tagged("size", "words")) decodeHexPlainWords();
      else decodeHexPlainBytes();
    }

    void decodeHexPlainBytes() throws IOException {
      bLen = 0;
      findNonemptyLine(true);
      boolean left = true;
      String word;
      while ((word = nextWord()) != null) {
        int i = 0;
        int n = word.length();
        if (n >= 2 && (word.startsWith("0x") || word.startsWith("0X"))) i += 2;
        for (; i < n; i++) {
          int d;
          try {
            d = hex2int(word.charAt(i));
          } catch (NumberFormatException e) {
            warn("Character '%s' is not a hex digit.", OutputStreamEscaper.escape(word.charAt(i)));
            continue;
          }
          if (left) {
            bytes[bLen++] = (byte) (d << 4);
          } else {
            bytes[bLen - 1] |= (byte) d;
          }
          left = !left;
          if (left && bLen >= 4096 && !deliver()) return;
        }
      }
      if (!left) warn("Odd number of hex digits found in file.");
      if (bLen > 0) deliver();
    }

    void decodeHexPlainWords() throws IOException {
      long offs = 0;
      findNonemptyLine(true);
      String word;
      while ((word = nextWord()) != null) {
        int i = 0;
        int n = word.length();
        if (n >= 2 && (word.startsWith("0x") || word.startsWith("0X"))) i += 2;
        long v = 0;
        for (; i < n; i++) {
          long d;
          try {
            d = hex2int(word.charAt(i));
          } catch (NumberFormatException e) {
            warn("Character '%s' is not a hex digit.", OutputStreamEscaper.escape(word.charAt(i)));
            continue;
          }
          v = (v << 4) | d;
        }
        set(offs++, v);
      }
    }

    void decodeHexAddressed() throws IOException {
      if (tagged("size", "words")) decodeHexAddressedWords();
      else decodeHexAddressedBytes();
    }

    void decodeHexAddressedBytes() throws IOException {
      // byte addressed style:
      // 0000: 00 00 00 00  [ascii] # comments
      skipDoubleSpaces = true;
      findNonemptyLine(true);
      while (curWords != null) {
        // first word should be "addr:"
        var addr = curWords[0];
        final var foundColon = addr.endsWith(":");
        final var stripOx = addr.startsWith("0x") || addr.startsWith("0X");
        if (foundColon) addr = addr.substring(stripOx ? 2 : 0, addr.length() - 1);
        else if (stripOx) addr = addr.substring(2);
        bLen = 0;
        long boffs = 0;
        try {
          boffs = hex2ulong(addr);
          if (!deliver()) return;
          memAddr = (boffs * 8) / memWidth;
          memAddrFrac = (boffs * 8) % memWidth;
        } catch (Exception e) {
          warn("\"%s\" is not a valid hex address.", addr);
          // Continue on with previous address, I guess?
        }
        int i = 1;
        int n = curWords.length;
        if (!foundColon && n >= 2 && curWords[1].equals(":")) i++;
        for (; i < n; i++) {
          final var word = curWords[i];
          boolean left = true;
          int j = 0;
          int m = word.length();
          if (word.startsWith("0x") || word.startsWith("0X")) j = 2;
          for (; j < m; j++) {
            int d;
            try {
              d = hex2int(word.charAt(i));
            } catch (NumberFormatException e) {
              warn("Character '%s' is not a hex digit.", OutputStreamEscaper.escape(word.charAt(i)));
              continue;
            }
            if (left) bytes[bLen++] = (byte) (d << 4);
            else bytes[bLen - 1] |= (byte) d;
            left = !left;
            if (left && bLen >= 4096 && !deliver()) return;
          }
          if (!left) warn("Odd number of hex digits found in line.");
        }
        if (bLen > 0 && !deliver()) return;
        findNonemptyLine(false);
      }
    }

    void decodeHexAddressedWords() throws IOException {
      // word addressed style:
      // 0000: 0000 0000 0000 0000  [ascii] # comments
      findNonemptyLine(true);
      skipDoubleSpaces = true;
      long offs = 0;
      while (curWords != null) {
        // first word should be "addr:"
        String addr = curWords[0];
        boolean foundColon = addr.endsWith(":");
        boolean stripOx = addr.startsWith("0x") || addr.startsWith("0X");
        if (foundColon) addr = addr.substring(stripOx ? 2 : 0, addr.length() - 1);
        else if (stripOx) addr = addr.substring(2);
        try {
          offs = hex2ulong(addr);
        } catch (Exception e) {
          warn("\"%s\" is not a valid hex address.", curWords[0]);
          // Continue on with previous address, I guess?
        }
        int i = 1;
        int n = curWords.length;
        if (!foundColon && n >= 2 && curWords[1].equals(":")) i++;
        for (; i < n; i++) {
          String word = curWords[i];
          if (word.startsWith("0x") || word.startsWith("0X")) word = word.substring(2);
          long val;
          try {
            val = hex2ulong(word);
          } catch (Exception e) {
            warn("Data word \"%s\" contains non-hex characters.", OutputStreamEscaper.escape(word));
            continue;
          }
          set(offs++, val);
        }
        findNonemptyLine(false);
      }
    }

    void decodeBinary() throws IOException {
      bLen = 0;
      int n = in.readBytes(bytes, 0, 4096);
      while (n > 0) {
        bLen += n;
        if (!deliver()) return;
        n = in.readBytes(bytes, bLen, 4096 - bLen);
      }
    }

    void decodeEscapedAscii() throws IOException {
      final var buf = new byte[4096];
      bLen = 0;
      var n = in.readBytes(buf, 0, 4096);
      curLineNo = 1;
      var esc = 0;
      var ehex = 0;
      while (n > 0) {
        // decode buf[] into bytes[]
        for (var i = 0; i < n; i++) {
          byte c = buf[i];
          if (c == '\n') curLineNo++;
          if (c < 0x20 || c > 0x7E) continue;
          if (esc == 3) { // backslash "x" hexdigit __
            int d;
            try {
              d = hex2int(c);
              bytes[bLen++] = (byte) (16 * ehex + d);
            } catch (NumberFormatException e) {
              warn("Invalid hex escape sequence.");
            }
            esc = 0;
          } else if (esc == 2) { // backslash "x" __ __
            int d;
            try {
              d = hex2int(c);
              ehex = d;
              esc++;
            } catch (NumberFormatException e) {
              warn("Invalid hex escape sequence.");
              esc = 0;
            }
          } else if (esc == 1 && c == 'x') {
            esc++;
          } else if (esc == 1) {
            esc = 0;
            if (c == 'n') bytes[bLen++] = 0x0a;
            else if (c == 'r') bytes[bLen++] = 0x0d;
            else if (c == 't') bytes[bLen++] = 0x09;
            else if (c == '0') bytes[bLen++] = 0x00;
            else if (c == '\\') bytes[bLen++] = 0x5c;
            else if (c == '\'') bytes[bLen++] = 0x27;
            else if (c == '\"') bytes[bLen++] = 0x22;
            else if (c == 'a') bytes[bLen++] = 0x07;
            else if (c == 'b') bytes[bLen++] = 0x08;
            else if (c == 'v') bytes[bLen++] = 0x0b;
            else if (c == 'f') bytes[bLen++] = 0x0c;
            else if (c == '?') bytes[bLen++] = 0x3f;
            else warn("Invalid ascii escape sequence.");
          } else if (c == '\\') {
            esc = 1;
          } else if (c >= 0x20 && c <= 0x7E) {
            bytes[bLen++] = c;
          } // else silently ignore
        }
        // deliver the bytes, move remaining to front of array
        if (!deliver()) return;
        // get more data, but not too much that bytes[] might overflow
        n = in.readBytes(buf, 0, 4096 - bLen);
      }
      if (esc != 0) warn("Truncated escape sequence at end of file.");
    }
  }

  public static class ParseResult {
    public final MemContents model;
    public final int numWords;

    ParseResult(MemContents m, int n) {
      model = m;
      numWords = n;
    }
  }

  private static class HexWriter extends FormatOptions {
    final MemContents src;
    final byte[] bytes = new byte[4096];
    int bLen;
    final int memWidth;
    long memAddr;
    long memEnd;
    int mAddrFrac;
    final boolean bigEndian;
    PrintWriter cOut;
    OutputStream bOut;

    HexWriter(OutputStream out, MemContents src, String desc) {
      super(desc);
      this.src = src;
      this.bOut = out;
      memEnd = src.getLastOffset();
      memWidth = src.getWidth();
      bigEndian = bigEndian();
    }

    long get(long addr) {
      return addr > memEnd ? 0L : (src.get(addr) & 0xffffffffL);
    }

    void buffer() {
      bLen = 0;
      if (bigEndian) {
        long val = 0;
        int nbits = -mAddrFrac;
        while (memAddr <= memEnd) {
          while (nbits < 8) {
            val = (val << memWidth) | get(memAddr++); // get() can go past end
            nbits += memWidth;
          }
          while (nbits >= 8) {
            bytes[bLen++] = (byte) ((val >>> (nbits - 8)) & 0xffL);
            nbits -= 8;
            if (bLen >= 4096) {
              memAddr -= ((nbits + memWidth - 1) / memWidth);
              mAddrFrac = memWidth - ((nbits + memWidth - 1) % memWidth) - 1;
              return;
            }
          }
        }
        // there may be 0 to 7 bits of a partial byte leftover
        if (memAddr <= memEnd && nbits > 0) bytes[bLen++] = (byte) ((val << (8 - nbits)) & 0xffL);
      } else {
        long val = 0;
        int nbits = -mAddrFrac;
        while (memAddr <= memEnd) {
          while (nbits < 8) {
            if (nbits < 0) {
              // can only happen on first loop
              val = get(memAddr++) >>> (-nbits);
            } else {
              // get() can go past end
              val = val | (get(memAddr++) << nbits);
            }
            nbits += memWidth;
          }
          while (nbits >= 8) {
            bytes[bLen++] = (byte) (val & 0xffL);
            val = val >>> 8;
            nbits -= 8;
            if (bLen >= 4096) {
              memAddr -= ((nbits + memWidth - 1) / memWidth);
              mAddrFrac = memWidth - ((nbits + memWidth - 1) % memWidth) - 1;
              return;
            }
          }
        }
        // there may be 0 to 7 bits of a partial byte leftover
        if (memAddr <= memEnd && nbits > 0) bytes[bLen++] = (byte) (val & ((1 << nbits) - 1));
      }
    }

    void save() throws IOException {
      try {
        if (taggedOrUnset("radix", "binary")) saveBinary();
        else if (tagged("radix", "ascii")) saveEscapedAscii();
        else if (tagged("radix", "raw")) saveRaw();
        else if (tagged("style", "plain")) saveHexPlain();
        else saveHexAddressed();
      } catch (IOException e) {
        try {
          if (cOut != null) {
            Writer o = cOut;
            cOut = null;
            o.close();
          }
        } catch (IOException ignored) {
        }
        try {
          OutputStream o = bOut;
          bOut = null;
          o.close();
        } catch (IOException ignored) {
        }
        throw new IOException(S.get("hexFileWriteError", e.getMessage()));
      } finally {
        if (cOut != null) cOut.close();
        bOut.close();
      }
    }

    void saveRaw() {
      cOut = new PrintWriter(new OutputStreamWriter(bOut));
      while (memEnd > 0 && src.get(memEnd) == 0) memEnd--;
      int tokens = 0;
      long offs = 0;
      while (offs <= memEnd) {
        long val = src.get(offs);
        long start = offs;
        offs++;
        while (offs <= memEnd && src.get(offs) == val) offs++;
        long len = offs - start;
        if (len < 4) {
          offs = start + 1;
          len = 1;
        }
        if (tokens > 0) cOut.write(tokens % 8 == 0 ? '\n' : ' ');
        if (offs != start + 1) cOut.write((offs - start) + "*");
        cOut.write(Long.toHexString(val));
        tokens++;
      }
      if (tokens > 0) cOut.write('\n');
    }

    void saveBinary() throws IOException {
      buffer();
      while (bLen > 0) {
        bOut.write(bytes, 0, bLen);
        buffer();
      }
    }

    void saveEscapedAscii() throws IOException {
      buffer();
      OutputStreamEscaper escaper = new OutputStreamEscaper(new OutputStreamWriter(bOut));
      escaper.textWidth(70);
      while (bLen > 0) {
        escaper.write(bytes, 0, bLen);
        // We could insert occational newlines, if desired,
        // but don't bother for now.
        buffer();
      }
      escaper.flush();
      escaper.close();
    }

    void saveHexPlain() {
      if (tagged("size", "words")) saveHexWords(false);
      else saveHexBytes(false);
    }

    String addrfmt(long maxAddr) {
      int w = String.format("%x", maxAddr).length();
      return "%0" + w + "x: ";
    }

    void saveHexBytes(boolean addressed) {
      cOut = new PrintWriter(new OutputStreamWriter(bOut));
      String afmt = addrfmt(memEnd);
      int col = 0;
      buffer();
      long offs = 0;
      while (bLen > 0) {
        for (int i = 0; i < bLen; i++) {
          if (col == 0 && addressed) cOut.printf(afmt, offs);
          offs++;
          byte b = bytes[i];
          cOut.printf("%02x", b); // no spaces
          col += 2;
          if (col >= 64) {
            cOut.printf("\n");
            col = 0;
          }
        }
        buffer();
      }
      if (col != 0) cOut.printf("\n");
    }

    // 00000000000000000000000000000000000000000000000000000000000000000000000000000000
    // 0 1 2 3 4 5 6 7 8 9 a b c d e f 0 1 2 3 4 5 6 7 8 9 a b c d e f
    // 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
    // 000 001 002 003 004 005 006 007 008 009 00a 00b 00c 00d 00e 00f
    // 0000 0001 0002 0003 0004 0005 0006 0007 0008 0009 000a 000b 000c 000d 000e 000f
    // 00000 00001 00002 00003 00004 00005 00006 00007
    // 000000 000001 000002 000003 000004 000005 000006 000007
    // 0000000 0000001 0000002 0000003 0000004 0000005 0000006 0000007
    // 00000000 00000001 00000002 00000003 00000004 00000005 00000006 00000007

    void saveHexWords(boolean addressed) {
      cOut = new PrintWriter(new OutputStreamWriter(bOut));
      String afmt = addrfmt(memEnd);
      int col = 0;
      int w = ((memWidth + 3) / 4);
      int ncol = (w == 1 ? 32 : w <= 4 ? 16 : 8);
      String fmt = "%0" + w + "x";
      for (int offs = 0; offs <= memEnd; offs++) {
        if (col == 0 && addressed) cOut.printf(afmt, offs); // with trailing space
        else if (col != 0) cOut.print(" "); // leading space
        cOut.printf(fmt, src.get(offs));
        col++;
        if (col >= ncol) {
          cOut.print("\n");
          col = 0;
        }
      }

      if (col != 0) cOut.printf("\n");
    }

    void saveHexAddressed() {
      if (tagged("size", "words")) saveHexWords(true);
      else saveHexBytes(true);
    }
  }

  private static class Preview extends JPanel implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;
    final JFileChooser chooser;
    final JTextArea preview;
    final MemContents memContents;

    Preview(JFileChooser chooser, MemContents memContents) {
      this.chooser = chooser;
      this.memContents = memContents;
      setLayout(new BorderLayout());

      preview = new JTextArea();
      preview.setEditable(false);
      preview.setFont(new Font("monospaced", Font.PLAIN, scaled(10)));
      JTabbedPane tabs = new JTabbedPane();
      tabs.setBorder(BorderFactory.createEmptyBorder(0, scaled(8), 0, 0));
      tabs.setFont(new Font("Dialog", Font.BOLD, scaled(9)));
      tabs.addTab("Preview", new JScrollPane(preview));
      add(tabs, BorderLayout.CENTER);

      chooser.addPropertyChangeListener(this);
      setPreferredSize(new Dimension(scaled(240), scaled(220)));

      refresh();
    }

    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      String changeName = changeEvent.getPropertyName();
      if (changeName.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
        refresh();
      }
    }

    void refresh() {
      String desc = chooser.getFileFilter().getDescription();
      String hdr = headerForFormat(desc);
      preview.setText(hdr + saveToString(memContents, desc, -1));
      preview.setCaretPosition(0);
    }
  }
}
