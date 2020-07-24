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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.gui.generic.OptionPane;

import java.awt.Rectangle;
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
    private String[] headers;
    private String[][] contents;

    Data(String[] headers, String[][] contents) {
      this.headers = headers;
      this.contents = contents;
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
      if (flavor == binaryFlavor) {
        return this;
      } else if (flavor == DataFlavor.stringFlavor) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
          buf.append(headers[i]);
          buf.append(i == headers.length - 1 ? '\n' : '\t');
        }
        for (int i = 0; i < contents.length; i++) {
          for (int j = 0; j < contents[i].length; j++) {
            buf.append(contents[i][j]);
            buf.append(j == contents[i].length - 1 ? '\n' : '\t');
          }
        }
        return buf.toString();
      } else {
        throw new UnsupportedFlavorException(flavor);
      }
    }

    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] {binaryFlavor, DataFlavor.stringFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor == binaryFlavor || flavor == DataFlavor.stringFlavor;
    }
  }

  private static final DataFlavor binaryFlavor = new DataFlavor(Data.class, "Binary data");

  private TableTab table;

  TableTabClip(TableTab table) {
    this.table = table;
  }

  public boolean canPaste() {
    Clipboard clip = table.getToolkit().getSystemClipboard();
    Transferable xfer = clip.getContents(this);
    return xfer.isDataFlavorSupported(binaryFlavor);
  }

  public void copy() {
    Rectangle s = table.getCaret().getSelection();
    if (s.width <= 0 || s.height <= 0) return;
    TruthTable t = table.getTruthTable();
    int inputs = t.getInputColumnCount();
    String[] header = new String[s.width];
    for (int c = s.x; c < s.x + s.width; c++) {
      if (c < inputs) {
        header[c - s.x] = t.getInputHeader(c);
      } else {
        header[c - s.x] = t.getOutputHeader(c - inputs);
      }
    }
    String[][] contents = new String[s.height][s.width];
    for (int r = s.y; r < s.y + s.height; r++) {
      for (int c = s.x; c < s.x + s.width; c++) {
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

  public void lostOwnership(Clipboard clip, Transferable transfer) {}

  public void paste() {
    Clipboard clip = table.getToolkit().getSystemClipboard();
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
        Data data = (Data) xfer.getTransferData(binaryFlavor);
        entries = new Entry[data.contents.length][];
        for (int i = 0; i < entries.length; i++) {
          Entry[] row = new Entry[data.contents[i].length];
          for (int j = 0; j < row.length; j++) {
            row[j] = Entry.parse(data.contents[i][j]);
          }
          entries[i] = row;
        }
      } catch (UnsupportedFlavorException e) {
        return;
      } catch (IOException e) {
        return;
      }
    } else if (xfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        String buf = (String) xfer.getTransferData(DataFlavor.stringFlavor);
        StringTokenizer lines = new StringTokenizer(buf, "\r\n");
        String first;
        if (!lines.hasMoreTokens()) return;
        first = lines.nextToken();
        StringTokenizer toks = new StringTokenizer(first, "\t,");
        String[] headers = new String[toks.countTokens()];
        Entry[] firstEntries = new Entry[headers.length];
        boolean allParsed = true;
        for (int i = 0; toks.hasMoreTokens(); i++) {
          headers[i] = toks.nextToken();
          firstEntries[i] = Entry.parse(headers[i]);
          allParsed = allParsed && firstEntries[i] != null;
        }
        int rows = lines.countTokens();
        if (allParsed) rows++;
        entries = new Entry[rows][];
        int cur = 0;
        if (allParsed) {
          entries[0] = firstEntries;
          cur++;
        }
        while (lines.hasMoreTokens()) {
          toks = new StringTokenizer(lines.nextToken(), "\t");
          Entry[] ents = new Entry[toks.countTokens()];
          for (int i = 0; toks.hasMoreTokens(); i++) {
            ents[i] = Entry.parse(toks.nextToken());
          }
          entries[cur] = ents;
          cur++;
        }
      } catch (UnsupportedFlavorException e) {
        return;
      } catch (IOException e) {
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
    Rectangle s = table.getCaret().getSelection();
    if (s.width <= 0 || s.height <= 0) return;
    TruthTable model = table.getTruthTable();
    int rows = model.getVisibleRowCount();
    int inputs = model.getInputColumnCount();
    int outputs = model.getOutputColumnCount();
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
    for (int r = 0; r < entries.length; r++) {
      for (int c = 0; c < entries[0].length; c++) {
        if (s.x + c >= inputs) {
          model.setVisibleOutputEntry(s.y + r, s.x + c - inputs, entries[r][c]);
        }
      }
    }
  }
}
