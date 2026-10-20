/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;
import java.util.StringTokenizer;

class TableTabClip implements ClipboardOwner {
  private static class Data implements Transferable, Serializable {
    private static final long serialVersionUID = 1L;
    private final String[] headers;
    private final String[][] contents;

    Data(String[] headers, String[][] contents) {
      this.headers = headers;
      this.contents = contents;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (flavor == binaryFlavor) {
        return this;
      } else if (flavor == DataFlavor.stringFlavor) {
        final var buf = new StringBuilder();
        for (var i = 0; i < headers.length; i++) {
          buf.append(headers[i]);
          buf.append(i == headers.length - 1 ? '\n' : '\t');
        }
        for (final var content : contents) {
          for (var j = 0; j < content.length; j++) {
            buf.append(content[j]);
            buf.append(j == content.length - 1 ? '\n' : '\t');
          }
        }
        return buf.toString();
      } else {
        throw new UnsupportedFlavorException(flavor);
      }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] {binaryFlavor, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor == binaryFlavor || flavor == DataFlavor.stringFlavor;
    }
  }

  private static final DataFlavor binaryFlavor = new DataFlavor(Data.class, "Binary data");

  private final TableTab table;

  TableTabClip(TableTab table) {
    this.table = table;
  }

  public boolean canPaste() {
    final var clip = table.getToolkit().getSystemClipboard();
    final var xfer = clip.getContents(this);
    return xfer.isDataFlavorSupported(binaryFlavor);
  }

  public void copy() {
    final var s = table.getCaret().getSelection();
    if (s.width <= 0 || s.height <= 0) return;
    final var t = table.getTruthTable();
    final var inputs = t.getInputColumnCount();
    final var header = new String[s.width];
    for (var c = s.x; c < s.x + s.width; c++) {
      if (c < inputs) {
        header[c - s.x] = t.getInputHeader(c);
      } else {
        header[c - s.x] = t.getOutputHeader(c - inputs);
      }
    }
    final var contents = new String[s.height][s.width];
    for (var r = s.y; r < s.y + s.height; r++) {
      for (var c = s.x; c < s.x + s.width; c++) {
        if (c < inputs) {
          contents[r - s.y][c - s.x] = t.getInputEntry(r, c).getDescription();
        } else {
          contents[r - s.y][c - s.x] = t.getOutputEntry(r, c - inputs).getDescription();
        }
      }
    }

    Clipboard clip = table.getToolkit().getSystemClipboard();
    clip.setContents(new Data(header, contents), this);
  }

  @Override
  public void lostOwnership(Clipboard clip, Transferable transfer) {
    // dummy
  }

  public void paste() {
    final var clip = table.getToolkit().getSystemClipboard();
    Transferable xfer;
    try {
      xfer = clip.getContents(this);
    } catch (IllegalStateException | ArrayIndexOutOfBoundsException t) {
      // I don't know - the above was observed to throw an odd
      // ArrayIndexOutOfBounds
      // exception on a Linux computer using Sun's Java 5 JVM
      OptionPane.showMessageDialog(
          table.getRootPane(),
          S.get("clipPasteSupportedError"),
          S.get("clipPasteErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return;
    }
    Entry[][] entries;
    if (xfer.isDataFlavorSupported(binaryFlavor)) {
      try {
        final var data = (Data) xfer.getTransferData(binaryFlavor);
        entries = new Entry[data.contents.length][];
        for (int i = 0; i < entries.length; i++) {
          final var row = new Entry[data.contents[i].length];
          for (var j = 0; j < row.length; j++) {
            row[j] = Entry.parse(data.contents[i][j]);
          }
          entries[i] = row;
        }
      } catch (UnsupportedFlavorException | IOException e) {
        return;
      }
    } else if (xfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        final var buf = (String) xfer.getTransferData(DataFlavor.stringFlavor);
        final var lines = new StringTokenizer(buf, "\r\n");
        String first;
        if (!lines.hasMoreTokens()) return;
        first = lines.nextToken();
        var toks = new StringTokenizer(first, "\t,");
        final var headers = new String[toks.countTokens()];
        final var firstEntries = new Entry[headers.length];
        var allParsed = true;
        for (int i = 0; toks.hasMoreTokens(); i++) {
          headers[i] = toks.nextToken();
          firstEntries[i] = Entry.parse(headers[i]);
          allParsed = allParsed && firstEntries[i] != null;
        }
        var rows = lines.countTokens();
        if (allParsed) rows++;
        entries = new Entry[rows][];
        var cur = 0;
        if (allParsed) {
          entries[0] = firstEntries;
          cur++;
        }
        while (lines.hasMoreTokens()) {
          toks = new StringTokenizer(lines.nextToken(), "\t");
          final var ents = new Entry[toks.countTokens()];
          for (var i = 0; toks.hasMoreTokens(); i++) {
            ents[i] = Entry.parse(toks.nextToken());
          }
          entries[cur] = ents;
          cur++;
        }
      } catch (UnsupportedFlavorException | IOException e) {
        return;
      }
    } else {
      OptionPane.showMessageDialog(
          table.getRootPane(),
          S.get("clipPasteSupportedError"),
          S.get("clipPasteErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return;
    }
    final var s = table.getCaret().getSelection();
    if (s.width <= 0 || s.height <= 0) return;
    final var model = table.getTruthTable();
    final var rows = model.getVisibleRowCount();
    final var inputs = model.getInputColumnCount();
    final var outputs = model.getOutputColumnCount();
    if (s.width == 1 && s.height == 1) {
      if (s.y + entries.length > rows || s.x + entries[0].length > inputs + outputs) {
        OptionPane.showMessageDialog(
            table.getRootPane(),
            S.get("clipPasteEndError"),
            S.get("clipPasteErrorTitle"),
            OptionPane.ERROR_MESSAGE);
        return;
      }
    } else {
      if (s.height != entries.length || s.width != entries[0].length) {
        OptionPane.showMessageDialog(
            table.getRootPane(),
            S.get("clipPasteSizeError"),
            S.get("clipPasteErrorTitle"),
            OptionPane.ERROR_MESSAGE);
        return;
      }
    }
    for (var r = 0; r < entries.length; r++) {
      for (var c = 0; c < entries[0].length; c++) {
        if (s.x + c >= inputs) {
          model.setVisibleOutputEntry(s.y + r, s.x + c - inputs, entries[r][c]);
        }
      }
    }
  }
}
