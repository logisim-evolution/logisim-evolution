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

import com.cburch.hex.HexEditor;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.std.memory.MemContents;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class Clip implements ClipboardOwner {
  private static final DataFlavor binaryFlavor = new DataFlavor(long[].class, "Binary data");
  private final HexEditor editor;

  Clip(HexEditor editor) {
    this.editor = editor;
  }

  public boolean canPaste() {
    Clipboard clip = editor.getToolkit().getSystemClipboard();
    Transferable xfer = clip.getContents(this);
    return xfer.isDataFlavorSupported(binaryFlavor);
  }

  public void copy() {
    final var caret = editor.getCaret();
    var p0 = caret.getMark();
    var p1 = caret.getDot();
    if (p0 < 0 || p1 < 0) return;
    if (p0 > p1) {
      long t = p0;
      p0 = p1;
      p1 = t;
    }
    p1++;

    final var data = new long[(int) (p1 - p0)];
    final var model = editor.getModel();
    for (var i = p0; i < p1; i++) {
      data[(int) (i - p0)] = model.get(i);
    }

    final var clip = editor.getToolkit().getSystemClipboard();
    clip.setContents(new Data(data), this);
  }

  @Override
  public void lostOwnership(Clipboard clip, Transferable transfer) {}

  public void paste() {
    final var clip = editor.getToolkit().getSystemClipboard();
    final var xfer = clip.getContents(this);
    final var model = (MemContents) editor.getModel();
    MemContents pasted = null;
    var numWords = 0;
    if (xfer.isDataFlavorSupported(binaryFlavor)) {
      try {
        final var data = (long[]) xfer.getTransferData(binaryFlavor);
        numWords = data.length;
        var addrBits = 32 - Integer.numberOfLeadingZeros(numWords);
        pasted = MemContents.create(addrBits, model.getValueWidth(), false);
        pasted.set(0, data);
      } catch (UnsupportedFlavorException | IOException e) {
        return;
      }
    } else if (xfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      String buf;
      try {
        buf = (String) xfer.getTransferData(DataFlavor.stringFlavor);
      } catch (UnsupportedFlavorException | IOException e) {
        return;
      }

      try {
        final var r = HexFile.parseFromClipboard(buf, model.getLogLength(), model.getValueWidth());
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

    final var caret = editor.getCaret();
    var p0 = caret.getMark();
    var p1 = caret.getDot();
    if (p0 == p1) {
      if (p0 + numWords - 1 <= model.getLastOffset()) {
        model.copyFrom(p0, pasted, 0, numWords);
      } else {
        model.copyFrom(p0, pasted, 0, (int) (model.getLastOffset() - p0 + 1));
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
        var action =
            OptionPane.showConfirmDialog(
                editor.getRootPane(),
                S.get("hexPasteTooSmall", numWords, p1 - p0),
                S.get("hexPasteErrorTitle"),
                OptionPane.OK_CANCEL_OPTION,
                OptionPane.QUESTION_MESSAGE);
        if (action != OptionPane.OK_OPTION) return;
        p1 = p0 + numWords;
      } else if (p1 - p0 < numWords) {
        var action =
            OptionPane.showConfirmDialog(
                editor.getRootPane(),
                S.get("hexPasteTooSmall", numWords, p1 - p0),
                S.get("hexPasteErrorTitle"),
                OptionPane.OK_CANCEL_OPTION,
                OptionPane.QUESTION_MESSAGE);
        if (action != OptionPane.OK_OPTION) return;
        numWords = (int) (p1 - p0);
      }
      model.copyFrom(p0, pasted, 0, numWords);
    }
  }

  private static class Data implements Transferable {
    private final long[] data;

    Data(long[] data) {
      this.data = data;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (flavor == binaryFlavor) {
        return data;
      } else if (flavor == DataFlavor.stringFlavor) {
        var bits = 1;
        for (final var datum : data) {
          var k = datum >> bits;
          while (k != 0 && bits < 32) {
            bits++;
            k >>= 1;
          }
        }

        var chars = (bits + 3) / 4;
        final var buf = new StringBuilder();
        for (var i = 0; i < data.length; i++) {
          if (i > 0) buf.append(i % 8 == 0 ? '\n' : ' ');
          buf.append(String.format("%0" + chars + "x", data[i]));
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
}
