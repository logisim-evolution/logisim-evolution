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

package com.cburch.logisim.gui.hex;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.hex.Caret;
import com.cburch.hex.HexEditor;
import com.cburch.hex.HexModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.std.memory.MemContents;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class Clip implements ClipboardOwner {
  private static class Data implements Transferable {
    private long[] data;

    Data(long[] data) {
      this.data = data;
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
      if (flavor == binaryFlavor) {
        return data;
      } else if (flavor == DataFlavor.stringFlavor) {
        int bits = 1;
        for (int i = 0; i < data.length; i++) {
          long k = data[i] >> bits;
          while (k != 0 && bits < 32) {
            bits++;
            k >>= 1;
          }
        }

        int chars = (bits + 3) / 4;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
          if (i > 0) {
            buf.append(i % 8 == 0 ? '\n' : ' ');
          }
          String s = Long.toHexString(data[i]);
          while (s.length() < chars) s = "0" + s;
          buf.append(s);
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

  private static final DataFlavor binaryFlavor = new DataFlavor(long[].class, "Binary data");

  private HexEditor editor;

  Clip(HexEditor editor) {
    this.editor = editor;
  }

  public boolean canPaste() {
    Clipboard clip = editor.getToolkit().getSystemClipboard();
    Transferable xfer = clip.getContents(this);
    return xfer.isDataFlavorSupported(binaryFlavor);
  }

  public void copy() {
    Caret caret = editor.getCaret();
    long p0 = caret.getMark();
    long p1 = caret.getDot();
    if (p0 < 0 || p1 < 0) return;
    if (p0 > p1) {
      long t = p0;
      p0 = p1;
      p1 = t;
    }
    p1++;

    long[] data = new long[(int) (p1 - p0)];
    HexModel model = editor.getModel();
    for (long i = p0; i < p1; i++) {
      data[(int) (i - p0)] = model.get(i);
    }

    Clipboard clip = editor.getToolkit().getSystemClipboard();
    clip.setContents(new Data(data), this);
  }

  public void lostOwnership(Clipboard clip, Transferable transfer) {}

  public void paste() {
    Clipboard clip = editor.getToolkit().getSystemClipboard();
    Transferable xfer = clip.getContents(this);
    MemContents model = (MemContents)editor.getModel();
    MemContents pasted = null;
    int numWords = 0;
    if (xfer.isDataFlavorSupported(binaryFlavor)) {
      try {
        long[] data = (long[]) xfer.getTransferData(binaryFlavor);
        numWords = data.length;
        int addrBits = 32 - Integer.numberOfLeadingZeros(numWords);
        pasted = MemContents.create(addrBits, model.getValueWidth());
        pasted.set(0, data);
      } catch (UnsupportedFlavorException e) {
        return;
      } catch (IOException e) {
        return;
      }
    } else if (xfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      String buf;
      try {
        buf = (String) xfer.getTransferData(DataFlavor.stringFlavor);
      } catch (UnsupportedFlavorException e) {
        return;
      } catch (IOException e) {
        return;
      }

      try {
        HexFile.ParseResult r = HexFile.parseFromClipboard(buf, model.getLogLength(), model.getValueWidth());
        pasted = r.model;
        numWords = r.numWords;
      } catch (IOException e) {
        OptionPane.showMessageDialog(
            editor.getRootPane(),
            e.getMessage(),
            S.get("hexPasteErrorTitle"),
            OptionPane.ERROR_MESSAGE);
        return;
      }
    } else {
      OptionPane.showMessageDialog(
          editor.getRootPane(),
          S.get("hexPasteSupportedError"),
          S.get("hexPasteErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return;
    }

    Caret caret = editor.getCaret();
    long p0 = caret.getMark();
    long p1 = caret.getDot();
    if (p0 == p1) {
      if (p0 + numWords - 1 <= model.getLastOffset()) {
        ((MemContents)model).copyFrom(p0, pasted, 0, numWords);
      } else {
        ((MemContents)model).copyFrom(p0, pasted, 0, (int)(model.getLastOffset() - p0 + 1));
      }
    } else {
      if (p0 < 0 || p1 < 0) return;
      if (p0 > p1) {
        long t = p0;
        p0 = p1;
        p1 = t;
      }
      p1++;
      if (p1 - p0 > numWords) {
          int action = OptionPane.showConfirmDialog(editor.getRootPane(),
              S.fmt("hexPasteTooSmall", numWords, p1 - p0),
            S.get("hexPasteErrorTitle"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
        if (action != OptionPane.OK_OPTION)
          return;
        p1 = p0 + numWords;
      } else if (p1 - p0 < numWords) {
        int action = OptionPane.showConfirmDialog(editor.getRootPane(),
            S.fmt("hexPasteTooSmall", numWords, p1 - p0),
            S.get("hexPasteErrorTitle"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
        if (action != OptionPane.OK_OPTION)
          return;
        numWords = (int)(p1 - p0);
      }
      ((MemContents)model).copyFrom(p0, pasted, 0, numWords);
    }
  }
}
